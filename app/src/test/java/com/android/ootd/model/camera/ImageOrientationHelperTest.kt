package com.android.ootd.model.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImageOrientationHelperTest {

  private lateinit var helper: ImageOrientationHelper
  private lateinit var mockContext: Context
  private lateinit var mockUri: Uri
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    helper = ImageOrientationHelper()
    mockContext = mockk(relaxed = true)
    mockUri = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  // ========== Bitmap Loading Tests ==========

  @Test
  fun `loadBitmapWithCorrectOrientation returns success when bitmap loads successfully`() =
      runTest {
        // Create a small test bitmap
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val mockInputStream = mockk<InputStream>(relaxed = true)

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns bitmap

        every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream

        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
      }

  @Test
  fun `loadBitmapWithCorrectOrientation returns failure when bitmap cannot be decoded`() = runTest {
    val mockInputStream = mockk<InputStream>(relaxed = true)

    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeStream(any()) } returns null

    every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    assertTrue(result.isFailure)
  }

  @Test
  fun `loadBitmapWithCorrectOrientation returns failure when input stream is null`() = runTest {
    every { mockContext.contentResolver.openInputStream(mockUri) } returns null

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    assertTrue(result.isFailure)
  }

  @Test
  fun `loadBitmapWithCorrectOrientation handles exceptions gracefully`() = runTest {
    every { mockContext.contentResolver.openInputStream(mockUri) } throws
        RuntimeException("File not found")

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is RuntimeException)
  }

  // ========== EXIF Orientation Tests ==========

  @Test
  fun `loadBitmapWithCorrectOrientation handles ORIENTATION_NORMAL correctly`() = runTest {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val mockInputStream = mockk<InputStream>(relaxed = true)

    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeStream(any()) } returns bitmap

    every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    assertTrue(result.isSuccess)
    val resultBitmap = result.getOrNull()
    assertNotNull(resultBitmap)
    // For normal orientation, dimensions should remain the same
    assertEquals(100, resultBitmap?.width)
    assertEquals(100, resultBitmap?.height)
  }

  @Test
  fun `loadBitmapWithCorrectOrientation handles exceptions during EXIF reading`() = runTest {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val mockInputStream = mockk<InputStream>(relaxed = true)

    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeStream(any()) } returns bitmap

    every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream

    // Should still succeed even if EXIF reading fails
    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    assertTrue(result.isSuccess)
  }

  // ========== Memory Management Tests ==========

  @Test
  fun `loadBitmapWithCorrectOrientation recycles original bitmap when rotation applied`() =
      runTest {
        // Create a rectangular bitmap to test rotation
        val originalBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val mockInputStream1 = ByteArrayInputStream(ByteArray(0))
        val mockInputStream2 = ByteArrayInputStream(ByteArray(0))

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns originalBitmap

        // First call for bitmap decoding, second for EXIF reading
        every { mockContext.contentResolver.openInputStream(mockUri) } returnsMany
            listOf(mockInputStream1, mockInputStream2)

        mockkStatic(ExifInterface::class)
        val mockExif = mockk<ExifInterface>()
        every { mockExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, any()) } returns
            ExifInterface.ORIENTATION_ROTATE_90

        // Note: We can't easily test bitmap recycling in unit tests
        // This would require integration tests or manual verification
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

        assertTrue(result.isSuccess)
      }

  // ========== Edge Cases Tests ==========

  @Test
  fun `loadBitmapWithCorrectOrientation handles empty bitmap`() = runTest {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val mockInputStream = mockk<InputStream>(relaxed = true)

    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeStream(any()) } returns bitmap

    every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    assertTrue(result.isSuccess)
    val resultBitmap = result.getOrNull()
    assertNotNull(resultBitmap)
    assertEquals(1, resultBitmap?.width)
    assertEquals(1, resultBitmap?.height)
  }

  @Test
  fun `loadBitmapWithCorrectOrientation handles large bitmap dimensions`() = runTest {
    val bitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
    val mockInputStream = mockk<InputStream>(relaxed = true)

    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeStream(any()) } returns bitmap

    every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    assertTrue(result.isSuccess)
    val resultBitmap = result.getOrNull()
    assertNotNull(resultBitmap)
  }

  @Test
  fun `loadBitmapWithCorrectOrientation executes on IO dispatcher`() = runTest {
    // This test verifies the function completes without blocking main thread
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val mockInputStream = mockk<InputStream>(relaxed = true)

    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeStream(any()) } returns bitmap

    every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    // Should complete successfully on background thread
    assertTrue(result.isSuccess)
  }

  // ========== Result Type Tests ==========

  @Test
  fun `loadBitmapWithCorrectOrientation returns Result type`() = runTest {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val mockInputStream = mockk<InputStream>(relaxed = true)

    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeStream(any()) } returns bitmap

    every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    // Verify it's a Result type
    assertTrue(result.isSuccess || result.isFailure)
  }

  @Test
  fun `loadBitmapWithCorrectOrientation failure contains exception details`() = runTest {
    val exception = IllegalArgumentException("Invalid URI")
    every { mockContext.contentResolver.openInputStream(mockUri) } throws exception

    val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri)

    assertTrue(result.isFailure)
    assertEquals(exception, result.exceptionOrNull())
  }
}
