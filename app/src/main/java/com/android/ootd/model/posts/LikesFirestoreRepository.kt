package com.android.ootd.model.posts

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val LIKE_COLLECTION_PATH = "likes"
private const val TAG_LIKE_REPOSITORY = "LikeRepository"

class LikesFirestoreRepository(private val db: FirebaseFirestore) : LikesRepository {

  override suspend fun likePost(like: Like) {
    try {
      db.collection(LIKE_COLLECTION_PATH)
          .document(like.postId)
          .collection("users")
          .document(like.postLikerUserId)
          .set(
              mapOf(
                  "postId" to like.postId,
                  "likerUserId" to like.postLikerUserId,
                  "timestamp" to like.timestamp),
          )
          .await()
    } catch (e: Exception) {
      Log.e(TAG_LIKE_REPOSITORY, "Failed to add like for post ${like.postId}", e)
      throw e
    }
  }

  override suspend fun unlikePost(postId: String, userId: String) {
    try {
      db.collection(LIKE_COLLECTION_PATH)
          .document(postId)
          .collection("users")
          .document(userId)
          .delete()
          .await()
    } catch (e: Exception) {
      Log.e(TAG_LIKE_REPOSITORY, "Failed to remove like for post $postId", e)
      throw e
    }
  }

  override suspend fun isPostLikedByUser(postId: String, userId: String): Boolean {
    return try {
      val snap =
          db.collection(LIKE_COLLECTION_PATH)
              .document(postId)
              .collection("users")
              .document(userId)
              .get()
              .await()

      snap.exists()
    } catch (e: Exception) {
      Log.e(TAG_LIKE_REPOSITORY, "Failed to check like state for $postId", e)
      false
    }
  }

  override suspend fun getLikesForPost(postId: String): List<Like> {
    return try {
      val snap =
          db.collection(LIKE_COLLECTION_PATH).document(postId).collection("users").get().await()

      snap.documents.mapNotNull { doc ->
        try {
          Like(
              postId = postId,
              postLikerUserId = doc.getString("likerUserId") ?: return@mapNotNull null,
              timestamp = doc.getLong("timestamp") ?: 0L)
        } catch (e: Exception) {
          Log.e(TAG_LIKE_REPOSITORY, "Invalid like document ${doc.id}", e)
          null
        }
      }
    } catch (e: Exception) {
      Log.e(TAG_LIKE_REPOSITORY, "Failed to fetch likes for $postId", e)
      emptyList()
    }
  }

  override suspend fun getLikeCount(postId: String): Int {
    return try {
      db.collection(LIKE_COLLECTION_PATH).document(postId).collection("users").get().await().size()
    } catch (e: Exception) {
      Log.e(TAG_LIKE_REPOSITORY, "Failed to retrieve like count for $postId", e)
      0
    }
  }
}
