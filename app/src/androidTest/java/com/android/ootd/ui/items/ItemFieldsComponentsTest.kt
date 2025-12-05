package com.android.ootd.ui.items

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.ootd.ui.post.items.ConditionQuickSelector
import com.android.ootd.ui.post.items.CurrencyChipSelector
import com.android.ootd.ui.post.items.QuickSelectChipsTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItemFieldsComponentsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun conditionQuickSelector_updatesSelection_whenChipClicked() {
    val conditions = listOf("New", "Used")
    lateinit var selectedState: MutableState<String>
    composeTestRule.setContent {
      selectedState = remember { mutableStateOf("") }
      ConditionQuickSelector(
          selectedCondition = selectedState.value,
          onConditionSelected = { selectedState.value = it },
          conditions = conditions,
          containerTestTag = "conditionSelector")
    }

    composeTestRule
        .onNodeWithTag("${QuickSelectChipsTestTags.CONDITION_CHIP_PREFIX}Used")
        .performClick()
    composeTestRule.runOnIdle { assertEquals("Used", selectedState.value) }
  }

  @Test
  fun conditionQuickSelector_clearsSelection_whenSameChipClickedAgain() {
    val conditions = listOf("New", "Used")
    lateinit var selectedState: MutableState<String>
    composeTestRule.setContent {
      selectedState = remember { mutableStateOf("New") }
      ConditionQuickSelector(
          selectedCondition = selectedState.value,
          onConditionSelected = { selectedState.value = it },
          conditions = conditions,
          containerTestTag = "conditionSelector")
    }

    // Click on the already selected chip to deselect
    composeTestRule
        .onNodeWithTag("${QuickSelectChipsTestTags.CONDITION_CHIP_PREFIX}New")
        .performClick()
    composeTestRule.runOnIdle { assertEquals("", selectedState.value) }
  }

  @Test
  fun currencyChipSelector_updatesSelection_whenChipClicked() {
    val currencies = listOf("CHF", "USD")
    lateinit var selectedState: MutableState<String>
    composeTestRule.setContent {
      selectedState = remember { mutableStateOf("CHF") }
      CurrencyChipSelector(
          selectedCurrency = selectedState.value,
          onCurrencySelected = { selectedState.value = it },
          currencies = currencies,
          containerTestTag = "currencySelector")
    }

    composeTestRule.onNodeWithTag("currencyChip_USD").performClick()
    composeTestRule.runOnIdle { assertEquals("USD", selectedState.value) }
  }
}
