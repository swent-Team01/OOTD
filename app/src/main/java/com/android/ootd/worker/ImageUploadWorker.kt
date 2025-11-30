package com.android.ootd.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ITEMS_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ImageUploadWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {
    val itemUuid = inputData.getString("itemUuid") ?: return Result.failure()
    val imageUriString = inputData.getString("imageUri") ?: return Result.failure()
    val fileName = inputData.getString("fileName") ?: return Result.failure()

    return try {
      val contentResolver = applicationContext.contentResolver
      val imageUri = Uri.parse(imageUriString)

      val inputStream = contentResolver.openInputStream(imageUri)
      if (inputStream == null) {
        return Result.failure()
      }

      val imageData = inputStream.use { it.readBytes() }

      // Upload image
      val uploadedImage = FirebaseImageUploader.uploadImageSuspending(imageData, fileName)

      // Update Firestore
      val db = FirebaseFirestore.getInstance()
      db.collection(ITEMS_COLLECTION).document(itemUuid).update("image", uploadedImage).await()

      Result.success()
    } catch (e: Exception) {
      if (runAttemptCount < 3) {
        Result.retry()
      } else {
        Result.failure()
      }
    }
  }
}
