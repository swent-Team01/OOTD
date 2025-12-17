package com.android.ootd.ui.post.items

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.R
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.post.rememberImageResizeScrollConnection
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.ImageSelectionDialog
import com.android.ootd.utils.composables.LoadingScreen
import com.android.ootd.utils.composables.OOTDTopBar

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
  const val ADDITIONAL_DETAILS_TOGGLE = "additionalDetailsToggle"
  const val ADDITIONAL_DETAILS_SECTION = "additionalDetailsSection"
  const val INPUT_CONDITION = "inputItemCondition"
  const val INPUT_SIZE = "inputItemSize"
  const val INPUT_FIT_TYPE = "inputItemFitType"
  const val INPUT_STYLE = "inputItemStyle"
  const val INPUT_NOTES = "inputItemNotes"
  const val STYLE_SUGGESTIONS = "styleSuggestions"
  const val FIT_TYPE_SUGGESTIONS = "fitTypeSuggestions"
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
        topBar = {
          OOTDTopBar(
              textModifier =
                  modifier.testTag(AddItemScreenTestTags.TITLE_ADD).clickable {
                    fillItemData(addItemsViewModel)
                  },
              centerText = "ADD ITEMS",
              leftComposable = {
                BackArrow(
                    onBackClick = goBack,
                    modifier = Modifier.testTag(AddItemScreenTestTags.GO_BACK_BUTTON))
              })
        },
        floatingActionButton = {
          val isEnabled = !itemsUIState.isLoading && (overridePhoto || itemsUIState.isAddingValid)

          ExtendedFloatingActionButton(
              onClick = { if (isEnabled) addItemsViewModel.onAddItemClick(context = context) },
              containerColor = if (isEnabled) Primary else Tertiary,
              icon = {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item", tint = White)
              },
              text = { Text(text = "Add new item", color = White) },
              modifier = Modifier.testTag(AddItemScreenTestTags.ADD_ITEM_BUTTON))
        },
        content = { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding).nestedScroll(nestedScrollConnection)) {
            ItemFieldsListLayout(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(18.dp)
                        .offset { IntOffset(0, currentImageSizeState.value.roundToPx()) }
                        .testTag(AddItemScreenTestTags.ALL_FIELDS),
                layoutConfig =
                    ItemFieldsLayoutConfig(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top),
                slots =
                    ItemFieldsListSlots(
                        imagePicker = {
                          ImagePickerRow(
                              showDialog = showDialog,
                              onShowDialogChange = { showDialog = it },
                              onTakePhoto = {
                                showDialog = false
                                showCamera = true
                              },
                              onPickFromGallery = {
                                showDialog = false
                                galleryLauncher.launch("image/*")
                              })
                        },
                        categoryField = {
                          CategoryField(
                              category = itemsUIState.category,
                              onChange = addItemsViewModel::setCategory,
                              testTag = AddItemScreenTestTags.INPUT_CATEGORY,
                              invalidCategory = itemsUIState.invalidCategory,
                              onValidate = addItemsViewModel::validateCategory)
                        },
                        typeField = {
                          TypeField(
                              type = itemsUIState.type,
                              suggestions = itemsUIState.typeSuggestion,
                              onChange = {
                                addItemsViewModel.setType(it)
                                addItemsViewModel.updateTypeSuggestions(it)
                              },
                              testTag = AddItemScreenTestTags.INPUT_TYPE,
                              dropdownTestTag = AddItemScreenTestTags.TYPE_SUGGESTIONS,
                              expandOnChange = true)
                        },
                        primaryFields = {
                          ItemPrimaryFields(
                              brand = itemsUIState.brand,
                              onBrandChange = addItemsViewModel::setBrand,
                              brandTag = AddItemScreenTestTags.INPUT_BRAND,
                              price = itemsUIState.price,
                              onPriceChange = addItemsViewModel::setPrice,
                              priceTag = AddItemScreenTestTags.INPUT_PRICE,
                              currency = itemsUIState.currency,
                              onCurrencyChange = addItemsViewModel::setCurrency,
                              currencyTag = AddItemScreenTestTags.INPUT_CURRENCY,
                              size = itemsUIState.size,
                              onSizeChange = addItemsViewModel::setSize,
                              sizeTag = AddItemScreenTestTags.INPUT_SIZE,
                              link = itemsUIState.link,
                              onLinkChange = addItemsViewModel::setLink,
                              linkTag = AddItemScreenTestTags.INPUT_LINK)
                        },
                        additionalDetails = {
                          AdditionalDetailsSection(
                              state =
                                  AdditionalDetailsState(
                                      condition = itemsUIState.condition,
                                      onConditionChange = addItemsViewModel::setCondition,
                                      material = itemsUIState.materialText,
                                      onMaterialChange = addItemsViewModel::setMaterial,
                                      fitType = itemsUIState.fitType,
                                      onFitTypeChange = addItemsViewModel::setFitType,
                                      style = itemsUIState.style,
                                      onStyleChange = addItemsViewModel::setStyle,
                                      notes = itemsUIState.notes,
                                      onNotesChange = addItemsViewModel::setNotes),
                              tags =
                                  AdditionalDetailsTags(
                                      toggle = AddItemScreenTestTags.ADDITIONAL_DETAILS_TOGGLE,
                                      section = AddItemScreenTestTags.ADDITIONAL_DETAILS_SECTION,
                                      conditionField = AddItemScreenTestTags.INPUT_CONDITION,
                                      materialField = AddItemScreenTestTags.INPUT_MATERIAL,
                                      fitTypeField = AddItemScreenTestTags.INPUT_FIT_TYPE,
                                      fitTypeDropdown = AddItemScreenTestTags.FIT_TYPE_SUGGESTIONS,
                                      styleField = AddItemScreenTestTags.INPUT_STYLE,
                                      styleDropdown = AddItemScreenTestTags.STYLE_SUGGESTIONS,
                                      notesField = AddItemScreenTestTags.INPUT_NOTES))
                        },
                        actionButtons = { Spacer(modifier = Modifier.height(24.dp)) }))

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

    if (itemsUIState.isLoading) {
      LoadingScreen(contentDescription = "Uploading item...")
    }
  }
}

@Composable
private fun ImagePickerRow(
    showDialog: Boolean,
    onShowDialogChange: (Boolean) -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    Button(
        onClick = { onShowDialogChange(true) },
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
    ImageSelectionDialog(
        modifier = Modifier.testTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG),
        onDismissRequest = { onShowDialogChange(false) },
        onTakePhoto = onTakePhoto,
        onPickFromGallery = onPickFromGallery,
        takePhotoTag = AddItemScreenTestTags.TAKE_A_PHOTO,
        pickGalleryTag = AddItemScreenTestTags.PICK_FROM_GALLERY)
  }
}

/**
 * Fills the item form with mock data for demonstration purposes. This function is triggered when
 * clicking on the "ADD ITEMS" title.
 */
private fun fillItemData(viewModel: AddItemsViewModel) {
  viewModel.setCategory("Clothing")
  viewModel.setType("T-Shirt")
  viewModel.setBrand("Nike")
  viewModel.setPrice(49.99)
  viewModel.setCurrency("USD")
  viewModel.setSize("M")
  viewModel.setLink("https://www.nike.com/demo-tshirt")
  viewModel.setCondition("New")
  viewModel.setMaterial("Cotton")
  viewModel.setFitType("Regular")
  viewModel.setStyle("Casual")
  viewModel.setNotes("Perfect item for a demonstration!")
}
