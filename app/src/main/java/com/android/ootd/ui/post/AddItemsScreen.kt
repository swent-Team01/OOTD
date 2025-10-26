package com.android.ootd.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.ui.theme.*
import java.io.File

object AddItemScreenTestTags {
  const val INPUT_TYPE = "inputItemType"
  const val INPUT_BRAND = "inputItemBrand"
  const val INPUT_PRICE = "inputItemPrice"
  const val INPUT_LINK = "inputItemLink"
  const val INPUT_MATERIAL = "inputItemMaterial"
  const val INPUT_CATEGORY = "inputItemCategory"
  const val ADD_ITEM_BUTTON = "addItemButton"
  const val ERROR_MESSAGE = "errorMessage"
  const val IMAGE_PICKER = "itemImagePicker"
  const val IMAGE_PREVIEW = "itemImagePreview"

  const val GO_BACK_BUTTON = "goBackButton"

  const val IMAGE_PICKER_DIALOG = "imagePickerDialog"

  const val TITLE_ADD = "titleAddItem"
  const val PICK_FROM_GALLERY = "pickFromGallery"
  const val TAKE_A_PHOTO = "takeAPhoto"

  const val TYPE_SUGGESTIONS = "typeSuggestion"

  const val CATEGORY_SUGGESTION = "categorySuggestion"

  const val ALL_FIELDS = "allFields"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemsScreen(
    addItemsViewModel: AddItemsViewModel = viewModel(),
    onNextScreen: () -> Unit = {},
    goBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    maxImageSize: Dp = 250.dp,
    minImageSize: Dp = 100.dp
) {

  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  var typeExpanded by remember { mutableStateOf(false) }
  var categoryExpanded by remember { mutableStateOf(false) }
  val itemsUIState by addItemsViewModel.uiState.collectAsState()
  var showDialog by remember { mutableStateOf(false) }
  var currentImageSize by remember { mutableStateOf(maxImageSize) }
  var imageScale by remember { mutableFloatStateOf(1f) }

  // Initialize type suggestions from YAML file
  LaunchedEffect(Unit) { addItemsViewModel.initTypeSuggestions(context) }

  val density = LocalDensity.current
  val nestedScrollConnection =
      remember(density) {
        object : NestedScrollConnection {
          override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val deltaDp = with(density) { available.y.toDp() }

            val previousImageSize = currentImageSize
            val newImageSize = (previousImageSize + deltaDp).coerceIn(minImageSize, maxImageSize)
            val consumedDp = newImageSize - previousImageSize

            currentImageSize = newImageSize
            imageScale = currentImageSize / maxImageSize

            val consumedPx = with(density) { consumedDp.toPx() }
            return Offset(0f, consumedPx)
          }
        }
      }

  var cameraUri by remember { mutableStateOf<Uri?>(null) }
  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        if (uri != null) {
          addItemsViewModel.setPhoto(uri)
        }
      }

  val cameraLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
          success: Boolean ->
        if (success && cameraUri != null) {
          addItemsViewModel.setPhoto(cameraUri!!)
        }
      }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = "ADD ITEMS",
                  style =
                      MaterialTheme.typography.displayLarge.copy(
                          fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                  modifier = modifier.testTag(AddItemScreenTestTags.TITLE_ADD))
            },
            navigationIcon = {
              Box(modifier = Modifier.padding(start = 4.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { goBack() },
                    modifier = Modifier.testTag(AddItemScreenTestTags.GO_BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                          contentDescription = "Back",
                          tint = MaterialTheme.colorScheme.tertiary)
                    }
              }
            })
      },
      content = { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).nestedScroll(nestedScrollConnection)) {
          LazyColumn(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(18.dp)
                      .offset { IntOffset(0, currentImageSize.roundToPx()) }
                      .testTag(AddItemScreenTestTags.ALL_FIELDS),
              horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = { showDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag(AddItemScreenTestTags.IMAGE_PICKER)) {
                              Icon(
                                  painter = painterResource(R.drawable.ic_photo_placeholder),
                                  contentDescription = "Upload",
                                  tint = Background,
                                  modifier = Modifier.size(16.dp))
                              Spacer(Modifier.width(8.dp))
                              Text(text = "Upload a picture of the Item", color = Background)
                            }

                        if (showDialog) {
                          AlertDialog(
                              modifier =
                                  Modifier.testTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG),
                              onDismissRequest = { showDialog = false },
                              title = { Text(text = "Select Image") },
                              text = {
                                Column {
                                  TextButton(
                                      onClick = {
                                        // Take a photo
                                        val file =
                                            File(
                                                context.cacheDir,
                                                "${System.currentTimeMillis()}.jpg")
                                        val uri =
                                            FileProvider.getUriForFile(
                                                context, "${context.packageName}.provider", file)
                                        cameraUri = uri
                                        cameraLauncher.launch(uri)
                                        showDialog = false
                                      },
                                      modifier =
                                          Modifier.testTag(AddItemScreenTestTags.TAKE_A_PHOTO)) {
                                        Text("📸 Take a Photo")
                                      }

                                  TextButton(
                                      onClick = {
                                        // Pick from gallery
                                        galleryLauncher.launch("image/*")
                                        showDialog = false
                                      },
                                      modifier =
                                          Modifier.testTag(
                                              AddItemScreenTestTags.PICK_FROM_GALLERY)) {
                                        Text("🖼️ Choose from Gallery")
                                      }
                                }
                              },
                              confirmButton = {},
                              dismissButton = {})
                        }
                      }
                }

                item {
                  Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = itemsUIState.category,
                        onValueChange = {
                          addItemsViewModel.setCategory(it)
                          addItemsViewModel.updateCategorySuggestions(it)
                          categoryExpanded = it.isNotBlank()
                        },
                        label = { Text("Category") },
                        placeholder = { Text("Enter a category") },
                        isError = itemsUIState.invalidCategory != null,
                        supportingText = {
                          itemsUIState.invalidCategory?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag(AddItemScreenTestTags.ERROR_MESSAGE))
                          }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .onFocusChanged { focusState ->
                                  if (!focusState.isFocused && itemsUIState.category.isNotBlank()) {
                                    addItemsViewModel.validateCategory()
                                    categoryExpanded = false
                                  }
                                }
                                .testTag(AddItemScreenTestTags.INPUT_CATEGORY),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                  if (itemsUIState.category.isNotBlank()) {
                                    addItemsViewModel.validateCategory()
                                  }
                                  categoryExpanded = false
                                  focusManager.clearFocus()
                                }))

                    DropdownMenu(
                        expanded = categoryExpanded && itemsUIState.categorySuggestion.isNotEmpty(),
                        onDismissRequest = {
                          categoryExpanded = false
                          if (itemsUIState.category.isNotBlank()) {
                            addItemsViewModel.validateCategory()
                          }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag(AddItemScreenTestTags.CATEGORY_SUGGESTION),
                        properties = PopupProperties(focusable = false)) {
                          itemsUIState.categorySuggestion.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                  addItemsViewModel.setCategory(suggestion)
                                  addItemsViewModel.validateCategory()
                                  categoryExpanded = false
                                })
                          }
                        }
                  }
                }

                item {
                  Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = itemsUIState.type,
                        onValueChange = {
                          addItemsViewModel.setType(it)
                          addItemsViewModel.updateTypeSuggestions(it)
                          typeExpanded = it.isNotBlank()
                        },
                        label = { Text("Type") },
                        placeholder = { Text("Enter a type") },
                        modifier =
                            Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_TYPE),
                        singleLine = true)

                    DropdownMenu(
                        expanded = typeExpanded && itemsUIState.typeSuggestion.isNotEmpty(),
                        onDismissRequest = { typeExpanded = false },
                        modifier =
                            Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.TYPE_SUGGESTIONS),
                        properties = PopupProperties(focusable = false)) {
                          itemsUIState.typeSuggestion.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                  addItemsViewModel.setType(suggestion)
                                  typeExpanded = false
                                })
                          }
                        }
                  }
                }

                item {
                  OutlinedTextField(
                      value = itemsUIState.brand,
                      onValueChange = { addItemsViewModel.setBrand(it) },
                      label = { Text("Brand") },
                      placeholder = { Text("Enter a brand") },
                      modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_BRAND))
                }

                item {
                  OutlinedTextField(
                      value = itemsUIState.price,
                      onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                          addItemsViewModel.setPrice(it)
                        }
                      },
                      label = { Text("Price") },
                      placeholder = { Text("Enter a price") },
                      modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_PRICE))
                }

                item {
                  OutlinedTextField(
                      value = itemsUIState.link,
                      onValueChange = { addItemsViewModel.setLink(it) },
                      label = { Text("Link") },
                      placeholder = { Text("Enter a link") },
                      modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_LINK))
                }

                item {
                  OutlinedTextField(
                      value = itemsUIState.materialText,
                      onValueChange = { addItemsViewModel.setMaterial(it) },
                      label = { Text("Material") },
                      placeholder = { Text("E.g., Cotton 80%, Wool 20%") },
                      modifier =
                          Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_MATERIAL),
                  )
                }

                item {
                  Spacer(modifier = Modifier.height(24.dp))
                  val isButtonEnabled = itemsUIState.isAddingValid
                  Button(
                      onClick = {
                        if (addItemsViewModel.canAddItems()) {
                          onNextScreen()
                        }
                      },
                      enabled = isButtonEnabled,
                      modifier =
                          Modifier.height(47.dp)
                              .width(140.dp)
                              .testTag(AddItemScreenTestTags.ADD_ITEM_BUTTON),
                      colors = ButtonDefaults.buttonColors(containerColor = Primary),
                  ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                          Icon(
                              imageVector = Icons.Default.Add,
                              contentDescription = "Add",
                              tint = Background,
                              modifier = Modifier.size(20.dp))
                          Spacer(Modifier.width(8.dp))

                          Text(
                              text = "Add Item",
                              modifier = Modifier.align(Alignment.CenterVertically))
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
                        scaleX = imageScale
                        scaleY = imageScale
                        translationY = -(maxImageSize.toPx() - currentImageSize.toPx()) / 2f
                      }
                      .clip(RoundedCornerShape(16.dp))
                      .border(4.dp, Tertiary, RoundedCornerShape(16.dp))
                      .testTag(AddItemScreenTestTags.IMAGE_PREVIEW),
              contentAlignment = Alignment.Center,
          ) {
            if (itemsUIState.image == Uri.EMPTY) {
              Icon(
                  painter = painterResource(R.drawable.ic_photo_placeholder),
                  contentDescription = "Placeholder icon",
                  modifier = Modifier.size(80.dp))
            } else {
              AsyncImage(
                  model = itemsUIState.image,
                  contentDescription = "Selected photo",
                  modifier = Modifier.matchParentSize(),
                  contentScale = ContentScale.Crop)
            }
          }
        }
      })
}
