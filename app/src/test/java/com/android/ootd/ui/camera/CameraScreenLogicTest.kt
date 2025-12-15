package com.android.ootd.ui.camera

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraScreenLogicTest {

  @Test
  fun `calculateCropRect maps coordinates correctly`() {
    // 100x100 bitmap
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Displayed in a 200x200 area (2x scale)
    val imageRect = Rect(0f, 0f, 200f, 200f)

    // Crop the top-left quarter of the displayed image (0,0 to 100,100)
    // This corresponds to 0,0 to 50,50 in the bitmap
    val cropRect = Rect(0f, 0f, 100f, 100f)

    val result = calculateCropRect(bitmap, imageRect, cropRect)

    assertEquals(0, result.left)
    assertEquals(0, result.top)
    assertEquals(50, result.right)
    assertEquals(50, result.bottom)
  }

  @Test
  fun `calculateCropRect handles offset correctly`() {
    // 100x100 bitmap
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Displayed in a 100x100 area at offset 50,50
    val imageRect = Rect(50f, 50f, 150f, 150f)

    // Crop the center 50x50 of the displayed image (75,75 to 125,125)
    // Relative to imageRect (50,50), this is 25,25 to 75,75
    // Since scale is 1, bitmap coords are 25,25 to 75,75
    val cropRect = Rect(75f, 75f, 125f, 125f)

    val result = calculateCropRect(bitmap, imageRect, cropRect)

    assertEquals(25, result.left)
    assertEquals(25, result.top)
    assertEquals(75, result.right)
    assertEquals(75, result.bottom)
  }
}
