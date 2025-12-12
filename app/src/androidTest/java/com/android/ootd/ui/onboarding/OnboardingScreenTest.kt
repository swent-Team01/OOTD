package com.android.ootd.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun onboardingScreen_displaysCoreElements() {
    composeTestRule.setContent { OOTDTheme { OnboardingScreen() } }

    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.TITLE).assertExists()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.SUBTITLE).assertExists()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.PAGER).assertExists()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.SKIP_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.NEXT_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.INDICATORS).assertExists()
  }

  @Test
  fun onboardingScreen_showsFirstPageCopy() {
    composeTestRule.setContent { OOTDTheme { OnboardingScreen() } }

    composeTestRule.onNodeWithText("Capture your outfit with a FitCheck").assertExists()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.NEXT_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.SKIP_BUTTON).assertIsEnabled()
    composeTestRule
        .onNodeWithTag(OnboardingScreenTestTags.TITLE)
        .assertTextEquals("Get to know OOTD")
  }

  @Test
  fun onboardingScreen_skipCallbackTriggered() {
    var skipCalled = false
    composeTestRule.setContent { OOTDTheme { OnboardingScreen(onSkip = { skipCalled = true }) } }
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.SKIP_BUTTON).performClick()
    assert(skipCalled)
  }

  @Test
  fun onboardingScreen_completionCallbackTriggered() {
    var completed = false
    composeTestRule.setContent { OOTDTheme { OnboardingScreen(onAgree = { completed = true }) } }

    repeat(3) { composeTestRule.onNodeWithTag(OnboardingScreenTestTags.NEXT_BUTTON).performClick() }
    composeTestRule.onNodeWithText("Start using OOTD").assertExists()
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.NEXT_BUTTON).performClick()
    assert(completed)
  }

  @Test
  fun onboardingScreen_errorDismissCallbackTriggered() {
    var dismissCalled = false
    composeTestRule.setContent {
      OOTDTheme {
        OnboardingScreen(errorMessage = "Test error", onErrorDismiss = { dismissCalled = true })
      }
    }
    composeTestRule.onNodeWithText("Dismiss").performClick()
    assert(dismissCalled)
  }
}
