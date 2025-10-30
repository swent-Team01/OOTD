package com.android.ootd.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
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
    repository = CameraRepository()
  }

  @Test
  fun `bindCamera returns success with ImageCapture when binding succeeds`() {
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
    assertNotNull(result.getOrNull())
    assertTrue(result.getOrNull() is ImageCapture)
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
}
