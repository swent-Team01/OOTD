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
  fun betaConsentScreen_titleDisplaysCorrectText() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.TITLE)
        .assertTextEquals("OOTD Beta Program")
  }

  @Test
  fun betaConsentScreen_agreeButtonDisabledInitially() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.AGREE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun betaConsentScreen_declineButtonAlwaysEnabled() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.DECLINE_BUTTON).assertIsEnabled()
  }

  @Test
  fun betaConsentScreen_onDeclineCallbackTriggered() {
    // Given
    var declinedCalled = false
    composeTestRule.setContent {
      OOTDTheme { BetaConsentScreen(onDecline = { declinedCalled = true }) }
    }

    // When
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.DECLINE_BUTTON).performClick()

    // Then
    assert(declinedCalled)
  }

  @Test
  fun betaConsentScreen_showsLoadingStateWhenLoading() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen(isLoading = true) } }

    // Then
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.CHECKBOX).performClick()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.AGREE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun betaConsentScreen_displaysErrorMessage() {
    // When
    composeTestRule.setContent {
      OOTDTheme { BetaConsentScreen(errorMessage = "Test error message") }
    }

    // Then
    composeTestRule.onNodeWithText("Test error message").assertExists()
    composeTestRule.onNodeWithText("Dismiss").assertExists()
  }

  @Test
  fun betaConsentScreen_errorMessageDismissCallbackTriggered() {
    // Given
    var dismissCalled = false
    composeTestRule.setContent {
      OOTDTheme {
        BetaConsentScreen(errorMessage = "Test error", onErrorDismiss = { dismissCalled = true })
      }
    }

    // When
    composeTestRule.onNodeWithText("Dismiss").performClick()

    // Then
    assert(dismissCalled)
  }

  @Test
  fun betaConsentScreen_displaysDataCollectionSection() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SECTION_DATA_COLLECTION)
        .performScrollTo()
        .assertExists()
  }

  @Test
  fun betaConsentScreen_displaysPhotosSection() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SECTION_PHOTOS)
        .performScrollTo()
        .assertExists()
  }

  @Test
  fun betaConsentScreen_displaysLocationSection() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule
        .onNodeWithTag(BetaConsentScreenTestTags.SECTION_LOCATION)
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

  @Test
  fun betaConsentScreen_displaysCorrectButtonText() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule.onNodeWithText("Decline").assertExists()
    composeTestRule.onNodeWithTag(BetaConsentScreenTestTags.AGREE_BUTTON).assertExists()
  }

  @Test
  fun betaConsentScreen_showsSavingTextWhenLoading() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen(isLoading = true) } }

    // Then
    composeTestRule.onNodeWithText("Saving...").assertExists()
  }

  @Test
  fun betaConsentScreen_displaysWelcomeText() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule.onNodeWithText("Welcome to the OOTD Beta!").assertExists()
  }

  @Test
  fun betaConsentScreen_displaysCS311Reference() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule
        .onNode(
            hasText(
                "This app is created for the course CS-311 at EPFL. It's still in active development, so if you encounter any bugs or just want to share feedback with us, feel free to reach out to us!",
                substring = true))
        .performScrollTo()
        .assertExists()
  }

  @Test
  fun betaConsentScreen_displaysLastUpdatedDate() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule
        .onNode(hasText("Last updated: November 5, 2025"))
        .performScrollTo()
        .assertExists()
  }

  @Test
  fun betaConsentScreen_displaysCheckboxAgreementText() {
    // When
    composeTestRule.setContent { OOTDTheme { BetaConsentScreen() } }

    // Then
    composeTestRule
        .onNode(
            hasText(
                "I agree to the data collection and usage terms described above", substring = true))
        .assertExists()
  }
}
