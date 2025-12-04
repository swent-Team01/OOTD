package com.android.ootd.model.post

import com.android.ootd.model.posts.Comment
import com.android.ootd.model.posts.OutfitPost

interface OutfitPostRepository {

  /**
   * Generates a new unique ID for a post. Used for both the Firestore document and the Storage
   * image filename.
   */
  fun getNewPostId(): String

  /**
   * Saves the post metadata to Firestore under the 'posts' collection.
   *
   * @param post The OutfitPost object containing all post data.
   */
  suspend fun savePostToFirestore(post: OutfitPost)

  /**
   * Fetches a post by its ID.
   *
   * @param postId The ID of the post to retrieve.
   * @return The OutfitPost if found, or null otherwise.
   */
  suspend fun getPostById(postId: String): OutfitPost?

  /**
   * Updates one or more fields of a post document in Firestore.
   *
   * @param postId The ID of the post to update.
   * @param updates A map of field-value pairs to update.
   */
  suspend fun updatePostFields(postId: String, updates: Map<String, Any?>)

  /**
   * Deletes both the Firestore post document and its associated image in Storage.
   *
   * @param postId The ID of the post to delete.
   */
  suspend fun deletePost(postId: String)

  /**
   * Uploads the outfit photo as a byte array to Firebase Storage and returns its download URL as a
   * String.
   *
   * @param imageData Byte array of the image data.
   * @param postId The unique ID of the post (used as the filename).
   * @return Download URL of the uploaded image.
   */
  suspend fun uploadOutfitWithCompressedPhoto(imageData: ByteArray, postId: String): String

  /**
   * Uploads a reaction image for a comment as a byte array Firebase Storage and returns its
   * download URL as a String.
   *
   * @param imageData Byte array of the reaction image data.
   * @param commentId The unique ID of the comment (used as the filename).
   * @return Download URL of the uploaded reaction image.
   */
  suspend fun uploadCompressedReactionImage(imageData: ByteArray, commentId: String): String

  /**
   * Adds a comment to a post, optionally uploading a reaction image.
   *
   * @param postId The ID of the post to comment on.
   * @param userId The ID of the user making the comment.
   * @param commentText The text content of the comment.
   * @param reactionImageData Optional byte array of the reaction image data.
   * @return The created Comment object.
   */
  suspend fun addCommentToPost(
      postId: String,
      userId: String,
      commentText: String,
      reactionImageData: ByteArray? = null // Optional - null means no reaction image
  ): Comment

  /**
   * Deletes a comment from a post.
   *
   * @param postId The ID of the post containing the comment.
   * @param commentId The ID of the comment to delete.
   */
  suspend fun deleteCommentFromPost(postId: String, comment: Comment)
}
