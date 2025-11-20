package com.android.ootd.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.ootd.model.map.Location
import com.android.ootd.ui.map.LocationSelectionTestTags
import com.android.ootd.ui.map.LocationSelectionViewModel
import com.android.ootd.ui.register.RegisterScreenTestTags

/** Disclaimer: Parts of the code have been written with the help of AI */
fun ComposeTestRule.enterUsername(text: String) {
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performTextClearance()
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performTextInput(text)
}

fun ComposeTestRule.enterDate(date: String) {
  if (date.isBlank()) {
    onNodeWithText("Dismiss").performClick()
    return
  }
  onNodeWithText("Date").performClick()
  waitForIdle()
  onNodeWithText("Date").assertIsDisplayed().performTextInput("10102020")
  waitForIdle()

  onNodeWithText("Confirm").performClick()
  waitForIdle()
}

fun ComposeTestRule.enterLocation(text: String) {
  onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION).performScrollTo().performClick()
  onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION).performTextInput(text)
  waitForIdle()
}

/**
 * Shared test helper for location dropdown behavior. Tests that the dropdown shows automatically
 * when suggestions arrive while field is focused.
 */
fun ComposeTestRule.testLocationDropdown_showsAutomatically_whenSuggestionsArriveWhileFocused() {
  // Arrange: focus the location field first
  onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION).performScrollTo().performClick()
  waitForIdle()

  // Act: type to trigger search (assumes mock repository is configured)
  enterLocation("Zur")
  waitForIdle()

  // Assert: dropdown should show suggestions automatically
  waitUntil(
      condition = {
        onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
            .fetchSemanticsNodes()
            .size >= 1
      },
      timeoutMillis = 5000)
}

/**
 * Shared test helper for location dropdown behavior. Tests that the dropdown hides automatically
 * when suggestions are cleared.
 */
fun ComposeTestRule.testLocationDropdown_hidesAutomatically_whenSuggestionsCleared(
    locationSelectionViewModel: LocationSelectionViewModel
) {
  // Arrange: show suggestions first (assumes mock repository is configured)
  enterLocation("Zur")
  waitForIdle()

  // Act: clear suggestions via viewModel
  runOnUiThread { locationSelectionViewModel.clearLocationSuggestions() }
  waitForIdle()

  // Assert: dropdown should be hidden
  waitUntil(
      condition = {
        onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
            .fetchSemanticsNodes()
            .isEmpty()
      },
      timeoutMillis = 5000)
}

/**
 * Shared test helper for location dropdown behavior. Tests that the dropdown hides when the
 * location field loses focus.
 */
fun ComposeTestRule.testLocationDropdown_hidesWhenLosingFocus(blurTargetTag: String) {
  // Arrange: show suggestions (assumes mock repository is configured)
  enterLocation("Zur")
  waitForIdle()

  // Act: click another field to lose focus
  onNodeWithTag(blurTargetTag).performScrollTo().performClick()
  waitForIdle()

  // Assert: dropdown should be hidden
  onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION).assertCountEquals(0)
}

/**
 * Shared test helper for location dropdown behavior. Tests that the dropdown shows again when
 * refocusing with existing suggestions.
 */
fun ComposeTestRule.testLocationDropdown_showsAgain_whenRefocusingWithExistingSuggestions(
    locationSelectionViewModel: LocationSelectionViewModel,
    blurTargetTag: String
) {
  // Arrange: set suggestions and then blur the field
  val suggestions = listOf(Location(47.3769, 8.5417, "Zürich, Switzerland"))
  runOnUiThread { locationSelectionViewModel.setLocationSuggestions(suggestions) }
  waitForIdle()

  // Focus another field first
  onNodeWithTag(blurTargetTag).performScrollTo().performClick()
  waitForIdle()

  // Act: refocus location field
  onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION).performScrollTo().performClick()
  waitForIdle()

  // Assert: dropdown should show again
  onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION).assertCountEquals(1)
}
