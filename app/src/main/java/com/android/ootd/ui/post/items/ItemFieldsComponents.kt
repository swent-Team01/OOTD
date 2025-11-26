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

val CONDITION_OPTIONS = listOf("New", "Like new", "Used", "Vintage", "Very Used")
val STYLE_SUGGESTIONS =
    listOf(
        "Casual", "Formal", "Streetwear", "Vintage", "Business", "Sporty", "Smart casual", "Chic")
val FIT_TYPE_SUGGESTIONS =
    listOf("Slim", "Regular", "Relaxed", "Oversized", "Skinny", "Tailored", "Boxy", "Loose")

private const val BRAND_MAX_LENGTH = 20
private const val MATERIAL_MAX_LENGTH = 100
private const val SIZE_MAX_LENGTH = 20
private const val LINK_MAX_LENGTH = 160
private const val NOTES_MAX_LENGTH = 250
private const val TYPE_MAX_LENGTH = 40
private const val STYLE_MAX_LENGTH = 40
private const val FIT_TYPE_MAX_LENGTH = 30

fun filterDropdownSuggestions(input: String, suggestions: List<String>): List<String> {
  return if (input.isBlank()) suggestions else suggestions.filter { it.startsWith(input, true) }
}

/**
 * Groups the main item inputs (brand, price/currency, size, and link) with consistent spacing so
 * screens can drop in the entire block without wiring each field manually.
 */
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
  SuggestionsDropdownField(
      value = type,
      onValueChange = onChange,
      suggestions = suggestions,
      visuals =
          SuggestionsDropdownVisuals(
              label = "Type",
              placeholder = "Enter a type",
              textFieldTag = testTag,
              dropdownTestTag = dropdownTestTag),
      onFocus = onFocus,
      expandOnChange = expandOnChange,
      filter = { input, options ->
        val base =
            if (input.isBlank()) options.take(5)
            else options.filter { it.startsWith(input, ignoreCase = true) }
        base.take(5)
      },
      focusExpansion = { isFocused, _ -> isFocused },
      maxChars = TYPE_MAX_LENGTH)
}

/** Reusable generic text field with common styling */
@Composable
private fun CommonTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String,
    testTag: String,
    maxChars: Int? = null
) {
  var isFocused by remember { mutableStateOf(false) }
  val shouldShowCounter = maxChars != null && isFocused

  OutlinedTextField(
      value = value,
      onValueChange = {
        if (maxChars == null || it.length <= maxChars) {
          onChange(it)
        }
      },
      label = { Text(label) },
      placeholder = { Text(placeholder) },
      textStyle =
          MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
      colors = commonTextFieldColors(),
      supportingText =
          if (shouldShowCounter) {
            {
              Text(
                  text = "${value.length}/${maxChars!!}",
                  style = MaterialTheme.typography.labelSmall)
            }
          } else {
            null
          },
      modifier =
          Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }.testTag(testTag))
}

/** Reusable text field for brand input */
@Composable
fun BrandField(brand: String, onChange: (String) -> Unit, testTag: String) {
  CommonTextField(
      value = brand,
      onChange = onChange,
      label = "Brand",
      placeholder = "Enter a brand",
      testTag = testTag,
      maxChars = BRAND_MAX_LENGTH)
}

/** Reusable text field for material input */
@Composable
fun MaterialField(materialText: String, onChange: (String) -> Unit, testTag: String) {
  CommonTextField(
      value = materialText,
      onChange = onChange,
      label = "Material",
      placeholder = "E.g., Cotton 80%, Wool 20%",
      testTag = testTag,
      maxChars = MATERIAL_MAX_LENGTH)
}

/** Reusable text field for size input */
@Composable
fun SizeField(size: String, onChange: (String) -> Unit, testTag: String) {
  CommonTextField(
      value = size,
      onChange = onChange,
      label = "Size",
      placeholder = "e.g., M, 42, One-size",
      testTag = testTag,
      maxChars = SIZE_MAX_LENGTH)
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
  SuggestionsDropdownField(
      value = style,
      onValueChange = onChange,
      suggestions = suggestions,
      visuals =
          SuggestionsDropdownVisuals(
              label = "Item style",
              placeholder = "e.g., Streetwear, Formal",
              textFieldTag = testTag,
              dropdownTestTag = dropdownTestTag),
      maxChars = STYLE_MAX_LENGTH)
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
  SuggestionsDropdownField(
      value = fitType,
      onValueChange = onChange,
      suggestions = suggestions,
      visuals =
          SuggestionsDropdownVisuals(
              label = "Item fit type",
              placeholder = "e.g., oversized, slim",
              textFieldTag = testTag,
              dropdownTestTag = dropdownTestTag),
      maxChars = FIT_TYPE_MAX_LENGTH)
}

/** Reusable text field for link input */
@Composable
fun LinkField(link: String, onChange: (String) -> Unit, testTag: String) {
  CommonTextField(
      value = link,
      onChange = onChange,
      label = "Link",
      placeholder = "e.g., https://example.com",
      testTag = testTag,
      maxChars = LINK_MAX_LENGTH)
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
  SelectionDropdownField(
      value = currency,
      onOptionSelected = onChange,
      options = options,
      visuals =
          SelectionDropdownVisuals(
              label = label,
              placeholder = "Select currency",
              textFieldTag = testTag,
              toggleContentDescription = "Toggle currency options",
              dropdownTestTag = dropdownTestTag))
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
  SelectionDropdownField(
      value = condition.ifEmpty { "" },
      onOptionSelected = onConditionChange,
      visuals =
          SelectionDropdownVisuals(
              label = "Condition",
              placeholder = "Select condition",
              textFieldTag = testTag,
              toggleContentDescription = "Toggle condition options",
              clearOptionLabel = "Clear condition"),
      options = options,
      expandedInitially = expandedInitially)
}

/** Reusable multi-line notes field sharing the same styling as other inputs. */
@Composable
fun NotesField(
    notes: String,
    onChange: (String) -> Unit,
    testTag: String,
    placeholder: String = "Optional notes"
) {
  var isFocused by remember { mutableStateOf(false) }
  val showCounter = isFocused

  OutlinedTextField(
      value = notes,
      onValueChange = {
        if (it.length <= NOTES_MAX_LENGTH) {
          onChange(it)
        }
      },
      label = { Text("Notes") },
      placeholder = { Text(placeholder) },
      textStyle =
          MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
      colors = commonTextFieldColors(),
      supportingText =
          if (showCounter) {
            {
              Text(
                  text = "${notes.length}/$NOTES_MAX_LENGTH",
                  style = MaterialTheme.typography.labelSmall)
            }
          } else null,
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = 80.dp)
              .onFocusChanged { isFocused = it.isFocused }
              .testTag(testTag),
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

/**
 * Shared dropdown used by condition/currency pickers, wiring label, placeholder, and optional
 * "clear" action while keeping the same Material styling everywhere.
 */
@Composable
private fun SelectionDropdownField(
    value: String,
    onOptionSelected: (String) -> Unit,
    options: List<String>,
    visuals: SelectionDropdownVisuals,
    expandedInitially: Boolean = false
) {
  var expanded by remember { mutableStateOf(expandedInitially) }
  Column(Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(visuals.label) },
        placeholder = { Text(visuals.placeholder) },
        trailingIcon = {
          IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector =
                    if (expanded) Icons.Filled.KeyboardArrowDown
                    else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = visuals.toggleContentDescription,
                tint = MaterialTheme.colorScheme.primary)
          }
        },
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        colors = commonTextFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag(visuals.textFieldTag))
    val menuModifier =
        visuals.dropdownTestTag?.let { Modifier.fillMaxWidth().testTag(it) }
            ?: Modifier.fillMaxWidth()
    DropdownMenu(
        expanded = expanded, onDismissRequest = { expanded = false }, modifier = menuModifier) {
          visuals.clearOptionLabel?.let { clearLabel ->
            DropdownMenuItem(
                text = {
                  Text(
                      clearLabel,
                      style =
                          MaterialTheme.typography.bodyMedium.copy(
                              color = MaterialTheme.colorScheme.primary))
                },
                onClick = {
                  onOptionSelected("")
                  expanded = false
                })
          }
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
                  onOptionSelected(opt)
                  expanded = false
                })
          }
        }
  }
}

/**
 * Text input with autocomplete suggestions (type/style/fit). It centralizes the menu behavior,
 * filtering, and test tags so every field behaves identically.
 */
@Composable
private fun SuggestionsDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    visuals: SuggestionsDropdownVisuals,
    onFocus: (() -> Unit)? = null,
    expandOnChange: Boolean = false,
    filter: (String, List<String>) -> List<String> = ::filterDropdownSuggestions,
    focusExpansion: (Boolean, Boolean) -> Boolean = { isFocused, hasSuggestions ->
      isFocused && hasSuggestions
    },
    maxChars: Int? = null
) {
  var expanded by remember { mutableStateOf(false) }
  var isFocused by remember { mutableStateOf(false) }
  val filtered = remember(value, suggestions) { filter(value, suggestions) }
  val showCounter = maxChars != null && isFocused

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = {
          if (maxChars == null || it.length <= maxChars) {
            onValueChange(it)
            expanded = if (expandOnChange) it.isNotBlank() else true
          }
        },
        label = { Text(visuals.label) },
        placeholder = { Text(visuals.placeholder) },
        modifier =
            Modifier.fillMaxWidth().testTag(visuals.textFieldTag).onFocusChanged { focusState ->
              isFocused = focusState.isFocused
              if (focusState.isFocused) {
                onFocus?.invoke()
                expanded = focusExpansion(true, filtered.isNotEmpty())
              } else {
                expanded = false
              }
            },
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        colors = commonTextFieldColors(),
        supportingText =
            if (showCounter) {
              { Text("${value.length}/${maxChars!!}", style = MaterialTheme.typography.labelSmall) }
            } else null)

    DropdownMenu(
        expanded = expanded && filtered.isNotEmpty(),
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth().testTag(visuals.dropdownTestTag),
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
                  onValueChange(suggestion)
                  expanded = false
                })
          }
        }
  }
}

private data class SelectionDropdownVisuals(
    val label: String,
    val placeholder: String,
    val textFieldTag: String,
    val toggleContentDescription: String,
    val dropdownTestTag: String? = null,
    val clearOptionLabel: String? = null
)

private data class SuggestionsDropdownVisuals(
    val label: String,
    val placeholder: String,
    val textFieldTag: String,
    val dropdownTestTag: String
)
