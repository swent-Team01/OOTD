package com.android.ootd.ui.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.android.ootd.model.camera.CameraRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CameraViewModelTest {

  private lateinit var viewModel: CameraViewModel
  private lateinit var mockRepository: CameraRepository
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
    viewModel = CameraViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is correct`() {
    val state = viewModel.uiState.value

    assertEquals(CameraSelector.LENS_FACING_BACK, state.lensFacing)
    assertNull(state.capturedImageUri)
    assertFalse(state.isCapturing)
    assertNull(state.errorMessage)
  }

  @Test
  fun `switchCamera toggles from back to front`() {
    // Initial state is BACK
    assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.uiState.value.lensFacing)

    viewModel.switchCamera()

    assertEquals(CameraSelector.LENS_FACING_FRONT, viewModel.uiState.value.lensFacing)
  }

  @Test
  fun `switchCamera toggles from front to back`() {
    // Switch to front first
    viewModel.switchCamera()
    assertEquals(CameraSelector.LENS_FACING_FRONT, viewModel.uiState.value.lensFacing)

    // Switch back to back
    viewModel.switchCamera()

    assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.uiState.value.lensFacing)
  }

  @Test
  fun `setCapturing updates isCapturing state`() {
    viewModel.setCapturing(true)

    assertTrue(viewModel.uiState.value.isCapturing)

    viewModel.setCapturing(false)

    assertFalse(viewModel.uiState.value.isCapturing)
  }

  @Test
  fun `setCapturedImage updates capturedImageUri and resets isCapturing`() {
    val mockUri = mockk<Uri>()
    viewModel.setCapturing(true)

    viewModel.setCapturedImage(mockUri)

    assertEquals(mockUri, viewModel.uiState.value.capturedImageUri)
    assertFalse(viewModel.uiState.value.isCapturing)
  }

  @Test
  fun `setCapturedImage with null clears capturedImageUri`() {
    val mockUri = mockk<Uri>()
    viewModel.setCapturedImage(mockUri)

    viewModel.setCapturedImage(null)

    assertNull(viewModel.uiState.value.capturedImageUri)
  }

  @Test
  fun `setError updates errorMessage and resets isCapturing`() {
    viewModel.setCapturing(true)

    viewModel.setError("Test error")

    assertEquals("Test error", viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.isCapturing)
  }

  @Test
  fun `setError with null clears errorMessage`() {
    viewModel.setError("Test error")

    viewModel.setError(null)

    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `clearError clears errorMessage`() {
    viewModel.setError("Test error")

    viewModel.clearError()

    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `reset clears all state to initial values`() {
    // Modify state
    viewModel.switchCamera() // Set to FRONT
    viewModel.setCapturing(true)
    val mockUri = mockk<Uri>()
    viewModel.setCapturedImage(mockUri)
    viewModel.setError("Test error")

    // Reset
    viewModel.reset()

    val state = viewModel.uiState.value
    assertEquals(CameraSelector.LENS_FACING_BACK, state.lensFacing)
    assertNull(state.capturedImageUri)
    assertFalse(state.isCapturing)
    assertNull(state.errorMessage)
  }

  @Test
  fun `bindCamera returns ImageCapture on success`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>()
    val mockPreviewView = mockk<PreviewView>()
    val mockLifecycleOwner = mockk<LifecycleOwner>()
    val mockImageCapture = mockk<ImageCapture>()

    every {
      mockRepository.bindCamera(
          mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_BACK)
    } returns Result.success(mockImageCapture)

    val result = viewModel.bindCamera(mockCameraProvider, mockPreviewView, mockLifecycleOwner)

    assertEquals(mockImageCapture, result)
    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `bindCamera returns null and sets error on failure`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>()
    val mockPreviewView = mockk<PreviewView>()
    val mockLifecycleOwner = mockk<LifecycleOwner>()
    val exception = Exception("Camera binding failed")

    every {
      mockRepository.bindCamera(
          mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_BACK)
    } returns Result.failure(exception)

    val result = viewModel.bindCamera(mockCameraProvider, mockPreviewView, mockLifecycleOwner)

    assertNull(result)
    assertEquals(
        "Failed to bind camera: Camera binding failed", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `bindCamera uses correct lensFacing from state`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>()
    val mockPreviewView = mockk<PreviewView>()
    val mockLifecycleOwner = mockk<LifecycleOwner>()
    val mockImageCapture = mockk<ImageCapture>()

    // Switch to front camera
    viewModel.switchCamera()

    every {
      mockRepository.bindCamera(
          mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_FRONT)
    } returns Result.success(mockImageCapture)

    viewModel.bindCamera(mockCameraProvider, mockPreviewView, mockLifecycleOwner)

    verify {
      mockRepository.bindCamera(
          mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_FRONT)
    }
  }

  @Test
  fun `capturePhoto calls onSuccess callback when photo is captured`() {
    val mockContext = mockk<Context>()
    val mockImageCapture = mockk<ImageCapture>()
    val mockExecutor = mockk<ExecutorService>()
    val mockUri = mockk<Uri>()
    var successCalled = false
    var capturedUri: Uri? = null

    val onSuccessSlot = slot<(Uri) -> Unit>()

    every {
      mockRepository.capturePhoto(
          mockContext, mockImageCapture, mockExecutor, capture(onSuccessSlot), any())
    } answers { onSuccessSlot.captured(mockUri) }

    viewModel.capturePhoto(mockContext, mockImageCapture, mockExecutor) { uri ->
      successCalled = true
      capturedUri = uri
    }

    assertTrue(successCalled)
    assertEquals(mockUri, capturedUri)
    assertEquals(mockUri, viewModel.uiState.value.capturedImageUri)
    assertFalse(viewModel.uiState.value.isCapturing)
  }

  @Test
  fun `capturePhoto sets error when capture fails`() {
    val mockContext = mockk<Context>()
    val mockImageCapture = mockk<ImageCapture>()
    val mockExecutor = mockk<ExecutorService>()
    val errorMessage = "Capture failed"

    val onErrorSlot = slot<(String) -> Unit>()

    every {
      mockRepository.capturePhoto(
          mockContext, mockImageCapture, mockExecutor, any(), capture(onErrorSlot))
    } answers { onErrorSlot.captured(errorMessage) }

    viewModel.capturePhoto(mockContext, mockImageCapture, mockExecutor) {}

    assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.isCapturing)
  }

  @Test
  fun `getCameraProvider calls onSuccess with provider`() = runTest {
    val mockContext = mockk<Context>()
    val mockProvider = mockk<ProcessCameraProvider>()
    var successCalled = false
    var receivedProvider: ProcessCameraProvider? = null

    coEvery { mockRepository.getCameraProvider(mockContext) } returns mockProvider

    viewModel.getCameraProvider(
        mockContext,
        onSuccess = { provider ->
          successCalled = true
          receivedProvider = provider
        },
        onError = {})

    advanceUntilIdle()

    assertTrue(successCalled)
    assertEquals(mockProvider, receivedProvider)
  }

  @Test
  fun `getCameraProvider calls onError when exception occurs`() = runTest {
    val mockContext = mockk<Context>()
    val exception = Exception("Failed to get provider")
    var errorCalled = false
    var errorMessage: String? = null

    coEvery { mockRepository.getCameraProvider(mockContext) } throws exception

    viewModel.getCameraProvider(
        mockContext,
        onSuccess = {},
        onError = { error ->
          errorCalled = true
          errorMessage = error
        })

    advanceUntilIdle()

    assertTrue(errorCalled)
    assertEquals("Failed to get camera provider: Failed to get provider", errorMessage)
  }
}
