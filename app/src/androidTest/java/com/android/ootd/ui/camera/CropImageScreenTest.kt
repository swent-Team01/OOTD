package com.android.ootd.ui.camera

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import org.junit.Rule
import org.junit.Test

class CropImageScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun cropImageScreen_displaysAndHandlesActions() {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    var cropCalled = false
    var cancelCalled = false

    composeTestRule.setContent {
      CropImageScreen(
          bitmap = bitmap, onCrop = { cropCalled = true }, onCancel = { cancelCalled = true })
    }

    val containerNode = composeTestRule.onNodeWithTag(CameraScreenTestTags.CROP_SCREEN_CONTAINER)
    containerNode.assertExists()

    // Simulate drag to cover gesture logic
    containerNode.performTouchInput { swipeRight() }

    composeTestRule.onNodeWithTag(CameraScreenTestTags.CROP_CANCEL_BUTTON).performClick()
    assert(cancelCalled)

    composeTestRule.onNodeWithTag(CameraScreenTestTags.CROP_SAVE_BUTTON).performClick()
    assert(cropCalled)
  }
}
