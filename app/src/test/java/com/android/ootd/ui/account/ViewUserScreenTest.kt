package com.android.ootd.ui.account

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.posts.OutfitPost
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * UI tests for ViewUserScreen.
 *
 * Tests cover:
 * - UI component visibility
 * - Loading state display
 * - User profile display
 * - Friend vs non-friend state
 * - Posts display
 * - Navigation interactions
 */
@RunWith(RobolectricTestRunner::class)
class ViewUserScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockViewModel: ViewUserViewModel
  private lateinit var uiStateFlow: MutableStateFlow<ViewUserData>

  private val testUserId = "testUserId"
  private val testPost1 =
      OutfitPost(
          postUID = "post1", ownerId = testUserId, outfitURL = "http://example.com/outfit1.jpg")
  private val testPost2 =
      OutfitPost(
          postUID = "post2", ownerId = testUserId, outfitURL = "http://example.com/outfit2.jpg")
  private val testPost3 =
      OutfitPost(
          postUID = "post3", ownerId = testUserId, outfitURL = "http://example.com/outfit3.jpg")

  @Before
  fun setUp() {
    mockViewModel = mockk(relaxed = true)
    uiStateFlow = MutableStateFlow(ViewUserData())
    every { mockViewModel.uiState } returns uiStateFlow
  }

  @Test
  fun viewUserScreen_displaysLoadingState() {
    uiStateFlow.value = ViewUserData(isLoading = true)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.LOADING_TAG).assertIsDisplayed()
  }

  @Test
  fun viewUserScreen_displaysUserProfile_whenLoaded() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser",
            profilePicture = "http://example.com/pic.jpg",
            isFriend = false,
            isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.USERNAME_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.USERNAME_TAG).assertTextContains("@testuser")
    composeTestRule.onNodeWithTag(ViewUserScreenTags.PROFILE_PICTURE_TAG).assertIsDisplayed()
  }

  @Test
  fun viewUserScreen_displaysAvatarLetter_whenNoProfilePicture() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser", profilePicture = "", isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.AVATAR_LETTER_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.AVATAR_LETTER_TAG).assertTextEquals("T")
  }

  @Test
  fun viewUserScreen_displaysFollowButton() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG).assertTextContains("Follow")
  }

  @Test
  fun viewUserScreen_displaysUnfollowButton_whenFriend() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = true, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG)
        .assertTextContains("Unfollow")
  }

  @Test
  fun viewUserScreen_displaysFriendCount() {
    uiStateFlow.value =
        ViewUserData(username = "testuser", friendCount = 42, isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.FRIEND_COUNT_TAG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.FRIEND_COUNT_TAG)
        .assertTextContains("42 friends")
  }

  @Test
  fun viewUserScreen_displaysFriendStatusText_whenFriend() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = true, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithText("This user is your friend").assertIsDisplayed()
  }

  @Test
  fun viewUserScreen_displaysFriendStatusText_whenNotFriend() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithText("This user is not your friend").assertIsDisplayed()
  }

  @Test
  fun viewUserScreen_displaysPosts_whenFriend() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser",
            isFriend = true,
            friendPosts = listOf(testPost1, testPost2, testPost3),
            isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG)
        .assertTextContains("Posts :")
    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG).assertCountEquals(3)
  }

  @Test
  fun viewUserScreen_displaysAddFriendMessage_whenNotFriend() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG)
        .assertTextContains("Add this user as a friend to see their posts")
    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG).assertCountEquals(0)
  }

  @Test
  fun viewUserScreen_backButton_callsOnBackButton() {
    var backCalled = false
    val onBackButton = { backCalled = true }

    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)

    composeTestRule.setContent {
      ViewUserProfile(viewModel = mockViewModel, userId = testUserId, onBackButton = onBackButton)
    }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.BACK_BUTTON_TAG).performClick()

    assert(backCalled)
  }

  @Test
  fun viewUserScreen_postClick_callsOnPostClick() {
    var clickedPostId: String? = null
    val onPostClick: (String) -> Unit = { postId -> clickedPostId = postId }

    uiStateFlow.value =
        ViewUserData(
            username = "testuser",
            isFriend = true,
            friendPosts = listOf(testPost1),
            isLoading = false)

    composeTestRule.setContent {
      ViewUserProfile(viewModel = mockViewModel, userId = testUserId, onPostClick = onPostClick)
    }

    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG)[0].performClick()

    assert(clickedPostId == "post1")
  }

  @Test
  fun viewUserScreen_displaysTitle_withUsername() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.TITLE_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.TITLE_TAG).assertTextEquals("@testuser")
  }

  @Test
  fun viewUserScreen_callsUpdate_onLaunch() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.waitForIdle()

    verify { mockViewModel.update(testUserId) }
  }

  @Test
  fun viewUserScreen_displaysNoPosts_whenFriendHasNoPosts() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser", isFriend = true, friendPosts = emptyList(), isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG).assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG).assertCountEquals(0)
  }

  @Test
  fun viewUserScreen_displaysBackButton() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.BACK_BUTTON_TAG).assertIsDisplayed()
  }

  @Test
  fun viewUserScreen_displaysCorrectly_withEmptyUsername() {
    uiStateFlow.value = ViewUserData(username = "", isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onNodeWithTag(ViewUserScreenTags.TITLE_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.TITLE_TAG).assertTextEquals("@")
  }

  @Test
  fun viewUserScreen_multiplePostsDisplay_inGrid() {
    val posts =
        List(9) { index ->
          OutfitPost(
              postUID = "post$index",
              ownerId = testUserId,
              outfitURL = "http://example.com/outfit$index.jpg")
        }

    uiStateFlow.value =
        ViewUserData(username = "testuser", isFriend = true, friendPosts = posts, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG).assertCountEquals(9)
  }

  @Test
  fun viewUserScreen_displaysZeroFriends_correctly() {
    uiStateFlow.value =
        ViewUserData(username = "testuser", friendCount = 0, isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.FRIEND_COUNT_TAG)
        .assertTextContains("0 friends")
  }

  @Test
  fun viewUserScreen_displaysOneFriend_correctly() {
    uiStateFlow.value =
        ViewUserData(username = "testuser", friendCount = 1, isFriend = false, isLoading = false)

    composeTestRule.setContent { ViewUserProfile(viewModel = mockViewModel, userId = testUserId) }

    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.FRIEND_COUNT_TAG)
        .assertTextContains("1 friends")
  }
}
