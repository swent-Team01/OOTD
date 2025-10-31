package com.android.ootd.ui.camera

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
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withTimeout

/**
 * Repository for handling CameraX operations. Separates camera business logic from UI and
 * ViewModel.
 */
class CameraRepository {

  /**
   * Binds the camera to the lifecycle and returns the Camera instance.
   *
   * @param cameraProvider The ProcessCameraProvider instance
   * @param previewView The PreviewView to display camera preview
   * @param lifecycleOwner The LifecycleOwner to bind the camera to
   * @param lensFacing The camera lens facing (front or back)
   * @return Pair of Camera and ImageCapture instances
   */
  fun bindCamera(
      cameraProvider: ProcessCameraProvider,
      previewView: PreviewView,
      lifecycleOwner: LifecycleOwner,
      lensFacing: Int
  ): Result<Pair<Camera, ImageCapture>> {
    return try {
      val preview =
          Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

      val imageCapture = ImageCapture.Builder().build()

      val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

      cameraProvider.unbindAll()
      val camera =
          cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)

      Result.success(Pair(camera, imageCapture))
    } catch (e: Exception) {
      Log.e("CameraRepository", "Camera binding failed", e)
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
  fun capturePhoto(
      context: Context,
      imageCapture: ImageCapture,
      executor: ExecutorService,
      onSuccess: (Uri) -> Unit,
      onError: (String) -> Unit
  ) {
    val photoFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
          override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val savedUri = Uri.fromFile(photoFile)
            onSuccess(savedUri)
          }

          override fun onError(exception: ImageCaptureException) {
            val errorMsg = "Photo capture failed: ${exception.message}"
            Log.e("CameraRepository", errorMsg, exception)
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
  suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
      withTimeout(10_000) {
        suspendCoroutine { continuation ->
          val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
          cameraProviderFuture.addListener(
              {
                try {
                  continuation.resume(cameraProviderFuture.get())
                } catch (e: Exception) {
                  continuation.resumeWithException(e)
                }
              },
              ContextCompat.getMainExecutor(context))
        }
      }
}
