package com.android.ootd.utils

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

/**
 * Utility object for uploading images to Firebase Storage.
 *
 * Provides a centralized way to upload images and retrieve their download URLs.
 */
object ImageUploader {

  private const val TAG = "ImageUploader"
  private const val UNKNOWN_ERROR = "Unknown Error"

  /**
   * Result of an image upload operation.
   *
   * @property success Whether the upload was successful.
   * @property url The download URL of the uploaded image, or the original URL if upload failed.
   * @property error The error message if upload failed, null otherwise.
   */
  data class UploadResult(val success: Boolean, val url: String, val error: String? = null)

  /**
   * Uploads an image to Firebase Storage and returns the download URL.
   *
   * This function handles:
   * - Blank/empty URLs (returns as-is)
   * - Already uploaded Firebase Storage URLs (returns as-is)
   * - Local file URIs (uploads and returns download URL)
   *
   * @param localUri The local URI or path of the image to upload.
   * @param storagePath The path in Firebase Storage where the image should be stored (e.g.,
   *   "profile_pictures/user123.jpg").
   * @param storage Firebase Storage instance (defaults to the default instance).
   * @return UploadResult containing the download URL and success status.
   */
  suspend fun uploadImage(
      localUri: String,
      storagePath: String,
      storage: FirebaseStorage = FirebaseStorage.getInstance()
  ): UploadResult {
    if (localUri.isBlank()) {
      return UploadResult(success = true, url = localUri)
    }

    // If it's already a Firebase Storage URL, return as-is
    if (localUri.startsWith("https://firebasestorage.googleapis.com")) {
      return UploadResult(success = true, url = localUri)
    }

    return try {
      val storageRef = storage.reference
      val fileRef = storageRef.child(storagePath)
      val fileUri = Uri.parse(localUri)

      fileRef.putFile(fileUri).await()
      val downloadUrl = fileRef.downloadUrl.await()

      UploadResult(success = true, url = downloadUrl.toString())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to upload image to $storagePath: ${e.message}", e)
      UploadResult(success = false, url = localUri, error = e.message ?: UNKNOWN_ERROR)
    }
  }

  /**
   * Uploads an image from a byte array to Firebase Storage and returns the download URL.
   *
   * @param imageData The image data as a byte array.
   * @param storagePath The path in Firebase Storage where the image should be stored.
   * @param storage Firebase Storage instance (defaults to the default instance).
   * @return UploadResult containing the download URL and success status.
   */
  suspend fun uploadImageBytes(
      imageData: ByteArray,
      storagePath: String,
      storage: FirebaseStorage = FirebaseStorage.getInstance()
  ): UploadResult {
    if (imageData.isEmpty()) {
      return UploadResult(success = false, url = "", error = "Image data is empty")
    }

    return try {
      val storageRef = storage.reference
      val fileRef = storageRef.child(storagePath)

      fileRef.putBytes(imageData).await()
      val downloadUrl = fileRef.downloadUrl.await()

      UploadResult(success = true, url = downloadUrl.toString())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to upload image bytes to $storagePath: ${e.message}", e)
      UploadResult(success = false, url = "", error = e.message ?: UNKNOWN_ERROR)
    }
  }

  /**
   * Uploads an image with a reference to an existing StorageReference.
   *
   * @param localUri The local URI of the image to upload.
   * @param storageRef The Firebase Storage reference where the image should be stored.
   * @return UploadResult containing the download URL and success status.
   */
  suspend fun uploadImageToReference(localUri: String, storageRef: StorageReference): UploadResult {
    if (localUri.isBlank()) {
      return UploadResult(success = true, url = localUri)
    }

    // If it's already a Firebase Storage URL, return as-is
    if (localUri.startsWith("https://firebasestorage.googleapis.com")) {
      return UploadResult(success = true, url = localUri)
    }

    return try {
      val fileUri = localUri.toUri()

      storageRef.putFile(fileUri).await()
      val downloadUrl = storageRef.downloadUrl.await()

      UploadResult(success = true, url = downloadUrl.toString())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to upload image: ${e.message}", e)
      UploadResult(success = false, url = localUri, error = e.message ?: UNKNOWN_ERROR)
    }
  }

  /**
   * Deletes an image from Firebase Storage.
   *
   * @param storagePath The path to the image in Firebase Storage.
   * @param storage Firebase Storage instance (defaults to the default instance).
   * @return True if deletion was successful or the file doesn't exist, false otherwise.
   */
  suspend fun deleteImage(
      storagePath: String,
      storage: FirebaseStorage = FirebaseStorage.getInstance()
  ): Boolean {
    if (storagePath.isBlank()) return true

    return try {
      val storageRef = storage.reference
      val fileRef = storageRef.child(storagePath)
      fileRef.delete().await()
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete image at $storagePath: ${e.message}", e)
      false
    }
  }
}
