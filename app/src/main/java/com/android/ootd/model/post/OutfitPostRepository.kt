package com.android.ootd.model.post

import android.net.Uri
import com.android.ootd.model.OutfitPost

/**
 * Repository interface defining all data operations related to user outfit posts.
 *
 * This abstraction allows the app to decouple data source logic (e.g. Firestore, Firebase Storage)
 * from the rest of the app (e.g. ViewModels or use cases).
 */
interface OutfitPostRepository {

  /**
   * Generates a new, unique post ID for a Firestore document.
   *
   * @return a unique string identifier that can be used for a new [OutfitPost].
   *
   * This method does **not** create a document in Firestore — it only reserves an ID to associate
   * with a new post or upload path.
   */
  fun getNewPostId(): String

  /**
   * Uploads an outfit image to Firebase Storage (or another configured storage provider).
   *
   * @param localUri The local [Uri] pointing to the image file on the device.
   * @param postId The ID of the post this photo belongs to, used as the storage filename.
   * @return A [String] containing the public download URL of the uploaded image.
   * @throws Exception if the upload fails or the URL cannot be retrieved.
   */
  suspend fun uploadOutfitPhoto(localUri: Uri, postId: String): String

  /**
   * Adds or updates an [OutfitPost] entry in the backend data source.
   *
   * If a document with the same [OutfitPost.postUID] already exists, it will be overwritten.
   *
   * @param post The [OutfitPost] object containing post metadata and URLs.
   * @throws Exception if the write operation fails.
   */
  suspend fun addPost(post: OutfitPost)

  /**
   * Retrieves an [OutfitPost] by its unique ID.
   *
   * @param postId The ID of the post to fetch.
   * @return The corresponding [OutfitPost] object, or `null` if not found.
   * @throws Exception if the read operation fails.
   */
  suspend fun getPostById(postId: String): OutfitPost?

  /**
   * Deletes a post and its associated image (if any) from the backend.
   *
   * @param postId The ID of the post to delete.
   * @throws Exception if the delete operation fails.
   *
   * Implementations should also remove the corresponding image file from Firebase Storage, if one
   * exists.
   */
  suspend fun deletePost(postId: String)
}
