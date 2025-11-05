package com.android.ootd.model.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withTimeout

/**
 * Implementation of CameraRepository for handling CameraX operations. Separates camera logic from
 * UI and ViewModel.
 */
class CameraRepositoryImplementation : CameraRepository {

  companion object {
    private const val TAG = "CameraRepositoryImpl"
    private const val CAMERA_PROVIDER_TIMEOUT_MS = 10_000L
    private const val IMAGE_FILE_PREFIX = "OOTD_"
    private const val IMAGE_FILE_EXTENSION = ".jpg"
  }

  override fun bindCamera(
      cameraProvider: ProcessCameraProvider,
      previewView: PreviewView,
      lifecycleOwner: LifecycleOwner,
      lensFacing: Int
  ): Result<Pair<Camera, ImageCapture>> {
    return try {
      Log.i(TAG, "Binding camera with lensFacing: $lensFacing")

      val preview =
          Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }

      val imageCapture = ImageCapture.Builder().build()

      val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

      cameraProvider.unbindAll()
      val camera =
          cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)

      Result.success(Pair(camera, imageCapture))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override fun capturePhoto(
      context: Context,
      imageCapture: ImageCapture,
      executor: ExecutorService,
      onSuccess: (Uri) -> Unit,
      onError: (String) -> Unit
  ) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val photoFile = File(context.cacheDir, "$IMAGE_FILE_PREFIX$timestamp$IMAGE_FILE_EXTENSION")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    Log.i(TAG, "Capturing photo to: ${photoFile.absolutePath}")

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
          override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val savedUri = Uri.fromFile(photoFile)
            Log.i(TAG, "Photo captured successfully: $savedUri")
            onSuccess(savedUri)
          }

          override fun onError(exception: ImageCaptureException) {
            val errorMsg = "Photo capture failed: ${exception.message}"
            Log.e(TAG, errorMsg, exception)
            runCatching {
              if (photoFile.exists()) {
                val deleted = photoFile.delete()
                if (!deleted) {
                  Log.w(TAG, "Failed to delete temporary photo file: ${photoFile.absolutePath}")
                }
              }
            }
            onError(errorMsg)
          }
        })
  }

  override suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
      withTimeout(CAMERA_PROVIDER_TIMEOUT_MS) {
        suspendCoroutine { continuation ->
          val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
          cameraProviderFuture.addListener(
              {
                try {
                  val provider = cameraProviderFuture.get()
                  Log.i(TAG, "CameraProvider obtained successfully")
                  continuation.resume(provider)
                } catch (e: Exception) {
                  Log.e(TAG, "Failed to get CameraProvider", e)
                  continuation.resumeWithException(e)
                }
              },
              ContextCompat.getMainExecutor(context))
        }
      }
}
