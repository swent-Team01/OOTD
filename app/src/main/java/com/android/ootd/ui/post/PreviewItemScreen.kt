package com.android.ootd.ui.post

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.Material
import com.android.ootd.model.map.Location
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.OOTDTopBar
import com.android.ootd.utils.composables.ActionIconButton
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.LoadingScreen
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ShowText

object PreviewItemScreenTestTags {
  const val EMPTY_ITEM_LIST_MSG = "emptyItemList"
  const val ITEM_LIST = "itemList"
  const val POST_BUTTON = "postButton"
  const val EXPAND_ICON = "expandCard"
  const val IMAGE_ITEM_PREVIEW = "imageItemPreview"
  const val EDIT_ITEM_BUTTON = "editItemButton"
  const val REMOVE_ITEM_BUTTON = "removeItemButton"
  const val CREATE_ITEM_BUTTON = "createItemButton"
  const val GO_BACK_BUTTON = "goBackButton"
  const val SCREEN_TITLE = "screenTitle"
  const val ADD_ITEM_DIALOG = "addItemDialog"
  const val CREATE_NEW_ITEM_OPTION = "createNewItemOption"
  const val SELECT_FROM_INVENTORY_OPTION = "selectFromInventoryOption"

  fun getTestTagForItem(item: Item): String = "item${item.itemUuid}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewItemScreen(
    outfitPreviewViewModel: OutfitPreviewViewModel = viewModel(),
    imageUri: String,
    description: String,
    location: Location,
    onEditItem: (String) -> Unit = {},
    onRemoveItem: (String) -> Unit = {},
    onAddItem: (String) -> Unit = {}, // now takes postUuid
    onSelectFromInventory: (String) -> Unit = {}, // new callback for inventory selection
    onPostSuccess: () -> Unit = {},
    onGoBack: (String) -> Unit = {},
    enablePreview: Boolean = false,
    uiStateOverride: PreviewUIState? = null,
    overridePhoto: Boolean = false
) {
  val context = LocalContext.current
  val realUiState by outfitPreviewViewModel.uiState.collectAsState()
  val ui = uiStateOverride ?: realUiState
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val lifecycleOwner = LocalLifecycleOwner.current

  // Initialise ViewModel with args and generate a new postUuid if needed
  if (!enablePreview) {
    LaunchedEffect(Unit) {
      outfitPreviewViewModel.initFromFitCheck(imageUri, description, location)
    }

    // Reload items when coming back from other screens (e.g., after adding an item from inventory)
    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          outfitPreviewViewModel.loadItemsForPost()
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
  }

  // Handle error messages
  if (!enablePreview) {
    LaunchedEffect(ui.errorMessage) {
      ui.errorMessage?.let { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        outfitPreviewViewModel.clearErrorMessage()
      }
    }
  }

  if (!enablePreview) {
    LaunchedEffect(ui.successMessage) {
      ui.successMessage?.let { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        outfitPreviewViewModel.clearErrorMessage()
        if (ui.isPublished) {
          onPostSuccess()
        }
      }
    }
  }

  PreviewItemScreenContent(
      ui = ui,
      scrollBehavior = scrollBehavior,
      onEditItem = onEditItem,
      onRemoveItem =
          onRemoveItem.takeIf { enablePreview } ?: outfitPreviewViewModel::removeItemFromPost,
      onAddItem = onAddItem,
      onSelectFromInventory = onSelectFromInventory,
      onPublish = {
        if (!enablePreview)
            outfitPreviewViewModel.publishPost(overridePhoto = overridePhoto, context = context)
      },
      onGoBack = onGoBack,
      enablePreview = enablePreview,
      overridePhoto = overridePhoto)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewItemScreenContent(
    ui: PreviewUIState,
    scrollBehavior: TopAppBarScrollBehavior,
    onEditItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddItem: (String) -> Unit,
    onSelectFromInventory: (String) -> Unit,
    onPublish: () -> Unit,
    onGoBack: (String) -> Unit,
    enablePreview: Boolean,
    overridePhoto: Boolean = false
) {
  val itemsList = ui.items
  val hasItems = itemsList.isNotEmpty()
  var showAddItemDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
          OOTDTopBar(
              textModifier = Modifier.testTag(PreviewItemScreenTestTags.SCREEN_TITLE),
              leftComposable = {
                BackArrow(
                    onBackClick = { onGoBack(ui.postUuid) },
                    modifier = Modifier.testTag(PreviewItemScreenTestTags.GO_BACK_BUTTON))
              })
        },
        bottomBar = {
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically) {
                if (overridePhoto || hasItems) {
                  Button(
                      onClick = onPublish,
                      modifier =
                          Modifier.height(47.dp)
                              .width(140.dp)
                              .testTag(PreviewItemScreenTestTags.POST_BUTTON),
                      colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Icon(Icons.Default.Check, contentDescription = "Post", tint = White)
                        Spacer(Modifier.width(8.dp))
                        Text("Post", color = White)
                      }
                } else {
                  OutlinedButton(
                      onClick = {},
                      enabled = false,
                      modifier =
                          Modifier.height(47.dp)
                              .width(140.dp)
                              .testTag(PreviewItemScreenTestTags.POST_BUTTON),
                      border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Post (add items first)",
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Post", color = MaterialTheme.colorScheme.primary)
                      }
                }
                Button(
                    onClick = { showAddItemDialog = true },
                    modifier =
                        Modifier.height(47.dp)
                            .width(140.dp)
                            .testTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                      Icon(Icons.Default.Add, contentDescription = "Add Item", tint = White)
                      Spacer(Modifier.width(8.dp))
                      Text("Add Item", color = White)
                    }
              }
        }) { innerPadding ->
          PreviewItemList(
              itemsList = itemsList,
              scrollBehavior = scrollBehavior,
              innerPadding = innerPadding,
              onEditItem = onEditItem,
              onRemoveItem = onRemoveItem)
        }

    // Add Item Dialog
    AddItemDialog(
        showAddItemDialog = showAddItemDialog,
        postUuid = ui.postUuid,
        onAddItem = onAddItem,
        onSelectFromInventory = onSelectFromInventory,
        onDismiss = { showAddItemDialog = false })

    if (ui.isLoading && !enablePreview) {
      LoadingScreen("Publishing your outfit...")
    }
  }
}

@SuppressLint("DefaultLocale")
@Composable
fun OutfitItem(item: Item, onClick: (String) -> Unit, onRemove: () -> Unit) {
  var isExpanded by remember { mutableStateOf(false) }
  val expandIcon =
      if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 8.dp)
              .animateContentSize()
              .testTag(PreviewItemScreenTestTags.getTestTagForItem(item)),
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Start,
              verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = item.image.imageUrl,
                    contentDescription = "Item image",
                    modifier =
                        Modifier.size(100.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .testTag(PreviewItemScreenTestTags.IMAGE_ITEM_PREVIEW),
                    placeholder = painterResource(R.drawable.ic_photo_placeholder),
                    contentScale = ContentScale.Crop)

                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)) {
                      Text(
                          text = item.category,
                          style =
                              Typography.titleLarge.copy(
                                  fontWeight = FontWeight.SemiBold,
                                  color = MaterialTheme.colorScheme.onSurface))
                      Text(
                          text = item.type ?: "Item Type",
                          style =
                              Typography.bodyMedium.copy(
                                  color = MaterialTheme.colorScheme.onSurface))
                      AnimatedVisibility(visible = isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          item.brand?.let {
                            Text(
                                text = it,
                                style =
                                    Typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                          }

                          if (item.material.isNotEmpty()) {
                            Text(
                                text = item.material.joinToString { m -> m?.name ?: "" },
                                style =
                                    Typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                          }
                          item.price?.let {
                            Text(
                                text = "CHF ${String.format("%.2f", it)}",
                                style =
                                    Typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                          }
                          item.link?.let { Text(text = it, style = Typography.bodySmall) }
                        }
                      }
                    }
              }
          Row(
              modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionIconButton(
                    onClick = { onClick(item.itemUuid) },
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit item",
                    modifier =
                        Modifier.size(24.dp).testTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON),
                    tint = MaterialTheme.colorScheme.onSurface)
                ActionIconButton(
                    onClick = { isExpanded = !isExpanded },
                    icon = expandIcon,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.testTag(PreviewItemScreenTestTags.EXPAND_ICON),
                    tint = MaterialTheme.colorScheme.onSurface)
              }
        }
      }
}

// -------------------------
// Compact full-screen preview
// -------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, name = "Preview Item Screen")
@Composable
fun PreviewItemScreenPreview() {
  val sampleItems =
      listOf(
          Item(
              itemUuid = "item1",
              postUuids = listOf("post1"),
              image = ImageData("img1", "https://picsum.photos/seed/cloth1/400/400"),
              category = "Clothing",
              type = "T-Shirt",
              brand = "BrandX",
              price = 19.99,
              material = listOf(Material("Cotton", 100.0)),
              link = "https://example.com/item1",
              ownerId = "user1"),
          Item(
              itemUuid = "item2",
              postUuids = listOf("post1"),
              image = ImageData("img2", "https://picsum.photos/seed/cloth2/400/400"),
              category = "Accessories",
              type = "Watch",
              brand = "BrandY",
              price = 129.0,
              material = emptyList(),
              link = null,
              ownerId = "user1"))
  val sampleState =
      PreviewUIState(
          postUuid = "post1",
          imageUri = "file:///preview.png",
          description = "Comfy casual look",
          items = sampleItems,
          isLoading = false,
          isPublished = false)

  MaterialTheme {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    PreviewItemScreenContent(
        ui = sampleState,
        scrollBehavior = scrollBehavior,
        onEditItem = {},
        onRemoveItem = {},
        onAddItem = {},
        onSelectFromInventory = {},
        onPublish = {},
        onGoBack = {},
        enablePreview = true)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewItemList(
    itemsList: List<Item>,
    scrollBehavior: TopAppBarScrollBehavior,
    innerPadding: PaddingValues,
    onEditItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit
) {
  if (itemsList.isNotEmpty()) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier.fillMaxWidth()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(16.dp)
                .padding(innerPadding)
                .testTag(PreviewItemScreenTestTags.ITEM_LIST)) {
          items(itemsList.size) { index ->
            OutfitItem(
                item = itemsList[index],
                onClick = { onEditItem(itemsList[index].itemUuid) },
                onRemove = { onRemoveItem(itemsList[index].itemUuid) })
          }
        }
  } else {
    EmptyItemPlaceholder()
  }
}

@Composable
private fun EmptyItemPlaceholder() {
  Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        ShowText(
            modifier =
                Modifier.widthIn(220.dp).testTag(PreviewItemScreenTestTags.EMPTY_ITEM_LIST_MSG),
            text = "What are you wearing today ?",
            style =
                Typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        ShowText(
            text = "Don't forget to add your items",
            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
      }
}

@Composable
private fun AddItemDialog(
    showAddItemDialog: Boolean,
    postUuid: String,
    onAddItem: (String) -> Unit,
    onSelectFromInventory: (String) -> Unit,
    onDismiss: () -> Unit
) {
  if (!showAddItemDialog) return

  AlertDialog(
      modifier = Modifier.testTag(PreviewItemScreenTestTags.ADD_ITEM_DIALOG),
      onDismissRequest = onDismiss,
      title = { Text(text = "Add Item to Outfit") },
      text = {
        Column {
          TextButton(
              onClick = {
                onDismiss()
                onAddItem(postUuid)
              },
              modifier = Modifier.testTag(PreviewItemScreenTestTags.CREATE_NEW_ITEM_OPTION)) {
                Text("Create New Item")
              }
          TextButton(
              onClick = {
                onDismiss()
                onSelectFromInventory(postUuid)
              },
              modifier = Modifier.testTag(PreviewItemScreenTestTags.SELECT_FROM_INVENTORY_OPTION)) {
                Text("Select from Inventory")
              }
        }
      },
      confirmButton = {},
      dismissButton = {})
}
