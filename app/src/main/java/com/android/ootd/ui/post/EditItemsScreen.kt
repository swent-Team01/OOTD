package com.android.ootd.ui.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import java.io.File

object EditItemsScreenTestTags {
  const val PLACEHOLDER_PICTURE = "placeholderPicture"
  const val INPUT_ADD_PICTURE_GALLERY = "inputAddPictureGallery"
  const val INPUT_ADD_PICTURE_CAMERA = "inputAddPictureCamera"
  const val INPUT_ITEM_CATEGORY = "inputItemCategory"
  const val INPUT_ITEM_TYPE = "inputItemType"
  const val INPUT_ITEM_BRAND = "inputItemBrand"
  const val INPUT_ITEM_PRICE = "inputItemPrice"
  const val INPUT_ITEM_LINK = "inputItemLink"
  const val BUTTON_SAVE_CHANGES = "buttonSaveChanges"
  const val BUTTON_DELETE_ITEM = "buttonDeleteItem"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemsScreen(
    itemUuid: String = "",
    editItemsViewModel: EditItemsViewModel = viewModel(),
    goBack: () -> Unit = {},
    maxImageSize: Dp = 250.dp,
    minImageSize: Dp = 100.dp
) {

  val itemsUIState by editItemsViewModel.uiState.collectAsState()
  val errorMsg = itemsUIState.errorMessage
  val context = LocalContext.current

  LaunchedEffect(itemsUIState.isSaveSuccessful) {
    if (itemsUIState.isSaveSuccessful) {
      goBack()
    }
  }

  // Initialize type suggestions from YAML file
  LaunchedEffect(Unit) { editItemsViewModel.initTypeSuggestions(context) }

  LaunchedEffect(itemUuid) {
    if (itemUuid.isNotEmpty()) {
      editItemsViewModel.loadItemById(itemUuid)
    }
  }

  var expanded by remember { mutableStateOf(false) }
  var cameraUri by remember { mutableStateOf<Uri?>(null) }
  val currentImageSizeState = remember { mutableStateOf(maxImageSize) }
  val imageScaleState = remember { mutableFloatStateOf(1f) }

  val nestedScrollConnection =
      rememberImageResizeScrollConnection(
          currentImageSize = currentImageSizeState,
          imageScale = imageScaleState,
          minImageSize = minImageSize,
          maxImageSize = maxImageSize)

  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        if (uri != null) {
          editItemsViewModel.setPhoto(uri)
        }
      }

  val cameraLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
          success: Boolean ->
        if (success && cameraUri != null) {
          editItemsViewModel.setPhoto(cameraUri!!)
        }
      }

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editItemsViewModel.clearErrorMsg()
    } else if (itemsUIState.itemId.isNotEmpty()) {
      // This is used when we want to delete an item, as the call is asynchronous
      goBack()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
          TopAppBar(
              title = { Text("Edit Item", style = MaterialTheme.typography.titleLarge) },
              navigationIcon = {
                IconButton(onClick = goBack) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                      contentDescription = "Go Back")
                }
              })
        },
        content = { innerPadding ->
          Box(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(innerPadding)
                      .nestedScroll(nestedScrollConnection)) {
                LazyColumn(
                    modifier =
                        Modifier.fillMaxWidth().padding(16.dp).offset {
                          IntOffset(0, currentImageSizeState.value.roundToPx())
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                      item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                              Button(
                                  onClick = { galleryLauncher.launch("image/*") },
                                  modifier =
                                      Modifier.weight(1f)
                                          .testTag(
                                              EditItemsScreenTestTags.INPUT_ADD_PICTURE_GALLERY),
                                  colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                                    Text("Select from Gallery")
                                  }
                              Button(
                                  onClick = {
                                    val file =
                                        File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                                    val uri =
                                        FileProvider.getUriForFile(
                                            context, "${context.packageName}.provider", file)
                                    cameraUri = uri
                                    cameraLauncher.launch(uri)
                                  },
                                  modifier =
                                      Modifier.weight(1f)
                                          .testTag(
                                              EditItemsScreenTestTags.INPUT_ADD_PICTURE_CAMERA),
                                  colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                                    Text("Take a new picture")
                                  }
                            }
                      }

                      item {
                        OutlinedTextField(
                            value = itemsUIState.category,
                            onValueChange = { editItemsViewModel.setCategory(it) },
                            label = { Text("Category") },
                            placeholder = { Text("e.g., Clothes") },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .testTag(EditItemsScreenTestTags.INPUT_ITEM_CATEGORY))
                      }

                      item {
                        Box {
                          OutlinedTextField(
                              value = itemsUIState.type,
                              onValueChange = {
                                editItemsViewModel.setType(it)
                                editItemsViewModel.updateTypeSuggestions(it)
                                expanded = true
                              },
                              label = { Text("Type") },
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .testTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE)
                                      .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                          editItemsViewModel.updateTypeSuggestions(
                                              itemsUIState.type)
                                          expanded = true
                                        }
                                      })
                          DropdownMenu(
                              expanded = expanded && itemsUIState.suggestions.isNotEmpty(),
                              onDismissRequest = { expanded = false },
                              modifier = Modifier.fillMaxWidth()) {
                                itemsUIState.suggestions.forEach { suggestion ->
                                  DropdownMenuItem(
                                      text = { Text(suggestion) },
                                      onClick = {
                                        editItemsViewModel.setType(suggestion)
                                        expanded = false
                                      })
                                }
                              }
                        }
                      }

                      item {
                        OutlinedTextField(
                            value = itemsUIState.brand,
                            onValueChange = { editItemsViewModel.setBrand(it) },
                            label = { Text("Brand") },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .testTag(EditItemsScreenTestTags.INPUT_ITEM_BRAND))
                      }

                      item {
                        OutlinedTextField(
                            value =
                                if (itemsUIState.price == 0.0) ""
                                else itemsUIState.price.toString(),
                            onValueChange = {
                              val price = it.toDoubleOrNull() ?: 0.0
                              editItemsViewModel.setPrice(price)
                            },
                            label = { Text("Price") },
                            placeholder = { Text("e.g., 49.99") },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .testTag(EditItemsScreenTestTags.INPUT_ITEM_PRICE))
                      }

                      item {
                        OutlinedTextField(
                            value = itemsUIState.link,
                            onValueChange = { editItemsViewModel.setLink(it) },
                            label = { Text("Link") },
                            placeholder = { Text("e.g., https://example.com") },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .testTag(EditItemsScreenTestTags.INPUT_ITEM_LINK))
                      }

                      item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                              Button(
                                  onClick = { editItemsViewModel.deleteItem() },
                                  enabled = itemsUIState.itemId.isNotEmpty(),
                                  colors =
                                      ButtonDefaults.buttonColors(
                                          containerColor = MaterialTheme.colorScheme.error,
                                          contentColor = MaterialTheme.colorScheme.onError),
                                  modifier =
                                      Modifier.weight(1f)
                                          .testTag(EditItemsScreenTestTags.BUTTON_DELETE_ITEM)) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically) {
                                          Icon(
                                              imageVector = Icons.Default.Delete,
                                              contentDescription = "Delete",
                                              modifier = Modifier.size(20.dp))
                                          Spacer(modifier = Modifier.size(8.dp))
                                          Text("Delete Item")
                                        }
                                  }

                              Button(
                                  onClick = { editItemsViewModel.onSaveItemClick() },
                                  enabled =
                                      itemsUIState.image != Uri.EMPTY &&
                                          itemsUIState.category.isNotEmpty(),
                                  modifier =
                                      Modifier.weight(1f)
                                          .testTag(EditItemsScreenTestTags.BUTTON_SAVE_CHANGES),
                                  colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                                    Text("Save Changes")
                                  }
                            }
                      }

                      // Add extra spacing at the bottom to ensure all content is scrollable
                      item { Spacer(modifier = Modifier.height(100.dp)) }
                    }

                Box(
                    modifier =
                        Modifier.size(maxImageSize)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                              scaleX = imageScaleState.floatValue
                              scaleY = imageScaleState.floatValue
                              translationY =
                                  -(maxImageSize.toPx() - currentImageSizeState.value.toPx()) / 2f
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .border(4.dp, Tertiary, RoundedCornerShape(16.dp))
                            .background(Secondary)
                            .testTag(EditItemsScreenTestTags.PLACEHOLDER_PICTURE),
                    contentAlignment = Alignment.Center) {
                      val localUri = itemsUIState.localPhotoUri
                      val remoteUrl = itemsUIState.image.imageUrl
                      if (localUri != null) {
                        AsyncImage(
                            model = localUri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop)
                      } else if (remoteUrl.isNotEmpty()) {
                        AsyncImage(
                            model = remoteUrl,
                            contentDescription = "Uploaded image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop)
                      } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                          Icon(
                              imageVector = Icons.Default.Add,
                              contentDescription = "Add photo",
                              tint = Tertiary,
                              modifier = Modifier.size(48.dp))
                          Spacer(Modifier.height(8.dp))
                          Text("No picture yet", style = MaterialTheme.typography.bodyMedium)
                        }
                      }
                    }
              }
        })

    if (itemsUIState.isLoading) {
      Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = Primary)
              Spacer(modifier = Modifier.height(12.dp))
              Text("Uploading item...", color = Color.White, style = Typography.bodyLarge)
            }
          }
    }
  }
}
