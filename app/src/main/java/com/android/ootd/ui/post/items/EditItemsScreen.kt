package com.android.ootd.ui.post.items

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary

object EditItemsScreenTestTags {
  const val PLACEHOLDER_PICTURE = "placeholderPicture"
  const val INPUT_ADD_PICTURE_GALLERY = "inputAddPictureGallery"
  const val INPUT_ADD_PICTURE_CAMERA = "inputAddPictureCamera"
  const val INPUT_ITEM_CATEGORY = "inputItemCategory"
  const val INPUT_ITEM_TYPE = "inputItemType"
  const val INPUT_ITEM_BRAND = "inputItemBrand"
  const val INPUT_ITEM_PRICE = "inputItemPrice"
  const val INPUT_ITEM_MATERIAL = "inputItemMaterial"
  const val INPUT_ITEM_LINK = "inputItemLink"
  const val BUTTON_SAVE_CHANGES = "buttonSaveChanges"
  const val BUTTON_DELETE_ITEM = "buttonDeleteItem"
  const val ALL_FIELDS = "allFields"
  const val TYPE_SUGGESTIONS = "typeSuggestions"
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
  var showCamera by remember { mutableStateOf(false) }

  LaunchedEffect(itemsUIState.isSaveSuccessful) { if (itemsUIState.isSaveSuccessful) goBack() }
  LaunchedEffect(itemsUIState.isDeleteSuccessful) { if (itemsUIState.isDeleteSuccessful) goBack() }
  LaunchedEffect(Unit) { editItemsViewModel.initTypeSuggestions(context) }
  LaunchedEffect(itemUuid) { if (itemUuid.isNotEmpty()) editItemsViewModel.loadItemById(itemUuid) }
  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editItemsViewModel.clearErrorMsg()
    }
  }

  val currentImageSizeState = remember { mutableStateOf(maxImageSize) }
  val imageScaleState = remember { mutableFloatStateOf(1f) }
  val nestedScrollConnection =
      _root_ide_package_.com.android.ootd.ui.post.rememberImageResizeScrollConnection(
          currentImageSize = currentImageSizeState,
          imageScale = imageScaleState,
          minImageSize = minImageSize,
          maxImageSize = maxImageSize)

  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        if (uri != null) editItemsViewModel.setPhoto(uri)
      }

  if (showCamera) {
    CameraScreen(
        onImageCaptured = { uri ->
          editItemsViewModel.setPhoto(uri)
          showCamera = false
        },
        onDismiss = { showCamera = false })
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = { EditTopBar(goBack) },
        content = { innerPadding ->
          Box(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(innerPadding)
                      .nestedScroll(nestedScrollConnection)) {
                FieldsList(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(16.dp)
                            .offset { IntOffset(0, currentImageSizeState.value.roundToPx()) }
                            .testTag(EditItemsScreenTestTags.ALL_FIELDS),
                    onPickFromGallery = { galleryLauncher.launch("image/*") },
                    onOpenCamera = { showCamera = true },
                    category = itemsUIState.category,
                    onCategoryChange = editItemsViewModel::setCategory,
                    type = itemsUIState.type,
                    suggestions = itemsUIState.suggestions,
                    onTypeChange = {
                      editItemsViewModel.setType(it)
                      editItemsViewModel.updateTypeSuggestions(it)
                    },
                    onTypeFocus = { editItemsViewModel.updateTypeSuggestions(itemsUIState.type) },
                    brand = itemsUIState.brand,
                    onBrandChange = editItemsViewModel::setBrand,
                    price = itemsUIState.price,
                    onPriceChange = editItemsViewModel::setPrice,
                    material = itemsUIState.materialText,
                    onMaterialChange = editItemsViewModel::setMaterial,
                    link = itemsUIState.link,
                    onLinkChange = editItemsViewModel::setLink,
                    isDeleteEnabled = itemsUIState.itemId.isNotEmpty(),
                    onDelete = editItemsViewModel::deleteItem,
                    isSaveEnabled =
                        (itemsUIState.localPhotoUri != null ||
                            itemsUIState.image.imageUrl.isNotEmpty()) &&
                            itemsUIState.category.isNotEmpty(),
                    onSave = editItemsViewModel::onSaveItemClick,
                )

                // Image preview overlay
                ItemsImagePreview(
                    localUri = itemsUIState.localPhotoUri,
                    remoteUrl = itemsUIState.image.imageUrl,
                    maxImageSize = maxImageSize,
                    imageScale = imageScaleState.floatValue,
                    currentSize = currentImageSizeState.value,
                    testTag = EditItemsScreenTestTags.PLACEHOLDER_PICTURE,
                    placeholderIcon = {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add photo",
                            tint = Tertiary,
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No picture yet", style = MaterialTheme.typography.bodyMedium)
                      }
                    })
              }
        })

    LoadingOverlay(visible = itemsUIState.isLoading)
  }
}

// --- Extracted composables to reduce cognitive complexity ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTopBar(goBack: () -> Unit) {
  CenterAlignedTopAppBar(
      title = {
        Text(
            "EDIT ITEMS",
            style =
                MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
      },
      navigationIcon = {
        Box(modifier = Modifier.padding(start = 4.dp), contentAlignment = Alignment.Center) {
          IconButton(onClick = { goBack() }) {
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
    onPickFromGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    type: String,
    suggestions: List<String>,
    onTypeChange: (String) -> Unit,
    onTypeFocus: () -> Unit,
    brand: String,
    onBrandChange: (String) -> Unit,
    price: Double,
    onPriceChange: (Double) -> Unit,
    material: String,
    onMaterialChange: (String) -> Unit,
    link: String,
    onLinkChange: (String) -> Unit,
    isDeleteEnabled: Boolean,
    onDelete: () -> Unit,
    isSaveEnabled: Boolean,
    onSave: () -> Unit,
) {
  LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item { ImagePickerRow(onPickFromGallery = onPickFromGallery, onOpenCamera = onOpenCamera) }
    item {
      CategoryField(
          category = category,
          onChange = onCategoryChange,
          testTag = EditItemsScreenTestTags.INPUT_ITEM_CATEGORY)
    }
    item {
      TypeField(
          type = type,
          suggestions = suggestions,
          onChange = onTypeChange,
          testTag = EditItemsScreenTestTags.INPUT_ITEM_TYPE,
          dropdownTestTag = EditItemsScreenTestTags.TYPE_SUGGESTIONS,
          onFocus = onTypeFocus)
    }
    item {
      BrandField(
          brand = brand,
          onChange = onBrandChange,
          testTag = EditItemsScreenTestTags.INPUT_ITEM_BRAND)
    }
    item { PriceField(price = price, onChange = onPriceChange) }
    item {
      MaterialField(
          materialText = material,
          onChange = onMaterialChange,
          testTag = EditItemsScreenTestTags.INPUT_ITEM_MATERIAL)
    }
    item {
      LinkField(
          link = link, onChange = onLinkChange, testTag = EditItemsScreenTestTags.INPUT_ITEM_LINK)
    }
    item {
      SaveDeleteRow(
          isDeleteEnabled = isDeleteEnabled,
          onDelete = onDelete,
          isSaveEnabled = isSaveEnabled,
          onSave = onSave)
    }
    item { Spacer(modifier = Modifier.height(100.dp)) }
  }
}

@Composable
private fun ImagePickerRow(onPickFromGallery: () -> Unit, onOpenCamera: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(
        onClick = onPickFromGallery,
        modifier = Modifier.weight(1f).testTag(EditItemsScreenTestTags.INPUT_ADD_PICTURE_GALLERY),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
          Text("Select from Gallery")
        }
    Button(
        onClick = onOpenCamera,
        modifier = Modifier.weight(1f).testTag(EditItemsScreenTestTags.INPUT_ADD_PICTURE_CAMERA),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
          Text("Take a new picture")
        }
  }
}

@Composable
private fun PriceField(price: Double, onChange: (Double) -> Unit) {
  val text = if (price == 0.0) "" else price.toString()
  OutlinedTextField(
      value = text,
      onValueChange = { onChange(it.toDoubleOrNull() ?: 0.0) },
      label = { Text("Price") },
      placeholder = { Text("e.g., 49.99") },
      textStyle =
          MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
      colors = commonTextFieldColors(),
      modifier = Modifier.fillMaxWidth().testTag(EditItemsScreenTestTags.INPUT_ITEM_PRICE))
}

@Composable
private fun SaveDeleteRow(
    isDeleteEnabled: Boolean,
    onDelete: () -> Unit,
    isSaveEnabled: Boolean,
    onSave: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(
        onClick = onDelete,
        enabled = isDeleteEnabled,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError),
        modifier = Modifier.weight(1f).testTag(EditItemsScreenTestTags.BUTTON_DELETE_ITEM)) {
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
        onClick = onSave,
        enabled = isSaveEnabled,
        modifier = Modifier.weight(1f).testTag(EditItemsScreenTestTags.BUTTON_SAVE_CHANGES),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
          Text("Save Changes")
        }
  }
}
