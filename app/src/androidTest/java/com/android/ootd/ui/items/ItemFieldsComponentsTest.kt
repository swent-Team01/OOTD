package com.android.ootd.ui.items

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.ootd.ui.post.items.CategoryQuickSelector
import com.android.ootd.ui.post.items.ConditionQuickSelector
import com.android.ootd.ui.post.items.CurrencyChipSelector
import com.android.ootd.ui.post.items.FitTypeQuickSelector
import com.android.ootd.ui.post.items.QuickSelectChipsTestTags
import com.android.ootd.ui.post.items.SizeQuickSelector
import com.android.ootd.ui.post.items.StyleQuickSelector
import com.android.ootd.ui.post.items.TypeField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItemFieldsComponentsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun quickSelectors_updateSelection_whenChipClicked() {
    lateinit var categoryState: MutableState<String>
    lateinit var conditionState: MutableState<String>
    lateinit var styleState: MutableState<String>
    lateinit var fitState: MutableState<String>
    lateinit var currencyState: MutableState<String>
    lateinit var sizeState: MutableState<String>

    composeTestRule.setContent {
      categoryState = remember { mutableStateOf("") }
      conditionState = remember { mutableStateOf("") }
      styleState = remember { mutableStateOf("") }
      fitState = remember { mutableStateOf("") }
      currencyState = remember { mutableStateOf("CHF") }
      sizeState = remember { mutableStateOf("") }

      Column {
        CategoryQuickSelector(
            selectedCategory = categoryState.value,
            onCategorySelected = { categoryState.value = it })
        ConditionQuickSelector(
            selectedCondition = conditionState.value,
            onConditionSelected = { conditionState.value = it })
        StyleQuickSelector(
            selectedStyle = styleState.value, onStyleSelected = { styleState.value = it })
        FitTypeQuickSelector(
            selectedFitType = fitState.value, onFitTypeSelected = { fitState.value = it })
        CurrencyChipSelector(
            selectedCurrency = currencyState.value,
            onCurrencySelected = { currencyState.value = it })
        SizeQuickSelector(selectedSize = sizeState.value, onSizeSelected = { sizeState.value = it })
      }
    }

    // Category
    composeTestRule
        .onNodeWithTag("${QuickSelectChipsTestTags.CATEGORY_CHIP_PREFIX}Clothing")
        .performClick()
    composeTestRule.runOnIdle { assertEquals("Clothing", categoryState.value) }

    // Condition
    composeTestRule
        .onNodeWithTag("${QuickSelectChipsTestTags.CONDITION_CHIP_PREFIX}Used")
        .performClick()
    composeTestRule.runOnIdle { assertEquals("Used", conditionState.value) }
    // Deselect
    composeTestRule
        .onNodeWithTag("${QuickSelectChipsTestTags.CONDITION_CHIP_PREFIX}Used")
        .performClick()
    composeTestRule.runOnIdle { assertEquals("", conditionState.value) }

    // Style
    composeTestRule
        .onNodeWithTag("${QuickSelectChipsTestTags.STYLE_CHIP_PREFIX}Casual")
        .performClick()
    composeTestRule.runOnIdle { assertEquals("Casual", styleState.value) }

    // FitType
    composeTestRule
        .onNodeWithTag("${QuickSelectChipsTestTags.FIT_TYPE_CHIP_PREFIX}Slim")
        .performClick()
    composeTestRule.runOnIdle { assertEquals("Slim", fitState.value) }

    // Currency
    composeTestRule.onNodeWithTag("currencyChip_USD").performClick()
    composeTestRule.runOnIdle { assertEquals("USD", currencyState.value) }

    // Size
    composeTestRule.onNodeWithTag("${QuickSelectChipsTestTags.SIZE_CHIP_PREFIX}M").performClick()
    composeTestRule.runOnIdle { assertEquals("M", sizeState.value) }
  }

  @Test
  fun typeField_showsSuggestions_andFilters() {
    lateinit var typeState: MutableState<String>
    val suggestions = listOf("Jacket", "Jeans", "Jumper", "Hat")

    composeTestRule.setContent {
      typeState = remember { mutableStateOf("") }
      TypeField(
          type = typeState.value,
          suggestions = suggestions,
          onChange = { typeState.value = it },
          testTag = "typeField",
          dropdownTestTag = "typeDropdown",
          expandOnChange = true)
    }

    // Type "J"
    composeTestRule.onNodeWithTag("typeField").performTextInput("J")
    composeTestRule.waitForIdle()

    // Check suggestions
    composeTestRule.onNodeWithText("Jacket").assertIsDisplayed()
    composeTestRule.onNodeWithText("Jeans").assertIsDisplayed()
    composeTestRule.onNodeWithText("Hat").assertDoesNotExist()

    // Select "Jacket"
    composeTestRule.onNodeWithText("Jacket").performClick()
    composeTestRule.runOnIdle { assertEquals("Jacket", typeState.value) }
  }
}
