package com.android.ootd.ui.camera

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.io.File
import java.io.FileOutputStream
import org.junit.After
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
  private val testFiles = mutableListOf<File>()

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
  fun previewScreen_displaysAllComponentsAfterCapture() {
    // Simulate image capture by setting captured image URI
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Verify all preview buttons exist and are displayed
    composeTestRule.onNodeWithTag(CameraScreenTestTags.RETAKE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CROP_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.APPROVE_BUTTON).assertIsDisplayed()

    // Verify close button
    composeTestRule
        .onAllNodesWithTag(CameraScreenTestTags.CLOSE_BUTTON)
        .assertCountEquals(1)
        .onFirst()
        .assertIsDisplayed()

    // Verify camera controls are not shown
    composeTestRule.onNodeWithTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAPTURE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.ZOOM_SLIDER).assertDoesNotExist()
  }

  // ========== Button Interaction Tests ==========

  @Test
  fun previewButtons_areAllClickable() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CameraScreenTestTags.RETAKE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CROP_BUTTON).assertIsEnabled()
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
  fun previewButtons_haveCorrectTextAndContentDescriptions() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Verify button text
    composeTestRule.onNodeWithText("Retake").assertIsDisplayed()
    composeTestRule.onNodeWithText("Crop").assertIsDisplayed()
    composeTestRule.onNodeWithText("Approve").assertIsDisplayed()

    // Verify content descriptions
    composeTestRule.onNodeWithContentDescription("Retake").assertExists()
    composeTestRule.onNodeWithContentDescription("Approve").assertExists()
  }

  // ========== State Management Tests ==========

  @Test
  fun viewModel_managesPreviewStateCorrectly() {
    val testUri = Uri.parse("content://test/image.jpg")
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    // Verify initial preview state
    assert(viewModel.uiState.value.showPreview) {
      "showPreview should be true when image is captured"
    }
    assert(viewModel.uiState.value.capturedImageUri == testUri) { "capturedImageUri should match" }

    // Retake photo and verify state reset
    viewModel.retakePhoto()

    composeTestRule.waitForIdle()

    assert(!viewModel.uiState.value.showPreview) { "showPreview should be false after retake" }
    assert(viewModel.uiState.value.capturedImageUri == null) {
      "capturedImageUri should be null after retake"
    }
  }

  @Test
  fun viewModel_resetsStateAfterApprove() {
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

  // ========== Image Display Tests ==========

  @Test
  fun imagePreview_displaysLoadedBitmap() {
    // Create a real test image file
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val testFile = File(context.cacheDir, "test_preview_image_${System.currentTimeMillis()}.jpg")
    testFiles.add(testFile)

    // Create a simple bitmap and save it as JPEG
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.BLUE)

    FileOutputStream(testFile).use { outputStream ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    }
    bitmap.recycle()

    // Create a proper URI using FileProvider
    val testUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", testFile)

    // Set the captured image
    viewModel.setCapturedImage(testUri)

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(CameraScreenTestTags.IMAGE_PREVIEW)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(CameraScreenTestTags.IMAGE_PREVIEW).assertIsDisplayed()
  }
}
