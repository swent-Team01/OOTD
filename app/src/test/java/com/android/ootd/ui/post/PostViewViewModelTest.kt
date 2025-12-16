package com.android.ootd.ui.post

import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.map.Location
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.UserRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import java.lang.reflect.Field
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PostViewViewModelTest {

  @RelaxedMockK private lateinit var postRepository: OutfitPostRepository
  @RelaxedMockK private lateinit var userRepository: UserRepository
  @RelaxedMockK private lateinit var likesRepository: LikesRepository
  @RelaxedMockK private lateinit var accountService: AccountService
  @RelaxedMockK private lateinit var itemsRepository: ItemsRepository
  @RelaxedMockK private lateinit var accountRepository: AccountRepository

  private lateinit var viewModel: PostViewViewModel
  private val samplePost =
      OutfitPost(
          postUID = "post-1",
          ownerId = "owner-1",
          name = "User",
          userProfilePicURL = "",
          outfitURL = "url",
          description = "desc",
          itemsID = emptyList(),
          timestamp = 0L,
          location = Location(1.0, 2.0, "loc"))

  @Before
  fun setup() {
    MockKAnnotations.init(this)
    val mockUser =
        com.android.ootd.model.user.User(
            uid = "owner-1", username = "Test User", profilePicture = "")
    // Mock the repositories to prevent init() from trying to load data
    coEvery { postRepository.getPostById(any()) } returns samplePost
    coEvery { userRepository.getUser(any()) } returns mockUser
    coEvery { likesRepository.getLikesForPost(any()) } returns emptyList()
    coEvery { itemsRepository.getFriendItemsForPost(any(), any()) } returns emptyList()
    coEvery { accountRepository.getStarredItems(any()) } returns emptyList()
    viewModel =
        PostViewViewModel(
            postId = samplePost.postUID,
            postRepository = postRepository,
            userRepository = userRepository,
            likesRepository = likesRepository,
            accountService = accountService,
            itemsRepository = itemsRepository,
            accountRepository = accountRepository)
    setState(PostViewUiState(post = samplePost))
  }

  @Test
  fun `savePostEdits updates description and location fields`() = runTest {
    val newLocation = Location(3.0, 4.0, "new-loc")
    coEvery { postRepository.updatePostFields(any(), any()) } returns Unit

    viewModel.savePostEdits("new-desc", newLocation)

    val updatesSlot = io.mockk.slot<Map<String, Any?>>()
    coVerify { postRepository.updatePostFields("post-1", capture(updatesSlot)) }
    val updates = updatesSlot.captured
    assertEquals("new-desc", updates["description"])
    val locMap = updates["location"] as Map<*, *>
    assertEquals("new-loc", locMap["name"])
    assertEquals(3.0, locMap["latitude"])
    assertEquals(4.0, locMap["longitude"])
  }

  @Test
  fun `deletePost invokes success callback and clears loading`() = runTest {
    var successCalled = false
    coEvery { postRepository.deletePost("post-1") } returns Unit

    viewModel.deletePost(onSuccess = { successCalled = true }, onError = {})

    assertTrue(successCalled)
    assertEquals(false, viewModel.uiState.value.isLoading)
  }

  @Test
  fun `deletePost invokes error callback when failing`() = runTest {
    var errorMsg: String? = null
    coEvery { postRepository.deletePost("post-1") } throws Exception("boom")

    viewModel.deletePost(onSuccess = {}, onError = { errorMsg = it })

    assertEquals("boom", errorMsg)
  }

  @Suppress("UNCHECKED_CAST")
  private fun setState(state: PostViewUiState) {
    val field: Field = PostViewViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val flow = field.get(viewModel) as MutableStateFlow<PostViewUiState>
    flow.value = state
  }
}
