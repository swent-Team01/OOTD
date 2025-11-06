package com.android.ootd.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.camera.ImageOrientationHelper
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CameraScreenTestTags {
  const val CAMERA_PREVIEW = "cameraPreview"
  const val CAPTURE_BUTTON = "captureButton"
  const val SWITCH_CAMERA_BUTTON = "switchCameraButton"
  const val CLOSE_BUTTON = "closeButton"
  const val PERMISSION_DENIED_MESSAGE = "permissionDeniedMessage"
  const val PERMISSION_REQUEST_BUTTON = "permissionRequestButton"
  const val ZOOM_SLIDER = "zoomSlider"
  const val IMAGE_PREVIEW = "imagePreview"
  const val RETAKE_BUTTON = "retakeButton"
  const val APPROVE_BUTTON = "approveButton"
  const val CROP_BUTTON = "cropButton"
}

/**
 * A full-screen camera dialog that captures photos using CameraX.
 *
 * @param onImageCaptured Callback when an image is successfully captured
 * @param onDismiss Callback when the camera is closed
 * @param cameraViewModel ViewModel for managing camera state
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cameraViewModel: CameraViewModel = viewModel()
) {
  val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
  val cameraUiState by cameraViewModel.uiState.collectAsState()

  Dialog(
      onDismissRequest = {
        cameraViewModel.reset()
        onDismiss()
      },
      properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
          when {
            cameraUiState.showPreview && cameraUiState.capturedImageUri != null -> {
              // Show preview screen
              ImagePreviewScreen(
                  imageUri = cameraUiState.capturedImageUri!!,
                  onRetake = { cameraViewModel.retakePhoto() },
                  onApprove = { uri ->
                    onImageCaptured(uri)
                    cameraViewModel.reset()
                    onDismiss()
                  },
                  onClose = {
                    cameraViewModel.reset()
                    onDismiss()
                  })
            }
            cameraPermissionState.status.isGranted -> {
              CameraView(
                  onImageCaptured = { uri -> cameraViewModel.setCapturedImage(uri) },
                  onClose = {
                    cameraViewModel.reset()
                    onDismiss()
                  },
                  cameraViewModel = cameraViewModel)
            }
            else -> {
              // Show permission request UI
              Column(
                  modifier = Modifier.fillMaxSize().padding(32.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
                    Text(
                        "Camera permission is required to take your fit checks photos !",
                        color = White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier.padding(16.dp)
                                .testTag(CameraScreenTestTags.PERMISSION_DENIED_MESSAGE))

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        modifier =
                            Modifier.testTag(CameraScreenTestTags.PERMISSION_REQUEST_BUTTON)) {
                          Text("Grant Permission !")
                        }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                          cameraViewModel.reset()
                          onDismiss()
                        }) {
                          Text("Cancel", color = Secondary)
                        }
                  }
            }
          }
        }
      }
}

@Suppress("UNUSED_PARAMETER")
@SuppressLint("DefaultLocale")
@Composable
private fun CameraView(
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    cameraViewModel: CameraViewModel
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraUiState by cameraViewModel.uiState.collectAsState()

  var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
  val previewView = remember { PreviewView(context) }

  // Rebind camera when lensFacing changes
  LaunchedEffect(cameraUiState.lensFacing) {
    cameraViewModel.getCameraProvider(
        context,
        onSuccess = { cameraProvider ->
          val capture = cameraViewModel.bindCamera(cameraProvider, previewView, lifecycleOwner)
          imageCapture = capture
        },
        onError = { error -> cameraViewModel.setError(error) })
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Camera Preview with pinch-to-zoom gesture
    var scale by remember { mutableFloatStateOf(1f) }

    AndroidView(
        factory = { previewView },
        modifier =
            Modifier.fillMaxSize().testTag(CameraScreenTestTags.CAMERA_PREVIEW).pointerInput(Unit) {
              detectTransformGestures { _, _, zoom, _ ->
                scale *= zoom
                val newZoomRatio = cameraUiState.zoomRatio * zoom
                cameraViewModel.setZoomRatio(newZoomRatio)
              }
            })

    // Close button (top-left)
    IconButton(
        onClick = onClose,
        modifier =
            Modifier.align(Alignment.TopStart)
                .padding(16.dp)
                .background(Primary.copy(alpha = 0.5f), CircleShape)
                .testTag(CameraScreenTestTags.CLOSE_BUTTON)) {
          Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Camera",
              tint = White,
              modifier = Modifier.size(36.dp))
        }

    // Bottom controls
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {

          // Zoom Slider
          if (cameraUiState.maxZoomRatio > cameraUiState.minZoomRatio) {
            Row(
                modifier =
                    Modifier.padding(top = 80.dp, start = 16.dp, end = 16.dp)
                        .background(Primary.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically) {
                  Slider(
                      value = cameraUiState.zoomRatio,
                      onValueChange = { newRatio -> cameraViewModel.setZoomRatio(newRatio) },
                      valueRange = cameraUiState.minZoomRatio..cameraUiState.maxZoomRatio,
                      modifier = Modifier.width(220.dp).testTag(CameraScreenTestTags.ZOOM_SLIDER),
                      colors =
                          SliderDefaults.colors(
                              thumbColor = Tertiary,
                              activeTrackColor = Tertiary,
                              inactiveTrackColor = White.copy(alpha = 0.3f)))

                  Spacer(modifier = Modifier.size(8.dp))

                  Text(
                      text = "${String.format("%.1f", cameraUiState.zoomRatio)}x",
                      color = White,
                      style = MaterialTheme.typography.bodyMedium)
                }
          }

          Spacer(modifier = Modifier.size(8.dp))

          Row(
              modifier =
                  Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 140.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically) {
                // Switch Camera Button
                IconButton(
                    onClick = { cameraViewModel.switchCamera() },
                    modifier = Modifier.testTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.Refresh,
                          contentDescription = "Switch Camera",
                          tint = White,
                          modifier = Modifier.size(32.dp))
                    }

                // Capture Button
                Box(
                    modifier =
                        Modifier.size(72.dp)
                            .border(4.dp, Tertiary, CircleShape)
                            .testTag(CameraScreenTestTags.CAPTURE_BUTTON),
                    contentAlignment = Alignment.Center) {
                      IconButton(
                          onClick = {
                            imageCapture?.let { capture ->
                              cameraViewModel.capturePhoto(
                                  context = context,
                                  imageCapture = capture,
                                  onSuccess = {
                                    // Just set the captured image, don't close or call
                                    // onImageCaptured
                                    // The preview screen will handle that
                                  })
                            }
                          },
                          enabled = !cameraUiState.isCapturing,
                          modifier = Modifier.size(60.dp).background(Primary, CircleShape)) {
                            // Empty icon button - the circle is the visual
                          }
                    }

                // Placeholder for symmetry on the bottom bar
                Spacer(modifier = Modifier.size(32.dp))
              }
        }
  }
}

/**
 * Image preview screen showing the captured photo with options to retake, crop, or approve.
 *
 * @param imageUri URI of the captured image
 * @param onRetake Callback when user wants to retake the photo
 * @param onApprove Callback when user approves the image
 * @param onClose Callback when user closes the preview
 */
@Composable
private fun ImagePreviewScreen(
    imageUri: Uri,
    onRetake: () -> Unit,
    onApprove: (Uri) -> Unit,
    onClose: () -> Unit
) {
  val context = LocalContext.current

  var isCropping by remember { mutableStateOf(false) }
  var croppedImageUri by remember { mutableStateOf<Uri?>(null) }
  val currentImageUri = croppedImageUri ?: imageUri
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Load bitmap for preview with correct orientation
  LaunchedEffect(currentImageUri) {
    isLoading = true
    errorMessage = null

    // Load bitmap on IO dispatcher to avoid blocking main thread
    withContext(Dispatchers.IO) {
      val helper = ImageOrientationHelper()
      helper
          .loadBitmapWithCorrectOrientation(context, currentImageUri)
          .onSuccess { loadedBitmap ->
            withContext(Dispatchers.Main) {
              bitmap?.recycle() // Recycle old bitmap if exists
              bitmap = loadedBitmap
              isLoading = false
            }
          }
          .onFailure { error ->
            withContext(Dispatchers.Main) {
              errorMessage = "Failed to load image: ${error.message}"
              isLoading = false
            }
          }
    }
  }

  // Clean up bitmap when composable leaves composition
  DisposableEffect(Unit) {
    onDispose {
      bitmap?.recycle()
      bitmap = null
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    if (isCropping) {
      Text("Crop will be implemented later on !")
    } else {
      // Preview mode
      Box(modifier = Modifier.fillMaxSize()) {
        // Image content area with padding for bottom controls
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp)) {
          when {
            isLoading -> {
              // Show loading indicator
              CircularProgressIndicator(
                  modifier = Modifier.align(Alignment.Center), color = Tertiary)
            }
            errorMessage != null -> {
              // Show error message
              Text(
                  text = errorMessage ?: "Unknown error",
                  color = White,
                  modifier = Modifier.align(Alignment.Center).padding(16.dp),
                  style = MaterialTheme.typography.bodyLarge)
            }
            bitmap != null -> {
              // Show the loaded bitmap
              Image(
                  bitmap = bitmap!!.asImageBitmap(),
                  contentDescription = "Captured Image",
                  modifier = Modifier.fillMaxSize().testTag(CameraScreenTestTags.IMAGE_PREVIEW),
                  contentScale = ContentScale.Fit)
            }
          }
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier =
                Modifier.align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Primary.copy(alpha = 0.5f), CircleShape)
                    .testTag(CameraScreenTestTags.CLOSE_BUTTON)) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close Preview",
                  tint = White,
                  modifier = Modifier.size(36.dp))
            }

        // Bottom action buttons
        Row(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Primary.copy(alpha = 0.8f))
                    .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
              // Retake button
              Button(
                  onClick = onRetake,
                  modifier = Modifier.testTag(CameraScreenTestTags.RETAKE_BUTTON),
                  colors = ButtonDefaults.buttonColors(containerColor = Tertiary)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retake",
                        modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retake")
                  }

              // Crop button
              Button(
                  onClick = { isCropping = true },
                  modifier = Modifier.testTag(CameraScreenTestTags.CROP_BUTTON),
                  colors = ButtonDefaults.buttonColors(containerColor = Tertiary)) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crop")
                  }

              // Approve button
              Button(
                  onClick = { onApprove(currentImageUri) },
                  modifier = Modifier.testTag(CameraScreenTestTags.APPROVE_BUTTON),
                  colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Approve",
                        modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve")
                  }
            }
      }
    }
  }
}
