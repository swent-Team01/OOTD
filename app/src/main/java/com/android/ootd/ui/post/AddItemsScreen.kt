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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography

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
    postUuid: String, // will be received from navigation
    modifier: Modifier = Modifier,
    addItemsViewModel: AddItemsViewModel = viewModel(),
    maxImageSize: Dp = 250.dp,
    minImageSize: Dp = 100.dp,
    onNextScreen: () -> Unit = {},
    goBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val itemsUIState by addItemsViewModel.uiState.collectAsState()
  var showDialog by remember { mutableStateOf(false) }
  var showCamera by remember { mutableStateOf(false) }

  val addOnSuccess by addItemsViewModel.addOnSuccess.collectAsState()

  LaunchedEffect(addOnSuccess) {
    if (addOnSuccess) {
      Toast.makeText(context, "Item added successfully!", Toast.LENGTH_SHORT).show()
      onNextScreen()
      addItemsViewModel.resetAddSuccess()
    }
  }

  // Initialise post ID
  LaunchedEffect(postUuid) { addItemsViewModel.initPostUuid(postUuid) }

  // Initialize type suggestions from YAML file
  LaunchedEffect(Unit) { addItemsViewModel.initTypeSuggestions(context) }

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
        if (uri != null) addItemsViewModel.setPhoto(uri)
      }

  // Show custom camera screen when needed
  if (showCamera) {
    CameraScreen(
        onImageCaptured = { uri ->
          addItemsViewModel.setPhoto(uri)
          showCamera = false
        },
        onDismiss = { showCamera = false })
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = { AddItemTopBar(modifier, goBack) },
        content = { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding).nestedScroll(nestedScrollConnection)) {
            FieldsList(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(18.dp)
                        .offset { IntOffset(0, currentImageSizeState.value.roundToPx()) }
                        .testTag(AddItemScreenTestTags.ALL_FIELDS),
                // image pick
                showDialog = showDialog,
                onShowDialogChange = { showDialog = it },
                onTakePhoto = {
                  showDialog = false
                  showCamera = true
                },
                onPickFromGallery = {
                  showDialog = false
                  galleryLauncher.launch("image/*")
                },
                // category
                category = itemsUIState.category,
                invalidCategory = itemsUIState.invalidCategory,
                categorySuggestions = itemsUIState.categorySuggestion,
                onCategoryChange = {
                  addItemsViewModel.setCategory(it)
                  addItemsViewModel.updateCategorySuggestions(it)
                },
                onCategoryValidate = { addItemsViewModel.validateCategory() },
                // type
                type = itemsUIState.type,
                typeSuggestions = itemsUIState.typeSuggestion,
                onTypeChange = {
                  addItemsViewModel.setType(it)
                  addItemsViewModel.updateTypeSuggestions(it)
                },
                // brand/price/link/material
                brand = itemsUIState.brand,
                onBrandChange = addItemsViewModel::setBrand,
                price = itemsUIState.price,
                onPriceChange = addItemsViewModel::setPrice,
                link = itemsUIState.link,
                onLinkChange = addItemsViewModel::setLink,
                material = itemsUIState.materialText,
                onMaterialChange = addItemsViewModel::setMaterial,
                // add
                isAddEnabled = itemsUIState.isAddingValid,
                onAddClick = addItemsViewModel::onAddItemClick,
            )

            // top image preview overlays the list
            ItemsImagePreview(
                localUri = itemsUIState.localPhotoUri,
                uploadURL = itemsUIState.image.imageUrl,
                maxImageSize = maxImageSize,
                imageScale = imageScaleState.floatValue,
                currentSize = currentImageSizeState.value,
            )
          }
        })

    LoadingOverlay(visible = itemsUIState.isLoading)
  }
}

// --- smaller composables ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemTopBar(modifier: Modifier, goBack: () -> Unit) {
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
}

@Composable
private fun FieldsList(
    modifier: Modifier,
    showDialog: Boolean,
    onShowDialogChange: (Boolean) -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    category: String,
    invalidCategory: String?,
    categorySuggestions: List<String>,
    onCategoryChange: (String) -> Unit,
    onCategoryValidate: () -> Unit,
    type: String,
    typeSuggestions: List<String>,
    onTypeChange: (String) -> Unit,
    brand: String,
    onBrandChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    link: String,
    onLinkChange: (String) -> Unit,
    material: String,
    onMaterialChange: (String) -> Unit,
    isAddEnabled: Boolean,
    onAddClick: () -> Unit,
) {
  LazyColumn(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    item {
      ImagePickerRow(
          onOpenDialog = { onShowDialogChange(true) },
          showDialog = showDialog,
          onDismissDialog = { onShowDialogChange(false) },
          onTakePhoto = onTakePhoto,
          onPickFromGallery = onPickFromGallery)
    }

    item {
      CategoryField(
          category = category,
          invalidCategory = invalidCategory,
          suggestions = categorySuggestions,
          onChange = onCategoryChange,
          onValidate = onCategoryValidate)
    }

    item { TypeField(type = type, suggestions = typeSuggestions, onChange = onTypeChange) }
    item { BrandField(brand = brand, onChange = onBrandChange) }
    item { PriceField(price = price, onChange = onPriceChange) }
    item { LinkField(link = link, onChange = onLinkChange) }
    item { MaterialField(materialText = material, onChange = onMaterialChange) }

    item {
      Spacer(modifier = Modifier.height(24.dp))
      AddItemButton(enabled = isAddEnabled, onClick = onAddClick)
    }

    item { Spacer(modifier = Modifier.height(100.dp)) }
  }
}

@Composable
private fun ImagePickerRow(
    onOpenDialog: () -> Unit,
    showDialog: Boolean,
    onDismissDialog: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    Button(
        onClick = onOpenDialog,
        colors = ButtonDefaults.buttonColors(containerColor = Primary),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.testTag(AddItemScreenTestTags.IMAGE_PICKER)) {
          Icon(
              painter = painterResource(R.drawable.ic_photo_placeholder),
              contentDescription = "Upload",
              tint = White,
              modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(8.dp))
          Text(text = "Upload a picture of the Item", color = White)
        }
  }

  if (showDialog) {
    AlertDialog(
        modifier = Modifier.testTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG),
        onDismissRequest = onDismissDialog,
        title = { Text(text = "Select Image") },
        text = {
          Column {
            TextButton(
                onClick = onTakePhoto,
                modifier = Modifier.testTag(AddItemScreenTestTags.TAKE_A_PHOTO)) {
                  Text("📸 Take a Photo")
                }
            TextButton(
                onClick = onPickFromGallery,
                modifier = Modifier.testTag(AddItemScreenTestTags.PICK_FROM_GALLERY)) {
                  Text("🖼️ Choose from Gallery")
                }
          }
        },
        confirmButton = {},
        dismissButton = {})
  }
}

@Composable
private fun CategoryField(
    category: String,
    invalidCategory: String?,
    suggestions: List<String>,
    onChange: (String) -> Unit,
    onValidate: () -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = category,
        onValueChange = {
          onChange(it)
          expanded = it.isNotBlank()
        },
        label = { Text("Category") },
        placeholder = { Text("Enter a category") },
        isError = invalidCategory != null,
        supportingText = {
          invalidCategory?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(AddItemScreenTestTags.ERROR_MESSAGE))
          }
        },
        modifier =
            Modifier.fillMaxWidth()
                .onFocusChanged { focusState ->
                  if (!focusState.isFocused && category.isNotBlank()) {
                    onValidate()
                    expanded = false
                  }
                }
                .testTag(AddItemScreenTestTags.INPUT_CATEGORY),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                  if (category.isNotBlank()) onValidate()
                  expanded = false
                  focusManager.clearFocus()
                }))

    DropdownMenu(
        expanded = expanded && suggestions.isNotEmpty(),
        onDismissRequest = {
          expanded = false
          if (category.isNotBlank()) onValidate()
        },
        modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.CATEGORY_SUGGESTION),
        properties = PopupProperties(focusable = false)) {
          suggestions.forEach { suggestion ->
            DropdownMenuItem(
                text = { Text(suggestion) },
                onClick = {
                  onChange(suggestion)
                  onValidate()
                  expanded = false
                })
          }
        }
  }
}

@Composable
private fun TypeField(type: String, suggestions: List<String>, onChange: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = type,
        onValueChange = {
          onChange(it)
          expanded = it.isNotBlank()
        },
        label = { Text("Type") },
        placeholder = { Text("Enter a type") },
        modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_TYPE),
        singleLine = true)

    DropdownMenu(
        expanded = expanded && suggestions.isNotEmpty(),
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.TYPE_SUGGESTIONS),
        properties = PopupProperties(focusable = false)) {
          suggestions.forEach { suggestion ->
            DropdownMenuItem(
                text = { Text(suggestion) },
                onClick = {
                  onChange(suggestion)
                  expanded = false
                })
          }
        }
  }
}

@Composable
private fun BrandField(brand: String, onChange: (String) -> Unit) {
  OutlinedTextField(
      value = brand,
      onValueChange = onChange,
      label = { Text("Brand") },
      placeholder = { Text("Enter a brand") },
      modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_BRAND))
}

@Composable
private fun PriceField(price: String, onChange: (String) -> Unit) {
  OutlinedTextField(
      value = price,
      onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) onChange(it) },
      label = { Text("Price") },
      placeholder = { Text("Enter a price") },
      modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_PRICE))
}

@Composable
private fun LinkField(link: String, onChange: (String) -> Unit) {
  OutlinedTextField(
      value = link,
      onValueChange = onChange,
      label = { Text("Link") },
      placeholder = { Text("Enter a link") },
      modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_LINK))
}

@Composable
private fun MaterialField(materialText: String, onChange: (String) -> Unit) {
  OutlinedTextField(
      value = materialText,
      onValueChange = onChange,
      label = { Text("Material") },
      placeholder = { Text("E.g., Cotton 80%, Wool 20%") },
      modifier = Modifier.fillMaxWidth().testTag(AddItemScreenTestTags.INPUT_MATERIAL))
}

@Composable
private fun AddItemButton(enabled: Boolean, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier =
          Modifier.height(47.dp).width(140.dp).testTag(AddItemScreenTestTags.ADD_ITEM_BUTTON),
      colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
              Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = "Add",
                  tint = Background,
                  modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(8.dp))
              Text(text = "Add Item", modifier = Modifier.align(Alignment.CenterVertically))
            }
      }
}

@Composable
private fun LoadingOverlay(visible: Boolean) {
  if (!visible) return
  Box(
      modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = Primary)
          Spacer(modifier = Modifier.height(12.dp))
          Text("Uploading item...", color = White, style = Typography.bodyLarge)
        }
      }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ItemsImagePreview(
    localUri: Uri?,
    uploadURL: String,
    maxImageSize: Dp,
    imageScale: Float,
    currentSize: Dp,
) {
  Box(
      modifier =
          Modifier.size(maxImageSize)
              .align(Alignment.TopCenter)
              .graphicsLayer {
                scaleX = imageScale
                scaleY = imageScale
                translationY = -((maxImageSize.toPx() - currentSize.toPx()) / 2f)
              }
              .clip(RoundedCornerShape(16.dp))
              .border(4.dp, Tertiary, RoundedCornerShape(16.dp))
              .testTag(AddItemScreenTestTags.IMAGE_PREVIEW),
      contentAlignment = Alignment.Center) {
        when {
          localUri != null ->
              AsyncImage(
                  model = localUri,
                  contentDescription = "Selected photo",
                  modifier = Modifier.matchParentSize(),
                  contentScale = ContentScale.Crop)
          uploadURL.isEmpty() ->
              Icon(
                  painter = painterResource(R.drawable.ic_photo_placeholder),
                  contentDescription = "Placeholder icon",
                  modifier = Modifier.size(80.dp))
          else ->
              AsyncImage(
                  model = uploadURL,
                  contentDescription = "Uploaded photo",
                  modifier = Modifier.matchParentSize(),
                  contentScale = ContentScale.Crop)
        }
      }
}
