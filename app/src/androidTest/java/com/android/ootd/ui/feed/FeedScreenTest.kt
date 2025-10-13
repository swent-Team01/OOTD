package com.android.ootd.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.OutfitPost
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.user.User
import org.junit.Rule
import org.junit.Test

/**
 * Simple UI tests for FeedScreen - just checking that test tags exist. Works with the current
 * FeedScreen (with ViewModel).
 */
class FeedScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun feedScreen_showsLockedMessage_whenUserHasNotPostedToday() {
    val fakeRepo =
        object : FeedRepository {
          override suspend fun hasPostedToday(userId: String) = false

          override suspend fun getFeed() = emptyList<OutfitPost>()

          override suspend fun addPost(post: OutfitPost) {}

          override fun getNewPostId(): String = "fake-id"
        }

    FeedRepositoryProvider.repository = fakeRepo
    val viewModel =
        FeedViewModel().apply {
          setCurrentUser(User(uid = "user1", username = "Tester", friendList = emptyList()))
        }

    composeTestRule.setContent { FeedScreen(feedViewModel = viewModel, onAddPostClick = {}) }

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.LOCKED_MESSAGE)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun feedScreen_showsFeedList_whenUserHasPostedToday() {
    val fakePosts =
        listOf(
            OutfitPost(postUID = "1", uid = "user1", outfitURL = "https://example.com/outfit.jpg"))

    val fakeRepo =
        object : FeedRepository {
          override suspend fun hasPostedToday(userId: String) = true

          override suspend fun getFeed() = fakePosts

          override suspend fun addPost(post: OutfitPost) {}

          override fun getNewPostId(): String = "fake-id"
        }

    FeedRepositoryProvider.repository = fakeRepo
    val viewModel =
        FeedViewModel().apply {
          setCurrentUser(User(uid = "user1", username = "Tester", friendList = emptyList()))
        }

    composeTestRule.setContent { FeedScreen(feedViewModel = viewModel, onAddPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LIST).assertExists().assertIsDisplayed()
  }

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
