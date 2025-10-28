package com.android.ootd.ui.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Camera screen.
 *
 * @property lensFacing Current camera lens facing (front or back)
 * @property capturedImageUri URI of the captured image, null if not yet captured
 * @property isCapturing Whether a photo capture is in progress
 * @property errorMessage Error message to display, null if no error
 */
data class CameraUIState(
    val lensFacing: Int = androidx.camera.core.CameraSelector.LENS_FACING_BACK,
    val capturedImageUri: Uri? = null,
    val isCapturing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Camera screen. Manages camera state and orchestrates camera operations through
 * the repository.
 *
 * @property repository The camera repository for handling CameraX operations
 */
class CameraViewModel(private val repository: CameraRepository = CameraRepository()) : ViewModel() {
  private val _uiState = MutableStateFlow(CameraUIState())
  val uiState: StateFlow<CameraUIState> = _uiState.asStateFlow()

  /** Toggles between front and back camera. */
  fun switchCamera() {
    val newLensFacing =
        if (_uiState.value.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK) {
          androidx.camera.core.CameraSelector.LENS_FACING_FRONT
        } else {
          androidx.camera.core.CameraSelector.LENS_FACING_BACK
        }
    _uiState.value = _uiState.value.copy(lensFacing = newLensFacing)
  }

  /**
   * Sets the capturing state.
   *
   * @param isCapturing Whether a photo capture is in progress
   */
  fun setCapturing(isCapturing: Boolean) {
    _uiState.value = _uiState.value.copy(isCapturing = isCapturing)
  }

  /**
   * Sets the captured image URI.
   *
   * @param uri The URI of the captured image
   */
  fun setCapturedImage(uri: Uri?) {
    _uiState.value = _uiState.value.copy(capturedImageUri = uri, isCapturing = false)
  }

  /**
   * Sets an error message.
   *
   * @param message The error message to display
   */
  fun setError(message: String?) {
    _uiState.value = _uiState.value.copy(errorMessage = message, isCapturing = false)
  }

  /** Clears the error message. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /** Resets the camera state. */
  fun reset() {
    _uiState.value = CameraUIState()
  }

  /**
   * Binds the camera to the lifecycle using the repository.
   *
   * @param cameraProvider The ProcessCameraProvider instance
   * @param previewView The PreviewView to display camera preview
   * @param lifecycleOwner The LifecycleOwner to bind the camera to
   * @return ImageCapture instance on success, null on failure
   */
  fun bindCamera(
      cameraProvider: ProcessCameraProvider,
      previewView: PreviewView,
      lifecycleOwner: LifecycleOwner
  ): ImageCapture? {
    val result =
        repository.bindCamera(
            cameraProvider, previewView, lifecycleOwner, _uiState.value.lensFacing)

    return result.getOrElse { exception ->
      setError("Failed to bind camera: ${exception.message}")
      null
    }
  }

  /**
   * Captures a photo using the repository.
   *
   * @param context The context for creating the output file
   * @param imageCapture The ImageCapture instance
   * @param executor The executor for running the capture operation
   * @param onSuccess Callback when image is successfully captured
   */
  fun capturePhoto(
      context: Context,
      imageCapture: ImageCapture,
      executor: ExecutorService,
      onSuccess: (Uri) -> Unit
  ) {
    setCapturing(true)
    repository.capturePhoto(
        context = context,
        imageCapture = imageCapture,
        executor = executor,
        onSuccess = { uri ->
          setCapturedImage(uri)
          onSuccess(uri)
        },
        onError = { error -> setError(error) })
  }

  /**
   * Gets the ProcessCameraProvider asynchronously.
   *
   * @param context The context for getting the camera provider
   * @param onSuccess Callback with the camera provider
   * @param onError Callback when getting the provider fails
   */
  fun getCameraProvider(
      context: Context,
      onSuccess: (ProcessCameraProvider) -> Unit,
      onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val provider = repository.getCameraProvider(context)
        onSuccess(provider)
      } catch (e: Exception) {
        onError("Failed to get camera provider: ${e.message}")
      }
    }
  }
}
