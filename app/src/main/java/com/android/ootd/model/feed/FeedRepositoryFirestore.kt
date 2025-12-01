package com.android.ootd.model.feed

import android.util.Log
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.LocationUtils.locationFromMap
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

const val POSTS_COLLECTION_PATH = "posts"

const val REFRESH_INTERVAL_MILLIS = 60_000L
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

  override suspend fun getPublicFeed(): List<OutfitPost> {
    return try {
      val snapshot =
          withTimeout(5_000) {
            db.collection(POSTS_COLLECTION_PATH)
                .whereEqualTo("isPublic", true)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
          }
      snapshot.documents.mapNotNull { mapToPost(it) }
    } catch (e: TimeoutCancellationException) {
      Log.w("FeedRepositoryFirestore", "Timed out fetching public feed", e)
      emptyList()
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error fetching public feed", e)
      emptyList()
    }
  }

  override fun observeRecentFeedForUids(uids: List<String>): Flow<List<OutfitPost>> = flow {
    // Emit initial state immediately
    val initialPosts = getRecentFeedForUids(uids)
    emit(initialPosts)

    // For Firestore, we poll every 60 seconds
    // This could be replaced with real snapshot listeners if needed
    while (true) {
      kotlinx.coroutines.delay(REFRESH_INTERVAL_MILLIS)
      try {
        val posts = getRecentFeedForUids(uids)
        emit(posts)
      } catch (e: Exception) {
        Log.e("FeedRepositoryFirestore", "Error polling posts", e)
        // Continue polling even on error
      }
    }
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
      val isPublic = doc.getBoolean("isPublic") ?: false

      OutfitPost(
          postUID = postUuid,
          ownerId = ownerId,
          timestamp = timestamp,
          description = description,
          itemsID = itemUuids,
          name = name,
          outfitURL = outfitUrl,
          userProfilePicURL = userProfilePicture,
          location = location,
          isPublic = isPublic)
    } catch (e: Exception) {
      Log.e("ItemsRepositoryFirestore", "Error converting document ${doc.id} to Item", e)
      null
    }
  }
}
