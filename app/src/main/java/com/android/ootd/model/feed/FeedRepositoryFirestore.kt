package com.android.ootd.model.feed

import android.util.Log
import com.android.ootd.model.post.OutfitPost
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

const val POSTS_COLLECTION_PATH = "posts"

class FeedRepositoryFirestore(private val db: FirebaseFirestore) : FeedRepository {

  //  /** Helper method to validate post data. */
  //  private fun checkPostData(post: OutfitPost): OutfitPost? {
  //    if (post.postUID.isBlank() || post.uid.isBlank() || post.outfitURL.isBlank()) {
  //      Log.e("FeedRepositoryFirestore", "Invalid post data for postUUID=${post.postUID}")
  //      return null
  //    }
  //    return post
  //  }
  //
  //  /** Helper method to safely transform a Firestore document into a post. */
  //  private fun transformPostDocument(document: DocumentSnapshot): OutfitPost? {
  //    return try {
  //      val post = document.toObject<OutfitPost>()
  //      if (post == null) {
  //        Log.e(
  //            "FeedRepositoryFirestore",
  //            "Failed to deserialize document ${document.id} to OutfitPost. Data:
  // ${document.data}")
  //        return null
  //      }
  //      checkPostData(post)
  //    } catch (e: Exception) {
  //      Log.e(
  //          "FeedRepositoryFirestore", "Error transforming document ${document.id}: ${e.message}",
  // e)
  //      null
  //    }
  //  }

  override suspend fun getFeed(): List<OutfitPost> {
    return try {
      val snapshot =
          withTimeout(5_000) {
            db.collection(POSTS_COLLECTION_PATH)
                .orderBy("timestamp") // Sorted by newest first, can be changed
                .get()
                .await()
          }

      snapshot.documents.mapNotNull { it.toObject<OutfitPost>() }
    } catch (e: TimeoutCancellationException) {
      Log.w("FeedRepositoryFirestore", "Timed out fetching feed; returning empty list", e)
      emptyList()
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error fetching feed", e)
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
              .whereEqualTo("uid", userId)
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
