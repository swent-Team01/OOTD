package com.android.ootd.model.feed

import android.util.Log
import com.android.ootd.model.OutfitPost
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

const val POSTS_COLLECTION_PATH = "posts"

class FeedRepositoryFirestore(private val db: FirebaseFirestore) : FeedRepository {

  /** Helper method to validate post data. */
  private fun checkPostData(post: OutfitPost): OutfitPost? {
    if (post.postUUID.isBlank() || post.userID.isBlank() || post.outfitURL.isBlank()) {
      Log.e("FeedRepositoryFirestore", "Invalid post data for postUUID=${post.postUUID}")
      return null
    }
    return post
  }

  /** Helper method to safely transform a Firestore document into a post. */
  private fun transformPostDocument(document: DocumentSnapshot): OutfitPost? {
    return try {
      val post = document.toObject<OutfitPost>()
      if (post == null) {
        Log.e(
            "FeedRepositoryFirestore",
            "Failed to deserialize document ${document.id} to OutfitPost. Data: ${document.data}")
        return null
      }
      checkPostData(post)
    } catch (e: Exception) {
      Log.e(
          "FeedRepositoryFirestore", "Error transforming document ${document.id}: ${e.message}", e)
      null
    }
  }

  override suspend fun getFeed(): List<OutfitPost> {
    return try {
      val snapshot =
          db.collection(POSTS_COLLECTION_PATH)
              .orderBy("timestamp") // I sorted by newest firs, can be changed
              .get()
              .await()

      snapshot.documents.mapNotNull { it.toObject<OutfitPost>() }
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error fetching feed", e)
      emptyList()
    }
  }

  override suspend fun addPost(post: OutfitPost) {
    try {
      db.collection(POSTS_COLLECTION_PATH).document(post.postUUID).set(post).await()
      Log.d("FeedRepositoryFirestore", "Successfully added post ${post.postUUID}")
    } catch (e: Exception) {
      Log.e("FeedRepositoryFirestore", "Error adding post: ${e.message}", e)
      throw e
    }
  }

  override suspend fun hasPostedToday(): Boolean {
    // TODO: Will need to connect to authentication to retrieve the user state
    return false
  }

  override fun getNewPostId(): String {
    return java.util.UUID.randomUUID().toString()
  }
}
