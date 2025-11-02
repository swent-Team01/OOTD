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
class CameraRepositoryImpl : CameraRepository {

  companion object {
    private const val TAG = "CameraRepositoryImpl"
    private const val CAMERA_PROVIDER_TIMEOUT_MS = 10_000L
    private const val IMAGE_FILE_PREFIX = "OOTD_"
    private const val IMAGE_FILE_EXTENSION = ".jpg"
  }

  /**
   * Binds the camera to the lifecycle and returns the Camera instance.
   *
   * @param cameraProvider The ProcessCameraProvider instance
   * @param previewView The PreviewView to display camera preview
   * @param lifecycleOwner The LifecycleOwner to bind the camera to
   * @param lensFacing The camera lens facing (front or back)
   * @return Result containing Pair of Camera and ImageCapture instances on success, or exception on
   *   failure
   */
  override fun bindCamera(
      cameraProvider: ProcessCameraProvider,
      previewView: PreviewView,
      lifecycleOwner: LifecycleOwner,
      lensFacing: Int
  ): Result<Pair<Camera, ImageCapture>> {
    return try {
      Log.d(TAG, "Binding camera with lensFacing: $lensFacing")

      val preview =
          Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

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

  /**
   * Captures a photo using the provided ImageCapture instance.
   *
   * @param context The context for creating the output file
   * @param imageCapture The ImageCapture instance
   * @param executor The executor for running the capture operation
   * @param onSuccess Callback when image is successfully captured
   * @param onError Callback when capture fails
   */
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

    Log.d(TAG, "Capturing photo to: ${photoFile.absolutePath}")

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
          override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val savedUri = Uri.fromFile(photoFile)
            Log.d(TAG, "Photo captured successfully: $savedUri")
            onSuccess(savedUri)
          }

          override fun onError(exception: ImageCaptureException) {
            val errorMsg = "Photo capture failed: ${exception.message}"
            Log.e(TAG, errorMsg, exception)
            onError(errorMsg)
          }
        })
  }

  /**
   * Gets the ProcessCameraProvider instance asynchronously.
   *
   * @param context The context for getting the camera provider
   * @return ProcessCameraProvider instance
   * @throws kotlinx.coroutines.TimeoutCancellationException if provider not available within 10
   *   seconds
   */
  override suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
      withTimeout(CAMERA_PROVIDER_TIMEOUT_MS) {
        suspendCoroutine { continuation ->
          val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
          cameraProviderFuture.addListener(
              {
                try {
                  val provider = cameraProviderFuture.get()
                  Log.d(TAG, "CameraProvider obtained successfully")
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
