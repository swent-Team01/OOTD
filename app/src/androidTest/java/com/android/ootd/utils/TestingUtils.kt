package com.android.ootd.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.android.ootd.ui.Inventory.InventoryScreenTestTags
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.register.RegisterScreenTestTags

fun verifyFeedScreenAppears(composeTestRule: ComposeContentTestRule) {
  // Verify we're on the Feed screen
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).isDisplayed()
  }
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onNodeWithTag(FeedScreenTestTags.TOP_BAR).isDisplayed()
  }
}

fun verifySignInScreenAppears(composeTestRule: ComposeContentTestRule) {
  composeTestRule
      .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
      .performScrollTo()
      .assertIsDisplayed()
  composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
}

fun verifyRegisterScreenAppears(composeTestRule: ComposeContentTestRule) {
  composeTestRule.onNodeWithTag(RegisterScreenTestTags.APP_LOGO).assertIsDisplayed()
  composeTestRule.onNodeWithTag(RegisterScreenTestTags.WELCOME_TITLE).assertIsDisplayed()
  composeTestRule
      .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
      .performScrollTo()
      .assertIsDisplayed()
  composeTestRule
      .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE)
      .performScrollTo()
      .assertIsDisplayed()
  composeTestRule
      .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
      .performScrollTo()
      .assertIsDisplayed()

  composeTestRule.waitForIdle()
}

fun verifyInventoryScreenAppears(composeTestRule: ComposeContentTestRule) {
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SCREEN).isDisplayed()
  }
}

fun clickWithWait(
    composeTestRule: ComposeContentTestRule,
    tag: String,
    shouldScroll: Boolean = false,
    useUnmergedTree: Boolean = false
) {
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).isDisplayed()
  }
  if (shouldScroll) {
    composeTestRule
        .onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
        .performScrollTo()
        .performClick()
  } else {
    composeTestRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).performClick()
  }
}

fun verifyElementDoesNotAppearWithTimer(composeTestRule: ComposeContentTestRule, tag: String) {
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
  }
}

fun verifyElementAppearsWithTimer(composeTestRule: ComposeContentTestRule, tag: String) {
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
  }
}
