package com.android.ootd.model.feed

import android.util.Log
import com.android.ootd.model.posts.Comment
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.LocationUtils.locationFromMap
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
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

  companion object {
    private const val MAX_BATCH_SIZE = 20L
  }

  override suspend fun getFeedForUids(uids: List<String>): List<OutfitPost> {
    if (uids.isEmpty()) return emptyList()

    val cleaned = uids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleaned.isEmpty()) return emptyList()

    return try {
      val chunks = cleaned.chunked(10) // whereIn supports up to 10 values
      val results = mutableListOf<OutfitPost>()
      for (chunk in chunks) {
        val snap =
            db.collection(POSTS_COLLECTION_PATH)
                .whereIn(ownerAttributeName, chunk)
                .orderBy("timestamp")
                .get()
                .await()
        results += snap.documents.mapNotNull { mapToPost(it) }
      }
      // Merge and sort by timestamp ascending
      results.sortedBy { it.timestamp }
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
            db.collection(POSTS_COLLECTION_PATH)
                .whereIn(ownerAttributeName, chunk)
                .whereGreaterThanOrEqualTo("timestamp", twentyFourHoursAgo)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
        results += snap.documents.mapNotNull { mapToPost(it) }
      }

      // Sort descending (meaning most recent first)
      results.sortedByDescending { it.timestamp }
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
    val now = System.currentTimeMillis()
    val twentyFourHoursAgo = now - MILLIS_IN_24_HOURS

    return try {
      val snapshot =
          db.collection(POSTS_COLLECTION_PATH)
              .whereEqualTo("isPublic", true)
              .whereGreaterThanOrEqualTo("timestamp", twentyFourHoursAgo)
              .orderBy("timestamp", Query.Direction.DESCENDING)
              .limit(MAX_BATCH_SIZE)
              .get()
              .await()

      snapshot.documents.mapNotNull { mapToPost(it) }
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

  override suspend fun getCachedFriendFeed(uids: List<String>): List<OutfitPost> {
    return try {
      val chunks = uids.distinct().chunked(10)
      val results = mutableListOf<OutfitPost>()
      for (chunk in chunks) {
        val snap =
            db.collection(POSTS_COLLECTION_PATH)
                .whereIn("ownerId", chunk)
                .get(Source.CACHE) // Fetch from cache
                .await()

        results += snap.documents.mapNotNull { mapToPost(it) }
      }
      results
          .filter { it.timestamp >= System.currentTimeMillis() - MILLIS_IN_24_HOURS }
          .sortedByDescending { it.timestamp }
    } catch (_: Exception) {
      emptyList()
    }
  }

  override suspend fun getCachedPublicFeed(): List<OutfitPost> {
    return try {
      val snap =
          db.collection(POSTS_COLLECTION_PATH)
              .whereEqualTo("isPublic", true)
              .get(Source.CACHE) // Fetch from cache
              .await()

      val now = System.currentTimeMillis()
      val twentyFourHoursAgo = now - MILLIS_IN_24_HOURS

      snap.documents
          .mapNotNull { mapToPost(it) }
          .filter { it.timestamp >= twentyFourHoursAgo }
          .sortedByDescending { it.timestamp }
    } catch (_: Exception) {
      emptyList()
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

      // ADD THIS: Parse comments
      val rawCommentsList = doc["comments"] as? List<*> ?: emptyList<Any>()
      val comments = rawCommentsList.mapNotNull { mapToComment(it) }

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
          isPublic = isPublic,
          comments = comments)
    } catch (e: Exception) {
      Log.e("FeedRepository", "Error mapping post", e)
      null
    }
  }

  // Helper to map comment data to Comment object
  private fun mapToComment(commentData: Any?): Comment? {
    return try {
      val commentMap = commentData as? Map<*, *> ?: return null
      Comment(
          commentId = commentMap["commentId"] as? String ?: "",
          ownerId = commentMap["ownerId"] as? String ?: "",
          text = commentMap["text"] as? String ?: "",
          timestamp = (commentMap["timestamp"] as? Long) ?: 0L,
          reactionImage = commentMap["reactionImage"] as? String ?: "")
    } catch (e: Exception) {
      Log.e("FeedRepository", "Error parsing comment", e)
      null
    }
  }
}
