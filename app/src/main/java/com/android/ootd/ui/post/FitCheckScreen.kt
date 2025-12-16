package com.android.ootd.ui.post

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.ootd.R
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.map.LocationSelectionSection
import com.android.ootd.ui.map.LocationSelectionViewModel
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.LocationUtils
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.CommonTextField
import com.android.ootd.utils.composables.ImageSelectionDialog
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ShowText

object FitCheckScreenTestTags {
  const val SCREEN = "fitCheckScreen"
  const val TOP_BAR = "fitCheckTopBar"
  const val BACK_BUTTON = "fitCheckBackButton"
  const val IMAGE_PREVIEW = "fitCheckImagePreview"
  const val PLACEHOLDER_ICON = "fitCheckPlaceholderIcon"
  const val ADD_PHOTO_BUTTON = "fitCheckAddPhotoButton"
  const val ALERT_DIALOG = "fitCheckPhotoDialog"
  const val TAKE_PHOTO_BUTTON = "fitCheckTakePhotoButton"
  const val CHOOSE_GALLERY_BUTTON = "fitCheckGalleryButton"
  const val NEXT_BUTTON = "fitCheckNextButton"
  const val ERROR_MESSAGE = "fitCheckErrorMessage"
  const val MISSING_PHOTO_WARNING = "fitCheckMissingPhotoWarning"
  const val DESCRIPTION_INPUT = "fitCheckDescriptionInput"
  const val DESCRIPTION_COUNTER = "fitCheckDescriptionCounter"
}

private const val MAX_DESCRIPTION_LENGTH = 100

/** Image preview box showing selected photo or placeholder */
@Composable
private fun ImagePreviewBox(imageUri: Uri) {
  Box(
      modifier =
          Modifier.size(220.dp)
              .clip(RoundedCornerShape(16.dp))
              .border(4.dp, Primary, RoundedCornerShape(16.dp))
              .background(Color.White)
              .testTag(FitCheckScreenTestTags.IMAGE_PREVIEW),
      contentAlignment = Alignment.Center) {
        if (imageUri == Uri.EMPTY) {
          Icon(
              painter = painterResource(id = R.drawable.ic_photo_placeholder),
              contentDescription = "Placeholder icon",
              modifier = Modifier.size(80.dp).testTag(FitCheckScreenTestTags.PLACEHOLDER_ICON),
              tint = Color.Gray)
        } else {
          val context = LocalContext.current

          AsyncImage(
              model =
                  ImageRequest.Builder(context)
                      .data(imageUri)
                      .crossfade(true)
                      .allowHardware(false)
                      .memoryCacheKey(imageUri.toString())
                      .diskCacheKey(imageUri.toString())
                      .build(),
              contentDescription = "Selected photo",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop)
        }
      }
}

/** Description input field with character counter */
@Composable
private fun DescriptionInputField(description: String, onDescriptionChange: (String) -> Unit) {
  val remainingChars = MAX_DESCRIPTION_LENGTH - description.length

  Column(modifier = Modifier.fillMaxWidth()) {
    CommonTextField(
        value = description,
        onChange = { newValue ->
          if (newValue.length <= MAX_DESCRIPTION_LENGTH) {
            onDescriptionChange(newValue)
          }
        },
        label = "Description",
        placeholder = "Add a short caption for your FitCheck",
        modifier = Modifier.fillMaxWidth().testTag(FitCheckScreenTestTags.DESCRIPTION_INPUT),
        singleLine = false,
        maxLines = 2)
    ShowText(
        text = "$remainingChars/$MAX_DESCRIPTION_LENGTH characters left",
        style = Typography.bodySmall,
        modifier =
            Modifier.align(Alignment.End)
                .padding(top = 4.dp, end = 4.dp)
                .testTag(FitCheckScreenTestTags.DESCRIPTION_COUNTER))
  }
}

/** Photo selection dialog */
@Composable
private fun PhotoSelectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
  if (showDialog) {
    ImageSelectionDialog(
        modifier = Modifier.testTag(FitCheckScreenTestTags.ALERT_DIALOG),
        onDismissRequest = onDismiss,
        onTakePhoto = {
          onDismiss()
          onTakePhoto()
        },
        onPickFromGallery = {
          onChooseFromGallery()
          onDismiss()
        },
        takePhotoTag = FitCheckScreenTestTags.TAKE_PHOTO_BUTTON,
        pickGalleryTag = FitCheckScreenTestTags.CHOOSE_GALLERY_BUTTON)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitCheckScreen(
    fitCheckViewModel: FitCheckViewModel = viewModel(),
    locationSelectionViewModel: LocationSelectionViewModel = viewModel(),
    postUuid: String = "", // passed from previous screen if editing existing post
    onNextClick: (String, String, Location) -> Unit = { _, _, _ -> },
    onBackClick: () -> Unit = {},
    overridePhoto: Boolean = false
) {
  val uiState by fitCheckViewModel.uiState.collectAsState()

  var showCamera by remember { mutableStateOf(false) }

  val locationPermissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { isGranted ->
            if (isGranted) {
              locationSelectionViewModel.onLocationPermissionGranted()
            } else {
              locationSelectionViewModel.onLocationPermissionDenied()
            }
          })

  val context = LocalContext.current
  val onGPSClick =
      remember(locationSelectionViewModel, locationPermissionLauncher, context) {
        {
          if (LocationUtils.hasLocationPermission(context)) {
            locationSelectionViewModel.onLocationPermissionGranted()
          } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
          }
        }
      }

  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        if (uri != null) fitCheckViewModel.setPhoto(uri)
      }

  // Show custom camera screen when needed
  if (showCamera) {
    CameraScreen(
        onImageCaptured = { uri -> fitCheckViewModel.setPhoto(uri) },
        onDismiss = { showCamera = false })
  }

  FitCheckScreenContent(
      uiState = uiState,
      locationSelectionViewModel = locationSelectionViewModel,
      onNextClick = { imageUri, description, location ->
        onNextClick(imageUri, description, location)
      },
      onBackClick = {
        if (postUuid.isNotEmpty()) {
          fitCheckViewModel.deleteItemsForPost(postUuid)
        }
        onBackClick()
      },
      onChooseFromGallery = { galleryLauncher.launch("image/*") },
      onTakePhoto = { showCamera = true },
      onDescriptionChange = { fitCheckViewModel.setDescription(it) },
      onClearError = { fitCheckViewModel.clearError() },
      onGPSClick = onGPSClick,
      onLocationSelect = { fitCheckViewModel.setLocation(it) },
      overridePhoto = overridePhoto)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FitCheckScreenContent(
    uiState: FitCheckUIState,
    locationSelectionViewModel: LocationSelectionViewModel? = null,
    onNextClick: (String, String, Location) -> Unit = { _, _, _ -> },
    onBackClick: () -> Unit = {},
    onChooseFromGallery: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onClearError: () -> Unit = {},
    onGPSClick: () -> Unit = {},
    onLocationSelect: (Location) -> Unit = {},
    overridePhoto: Boolean = false
) {
  var showDialog by remember { mutableStateOf(false) }
  var showMissingPhotoWarning by remember { mutableStateOf(false) }
  val hasPhoto = uiState.image != Uri.EMPTY

  LaunchedEffect(hasPhoto) { if (hasPhoto) showMissingPhotoWarning = false }

  Scaffold(
      modifier = Modifier.testTag(FitCheckScreenTestTags.SCREEN),
      topBar = {
        OOTDTopBar(
            modifier = Modifier.testTag(FitCheckScreenTestTags.TOP_BAR),
            centerText = "FitCheck",
            leftComposable = {
              BackArrow(
                  onBackClick = onBackClick,
                  modifier = Modifier.testTag(FitCheckScreenTestTags.BACK_BUTTON))
            })
      },
      bottomBar = {
        Button(
            onClick = {
              if (overridePhoto || uiState.isPhotoValid) {
                onClearError()
                // Get the location from the locationSelectionViewModel if available, otherwise use
                // emptyLocation
                val finalLocation =
                    locationSelectionViewModel?.uiState?.value?.selectedLocation ?: emptyLocation
                onNextClick(uiState.image.toString(), uiState.description, finalLocation)
              } else {
                showMissingPhotoWarning = true
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag(FitCheckScreenTestTags.NEXT_BUTTON),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(16.dp)) {
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Add items",
                        tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Add items", color = Color.White)
                  }
            }
      }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
          Column(
              modifier =
                  Modifier.padding(innerPadding)
                      .padding(24.dp)
                      .fillMaxSize()
                      .verticalScroll(rememberScrollState()),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // Image preview
                ImagePreviewBox(imageUri = uiState.image)

                // Error message
                uiState.errorMessage?.let { msg ->
                  Text(
                      text = msg,
                      color = MaterialTheme.colorScheme.error,
                      style = Typography.bodyMedium,
                      modifier =
                          Modifier.padding(top = 8.dp)
                              .testTag(FitCheckScreenTestTags.ERROR_MESSAGE))
                }

                // Missing Photo Warning Text
                if (showMissingPhotoWarning) {
                  Text(
                      text = "Please add a photo before continuing.",
                      color = MaterialTheme.colorScheme.error,
                      style = Typography.bodyMedium,
                      modifier =
                          Modifier.padding(horizontal = 8.dp)
                              .testTag(FitCheckScreenTestTags.MISSING_PHOTO_WARNING))
                }

                // Description field with counter
                DescriptionInputField(
                    description = uiState.description, onDescriptionChange = onDescriptionChange)

                // Add photo button
                Button(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                      Text("Add Fit Photo", color = Color.White)
                    }

                // Location section (optional)
                locationSelectionViewModel?.let { viewModel ->
                  LocationSection(
                      locationSelectionViewModel = viewModel,
                      onGPSClick = onGPSClick,
                      onLocationSelect = onLocationSelect)
                }

                // Photo selection dialog
                PhotoSelectionDialog(
                    showDialog = showDialog,
                    onDismiss = { showDialog = false },
                    onTakePhoto = onTakePhoto,
                    onChooseFromGallery = onChooseFromGallery)
              }
        }
      }
}

@Composable
fun LocationSection(
    locationSelectionViewModel: LocationSelectionViewModel,
    onGPSClick: () -> Unit = {},
    onLocationSelect: (Location) -> Unit = {}
) {
  LocationSelectionSection(
      modifier = Modifier.fillMaxWidth(),
      viewModel = locationSelectionViewModel,
      textGPSButton = "Use current location (GPS)",
      textLocationField = "Location (Optional)",
      onGPSClick = onGPSClick,
      onLocationSelect = onLocationSelect)
}

@Preview(showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun FitCheckScreenPreview() {
  val previewState =
      FitCheckUIState(image = Uri.EMPTY, description = "Comfy autumn layers", errorMessage = null)
  OOTDTheme { FitCheckScreenContent(uiState = previewState) }
}
