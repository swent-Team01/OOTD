package com.android.ootd.ui.post.items

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.CategoryNormalizer

/** Reusable category dropdown field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryField(
    category: String,
    onChange: (String) -> Unit,
    testTag: String,
    invalidCategory: String? = null,
    onValidate: (() -> Unit)? = null,
    dropdownTestTag: String? = null
) {
  var expanded by remember { mutableStateOf(false) }
  val categories = CategoryNormalizer.VALID_CATEGORIES

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = it },
      modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = category,
            onValueChange = {}, // Read-only: selection only
            readOnly = true,
            label = { Text("Item Category*") },
            placeholder = { Text("Select the Item Category") },
            isError = invalidCategory != null,
            supportingText =
                invalidCategory?.let { msg ->
                  { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag(testTag),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors())

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier =
                (dropdownTestTag?.let { Modifier.fillMaxWidth().testTag(it) }
                    ?: Modifier.fillMaxWidth())) {
              categories.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                      onChange(option)
                      onValidate?.invoke()
                      expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding)
              }
            }
      }
}

/** Reusable text field for type input with autocomplete suggestions */
@Composable
fun TypeField(
    type: String,
    suggestions: List<String>,
    onChange: (String) -> Unit,
    testTag: String,
    dropdownTestTag: String,
    onFocus: (() -> Unit)? = null,
    expandOnChange: Boolean = false
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = type,
        onValueChange = {
          onChange(it)
          expanded = expandOnChange && it.isNotBlank() || !expandOnChange
        },
        label = { Text("Type") },
        placeholder = { Text("Enter a type") },
        modifier =
            Modifier.fillMaxWidth()
                .testTag(testTag)
                .then(
                    onFocus?.let {
                      Modifier.onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                          onFocus()
                          expanded = true
                        }
                      }
                    } ?: Modifier),
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        colors = commonTextFieldColors())

    DropdownMenu(
        expanded = expanded && suggestions.isNotEmpty(),
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth().testTag(dropdownTestTag),
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

/** Reusable generic text field with common styling */
@Composable
private fun CommonTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String,
    testTag: String
) {
  OutlinedTextField(
      value = value,
      onValueChange = onChange,
      label = { Text(label) },
      placeholder = { Text(placeholder) },
      textStyle =
          MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
      colors = commonTextFieldColors(),
      modifier = Modifier.fillMaxWidth().testTag(testTag))
}

/** Reusable text field for brand input */
@Composable
fun BrandField(brand: String, onChange: (String) -> Unit, testTag: String) {
  CommonTextField(
      value = brand,
      onChange = onChange,
      label = "Brand",
      placeholder = "Enter a brand",
      testTag = testTag)
}

/** Reusable text field for material input */
@Composable
fun MaterialField(materialText: String, onChange: (String) -> Unit, testTag: String) {
  CommonTextField(
      value = materialText,
      onChange = onChange,
      label = "Material",
      placeholder = "E.g., Cotton 80%, Wool 20%",
      testTag = testTag)
}

/** Reusable text field for link input */
@Composable
fun LinkField(link: String, onChange: (String) -> Unit, testTag: String) {
  CommonTextField(
      value = link,
      onChange = onChange,
      label = "Link",
      placeholder = "e.g., https://example.com",
      testTag = testTag)
}

/** Reusable loading overlay with progress indicator */
@Composable
fun LoadingOverlay(visible: Boolean) {
  if (!visible) return
  Box(
      modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = Primary)
          Spacer(modifier = Modifier.height(12.dp))
          Text("Uploading item...", color = Color.White, style = Typography.bodyLarge)
        }
      }
}

/** Image preview (local URI, remote URL, or placeholder) */
@Composable
fun BoxScope.ItemsImagePreview(
    localUri: Uri?,
    remoteUrl: String,
    maxImageSize: Dp,
    imageScale: Float,
    currentSize: Dp,
    testTag: String,
    placeholderIcon: @Composable () -> Unit = {
      Icon(
          painter = painterResource(R.drawable.ic_photo_placeholder),
          contentDescription = "Placeholder icon",
          modifier = Modifier.size(80.dp))
    }
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
              .clip(RoundedCornerShape(12.dp))
              .border(6.dp, Primary, RoundedCornerShape(12.dp))
              .background(Secondary)
              .testTag(testTag),
      contentAlignment = Alignment.Center) {
        when {
          localUri != null ->
              AsyncImage(
                  model = localUri,
                  contentDescription = "Selected photo",
                  modifier = Modifier.matchParentSize(),
                  contentScale = ContentScale.Crop)
          remoteUrl.isNotEmpty() ->
              AsyncImage(
                  model = remoteUrl,
                  contentDescription = "Uploaded photo",
                  modifier = Modifier.matchParentSize(),
                  contentScale = ContentScale.Crop)
          else -> placeholderIcon()
        }
      }
}

/** Common text field colors for all item input fields */
@Composable
fun commonTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
        unfocusedBorderColor = MaterialTheme.colorScheme.tertiary,
        disabledBorderColor = MaterialTheme.colorScheme.tertiary,
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.primary,
        focusedPlaceholderColor = MaterialTheme.colorScheme.primary,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.primary,
        unfocusedTextColor = MaterialTheme.colorScheme.primary)
