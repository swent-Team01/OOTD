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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.R
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.Primary

object AddItemScreenTestTags {
  const val INPUT_TYPE = "inputItemType"
  const val INPUT_BRAND = "inputItemBrand"
  const val INPUT_PRICE = "inputItemPrice"
  const val INPUT_CURRENCY = "inputItemCurrency"
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
    postUuid: String,
    modifier: Modifier = Modifier,
    overridePhoto: Boolean = false,
    addItemsViewModel: AddItemsViewModel =
        viewModel(factory = AddItemsViewModelFactory(overridePhoto = overridePhoto)),
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

  // Initialize post ID and suggestions
  LaunchedEffect(postUuid) { addItemsViewModel.initPostUuid(postUuid) }
  LaunchedEffect(Unit) { addItemsViewModel.initTypeSuggestions(context) }

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
        if (uri != null) addItemsViewModel.setPhoto(uri)
      }

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
                onCategoryChange = { addItemsViewModel.setCategory(it) },
                onCategoryValidate = { addItemsViewModel.validateCategory() },

                // type
                type = itemsUIState.type,
                typeSuggestions = itemsUIState.typeSuggestion,
                onTypeChange = {
                  addItemsViewModel.setType(it)
                  addItemsViewModel.updateTypeSuggestions(it)
                },

                // brand / price / link / material / currency
                brand = itemsUIState.brand,
                onBrandChange = addItemsViewModel::setBrand,
                price = itemsUIState.price,
                onPriceChange = addItemsViewModel::setPrice,
                currency = itemsUIState.currency,
                onCurrencyChange = addItemsViewModel::setCurrency,
                link = itemsUIState.link,
                onLinkChange = addItemsViewModel::setLink,
                material = itemsUIState.materialText,
                onMaterialChange = addItemsViewModel::setMaterial,

                // add
                isAddEnabled = overridePhoto || itemsUIState.isAddingValid,
                onAddClick = addItemsViewModel::onAddItemClick,
            )

            // top image preview overlays the list
            ItemsImagePreview(
                localUri = itemsUIState.localPhotoUri,
                remoteUrl = itemsUIState.image.imageUrl,
                maxImageSize = maxImageSize,
                imageScale = imageScaleState.floatValue,
                currentSize = currentImageSizeState.value,
                testTag = AddItemScreenTestTags.IMAGE_PREVIEW,
                placeholderIcon = {
                  Icon(
                      painter = painterResource(R.drawable.ic_photo_placeholder),
                      contentDescription = "Placeholder icon",
                      modifier = Modifier.size(80.dp))
                })
          }
        })

    LoadingOverlay(visible = itemsUIState.isLoading)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemTopBar(modifier: Modifier, goBack: () -> Unit) {
  CenterAlignedTopAppBar(
      title = {
        Text(
            text = "ADD ITEM",
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
    onCategoryChange: (String) -> Unit,
    onCategoryValidate: () -> Unit,
    type: String,
    typeSuggestions: List<String>,
    onTypeChange: (String) -> Unit,
    brand: String,
    onBrandChange: (String) -> Unit,
    price: Double,
    onPriceChange: (Double) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
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
          onChange = onCategoryChange,
          testTag = AddItemScreenTestTags.INPUT_CATEGORY,
          invalidCategory = invalidCategory,
          onValidate = onCategoryValidate,
          dropdownTestTag = AddItemScreenTestTags.CATEGORY_SUGGESTION)
    }

    item {
      TypeField(
          type = type,
          suggestions = typeSuggestions,
          onChange = onTypeChange,
          testTag = AddItemScreenTestTags.INPUT_TYPE,
          dropdownTestTag = AddItemScreenTestTags.TYPE_SUGGESTIONS,
          expandOnChange = true)
    }

    item {
      BrandField(
          brand = brand, onChange = onBrandChange, testTag = AddItemScreenTestTags.INPUT_BRAND)
    }

    item {
      PriceField(
          price = price, onChange = onPriceChange, testTag = AddItemScreenTestTags.INPUT_PRICE)
    }

    item {
      CurrencyField(
          currency = currency,
          onChange = onCurrencyChange,
          testTag = AddItemScreenTestTags.INPUT_CURRENCY)
    }

    item {
      LinkField(link = link, onChange = onLinkChange, testTag = AddItemScreenTestTags.INPUT_LINK)
    }

    item {
      MaterialField(
          materialText = material,
          onChange = onMaterialChange,
          testTag = AddItemScreenTestTags.INPUT_MATERIAL)
    }

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
          Text(text = "Upload a picture of the Item*", color = White)
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

@Preview(name = "Add Items", showBackground = true)
@Composable
fun AddItemsScreenSmallPreview() {
  MaterialTheme {
    val maxImageSize = 180.dp
    val minImageSize = 80.dp

    var showDialog by remember { mutableStateOf(false) }
    val currentImageSizeState = remember { mutableStateOf(maxImageSize) }
    val imageScaleState = remember { mutableFloatStateOf(1f) }

    val nestedScrollConnection =
        _root_ide_package_.com.android.ootd.ui.post.rememberImageResizeScrollConnection(
            currentImageSize = currentImageSizeState,
            imageScale = imageScaleState,
            minImageSize = minImageSize,
            maxImageSize = maxImageSize)

    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
          topBar = { AddItemTopBar(Modifier, goBack = {}) },
          content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).nestedScroll(nestedScrollConnection)) {
              FieldsList(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(18.dp)
                          .offset { IntOffset(0, currentImageSizeState.value.roundToPx()) }
                          .testTag(AddItemScreenTestTags.ALL_FIELDS),
                  showDialog = showDialog,
                  onShowDialogChange = { showDialog = it },
                  onTakePhoto = { showDialog = false },
                  onPickFromGallery = { showDialog = false },
                  category = "Tops",
                  invalidCategory = null,
                  onCategoryChange = {},
                  onCategoryValidate = {},
                  type = "T-Shirt",
                  typeSuggestions = listOf("T-Shirt", "Sweater", "Jacket"),
                  onTypeChange = {},
                  brand = "BrandX",
                  onBrandChange = {},
                  price = 19.99,
                  onPriceChange = {},
                  currency = "CHF",
                  onCurrencyChange = {},
                  link = "https://example.com/item",
                  onLinkChange = {},
                  material = "Cotton",
                  onMaterialChange = {},
                  isAddEnabled = true,
                  onAddClick = {})
              ItemsImagePreview(
                  localUri = null,
                  remoteUrl = "",
                  maxImageSize = maxImageSize,
                  imageScale = imageScaleState.floatValue,
                  currentSize = currentImageSizeState.value,
                  testTag = AddItemScreenTestTags.IMAGE_PREVIEW,
                  placeholderIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_photo_placeholder),
                        contentDescription = "Placeholder icon",
                        modifier = Modifier.size(64.dp))
                  })
            }
          })

      LoadingOverlay(visible = false)
    }
  }
}
