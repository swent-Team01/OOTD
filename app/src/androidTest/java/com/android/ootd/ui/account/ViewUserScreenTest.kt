package com.android.ootd.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.OOTDTheme
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for ViewUserScreen, done with AI
 *
 * Tests cover:
 * - UI component visibility
 * - Loading state display
 * - User profile display
 * - Friend vs non-friend state
 * - Posts display
 * - Navigation interactions
 */
@RunWith(AndroidJUnit4::class)
class ViewUserScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

  private var onBackButtonCalled = false
  private var onPostClickCalledWithId: String? = null

  @Before
  fun setUp() {
    mockViewModel = mockk(relaxed = true)
    uiStateFlow = MutableStateFlow(ViewUserData())
    every { mockViewModel.uiState } returns uiStateFlow

    onBackButtonCalled = false
    onPostClickCalledWithId = null
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  private fun setContent() {
    composeTestRule.setContent {
      OOTDTheme {
        ViewUserProfile(
            viewModel = mockViewModel,
            userId = testUserId,
            onBackButton = { onBackButtonCalled = true },
            onPostClick = { postId -> onPostClickCalledWithId = postId })
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun screen_displays_loading_state() {
    uiStateFlow.value = ViewUserData(isLoading = true)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.LOADING_TAG).assertIsDisplayed()
  }

  @Test
  fun screen_displays_user_profile_with_picture_and_username() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser",
            profilePicture = "http://example.com/pic.jpg",
            isFriend = false,
            isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.USERNAME_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.USERNAME_TAG).assertTextContains("@testuser")
    composeTestRule.onNodeWithTag(ViewUserScreenTags.PROFILE_PICTURE_TAG).assertIsDisplayed()
  }

  @Test
  fun screen_displays_avatar_letter_when_no_profile_picture() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser", profilePicture = "", isFriend = false, isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.AVATAR_LETTER_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.AVATAR_LETTER_TAG).assertTextEquals("T")
  }

  @Test
  fun screen_displays_follow_button_when_not_friend() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG).assertTextContains("Follow")
    composeTestRule.onNodeWithText("This user is not your friend").assertIsDisplayed()
  }

  @Test
  fun screen_displays_unfollow_button_when_friend() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = true, isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG)
        .assertTextContains("Unfollow")
    composeTestRule.onNodeWithText("This user is your friend").assertIsDisplayed()
  }

  @Test
  fun screen_displays_friend_count_correctly() {
    uiStateFlow.value =
        ViewUserData(username = "testuser", friendCount = 42, isFriend = false, isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.FRIEND_COUNT_TAG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.FRIEND_COUNT_TAG)
        .assertTextContains("42 friends")
  }

  @Test
  fun screen_displays_posts_when_friend() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser",
            isFriend = true,
            friendPosts = listOf(testPost1, testPost2, testPost3),
            isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG)
        .assertTextContains("Posts :")
    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG).assertCountEquals(3)
  }

  @Test
  fun screen_displays_add_friend_message_when_not_friend() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG)
        .assertTextContains("Add this user as a friend to see their posts")
    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG).assertCountEquals(0)
  }

  @Test
  fun screen_displays_no_posts_when_friend_has_no_posts() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser", isFriend = true, friendPosts = emptyList(), isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.POSTS_SECTION_TAG).assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG).assertCountEquals(0)
  }

  @Test
  fun screen_back_button_triggers_callback() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)
    setContent()

    composeTestRule.onNodeWithTag(ViewUserScreenTags.BACK_BUTTON_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewUserScreenTags.BACK_BUTTON_TAG).performClick()

    assert(onBackButtonCalled)
  }

  @Test
  fun screen_post_click_triggers_callback_with_correct_id() {
    uiStateFlow.value =
        ViewUserData(
            username = "testuser",
            isFriend = true,
            friendPosts = listOf(testPost1),
            isLoading = false)
    setContent()

    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG)[0].performClick()

    assert(onPostClickCalledWithId == "post1")
  }

  @Test
  fun screen_calls_update_on_launch() {
    uiStateFlow.value = ViewUserData(username = "testuser", isFriend = false, isLoading = false)
    setContent()

    verify { mockViewModel.update(testUserId) }
  }

  @Test
  fun screen_handles_multiple_posts_in_grid() {
    val posts =
        List(9) { index ->
          OutfitPost(
              postUID = "post$index",
              ownerId = testUserId,
              outfitURL = "http://example.com/outfit$index.jpg")
        }

    uiStateFlow.value =
        ViewUserData(username = "testuser", isFriend = true, friendPosts = posts, isLoading = false)
    setContent()

    composeTestRule.onAllNodesWithTag(ViewUserScreenTags.POST_TAG).assertCountEquals(9)
  }
}
