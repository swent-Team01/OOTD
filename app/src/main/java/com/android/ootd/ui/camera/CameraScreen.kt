package com.android.ootd.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.camera.ImageOrientationHelper
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.CircularIconButton
import com.android.ootd.utils.composables.PermissionRequestScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
  const val ERROR_MESSAGE = "errorMessage"
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

  DisposableEffect(Unit) { onDispose { cameraViewModel.unbindCamera() } }

  Dialog(
      onDismissRequest = {
        cameraViewModel.reset()
        onDismiss()
      },
      properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
          when {
            cameraUiState.showPreview && cameraUiState.capturedImageUri != null -> {
              val context = LocalContext.current
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
                  },
                  onSaveBitmap = { bitmap -> cameraViewModel.saveCroppedImage(context, bitmap) })
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
              PermissionRequestScreen(
                  message = "Camera permission is required to take your fit checks photos !",
                  onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                  onCancel = {
                    cameraViewModel.reset()
                    onDismiss()
                  },
                  modifier = Modifier.testTag(CameraScreenTestTags.PERMISSION_DENIED_MESSAGE))
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

  val previewView = remember { PreviewView(context) }

  // Rebind camera when lensFacing changes
  LaunchedEffect(cameraUiState.lensFacing) {
    cameraViewModel.getCameraProvider(
        context,
        onSuccess = { cameraProvider ->
          cameraViewModel.bindCamera(cameraProvider, previewView, lifecycleOwner)
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

    // Top Bar with Close button
    Box(
        modifier =
            Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)))
                .padding(16.dp)) {
          CircularIconButton(
              onClick = onClose,
              icon = Icons.Default.Close,
              contentDescription = "Close Camera",
              modifier =
                  Modifier.align(Alignment.TopStart).testTag(CameraScreenTestTags.CLOSE_BUTTON),
              backgroundColor = Primary.copy(alpha = 0.3f),
              iconTint = White,
              iconSize = 24.dp)
        }

    // Bottom controls
    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(bottom = 48.dp, top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {

          // Zoom Slider
          if (cameraUiState.maxZoomRatio > cameraUiState.minZoomRatio) {
            Row(
                modifier =
                    Modifier.padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = "${String.format("%.1f", cameraUiState.zoomRatio)}x",
                      color = White,
                      style = Typography.labelLarge,
                      modifier = Modifier.padding(end = 8.dp))
                  Slider(
                      value = cameraUiState.zoomRatio,
                      onValueChange = { newRatio -> cameraViewModel.setZoomRatio(newRatio) },
                      valueRange = cameraUiState.minZoomRatio..cameraUiState.maxZoomRatio,
                      modifier = Modifier.width(150.dp).testTag(CameraScreenTestTags.ZOOM_SLIDER),
                      colors =
                          SliderDefaults.colors(
                              thumbColor = White,
                              activeTrackColor = Tertiary,
                              inactiveTrackColor = White.copy(alpha = 0.3f)))
                }
          }

          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically) {
                // Switch Camera Button
                IconButton(
                    onClick = { cameraViewModel.switchCamera() },
                    modifier =
                        Modifier.size(48.dp)
                            .background(White.copy(alpha = 0.2f), CircleShape)
                            .testTag(CameraScreenTestTags.SWITCH_CAMERA_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.Refresh,
                          contentDescription = "Switch Camera",
                          tint = White,
                          modifier = Modifier.size(24.dp))
                    }

                // Capture Button
                Box(
                    modifier =
                        Modifier.size(80.dp)
                            .border(4.dp, Secondary, CircleShape)
                            .padding(6.dp)
                            .background(
                                if (cameraUiState.isCapturing) Color.Gray else Primary, CircleShape)
                            .testTag(CameraScreenTestTags.CAPTURE_BUTTON)
                            .pointerInput(Unit) {
                              awaitPointerEventScope {
                                while (true) {
                                  val event = awaitPointerEvent()
                                  if (event.type == PointerEventType.Press) {
                                    if (!cameraUiState.isCapturing) {
                                      cameraViewModel.capturePhoto(
                                          context = context, onSuccess = {})
                                    }
                                  }
                                }
                              }
                            })

                // Placeholder for symmetry on the bottom bar
                Spacer(modifier = Modifier.size(48.dp))
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
    onClose: () -> Unit,
    onSaveBitmap: suspend (Bitmap) -> Uri
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

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
    if (isCropping && bitmap != null) {
      CropImageScreen(
          bitmap = bitmap!!,
          onCrop = { croppedBitmap ->
            scope.launch {
              val newUri = onSaveBitmap(croppedBitmap)
              croppedImageUri = newUri
              isCropping = false
            }
          },
          onCancel = { isCropping = false })
    } else {
      // Preview mode
      Box(modifier = Modifier.fillMaxSize()) {
        // Image content area
        when {
          isLoading -> {
            // Show loading indicator
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Tertiary)
          }
          errorMessage != null -> {
            // Show error message
            Text(
                text = errorMessage ?: "Unknown error",
                color = White,
                modifier =
                    Modifier.align(Alignment.Center)
                        .padding(16.dp)
                        .testTag(CameraScreenTestTags.ERROR_MESSAGE),
                style = Typography.bodyLarge)
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

        // Close button
        CircularIconButton(
            onClick = onClose,
            icon = Icons.Default.Close,
            contentDescription = "Close Preview",
            modifier =
                Modifier.align(Alignment.TopStart)
                    .padding(16.dp)
                    .testTag(CameraScreenTestTags.CLOSE_BUTTON),
            backgroundColor = Primary.copy(alpha = 0.5f),
            iconTint = White,
            iconSize = 36.dp)

        // Bottom action buttons
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(bottom = 48.dp, top = 24.dp)) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    // Retake button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      IconButton(
                          onClick = onRetake,
                          modifier =
                              Modifier.size(56.dp)
                                  .background(White.copy(alpha = 0.2f), CircleShape)
                                  .border(1.dp, White.copy(alpha = 0.5f), CircleShape)
                                  .testTag(CameraScreenTestTags.RETAKE_BUTTON)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retake",
                                tint = White,
                                modifier = Modifier.size(28.dp))
                          }
                      Spacer(modifier = Modifier.height(8.dp))
                      Text("Retake", color = White, style = Typography.bodyMedium)
                    }

                    // Crop button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      IconButton(
                          onClick = { isCropping = true },
                          enabled = bitmap != null,
                          modifier =
                              Modifier.size(56.dp)
                                  .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                  .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                  .testTag(CameraScreenTestTags.CROP_BUTTON)) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Crop",
                                tint = White,
                                modifier = Modifier.size(28.dp))
                          }
                      Spacer(modifier = Modifier.height(8.dp))
                      Text("Crop", color = White, style = Typography.bodyMedium)
                    }

                    // Approve button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      IconButton(
                          onClick = { onApprove(currentImageUri) },
                          modifier =
                              Modifier.size(56.dp)
                                  .background(Primary, CircleShape)
                                  .testTag(CameraScreenTestTags.APPROVE_BUTTON)) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Approve",
                                tint = White,
                                modifier = Modifier.size(28.dp))
                          }
                      Spacer(modifier = Modifier.height(8.dp))
                      Text("Approve", color = White, style = Typography.bodyMedium)
                    }
                  }
            }
      }
    }
  }
}

private enum class CropHandle {
  TopLeft,
  TopRight,
  BottomLeft,
  BottomRight,
  Center,
  None
}

@Composable
fun CropImageScreen(bitmap: Bitmap, onCrop: (Bitmap) -> Unit, onCancel: () -> Unit) {
  var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
  var cropRect by remember { mutableStateOf(Rect.Zero) }
  var imageRect by remember { mutableStateOf(Rect.Zero) }
  var activeHandle by remember { mutableStateOf(CropHandle.None) }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Color.Black)
              .onGloballyPositioned { coordinates ->
                if (containerSize == androidx.compose.ui.geometry.Size.Zero) {
                  containerSize = coordinates.size.toSize()

                  val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                  val containerRatio = containerSize.width / containerSize.height

                  val displayedWidth: Float
                  val displayedHeight: Float

                  if (bitmapRatio > containerRatio) {
                    displayedWidth = containerSize.width
                    displayedHeight = containerSize.width / bitmapRatio
                  } else {
                    displayedHeight = containerSize.height
                    displayedWidth = containerSize.height * bitmapRatio
                  }

                  val cx = containerSize.width / 2
                  val cy = containerSize.height / 2

                  imageRect =
                      Rect(
                          left = cx - displayedWidth / 2,
                          top = cy - displayedHeight / 2,
                          right = cx + displayedWidth / 2,
                          bottom = cy + displayedHeight / 2)
                  // Initialize crop rect to 80% of image
                  cropRect = imageRect.inflate(-min(displayedWidth, displayedHeight) * 0.1f)
                }
              }
              .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> activeHandle = getHitHandle(offset, cropRect) },
                    onDrag = { change, dragAmount ->
                      change.consume()
                      if (activeHandle != CropHandle.None) {
                        cropRect = updateCropRect(cropRect, imageRect, activeHandle, dragAmount)
                      }
                    },
                    onDragEnd = { activeHandle = CropHandle.None },
                    onDragCancel = { activeHandle = CropHandle.None })
              }) {
        // Display Image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Crop Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit)

        // Draw Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
          // Draw dimmed background outside cropRect
          val path = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
          val cropPath = Path().apply { addRect(cropRect) }

          // Clip out the crop rect
          clipPath(cropPath, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.6f))
          }

          // Draw crop border
          drawRect(
              color = Color.White,
              topLeft = cropRect.topLeft,
              size = cropRect.size,
              style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

          // Draw corners
          val handleSize = 20.dp.toPx()
          val handleColor = Color.White

          // TopLeft
          drawCircle(handleColor, radius = handleSize / 2, center = cropRect.topLeft)
          // TopRight
          drawCircle(handleColor, radius = handleSize / 2, center = cropRect.topRight)
          // BottomLeft
          drawCircle(handleColor, radius = handleSize / 2, center = cropRect.bottomLeft)
          // BottomRight
          drawCircle(handleColor, radius = handleSize / 2, center = cropRect.bottomRight)
        }

        // Buttons
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(bottom = 48.dp, top = 24.dp)) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onCancel) {
                      Text("Cancel", color = White, style = Typography.titleMedium)
                    }

                    Button(
                        onClick = {
                          val finalCropRect = calculateCropRect(bitmap, imageRect, cropRect)
                          val helper = ImageOrientationHelper()
                          val result = helper.cropBitmap(bitmap, finalCropRect)
                          result.onSuccess { onCrop(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)) {
                          Text("Save", color = White, style = Typography.titleMedium)
                        }
                  }
            }
      }
}

private fun getHitHandle(offset: Offset, rect: Rect, threshold: Float = 100f): CropHandle {
  if ((offset - rect.topLeft).getDistance() < threshold) return CropHandle.TopLeft
  if ((offset - rect.topRight).getDistance() < threshold) return CropHandle.TopRight
  if ((offset - rect.bottomLeft).getDistance() < threshold) return CropHandle.BottomLeft
  if ((offset - rect.bottomRight).getDistance() < threshold) return CropHandle.BottomRight
  if (rect.contains(offset)) return CropHandle.Center
  return CropHandle.None
}

private fun updateCropRect(current: Rect, bounds: Rect, handle: CropHandle, drag: Offset): Rect {
  var newLeft = current.left
  var newTop = current.top
  var newRight = current.right
  var newBottom = current.bottom

  val minSize = 100f

  when (handle) {
    CropHandle.TopLeft -> {
      newLeft = (newLeft + drag.x).coerceIn(bounds.left, newRight - minSize)
      newTop = (newTop + drag.y).coerceIn(bounds.top, newBottom - minSize)
    }
    CropHandle.TopRight -> {
      newRight = (newRight + drag.x).coerceIn(newLeft + minSize, bounds.right)
      newTop = (newTop + drag.y).coerceIn(bounds.top, newBottom - minSize)
    }
    CropHandle.BottomLeft -> {
      newLeft = (newLeft + drag.x).coerceIn(bounds.left, newRight - minSize)
      newBottom = (newBottom + drag.y).coerceIn(newTop + minSize, bounds.bottom)
    }
    CropHandle.BottomRight -> {
      newRight = (newRight + drag.x).coerceIn(newLeft + minSize, bounds.right)
      newBottom = (newBottom + drag.y).coerceIn(newTop + minSize, bounds.bottom)
    }
    CropHandle.Center -> {
      val width = current.width
      val height = current.height
      newLeft = (newLeft + drag.x).coerceIn(bounds.left, bounds.right - width)
      newTop = (newTop + drag.y).coerceIn(bounds.top, bounds.bottom - height)
      newRight = newLeft + width
      newBottom = newTop + height
    }
    CropHandle.None -> {}
  }

  return Rect(newLeft, newTop, newRight, newBottom)
}

fun calculateCropRect(bitmap: Bitmap, imageRect: Rect, cropRect: Rect): android.graphics.Rect {
  val scaleX = bitmap.width / imageRect.width
  val scaleY = bitmap.height / imageRect.height

  val x = ((cropRect.left - imageRect.left) * scaleX).toInt()
  val y = ((cropRect.top - imageRect.top) * scaleY).toInt()
  val width = (cropRect.width * scaleX).toInt()
  val height = (cropRect.height * scaleY).toInt()

  return android.graphics.Rect(
      max(0, x), max(0, y), min(bitmap.width, x + width), min(bitmap.height, y + height))
}
