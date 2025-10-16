package com.android.ootd.model.post

import android.util.Log
import com.android.ootd.model.OutfitPost
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/** Firestore collection name for outfit posts* */
const val POSTS_COLLECTION = "outfit_posts"

/** Firestore collection name for outfit images * */
const val POSTS_IMAGES_FOLDER = "images"

/**
 * Repository implementation that handles all OutfitPost operations involving both Firestore and
 * Firebase Storage.
 *
 * Each post is stored as a Firestore document in [POSTS_COLLECTION], while its corresponding image
 * is stored under [POSTS_IMAGES_FOLDER]/{postId}.jpg.
 *
 * Firestore stores lightweight metadata (text, URLs, timestamps), and Storage handles binary image
 * uploads/downloads.
 *
 * The `postUID` acts as the shared identifier across both systems.
 */
class OutfitPostRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : OutfitPostRepository {

  override fun getNewPostId(): String = db.collection(POSTS_COLLECTION).document().id

  override suspend fun uploadOutfitPhoto(localPath: String, postId: String): String {
    val ref = storage.reference.child("$POSTS_IMAGES_FOLDER/$postId.jpg")
    val fileUri = android.net.Uri.parse(localPath)
    ref.putFile(fileUri).await()
    return ref.downloadUrl.await().toString()
  }

  override suspend fun savePostToFirestore(post: OutfitPost) {
    val data =
        mapOf(
            "postUID" to post.postUID,
            "name" to post.name,
            "uid" to post.uid,
            "userProfilePicURL" to post.userProfilePicURL,
            "outfitURL" to post.outfitURL,
            "description" to post.description,
            "itemsID" to post.itemsID,
            "timestamp" to post.timestamp)

    db.collection(POSTS_COLLECTION).document(post.postUID).set(data).await()
  }

  override suspend fun getPostById(postId: String): OutfitPost? {
    val doc = db.collection(POSTS_COLLECTION).document(postId).get().await()
    return if (doc.exists()) mapToOutfitPost(doc) else null
  }

  override suspend fun deletePost(postId: String) {
    db.collection(POSTS_COLLECTION).document(postId).delete().await()
    try {
      storage.reference.child("$POSTS_IMAGES_FOLDER/$postId.jpg").delete().await()
    } catch (e: Exception) {
      Log.w("OutfitPostRepository", "No image found for post $postId — skipping delete.")
    }
  }

  /** Converts a Firestore [DocumentSnapshot] into an [OutfitPost] model. */
  private fun mapToOutfitPost(doc: DocumentSnapshot): OutfitPost? {
    return try {
      OutfitPost(
          postUID = doc.getString("postUID") ?: "",
          name = doc.getString("name") ?: "",
          uid = doc.getString("uid") ?: "",
          userProfilePicURL = doc.getString("userProfilePicURL") ?: "",
          outfitURL = doc.getString("outfitURL") ?: "",
          description = doc.getString("description") ?: "",
          itemsID = doc.get("itemsID") as? List<String> ?: emptyList(),
          timestamp = doc.getLong("timestamp") ?: 0L)
    } catch (e: Exception) {
      Log.e("OutfitPostRepository", "Error converting document ${doc.id} to OutfitPost", e)
      null
    }
  }
}
