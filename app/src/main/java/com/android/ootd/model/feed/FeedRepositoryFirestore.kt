package com.android.ootd.model.feed

import android.util.Log
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

const val POSTS_COLLECTION_PATH = "posts"

class FeedRepositoryFirestore(private val db: FirebaseFirestore) : FeedRepository {
  private val ownerAttributeName = "ownerId"

  override suspend fun getUserPosts(): List<OutfitPost> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("ToDosRepositoryFirestore: User not logged in.")
    return try {
      val snapshot =
          withTimeout(5_000) {
            db.collection(POSTS_COLLECTION_PATH)
                .whereEqualTo(ownerAttributeName, ownerId)
                .orderBy("timestamp")
                .get()
                .await()
          }

      snapshot.toObjects<OutfitPost>()
    } catch (e: TimeoutCancellationException) {
      Log.w("FeedRepositoryFirestore", "Timed out fetching feed; returning empty list", e)
      emptyList()
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error fetching feed", e)
      emptyList()
    }
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
            withTimeout(5_000) {
              db.collection(POSTS_COLLECTION_PATH)
                  .whereIn(ownerAttributeName, chunk)
                  .orderBy("timestamp")
                  .get()
                  .await()
            }
        results += snap.toObjects<OutfitPost>()
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
}
