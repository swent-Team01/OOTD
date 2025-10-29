package com.android.ootd.ui.camera

import android.Manifest
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object CameraScreenTestTags {
  const val CAMERA_PREVIEW = "cameraPreview"
  const val CAPTURE_BUTTON = "captureButton"
  const val SWITCH_CAMERA_BUTTON = "switchCameraButton"
  const val CLOSE_BUTTON = "closeButton"
  const val PERMISSION_DENIED_MESSAGE = "permissionDeniedMessage"
  const val PERMISSION_REQUEST_BUTTON = "permissionRequestButton"
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

  Dialog(
      onDismissRequest = {
        cameraViewModel.reset()
        onDismiss()
      },
      properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
          when {
            cameraPermissionState.status.isGranted -> {
              CameraView(
                  onImageCaptured = onImageCaptured,
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
  val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
  val previewView = remember { PreviewView(context) }

  DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

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
    // Camera Preview
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize().testTag(CameraScreenTestTags.CAMERA_PREVIEW))

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
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 32.dp, bottom = 140.dp),
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
                            executor = cameraExecutor,
                            onSuccess = { uri ->
                              onImageCaptured(uri)
                              onClose()
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
