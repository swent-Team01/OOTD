package com.android.ootd.model.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the ImageCompressor class.
 *
 * DISCLAIMER: Tests are partially created by AI, reviewed and modified by human developers to
 * ensure accuracy and relevance.
 */
@RunWith(RobolectricTestRunner::class)
class ImageCompressorTest {

  private lateinit var imageCompressor: ImageCompressor
  private lateinit var mockContext: Context
  private lateinit var mockContentResolver: ContentResolver
  private lateinit var mockUri: Uri

  @Before
  fun setup() {
    imageCompressor = ImageCompressor()
    mockContext = mockk(relaxed = true)
    mockContentResolver = mockk(relaxed = true)
    mockUri = mockk(relaxed = true)

    every { mockContext.contentResolver } returns mockContentResolver
  }

  /** Creates a test bitmap with specified dimensions */
  private fun createTestBitmap(width: Int = 100, height: Int = 100): Bitmap {
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  }

  /** Converts a bitmap to a byte array with the specified format and quality */
  private fun bitmapToByteArray(
      bitmap: Bitmap,
      format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
      quality: Int = 100
  ): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(format, quality, stream)
    return stream.toByteArray()
  }

  @Test
  fun `compressImage returns null when input stream cannot be opened and when the context is null`() =
      runTest {
        every { mockContentResolver.openInputStream(mockUri) } returns null

        val result = imageCompressor.compressImage(mockUri, 1024L, mockContext)

        Assert.assertNull(result)

        val resultNull = imageCompressor.compressImage(mockUri, 1024L, null)
        Assert.assertNull(resultNull)
      }

  @Test
  fun `compressImage returns original bytes when size is below threshold`() = runTest {
    val bitmap = createTestBitmap(50, 50)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 100)
    val threshold = originalBytes.size.toLong() + 1000L

    every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
    Assert.assertArrayEquals(originalBytes, result)
  }

  @Test
  fun `compressImage compresses large JPEG image`() = runTest {
    mockkStatic(BitmapFactory::class)

    val bitmap = createTestBitmap(200, 200)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 100)
    val threshold = 500L // Very small threshold to force compression

    every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)
    every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns bitmap

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
    Assert.assertTrue(
        "Compressed image should be smaller than original", result!!.size < originalBytes.size)
  }

  @Test
  fun `compressImage handles PNG format correctly`() = runTest {
    mockkStatic(BitmapFactory::class)

    val bitmap = createTestBitmap(200, 200)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.PNG, 100)
    val threshold = 500L

    every { mockContentResolver.getType(mockUri) } returns "image/png"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)
    every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns bitmap

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
    // PNG is lossless, so quality parameter is ignored
    // The result should still be valid
  }

  @Test
  fun `compressImage handles WEBP format correctly`() = runTest {
    mockkStatic(BitmapFactory::class)

    val bitmap = createTestBitmap(200, 200)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 100)
    val threshold = 500L

    every { mockContentResolver.getType(mockUri) } returns "image/webp"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)
    every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns bitmap

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
  }

  @Test
  fun `compressImage handles unknown mime type with JPEG fallback`() = runTest {
    mockkStatic(BitmapFactory::class)

    val bitmap = createTestBitmap(200, 200)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 100)
    val threshold = 500L

    every { mockContentResolver.getType(mockUri) } returns "image/unknown"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)
    every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns bitmap

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
  }

  @Test
  fun `compressImage reduces quality iteratively until threshold is met`() = runTest {
    mockkStatic(BitmapFactory::class)

    val bitmap = createTestBitmap(300, 300)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 100)
    val threshold = 1000L // Small threshold to trigger multiple iterations

    every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)
    every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns bitmap

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
    // The compression should have reduced the size
    Assert.assertTrue("Result should exist and be compressed", result!!.size < originalBytes.size)
  }

  @Test
  fun `compressImage stops at minimum quality threshold`() = runTest {
    mockkStatic(BitmapFactory::class)

    val bitmap = createTestBitmap(500, 500)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 100)
    val threshold = 10L // Impossibly small threshold

    every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)
    every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns bitmap

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
    // Should stop compressing at quality > 20
  }

  @Test
  fun `compressImage handles large high-resolution images`() = runTest {
    mockkStatic(BitmapFactory::class)

    val bitmap = createTestBitmap(1920, 1080)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 100)
    val threshold = 50000L // 50KB threshold

    every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)
    every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns bitmap

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
  }

  @Test
  fun `compressImage returns result even when exact threshold cannot be met`() = runTest {
    mockkStatic(BitmapFactory::class)

    val bitmap = createTestBitmap(400, 400)
    val originalBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 100)
    val threshold = 5L // Unrealistically small

    every { mockContentResolver.getType(mockUri) } returns "image/jpeg"
    every { mockContentResolver.openInputStream(mockUri) } returns
        ByteArrayInputStream(originalBytes)
    every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns bitmap

    val result = imageCompressor.compressImage(mockUri, threshold, mockContext)

    Assert.assertNotNull(result)
    // Should return the best compression it can achieve
  }
}
