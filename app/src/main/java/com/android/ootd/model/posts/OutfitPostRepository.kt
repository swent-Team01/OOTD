package com.android.ootd.model.post

import com.android.ootd.model.posts.OutfitPost

interface OutfitPostRepository {

  /**
   * Generates a new unique ID for a post. Used for both the Firestore document and the Storage
   * image filename.
   */
  fun getNewPostId(): String

  /**
   * Uploads the outfit photo to Firebase Storage and returns its download URL as a String.
   *
   * @param localPath Local file path or URI string of the image.
   * @param postId The unique ID of the post (used as the filename).
   * @return Download URL of the uploaded image.
   */
  suspend fun uploadOutfitPhoto(localPath: String, postId: String): String

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
   * Creates a new post in Firestore with only the photo and description fields filled. Uploads the
   * outfit photo to Firebase Storage and saves the metadata document.
   *
   * Returns the created [OutfitPost] object (including its Firestore ID and photo URL).
   */
  suspend fun savePostWithMainPhoto(
      uid: String,
      name: String,
      userProfilePicURL: String,
      localPath: String,
      description: String
  ): OutfitPost

  /**
   * Deletes both the Firestore post document and its associated image in Storage.
   *
   * @param postId The ID of the post to delete.
   */
  suspend fun deletePost(postId: String)
}
