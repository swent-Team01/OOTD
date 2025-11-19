package com.android.ootd.ui.items

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.ootd.ui.post.items.ConditionDropdown
import com.android.ootd.ui.post.items.CurrencyField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItemFieldsComponentsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun conditionDropdown_updatesSelection_whenOptionChosen() {
    val options = listOf("New", "Used")
    lateinit var selectedState: MutableState<String>
    composeTestRule.setContent {
      selectedState = remember { mutableStateOf("") }
      ConditionDropdown(
          condition = selectedState.value,
          onConditionChange = { selectedState.value = it },
          testTag = "conditionDropdown",
          expandedInitially = false,
          options = options)
    }

    composeTestRule.onNodeWithContentDescription("Toggle condition options").performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithText("Used", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Used", useUnmergedTree = true).performClick()
    composeTestRule.runOnIdle { assertEquals("Used", selectedState.value) }
  }

  @Test
  fun currencyField_updatesSelection_whenCurrencyChosen() {
    val options = listOf("CHF", "USD")
    lateinit var selectedState: MutableState<String>
    composeTestRule.setContent {
      selectedState = remember { mutableStateOf("CHF") }
      CurrencyField(
          currency = selectedState.value,
          onChange = { selectedState.value = it },
          testTag = "currencyField",
          dropdownTestTag = "currencyMenu",
          options = options)
    }

    composeTestRule.onNodeWithContentDescription("Toggle currency options").performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithText("USD", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("USD", useUnmergedTree = true).performClick()
    composeTestRule.runOnIdle { assertEquals("USD", selectedState.value) }
  }
}
