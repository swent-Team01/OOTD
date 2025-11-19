package com.android.ootd.ui.post.items

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.constraintlayout.solver.widgets.Optimizer.enabled
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.CategoryNormalizer

val CONDITION_OPTIONS = listOf("New", "Like new", "Used", "Vintage", "Very Used")
val STYLE_SUGGESTIONS =
    listOf(
        "Casual", "Formal", "Streetwear", "Vintage", "Business", "Sporty", "Smart casual", "Chic")
val FIT_TYPE_SUGGESTIONS =
    listOf("Slim", "Regular", "Relaxed", "Oversized", "Skinny", "Tailored", "Boxy", "Loose")

fun filterDropdownSuggestions(input: String, suggestions: List<String>): List<String> {
  return if (input.isBlank()) suggestions else suggestions.filter { it.startsWith(input, true) }
}

@Composable
fun ItemPrimaryFields(
    brand: String,
    onBrandChange: (String) -> Unit,
    brandTag: String,
    price: Double,
    onPriceChange: (Double) -> Unit,
    priceTag: String,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    currencyTag: String,
    size: String,
    onSizeChange: (String) -> Unit,
    sizeTag: String,
    link: String,
    onLinkChange: (String) -> Unit,
    linkTag: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    BrandField(brand = brand, onChange = onBrandChange, testTag = brandTag)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(modifier = Modifier.weight(1f)) {
        PriceField(price = price, onChange = onPriceChange, testTag = priceTag)
      }
      Box(modifier = Modifier.weight(1f)) {
        CurrencyField(currency = currency, onChange = onCurrencyChange, testTag = currencyTag)
      }
    }

    SizeField(size = size, onChange = onSizeChange, testTag = sizeTag)

    LinkField(link = link, onChange = onLinkChange, testTag = linkTag)
  }
}

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
            modifier =
                Modifier.menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                    .fillMaxWidth()
                    .testTag(testTag),
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
  val filtered =
      remember(type, suggestions) {
        val base =
            if (type.isBlank()) suggestions.take(5)
            else suggestions.filter { it.startsWith(type, ignoreCase = true) }
        base.take(5)
      }

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
        expanded = expanded && filtered.isNotEmpty(),
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth().testTag(dropdownTestTag),
        properties = PopupProperties(focusable = false)) {
          filtered.forEach { suggestion ->
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

/** Reusable text field for size input */
@Composable
fun SizeField(size: String, onChange: (String) -> Unit, testTag: String) {
  CommonTextField(
      value = size,
      onChange = onChange,
      label = "Size",
      placeholder = "e.g., M, 42, One-size",
      testTag = testTag)
}

/** Autocomplete-style field for item style suggestions. */
@Composable
fun StyleField(
    style: String,
    onChange: (String) -> Unit,
    testTag: String,
    dropdownTestTag: String,
    suggestions: List<String> = STYLE_SUGGESTIONS
) {
  var expanded by remember { mutableStateOf(false) }
  val filtered = remember(style, suggestions) { filterDropdownSuggestions(style, suggestions) }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = style,
        onValueChange = {
          onChange(it)
          expanded = true
        },
        label = { Text("Item style") },
        placeholder = { Text("e.g., Streetwear, Formal") },
        modifier =
            Modifier.fillMaxWidth().testTag(testTag).onFocusChanged { focusState ->
              expanded = focusState.isFocused && filtered.isNotEmpty()
            },
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        colors = commonTextFieldColors())

    DropdownMenu(
        expanded = expanded && filtered.isNotEmpty(),
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth().testTag(dropdownTestTag),
        properties = PopupProperties(focusable = false)) {
          filtered.forEach { suggestion ->
            DropdownMenuItem(
                text = {
                  Text(
                      suggestion,
                      style =
                          MaterialTheme.typography.bodyMedium.copy(
                              color = MaterialTheme.colorScheme.primary))
                },
                onClick = {
                  onChange(suggestion)
                  expanded = false
                })
          }
        }
  }
}

/** Autocomplete-style field for item fit type suggestions. */
@Composable
fun FitTypeField(
    fitType: String,
    onChange: (String) -> Unit,
    testTag: String,
    dropdownTestTag: String,
    suggestions: List<String> = FIT_TYPE_SUGGESTIONS
) {
  var expanded by remember { mutableStateOf(false) }
  val filtered = remember(fitType, suggestions) { filterDropdownSuggestions(fitType, suggestions) }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = fitType,
        onValueChange = {
          onChange(it)
          expanded = true
        },
        label = { Text("Item fit type") },
        placeholder = { Text("e.g., oversized, slim") },
        modifier =
            Modifier.fillMaxWidth().testTag(testTag).onFocusChanged { focusState ->
              expanded = focusState.isFocused && filtered.isNotEmpty()
            },
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        colors = commonTextFieldColors())

    DropdownMenu(
        expanded = expanded && filtered.isNotEmpty(),
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth().testTag(dropdownTestTag),
        properties = PopupProperties(focusable = false)) {
          filtered.forEach { suggestion ->
            DropdownMenuItem(
                text = {
                  Text(
                      suggestion,
                      style =
                          MaterialTheme.typography.bodyMedium.copy(
                              color = MaterialTheme.colorScheme.primary))
                },
                onClick = {
                  onChange(suggestion)
                  expanded = false
                })
          }
        }
  }
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

/** Shared price field that always exposes a Double value. */
@Composable
fun PriceField(
    price: Double,
    onChange: (Double) -> Unit,
    testTag: String,
    label: String = "Item price",
    placeholder: String = "Enter the item price"
) {
  val text = if (price == 0.0) "" else price.toString()
  OutlinedTextField(
      value = text,
      onValueChange = {
        // Accept empty, integers, and decimals
        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
          onChange(it.toDoubleOrNull() ?: 0.0)
        }
      },
      label = { Text(label) },
      placeholder = { Text(placeholder) },
      textStyle =
          MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
      colors = commonTextFieldColors(),
      modifier = Modifier.fillMaxWidth().testTag(testTag))
}

/** Simple currency dropdown similar to ConditionDropdown with fixed options. */
@Composable
fun CurrencyField(
    currency: String,
    onChange: (String) -> Unit,
    testTag: String,
    dropdownTestTag: String? = null,
    label: String = "Currency",
    options: List<String> =
        listOf("CHF", "USD", "EUR", "JPY", "GBP", "CAD", "AUD", "CNY", "V-BUCKS")
) {
  var expanded by remember { mutableStateOf(false) }
  Column(Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = currency,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("Select currency") },
        trailingIcon = {
          IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector =
                    if (expanded) Icons.Filled.KeyboardArrowDown
                    else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Toggle currency options",
                tint = MaterialTheme.colorScheme.primary)
          }
        },
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        colors = commonTextFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag(testTag))
    val menuModifier = dropdownTestTag?.let { Modifier.testTag(it) } ?: Modifier
    DropdownMenu(
        expanded = expanded, onDismissRequest = { expanded = false }, modifier = menuModifier) {
          options.forEach { opt ->
            DropdownMenuItem(
                text = {
                  Text(
                      opt,
                      style =
                          MaterialTheme.typography.bodyMedium.copy(
                              color = MaterialTheme.colorScheme.primary))
                },
                onClick = {
                  onChange(opt)
                  expanded = false
                })
          }
        }
  }
}

/** Shared condition dropdown used by add/edit screens. */
@Composable
fun ConditionDropdown(
    condition: String,
    onConditionChange: (String) -> Unit,
    testTag: String,
    expandedInitially: Boolean = false,
    options: List<String> = CONDITION_OPTIONS
) {
  var expanded by remember { mutableStateOf(expandedInitially) }
  Column(Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = condition.ifEmpty { "" },
        onValueChange = { onConditionChange(it) },
        label = { Text("Condition") },
        placeholder = { Text("Select condition") },
        readOnly = true,
        trailingIcon = {
          IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector =
                    if (expanded) Icons.Filled.KeyboardArrowDown
                    else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Toggle condition options",
                tint = MaterialTheme.colorScheme.primary)
          }
        },
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        colors = commonTextFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag(testTag))
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { opt ->
        DropdownMenuItem(
            text = {
              Text(
                  opt,
                  style =
                      MaterialTheme.typography.bodyMedium.copy(
                          color = MaterialTheme.colorScheme.primary))
            },
            onClick = {
              onConditionChange(opt)
              expanded = false
            })
      }
    }
  }
}

/** Reusable multi-line notes field sharing the same styling as other inputs. */
@Composable
fun NotesField(
    notes: String,
    onChange: (String) -> Unit,
    testTag: String,
    placeholder: String = "Optional notes"
) {
  OutlinedTextField(
      value = notes,
      onValueChange = onChange,
      label = { Text("Notes") },
      placeholder = { Text(placeholder) },
      textStyle =
          MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
      colors = commonTextFieldColors(),
      modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).testTag(testTag),
      maxLines = 5)
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
