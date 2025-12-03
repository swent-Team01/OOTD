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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.post.rememberImageResizeScrollConnection
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.CenteredLoadingState
import com.android.ootd.utils.composables.OOTDTopBar

object EditItemsScreenTestTags {
  const val PLACEHOLDER_PICTURE = "placeholderPicture"
  const val INPUT_ADD_PICTURE_GALLERY = "inputAddPictureGallery"
  const val INPUT_ADD_PICTURE_CAMERA = "inputAddPictureCamera"
  const val INPUT_ITEM_CATEGORY = "inputItemCategory"
  const val INPUT_ITEM_TYPE = "inputItemType"
  const val INPUT_ITEM_BRAND = "inputItemBrand"
  const val INPUT_ITEM_PRICE = "inputItemPrice"
  const val INPUT_ITEM_CURRENCY = "inputItemCurrency"
  const val INPUT_ITEM_MATERIAL = "inputItemMaterial"
  const val INPUT_ITEM_LINK = "inputItemLink"
  const val BUTTON_SAVE_CHANGES = "buttonSaveChanges"
  const val BUTTON_DELETE_ITEM = "buttonDeleteItem"
  const val ALL_FIELDS = "allFields"
  const val TYPE_SUGGESTIONS = "typeSuggestions"
  const val ADDITIONAL_DETAILS_TOGGLE = "edit_additionalDetailsToggle"
  const val ADDITIONAL_DETAILS_SECTION = "edit_additionalDetailsSection"
  const val INPUT_ITEM_CONDITION = "edit_inputItemCondition"
  const val INPUT_ITEM_SIZE = "edit_inputItemSize"
  const val INPUT_ITEM_FIT_TYPE = "edit_inputItemFitType"
  const val INPUT_ITEM_STYLE = "edit_inputItemStyle"
  const val INPUT_ITEM_NOTES = "edit_inputItemNotes"
  const val STYLE_SUGGESTIONS = "edit_styleSuggestions"
  const val FIT_TYPE_SUGGESTIONS = "edit_fitTypeSuggestions"
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
      rememberImageResizeScrollConnection(
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
        topBar = {
          OOTDTopBar(
              centerText = "EDIT ITEMS", leftComposable = { BackArrow(onBackClick = goBack) })
        },
        floatingActionButton = {
          Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              horizontalAlignment = Alignment.End) {
                // Delete FAB
                val isDeleteEnabled = itemsUIState.itemId.isNotEmpty()
                FloatingActionButton(
                    onClick = { if (isDeleteEnabled) editItemsViewModel.deleteItem() },
                    containerColor =
                        if (isDeleteEnabled) MaterialTheme.colorScheme.error else Color.Gray,
                    modifier =
                        Modifier.testTag(EditItemsScreenTestTags.BUTTON_DELETE_ITEM).semantics {
                          if (!isDeleteEnabled) disabled()
                        }) {
                      Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Delete Item",
                          tint =
                              if (isDeleteEnabled) MaterialTheme.colorScheme.onError
                              else Color.White)
                    }

                // Save FAB
                val isSaveEnabled =
                    (itemsUIState.localPhotoUri != null ||
                        itemsUIState.image.imageUrl.isNotEmpty()) &&
                        itemsUIState.category.isNotEmpty()
                FloatingActionButton(
                    onClick = { if (isSaveEnabled) editItemsViewModel.onSaveItemClick(context) },
                    containerColor = if (isSaveEnabled) Primary else Color.Gray,
                    modifier =
                        Modifier.testTag(EditItemsScreenTestTags.BUTTON_SAVE_CHANGES).semantics {
                          if (!isSaveEnabled) disabled()
                        }) {
                      Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = "Save Changes",
                          tint = White)
                    }
              }
        },
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
                    currency = itemsUIState.currency,
                    onCurrencyChange = editItemsViewModel::setCurrency,
                    link = itemsUIState.link,
                    onLinkChange = editItemsViewModel::setLink,
                    // Additional
                    condition = itemsUIState.condition,
                    onConditionChange = editItemsViewModel::setCondition,
                    size = itemsUIState.size,
                    onSizeChange = editItemsViewModel::setSize,
                    fitType = itemsUIState.fitType,
                    onFitTypeChange = editItemsViewModel::setFitType,
                    style = itemsUIState.style,
                    onStyleChange = editItemsViewModel::setStyle,
                    notes = itemsUIState.notes,
                    onNotesChange = editItemsViewModel::setNotes,
                    material = itemsUIState.materialText,
                    onMaterialChange = editItemsViewModel::setMaterial,
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
                        Text("No picture yet", style = Typography.bodyMedium)
                      }
                    })
              }
        })

    if (itemsUIState.isLoading) {
      CenteredLoadingState(message = "Uploading item...")
    }
  }
}

// --- Extracted composables to reduce cognitive complexity ---
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
    currency: String,
    onCurrencyChange: (String) -> Unit,
    link: String,
    onLinkChange: (String) -> Unit,
    condition: String,
    onConditionChange: (String) -> Unit,
    size: String,
    onSizeChange: (String) -> Unit,
    fitType: String,
    onFitTypeChange: (String) -> Unit,
    style: String,
    onStyleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    material: String,
    onMaterialChange: (String) -> Unit,
    // Preview flags
    additionalExpandedPreview: Boolean = false,
    conditionMenuExpandedPreview: Boolean = false,
) {
  val detailsTags =
      AdditionalDetailsTags(
          toggle = EditItemsScreenTestTags.ADDITIONAL_DETAILS_TOGGLE,
          section = EditItemsScreenTestTags.ADDITIONAL_DETAILS_SECTION,
          conditionField = EditItemsScreenTestTags.INPUT_ITEM_CONDITION,
          materialField = EditItemsScreenTestTags.INPUT_ITEM_MATERIAL,
          fitTypeField = EditItemsScreenTestTags.INPUT_ITEM_FIT_TYPE,
          fitTypeDropdown = EditItemsScreenTestTags.FIT_TYPE_SUGGESTIONS,
          styleField = EditItemsScreenTestTags.INPUT_ITEM_STYLE,
          styleDropdown = EditItemsScreenTestTags.STYLE_SUGGESTIONS,
          notesField = EditItemsScreenTestTags.INPUT_ITEM_NOTES)

  ItemFieldsListLayout(
      modifier = modifier,
      layoutConfig =
          ItemFieldsLayoutConfig(
              horizontalAlignment = Alignment.Start,
              verticalArrangement = Arrangement.spacedBy(10.dp)),
      slots =
          ItemFieldsListSlots(
              imagePicker = {
                ImagePickerRow(onPickFromGallery = onPickFromGallery, onOpenCamera = onOpenCamera)
              },
              categoryField = {
                CategoryField(
                    category = category,
                    onChange = onCategoryChange,
                    testTag = EditItemsScreenTestTags.INPUT_ITEM_CATEGORY)
              },
              typeField = {
                TypeField(
                    type = type,
                    suggestions = suggestions,
                    onChange = onTypeChange,
                    testTag = EditItemsScreenTestTags.INPUT_ITEM_TYPE,
                    dropdownTestTag = EditItemsScreenTestTags.TYPE_SUGGESTIONS,
                    onFocus = onTypeFocus)
              },
              primaryFields = {
                ItemPrimaryFields(
                    brand = brand,
                    onBrandChange = onBrandChange,
                    brandTag = EditItemsScreenTestTags.INPUT_ITEM_BRAND,
                    price = price,
                    onPriceChange = onPriceChange,
                    priceTag = EditItemsScreenTestTags.INPUT_ITEM_PRICE,
                    currency = currency,
                    onCurrencyChange = onCurrencyChange,
                    currencyTag = EditItemsScreenTestTags.INPUT_ITEM_CURRENCY,
                    size = size,
                    onSizeChange = onSizeChange,
                    sizeTag = EditItemsScreenTestTags.INPUT_ITEM_SIZE,
                    link = link,
                    onLinkChange = onLinkChange,
                    linkTag = EditItemsScreenTestTags.INPUT_ITEM_LINK)
              },
              additionalDetails = {
                AdditionalDetailsSection(
                    state =
                        AdditionalDetailsState(
                            condition = condition,
                            onConditionChange = onConditionChange,
                            material = material,
                            onMaterialChange = onMaterialChange,
                            fitType = fitType,
                            onFitTypeChange = onFitTypeChange,
                            style = style,
                            onStyleChange = onStyleChange,
                            notes = notes,
                            onNotesChange = onNotesChange,
                            expandedInitially = additionalExpandedPreview,
                            condExpandedInitially = conditionMenuExpandedPreview),
                    tags = detailsTags)
              },
              actionButtons = { Spacer(modifier = Modifier.height(24.dp)) }))
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

@Preview(name = "Edit Items", showBackground = true)
@Composable
fun EditItemsScreenSmallPreview() {
  MaterialTheme {
    val maxImageSize = 180.dp
    val minImageSize = 80.dp

    val currentImageSizeState = remember { mutableStateOf(maxImageSize) }
    val imageScaleState = remember { mutableFloatStateOf(1f) }

    val nestedScrollConnection =
        rememberImageResizeScrollConnection(
            currentImageSize = currentImageSizeState,
            imageScale = imageScaleState,
            minImageSize = minImageSize,
            maxImageSize = maxImageSize)

    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
          topBar = {
            OOTDTopBar(centerText = "EDIT ITEMS", leftComposable = { BackArrow(onBackClick = {}) })
          },
          content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).nestedScroll(nestedScrollConnection)) {
              FieldsList(
                  modifier =
                      Modifier.fillMaxWidth().padding(16.dp).offset {
                        IntOffset(0, currentImageSizeState.value.roundToPx())
                      },
                  onPickFromGallery = {},
                  onOpenCamera = {},
                  category = "Tops",
                  onCategoryChange = {},
                  type = "Hoodie",
                  suggestions = listOf("Hoodie", "Jacket", "T-Shirt"),
                  onTypeChange = {},
                  onTypeFocus = {},
                  brand = "BrandY",
                  onBrandChange = {},
                  price = 49.9,
                  onPriceChange = {},
                  currency = "EUR",
                  onCurrencyChange = {},
                  link = "https://example.com/item",
                  onLinkChange = {},
                  condition = "New",
                  onConditionChange = {},
                  size = "M",
                  onSizeChange = {},
                  fitType = "Regular",
                  onFitTypeChange = {},
                  style = "Casual",
                  onStyleChange = {},
                  notes = "Great condition",
                  onNotesChange = {},
                  material = "Cotton 80%, Wool 20%",
                  onMaterialChange = {},
                  additionalExpandedPreview = true,
                  conditionMenuExpandedPreview = false)

              ItemsImagePreview(
                  localUri = null,
                  remoteUrl = "",
                  maxImageSize = maxImageSize,
                  imageScale = imageScaleState.floatValue,
                  currentSize = currentImageSizeState.value,
                  testTag = EditItemsScreenTestTags.PLACEHOLDER_PICTURE)
            }
          })
    }
  }
}
