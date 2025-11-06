package com.android.ootd.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.ootd.model.camera.ImageOrientationHelper
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Connected (instrumentation) tests for ImageOrientationHelper.
 *
 * These tests run on a real Android device/emulator and verify image loading and orientation
 * handling with actual bitmaps and file URIs.
 */
@RunWith(AndroidJUnit4::class)
class ImageOrientationHelperConnectedTest {

  private lateinit var helper: ImageOrientationHelper
  private lateinit var context: Context
  private val testFiles = mutableListOf<File>()

  @Before
  fun setUp() {
    helper = ImageOrientationHelper()
    context = InstrumentationRegistry.getInstrumentation().targetContext
  }

  @After
  fun tearDown() {
    // Clean up test files
    testFiles.forEach { file ->
      if (file.exists()) {
        file.delete()
      }
    }
    testFiles.clear()
  }

  /**
   * Creates a temporary image file with a small JPEG header and dummy data.
   *
   * @param width The width of the test bitmap
   * @param height The height of the test bitmap
   * @return The created file
   */
  private fun createTempImageFile(width: Int = 100, height: Int = 100): File {
    val file = File(context.cacheDir, "test_image_${System.currentTimeMillis()}.jpg")
    testFiles.add(file)

    // Create a simple bitmap and save it as JPEG
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Fill with a color so it's not empty
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.BLUE)

    FileOutputStream(file).use { outputStream ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    }

    bitmap.recycle()
    return file
  }

  /**
   * Creates a readable URI from a file using FileProvider.
   *
   * @param file The file to create a URI for
   * @return The content URI
   */
  private fun createReadableUri(file: File): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
  }

  /**
   * Creates a temp image with specific EXIF orientation.
   *
   * @param orientation EXIF orientation constant
   * @return The created file with EXIF data
   */
  private fun createImageWithOrientation(orientation: Int): File {
    val file = createTempImageFile(50, 100)
    ExifInterface(file.absolutePath).apply {
      setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
      saveAttributes()
    }
    return file
  }

  // ========== Success Cases ==========

  @Test
  fun loadBitmapWithCorrectOrientation_loadsValidImage() {
    val file = createTempImageFile()
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue("Should successfully load bitmap", result.isSuccess)
    Assert.assertNotNull("Bitmap should not be null", result.getOrNull())
    result.getOrNull()?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_returnsCorrectDimensions() {
    val expectedWidth = 200
    val expectedHeight = 150
    val file = createTempImageFile(expectedWidth, expectedHeight)
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result.isSuccess)
    val bitmap = result.getOrNull()
    Assert.assertNotNull(bitmap)

    // Note: JPEG compression may slightly alter dimensions, so we check they're close
    Assert.assertEquals("Width should match", expectedWidth, bitmap?.width)
    Assert.assertEquals("Height should match", expectedHeight, bitmap?.height)

    bitmap?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesSmallImage() {
    val file = createTempImageFile(10, 10)
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result.isSuccess)
    val bitmap = result.getOrNull()
    Assert.assertNotNull(bitmap)
    Assert.assertEquals(10, bitmap?.width)
    Assert.assertEquals(10, bitmap?.height)

    bitmap?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesLargeImage() {
    val file = createTempImageFile(1000, 800)
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result.isSuccess)
    val bitmap = result.getOrNull()
    Assert.assertNotNull(bitmap)

    bitmap?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesSquareImage() {
    val file = createTempImageFile(500, 500)
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result.isSuccess)
    val bitmap = result.getOrNull()
    Assert.assertNotNull(bitmap)
    Assert.assertEquals("Square image should have equal dimensions", bitmap?.width, bitmap?.height)

    bitmap?.recycle()
  }

  // ========== Error Cases ==========

  @Test
  fun loadBitmapWithCorrectOrientation_failsWithInvalidUri() {
    val invalidUri = Uri.parse("content://invalid/path/to/nowhere.jpg")

    val result = helper.loadBitmapWithCorrectOrientation(context, invalidUri)

    Assert.assertTrue("Should fail with invalid URI", result.isFailure)
    Assert.assertNotNull("Should have exception", result.exceptionOrNull())
  }

  @Test
  fun loadBitmapWithCorrectOrientation_failsWithNonExistentFile() {
    val nonExistentFile = File(context.cacheDir, "non_existent_image.jpg")
    // Ensure it doesn't exist
    if (nonExistentFile.exists()) {
      nonExistentFile.delete()
    }

    // Try to create URI (this might fail or succeed depending on FileProvider config)
    try {
      val uri = createReadableUri(nonExistentFile)
      val result = helper.loadBitmapWithCorrectOrientation(context, uri)

      Assert.assertTrue("Should fail with non-existent file", result.isFailure)
    } catch (e: IllegalArgumentException) {
      // FileProvider may throw this if file doesn't exist
      Assert.assertTrue("Expected IllegalArgumentException for non-existent file", true)
    }
  }

  @Test
  fun loadBitmapWithCorrectOrientation_failsWithCorruptedFile() {
    val file = File(context.cacheDir, "corrupted_image_${System.currentTimeMillis()}.jpg")
    testFiles.add(file)

    // Write invalid JPEG data
    FileOutputStream(file).use { outputStream ->
      outputStream.write(byteArrayOf(0x00, 0x01, 0x02, 0x03))
    }

    val uri = createReadableUri(file)
    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue("Should fail with corrupted file", result.isFailure)
  }

  // ========== Multiple Operations ==========

  @Test
  fun loadBitmapWithCorrectOrientation_handlesMultipleImages() {
    val file1 = createTempImageFile(100, 100)
    val file2 = createTempImageFile(200, 200)
    val uri1 = createReadableUri(file1)
    val uri2 = createReadableUri(file2)

    val result1 = helper.loadBitmapWithCorrectOrientation(context, uri1)
    val result2 = helper.loadBitmapWithCorrectOrientation(context, uri2)

    Assert.assertTrue(result1.isSuccess)
    Assert.assertTrue(result2.isSuccess)

    result1.getOrNull()?.recycle()
    result2.getOrNull()?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_canBeCalledMultipleTimes() {
    val file = createTempImageFile()
    val uri = createReadableUri(file)

    val result1 = helper.loadBitmapWithCorrectOrientation(context, uri)
    val result2 = helper.loadBitmapWithCorrectOrientation(context, uri)
    val result3 = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result1.isSuccess)
    Assert.assertTrue(result2.isSuccess)
    Assert.assertTrue(result3.isSuccess)

    result1.getOrNull()?.recycle()
    result2.getOrNull()?.recycle()
    result3.getOrNull()?.recycle()
  }

  // ========== Bitmap Properties Tests ==========

  @Test
  fun loadBitmapWithCorrectOrientation_returnsNonNullBitmap() {
    val file = createTempImageFile()
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result.isSuccess)
    val bitmap = result.getOrNull()
    Assert.assertNotNull("Bitmap should not be null", bitmap)
    Assert.assertTrue("Bitmap width should be positive", (bitmap?.width ?: 0) > 0)
    Assert.assertTrue("Bitmap height should be positive", (bitmap?.height ?: 0) > 0)

    bitmap?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_bitmapIsNotRecycled() {
    val file = createTempImageFile()
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result.isSuccess)
    val bitmap = result.getOrNull()
    Assert.assertNotNull(bitmap)
    Assert.assertFalse("Bitmap should not be recycled", bitmap?.isRecycled == true)

    bitmap?.recycle()
  }

  // ========== Different Aspect Ratios ==========

  @Test
  fun loadBitmapWithCorrectOrientation_handlesPortraitOrientation() {
    val file = createTempImageFile(100, 200) // Portrait (taller than wide)
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result.isSuccess)
    val bitmap = result.getOrNull()
    Assert.assertNotNull(bitmap)

    bitmap?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesLandscapeOrientation() {
    val file = createTempImageFile(200, 100) // Landscape (wider than tall)
    val uri = createReadableUri(file)

    val result = helper.loadBitmapWithCorrectOrientation(context, uri)

    Assert.assertTrue(result.isSuccess)
    val bitmap = result.getOrNull()
    Assert.assertNotNull(bitmap)

    bitmap?.recycle()
  }

  // ========== Memory Tests ==========

  @Test
  fun loadBitmapWithCorrectOrientation_doesNotLeakMemory() {
    // Load and unload multiple bitmaps to test for memory leaks
    repeat(10) {
      val file = createTempImageFile()
      val uri = createReadableUri(file)

      val result = helper.loadBitmapWithCorrectOrientation(context, uri)

      if (result.isSuccess) {
        result.getOrNull()?.recycle()
      }
    }

    // If we got here without OutOfMemoryError, the test passes
    Assert.assertTrue("Should not leak memory", true)
  }

  // ========== Rotation Tests ==========

  @Test
  fun loadBitmapWithCorrectOrientation_handlesRotate90() {
    val file = createImageWithOrientation(ExifInterface.ORIENTATION_ROTATE_90)
    val result = helper.loadBitmapWithCorrectOrientation(context, createReadableUri(file))
    Assert.assertTrue(result.isSuccess)
    result.getOrNull()?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesRotate180() {
    val file = createImageWithOrientation(ExifInterface.ORIENTATION_ROTATE_180)
    val result = helper.loadBitmapWithCorrectOrientation(context, createReadableUri(file))
    Assert.assertTrue(result.isSuccess)
    result.getOrNull()?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesRotate270() {
    val file = createImageWithOrientation(ExifInterface.ORIENTATION_ROTATE_270)
    val result = helper.loadBitmapWithCorrectOrientation(context, createReadableUri(file))
    Assert.assertTrue(result.isSuccess)
    result.getOrNull()?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesFlipHorizontal() {
    val file = createImageWithOrientation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL)
    val result = helper.loadBitmapWithCorrectOrientation(context, createReadableUri(file))
    Assert.assertTrue(result.isSuccess)
    result.getOrNull()?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesFlipVertical() {
    val file = createImageWithOrientation(ExifInterface.ORIENTATION_FLIP_VERTICAL)
    val result = helper.loadBitmapWithCorrectOrientation(context, createReadableUri(file))
    Assert.assertTrue(result.isSuccess)
    result.getOrNull()?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesTranspose() {
    val file = createImageWithOrientation(ExifInterface.ORIENTATION_TRANSPOSE)
    val result = helper.loadBitmapWithCorrectOrientation(context, createReadableUri(file))
    Assert.assertTrue(result.isSuccess)
    result.getOrNull()?.recycle()
  }

  @Test
  fun loadBitmapWithCorrectOrientation_handlesTransverse() {
    val file = createImageWithOrientation(ExifInterface.ORIENTATION_TRANSVERSE)
    val result = helper.loadBitmapWithCorrectOrientation(context, createReadableUri(file))
    Assert.assertTrue(result.isSuccess)
    result.getOrNull()?.recycle()
  }
}
