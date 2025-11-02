package com.android.ootd.model.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for loading images with correct orientation based on EXIF data. Handles bitmap
 * operations in a memory-efficient way on background threads.
 */
class ImageOrientationHelper {

  companion object {
    private const val TAG = "ImageOrientationHelper"
  }

  /**
   * Loads a bitmap with correct orientation based on EXIF data. Performs all I/O operations on IO
   * dispatcher to avoid blocking the main thread.
   *
   * @param context The context for accessing content resolver
   * @param uri The URI of the image to load
   * @return Result containing the correctly oriented Bitmap on success, or exception on failure
   */
  suspend fun loadBitmapWithCorrectOrientation(context: Context, uri: Uri): Result<Bitmap> =
      withContext(Dispatchers.IO) {
        runCatching {
              Log.d(TAG, "Loading bitmap from URI: $uri")

              // Decode bitmap of the picture
              val bitmap =
                  context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                  }
                      ?: run {
                        Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                        return@runCatching Result.failure<Bitmap>(
                            IllegalStateException("Failed to decode bitmap"))
                      }

              // Read EXIF orientation
              val orientation = readExifOrientation(context, uri)

              // Apply orientation correction if needed
              val correctedBitmap = applyOrientation(bitmap, orientation)

              Log.d(TAG, "Bitmap loaded successfully with orientation: $orientation")
              Result.success(correctedBitmap)
            }
            .getOrElse { exception ->
              Log.e(TAG, "Error loading bitmap", exception)
              Result.failure(exception)
            }
      }

  /**
   * Reads the EXIF orientation from an image. This is used for example to understand if the image
   * was taken in portrait of landscape.
   *
   * @param context The context for accessing content resolver
   * @param uri The URI of the image
   * @return The EXIF orientation value, or ORIENTATION_NORMAL if not found
   */
  private fun readExifOrientation(context: Context, uri: Uri): Int {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
      Log.w(TAG, "Failed to read EXIF orientation, using default", e)
      ExifInterface.ORIENTATION_NORMAL
    }
  }

  /**
   * Applies rotation to a bitmap based on EXIF orientation.
   *
   * @param bitmap The original bitmap
   * @param orientation The EXIF orientation value
   * @return The rotated bitmap, or the original if no rotation is needed. The original bitmap is
   *   recycled if rotation is applied.
   */
  private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val rotationAngle =
        when (orientation) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90f
          ExifInterface.ORIENTATION_ROTATE_180 -> 180f
          ExifInterface.ORIENTATION_ROTATE_270 -> 270f
          else -> return bitmap // No rotation needed
        }

    Log.d(TAG, "Applying rotation: $rotationAngle degrees")

    val matrix = Matrix().apply { postRotate(rotationAngle) }
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    // Recycle the original bitmap to free memory if it's different from the rotated one
    // to prevent mem leak

    if (rotatedBitmap != bitmap) {
      bitmap.recycle()
    }

    return rotatedBitmap
  }
}
