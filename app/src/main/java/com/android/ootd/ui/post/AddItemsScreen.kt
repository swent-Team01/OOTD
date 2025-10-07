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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemsScreen(addItemsViewModel: AddItemsViewModel = viewModel()) {

  val itemsUIState by addItemsViewModel.uiState.collectAsState()
  val errorMsg = itemsUIState.errorMessage

  var expanded by remember { mutableStateOf(false) }
  var cameraUri by remember { mutableStateOf<Uri?>(null) }

  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        uri?.let { addItemsViewModel.setPhoto(it) }
      }

  val cameraLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success
        ->
        if (success && cameraUri != null) addItemsViewModel.setPhoto(cameraUri!!)
      }

  val context = LocalContext.current
  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      // For example, using Toast:
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      addItemsViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Add Items", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
              IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back")
              }
            })
      },
      content = { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(220.dp)
                          .clip(RoundedCornerShape(16.dp))
                          .background(
                              MaterialTheme.colorScheme
                                  .background), // light gray placeholder background
                  contentAlignment = Alignment.Center) {
                    if (itemsUIState.image != Uri.EMPTY) {
                      // 🖼️ Show selected photo
                      AsyncImage(
                          model = itemsUIState.image,
                          contentDescription = "Selected image",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop)
                    } else {
                      // ➕ Placeholder when no image
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap to add a photo",
                            style = MaterialTheme.typography.bodyMedium)
                      }
                    }
                  }
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)) {
                          Text("Gallery")
                        }
                    Button(
                        onClick = {
                          val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                          val uri =
                              FileProvider.getUriForFile(
                                  context, "${context.packageName}.fileprovider", file)
                          cameraUri = uri
                          cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f)) {
                          Text("Camera")
                        }
                  }

              OutlinedTextField(
                  value = itemsUIState.category,
                  onValueChange = { addItemsViewModel.setCategory(it) },
                  label = { Text("Category") },
                  placeholder = { Text("e.g., Clothes") },
                  modifier = Modifier.fillMaxWidth())

              Box {
                OutlinedTextField(
                    value = itemsUIState.type,
                    onValueChange = {
                      addItemsViewModel.setType(it)
                      addItemsViewModel.updateTypeSuggestions(it)
                      expanded = true
                    },
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth())
                DropdownMenu(
                    expanded = expanded && itemsUIState.suggestions.isNotEmpty(),
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()) {
                      itemsUIState.suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                              addItemsViewModel.setType(suggestion)
                              expanded = false
                            })
                      }
                    }
              }

              OutlinedTextField(
                  value = itemsUIState.brand,
                  onValueChange = { addItemsViewModel.setBrand(it) },
                  label = { Text("Brand") },
                  modifier = Modifier.fillMaxWidth())

              OutlinedTextField(
                  value = if (itemsUIState.price == 0.0) "" else itemsUIState.price.toString(),
                  onValueChange = {
                    val price = it.toDoubleOrNull() ?: 0.0
                    addItemsViewModel.setPrice(price)
                  },
                  label = { Text("Price") },
                  placeholder = { Text("e.g., 49.99") },
                  modifier = Modifier.fillMaxWidth())

              OutlinedTextField(
                  value = itemsUIState.link,
                  onValueChange = { addItemsViewModel.setLink(it) },
                  label = { Text("Link") },
                  placeholder = { Text("e.g., https://zara.com") },
                  modifier = Modifier.fillMaxWidth())

              Button(
                  onClick = { addItemsViewModel.canAddItems() },
                  enabled = itemsUIState.image != Uri.EMPTY && itemsUIState.category.isNotEmpty(),
                  modifier = Modifier.fillMaxWidth()) {
                    Text("Add Item")
                  }
            }
      })
}
