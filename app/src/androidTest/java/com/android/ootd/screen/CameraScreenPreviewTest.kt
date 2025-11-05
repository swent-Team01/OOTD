package com.android.ootd.screen

import android.Manifest
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.camera.CameraScreenTestTags
import com.android.ootd.ui.camera.CameraViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Connected tests for CameraScreen image preview functionality.
 *
 * These tests verify the preview screen that appears after capturing a photo, including retake,
 * crop, and approve actions.
 */
class CameraScreenPreviewTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

  private lateinit var viewModel: CameraViewModel
  private var capturedUri: Uri? = null
  private var dismissed = false

  @Before
  fun setUp() {
    viewModel = CameraViewModel()
    capturedUri = null
    dismissed = false

    composeTestRule.setContent {
      CameraScreen(
          onImageCaptured = { uri -> capturedUri = uri },
          onDismiss = { dismissed = true },
          cameraViewModel = viewModel)
    }
  }

  // ========== Preview Display Tests ==========

  @Test
  fun previewScreen_displaysAfterCapture() {
    // Simulate image capture by setting captured image URI
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Should show preview buttons
    composeTestRule.onNodeWithTag(CameraScreenTestTags.RETAKE_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CROP_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.APPROVE_BUTTON).assertExists()
  }

  @Test
  fun previewScreen_displaysCloseButton() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule
        .onAllNodesWithTag(CameraScreenTestTags.CLOSE_BUTTON)
        .assertCountEquals(1)
        .onFirst()
        .assertIsDisplayed()
  }

  @Test
  fun previewScreen_displaysAllActionButtons() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CameraScreenTestTags.RETAKE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CROP_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.APPROVE_BUTTON).assertIsDisplayed()
  }

  // ========== Button Interaction Tests ==========

  @Test
  fun retakeButton_isClickable() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CameraScreenTestTags.RETAKE_BUTTON).assertIsEnabled()
  }

  @Test
  fun cropButton_isClickable() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CameraScreenTestTags.CROP_BUTTON).assertIsEnabled()
  }

  @Test
  fun approveButton_isClickable() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CameraScreenTestTags.APPROVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun retakeButton_returnsToCameraView() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Click retake
    composeTestRule.onNodeWithTag(CameraScreenTestTags.RETAKE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Should show camera preview again
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAMERA_PREVIEW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAPTURE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun approveButton_callsOnImageCaptured() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Click approve
    composeTestRule.onNodeWithTag(CameraScreenTestTags.APPROVE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Should have called onImageCaptured with the URI
    assert(capturedUri != null) { "onImageCaptured should be called with URI" }
    assert(capturedUri == testUri) { "URI should match the captured image URI" }
  }

  @Test
  fun approveButton_dismissesDialog() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Click approve
    composeTestRule.onNodeWithTag(CameraScreenTestTags.APPROVE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Should have called onDismiss
    assert(dismissed) { "onDismiss should be called after approve" }
  }

  @Test
  fun closeButton_inPreview_dismissesDialog() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Click close in preview
    composeTestRule.onAllNodesWithTag(CameraScreenTestTags.CLOSE_BUTTON).onFirst().performClick()

    composeTestRule.waitForIdle()

    // Should have called onDismiss
    assert(dismissed) { "onDismiss should be called when close is clicked in preview" }
  }

  // ========== Button Content Tests ==========

  @Test
  fun retakeButton_hasCorrectText() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Retake").assertIsDisplayed()
  }

  @Test
  fun cropButton_hasCorrectText() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Crop").assertIsDisplayed()
  }

  @Test
  fun approveButton_hasCorrectText() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Approve").assertIsDisplayed()
  }

  @Test
  fun retakeButton_hasCorrectContentDescription() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Retake").assertExists()
  }

  @Test
  fun approveButton_hasCorrectContentDescription() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Approve").assertExists()
  }

  // ========== State Management Tests ==========

  @Test
  fun previewScreen_doesNotShowCameraControls() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Camera controls should not be visible
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAPTURE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.ZOOM_SLIDER).assertDoesNotExist()
  }

  @Test
  fun viewModel_showPreview_isTrue_whenImageCaptured() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.showPreview) {
      "showPreview should be true when image is captured"
    }
    assert(viewModel.uiState.value.capturedImageUri == testUri) { "capturedImageUri should match" }
  }

  @Test
  fun viewModel_showPreview_isFalse_afterRetake() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Retake photo
    viewModel.retakePhoto()

    composeTestRule.waitForIdle()

    assert(!viewModel.uiState.value.showPreview) { "showPreview should be false after retake" }
    assert(viewModel.uiState.value.capturedImageUri == null) {
      "capturedImageUri should be null after retake"
    }
  }

  @Test
  fun viewModel_resetsState_afterApprove() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Click approve (this triggers reset in the callback)
    composeTestRule.onNodeWithTag(CameraScreenTestTags.APPROVE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // ViewModel should be reset (done by CameraScreen)
    // We can verify dismiss was called
    assert(dismissed) { "Dialog should be dismissed" }
  }

  // ========== Multiple Capture Tests ==========

  @Test
  fun canCaptureMultipleImages_sequentially() {
    // First capture
    val testUri1 = Uri.parse("content://test/image1.jpg")
    viewModel.setCapturedImage(testUri1)

    composeTestRule.waitForIdle()

    // Retake
    composeTestRule.onNodeWithTag(CameraScreenTestTags.RETAKE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Second capture
    val testUri2 = Uri.parse("content://test/image2.jpg")
    viewModel.setCapturedImage(testUri2)

    composeTestRule.waitForIdle()

    // Should show preview for second image
    composeTestRule.onNodeWithTag(CameraScreenTestTags.APPROVE_BUTTON).assertIsDisplayed()
    assert(viewModel.uiState.value.capturedImageUri == testUri2)
  }
}
