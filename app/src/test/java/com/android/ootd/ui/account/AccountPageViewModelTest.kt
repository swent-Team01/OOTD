package com.android.ootd.ui.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.android.ootd.ui.theme.OOTDTheme
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountPageViewModelTest {

  @get:Rule val composeRule = createComposeRule()
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
  private val defaultItemLookup = mapOf(starredItem.itemUuid to starredItem)

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
    assertEquals(setOf(starredItem.itemUuid), viewModel.uiState.value.starredItemIds)
  }

  @Test
  fun `retrieveUserData loads friend owned starred items`() = runTest {
    val friendItem = starredItem.copy(itemUuid = "friend-item", ownerId = "friend-123")
    val responses = ArrayDeque(listOf(listOf(friendItem.itemUuid)))
    stubCommonRepositories(responses, mapOf(friendItem.itemUuid to friendItem))

    createViewModel()
    advanceUntilIdle()

    assertEquals(listOf(friendItem), viewModel.uiState.value.starredItems)
    assertEquals(setOf(friendItem.itemUuid), viewModel.uiState.value.starredItemIds)
  }

  @Test
  fun `toggleStar removes existing starred entry`() = runTest {
    stubCommonRepositories(ArrayDeque(listOf(listOf(starredItem.itemUuid), emptyList())))
    coEvery { accountRepository.toggleStarredItem(starredItem.itemUuid) } returns emptyList()

    createViewModel()
    advanceUntilIdle()

    viewModel.toggleStar(starredItem)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.starredItems.isNotEmpty())
    assertTrue(viewModel.uiState.value.starredItemIds.isEmpty())
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
    assertEquals(setOf(starredItem.itemUuid), viewModel.uiState.value.starredItemIds)
    coVerify { accountRepository.toggleStarredItem(starredItem.itemUuid) }
  }

  @Test
  fun `selectTab updates selectedTab state`() = runTest {
    stubCommonRepositories(ArrayDeque(listOf(emptyList())))

    createViewModel()
    advanceUntilIdle()

    assertEquals(AccountTab.Posts, viewModel.uiState.value.selectedTab)
    viewModel.selectTab(AccountTab.Starred)

    assertEquals(AccountTab.Starred, viewModel.uiState.value.selectedTab)
  }

  @Test
  fun `AccountPageContent renders preview state and forwards tab selection`() {
    val sampleState =
        AccountPageViewState(
            username = "JohnDoe",
            profilePicture = "",
            friends = listOf("f1", "f2", "f3"),
            friendDetails =
                listOf(
                    User(uid = "f1", username = "Alice"),
                    User(uid = "f2", username = "Bob"),
                    User(uid = "f3", username = "Cara")),
            posts = emptyList(),
            isLoading = false,
            starredItems =
                listOf(
                    Item(
                        itemUuid = "1",
                        postUuids = emptyList(),
                        image = ImageData("1", ""),
                        category = "Clothing",
                        ownerId = "user")))
    var selected: AccountTab? = null

    composeRule.setContent {
      OOTDTheme {
        AccountPageContent(
            uiState = sampleState,
            onEditAccount = {},
            onPostClick = {},
            onSelectTab = { selected = it },
            onToggleStar = {})
      }
    }

    composeRule.onNodeWithTag(AccountPageTestTags.USERNAME_TEXT).assertIsDisplayed()
    composeRule.onNodeWithText("JohnDoe").assertIsDisplayed()
    composeRule.onNodeWithTag(AccountPageTestTags.FRIEND_COUNT_TEXT).assertIsDisplayed()
    composeRule.onNodeWithTag(AccountPageTestTags.STARRED_TAB).assertIsDisplayed().performClick()
    composeRule.runOnIdle { assertEquals(AccountTab.Starred, selected) }
  }

  @Test
  fun `clicking friend count shows friend list dialog`() {
    val sampleState =
        AccountPageViewState(
            username = "JohnDoe",
            profilePicture = "",
            friends = listOf("f1", "f2"),
            friendDetails =
                listOf(User(uid = "f1", username = "Alice"), User(uid = "f2", username = "Bob")))

    composeRule.setContent {
      OOTDTheme {
        AccountPageContent(
            uiState = sampleState,
            onEditAccount = {},
            onPostClick = {},
            onSelectTab = {},
            onToggleStar = {})
      }
    }

    composeRule.onNodeWithTag(AccountPageTestTags.FRIEND_COUNT_TEXT).performClick()
    composeRule.onNodeWithTag(AccountPageTestTags.FRIEND_LIST_DIALOG).assertIsDisplayed()
    composeRule.onNodeWithText("Alice").assertIsDisplayed()
    composeRule.onNodeWithText("Bob").assertIsDisplayed()
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

  private fun stubCommonRepositories(
      starredResponses: ArrayDeque<List<String>>,
      lookup: Map<String, Item> = defaultItemLookup
  ) {
    var lastResponse = starredResponses.lastOrNull() ?: emptyList()
    coEvery { userRepository.getUser(userId) } returns sampleUser
    coEvery { accountRepository.getAccount(userId) } returns sampleAccount
    coEvery { feedRepository.getFeedForUids(listOf(userId)) } returns listOf(samplePost)
    coEvery { accountRepository.refreshStarredItems(userId) } answers
        {
          if (starredResponses.isNotEmpty()) {
            lastResponse = starredResponses.removeFirst()
          }
          lastResponse
        }
    coEvery { itemsRepository.getItemsByIdsAcrossOwners(any()) } answers
        {
          val ids = firstArg<List<String>>()
          ids.mapNotNull { lookup[it] }
        }
  }
}
