package com.android.ootd.model.reactions

import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

private const val TAG = "ReactionRepository"
private const val REACTION_IMAGES_FOLDER = "images/reactions"
private const val REACTIONS_COLLECTION = "reactions"

class ReactionFirestoreRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ReactionRepository {

  override suspend fun uploadReactionImage(
      postId: String,
      userId: String,
      localPath: String
  ): String {
    val ref = storage.reference.child("$REACTION_IMAGES_FOLDER/$postId/$userId.jpg")

    return try {
      ref.putFile(localPath.toUri()).await()
      ref.downloadUrl.await().toString()
    } catch (e: Exception) {
      Log.e(TAG, "uploadReactionImage failed", e)
      throw e
    }
  }

  override suspend fun addOrReplaceReaction(
      postId: String,
      userId: String,
      localImagePath: String
  ) {
    // Upload the image
    val reactionURL = uploadReactionImage(postId, userId, localImagePath)

    // Build the reaction document
    val data =
        mapOf(
            "postId" to postId,
            "ownerId" to userId,
            "reactionURL" to reactionURL,
            "timestamp" to System.currentTimeMillis())

    // Store reaction
    db.collection(REACTIONS_COLLECTION)
        .document(postId)
        .collection("users")
        .document(userId)
        .set(data)
        .await()
  }

  override suspend fun deleteReaction(postId: String, userId: String) {
    // Delete Firestore doc
    try {
      db.collection(REACTIONS_COLLECTION)
          .document(postId)
          .collection("users")
          .document(userId)
          .delete()
          .await()
    } catch (e: Exception) {
      Log.w(TAG, "deleteReaction doc failed", e)
    }

    // Delete Storage file
    try {
      storage.reference.child("$REACTION_IMAGES_FOLDER/$postId/$userId.jpg").delete().await()
    } catch (e: Exception) {
      Log.w(TAG, "deleteReaction image failed", e)
    }
  }

  override suspend fun getUserReaction(postId: String, userId: String): Reaction? {
    return try {
      val doc =
          db.collection(REACTIONS_COLLECTION)
              .document(postId)
              .collection("users")
              .document(userId)
              .get()
              .await()

      if (!doc.exists()) return null

      Reaction(
          postUID = doc.getString("postId") ?: "",
          ownerId = doc.getString("ownerId") ?: "",
          reactionURL = doc.getString("reactionURL") ?: "")
    } catch (e: Exception) {
      Log.e(TAG, "getReaction failed", e)
      null
    }
  }

  override suspend fun getAllReactions(postId: String): List<Reaction> {
    return try {
      val snap =
          db.collection(REACTIONS_COLLECTION).document(postId).collection("users").get().await()

      snap.documents.map {
        Reaction(
            postUID = it.getString("postId") ?: postId,
            ownerId = it.getString("ownerId") ?: "",
            reactionURL = it.getString("reactionURL") ?: "")
      }
    } catch (e: Exception) {
      Log.e(TAG, "getAllReactions failed", e)
      emptyList()
    }
  }
}
