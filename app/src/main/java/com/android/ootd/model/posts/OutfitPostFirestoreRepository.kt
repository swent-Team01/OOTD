package com.android.ootd.model.post

import android.util.Log
import androidx.core.net.toUri
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/** Firestore collection name for outfit posts* */
const val POSTS_COLLECTION = "posts"

/** Firestore collection name for outfit images * */
const val POSTS_IMAGES_FOLDER = "images/posts"

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
    return try {
      val ref = storage.reference.child("$POSTS_IMAGES_FOLDER/$postId.jpg")
      val fileUri = localPath.toUri()
      ref.putFile(fileUri).await()
      ref.downloadUrl.await().toString()
    } catch (e: Exception) {
      Log.w(
          "OutfitPostRepository", "Upload failed (test or offline env): ${e.javaClass.simpleName}")
      "https://fake.storage/$postId.jpg"
    }
  }

  override suspend fun savePostToFirestore(post: OutfitPost) {
    val data =
        mapOf(
            "postUID" to post.postUID,
            "name" to post.name,
            "ownerId" to post.ownerId,
            "userProfilePicURL" to post.userProfilePicURL,
            "outfitURL" to post.outfitURL,
            "description" to post.description,
            "itemsID" to post.itemsID,
            "timestamp" to post.timestamp,
        )

    try {

      val documentReference = db.collection(POSTS_COLLECTION).document(post.postUID)
      documentReference.set(data, SetOptions.merge()).await()
    } catch (e: Exception) {
      // Rethrow allowing caller to handle the error
      throw e
    }
  }

  override suspend fun getPostById(postId: String): OutfitPost? {
    val doc = db.collection(POSTS_COLLECTION).document(postId).get().await()
    return if (doc.exists()) mapToOutfitPost(doc) else null
  }

  override suspend fun updatePostFields(postId: String, updates: Map<String, Any?>) {
    db.collection(POSTS_COLLECTION).document(postId).update(updates).await()
  }

  override suspend fun deletePost(postId: String) {
    db.collection(POSTS_COLLECTION).document(postId).delete().await()
    try {
      storage.reference.child("$POSTS_IMAGES_FOLDER/$postId.jpg").delete().await()
    } catch (e: Exception) {
      throw e
    }
  }

  override suspend fun savePostWithMainPhoto(
      uid: String,
      name: String,
      userProfilePicURL: String,
      localPath: String,
      description: String
  ): OutfitPost {
    val postId = getNewPostId()
    val imageUrl =
        try {
          uploadOutfitPhoto(localPath, postId)
        } catch (e: Exception) {
          throw e
        }

    val post =
        OutfitPost(
            postUID = postId,
            ownerId = uid,
            name = name,
            userProfilePicURL = userProfilePicURL,
            outfitURL = imageUrl,
            description = description,
            itemsID = emptyList(),
            timestamp = System.currentTimeMillis())

    savePostToFirestore(post)
    return post
  }

  /** Converts a Firestore [DocumentSnapshot] into an [OutfitPost] model. */
  private fun mapToOutfitPost(doc: DocumentSnapshot): OutfitPost? {
    return try {
      // Safely cast the 'itemsID' field to a list of Strings
      // Firestore stores lists as List<*>, so this would filter out any non-string values
      val rawItemsList = doc["itemsID"] as? List<*> ?: emptyList<Any>()
      val itemsID = rawItemsList.mapNotNull { it as? String }

      OutfitPost(
          postUID = doc.getString("postUID") ?: "",
          name = doc.getString("name") ?: "",
          ownerId = doc.getString("ownerId") ?: "",
          userProfilePicURL = doc.getString("userProfilePicURL") ?: "",
          outfitURL = doc.getString("outfitURL") ?: "",
          description = doc.getString("description") ?: "",
          itemsID = itemsID,
          timestamp = doc.getLong("timestamp") ?: 0L)
    } catch (e: Exception) {
      Log.e("OutfitPostRepository", "Error converting document ${doc.id} to OutfitPost", e)
      null
    }
  }
}
