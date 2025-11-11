package com.android.ootd.ui.post

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.OOTDTheme
import io.mockk.clearAllMocks
import io.mockk.coEvery
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

  @Before
  fun setup() {
    mockRepository = mockk(relaxed = true)
    onBackCalled = false
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  private fun setContent(postId: String) {
    viewModel = PostViewViewModel(postId, mockRepository)

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
          // Simulate slow loading
          kotlinx.coroutines.delay(1000)
          testPost
        }

    setContent("test-post-id")

    composeTestRule.onNodeWithTag(PostViewTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun screen_displays_post_image_when_loaded_successfully() = runTest {
    coEvery { mockRepository.getPostById("test-post-id") } returns testPost

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.POST_IMAGE).assertIsDisplayed()
  }

  @Test
  fun screen_displays_description_when_post_has_description_or_not() = runTest {
    coEvery { mockRepository.getPostById("test-post-id") } returns testPost

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Test outfit description").assertIsDisplayed()

    composeTestRule.onNodeWithTag(PostViewTestTags.BACK_BUTTON).performClick()

    val postWithoutDescription = testPost.copy(description = "")
    coEvery { mockRepository.getPostById("test-post-id") } returns postWithoutDescription

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.POST_IMAGE).assertIsDisplayed()
  }

  @Test
  fun screen_displays_error_message_when_post_not_found() = runTest {
    coEvery { mockRepository.getPostById("non-existent-id") } returns null

    setContent("non-existent-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.ERROR_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithText("Post not found").assertIsDisplayed()
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
  fun screen_displays_top_bar_with_title() = runTest {
    coEvery { mockRepository.getPostById("test-post-id") } returns testPost

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithText("Post").assertIsDisplayed()
  }

  @Test
  fun screen_structure_is_correct() = runTest {
    coEvery { mockRepository.getPostById("test-post-id") } returns testPost

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Verify main components exist
    composeTestRule.onNodeWithTag(PostViewTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostViewTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostViewTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostViewTestTags.POST_IMAGE).assertIsDisplayed()
  }
}
