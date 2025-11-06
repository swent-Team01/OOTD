package com.android.ootd.model.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

/**
 * Helper class for loading images with correct orientation. Handles EXIF orientation data to ensure
 * images are displayed properly.
 */
class ImageOrientationHelper {

  /**
   * Loads a bitmap from a URI with the correct orientation based on EXIF data.
   *
   * @param context The Android context
   * @param imageUri The URI of the image to load
   * @return Result containing the correctly oriented bitmap, or an error
   */
  fun loadBitmapWithCorrectOrientation(context: Context, imageUri: Uri): Result<Bitmap> {
    return try {
      // Load the bitmap
      val inputStream =
          context.contentResolver.openInputStream(imageUri)
              ?: return Result.failure(IOException("Failed to open input stream for $imageUri"))

      val bitmap = BitmapFactory.decodeStream(inputStream)
      inputStream.close()

      if (bitmap == null) {
        return Result.failure(IOException("Failed to decode bitmap from $imageUri"))
      }

      // Get the orientation from EXIF data
      val orientation = getOrientation(context, imageUri)

      // Rotate the bitmap if needed
      val rotatedBitmap = rotateBitmap(bitmap, orientation)

      Result.success(rotatedBitmap)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Gets the orientation of an image from its EXIF data.
   *
   * @param context The Android context
   * @param imageUri The URI of the image
   * @return The orientation value from ExifInterface
   */
  private fun getOrientation(context: Context, imageUri: Uri): Int {
    return try {
      val inputStream =
          context.contentResolver.openInputStream(imageUri)
              ?: return ExifInterface.ORIENTATION_NORMAL

      val exif = ExifInterface(inputStream)
      inputStream.close()

      val orientation =
          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

      // Map ORIENTATION_UNDEFINED to ORIENTATION_NORMAL for proper handling
      if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
        ExifInterface.ORIENTATION_NORMAL
      } else {
        orientation
      }
    } catch (_: IOException) {
      ExifInterface.ORIENTATION_NORMAL
    }
  }

  /**
   * Rotates a bitmap based on EXIF orientation.
   *
   * @param bitmap The bitmap to rotate
   * @param orientation The EXIF orientation value
   * @return The rotated bitmap (or original if no rotation needed)
   */
  private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()

    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.postRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.postRotate(-90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_UNDEFINED,
      ExifInterface.ORIENTATION_NORMAL -> return bitmap // No rotation needed
      else -> return bitmap // No rotation needed for unknown orientations
    }

    return try {
      val rotatedBitmap =
          Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

      // Recycle the original bitmap if a new one was created
      if (rotatedBitmap != bitmap) {
        bitmap.recycle()
      }

      rotatedBitmap
    } catch (_: OutOfMemoryError) {
      // If we run out of memory, return the original bitmap
      bitmap
    }
  }
}
