package com.android.ootd.screen

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.camera.CameraScreenTestTags
import com.android.ootd.ui.camera.CameraViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Essential UI tests for CameraScreen to improve coverage. */
class CameraScreenUITest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

  private lateinit var viewModel: CameraViewModel

  @Before
  fun setUp() {
    viewModel = CameraViewModel()
    composeTestRule.setContent {
      CameraScreen(onImageCaptured = {}, onDismiss = {}, cameraViewModel = viewModel)
    }
  }

  @Test
  fun cameraComponentsAreDisplayed() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAMERA_PREVIEW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CLOSE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAPTURE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun switchCameraTogglesLensFacing() {
    val initial = viewModel.uiState.value.lensFacing
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assert(viewModel.uiState.value.lensFacing != initial)
  }
}
