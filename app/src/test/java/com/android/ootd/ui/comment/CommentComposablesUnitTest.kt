package com.android.ootd.ui.comment

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.posts.Comment
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.ui.comments.CommentBottomSheet
import com.android.ootd.ui.comments.CommentScreenTestTags
import com.android.ootd.ui.comments.CommentUiState
import com.android.ootd.ui.comments.CommentViewModel
import com.android.ootd.ui.comments.formatTimestamp
import io.mockk.*
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommentBottomSheetTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockViewModel: CommentViewModel
  private lateinit var uiStateFlow: MutableStateFlow<CommentUiState>

  private fun setupMockViewModel() {
    mockViewModel = mockk(relaxed = true)
    uiStateFlow = MutableStateFlow(CommentUiState())

    every { mockViewModel.uiState } returns uiStateFlow
    coEvery { mockViewModel.getUserData(any()) } returns
        User(uid = "user1", username = "TestUser", profilePicture = "")
    coEvery { mockViewModel.addComment(any(), any(), any(), any(), any()) } returns
        Result.success(
            Comment(
                commentId = "comment1",
                ownerId = "user1",
                text = "Test comment",
                timestamp = System.currentTimeMillis(),
                reactionImage = ""))
    coEvery { mockViewModel.deleteComment(any(), any()) } returns Result.success(Unit)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  // -------- Header tests --------
  @Test
  fun formatTimestamp_allTimeRanges_returnCorrectFormats() {
    val now = System.currentTimeMillis()

    val testCases =
        listOf(
            30_000L to "Just now", // 30 seconds
            59_000L to "Just now", // 59 seconds
            60_000L to "1m ago", // 1 minute
            5 * 60_000L to "5m ago", // 5 minutes
            59 * 60_000L to "59m ago", // 59 minutes
            60 * 60_000L to "1h ago", // 1 hour
            3 * 60 * 60_000L to "3h ago", // 3 hours
            23 * 60 * 60_000L to "23h ago", // 23 hours
            24 * 60 * 60_000L to "1d ago", // 1 day
            2 * 24 * 60 * 60_000L to "2d ago", // 2 days
            7 * 24 * 60 * 60_000L to "7d ago", // 7 days
            30 * 24 * 60 * 60_000L to "30d ago" // 30 days
            )

    testCases.forEach { (millisAgo, expected) ->
      val timestamp = now - millisAgo
      val result = formatTimestamp(timestamp)
      assertEquals(expected, result)
    }
  }

  @Test
  fun commentBottomSheet_displaysHeader_withCorrectCommentCount() {
    setupMockViewModel()
    val post = samplePost(commentsCount = 3)

    composeTestRule.setContent {
      CommentBottomSheet(
          post = post,
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithTag(CommentScreenTestTags.COMMENT_BOTTOM_SHEET).assertIsDisplayed()
    composeTestRule.onNode(hasText("Comments (3)"), useUnmergedTree = true).assertIsDisplayed()
  }

  // -------- Empty state tests --------

  @Test
  fun commentsList_showsEmptyState_whenNoComments() {
    setupMockViewModel()
    val post = samplePost(commentsCount = 0)

    composeTestRule.setContent {
      CommentBottomSheet(
          post = post,
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithTag(CommentScreenTestTags.EMPTY_COMMENTS_STATE).assertIsDisplayed()
    composeTestRule.onNode(hasText("No comments yet"), useUnmergedTree = true).assertIsDisplayed()
    composeTestRule
        .onNode(hasText("Be the first to comment!"), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  // -------- Comments list tests --------

  @Test
  fun commentsList_displaysComments_whenPresent() {
    setupMockViewModel()
    val comments =
        listOf(
            Comment(
                commentId = "c1",
                ownerId = "user1",
                text = "First comment",
                timestamp = System.currentTimeMillis(),
                reactionImage = ""),
            Comment(
                commentId = "c2",
                ownerId = "user2",
                text = "Second comment",
                timestamp = System.currentTimeMillis(),
                reactionImage = ""))
    val post = samplePost(comments = comments)

    composeTestRule.setContent {
      CommentBottomSheet(
          post = post,
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithTag(CommentScreenTestTags.COMMENTS_LIST).assertIsDisplayed()
    composeTestRule.onNode(hasText("First comment"), useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNode(hasText("Second comment"), useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun commentItem_displaysReactionImage_whenPresent() {
    setupMockViewModel()
    val commentWithReaction =
        Comment(
            commentId = "c1",
            ownerId = "user1",
            text = "Comment with reaction",
            timestamp = System.currentTimeMillis(),
            reactionImage = "https://example.com/reaction.jpg")
    val post = samplePost(comments = listOf(commentWithReaction))

    composeTestRule.setContent {
      CommentBottomSheet(
          post = post,
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithTag("${CommentScreenTestTags.REACTION_IMAGE}_c1").assertIsDisplayed()
  }

  @Test
  fun commentItem_showsDeleteButton_forOwnComment() {
    setupMockViewModel()
    val ownComment =
        Comment(
            commentId = "c1",
            ownerId = "currentUser",
            text = "My comment",
            timestamp = System.currentTimeMillis(),
            reactionImage = "")
    val post = samplePost(comments = listOf(ownComment))

    composeTestRule.setContent {
      CommentBottomSheet(
          post = post,
          currentUserId = "currentUser",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule
        .onNodeWithTag("${CommentScreenTestTags.DELETE_COMMENT_BUTTON}_c1")
        .assertIsDisplayed()
  }

  @Test
  fun commentItem_hidesDeleteButton_forOtherUsersComment() {
    setupMockViewModel()
    val otherComment =
        Comment(
            commentId = "c1",
            ownerId = "otherUser",
            text = "Other comment",
            timestamp = System.currentTimeMillis(),
            reactionImage = "")
    val post = samplePost(comments = listOf(otherComment))

    composeTestRule.setContent {
      CommentBottomSheet(
          post = post,
          currentUserId = "currentUser",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule
        .onNodeWithTag("${CommentScreenTestTags.DELETE_COMMENT_BUTTON}_c1")
        .assertDoesNotExist()
  }

  // -------- Add comment section tests --------

  @Test
  fun addCommentSection_displaysTextFieldAndButtons() {
    setupMockViewModel()

    composeTestRule.setContent {
      CommentBottomSheet(
          post = samplePost(),
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithTag(CommentScreenTestTags.ADD_COMMENT_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CommentScreenTestTags.COMMENT_TEXT_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CommentScreenTestTags.ADD_IMAGE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CommentScreenTestTags.POST_COMMENT_BUTTON).assertIsDisplayed()
  }

  /*
  @Test
  fun commentTextField_showsCharacterCounter() {
    setupMockViewModel()

    composeTestRule.setContent {
      CommentBottomSheet(
          post = samplePost(),
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    val textField = composeTestRule.onNodeWithTag(CommentScreenTestTags.COMMENT_TEXT_FIELD)

    // Input 50 characters
    textField.performTextInput("A".repeat(50))

    // Should show 450 characters remaining
    composeTestRule
        .onNode(hasText("450 characters remaining"), useUnmergedTree = true)
        .assertIsDisplayed()
  }
  */

  @Test
  fun postButton_isDisabled_whenTextIsEmpty() {
    setupMockViewModel()

    composeTestRule.setContent {
      CommentBottomSheet(
          post = samplePost(),
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithTag(CommentScreenTestTags.POST_COMMENT_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun postButton_isEnabled_whenTextIsNotEmpty() {
    setupMockViewModel()

    composeTestRule.setContent {
      CommentBottomSheet(
          post = samplePost(),
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule
        .onNodeWithTag(CommentScreenTestTags.COMMENT_TEXT_FIELD)
        .performTextInput("Great post!")

    composeTestRule.onNodeWithTag(CommentScreenTestTags.POST_COMMENT_BUTTON).assertIsEnabled()
  }

  @Test
  fun selectedImage_displaysPreview() {
    setupMockViewModel()
    val mockUri = mockk<Uri>()
    uiStateFlow.value = CommentUiState(selectedImage = mockUri)

    composeTestRule.setContent {
      CommentBottomSheet(
          post = samplePost(),
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    composeTestRule.onNodeWithTag(CommentScreenTestTags.SELECTED_IMAGE_PREVIEW).assertIsDisplayed()
  }

  @Test
  fun postButton_showsLoadingIndicator_whenSubmitting() {
    setupMockViewModel()
    uiStateFlow.value = CommentUiState(isSubmitting = true)

    composeTestRule.setContent {
      CommentBottomSheet(
          post = samplePost(),
          currentUserId = "user1",
          onDismiss = {},
          onCommentAdded = {},
          viewModel = mockViewModel)
    }

    // Button should be disabled during submission
    composeTestRule.onNodeWithTag(CommentScreenTestTags.POST_COMMENT_BUTTON).assertIsNotEnabled()
  }

  // -------- Helper functions --------

  private fun samplePost(
      postId: String = "post1",
      comments: List<Comment> = emptyList(),
      commentsCount: Int = comments.size
  ): OutfitPost {
    val actualComments =
        if (comments.isEmpty() && commentsCount > 0) {
          List(commentsCount) { index ->
            Comment(
                commentId = "c$index",
                ownerId = "user$index",
                text = "Comment $index",
                timestamp = System.currentTimeMillis(),
                reactionImage = "")
          }
        } else {
          comments
        }

    return OutfitPost(
        postUID = postId,
        ownerId = "owner1",
        name = "Test Post",
        userProfilePicURL = "",
        outfitURL = "",
        description = "Test description",
        itemsID = emptyList(),
        timestamp = System.currentTimeMillis(),
        comments = actualComments)
  }
}
