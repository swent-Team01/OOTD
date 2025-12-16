package com.android.ootd.model.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * ImageCompressor provides functionality to compress images from a given URI.
 *
 * The compression is performed based on a specified size threshold. If the image size exceeds the
 * threshold, it will be compressed iteratively by reducing quality until it meets the size
 * requirement or reaches a minimum quality level. This compressor also handles image rotation based
 * on EXIF data to ensure correct orientation.
 *
 * This compressor is implemented based on https://www.youtube.com/watch?v=Q0Njj-rfEXE
 *
 * @param dispatcherProvider Provides CoroutineDispatchers for various threading needs.
 */
class ImageCompressor(
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) {

  /**
   * Compresses an image from the provided [contentUri] if its size exceeds the
   * [compressionThreshold].
   *
   * @param contentUri The URI of the image to be compressed.
   * @param compressionThreshold The size threshold in bytes. Images larger than this will be
   *   compressed.
   * @param context The context used to access content resolver.
   * @return A [ByteArray] of the compressed image, or null if the image could not be read.
   */
  suspend fun compressImage(
      contentUri: Uri,
      compressionThreshold: Long,
      context: Context?
  ): ByteArray? =
      withContext(dispatcherProvider.io) {

        // Get the MIME type of the image
        val mimeType = context?.contentResolver?.getType(contentUri)

        // Read the image bytes from the content URI
        val inputBytes =
            context?.contentResolver?.openInputStream(contentUri)?.use { inputStream ->
              inputStream.readBytes()
            } ?: return@withContext null

        if (inputBytes.size <= compressionThreshold) {
          // No need to compress
          return@withContext inputBytes
        }

        // Read EXIF orientation
        val exif =
            context.contentResolver.openInputStream(contentUri)?.use {
              androidx.exifinterface.media.ExifInterface(it)
            }

        // Get rotation degrees from EXIF data
        val rotationDegrees = exif?.rotationDegrees ?: 0

        // Decode the image bytes into a Bitmap
        val bitmap =
            BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size) ?: return@withContext null
        // Rotate the bitmap if necessary
        val rotatedBitmap =
            if (rotationDegrees != 0) {
              val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
              val rotated =
                  Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
              // Recycle the original bitmap to free memory - it's no longer needed
              bitmap.recycle()
              rotated
            } else {
              bitmap
            }

        ensureActive()

        withContext(dispatcherProvider.default) {
          val compressFormat =
              when (mimeType) {
                "image/png" -> Bitmap.CompressFormat.PNG // lossless == quality is ignored
                "image/jpeg" -> Bitmap.CompressFormat.JPEG
                "image/webp" ->
                    // Use WEBP_LOSSLESS for API 30+, otherwise use WEBP
                    if (Build.VERSION.SDK_INT >= 30) {
                      Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else Bitmap.CompressFormat.WEBP

                else -> Bitmap.CompressFormat.JPEG
              }

          var outputBytes: ByteArray
          var quality = 90
          do {
            ByteArrayOutputStream().use { outputStream ->
              rotatedBitmap.compress(compressFormat, quality, outputStream)
              outputBytes = outputStream.toByteArray()
              quality -= (quality * 0.1).roundToInt()
            }
          } while (isActive &&
              outputBytes.size > compressionThreshold &&
              quality > 20 &&
              compressFormat != Bitmap.CompressFormat.PNG)

          outputBytes
        }
      }
}
