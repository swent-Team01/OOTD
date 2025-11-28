package com.android.ootd.ui.post

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.Like
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.theme.OOTDTheme
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
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

  private fun setContent(postId: String) {
    viewModel =
        PostViewViewModel(postId, mockRepository, mockUserRepo, mockLikesRepo, mockAccountService)

    composeTestRule.setContent {
      OOTDTheme {
        PostViewScreen(postId = postId, onBack = { onBackCalled = true }, viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun screen_displays_loading_indicator_initially() {
    coEvery { mockRepository.getPostById(any()) } coAnswers
        {
          delay(1000)
          testPost
        }

    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    setContent("test-post-id")

    composeTestRule.onNodeWithTag(PostViewTestTags.LOADING_INDICATOR).assertIsDisplayed()
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

    composeTestRule.onNodeWithTag(PostViewTestTags.ERROR_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithText("Failed to load post").assertIsDisplayed()
  }

  @Test
  fun owner_section_displays_username() = runTest {
    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    setContent("test-post-id")

    composeTestRule.onNodeWithText("Owner Name").assertIsDisplayed()
  }

  @Test
  fun description_is_displayed_when_present() = runTest {
    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    setContent("test-post-id")

    composeTestRule.onNodeWithText("Test outfit description").assertIsDisplayed()
  }

  @Test
  fun description_not_displayed_when_blank() = runTest {
    val noDescription = testPost.copy(description = "")

    coEvery { mockRepository.getPostById(any()) } returns noDescription
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    setContent("test-post-id")

    composeTestRule.onNodeWithText("Test outfit description").assertDoesNotExist()
  }

  @Test
  fun like_row_shows_correct_like_count() = runTest {
    val likes = listOf(Like("test-post-id", "u1", 0), Like("test-post-id", "u2", 0))

    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser("test-owner-id") } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns likes
    coEvery { mockUserRepo.getUser("u1") } returns User("u1", "A", "")
    coEvery { mockUserRepo.getUser("u2") } returns User("u2", "B", "")

    setContent("test-post-id")

    composeTestRule.onNodeWithText("2 likes").assertIsDisplayed()
  }

  @Test
  fun like_button_is_clickable() = runTest {
    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    setContent("test-post-id")

    composeTestRule.onNodeWithContentDescription("Like").performClick()
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
    composeTestRule.onNodeWithTag(PostViewTestTags.DESCRIPTION_COUNTER).assertIsDisplayed()
  }

  @Test
  fun cancel_button_exits_edit_mode() = runTest {
    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.DROPDOWN_OPTIONS_MENU).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_OPTION).performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD)
        .assertExists()
        .assertIsDisplayed()
    composeTestRule.waitForIdle()

    // Ensure edit mode ON
    composeTestRule.onNodeWithTag(PostViewTestTags.DESCRIPTION_COUNTER).assertIsDisplayed()

    // Click cancel
    composeTestRule.onNodeWithTag(PostViewTestTags.CANCEL_EDITING_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Ensure edit mode OFF
    composeTestRule.onNodeWithTag(PostViewTestTags.DESCRIPTION_COUNTER).assertDoesNotExist()
  }

  @Test
  fun edit_mode_shows_text_field_with_existing_description() = runTest {
    coEvery { mockRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.DROPDOWN_OPTIONS_MENU).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_OPTION).performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD)
        .assertExists()
        .assertIsDisplayed()
    composeTestRule.waitForIdle()

    // Verify initial description is inside the TextField
    composeTestRule
        .onNode(hasSetTextAction())
        .assertIsDisplayed()
        .assertTextContains(testPost.description)
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
}
