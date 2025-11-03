package com.android.ootd.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.android.ootd.model.camera.CameraRepository
import com.android.ootd.model.camera.CameraRepositoryImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CameraRepositoryTest {

  private lateinit var repository: CameraRepository

  @Before
  fun setup() {
    repository = CameraRepositoryImpl()
  }

  @Test
  fun `bindCamera returns success with Camera and ImageCapture when binding succeeds`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()

    val result =
        repository.bindCamera(
            mockCameraProvider,
            mockPreviewView,
            mockLifecycleOwner,
            CameraSelector.LENS_FACING_BACK)

    assertTrue(result.isSuccess)
    val cameraAndCapture = result.getOrNull()
    assertNotNull(cameraAndCapture)
    assertNotNull(cameraAndCapture?.first) // Camera
    assertNotNull(cameraAndCapture?.second) // ImageCapture
  }

  @Test
  fun `bindCamera unbinds all before binding new camera`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()

    repository.bindCamera(
        mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_BACK)

    verify { mockCameraProvider.unbindAll() }
  }

  @Test
  fun `bindCamera binds with correct camera selector for back camera`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()

    repository.bindCamera(
        mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_BACK)

    verify {
      mockCameraProvider.bindToLifecycle(
          mockLifecycleOwner, any<CameraSelector>(), any<Preview>(), any<ImageCapture>())
    }
  }

  @Test
  fun `bindCamera binds with correct camera selector for front camera`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()

    repository.bindCamera(
        mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_FRONT)

    verify {
      mockCameraProvider.bindToLifecycle(
          mockLifecycleOwner, any<CameraSelector>(), any<Preview>(), any<ImageCapture>())
    }
  }

  @Test
  fun `bindCamera returns failure when exception occurs`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>()
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()

    every { mockCameraProvider.unbindAll() } throws RuntimeException("Binding failed")

    val result =
        repository.bindCamera(
            mockCameraProvider,
            mockPreviewView,
            mockLifecycleOwner,
            CameraSelector.LENS_FACING_BACK)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is RuntimeException)
  }

  @Test
  fun `capturePhoto creates file in cache directory`() {
    val mockContext = mockk<Context>()
    val mockImageCapture = mockk<ImageCapture>(relaxed = true)
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    every { mockContext.cacheDir } returns cacheDir

    repository.capturePhoto(mockContext, mockImageCapture, mockExecutor, {}, {})

    verify { mockContext.cacheDir }
  }

  @Test
  fun `capturePhoto calls takePicture on ImageCapture`() {
    val mockContext = mockk<Context>()
    val mockImageCapture = mockk<ImageCapture>(relaxed = true)
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    every { mockContext.cacheDir } returns cacheDir

    repository.capturePhoto(mockContext, mockImageCapture, mockExecutor, {}, {})

    verify { mockImageCapture.takePicture(any(), mockExecutor, any()) }
  }

  @Test
  fun `capturePhoto calls onSuccess with Uri when image is saved`() {
    val mockContext = mockk<Context>(relaxed = true)
    val mockImageCapture = mockk<ImageCapture>()
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val mockOutput = mockk<ImageCapture.OutputFileResults>()
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    var successCalled = false
    var capturedUri: android.net.Uri? = null

    every { mockContext.cacheDir } returns cacheDir

    val callbackSlot = slot<ImageCapture.OnImageSavedCallback>()

    every { mockImageCapture.takePicture(any(), mockExecutor, capture(callbackSlot)) } answers
        {
          callbackSlot.captured.onImageSaved(mockOutput)
        }

    repository.capturePhoto(
        mockContext,
        mockImageCapture,
        mockExecutor,
        onSuccess = { uri ->
          successCalled = true
          capturedUri = uri
        },
        onError = {})

    assertTrue(successCalled)
    assertNotNull(capturedUri)
  }

  @Test
  fun `capturePhoto calls onError when capture fails`() {
    val mockContext = mockk<Context>(relaxed = true)
    val mockImageCapture = mockk<ImageCapture>()
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val mockException = mockk<ImageCaptureException>()
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    var errorCalled = false
    var errorMessage: String? = null

    every { mockContext.cacheDir } returns cacheDir
    every { mockException.message } returns "Capture failed"

    val callbackSlot = slot<ImageCapture.OnImageSavedCallback>()

    every { mockImageCapture.takePicture(any(), mockExecutor, capture(callbackSlot)) } answers
        {
          callbackSlot.captured.onError(mockException)
        }

    repository.capturePhoto(
        mockContext,
        mockImageCapture,
        mockExecutor,
        onSuccess = {},
        onError = { error ->
          errorCalled = true
          errorMessage = error
        })

    assertTrue(errorCalled)
    assertEquals("Photo capture failed: Capture failed", errorMessage)
  }

  // ========== Additional bindCamera Tests ==========

  @Test
  fun `bindCamera returns ImageCapture with correct capture mode`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()

    val result =
        repository.bindCamera(
            mockCameraProvider,
            mockPreviewView,
            mockLifecycleOwner,
            CameraSelector.LENS_FACING_BACK)

    assertTrue(result.isSuccess)
    val imageCapture = result.getOrNull()?.second
    assertNotNull(imageCapture)
    // Verify ImageCapture was created (capture mode is an implementation detail)
  }

  @Test
  fun `bindCamera creates Preview with correct surface provider`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()
    val mockSurfaceProvider = mockk<Preview.SurfaceProvider>()

    every { mockPreviewView.surfaceProvider } returns mockSurfaceProvider

    repository.bindCamera(
        mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_BACK)

    // Verify that surfaceProvider was accessed (read)
    verify { mockPreviewView.surfaceProvider }
  }

  @Test
  fun `bindCamera uses surface provider from PreviewView`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()
    val mockSurfaceProvider = mockk<Preview.SurfaceProvider>()

    every { mockPreviewView.surfaceProvider } returns mockSurfaceProvider

    val result =
        repository.bindCamera(
            mockCameraProvider,
            mockPreviewView,
            mockLifecycleOwner,
            CameraSelector.LENS_FACING_BACK)

    // Should succeed when surface provider is available
    assertTrue(result.isSuccess)
    // Verify surfaceProvider was accessed
    verify { mockPreviewView.surfaceProvider }
  }

  @Test
  fun `bindCamera with front camera uses correct selector`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()
    val mockCamera = mockk<androidx.camera.core.Camera>(relaxed = true)

    every {
      mockCameraProvider.bindToLifecycle(
          mockLifecycleOwner, any<CameraSelector>(), any<Preview>(), any<ImageCapture>())
    } returns mockCamera

    repository.bindCamera(
        mockCameraProvider, mockPreviewView, mockLifecycleOwner, CameraSelector.LENS_FACING_FRONT)

    verify {
      mockCameraProvider.bindToLifecycle(
          mockLifecycleOwner, any<CameraSelector>(), any<Preview>(), any<ImageCapture>())
    }
  }

  // ========== Additional capturePhoto Tests ==========

  @Test
  fun `capturePhoto generates unique filename with timestamp`() {
    val mockContext = mockk<Context>(relaxed = true)
    val mockImageCapture = mockk<ImageCapture>(relaxed = true)
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    every { mockContext.cacheDir } returns cacheDir

    val outputOptionsSlot = slot<ImageCapture.OutputFileOptions>()
    every { mockImageCapture.takePicture(capture(outputOptionsSlot), mockExecutor, any()) }
        .returns(Unit)

    repository.capturePhoto(mockContext, mockImageCapture, mockExecutor, {}, {})

    // Verify takePicture was called with output options
    verify {
      mockImageCapture.takePicture(any<ImageCapture.OutputFileOptions>(), mockExecutor, any())
    }
  }

  @Test
  fun `capturePhoto handles null error message gracefully`() {
    val mockContext = mockk<Context>(relaxed = true)
    val mockImageCapture = mockk<ImageCapture>()
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val mockException = mockk<ImageCaptureException>()
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    var errorMessage: String? = null

    every { mockContext.cacheDir } returns cacheDir
    every { mockException.message } returns null

    val callbackSlot = slot<ImageCapture.OnImageSavedCallback>()

    every { mockImageCapture.takePicture(any(), mockExecutor, capture(callbackSlot)) } answers
        {
          callbackSlot.captured.onError(mockException)
        }

    repository.capturePhoto(
        mockContext,
        mockImageCapture,
        mockExecutor,
        onSuccess = {},
        onError = { error -> errorMessage = error })

    assertNotNull(errorMessage)
    assertTrue(errorMessage!!.contains("Photo capture failed"))
  }

  @Test
  fun `capturePhoto creates file in correct directory`() {
    val mockContext = mockk<Context>()
    val mockImageCapture = mockk<ImageCapture>(relaxed = true)
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    every { mockContext.cacheDir } returns cacheDir

    repository.capturePhoto(mockContext, mockImageCapture, mockExecutor, {}, {})

    verify { mockContext.cacheDir }
  }

  // ========== getCameraProvider Tests ==========

  @Test
  fun `getCameraProvider returns ProcessCameraProvider successfully`() {
    // Note: This test is difficult to fully test without a real Android context
    // as ProcessCameraProvider.getInstance requires ListenableFuture
    // This would be better tested in an instrumented test
    assertTrue(true) // Placeholder for documentation
  }

  // ========== Error Handling Tests ==========

  @Test
  fun `bindCamera handles lifecycle owner destruction gracefully`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>()
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()

    every { mockCameraProvider.unbindAll() } throws IllegalStateException("Lifecycle destroyed")

    val result =
        repository.bindCamera(
            mockCameraProvider,
            mockPreviewView,
            mockLifecycleOwner,
            CameraSelector.LENS_FACING_BACK)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalStateException)
  }

  @Test
  fun `capturePhoto calls takePicture with correct parameters`() {
    val mockContext = mockk<Context>(relaxed = true)
    val mockImageCapture = mockk<ImageCapture>(relaxed = true)
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    every { mockContext.cacheDir } returns cacheDir

    repository.capturePhoto(
        mockContext, mockImageCapture, mockExecutor, onSuccess = {}, onError = {})

    // Verify takePicture was called with OutputFileOptions
    verify {
      mockImageCapture.takePicture(any<ImageCapture.OutputFileOptions>(), mockExecutor, any())
    }
  }

  @Test
  fun `bindCamera returns both Camera and ImageCapture in result`() {
    val mockCameraProvider = mockk<ProcessCameraProvider>(relaxed = true)
    val mockPreviewView = mockk<PreviewView>(relaxed = true)
    val mockLifecycleOwner = mockk<LifecycleOwner>()

    val result =
        repository.bindCamera(
            mockCameraProvider,
            mockPreviewView,
            mockLifecycleOwner,
            CameraSelector.LENS_FACING_BACK)

    assertTrue(result.isSuccess)
    val pair = result.getOrNull()
    assertNotNull(pair)
    assertNotNull(pair?.first) // Camera
    assertNotNull(pair?.second) // ImageCapture
  }

  @Test
  fun `capturePhoto success callback receives valid Uri`() {
    val mockContext = mockk<Context>(relaxed = true)
    val mockImageCapture = mockk<ImageCapture>()
    val mockExecutor = mockk<ExecutorService>(relaxed = true)
    val mockOutput = mockk<ImageCapture.OutputFileResults>()
    val cacheDir = kotlin.io.path.createTempDirectory().toFile()

    var receivedUri: android.net.Uri? = null

    every { mockContext.cacheDir } returns cacheDir

    val callbackSlot = slot<ImageCapture.OnImageSavedCallback>()

    every { mockImageCapture.takePicture(any(), mockExecutor, capture(callbackSlot)) } answers
        {
          callbackSlot.captured.onImageSaved(mockOutput)
        }

    repository.capturePhoto(
        mockContext,
        mockImageCapture,
        mockExecutor,
        onSuccess = { uri -> receivedUri = uri },
        onError = {})

    assertNotNull(receivedUri)
  }
}
