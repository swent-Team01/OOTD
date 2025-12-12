package com.android.ootd.model.camera

import android.graphics.Bitmap
import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageCroppingTest {

  private val helper = ImageOrientationHelper()

  @Test
  fun `cropBitmap returns cropped bitmap successfully`() {
    // Create a 100x100 bitmap
    val originalBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Crop a 50x50 square from the center
    val cropRect = Rect(25, 25, 75, 75)

    val result = helper.cropBitmap(originalBitmap, cropRect)

    assertTrue(result.isSuccess)
    val croppedBitmap = result.getOrNull()!!
    assertEquals(50, croppedBitmap.width)
    assertEquals(50, croppedBitmap.height)
  }

  @Test
  fun `cropBitmap fails with invalid rect`() {
    val originalBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Invalid rect (outside bounds)
    val cropRect = Rect(100, 100, 200, 200)

    val result = helper.cropBitmap(originalBitmap, cropRect)

    assertTrue(result.isFailure)
  }
}
