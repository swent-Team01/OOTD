package com.android.ootd.ui.post

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.Primary

object PreviewItemScreenTestTags {
  const val EMPTY_ITEM_LIST_MSG = "emptyItemList"
  const val ITEM_LIST = "itemList"
  const val POST_BUTTON = "postFab"

  const val EXPAND_ICON = "expandCard"
  const val IMAGE_ITEM_PREVIEW = "imageItemPreview"
  const val EDIT_ITEM_BUTTON = "editItemButton"
  const val CREATE_ITEM_BUTTON = "createItemButton"

  fun getTestTagForItem(item: Item): String = "item${item.uuid}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewItemScreen(outfitPreviewViewModel: OutfitPreviewViewModel = viewModel()) {
  val context = LocalContext.current
  val uiState by outfitPreviewViewModel.uiState.collectAsState()
  val itemsList = uiState.items

  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { message ->
      // Show error message as a toast or snackbar
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      outfitPreviewViewModel.clearErrorMessage()
    }
  }
  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = "OOTD",
                  //                      style = Theme.typography.titleSmall.copy(
                  //                            color = Primary,
                  //                            fontSize =
                  // MaterialTheme.typography.displayLarge.fontSize * 1.5f,
                  //                            fontWeight = FontWeight.SemiBold
                  //                        )
              )
            },
            navigationIcon = {
              IconButton(onClick = { /* Handle navigation icon press */}) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "go back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            scrollBehavior = scrollBehavior)
      }) { innerPadding ->
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
                  OutfitItem(item = itemsList[index], onClick = { /* Handle item click */})
                }
              }
        } else {

          Column(
              modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center) {
                Text(
                    modifier =
                        Modifier.widthIn(220.dp)
                            .testTag(PreviewItemScreenTestTags.EMPTY_ITEM_LIST_MSG),
                    text = "What are you wearing today ?",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                        ))

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { /* Handle navigation icon press */},
                    modifier =
                        Modifier.height(47.dp)
                            .width(140.dp)
                            .testTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create item",
                                tint = White,
                                modifier = Modifier.size(20.dp))
                            Text(
                                text = "Add Item",
                                modifier = Modifier.align(Alignment.CenterVertically))
                          }
                    }
              }
        }
      }
}

@Composable
fun OutfitItem(item: Item, onClick: () -> Unit) {
  var isExpanded by remember { mutableStateOf(false) }

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 8.dp)
              .animateContentSize()
              .testTag(PreviewItemScreenTestTags.getTestTagForItem(item)),
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSecondary),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          // 🟣 Main content (image + text)
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Start,
              verticalAlignment = Alignment.CenterVertically) {
                // Left: image
                AsyncImage(
                    model = item.image,
                    contentDescription = "Item image",
                    modifier =
                        Modifier.size(100.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .testTag(PreviewItemScreenTestTags.IMAGE_ITEM_PREVIEW),
                    placeholder = painterResource(R.drawable.ic_photo_placeholder),
                    contentScale = ContentScale.Crop)

                Spacer(modifier = Modifier.width(12.dp))

                // Right: text column
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)) {
                      Text(
                          text = item.category,
                          style =
                              MaterialTheme.typography.titleLarge.copy(
                                  fontWeight = FontWeight.SemiBold,
                                  color = MaterialTheme.colorScheme.onSurface))
                      Text(
                          text = item.type ?: "Item Type",
                          style =
                              MaterialTheme.typography.bodyMedium.copy(
                                  color = MaterialTheme.colorScheme.onSurface))

                      // 🟢 Expanded section
                      AnimatedVisibility(visible = isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          item.brand?.let {
                            Text(
                                text = it,
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                          }

                          if (item.material.isNotEmpty()) {
                            Text(
                                text = item.material.joinToString { m -> m?.name ?: "" },
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                          }
                          item.price?.let {
                            Text(
                                text = "CHF ${String.format("%.2f", it)}",
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant))
                          }
                          item.link?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall.copy(color = Primary))
                          }
                        }
                      }
                    }
              }

          // 🟣 Top-right overlay icons (floating over content)
          Row(
              modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onClick,
                    modifier =
                        Modifier.size(24.dp).testTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.Edit,
                          contentDescription = "Edit item",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier =
                        Modifier.size(24.dp).testTag(PreviewItemScreenTestTags.EXPAND_ICON)) {
                      Icon(
                          imageVector =
                              if (isExpanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                          contentDescription = if (isExpanded) "Collapse" else "Expand",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
              }
        }
      }
}
