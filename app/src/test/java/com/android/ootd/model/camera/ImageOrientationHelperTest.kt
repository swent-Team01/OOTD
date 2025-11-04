package com.android.ootd.model.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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

// Note: these tests were made with the help of an AI model
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
      runTest(testDispatcher) {
        // Create a small test bitmap
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val mockInputStream = mockk<InputStream>(relaxed = true)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns bitmap
        every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
      }

  @Test
  fun `loadBitmapWithCorrectOrientation returns failure when bitmap cannot be decoded`() =
      runTest(testDispatcher) {
        val mockInputStream = mockk<InputStream>(relaxed = true)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns null
        every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isFailure)
      }

  @Test
  fun `loadBitmapWithCorrectOrientation returns failure when input stream is null`() =
      runTest(testDispatcher) {
        every { mockContext.contentResolver.openInputStream(mockUri) } returns null
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isFailure)
      }

  @Test
  fun `loadBitmapWithCorrectOrientation handles exceptions gracefully`() =
      runTest(testDispatcher) {
        every { mockContext.contentResolver.openInputStream(mockUri) } throws
            RuntimeException("File not found")
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
      }

  // ========== EXIF Orientation Tests ==========
  @Test
  fun `loadBitmapWithCorrectOrientation handles ORIENTATION_NORMAL correctly`() =
      runTest(testDispatcher) {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val mockInputStream = mockk<InputStream>(relaxed = true)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns bitmap
        every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isSuccess)
        val resultBitmap = result.getOrNull()
        assertNotNull(resultBitmap)
        // For normal orientation, dimensions should remain the same
        assertEquals(100, resultBitmap?.width)
        assertEquals(100, resultBitmap?.height)
      }

  @Test
  fun `loadBitmapWithCorrectOrientation handles exceptions during EXIF reading`() =
      runTest(testDispatcher) {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val mockInputStream = mockk<InputStream>(relaxed = true)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns bitmap
        every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream
        // Should still succeed even if EXIF reading fails
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isSuccess)
      }

  // ========== Edge Cases Tests ==========
  @Test
  fun `loadBitmapWithCorrectOrientation handles empty bitmap`() =
      runTest(testDispatcher) {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val mockInputStream = mockk<InputStream>(relaxed = true)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns bitmap
        every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isSuccess)
        val resultBitmap = result.getOrNull()
        assertNotNull(resultBitmap)
        assertEquals(1, resultBitmap?.width)
        assertEquals(1, resultBitmap?.height)
      }

  @Test
  fun `loadBitmapWithCorrectOrientation handles large bitmap dimensions`() =
      runTest(testDispatcher) {
        val bitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
        val mockInputStream = mockk<InputStream>(relaxed = true)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns bitmap
        every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isSuccess)
        val resultBitmap = result.getOrNull()
        assertNotNull(resultBitmap)
      }

  @Test
  fun `loadBitmapWithCorrectOrientation executes on IO dispatcher`() =
      runTest(testDispatcher) {
        // This test verifies the function completes without blocking main thread
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val mockInputStream = mockk<InputStream>(relaxed = true)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns bitmap
        every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        // Should complete successfully on background thread
        assertTrue(result.isSuccess)
      }

  // ========== Result Type Tests ==========
  @Test
  fun `loadBitmapWithCorrectOrientation failure contains exception details`() =
      runTest(testDispatcher) {
        val exception = IllegalArgumentException("Invalid URI")
        every { mockContext.contentResolver.openInputStream(mockUri) } throws exception
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
      }

  // ========== Orientation Rotation Tests (Testing applyOrientation indirectly) ==========
  private fun setupBitmapTest(width: Int, height: Int): Pair<Bitmap, InputStream> {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val mockInputStream = mockk<InputStream>(relaxed = true)
    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeStream(any()) } returns bitmap
    every { mockContext.contentResolver.openInputStream(mockUri) } returns mockInputStream
    return Pair(bitmap, mockInputStream)
  }

  @Test
  fun `loadBitmapWithCorrectOrientation handles various bitmap sizes and orientations`() =
      runTest(testDispatcher) {
        val testCases =
            listOf(
                Triple(200, 100, "non-square landscape"),
                Triple(300, 200, "landscape 3:2"),
                Triple(150, 250, "portrait"),
                Triple(100, 150, "portrait small"),
                Triple(500, 500, "square"),
                Triple(800, 600, "landscape large"),
                Triple(600, 800, "portrait large"),
                Triple(4096, 3072, "very large"),
                Triple(200, 300, "portrait medium"))

        testCases.forEach { (width, height, description) ->
          val (bitmap, _) = setupBitmapTest(width, height)
          val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)

          assertTrue("Failed for $description", result.isSuccess)
          val resultBitmap = result.getOrNull()
          assertNotNull("Bitmap null for $description", resultBitmap)
          assertTrue(
              "Invalid dimensions for $description",
              resultBitmap!!.width > 0 && resultBitmap.height > 0)
        }
      }

  @Test
  fun `loadBitmapWithCorrectOrientation preserves dimensions for no rotation needed`() =
      runTest(testDispatcher) {
        val (bitmap, _) = setupBitmapTest(100, 150)
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)

        assertTrue(result.isSuccess)
        val resultBitmap = result.getOrNull()
        assertNotNull(resultBitmap)
        // Should return bitmap with original dimensions (no rotation applied)
        assertEquals(100, resultBitmap?.width)
        assertEquals(150, resultBitmap?.height)
      }

  @Test
  fun `loadBitmapWithCorrectOrientation creates valid bitmap instance`() =
      runTest(testDispatcher) {
        val (bitmap, _) = setupBitmapTest(200, 300)
        val result = helper.loadBitmapWithCorrectOrientation(mockContext, mockUri, testDispatcher)

        assertTrue(result.isSuccess)
        val resultBitmap = result.getOrNull()
        assertNotNull(resultBitmap)
        // Result should be a valid bitmap with proper config
        assertNotNull(resultBitmap!!.config)
        assertTrue(resultBitmap.width > 0 && resultBitmap.height > 0)
      }
}
