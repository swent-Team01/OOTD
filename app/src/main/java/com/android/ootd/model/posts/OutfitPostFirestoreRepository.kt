package com.android.ootd.model.post

import android.util.Log
import com.android.ootd.model.account.MissingLocationException
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.posts.Comment
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.LocationUtils.locationFromMap
import com.android.ootd.utils.LocationUtils.mapFromLocation
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/** Firestore collection name for outfit posts* */
const val POSTS_COLLECTION = "posts"
/** Tag for logging in OutfitPostRepository * */
const val OUTFITPOST_TAG = "OutfitPostRepository"
/** Firestore collection name for outfit images * */
const val POSTS_IMAGES_FOLDER = "images/posts"
/** Firestore collection name for comment reaction images * */
const val COMMENT_REACTIONS_FOLDER = "images/comment_reactions"

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

  override suspend fun uploadOutfitWithCompressedPhoto(
      imageData: ByteArray,
      postId: String
  ): String {
    return try {
      val ref = storage.reference.child("$POSTS_IMAGES_FOLDER/$postId.jpg")
      ref.putBytes(imageData).await()
      ref.downloadUrl.await().toString()
    } catch (e: Exception) {
      Log.e(OUTFITPOST_TAG, "Upload failed (test or offline env): ${e.javaClass.simpleName}")
      ""
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
            "location" to mapFromLocation(post.location),
            "isPublic" to post.isPublic)

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

  // Functions concerning uploading comments
  override suspend fun uploadCompressedReactionImage(
      imageData: ByteArray,
      commentId: String
  ): String {
    return try {
      val ref = storage.reference.child("$COMMENT_REACTIONS_FOLDER/$commentId.jpg")
      ref.putBytes(imageData).await()
      ref.downloadUrl.await().toString()
    } catch (e: Exception) {
      Log.e(OUTFITPOST_TAG, "Failed to upload reaction image", e)
      throw e
    }
  }

  override suspend fun addCommentToPost(
      postId: String,
      userId: String,
      commentText: String,
      reactionImageData: ByteArray? // Optional - null means no reaction image
  ): Comment {
    // Generate a unique comment ID
    val commentId = db.collection(POSTS_COLLECTION).document().id

    // Upload reaction image if provided
    val reactionImageUrl =
        if (reactionImageData != null) {
          uploadCompressedReactionImage(reactionImageData, commentId)
        } else {
          "" // No reaction image
        }

    // Create the comment object
    val comment =
        Comment(
            commentId = commentId,
            ownerId = userId,
            text = commentText,
            timestamp = System.currentTimeMillis(),
            reactionImage = reactionImageUrl)

    // Add comment to the post's comments array
    db.collection(POSTS_COLLECTION)
        .document(postId)
        .update("comments", FieldValue.arrayUnion(commentToMap(comment)))
        .await()

    return comment
  }

  override suspend fun deleteCommentFromPost(postId: String, comment: Comment) {
    // Remove comment from Firestore array
    db.collection(POSTS_COLLECTION)
        .document(postId)
        .update("comments", FieldValue.arrayRemove(commentToMap(comment)))
        .await()

    // Delete the reaction image from Storage (if it exists)
    if (comment.reactionImage.isNotEmpty()) {
      try {
        storage.reference
            .child("$COMMENT_REACTIONS_FOLDER/${comment.commentId}.jpg")
            .delete()
            .await()
      } catch (e: Exception) {
        Log.w(OUTFITPOST_TAG, "Could not delete reaction image", e)
        // we don't throw, comment is already deleted from Firestore
      }
    }
  }

  /**
   * Converts a [Comment] object to a Firestore-compatible map. This ensures consistent mapping
   * between Comment objects and Firestore documents.
   */
  private fun commentToMap(comment: Comment): Map<String, Any> {
    return mapOf(
        "commentId" to comment.commentId,
        "userId" to comment.ownerId,
        "text" to comment.text,
        "timestamp" to comment.timestamp,
        "reactionImageUrl" to comment.reactionImage)
  }

  /** Converts a Firestore [DocumentSnapshot] into an [OutfitPost] model. */
  private fun mapToOutfitPost(doc: DocumentSnapshot): OutfitPost? {
    return try {
      // Safely cast the 'itemsID' field to a list of Strings
      // Firestore stores lists as List<*>, so this would filter out any non-string values
      val rawItemsList = doc["itemsID"] as? List<*> ?: emptyList<Any>()
      val itemsID = rawItemsList.mapNotNull { it as? String }

      // Parse comments if present
      val rawCommentsList = doc["comments"] as? List<*> ?: emptyList<Any>()
      val comments = rawCommentsList.mapNotNull { mapToComment(it) }

      // Parse location if present, otherwise throw MissingLocationException
      val locationRaw = doc["location"]
      val location =
          when {
            locationRaw == null -> emptyLocation
            locationRaw is Map<*, *> -> locationFromMap(locationRaw)
            else -> throw MissingLocationException()
          }

      val isPublic = doc.getBoolean("isPublic") ?: false

      OutfitPost(
          postUID = doc.getString("postUID") ?: "",
          name = doc.getString("name") ?: "",
          ownerId = doc.getString("ownerId") ?: "",
          userProfilePicURL = doc.getString("userProfilePicURL") ?: "",
          outfitURL = doc.getString("outfitURL") ?: "",
          description = doc.getString("description") ?: "",
          itemsID = itemsID,
          timestamp = doc.getLong("timestamp") ?: 0L,
          location = location,
          comments = comments,
          isPublic = isPublic)
    } catch (e: Exception) {
      Log.e(OUTFITPOST_TAG, "Error converting document ${doc.id} to OutfitPost", e)
      null
    }
  }

  /** Converts a Firestore comment map into a [Comment] model. Returns null if parsing fails. */
  private fun mapToComment(commentData: Any?): Comment? {
    return try {
      val commentMap = commentData as? Map<*, *> ?: return null
      Comment(
          commentId = commentMap["commentId"] as? String ?: "",
          ownerId = commentMap["userId"] as? String ?: "",
          text = commentMap["text"] as? String ?: "",
          timestamp = (commentMap["timestamp"] as? Long) ?: 0L,
          reactionImage = commentMap["reactionImageUrl"] as? String ?: "")
    } catch (e: Exception) {
      Log.e(OUTFITPOST_TAG, "Error parsing comment", e)
      null
    }
  }
}
