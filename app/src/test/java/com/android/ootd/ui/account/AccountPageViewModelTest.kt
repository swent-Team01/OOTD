package com.android.ootd.ui.account

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import io.mockk.*
import kotlin.collections.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountPageViewModelTest {

  private lateinit var accountService: AccountService
  private lateinit var accountRepository: AccountRepository
  private lateinit var userRepository: UserRepository
  private lateinit var feedRepository: FeedRepository
  private lateinit var itemsRepository: ItemsRepository
  private lateinit var viewModel: AccountPageViewModel
  private val dispatcher = StandardTestDispatcher()

  private val userId = "user-123"
  private val sampleUser = User(uid = userId, username = "tester", profilePicture = "avatar.jpg")
  private val sampleAccount =
      Account(uid = userId, ownerId = userId, friendUids = listOf("friend-1"))
  private val samplePost =
      OutfitPost(postUID = "post-1", ownerId = userId, outfitURL = "url://outfit")
  private val starredItem =
      Item(
          itemUuid = "star-item",
          postUuids = emptyList(),
          ownerId = userId,
          image = ImageData("image-1", "url://img"),
          category = "Clothing",
          type = "Jacket")
  private val itemLookup = mapOf(starredItem.itemUuid to starredItem)

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)
    accountService = mockk(relaxed = true)
    accountRepository = mockk(relaxed = true)
    userRepository = mockk(relaxed = true)
    feedRepository = mockk(relaxed = true)
    itemsRepository = mockk(relaxed = true)
    every { accountService.currentUserId } returns userId
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `retrieveUserData loads starred items`() = runTest {
    stubCommonRepositories(ArrayDeque(listOf(listOf(starredItem.itemUuid))))

    createViewModel()
    advanceUntilIdle()

    assertEquals(listOf(starredItem), viewModel.uiState.value.starredItems)
  }

  @Test
  fun `toggleStar removes existing starred entry`() = runTest {
    stubCommonRepositories(ArrayDeque(listOf(listOf(starredItem.itemUuid), emptyList())))
    coEvery { accountRepository.toggleStarredItem(starredItem.itemUuid) } returns emptyList()

    createViewModel()
    advanceUntilIdle()

    viewModel.toggleStar(starredItem)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.starredItems.isEmpty())
    coVerify { accountRepository.toggleStarredItem(starredItem.itemUuid) }
  }

  @Test
  fun `toggleStar adds new starred entry`() = runTest {
    stubCommonRepositories(ArrayDeque(listOf(emptyList(), listOf(starredItem.itemUuid))))
    coEvery { accountRepository.toggleStarredItem(starredItem.itemUuid) } returns
        listOf(starredItem.itemUuid)

    createViewModel()
    advanceUntilIdle()

    viewModel.toggleStar(starredItem)
    advanceUntilIdle()

    assertEquals(listOf(starredItem), viewModel.uiState.value.starredItems)
    coVerify { accountRepository.toggleStarredItem(starredItem.itemUuid) }
  }

  private fun createViewModel() {
    viewModel =
        AccountPageViewModel(
            accountService = accountService,
            accountRepository = accountRepository,
            userRepository = userRepository,
            feedRepository = feedRepository,
            itemsRepository = itemsRepository)
  }

  private fun stubCommonRepositories(starredResponses: ArrayDeque<List<String>>) {
    var lastResponse = starredResponses.lastOrNull() ?: emptyList()
    coEvery { userRepository.getUser(userId) } returns sampleUser
    coEvery { accountRepository.getAccount(userId) } returns sampleAccount
    coEvery { feedRepository.getFeedForUids(listOf(userId)) } returns listOf(samplePost)
    coEvery { accountRepository.getStarredItems(userId) } answers
        {
          if (starredResponses.isNotEmpty()) {
            lastResponse = starredResponses.removeFirst()
          }
          lastResponse
        }
    coEvery { itemsRepository.getItemsByIds(any()) } answers
        {
          val ids = firstArg<List<String>>()
          ids.mapNotNull { itemLookup[it] }
        }
  }
}
