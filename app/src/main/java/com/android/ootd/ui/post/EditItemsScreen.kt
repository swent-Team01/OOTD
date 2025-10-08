package com.android.ootd.ui.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    editItemsViewModel: EditItemsViewModel = viewModel(),
    onSave: () -> Unit = {},
    goBack: () -> Unit = {}
) {

  val itemsUIState by editItemsViewModel.uiState.collectAsState()
  val errorMsg = itemsUIState.errorMessage
  val context = LocalContext.current

  // Initialize type suggestions from YAML file
  LaunchedEffect(Unit) { editItemsViewModel.initTypeSuggestions(context) }

  var expanded by remember { mutableStateOf(false) }
  var cameraUri by remember { mutableStateOf<Uri?>(null) }

  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        uri?.let { editItemsViewModel.setPhoto(it) }
      }

  val cameraLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success
        ->
        if (success && cameraUri != null) editItemsViewModel.setPhoto(cameraUri!!)
      }

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editItemsViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Edit Item", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
              IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Go Back")
              }
            })
      },
      content = { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(220.dp)
                          .clip(RoundedCornerShape(16.dp))
                          .background(MaterialTheme.colorScheme.background)
                          .testTag(EditItemsScreenTestTags.PLACEHOLDER_PICTURE),
                  contentAlignment = Alignment.Center) {
                    if (itemsUIState.image != Uri.EMPTY) {
                      AsyncImage(
                          model = itemsUIState.image,
                          contentDescription = "Selected image",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop)
                    } else {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add photo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap to add photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier =
                            Modifier.weight(1f)
                                .testTag(EditItemsScreenTestTags.INPUT_ADD_PICTURE_GALLERY)) {
                          Text("Gallery")
                        }
                    Button(
                        onClick = {
                          val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
                          val uri =
                              FileProvider.getUriForFile(
                                  context, "${context.packageName}.fileprovider", tempFile)
                          cameraUri = uri
                          cameraLauncher.launch(uri)
                        },
                        modifier =
                            Modifier.weight(1f)
                                .testTag(EditItemsScreenTestTags.INPUT_ADD_PICTURE_CAMERA)) {
                          Text("Camera")
                        }
                  }

              OutlinedTextField(
                  value = itemsUIState.category,
                  onValueChange = { editItemsViewModel.setCategory(it) },
                  label = { Text("Category") },
                  placeholder = { Text("e.g., Clothes") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditItemsScreenTestTags.INPUT_ITEM_CATEGORY))

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
                                editItemsViewModel.updateTypeSuggestions(itemsUIState.type)
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

              OutlinedTextField(
                  value = itemsUIState.brand,
                  onValueChange = { editItemsViewModel.setBrand(it) },
                  label = { Text("Brand") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditItemsScreenTestTags.INPUT_ITEM_BRAND))

              OutlinedTextField(
                  value = if (itemsUIState.price == 0.0) "" else itemsUIState.price.toString(),
                  onValueChange = {
                    val price = it.toDoubleOrNull() ?: 0.0
                    editItemsViewModel.setPrice(price)
                  },
                  label = { Text("Price") },
                  placeholder = { Text("e.g., 49.99") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditItemsScreenTestTags.INPUT_ITEM_PRICE))

              OutlinedTextField(
                  value = itemsUIState.link,
                  onValueChange = { editItemsViewModel.setLink(it) },
                  label = { Text("Link") },
                  placeholder = { Text("e.g., https://zara.com") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditItemsScreenTestTags.INPUT_ITEM_LINK))

              Button(
                  onClick = {
                    if (editItemsViewModel.canEditItems()) {
                      onSave()
                    }
                  },
                  enabled = itemsUIState.image != Uri.EMPTY && itemsUIState.category.isNotEmpty(),
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(EditItemsScreenTestTags.BUTTON_SAVE_CHANGES)) {
                    Text("Save Changes")
                  }

              Spacer(modifier = Modifier.height(8.dp))

              Button(
                  onClick = {
                    editItemsViewModel.deleteItem()
                    goBack()
                  },
                  enabled = itemsUIState.itemId.isNotEmpty(),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.error,
                          contentColor = MaterialTheme.colorScheme.onError),
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditItemsScreenTestTags.BUTTON_DELETE_ITEM)) {
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
            }
      })
}
