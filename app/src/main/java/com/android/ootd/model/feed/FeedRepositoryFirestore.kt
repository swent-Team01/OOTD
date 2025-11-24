package com.android.ootd.model.feed

import android.util.Log
import com.android.ootd.model.map.locationFromMap
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

const val POSTS_COLLECTION_PATH = "posts"
private val MILLIS_IN_24_HOURS = Duration.ofHours(24).toMillis()

class FeedRepositoryFirestore(private val db: FirebaseFirestore) : FeedRepository {
  private val ownerAttributeName = "ownerId"

  override suspend fun getFeedForUids(uids: List<String>): List<OutfitPost> {
    if (uids.isEmpty()) return emptyList()

    val cleaned = uids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleaned.isEmpty()) return emptyList()

    return try {
      val chunks = cleaned.chunked(10) // whereIn supports up to 10 values
      val results = mutableListOf<OutfitPost>()
      for (chunk in chunks) {
        val snap =
            withTimeout(5_000) {
              db.collection(POSTS_COLLECTION_PATH)
                  .whereIn(ownerAttributeName, chunk)
                  .orderBy("timestamp")
                  .get()
                  .await()
            }
        results += snap.documents.mapNotNull { mapToPost(it) }
      }
      // Merge and sort by timestamp ascending
      results.sortedBy { it.timestamp }
    } catch (e: TimeoutCancellationException) {
      Log.w(
          "FeedRepositoryFirestore",
          "Timed out fetching friend-filtered feed; returning empty list",
          e)
      emptyList()
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error fetching friend-filtered feed", e)
      emptyList()
    }
  }

  override suspend fun getRecentFeedForUids(uids: List<String>): List<OutfitPost> {
    if (uids.isEmpty()) return emptyList()

    val cleaned = uids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleaned.isEmpty()) return emptyList()

    val now = System.currentTimeMillis()
    val twentyFourHoursAgo = now - MILLIS_IN_24_HOURS // milliseconds in 24h

    return try {
      val chunks = cleaned.chunked(10)
      val results = mutableListOf<OutfitPost>()
      for (chunk in chunks) {
        val snap =
            withTimeout(5_000) {
              db.collection(POSTS_COLLECTION_PATH)
                  .whereIn(ownerAttributeName, chunk)
                  .whereGreaterThanOrEqualTo("timestamp", twentyFourHoursAgo)
                  .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                  .get()
                  .await()
            }
        results += snap.documents.mapNotNull { mapToPost(it) }
      }

      // Sort descending (meaning most recent first)
      results.sortedByDescending { it.timestamp }
    } catch (e: TimeoutCancellationException) {
      Log.w("FeedRepositoryFirestore", "Timed out fetching recent feed; returning empty list", e)
      emptyList()
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error fetching recent friend feed", e)
      emptyList()
    }
  }

  override suspend fun addPost(post: OutfitPost) {
    try {
      withTimeout(5_000) {
        db.collection(POSTS_COLLECTION_PATH).document(post.postUID).set(post).await()
      }
      Log.d("FeedRepositoryFirestore", "Successfully added post ${post.postUID}")
    } catch (e: TimeoutCancellationException) {
      Log.w(
          "FeedRepositoryFirestore",
          "Timed out adding post ${post.postUID}; assuming offline-ack and continuing",
          e)
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error adding post: ${e.message}", e)
      throw e
    }
  }

  override suspend fun hasPostedToday(userId: String): Boolean {
    return try {
      val todayStart =
          LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
      val snapshot =
          db.collection(POSTS_COLLECTION_PATH)
              .whereEqualTo(ownerAttributeName, userId)
              .whereGreaterThanOrEqualTo("timestamp", todayStart)
              .get()
              .await()
      snapshot.documents.isNotEmpty()
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error checking hasPostedToday", e)
      false
    }
  }

  override fun getNewPostId(): String {
    return java.util.UUID.randomUUID().toString()
  }

  override suspend fun getPostById(postUuid: String): OutfitPost? {
    val doc = db.collection(POSTS_COLLECTION_PATH).document(postUuid).get().await()
    return mapToPost(doc) ?: throw Exception("ItemsRepositoryFirestore: Item not found")
  }

  override fun observeRecentFeedForUids(uids: List<String>): Flow<List<OutfitPost>> = callbackFlow {
    if (uids.isEmpty()) {
      trySend(emptyList())
      close()
      return@callbackFlow
    }

    val cleaned = uids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleaned.isEmpty()) {
      trySend(emptyList())
      close()
      return@callbackFlow
    }

    val now = System.currentTimeMillis()
    val twentyFourHoursAgo = now - MILLIS_IN_24_HOURS

    val listeners = mutableListOf<ListenerRegistration>()
    val allPosts = mutableMapOf<String, OutfitPost>()
    val listenersInitializedSet = mutableSetOf<Int>()
    val lock = Any()

    try {
      // divide in chunks as firestore only allows whereIn with max 10 elements
      val chunks = cleaned.chunked(10)

      chunks.forEachIndexed { chunkIndex, chunk ->
        val listener =
            db.collection(POSTS_COLLECTION_PATH)
                .whereIn(ownerAttributeName, chunk)
                .whereGreaterThanOrEqualTo("timestamp", twentyFourHoursAgo)
                .addSnapshotListener { snapshot, error ->
                  if (error != null) {
                    Log.e("FeedRepositoryFirestore", "Error observing posts", error)
                    return@addSnapshotListener
                  }

                  if (snapshot != null) {
                    var shouldEmit = false

                    synchronized(lock) {
                      // Process document changes
                      snapshot.documentChanges.forEach { change ->
                        val post = mapToPost(change.document)
                        if (post != null) {
                          when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                              allPosts[post.postUID] = post
                            }
                            DocumentChange.Type.REMOVED -> {
                              allPosts.remove(post.postUID)
                            }
                          }
                        }
                      }

                      // Track initialization - each listener's first snapshot counts
                      val wasNotInitialized = listenersInitializedSet.add(chunkIndex)

                      // Emit if all listeners initialized OR if this is an update after
                      // initialization
                      shouldEmit =
                          listenersInitializedSet.size >= chunks.size ||
                              (!wasNotInitialized && snapshot.documentChanges.isNotEmpty())
                    }

                    if (shouldEmit) {
                      val sortedPosts =
                          synchronized(lock) { allPosts.values.sortedByDescending { it.timestamp } }
                      trySend(sortedPosts)
                    }
                  }
                }

        listeners.add(listener)
      }
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error setting up observers", e)
      trySend(emptyList())
    }

    awaitClose { listeners.forEach { it.remove() } }
  }

  private fun mapToPost(doc: DocumentSnapshot): OutfitPost? {
    return try {
      val postUuid = doc.getString("postUID") ?: ""
      val ownerId = doc.getString("ownerId") ?: ""
      val timestamp = doc.getLong("timestamp") ?: 0L
      val description = doc.getString("description") ?: ""
      val itemsList = doc["itemsID"] as? List<*>
      val itemUuids = itemsList?.mapNotNull { it as? String } ?: emptyList()
      val name = doc.getString("name") ?: ""
      val outfitUrl = doc.getString("outfitURL") ?: ""
      val userProfilePicture = doc.getString("userProfilePicURL") ?: ""
      val location = locationFromMap(doc["location"] as? Map<*, *>)

      OutfitPost(
          postUID = postUuid,
          ownerId = ownerId,
          timestamp = timestamp,
          description = description,
          itemsID = itemUuids,
          name = name,
          outfitURL = outfitUrl,
          userProfilePicURL = userProfilePicture,
          location = location)
    } catch (e: Exception) {
      Log.e("ItemsRepositoryFirestore", "Error converting document ${doc.id} to Item", e)
      null
    }
  }
}
