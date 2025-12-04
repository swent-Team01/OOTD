package com.android.ootd.ui.comment

import android.content.Context
import android.net.Uri
import com.android.ootd.model.image.ImageCompressor
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.Comment
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.comments.CommentViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentViewModelUnitTest {

  private lateinit var viewModel: CommentViewModel
  private lateinit var postRepository: OutfitPostRepository
  private lateinit var userRepository: UserRepository
  private lateinit var imageCompressor: ImageCompressor
  private lateinit var context: Context
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    postRepository = mockk(relaxed = true)
    userRepository = mockk(relaxed = true)
    imageCompressor = mockk(relaxed = true)
    context = mockk(relaxed = true)

    viewModel = CommentViewModel(postRepository, userRepository, imageCompressor)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  // -------- getUserData tests --------

  @Test
  fun `getUserData returns cached user when available`() = runTest {
    val user = User(uid = "user1", username = "TestUser")

    // Manually populate cache
    viewModel.uiState.value.userData + ("user1" to user)
    coEvery { userRepository.getUser("user1") } returns user

    // First call - should cache
    val result1 = viewModel.getUserData("user1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Second call - should use cache
    val result2 = viewModel.getUserData("user1")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(user, result1)
    assertEquals(user, result2)
    // Should only call repository once (for first fetch)
    coVerify(exactly = 1) { userRepository.getUser("user1") }
  }

  @Test
  fun `getUserData fetches from repository when not cached`() = runTest {
    val user = User(uid = "user2", username = "NewUser")
    coEvery { userRepository.getUser("user2") } returns user

    val result = viewModel.getUserData("user2")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(user, result)
    coVerify { userRepository.getUser("user2") }
    assertTrue(viewModel.uiState.value.userData.containsKey("user2"))
  }

  @Test
  fun `getUserData returns null on repository failure`() = runTest {
    coEvery { userRepository.getUser("user3") } throws Exception("Network error")

    val result = viewModel.getUserData("user3")
    testDispatcher.scheduler.advanceUntilIdle()

    assertNull(result)
  }

  // -------- addComment tests --------

  @Test
  fun `addComment without image adds comment successfully`() = runTest {
    val comment =
        Comment(
            commentId = "comment1",
            ownerId = "user1",
            text = "Great post!",
            timestamp = 123456L,
            reactionImage = "")
    coEvery { postRepository.addCommentToPost("post1", "user1", "Great post!", null) } returns
        comment

    val result = viewModel.addComment("post1", "user1", "Great post!", null, context)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(result.isSuccess)
    assertEquals(comment, result.getOrNull())
    coVerify { postRepository.addCommentToPost("post1", "user1", "Great post!", null) }
    assertFalse(viewModel.uiState.value.isSubmitting)
    assertNull(viewModel.uiState.value.selectedImage)
  }

  @Test
  fun `addComment with image compresses and uploads`() = runTest {
    val imageUri = mockk<Uri>()
    val compressedData = byteArrayOf(1, 2, 3)
    val comment =
        Comment(
            commentId = "comment2",
            ownerId = "user1",
            text = "Nice!",
            timestamp = 123456L,
            reactionImage = "https://example.com/reaction.jpg")

    coEvery { imageCompressor.compressImage(imageUri, 200_000L, context) } returns compressedData
    coEvery { postRepository.addCommentToPost("post1", "user1", "Nice!", compressedData) } returns
        comment

    val result = viewModel.addComment("post1", "user1", "Nice!", imageUri, context)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(result.isSuccess)
    coVerify { imageCompressor.compressImage(imageUri, 200_000L, context) }
    coVerify { postRepository.addCommentToPost("post1", "user1", "Nice!", compressedData) }
  }

  @Test
  fun `addComment sets isSubmitting state correctly`() = runTest {
    val comment =
        Comment(
            commentId = "comment3",
            ownerId = "user1",
            text = "Test",
            timestamp = 123456L,
            reactionImage = "")
    coEvery { postRepository.addCommentToPost(any(), any(), any(), any()) } coAnswers
        {
          // Check state during execution
          assertTrue(viewModel.uiState.value.isSubmitting)
          comment
        }

    assertFalse(viewModel.uiState.value.isSubmitting)

    viewModel.addComment("post1", "user1", "Test", null, context)
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isSubmitting)
  }

  @Test
  fun `addComment handles failure and sets error message`() = runTest {
    coEvery { postRepository.addCommentToPost(any(), any(), any(), any()) } throws
        Exception("Upload failed")

    val result = viewModel.addComment("post1", "user1", "Test", null, context)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(result.isFailure)
    assertFalse(viewModel.uiState.value.isSubmitting)
    assertTrue(viewModel.uiState.value.errorMessage?.contains("Failed to add comment") == true)
  }

  // -------- deleteComment tests --------

  @Test
  fun `deleteComment removes comment successfully`() = runTest {
    val comment =
        Comment(
            commentId = "comment1",
            ownerId = "user1",
            text = "Delete me",
            timestamp = 123456L,
            reactionImage = "")
    coEvery { postRepository.deleteCommentFromPost("post1", comment) } returns Unit

    val result = viewModel.deleteComment("post1", comment)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(result.isSuccess)
    coVerify { postRepository.deleteCommentFromPost("post1", comment) }
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun `deleteComment sets isLoading state correctly`() = runTest {
    val comment =
        Comment(
            commentId = "comment2",
            ownerId = "user1",
            text = "Test",
            timestamp = 123456L,
            reactionImage = "")
    coEvery { postRepository.deleteCommentFromPost(any(), any()) } coAnswers
        {
          // Check state during execution
          assertTrue(viewModel.uiState.value.isLoading)
        }

    assertFalse(viewModel.uiState.value.isLoading)

    viewModel.deleteComment("post1", comment)
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun `deleteComment handles failure and sets error message`() = runTest {
    val comment =
        Comment(
            commentId = "comment3",
            ownerId = "user1",
            text = "Test",
            timestamp = 123456L,
            reactionImage = "")
    coEvery { postRepository.deleteCommentFromPost(any(), any()) } throws Exception("Delete failed")

    val result = viewModel.deleteComment("post1", comment)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(result.isFailure)
    assertFalse(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.errorMessage?.contains("Failed to delete comment") == true)
  }

  // -------- setSelectedImage tests --------

  @Test
  fun `setSelectedImage updates state`() {
    val uri = mockk<Uri>()

    assertNull(viewModel.uiState.value.selectedImage)

    viewModel.setSelectedImage(uri)
    assertEquals(uri, viewModel.uiState.value.selectedImage)

    viewModel.setSelectedImage(null)
    assertNull(viewModel.uiState.value.selectedImage)
  }

  // -------- clearError tests --------

  @Test
  fun `clearError removes error message`() = runTest {
    // Trigger an error
    coEvery { postRepository.addCommentToPost(any(), any(), any(), any()) } throws
        Exception("Error")

    viewModel.addComment("post1", "user1", "Test", null, context)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify error exists
    assertTrue(viewModel.uiState.value.errorMessage != null)

    // Clear error
    viewModel.clearError()
    assertNull(viewModel.uiState.value.errorMessage)
  }
}
