package com.android.ootd.model.post

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.android.ootd.model.OutfitPost
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

const val POSTS_COLLECTION = "posts"
const val POSTS_IMAGES_FOLDER = "images"

class OutfitPostRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : OutfitPostRepository {
  override fun getNewPostId(): String = db.collection(POSTS_COLLECTION).document().id

  override suspend fun uploadOutfitPhoto(localUri: Uri, postId: String): String {
    val ref = storage.reference.child("$POSTS_IMAGES_FOLDER/$postId.jpg")
    ref.putFile(localUri).await()
    return ref.downloadUrl.await().toString()
  }

  override suspend fun addPost(post: OutfitPost) {
    val data =
        mapOf(
            "postUID" to post.postUID,
            "name" to post.name,
            "uid" to post.uid,
            "userProfilePicURL" to post.userProfilePicURL.toString(),
            "outfitURL" to post.outfitURL.toString(),
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

  private fun mapToOutfitPost(doc: DocumentSnapshot): OutfitPost? {
    return try {
      OutfitPost(
          postUID = doc.getString("postUID") ?: "",
          name = doc.getString("name") ?: "",
          uid = doc.getString("uid") ?: "",
          userProfilePicURL = doc.getString("userProfilePicURL")?.toUri() ?: Uri.EMPTY,
          outfitURL = doc.getString("outfitURL")?.toUri() ?: Uri.EMPTY,
          description = doc.getString("description") ?: "",
          itemsID = doc.get("itemsID") as? List<String> ?: emptyList(),
          timestamp = doc.getLong("timestamp") ?: 0L)
    } catch (e: Exception) {
      Log.e("OutfitPostRepository", "Error converting document ${doc.id} to OutfitPost", e)
      null
    }
  }
}
