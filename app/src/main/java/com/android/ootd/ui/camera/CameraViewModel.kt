package com.android.ootd.ui.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.camera.CameraRepository
import com.android.ootd.model.camera.CameraRepositoryImplementation
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
 * @property zoomRatio Current zoom ratio
 * @property minZoomRatio Minimum zoom ratio supported by the camera
 * @property maxZoomRatio Maximum zoom ratio supported by the camera
 * @property showPreview Whether to show the preview screen after capture
 */
data class CameraUIState(
    val lensFacing: Int = androidx.camera.core.CameraSelector.LENS_FACING_BACK,
    val capturedImageUri: Uri? = null,
    val isCapturing: Boolean = false,
    val errorMessage: String? = null,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val showPreview: Boolean = false
)

/**
 * ViewModel for the Camera screen. Manages camera state and orchestrates camera operations through
 * the repository.
 *
 * @property repository The camera repository for handling CameraX operations
 */
class CameraViewModel(private val repository: CameraRepository = CameraRepositoryImplementation()) :
    ViewModel() {

  companion object {
    private const val TAG = "CameraViewModel"
  }

  private val _uiState = MutableStateFlow(CameraUIState())
  val uiState: StateFlow<CameraUIState> = _uiState.asStateFlow()

  private var cameraProvider: ProcessCameraProvider? = null
  private var camera: Camera? = null
  private var zoomStateObserver: Observer<androidx.camera.core.ZoomState>? = null

  // ExecutorService managed by ViewModel to ensure proper cleanup
  private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

  /** Called when the ViewModel is cleared. Cleans up resources to prevent memory leaks. */
  override fun onCleared() {
    super.onCleared()
    Log.i(TAG, "Cleaning up ViewModel resources")

    // Shutdown executor
    cameraExecutor.shutdown()

    // Remove zoom state observer to prevent mem leaks
    zoomStateObserver?.let { observer -> camera?.cameraInfo?.zoomState?.removeObserver(observer) }

    // Clear references
    camera = null
    cameraProvider = null
    zoomStateObserver = null
  }

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
    _uiState.value =
        _uiState.value.copy(capturedImageUri = uri, isCapturing = false, showPreview = uri != null)
  }

  /** Retakes the photo (clears captured image and shows camera). */
  fun retakePhoto() {
    _uiState.value = _uiState.value.copy(capturedImageUri = null, showPreview = false)
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

    // This removes previous observer if exists so that we can use our own camera zoom
    camera?.let { cam ->
      zoomStateObserver?.let { observer -> cam.cameraInfo.zoomState.removeObserver(observer) }
    }

    val result =
        repository.bindCamera(
            cameraProvider, previewView, lifecycleOwner, _uiState.value.lensFacing)

    return result
        .getOrElse { exception ->
          setError("Failed to bind camera: ${exception.message}")
          null
        }
        ?.let { (cameraInstance, imageCapture) ->
          camera = cameraInstance

          // Observe zoom state changes on our camera
          zoomStateObserver = Observer { zoomState ->
            _uiState.value =
                _uiState.value.copy(
                    zoomRatio = zoomState.zoomRatio,
                    minZoomRatio = zoomState.minZoomRatio,
                    maxZoomRatio = zoomState.maxZoomRatio)
          }

          cameraInstance.cameraInfo.zoomState.observe(lifecycleOwner, zoomStateObserver!!)

          imageCapture
        }
  }

  /**
   * Sets the zoom ratio.
   *
   * @param ratio The zoom ratio to set (must be between minZoomRatio and maxZoomRatio)
   */
  fun setZoomRatio(ratio: Float) {
    require(ratio > 0) { "Zoom ratio must be positive" }

    camera?.let { cam ->
      val clampedRatio = ratio.coerceIn(_uiState.value.minZoomRatio, _uiState.value.maxZoomRatio)

      if (clampedRatio != _uiState.value.zoomRatio) {
        cam.cameraControl.setZoomRatio(clampedRatio)
        Log.i(TAG, "Zoom ratio set to: $clampedRatio")
      }
    } ?: Log.w(TAG, "Cannot set zoom ratio: camera is null")
  }

  /**
   * Captures a photo using the repository. Uses the ViewModel's managed executor.
   *
   * @param context The context for creating the output file
   * @param imageCapture The ImageCapture instance
   * @param onSuccess Callback when image is successfully captured
   */
  fun capturePhoto(context: Context, imageCapture: ImageCapture, onSuccess: (Uri) -> Unit) {
    setCapturing(true)
    Log.i(TAG, "Starting photo capture")

    repository.capturePhoto(
        context = context,
        imageCapture = imageCapture,
        executor = cameraExecutor,
        onSuccess = { uri ->
          Log.i(TAG, "Photo captured successfully: $uri")
          setCapturedImage(uri)
          onSuccess(uri)
        },
        onError = { error ->
          Log.e(TAG, "Photo capture failed: $error")
          setError(error)
        })
  }

  /**
   * Gets the ProcessCameraProvider asynchronously. Uses cached provider if available.
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
    // Return cached provider if available
    cameraProvider?.let {
      onSuccess(it)
      return
    }

    viewModelScope.launch {
      try {
        val provider = repository.getCameraProvider(context)
        cameraProvider = provider // Cache the provider for later use
        onSuccess(provider)
      } catch (e: Exception) {
        onError("Failed to get camera provider: ${e.message}")
      }
    }
  }
}
