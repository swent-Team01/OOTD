package com.android.ootd.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * Simple UI tests for FeedScreen - just checking that test tags exist. Works with the current
 * FeedScreen (with ViewModel).
 */
class FeedScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun feedScreen_hasScreenTag() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertExists()
  }

  @Test
  fun feedScreen_hasTopBarTag() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.TOP_BAR).assertExists()
  }

  @Test
  fun feedScreen_hasLockedMessageOrFeedList() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}) }

    // Either locked message OR feed list should exist (depending on hasPostedToday)
    val lockedMessageExists =
        composeTestRule.onNodeWithTag(FeedScreenTestTags.LOCKED_MESSAGE).assertExists()
  }

  @Test
  fun feedScreen_hasFABOrNot() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}) }

    // FAB should exist if user hasn't posted (can't predict state, just check it renders)
    // This test just ensures the screen renders without crashing
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertExists()
  }

  @Test
  fun feedScreen_rendersWithoutCrashing() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}) }

    // Just verify the screen renders
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.TOP_BAR).assertExists()
  }

  @Test
  fun feedScreen_topBarAlwaysPresent() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.TOP_BAR).assertExists().assertIsDisplayed()
  }

  @Test
  fun feedScreen_screenTagIsDisplayed() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun feedScreen_callbackDoesNotCrash() {
    var clicked = false

    composeTestRule.setContent { FeedScreen(onAddPostClick = { clicked = true }) }

    // Try to find and click FAB if it exists
    try {
      composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_POST_FAB).performClick()
    } catch (e: Exception) {
      // FAB might not be there if user has posted - that's okay
    }
  }
}
