package com.android.ootd.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.inventory.InventoryScreenTestTags
import com.android.ootd.ui.theme.OOTDTheme
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockFeedRepository: FeedRepository
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var viewModel: AccountPageViewModel

  private var onEditAccountCalled = false
  private var onPostClickCalledWithId: String? = null

  private val testUser =
      User(
          uid = "test-uid",
          username = "testuser",
          profilePicture = "https://example.com/profile.jpg")

  private val testAccount =
      Account(
          uid = "test-uid",
          ownerId = "test-uid",
          username = "testuser",
          profilePicture = "https://example.com/profile.jpg",
          friendUids = listOf("friend1", "friend2", "friend3"),
          isPrivate = false)

  private val testPosts =
      listOf(
          OutfitPost(
              postUID = "post1",
              name = "testuser",
              ownerId = "test-uid",
              outfitURL = "https://example.com/post1.jpg",
              description = "First post"),
          OutfitPost(
              postUID = "post2",
              name = "testuser",
              ownerId = "test-uid",
              outfitURL = "https://example.com/post2.jpg",
              description = "Second post"),
          OutfitPost(
              postUID = "post3",
              name = "testuser",
              ownerId = "test-uid",
              outfitURL = "https://example.com/post3.jpg",
              description = "Third post"))

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    mockFeedRepository = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)

    every { mockAccountService.currentUserId } returns "test-uid"
    coEvery { mockUserRepository.getUser("test-uid") } returns testUser
    coEvery { mockAccountRepository.getAccount("test-uid") } returns testAccount
    coEvery { mockFeedRepository.getFeedForUids(listOf("test-uid")) } returns testPosts

    onEditAccountCalled = false
    onPostClickCalledWithId = null
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  private fun setContent() {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)

    composeTestRule.setContent {
      OOTDTheme {
        AccountPage(
            accountModel = viewModel,
            onEditAccount = { onEditAccountCalled = true },
            onPostClick = { postId -> onPostClickCalledWithId = postId })
      }
    }
  }

  private fun setContent(state: AccountPageViewState) {
    composeTestRule.setContent {
      OOTDTheme {
        AccountPageContent(
            uiState = state,
            onEditAccount = {},
            onPostClick = {},
            onSelectTab = {},
            onToggleStar = {})
      }
    }
  }

  @Test
  fun screen_displays_user_posts() {
    setContent()

    // Should display all posts (we can check by counting nodes with POST_TAG)
    composeTestRule
        .onAllNodesWithTag(AccountPageTestTags.POST_TAG)
        .fetchSemanticsNodes()
        .size
        .let { count -> assert(count == 3) { "Expected 3 posts, found $count" } }
  }

  @Test
  fun clicking_post_calls_onPostClick_with_correct_id() {
    setContent()

    // Click on first post
    composeTestRule.onAllNodesWithTag(AccountPageTestTags.POST_TAG)[0].performClick()

    assert(onPostClickCalledWithId != null) { "onPostClick should have been called" }
    assert(onPostClickCalledWithId in listOf("post1", "post2", "post3")) {
      "Post ID should be one of the test posts"
    }
  }

  @Test
  fun screen_displays_loading_indicator_when_loading() {
    coEvery { mockUserRepository.getUser("test-uid") } coAnswers
        {
          kotlinx.coroutines.delay(5000)
          testUser
        }

    setContent()

    composeTestRule.onNodeWithTag(AccountPageTestTags.LOADING).assertIsDisplayed()
  }

  @Test
  fun screen_handles_empty_posts_list() {
    coEvery { mockFeedRepository.getFeedForUids(listOf("test-uid")) } returns emptyList()

    setContent()

    composeTestRule.onNodeWithTag(AccountPageTestTags.POST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AccountPageTestTags.POST_TAG).assertTextContains("No posts !")
  }

  @Test
  fun screen_displays_correct_avatar_letter_for_lowercase_username() {
    coEvery { mockUserRepository.getUser("test-uid") } returns
        testUser.copy(username = "alice", profilePicture = "")
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        testAccount.copy(username = "alice", profilePicture = "")

    setContent()

    composeTestRule
        .onNodeWithTag(AccountPageTestTags.AVATAR_LETTER)
        .assertIsDisplayed()
        .assertTextContains("A")
  }

  @Test
  fun screen_handles_username_with_special_characters() {
    coEvery { mockUserRepository.getUser("test-uid") } returns
        testUser.copy(username = "@user123", profilePicture = "")
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        testAccount.copy(username = "@user123", profilePicture = "")

    setContent()

    composeTestRule
        .onNodeWithTag(AccountPageTestTags.USERNAME_TEXT)
        .assertIsDisplayed()
        .assertTextContains("@user123")
  }

  @Test
  fun screen_structure_is_complete() {
    setContent()

    // Verify all main components are present
    composeTestRule.onNodeWithTag(AccountPageTestTags.TITLE_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AccountPageTestTags.SETTINGS_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AccountPageTestTags.USERNAME_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AccountPageTestTags.FRIEND_COUNT_TEXT).assertIsDisplayed()
  }

  @Test
  fun screen_handles_large_number_of_posts() {
    val manyPosts =
        (1..20).map { index ->
          OutfitPost(
              postUID = "post$index",
              name = "testuser",
              ownerId = "test-uid",
              outfitURL = "https://example.com/post$index.jpg",
              description = "Post $index")
        }

    coEvery { mockFeedRepository.getFeedForUids(listOf("test-uid")) } returns manyPosts

    setContent()

    composeTestRule
        .onAllNodesWithTag(AccountPageTestTags.POST_TAG)
        .fetchSemanticsNodes()
        .size
        .let { count -> assert(count == 20) { "Expected 20 posts, found $count" } }
  }

  @Test
  fun starredTab_showsEmptyMessage_whenNoItems() {
    val state =
        AccountPageViewState(
            username = "starUser",
            selectedTab = AccountTab.Starred,
            starredItems = emptyList(),
            posts = emptyList())

    setContent(state)

    composeTestRule
        .onNodeWithText("Star items from your inventory to build your wishlist.")
        .assertIsDisplayed()
  }

  @Test
  fun starredTab_displaysInventoryGrid_whenItemsExist() {
    val starredItem =
        Item(
            itemUuid = "item-1",
            postUuids = emptyList(),
            image = ImageData("img", ""),
            category = "Unknown Category",
            ownerId = "starUser")
    val state =
        AccountPageViewState(
            username = "starUser",
            selectedTab = AccountTab.Starred,
            starredItems = listOf(starredItem),
            posts = emptyList())

    setContent(state)

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.ITEMS_GRID).assertIsDisplayed()
  }
}
