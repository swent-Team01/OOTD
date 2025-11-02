package com.android.ootd.ui.consent

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test

class BetaConsentScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun betaConsentScreen_displaysCorrectly() {
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Verify main components are displayed
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SCREEN)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.TITLE)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("OOTD Beta Program").assertExists().assertIsDisplayed()

    composeTestRule
        .onNodeWithText("Data Collection & Usage Agreement")
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun betaConsentScreen_agreeButtonDisabledInitially() {
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Agree button should be disabled when checkbox is not checked
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.AGREE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun betaConsentScreen_checkboxEnablesAgreeButton() {
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Find and click the checkbox
    val checkboxMatcher = hasSetTextAction().not() and isToggleable()
    composeTestRule.onNode(checkboxMatcher).performClick()

    // Now the agree button should be enabled
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.AGREE_BUTTON).assertIsEnabled()
  }

  @Test
  fun betaConsentScreen_agreeButtonCallsCallback() {
    var agreeCalled = false
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen(onAgree = { agreeCalled = true }) } }

    // Check the checkbox
    val checkboxMatcher = hasSetTextAction().not() and isToggleable()
    composeTestRule.onNode(checkboxMatcher).performClick()

    // Click agree button
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.AGREE_BUTTON).performClick()

    assert(agreeCalled)
  }

  @Test
  fun betaConsentScreen_declineButtonCallsCallback() {
    var declineCalled = false
    composeTestRule.setContent {
      OOTDTheme { BetaConsentScreen(onDecline = { declineCalled = true }) }
    }

    // Click decline button
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.DECLINE_BUTTON).performClick()

    assert(declineCalled)
  }

  @Test
  fun betaConsentScreen_displaysSections() {
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Verify key sections are present
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.SECTION_DATA_COLLECTION).assertExists()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.SECTION_PHOTOS).assertExists()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.SECTION_LOCATION).assertExists()
  }

  @Test
  fun betaConsentScreen_scrollContentIsScrollable() {
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Verify scrollable content exists
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SCROLL_CONTENT)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun betaConsentScreen_containsKeyInformation() {
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Verify key text content is present
    composeTestRule.onNodeWithText("Photos & Images", substring = true).assertExists()
    composeTestRule.onNodeWithText("Location Data", substring = true).assertExists()
    composeTestRule.onNodeWithText("Your Rights & Privacy", substring = true).assertExists()
  }
}
