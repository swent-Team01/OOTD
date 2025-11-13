package com.android.ootd.ui.post

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.theme.OOTDTheme

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
  const val DESCRIPTION_INPUT = "fitCheckDescriptionInput"
  const val DESCRIPTION_COUNTER = "fitCheckDescriptionCounter"
}

private const val MAX_DESCRIPTION_LENGTH = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitCheckScreen(
    fitCheckViewModel: FitCheckViewModel = viewModel(),
    postUuid: String = "", // passed from previous screen if editing existing post
    onNextClick: (String, String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    overridePhoto: Boolean = false
) {
  val uiState by fitCheckViewModel.uiState.collectAsState()

  var showCamera by remember { mutableStateOf(false) }

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
      onNextClick = onNextClick,
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
      overridePhoto = overridePhoto)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FitCheckScreenContent(
    uiState: FitCheckUIState,
    onNextClick: (String, String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    onChooseFromGallery: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onClearError: () -> Unit = {},
    overridePhoto: Boolean = false
) {
  var showDialog by remember { mutableStateOf(false) }

  Scaffold(
      modifier = Modifier.testTag(FitCheckScreenTestTags.SCREEN),
      topBar = {
        CenterAlignedTopAppBar(
            modifier = Modifier.testTag(FitCheckScreenTestTags.TOP_BAR),
            title = {
              Text(
                  text = "FitCheck",
                  style =
                      MaterialTheme.typography.displayLarge.copy(
                          fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            },
            navigationIcon = {
              IconButton(
                  onClick = { onBackClick() },
                  modifier = Modifier.testTag(FitCheckScreenTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.tertiary)
                  }
            },
        )
      },
      bottomBar = {
        Button(
            onClick = {
              if (overridePhoto || uiState.isPhotoValid) {
                onClearError()
                onNextClick(uiState.image.toString(), uiState.description)
              } else {
                onDescriptionChange(uiState.description) // no-op; real screen sets error
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag(FitCheckScreenTestTags.NEXT_BUTTON),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
        Column(
            modifier =
                Modifier.padding(innerPadding)
                    .padding(24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
              Box(
                  modifier =
                      Modifier.size(220.dp)
                          .clip(RoundedCornerShape(16.dp))
                          .border(
                              4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                          .background(Color.White)
                          .testTag(FitCheckScreenTestTags.IMAGE_PREVIEW),
                  contentAlignment = Alignment.Center) {
                    if (uiState.image == Uri.EMPTY) {
                      Icon(
                          painter = painterResource(id = R.drawable.ic_photo_placeholder),
                          contentDescription = "Placeholder icon",
                          modifier =
                              Modifier.size(80.dp).testTag(FitCheckScreenTestTags.PLACEHOLDER_ICON),
                          tint = Color.Gray)
                    } else {
                      AsyncImage(
                          model = uiState.image,
                          contentDescription = "Selected photo",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop)
                    }
                  }

              uiState.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier =
                        Modifier.padding(top = 8.dp).testTag(FitCheckScreenTestTags.ERROR_MESSAGE))
              }

              // --- Description field with char limit & counter ---
              val description = uiState.description
              val remainingChars = MAX_DESCRIPTION_LENGTH - description.length

              Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { newValue ->
                      if (newValue.length <= MAX_DESCRIPTION_LENGTH) {
                        onDescriptionChange(newValue)
                      }
                    },
                    label = { Text("Description") },
                    placeholder = { Text("Add a short caption for your FitCheck") },
                    modifier =
                        Modifier.fillMaxWidth().testTag(FitCheckScreenTestTags.DESCRIPTION_INPUT),
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp))

                Text(
                    text = "$remainingChars/$MAX_DESCRIPTION_LENGTH characters left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier.align(Alignment.End)
                            .padding(top = 4.dp, end = 4.dp)
                            .testTag(FitCheckScreenTestTags.DESCRIPTION_COUNTER))
              }

              Button(
                  onClick = { showDialog = true },
                  shape = RoundedCornerShape(24.dp),
                  modifier = Modifier.testTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Add Fit Photo", color = Color.White)
                  }

              if (showDialog) {
                AlertDialog(
                    modifier = Modifier.testTag(FitCheckScreenTestTags.ALERT_DIALOG),
                    onDismissRequest = { showDialog = false },
                    title = { Text("Select Photo") },
                    text = {
                      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                              showDialog = false
                              onTakePhoto()
                            },
                            modifier = Modifier.testTag(FitCheckScreenTestTags.TAKE_PHOTO_BUTTON)) {
                              Text("Take Photo")
                            }

                        TextButton(
                            onClick = {
                              onChooseFromGallery()
                              showDialog = false
                            },
                            modifier =
                                Modifier.testTag(FitCheckScreenTestTags.CHOOSE_GALLERY_BUTTON)) {
                              Text("Choose from Gallery")
                            }
                      }
                    },
                    confirmButton = {},
                    dismissButton = {})
              }
            }
      }
}

@Preview(showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun FitCheckScreenPreview() {
  val previewState =
      FitCheckUIState(image = Uri.EMPTY, description = "Comfy autumn layers", errorMessage = null)
  OOTDTheme { FitCheckScreenContent(uiState = previewState) }
}
