package com.android.ootd.ui.post.items

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography

val CONDITION_OPTIONS = listOf("New", "Like new", "Used", "Vintage", "Very Used")
val STYLE_SUGGESTIONS =
    listOf(
        "Casual", "Formal", "Streetwear", "Vintage", "Business", "Sporty", "Smart casual", "Chic")
val FIT_TYPE_SUGGESTIONS =
    listOf("Slim", "Regular", "Relaxed", "Oversized", "Skinny", "Tailored", "Boxy", "Loose")

private const val BRAND_MAX_LENGTH = 40
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
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    BrandField(brand = brand, onChange = onBrandChange, testTag = brandTag)

    // Price row with chip-based currency selector
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      PriceField(price = price, onChange = onPriceChange, testTag = priceTag)
      CurrencyChipSelector(
          selectedCurrency = currency.ifEmpty { "CHF" },
          onCurrencySelected = onCurrencyChange,
          containerTestTag = currencyTag)
    }

    // Size section with quick-select chips
    SizeFieldWithChips(size = size, onChange = onSizeChange, testTag = sizeTag)

    LinkField(link = link, onChange = onLinkChange, testTag = linkTag)
  }
}

/** Reusable category field with quick-select chips. This field is mandatory. */
@Composable
fun CategoryField(
    category: String,
    onChange: (String) -> Unit,
    testTag: String,
    invalidCategory: String? = null,
    onValidate: (() -> Unit)? = null,
    dropdownTestTag: String? = null
) {
  CategoryQuickSelector(
      selectedCategory = category,
      onCategorySelected = { newCategory ->
        onChange(newCategory)
        onValidate?.invoke()
      },
      isError = invalidCategory != null,
      errorMessage = invalidCategory,
      containerTestTag = dropdownTestTag ?: testTag)
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
      textStyle = Typography.bodyMedium,
      colors = commonTextFieldColors(),
      supportingText =
          if (shouldShowCounter) {
            { Text(text = "${value.length}/${maxChars}", style = Typography.labelSmall) }
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

/**
 * Enhanced size field with quick-select chips for standard sizes and a text field for custom sizes.
 */
@Composable
fun SizeFieldWithChips(
    size: String,
    onChange: (String) -> Unit,
    testTag: String,
    sizes: List<String> = STANDARD_SIZES
) {
  val isStandardSize = sizes.contains(size)

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        text = "Size",
        style = Typography.bodyMedium.copy(color = Primary, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = 4.dp))

    // Quick-select chips for standard sizes
    SizeQuickSelector(
        selectedSize = if (isStandardSize) size else "",
        onSizeSelected = onChange,
        containerTestTag = "${testTag}_chips")

    // Custom size input for non-standard sizes
    OutlinedTextField(
        value = if (isStandardSize) "" else size,
        onValueChange = { newValue ->
          if (newValue.length <= SIZE_MAX_LENGTH) {
            onChange(newValue)
          }
        },
        label = { Text("Or enter custom size") },
        placeholder = { Text("e.g., 42, One-size, EU 38") },
        textStyle = Typography.bodyMedium,
        colors = commonTextFieldColors(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(testTag))
  }
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
      textStyle = Typography.bodyMedium,
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
      textStyle = Typography.bodyMedium,
      colors = commonTextFieldColors(),
      supportingText =
          if (showCounter) {
            { Text(text = "${notes.length}/$NOTES_MAX_LENGTH", style = Typography.labelSmall) }
          } else null,
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = 80.dp)
              .onFocusChanged { isFocused = it.isFocused }
              .testTag(testTag),
      maxLines = 5)
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
        focusedBorderColor = Tertiary,
        unfocusedBorderColor = Tertiary,
        disabledBorderColor = Tertiary,
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = Primary,
        focusedLabelColor = Primary,
        unfocusedLabelColor = Primary,
        focusedPlaceholderColor = Primary,
        unfocusedPlaceholderColor = Primary,
        focusedTextColor = Primary,
        unfocusedTextColor = Primary)

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
                tint = Primary)
          }
        },
        textStyle = Typography.bodyMedium.copy(color = Primary),
        colors = commonTextFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag(visuals.textFieldTag))
    val menuModifier =
        visuals.dropdownTestTag?.let { Modifier.fillMaxWidth().testTag(it) }
            ?: Modifier.fillMaxWidth()
    DropdownMenu(
        expanded = expanded, onDismissRequest = { expanded = false }, modifier = menuModifier) {
          visuals.clearOptionLabel?.let { clearLabel ->
            DropdownMenuItem(
                text = { Text(clearLabel, style = Typography.bodyMedium.copy(color = Primary)) },
                onClick = {
                  onOptionSelected("")
                  expanded = false
                })
          }
          options.forEach { opt ->
            DropdownMenuItem(
                text = { Text(opt, style = Typography.bodyMedium.copy(color = Primary)) },
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
  val supportingText: (@Composable () -> Unit)? =
      if (showCounter) {
        { Text("${value.length}/$maxChars", style = Typography.labelSmall) }
      } else null

  Box(modifier = Modifier.fillMaxWidth()) {
    SuggestionInputField(
        value = value,
        visuals = visuals,
        supportingText = supportingText,
        onValueChanged = { input ->
          if (maxChars == null || input.length <= maxChars) {
            onValueChange(input)
            expanded = if (expandOnChange) input.isNotBlank() else true
          }
        },
        onFocusChanged = { focused ->
          isFocused = focused
          if (focused) {
            onFocus?.invoke()
            expanded = focusExpansion(true, filtered.isNotEmpty())
          } else {
            expanded = false
          }
        })

    SuggestionsMenu(
        showMenu = expanded && filtered.isNotEmpty(),
        options = filtered,
        dropdownTag = visuals.dropdownTestTag,
        onSelect = {
          onValueChange(it)
          expanded = false
        },
        onDismiss = { expanded = false })
  }
}

@Composable
private fun SuggestionInputField(
    value: String,
    visuals: SuggestionsDropdownVisuals,
    supportingText: (@Composable () -> Unit)?,
    onValueChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChanged,
      label = { Text(visuals.label) },
      placeholder = { Text(visuals.placeholder) },
      modifier =
          Modifier.fillMaxWidth().testTag(visuals.textFieldTag).onFocusChanged {
            onFocusChanged(it.isFocused)
          },
      singleLine = true,
      textStyle = Typography.bodyMedium.copy(color = Primary),
      colors = commonTextFieldColors(),
      supportingText = supportingText)
}

@Composable
private fun SuggestionsMenu(
    showMenu: Boolean,
    options: List<String>,
    dropdownTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
  DropdownMenu(
      expanded = showMenu,
      onDismissRequest = onDismiss,
      modifier = Modifier.fillMaxWidth().testTag(dropdownTag),
      properties = PopupProperties(focusable = false)) {
        options.forEach { suggestion ->
          DropdownMenuItem(
              text = { Text(suggestion, style = Typography.bodyMedium.copy(color = Primary)) },
              onClick = { onSelect(suggestion) })
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
