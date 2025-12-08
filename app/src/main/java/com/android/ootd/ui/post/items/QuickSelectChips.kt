package com.android.ootd.ui.post.items

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.CategoryNormalizer

/** Standard clothing sizes for quick selection */
val STANDARD_SIZES = listOf("One-size", "XS", "S", "M", "L", "XL", "XXL")
private const val CHIP_ANIMATION_DURATION = 200

object QuickSelectChipsTestTags {
  const val SIZE_CHIP_PREFIX = "sizeChip_"
  const val SIZE_SELECTOR = "sizeSelector"
  const val CONDITION_CHIP_PREFIX = "conditionChip_"
  const val CONDITION_SELECTOR = "conditionSelector"
  const val STYLE_CHIP_PREFIX = "styleChip_"
  const val STYLE_SELECTOR = "styleSelector"
  const val FIT_TYPE_CHIP_PREFIX = "fitTypeChip_"
  const val FIT_TYPE_SELECTOR = "fitTypeSelector"
  const val CATEGORY_CHIP_PREFIX = "categoryChip_"
  const val CATEGORY_SELECTOR = "categorySelector"
}

/**
 * A single selectable chip with modern styling and smooth animations. Can be used independently or
 * as part of a chip group.
 */
@Composable
fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    showCheckIcon: Boolean = false
) {
  val backgroundColor by
      animateColorAsState(
          targetValue = if (isSelected) Primary else Color.White,
          animationSpec = tween(durationMillis = CHIP_ANIMATION_DURATION),
          label = "chipBackground")
  val contentColor by
      animateColorAsState(
          targetValue = if (isSelected) Color.White else Primary,
          animationSpec = tween(durationMillis = CHIP_ANIMATION_DURATION),
          label = "chipContent")
  val borderColor by
      animateColorAsState(
          targetValue = if (isSelected) Primary else Tertiary,
          animationSpec = tween(durationMillis = CHIP_ANIMATION_DURATION),
          label = "chipBorder")
  val elevation by
      animateDpAsState(
          targetValue = if (isSelected) 4.dp else 0.dp,
          animationSpec = tween(durationMillis = CHIP_ANIMATION_DURATION),
          label = "chipElevation")

  OutlinedButton(
      onClick = onClick,
      modifier = modifier.height(40.dp).testTag(testTag),
      shape = RoundedCornerShape(20.dp),
      colors =
          ButtonDefaults.outlinedButtonColors(
              containerColor = backgroundColor, contentColor = contentColor),
      border = BorderStroke(1.5.dp, borderColor),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      elevation =
          ButtonDefaults.buttonElevation(
              defaultElevation = elevation, pressedElevation = elevation + 2.dp)) {
        if (showCheckIcon && isSelected) {
          Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(16.dp).padding(end = 4.dp),
              tint = contentColor)
        }
        Text(
            text = text,
            style =
                Typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor))
      }
}

/**
 * A horizontally scrollable row of selectable chips. Perfect for size, style, or condition
 * selection.
 */
@Composable
fun ChipSelector(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    testTagPrefix: String = "",
    containerTestTag: String = "",
    showCheckIcon: Boolean = false,
    allowDeselect: Boolean = true
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (label != null) {
      Text(
          text = label,
          style = Typography.bodyMedium.copy(color = Primary, fontWeight = FontWeight.Medium),
          modifier = Modifier.padding(start = 4.dp))
    }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag(containerTestTag),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          options.forEach { option ->
            SelectableChip(
                text = option,
                isSelected = selectedOption == option,
                onClick = {
                  if (allowDeselect && selectedOption == option) {
                    onOptionSelected("")
                  } else {
                    onOptionSelected(option)
                  }
                },
                testTag = "$testTagPrefix$option",
                showCheckIcon = showCheckIcon)
          }
        }
  }
}

/**
 * Quick size selector with standard clothing sizes (XS to 3XL). Includes an option for custom size
 * input.
 */
@Composable
fun SizeQuickSelector(
    selectedSize: String,
    onSizeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    sizes: List<String> = STANDARD_SIZES,
    testTagPrefix: String = QuickSelectChipsTestTags.SIZE_CHIP_PREFIX,
    containerTestTag: String = QuickSelectChipsTestTags.SIZE_SELECTOR
) {
  ChipSelector(
      options = sizes,
      selectedOption = selectedSize,
      onOptionSelected = onSizeSelected,
      modifier = modifier,
      testTagPrefix = testTagPrefix,
      containerTestTag = containerTestTag,
      showCheckIcon = true)
}

/** Quick condition selector with predefined condition options. */
@Composable
fun ConditionQuickSelector(
    selectedCondition: String,
    onConditionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    conditions: List<String> = listOf("New", "Like new", "Used", "Vintage"),
    label: String = "Condition",
    testTagPrefix: String = QuickSelectChipsTestTags.CONDITION_CHIP_PREFIX,
    containerTestTag: String = QuickSelectChipsTestTags.CONDITION_SELECTOR
) {
  ChipSelector(
      options = conditions,
      selectedOption = selectedCondition,
      onOptionSelected = onConditionSelected,
      modifier = modifier,
      label = label,
      testTagPrefix = testTagPrefix,
      containerTestTag = containerTestTag,
      showCheckIcon = true)
}

/** Quick style selector with common fashion styles. */
@Composable
fun StyleQuickSelector(
    selectedStyle: String,
    onStyleSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    styles: List<String> = listOf("Casual", "Formal", "Streetwear", "Vintage", "Sporty", "Chic"),
    label: String = "Style",
    testTagPrefix: String = QuickSelectChipsTestTags.STYLE_CHIP_PREFIX,
    containerTestTag: String = QuickSelectChipsTestTags.STYLE_SELECTOR
) {
  ChipSelector(
      options = styles,
      selectedOption = selectedStyle,
      onOptionSelected = onStyleSelected,
      modifier = modifier,
      label = label,
      testTagPrefix = testTagPrefix,
      containerTestTag = containerTestTag,
      showCheckIcon = true)
}

/** Quick fit type selector with common fit options. */
@Composable
fun FitTypeQuickSelector(
    selectedFitType: String,
    onFitTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    fitTypes: List<String> = listOf("Slim", "Regular", "Relaxed", "Oversized", "Tailored"),
    label: String = "Fit Type",
    testTagPrefix: String = QuickSelectChipsTestTags.FIT_TYPE_CHIP_PREFIX,
    containerTestTag: String = QuickSelectChipsTestTags.FIT_TYPE_SELECTOR
) {
  ChipSelector(
      options = fitTypes,
      selectedOption = selectedFitType,
      onOptionSelected = onFitTypeSelected,
      modifier = modifier,
      label = label,
      testTagPrefix = testTagPrefix,
      containerTestTag = containerTestTag,
      showCheckIcon = true)
}

/**
 * Quick category selector for item categories. This field is mandatory so it doesn't allow
 * deselection.
 */
@Composable
fun CategoryQuickSelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<String> = CategoryNormalizer.VALID_CATEGORIES,
    label: String = "Category*",
    isError: Boolean = false,
    errorMessage: String? = null,
    testTagPrefix: String = QuickSelectChipsTestTags.CATEGORY_CHIP_PREFIX,
    containerTestTag: String = QuickSelectChipsTestTags.CATEGORY_SELECTOR
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
              text = label,
              style =
                  Typography.bodyMedium.copy(
                      color = if (isError) Red else Primary, fontWeight = FontWeight.Medium),
              modifier = Modifier.padding(start = 4.dp))
          if (selectedCategory.isEmpty()) {
            Text(
                text = "(required)",
                style =
                    Typography.labelSmall.copy(
                        color = if (isError) Red else Tertiary, fontWeight = FontWeight.Normal))
          }
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag(containerTestTag),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          categories.forEach { category ->
            SelectableChip(
                text = category,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                testTag = "$testTagPrefix$category",
                showCheckIcon = true)
          }
        }

    if (isError && errorMessage != null) {
      Text(
          text = errorMessage,
          style = Typography.labelSmall.copy(color = Red),
          modifier = Modifier.padding(start = 4.dp))
    }
  }
}

/** A compact currency selector using chips instead of dropdown. */
@Composable
fun CurrencyChipSelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    currencies: List<String> = listOf("CHF", "USD", "EUR", "GBP", "JPY"),
    label: String? = "Currency",
    testTagPrefix: String = "currencyChip_",
    containerTestTag: String = "currencySelector"
) {
  ChipSelector(
      options = currencies,
      selectedOption = selectedCurrency,
      onOptionSelected = onCurrencySelected,
      modifier = modifier,
      label = label,
      testTagPrefix = testTagPrefix,
      containerTestTag = containerTestTag,
      allowDeselect = false)
}

@Preview(showBackground = true)
@Composable
private fun SizeQuickSelectorPreview() {
  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
    CategoryQuickSelector(selectedCategory = "Clothing", onCategorySelected = {})

    CategoryQuickSelector(
        selectedCategory = "",
        onCategorySelected = {},
        isError = true,
        errorMessage = "Please select a category")

    SizeQuickSelector(selectedSize = "M", onSizeSelected = {})

    ConditionQuickSelector(selectedCondition = "Like new", onConditionSelected = {})

    StyleQuickSelector(selectedStyle = "Streetwear", onStyleSelected = {})

    FitTypeQuickSelector(selectedFitType = "Oversized", onFitTypeSelected = {})

    CurrencyChipSelector(selectedCurrency = "CHF", onCurrencySelected = {})
  }
}
