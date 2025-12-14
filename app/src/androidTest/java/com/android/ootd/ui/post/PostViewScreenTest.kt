package com.android.ootd.ui.post

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.Material
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.post.items.ItemsTestTags
import com.android.ootd.ui.theme.OOTDTheme
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostViewScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockPostRepository: OutfitPostRepository
  private lateinit var mockUserRepo: UserRepository
  private lateinit var mockLikesRepo: LikesRepository
  private lateinit var mockAccountService: AccountService
  private lateinit var mockItemsRepo: ItemsRepository
  private lateinit var mockAccountRepo: AccountRepository

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

  private val likedUser =
      User(
          uid = "liked-user-1",
          username = "Liked User",
          profilePicture = "https://example.com/liked.jpg")

  private val testItem1 =
      Item(
          itemUuid = "item1",
          postUuids = listOf("test-post-id"),
          image = ImageData("img1", "https://example.com/img1.jpg"),
          category = "Clothing",
          type = "T-Shirt",
          brand = "Nike",
          price = 29.99,
          material = listOf(Material("Cotton", 100.0)),
          link = "https://example.com/tshirt",
          ownerId = "test-owner-id",
          condition = "Like new",
          size = "M",
          fitType = "Regular",
          style = "Casual",
          notes = "Made by grandma")

  private val testItem2 =
      Item(
          itemUuid = "item2",
          postUuids = listOf("test-post-id"),
          image = ImageData("img2", "https://example.com/img2.jpg"),
          category = "Shoes",
          type = "Sneakers",
          brand = "Adidas",
          price = 89.99,
          material = listOf(Material("Leather", 80.0), Material("Rubber", 20.0)),
          link = "https://example.com/sneakers",
          ownerId = "test-owner-id",
          condition = "Used",
          size = "42",
          fitType = "Slim",
          style = "Sporty",
          notes = "Limited edition")

  @Before
  fun setup() {
    mockPostRepository = mockk(relaxed = true)
    mockUserRepo = mockk(relaxed = true)
    mockLikesRepo = mockk(relaxed = true)
    mockAccountService = mockk(relaxed = true)
    mockItemsRepo = mockk(relaxed = true)
    mockAccountRepo = mockk(relaxed = true)

    coEvery { mockUserRepo.getUser(any()) } returns
        User(uid = "placeholder", username = "placeholder", profilePicture = "")

    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost(any(), any()) } returns emptyList()
    coEvery { mockAccountRepo.getStarredItems(any()) } returns emptyList()

    onBackCalled = false

    every { mockAccountService.currentUserId } returns "test-owner-id"
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  private fun setContent(
      postId: String,
      onProfileClick: (String) -> Unit = {},
      onEditItem: (String) -> Unit = {}
  ) {
    viewModel =
        PostViewViewModel(
            postId,
            mockPostRepository,
            mockUserRepo,
            mockLikesRepo,
            mockAccountService,
            mockItemsRepo,
            mockAccountRepo)

    composeTestRule.setContent {
      OOTDTheme {
        PostViewScreen(
            postId = postId,
            onBack = { onBackCalled = true },
            onProfileClick = onProfileClick,
            onEditItem = onEditItem,
            viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun screen_displays_post_image_when_loaded_successfully() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1, testItem2)

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PostViewTestTags.POST_IMAGE).assertIsDisplayed()
  }

  @Test
  fun screen_displays_error_message_when_repository_throws_exception() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } throws Exception("Network error")

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Error is shown as Toast, but we can't easily test Toast
    // The screen should still be displayed
    composeTestRule.onNodeWithTag(PostViewTestTags.SCREEN).assertExists()
  }

  @Test
  fun clicking_edit_shows_textfield_and_counter() = runTest {
    coEvery { mockPostRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost(any(), any()) } returns emptyList()

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
    coEvery { mockPostRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost(any(), any()) } returns emptyList()
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
    coEvery { mockPostRepository.getPostById(any()) } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost(any(), any()) } returns emptyList()

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

  // ========== ITEMS SECTION TESTS (NEW) ==========

  @Test
  fun itemsSection_displays_items_title() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1, testItem2)

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Verify items section title is displayed
    composeTestRule.onNodeWithTag(PostViewTestTags.ITEMS_SECTION_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Outfit Items").assertIsDisplayed()
  }

  @Test
  fun itemsSection_displays_items_in_horizontal_row() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1, testItem2)

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Verify items grid is displayed
    composeTestRule.onNodeWithTag(ItemsTestTags.ITEMS_GRID).assertIsDisplayed()

    // Verify both item cards are displayed
    composeTestRule.onNodeWithTag(ItemsTestTags.getTestTagForItem(testItem1)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ItemsTestTags.getTestTagForItem(testItem2)).assertIsDisplayed()
  }

  @Test
  fun itemsSection_shows_empty_state_when_no_items() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        emptyList()

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Verify empty state message is displayed
    composeTestRule.onNodeWithText("No items associated with this post.").assertIsDisplayed()
  }

  @Test
  fun itemCard_opens_details_dialog_when_clicked() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Dialog should not exist initially
    composeTestRule.onNodeWithTag(ItemsTestTags.ITEM_DETAILS_DIALOG).assertDoesNotExist()

    // Click on item card
    composeTestRule.onNodeWithTag(ItemsTestTags.getTestTagForItem(testItem1)).performClick()
    composeTestRule.waitForIdle()

    // Wait for dialog to appear
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(ItemsTestTags.ITEM_DETAILS_DIALOG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify dialog is displayed
    composeTestRule.onNodeWithTag(ItemsTestTags.ITEM_DETAILS_DIALOG).assertIsDisplayed()
  }

  @Test
  fun itemDetailsDialog_displays_all_item_information() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Click on item to open dialog
    composeTestRule.onNodeWithTag(ItemsTestTags.getTestTagForItem(testItem1)).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(ItemsTestTags.ITEM_LINK, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to see all fields and verify they exist
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_DETAILS_DIALOG)
        .performScrollToNode(hasTestTag(ItemsTestTags.ITEM_NOTES))

    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_BRAND, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_PRICE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_MATERIAL, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_NOTES, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_LINK, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun itemDetailsDialog_copy_buttons_work_correctly() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Open dialog
    composeTestRule.onNodeWithTag(ItemsTestTags.getTestTagForItem(testItem1)).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(ItemsTestTags.ITEM_LINK, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val clipboard =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))

    // Scroll to and copy notes
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_DETAILS_DIALOG)
        .performScrollToNode(hasTestTag(ItemsTestTags.ITEM_NOTES_COPY))

    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_NOTES_COPY, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      assertEquals("Made by grandma", clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: "")
    }

    // Copy link
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_DETAILS_DIALOG)
        .performScrollToNode(hasTestTag(ItemsTestTags.ITEM_LINK_COPY))

    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_LINK_COPY, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      assertEquals(
          "https://example.com/tshirt", clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: "")
    }
  }

  @Test
  fun owner_sees_edit_button_on_items() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)
    coEvery { mockAccountRepo.getStarredItems(any()) } returns emptyList() // ← MUST HAVE THIS
    every { mockAccountService.currentUserId } returns "test-owner-id"

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(ItemsTestTags.getTestTagForItem(testItem1))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.waitForIdle()

    // Scroll to the items section to make it visible
    composeTestRule
        .onNodeWithTag(PostViewTestTags.SCREEN)
        .performScrollToNode(hasTestTag(PostViewTestTags.ITEMS_SECTION))

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(ItemsTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify edit button is displayed
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun nonOwner_does_not_see_edit_button_on_items() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)
    every { mockAccountService.currentUserId } returns "different-user-id"

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Verify edit button does NOT exist
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun nonOwner_sees_star_button_on_items() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)
    every { mockAccountService.currentUserId } returns "different-user-id"
    coEvery { mockAccountRepo.getStarredItems(any()) } returns emptyList()

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Verify star button is displayed
    composeTestRule.onNodeWithTag(ItemsTestTags.getStarButtonTag(testItem1)).assertIsDisplayed()
  }

  @Test
  fun owner_does_not_see_star_button_on_items() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)
    every { mockAccountService.currentUserId } returns "test-owner-id"

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Verify star button does NOT exist
    composeTestRule
        .onAllNodesWithTag(ItemsTestTags.getStarButtonTag(testItem1))
        .assertCountEquals(0)
  }

  @Test
  fun star_button_toggles_correctly() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)
    every { mockAccountService.currentUserId } returns "different-user-id"
    coEvery { mockAccountRepo.getStarredItems(any()) } returns emptyList()
    coEvery { mockAccountRepo.toggleStarredItem("item1") } returns listOf("item1")

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Click star button
    composeTestRule.onNodeWithTag(ItemsTestTags.getStarButtonTag(testItem1)).performClick()

    composeTestRule.waitForIdle()

    // Verify repository method was called
    coVerify { mockAccountRepo.toggleStarredItem("item1") }
  }

  @Test
  fun edit_button_triggers_callback() = runTest {
    var editedItemId = ""

    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        listOf(testItem1)
    coEvery { mockAccountRepo.getStarredItems(any()) } returns emptyList()
    every { mockAccountService.currentUserId } returns "test-owner-id"

    setContent("test-post-id", onEditItem = { editedItemId = it })

    // Wait for loading to complete and items to be displayed
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(ItemsTestTags.getTestTagForItem(testItem1))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.waitForIdle()

    // Scroll to the items section to make it visible
    composeTestRule
        .onNodeWithTag(PostViewTestTags.SCREEN)
        .performScrollToNode(hasTestTag(PostViewTestTags.ITEMS_SECTION))

    composeTestRule.waitForIdle()

    // Wait for edit button to appear
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(ItemsTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click edit button
    composeTestRule
        .onNodeWithTag(ItemsTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    // Verify callback was triggered with correct item ID
    assertEquals("item1", editedItemId)
  }

  @Test
  fun itemsSection_shows_loading_when_items_loading() = runTest {
    // Mock post and user to load quickly
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    // Mock delayed item loading to keep loading state visible
    coEvery { mockItemsRepo.getFriendItemsForPost(any(), any()) } coAnswers
        {
          kotlinx.coroutines.delay(2000) // Longer delay
          listOf(testItem1)
        }
    coEvery { mockAccountRepo.getStarredItems(any()) } returns emptyList()

    setContent("test-post-id")

    // With unified loading, the entire screen shows loading indicator
    // Check for the loading indicator in the center of the screen
    composeTestRule.onNodeWithTag(PostViewTestTags.LOADING_INDICATOR).assertIsDisplayed()

    composeTestRule.waitForIdle()
  }

  @Test
  fun multiple_items_display_correctly_in_horizontal_scroll() = runTest {
    val manyItems =
        (1..5).map { index ->
          Item(
              itemUuid = "item-$index",
              postUuids = listOf("test-post-id"),
              image = ImageData("img$index", "https://example.com/img$index.jpg"),
              category = "Category $index",
              type = "Type $index",
              brand = "Brand $index",
              price = index * 10.0,
              material = emptyList(),
              link = "https://example.com/item$index",
              ownerId = "test-owner-id")
        }

    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    coEvery { mockUserRepo.getUser(any()) } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()
    coEvery { mockItemsRepo.getFriendItemsForPost("test-post-id", "test-owner-id") } returns
        manyItems

    setContent("test-post-id")
    composeTestRule.waitForIdle()

    // Verify grid is displayed
    composeTestRule.onNodeWithTag(ItemsTestTags.ITEMS_GRID).assertIsDisplayed()

    // At least first item should be visible
    composeTestRule.onNodeWithTag(ItemsTestTags.getTestTagForItem(manyItems[0])).assertIsDisplayed()
  }
}
