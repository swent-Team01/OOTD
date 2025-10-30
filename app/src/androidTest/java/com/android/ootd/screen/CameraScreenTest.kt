package com.android.ootd.screen

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.camera.CameraScreenTestTags
import com.android.ootd.ui.camera.CameraViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Connected tests for the CameraScreen.
 *
 * These tests verify the camera screen UI components, permission handling, and user interactions in
 * a more integrated environment.
 */
class CameraScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

  private lateinit var viewModel: CameraViewModel
  private var imageCaptured = false
  private var dismissed = false

  @Before
  fun setUp() {
    viewModel = CameraViewModel()
    imageCaptured = false
    dismissed = false

    composeTestRule.setContent {
      CameraScreen(
          onImageCaptured = { imageCaptured = true },
          onDismiss = { dismissed = true },
          cameraViewModel = viewModel)
    }
  }

  // ========== Component Display Tests ==========

  @Test
  fun displayAllCameraComponents() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAMERA_PREVIEW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CLOSE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAPTURE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun closeButtonIsClickable() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CLOSE_BUTTON).assertIsEnabled()
  }

  @Test
  fun switchCameraButtonIsClickable() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).assertIsEnabled()
  }

  @Test
  fun captureButtonIsEnabledByDefault() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAPTURE_BUTTON).assertIsEnabled()
  }

  // ========== Interaction Tests ==========

  @Test
  fun clickingCloseButtonCallsOnDismiss() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CLOSE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assert(dismissed) { "onDismiss should be called when close button is clicked" }
  }

  @Test
  fun clickingSwitchCameraButtonTogglesCamera() {
    val initialLensFacing = viewModel.uiState.value.lensFacing

    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val newLensFacing = viewModel.uiState.value.lensFacing
    assert(initialLensFacing != newLensFacing) {
      "Camera lens facing should toggle when switch button is clicked"
    }
  }

  @Test
  fun captureButtonEnabledWhenNotCapturing() {
    viewModel.setCapturing(false)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAPTURE_BUTTON).assertIsEnabled()
  }

  // ========== Content Description Tests ==========

  @Test
  fun closeButtonHasCorrectContentDescription() {
    composeTestRule.onNodeWithContentDescription("Close Camera").assertIsDisplayed()
  }

  @Test
  fun switchCameraButtonHasCorrectContentDescription() {
    composeTestRule.onNodeWithContentDescription("Switch Camera").assertIsDisplayed()
  }

  // ========== State Tests ==========

  @Test
  fun viewModelStateUpdatesCorrectly() {
    // Initially should be back camera
    assert(
        viewModel.uiState.value.lensFacing ==
            androidx.camera.core.CameraSelector.LENS_FACING_BACK) {
          "Initial camera should be back-facing"
        }

    // Switch camera
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assert(
        viewModel.uiState.value.lensFacing ==
            androidx.camera.core.CameraSelector.LENS_FACING_FRONT) {
          "Camera should switch to front-facing"
        }
  }
}

/**
 * Connected tests for CameraScreen permission handling.
 *
 * These tests verify the permission request UI and behavior. Note: These tests do not grant camera
 * permission to test the permission request flow.
 */
class CameraScreenPermissionTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: CameraViewModel
  private var dismissed = false

  @Before
  fun setUp() {
    viewModel = CameraViewModel()
    dismissed = false

    composeTestRule.setContent {
      CameraScreen(
          onImageCaptured = {}, onDismiss = { dismissed = true }, cameraViewModel = viewModel)
    }
  }

  @Test
  fun displayPermissionRequestWhenNotGranted() {
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.PERMISSION_DENIED_MESSAGE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.PERMISSION_REQUEST_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun permissionMessageDisplaysCorrectText() {
    composeTestRule
        .onNodeWithText("Camera permission is required to take your fit checks photos !")
        .assertIsDisplayed()
  }

  @Test
  fun grantPermissionButtonDisplayed() {
    composeTestRule.onNodeWithText("Grant Permission !").assertIsDisplayed()
  }

  @Test
  fun cancelButtonDisplayedInPermissionRequest() {
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun clickingCancelButtonCallsOnDismiss() {
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    assert(dismissed) { "onDismiss should be called when cancel is clicked" }
  }

  @Test
  fun grantPermissionButtonIsClickable() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.PERMISSION_REQUEST_BUTTON).assertIsEnabled()
  }
}
