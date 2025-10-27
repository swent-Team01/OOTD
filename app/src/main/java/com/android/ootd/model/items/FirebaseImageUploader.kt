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

  private val storage by lazy {
    try {
      Firebase.storage.reference
    } catch (e: IllegalStateException) {
      // This happens in unit tests (Firebase not initialized)
      Log.w("FirebaseImageUploader", "Firebase not initialized, using dummy reference.")
      null
    }
  }

  /**
   * Uploads an image to Firebase Storage and returns the corresponding [ImageData].
   *
   * @param localUri The local URI of the image to upload.
   * @param fileName The desired file name (without extension) for the uploaded image.
   * @return An [ImageData] object containing the image ID and download URL.
   */
  suspend fun uploadImage(localUri: Uri, fileName: String): ImageData {
    val ref = storage ?: return ImageData("", "")

    return try {
      val sanitizedFileName = refactorFileName(fileName)
      val imageRef = ref.child("images/$sanitizedFileName.jpg")
      imageRef.putFile(localUri).await()
      val downloadUrl = imageRef.downloadUrl.await()
      ImageData(imageId = fileName, imageUrl = downloadUrl.toString())
    } catch (e: Exception) {
      Log.e("FirebaseImageUploader", "Image upload failed", e)
      ImageData("", "")
    }
  }

  suspend fun deleteImage(imageId: String): Boolean {
    if (imageId.isEmpty()) return false
    val ref = storage ?: return true
    return try {
      val sanitizedImageId = refactorFileName(imageId)
      val imageRef = ref.child("images/$sanitizedImageId.jpg")
      imageRef.delete().await()
      true
    } catch (e: Exception) {
      // If the image does not exist, consider it a successful deletion because if the object is
      // already missing, that post-condition is still true
      if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
        Log.w("FirebaseImageUploader", "Image not found for deletion: $imageId")
        true
      } else {
        Log.e("FirebaseImageUploader", "Image deletion failed", e)
        false
      }
    }
  }

  private fun refactorFileName(original: String): String {
    return original.trim().replace("\\s+".toRegex(), "_").replace("[^A-Za-z0-9_.-]".toRegex(), "_")
  }
}
