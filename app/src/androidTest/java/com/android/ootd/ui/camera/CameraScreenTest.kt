package com.android.ootd.ui.camera

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
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
  fun displayAllCameraComponentsAndVerifyEnabled() {
    // Verify all components are displayed
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAMERA_PREVIEW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CLOSE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAPTURE_BUTTON).assertIsDisplayed()

    // Verify buttons are enabled
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CLOSE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).assertIsEnabled()
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
  fun switchCameraButtonTogglesLensAndContentDescription() {
    val initialLensFacing = viewModel.uiState.value.lensFacing

    // Verify content description before click
    composeTestRule.onNodeWithContentDescription("Switch Camera").assertIsDisplayed()

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

  @Test
  fun closeButtonHasCorrectContentDescription() {
    composeTestRule.onNodeWithContentDescription("Close Camera").assertIsDisplayed()
  }

  // ========== State Tests ==========

  @Test
  fun viewModelStateUpdatesCorrectly() {
    // Initially should be back camera
    assert(viewModel.uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
      "Initial camera should be back-facing"
    }

    // Switch camera
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.lensFacing == CameraSelector.LENS_FACING_FRONT) {
      "Camera should switch to front-facing"
    }
  }

  @Test
  fun zoomSlider_displaysWhenZoomIsSupported() {
    // Switch to front camera as it often has zoom capabilities in emulators
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Wait for camera to fully initialize and bind with front camera
    Thread.sleep(1500) // Give camera time to bind and update zoom state

    val uiState = viewModel.uiState.value

    // Log zoom capabilities for debugging
    android.util.Log.d(
        "CameraScreenTest",
        "Front camera zoom capabilities - min: ${uiState.minZoomRatio}, max: ${uiState.maxZoomRatio}")
    composeTestRule.onNodeWithTag(CameraScreenTestTags.ZOOM_SLIDER).assertExists()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.ZOOM_SLIDER).assertIsDisplayed()

    // Verify the zoom text displays current ratio
    val expectedText = String.format("%.1f", uiState.zoomRatio) + "x"
    composeTestRule.onNodeWithText(expectedText).assertExists()
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
  fun displayPermissionRequestWithAllComponents() {
    // Verify all permission UI components are displayed
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.PERMISSION_DENIED_MESSAGE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.PERMISSION_REQUEST_BUTTON)
        .assertIsDisplayed()
        .assertIsEnabled()

    // Verify text content
    composeTestRule
        .onNodeWithText("Camera permission is required to take your fit checks photos !")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Grant Permission !").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun clickingCancelButtonCallsOnDismiss() {
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    assert(dismissed) { "onDismiss should be called when cancel is clicked" }
  }
}
