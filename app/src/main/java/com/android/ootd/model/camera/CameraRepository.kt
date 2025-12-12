package com.android.ootd.model.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService

/**
 * Repository interface for handling CameraX operations. Defines the logic for the custom camera
 * using the cameraX google lib.
 */
interface CameraRepository {

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
  fun bindCamera(
      cameraProvider: ProcessCameraProvider,
      previewView: PreviewView,
      lifecycleOwner: LifecycleOwner,
      lensFacing: Int
  ): Result<Pair<Camera, ImageCapture>>

  /**
   * Captures a photo using the provided ImageCapture instance.
   *
   * @param context The context for creating the output file
   * @param imageCapture The ImageCapture instance
   * @param executor The executor for running the capture operation
   * @param onSuccess Callback when image is successfully captured with the URI
   * @param onError Callback when capture fails with error message
   */
  fun capturePhoto(
      context: Context,
      imageCapture: ImageCapture,
      executor: ExecutorService,
      onSuccess: (Uri) -> Unit,
      onError: (String) -> Unit
  )

  /**
   * Gets the ProcessCameraProvider instance asynchronously.
   *
   * @param context The context for getting the camera provider
   * @return ProcessCameraProvider instance
   * @throws kotlinx.coroutines.TimeoutCancellationException if provider not available within 10
   *   seconds
   */
  suspend fun getCameraProvider(context: Context): ProcessCameraProvider

  /**
   * Cleans up old cached images from the cache directory.
   *
   * @param context The context for accessing the cache directory
   * @param olderThanHours Maximum age of files to keep in hours (default: 24 hours)
   */
  fun cleanupOldCachedImages(context: Context, olderThanHours: Int = 24)

  /**
   * Deletes a specific cached image file.
   *
   * @param context The context for accessing the file system
   * @param imageUri The URI of the image to delete
   * @return true if the file was successfully deleted, false otherwise
   */
  fun deleteCachedImage(context: Context, imageUri: Uri): Boolean

  /**
   * Saves a bitmap to a file in the cache directory.
   *
   * @param context The context for accessing the cache directory
   * @param bitmap The bitmap to save
   * @return The URI of the saved image
   */
  fun saveBitmap(context: Context, bitmap: android.graphics.Bitmap): Uri
}
