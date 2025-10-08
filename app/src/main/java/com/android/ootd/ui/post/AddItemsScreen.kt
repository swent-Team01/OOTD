package com.android.ootd.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemsScreen(addItemsViewModel: AddItemsViewModel = viewModel()) {

  val context = LocalContext.current
  var typeExpanded by remember { mutableStateOf(false) }
  var categoryExpanded by remember { mutableStateOf(false) }
  val itemsUIState by addItemsViewModel.uiState.collectAsState()
  var showDialog by remember { mutableStateOf(false) }

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
        TopAppBar(
            title = {
              Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                      text = "ADD ITEMS",
                      style = MaterialTheme.typography.displayLarge,
                      color = Primary)
                }
              }
            },
            navigationIcon = {
              Box(modifier = Modifier.padding(start = 4.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = {}) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                      contentDescription = "Back",
                  )
                }
              }
            })
      },
      content = { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(18.dp)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Box(
                  modifier =
                      Modifier.size(180.dp)
                          .clip(RoundedCornerShape(16.dp))
                          .border(4.dp, Tertiary, RoundedCornerShape(16.dp))
                          .background(Color.White)
                          .align(Alignment.CenterHorizontally),
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

              Spacer(Modifier.height(8.dp))

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(24.dp),
                ) {
                  Icon(
                      painter = painterResource(R.drawable.ic_photo_placeholder),
                      contentDescription = "Upload",
                      tint = White,
                      modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(8.dp))
                  Text(text = "Upload a picture of the Item", color = White)
                }

                if (showDialog) {
                  AlertDialog(
                      onDismissRequest = { showDialog = false },
                      title = { Text("Select Image") },
                      text = {
                        Column {
                          TextButton(
                              onClick = {
                                // Take a photo
                                val file =
                                    File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                                val uri =
                                    FileProvider.getUriForFile(
                                        context, "${context.packageName}.provider", file)
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                                showDialog = false
                              }) {
                                Text("📸 Take a Photo")
                              }

                          TextButton(
                              onClick = {
                                // Pick from gallery
                                galleryLauncher.launch("image/*")
                                showDialog = false
                              }) {
                                Text("🖼️ Choose from Gallery")
                              }
                        }
                      },
                      confirmButton = {},
                      dismissButton = {})
                }
              }

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
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                      }
                    },
                    modifier =
                        Modifier.fillMaxWidth().onFocusChanged { focusState ->
                          if (!focusState.isFocused && itemsUIState.category.isNotBlank()) {
                            addItemsViewModel.validateCategory()
                          }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                              if (itemsUIState.category.isNotBlank()) {
                                addItemsViewModel.validateCategory()
                              }
                              categoryExpanded = false
                            }))

                DropdownMenu(
                    expanded = categoryExpanded && itemsUIState.categorySuggestion.isNotEmpty(),
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.fillMaxWidth(),
                    properties = PopupProperties(focusable = false)) {
                      itemsUIState.categorySuggestion.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                              addItemsViewModel.setCategory(suggestion)
                              categoryExpanded = false
                            })
                      }
                    }
              }

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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true)

                DropdownMenu(
                    expanded = typeExpanded && itemsUIState.typeSuggestion.isNotEmpty(),
                    onDismissRequest = { typeExpanded = false },
                    modifier = Modifier.fillMaxWidth(),
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

              OutlinedTextField(
                  value = itemsUIState.brand,
                  onValueChange = {},
                  label = { Text("Brand") },
                  placeholder = { Text("Enter a brand") },
                  modifier = Modifier.fillMaxWidth())

              OutlinedTextField(
                  value = itemsUIState.price.toString(),
                  onValueChange = {},
                  label = { Text("Price") },
                  placeholder = { Text("Enter a price") },
                  modifier = Modifier.fillMaxWidth())

              OutlinedTextField(
                  value = itemsUIState.link,
                  onValueChange = {},
                  label = { Text("Link") },
                  placeholder = { Text("Enter a link") },
                  modifier = Modifier.fillMaxWidth())

              OutlinedTextField(
                  value =
                      itemsUIState.material.joinToString(", ") { "${it.name} ${it.percentage}%" },
                  onValueChange = {},
                  label = { Text("Material") },
                  placeholder = { Text("Enter a material") },
                  modifier = Modifier.fillMaxWidth())

              Spacer(Modifier.height(16.dp))

              Button(
                  onClick = {},
                  modifier =
                      Modifier.height(47.dp).width(140.dp).align(Alignment.CenterHorizontally),
                  colors = ButtonDefaults.buttonColors(containerColor = Primary),
              ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                      Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = "Add",
                          tint = White,
                          modifier = Modifier.size(20.dp))
                      Spacer(Modifier.width(8.dp))

                      Text(text = "Add Item", modifier = Modifier.align(Alignment.CenterVertically))
                    }
              }
            }
      })
}
