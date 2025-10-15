package com.android.ootd.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.R
import java.io.File

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitCheckScreen(
    fitCheckViewModel: FitCheckViewModel = viewModel(),
    onNextClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by fitCheckViewModel.uiState.collectAsState()

  var showDialog by remember { mutableStateOf(false) }
  var cameraUri by remember { mutableStateOf<Uri?>(null) }

  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        if (uri != null) fitCheckViewModel.setPhoto(uri)
      }

  val cameraLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success
        ->
        if (success && cameraUri != null) {
          fitCheckViewModel.setPhoto(cameraUri!!)
        }
      }

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
                  onClick = onBackClick,
                  modifier = Modifier.testTag(FitCheckScreenTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.tertiary)
                  }
            },
        )
      },
      bottomBar = {
        Button(
            onClick = {
              if (uiState.isPhotoValid) {
                fitCheckViewModel.clearError()
                onNextClick()
              } else {
                fitCheckViewModel.setErrorMsg("Please select a photo before continuing.")
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
                    Text("Next", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White)
                  }
            }
      }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(24.dp).fillMaxSize(),
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

              OutlinedTextField(
                  value = uiState.description,
                  onValueChange = { fitCheckViewModel.setDescription(it) },
                  label = { Text("Description") },
                  placeholder = { Text("Add a short caption for your FitCheck") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(FitCheckScreenTestTags.DESCRIPTION_INPUT),
                  singleLine = false,
                  maxLines = 2,
                  shape = RoundedCornerShape(12.dp))

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
                              val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                              val uri =
                                  FileProvider.getUriForFile(
                                      context, "${context.packageName}.provider", file)
                              cameraUri = uri
                              cameraLauncher.launch(uri)
                              showDialog = false
                            },
                            modifier = Modifier.testTag(FitCheckScreenTestTags.TAKE_PHOTO_BUTTON)) {
                              Text("Take Photo")
                            }

                        TextButton(
                            onClick = {
                              galleryLauncher.launch("image/*")
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
