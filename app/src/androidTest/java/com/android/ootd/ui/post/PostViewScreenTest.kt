package com.android.ootd.ui.post

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.theme.OOTDTheme
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostViewScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockRepository: OutfitPostRepository
  private lateinit var mockUserRepo: UserRepository
  private lateinit var mockLikesRepo: LikesRepository

  private lateinit var mockAccountService: AccountService

  private lateinit var viewModel: PostViewViewModel
  private var onBackCalled = false

  private val testPost =
      OutfitPost(
          postUID = "test-post-id",
          name = "Test User",
          ownerId = "test-owner-id",
          userProfilePicURL = "https://example.com/profile.jpg",
          outfitURL = "https://example.com/outfit.jpg",
          description = "Test outfit description",
          itemsID = listOf("item1", "item2"),
          timestamp = System.currentTimeMillis())

  private val ownerUser =
      User(
          uid = "test-owner-id",
          username = "Owner Name",
          profilePicture = "https://example.com/pfp.jpg")

  @Before
  fun setup() {
    mockRepository = mockk(relaxed = true)
    mockUserRepo = mockk(relaxed = true)
    mockLikesRepo = mockk(relaxed = true)
    mockAccountService = mockk(relaxed = true)

    coEvery { mockUserRepo.getUser(any()) } returns
        User(uid = "placeholder", username = "placeholder", profilePicture = "")

    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    onBackCalled = false

    every { mockAccountService.currentUserId } returns "test-owner-id"
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  private fun setContent(postId: String, onProfileClick: (String) -> Unit = {}) {
    viewModel =
        PostViewViewModel(postId, mockRepository, mockUserRepo, mockLikesRepo, mockAccountService)

    composeTestRule.setContent {
      OOTDTheme {
        PostViewScreen(
            postId = postId,
            onBack = { onBackCalled = true },
            onProfileClick = onProfileClick,
            viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun screen_displays_post_image_when_loaded_successfully() = runTest {
    coEvery { mockRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockUserRepo.getUser("test-owner-id") } returns ownerUser
    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.POST_IMAGE).assertIsDisplayed()
  }

  @Test
  fun screen_displays_error_message_when_repository_throws_exception() = runTest {
    coEvery { mockRepository.getPostById("test-post-id") } throws Exception("Network error")

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.SNACKBAR_HOST).assertIsDisplayed().assertExists()
  }

  @Test
  fun clicking_edit_shows_textfield_and_counter() = runTest {
    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(PostViewTestTags.DROPDOWN_OPTIONS_MENU)
        .assertIsDisplayed()
        .assertExists()
    // Open dropdown
    composeTestRule.onNodeWithTag(PostViewTestTags.DROPDOWN_OPTIONS_MENU).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_OPTION).assertIsDisplayed()

    // Click edit
    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_OPTION).performClick()
    composeTestRule.waitForIdle()

    // TextField appears = edit mode active
    composeTestRule
        .onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD)
        .assertIsDisplayed()
        .assertExists()
    composeTestRule.waitForIdle()

    // Counter appears = edit mode active
    composeTestRule.onNodeWithTag(PostViewTestTags.DESCRIPTION_COUNTER).assertExists()
  }

  @Test
  fun three_dots_icon_and_options_invisible_for_non_owner() = runTest {
    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    every { mockAccountService.currentUserId } returns "some-other-user-id"

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.DROPDOWN_OPTIONS_MENU).assertDoesNotExist()

    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_OPTION).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PostViewTestTags.DELETE_POST_OPTION).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PostViewTestTags.DESCRIPTION_COUNTER).assertDoesNotExist()
  }

  @Test
  fun description_stays_same_after_edit_cancel() = runTest {
    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    val original = testPost.description
    val modified = "Modified description text"

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Open edit mode
    composeTestRule.onNodeWithTag(PostViewTestTags.DROPDOWN_OPTIONS_MENU).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_OPTION).performClick()
    composeTestRule.waitForIdle()

    // Ensure edit field visible with original text
    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD)
        .assertTextContains(original)

    // Modify the description text
    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD)
        .performTextInput(modified)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD)
        .assertTextContains(modified)

    // Cancel edit
    composeTestRule.onNodeWithTag(PostViewTestTags.CANCEL_EDITING_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Ensure screen shows original description and not the modified one
    // Note: description is now displayed with username, so use substring match
    composeTestRule.onNodeWithText(original, substring = true).assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText(modified, substring = true).assertDoesNotExist()

    // Re-open edit
    composeTestRule.onNodeWithTag(PostViewTestTags.DROPDOWN_OPTIONS_MENU).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_OPTION).performClick()
    composeTestRule.waitForIdle()

    // TextField should contain the original description again
    composeTestRule
        .onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD)
        .assertExists()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD)
        .assertTextContains(original)
    composeTestRule.onNodeWithText(modified, substring = true).assertDoesNotExist()
  }

  @Test
  fun likedUsersRow_profileClick_triggersCallback() = runTest {
    val likedUser1 = User(uid = "liker-1", username = "Liker One", profilePicture = "")
    val likedUser2 = User(uid = "liker-2", username = "Liker Two", profilePicture = "")

    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser("test-owner-id") } returns ownerUser
    coEvery { mockUserRepo.getUser("liker-1") } returns likedUser1
    coEvery { mockUserRepo.getUser("liker-2") } returns likedUser2
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns
        listOf(
            com.android.ootd.model.posts.Like(
                postId = "test-post-id", postLikerId = "liker-1", timestamp = 0L),
            com.android.ootd.model.posts.Like(
                postId = "test-post-id", postLikerId = "liker-2", timestamp = 0L))
    coEvery { mockLikesRepo.isPostLikedByUser(any(), any()) } returns false

    var clickedUserId: String? = null

    setContent("test-post-id") { userId -> clickedUserId = userId }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithText("Liker One", substring = true, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithText("Liker One", substring = true, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    assert(clickedUserId == "liker-1") { "Expected liker-1 but got $clickedUserId" }
  }

  @Test
  fun likedUsersRow_displaysAndClickable() = runTest {
    val likedUser1 = User(uid = "liker-1", username = "Liker One", profilePicture = "")

    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser("test-owner-id") } returns ownerUser
    coEvery { mockUserRepo.getUser("liker-1") } returns likedUser1
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns
        listOf(
            com.android.ootd.model.posts.Like(
                postId = "test-post-id", postLikerId = "liker-1", timestamp = 0L))
    coEvery { mockLikesRepo.isPostLikedByUser(any(), any()) } returns false

    var clickedUserId: String? = null
    setContent("test-post-id") { userId -> clickedUserId = userId }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithText("Liker One", substring = true, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithText("Liker One", substring = true, useUnmergedTree = true)
        .assertExists()

    composeTestRule
        .onNodeWithText("Liker One", substring = true, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    assert(clickedUserId == "liker-1") { "Expected liker-1 but got $clickedUserId" }
  }
}
