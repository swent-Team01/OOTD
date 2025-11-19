package com.android.ootd.model.items

import android.net.Uri
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

/**
 * Object responsible for uploading images to Firebase Storage and return [ImageData]
 *
 * Keeps upload logic separate from ViewModels and Repositories.
 */
object FirebaseImageUploader {

  private const val TAG = "FirebaseImageUploader"

  // Timeout duration for upload operations to Firebase Storage
  private const val UPLOAD_TIMEOUT_MS = 10_000L // 10 seconds

  private val storage by lazy {
    try {
      Firebase.storage.reference
    } catch (_: IllegalStateException) {
      Log.w(TAG, "Firebase not initialized, using dummy reference.")
      null
    }
  }

  /**
   * Uploads an image to Firebase Storage and returns the corresponding [ImageData].
   *
   * **Offline Mode Handling:**
   * - When offline, returns the local URI as imageUrl so the item can be created
   * - The image is stored locally on the device
   * - When back online, the image upload should be retried (future enhancement)
   *
   * @param localUri The local URI of the image to upload.
   * @param fileName The desired file name (without extension) for the uploaded image.
   * @param imageData The image data as a ByteArray.
   * @return An [ImageData] object containing the image ID and download URL (or local URI if
   *   offline).
   */
  suspend fun uploadImage(imageData: ByteArray, fileName: String, localUri: Uri): ImageData {
    val ref = storage ?: return fallbackImageData(localUri, fileName)

    return try {
      // Add timeout to prevent indefinite hanging when offline
      kotlinx.coroutines.withTimeout(UPLOAD_TIMEOUT_MS) {
        val sanitizedFileName = ImageFilenameSanitizer.sanitize(fileName)
        val imageRef = ref.child("images/items/$sanitizedFileName.jpg")
        imageRef.putBytes(imageData).await()
        val downloadUrl = imageRef.downloadUrl.await()
        ImageData(imageId = fileName, imageUrl = downloadUrl.toString())
      }
    } catch (e: Exception) {
      Log.w(TAG, "Image upload failed (likely offline or invalid): ${e.message}")
      fallbackImageData(localUri, fileName)
    }
  }

  private fun fallbackImageData(localUri: Uri, fileName: String): ImageData {
    // If local file exists, keep offline URI; else return empty to signal invalid selection
    return try {
      val isFile = localUri.scheme == "file"
      val path = localUri.path
      val fileExists = isFile && path != null && java.io.File(path).exists()
      if (fileExists) ImageData(imageId = fileName, imageUrl = localUri.toString())
      else ImageData("", "")
    } catch (_: Exception) {
      ImageData("", "")
    }
  }

  suspend fun deleteImage(imageId: String): Boolean {
    if (imageId.isEmpty()) return false
    val ref = storage ?: return true
    return try {
      val sanitizedImageId = ImageFilenameSanitizer.sanitize(imageId)
      val imageRef = ref.child("images/items/$sanitizedImageId.jpg")
      imageRef.delete().await()
      true
    } catch (e: Exception) {
      // If the image does not exist, consider it a successful deletion because if the object is
      // already missing, that post-condition is still true
      if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
        Log.w(TAG, "Image not found for deletion: $imageId")
        true
      } else {
        Log.e(TAG, "Image deletion failed", e)
        false
      }
    }
  }
}
