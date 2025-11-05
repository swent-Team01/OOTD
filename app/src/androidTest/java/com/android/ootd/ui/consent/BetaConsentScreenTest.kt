package com.android.ootd.ui.consent

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BetaConsentScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun betaConsentScreen_displaysAllMainElements() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.TITLE).assertExists()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.SCROLL_CONTENT).assertExists()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.CHECKBOX).assertExists()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.AGREE_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.DECLINE_BUTTON).assertExists()
  }

  @Test
  fun betaConsentScreen_displaysCorrectInitialState() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then - verify title, button states, and text content
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.TITLE)
        .assertTextEquals("OOTD Beta Program")
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.AGREE_BUTTON).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.DECLINE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithText("Welcome to the OOTD Beta!").assertExists()
    composeTestRule.onNodeWithText("Decline").assertExists()
  }

  @Test
  fun betaConsentScreen_declineCallbackTriggered() {
    // Test decline callback
    var declinedCalled = false
    composeTestRule.setContent {
      OOTDTheme { BetaConsentScreen(onDecline = { declinedCalled = true }) }
    }
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.DECLINE_BUTTON).performClick()
    assert(declinedCalled)
  }

  @Test
  fun betaConsentScreen_errorDismissCallbackTriggered() {
    // Test error dismiss callback
    var dismissCalled = false
    composeTestRule.setContent {
      OOTDTheme {
        BetaConsentScreen(errorMessage = "Test error", onErrorDismiss = { dismissCalled = true })
      }
    }
    composeTestRule.onNodeWithText("Dismiss").performClick()
    assert(dismissCalled)
  }

  @Test
  fun betaConsentScreen_displaysAllContentSections() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then - verify all sections are present
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SECTION_DATA_COLLECTION)
        .performScrollTo()
        .assertExists()
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SECTION_PHOTOS)
        .performScrollTo()
        .assertExists()
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SECTION_LOCATION)
        .performScrollTo()
        .assertExists()
    composeTestRule
        .onNode(
            hasText(
                "This app is created for the course CS-311 at EPFL. It's still in active development, so if you encounter any bugs or just want to share feedback with us, feel free to reach out to us!",
                substring = true))
        .performScrollTo()
        .assertExists()
    composeTestRule
        .onNode(hasText("Last updated: November 5, 2025"))
        .performScrollTo()
        .assertExists()
  }

  @Test
  fun betaConsentScreen_scrollContentIsScrollable() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then - verify scrollable content exists and can be scrolled
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SCROLL_CONTENT)
        .assertExists()
        .performScrollToNode(hasTestTag(BetaConsentScreenTestTags.SECTION_LOCATION))
  }
}
