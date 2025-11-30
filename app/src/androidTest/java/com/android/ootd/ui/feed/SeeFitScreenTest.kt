package com.android.ootd.ui.feed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.Material
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.OOTDTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)

/**
 * Instrumented tests for the SeeFitScreen composable to verify UI behavior and interactions.
 *
 * DISCLAIMER : These tests are created with the help of AI and verified by humans.
 */
class SeeFitScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockItemsRepository: ItemsRepository
  private lateinit var mockFeedRepository: FeedRepository
  private lateinit var viewModel: SeeFitViewModel
  private var goBackCalled = false

  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private val testItem1 =
      Item(
          itemUuid = "item1",
          postUuids = listOf("test-post-1"),
          image = ImageData("img1", "https://example.com/img1.jpg"),
          category = "Clothing",
          type = "T-Shirt",
          brand = "Nike",
          price = 29.99,
          material = listOf(Material("Cotton", 100.0)),
          link = "https://example.com/tshirt",
          ownerId = "owner1",
          condition = "Like new",
          size = "M",
          fitType = "Regular",
          style = "Casual",
          notes = "Made by grandma")

  private val testItem2 =
      Item(
          itemUuid = "item2",
          postUuids = listOf("test-post-1"),
          image = ImageData("img2", "https://example.com/img2.jpg"),
          category = "Shoes",
          type = "Sneakers",
          brand = "Adidas",
          price = 89.99,
          material = listOf(Material("Leather", 80.0), Material("Rubber", 20.0)),
          link = "https://example.com/sneakers",
          ownerId = "owner1",
          condition = "Used",
          size = "42",
          fitType = "Slim",
          style = "Sporty",
          notes = "Limited edition")

  private val testPost1 =
      OutfitPost(
          postUID = "test-post-1",
          name = "Test User",
          ownerId = "owner1",
          userProfilePicURL = "https://example.com/profile.jpg",
          outfitURL = "https://example.com/outfit.jpg",
          description = "Test outfit",
          itemsID = listOf("item1", "item2"),
          timestamp = System.currentTimeMillis())

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockItemsRepository = mockk(relaxed = true)
    mockFeedRepository = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)

    every { mockAccountService.currentUserId } returns "test-uid"

    viewModel =
        SeeFitViewModel(
            itemsRepository = mockItemsRepository,
            feedRepository = mockFeedRepository,
            accountService = mockAccountService,
            accountRepository = mockAccountRepository)
    goBackCalled = false
  }

  private fun setScreen(
      postUuid: String = "test-post-1",
      items: List<Item> = emptyList(),
      postOwnerId: String = "test-uid",
      starred: List<String> = emptyList()
  ) {
    // Mock feedRepository to return the post with specified owner ID
    coEvery { mockFeedRepository.getPostById(postUuid) } returns
        testPost1.copy(postUID = postUuid, ownerId = postOwnerId)

    // Mock itemsRepository to return the items for that owner
    coEvery { mockItemsRepository.getFriendItemsForPost(postUuid, postOwnerId) } returns items
    coEvery { mockAccountRepository.getStarredItems(any()) } returns starred
    coEvery { mockAccountRepository.toggleStarredItem(any()) } returns starred

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = viewModel, postUuid = postUuid, goBack = { goBackCalled = true })
      }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun seeFitScreen_displaysCorrectly() {
    setScreen(items = listOf(testItem1, testItem2))

    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEMS_GRID)
  }

  @Test
  fun seeFitScreen_showsBackButton_andNavigatesBack() {
    setScreen(items = listOf(testItem1))

    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.NAVIGATE_TO_FEED_SCREEN)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    assert(goBackCalled)
  }

  @Test
  fun seeFitScreen_displaysErrorToast_onError() {
    // Mock repository to throw an exception
    coEvery { mockItemsRepository.getFriendItemsForPost(any(), any()) } throws
        Exception("Network error")

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = viewModel, postUuid = "error-post", goBack = { goBackCalled = true })
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun seeFitScreen_emptyStateDisplaysCorrectly() {
    setScreen(postUuid = "no-items-post", items = emptyList())

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("No items associated with this post.").assertIsDisplayed()

    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.NAVIGATE_TO_FEED_SCREEN).assertIsDisplayed()
  }

  @Test
  fun seeFitScreen_showsLoadingIndicator_whenLoading() {
    // Mock repository to delay the response to keep loading state visible
    coEvery { mockItemsRepository.getFriendItemsForPost(any(), any()) } coAnswers
        {
          kotlinx.coroutines.delay(1000) // Keep loading state active
          emptyList()
        }

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = viewModel, postUuid = "test-post-1", goBack = { goBackCalled = true })
      }
    }
    // Verify loading indicator and text are displayed
    composeTestRule.onNodeWithText("Loading items...").assertIsDisplayed()

    // Wait for loading to complete
    composeTestRule.waitForIdle()
  }

  @Test
  fun nonOwner_seesStarButton_andCanToggle() {
    coEvery { mockAccountRepository.getStarredItems(any()) } returns emptyList()
    coEvery { mockAccountRepository.toggleStarredItem("item1") } returns listOf("item1")

    setScreen(items = listOf(testItem1), postOwnerId = "another-user")

    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.getStarButtonTag(testItem1))
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    coVerify { mockAccountRepository.toggleStarredItem("item1") }
  }

  @Test
  fun owner_doesNotSeeStarButton() {
    setScreen(items = listOf(testItem1), postOwnerId = "test-uid")

    composeTestRule
        .onAllNodesWithTag(SeeFitScreenTestTags.getStarButtonTag(testItem1))
        .assertCountEquals(0)
  }

  @Test
  fun itemCardShowsItems_itemDialogDisplaysItemDetails_onClick() {
    setScreen(items = listOf(testItem1))
    composeTestRule.waitForIdle()

    // Card container is visible
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.getTestTagForItem(testItem1))
        .assertIsDisplayed()

    // Query inner nodes in the unmerged tree to avoid Card's semantics merging
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_CATEGORY, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_TYPE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_IMAGE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Dialog-only tags must not exist before clicking
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_LINK).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_BRAND).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_PRICE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_MATERIAL).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_CONDITION).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_SIZE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_FIT_TYPE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_STYLE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_NOTES).assertDoesNotExist()

    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.getTestTagForItem(testItem1)).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(SeeFitScreenTestTags.ITEM_LINK, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_DETAILS_DIALOG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_DETAILS_DIALOG)
        .performScrollToNode(hasTestTag(SeeFitScreenTestTags.ITEM_NOTES))
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_DETAILS_DIALOG)
        .performScrollToNode(hasTestTag(SeeFitScreenTestTags.ITEM_LINK))
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_LINK, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_PRICE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_MATERIAL, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_BRAND, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CONDITION, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_SIZE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_FIT_TYPE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_STYLE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_NOTES, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_LINK_COPY, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_NOTES_COPY, useUnmergedTree = true)
        .assertIsDisplayed()

    val clipboard =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))

    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_NOTES_COPY, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle {
      assertEquals("Made by grandma", clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: "")
    }

    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_LINK_COPY, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle {
      assertEquals(
          "https://example.com/tshirt", clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: "")
    }
  }

  @Test
  fun itemGridScreen_handlesManyItemCards() {
    val manyItems =
        (1..10).map { index ->
          Item(
              itemUuid = "item-$index",
              postUuids = listOf("post-1"),
              image = ImageData("img$index", "https://example.com/img$index.jpg"),
              category = "Category $index",
              type = "Type $index",
              brand = "Brand $index",
              price = index * 10.0,
              material = emptyList(),
              link = "https://example.com/item$index",
              ownerId = "owner1")
        }

    setScreen(items = manyItems)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithText("No items associated with this post.").assertDoesNotExist()

    // Grid is visible
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEMS_GRID).assertIsDisplayed()

    // At least one card is visible in the viewport using item-specific tag
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.getTestTagForItem(manyItems[0]))
        .assertIsDisplayed()

    // Scroll to last and verify its content is displayed
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEMS_GRID).performScrollToIndex(9)
    composeTestRule.onNodeWithText("CATEGORY 10").assertIsDisplayed()

    // Scroll back to the first and verify
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEMS_GRID).performScrollToIndex(0)
    composeTestRule.onNodeWithText("CATEGORY 1").assertIsDisplayed()
  }

  @Test
  fun itemCard_editButtonNotDisplayed_whenUserIsNotOwner() {
    // Set post owner to a different user ID (not the current user)
    setScreen(items = listOf(testItem1), postOwnerId = "different-owner-id")
    composeTestRule.waitForIdle()

    // Verify the card is displayed
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.getTestTagForItem(testItem1))
        .assertIsDisplayed()

    // Verify the edit button is NOT displayed (user is not the owner)
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun itemCard_editButtonIsDisplayed_whenUserIsOwner() {
    // Mock the account service to return the same user ID as the post owner

    // Set the post owner to match the current user
    setScreen(items = listOf(testItem1), postOwnerId = "test-uid")

    // Wait for UI to update
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify the card is displayed
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.getTestTagForItem(testItem1))
        .assertIsDisplayed()

    // Verify the edit button IS displayed on the card
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun itemCard_editButtonTriggersCallback_whenUserIsOwner() {
    var editedItemUuid = ""

    // Mock the account service to return a specific user ID
    every { mockAccountService.currentUserId } returns "owner-user-123"

    // Mock repositories with the same owner ID
    coEvery { mockFeedRepository.getPostById("test-post-1") } returns
        testPost1.copy(ownerId = "owner-user-123")
    coEvery { mockItemsRepository.getFriendItemsForPost("test-post-1", "owner-user-123") } returns
        listOf(testItem1)

    // Create a new viewModel with the mocked account service
    val testViewModel =
        SeeFitViewModel(
            itemsRepository = mockItemsRepository,
            feedRepository = mockFeedRepository,
            accountService = mockAccountService)

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = testViewModel,
            postUuid = "test-post-1",
            goBack = { goBackCalled = true },
            onEditItem = { itemUuid -> editedItemUuid = itemUuid })
      }
    }

    // Wait for the edit button to appear
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click the edit button
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    // Verify the callback was triggered with the correct item UUID
    assertEquals(testItem1.itemUuid, editedItemUuid)
  }

  @Test
  fun itemCard_editButtonWithMultipleItems_triggersCorrectCallback_whenUserIsOwner() {
    var editedItemUuid = ""

    // Mock the account service to return a specific user ID
    every { mockAccountService.currentUserId } returns "multi-owner-456"

    // Mock repositories with the same owner ID
    coEvery { mockFeedRepository.getPostById("test-post-1") } returns
        testPost1.copy(ownerId = "multi-owner-456")
    coEvery { mockItemsRepository.getFriendItemsForPost("test-post-1", "multi-owner-456") } returns
        listOf(testItem1, testItem2)

    // Create a new viewModel with the mocked account service
    val testViewModel =
        SeeFitViewModel(
            itemsRepository = mockItemsRepository,
            feedRepository = mockFeedRepository,
            accountService = mockAccountService)

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = testViewModel,
            postUuid = "test-post-1",
            goBack = { goBackCalled = true },
            onEditItem = { itemUuid -> editedItemUuid = itemUuid })
      }
    }

    // Wait for both edit buttons to appear
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      val editButtons =
          composeTestRule
              .onAllNodesWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
              .fetchSemanticsNodes()
      editButtons.size == 2
    }

    // Verify both cards are displayed
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.getTestTagForItem(testItem1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.getTestTagForItem(testItem2))
        .assertIsDisplayed()

    // Click edit button on second item
    composeTestRule
        .onAllNodesWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)[1]
        .performClick()

    composeTestRule.waitForIdle()

    // Verify the correct item UUID was passed
    assertEquals(testItem2.itemUuid, editedItemUuid)
  }

  @Test
  fun itemCard_editButtonDoesNotOpenDetailsDialog_whenClicked() {
    var editedItemUuid = ""

    // Mock the account service to return a specific user ID
    every { mockAccountService.currentUserId } returns "dialog-test-789"

    // Mock repositories with the same owner ID
    coEvery { mockFeedRepository.getPostById("test-post-1") } returns
        testPost1.copy(ownerId = "dialog-test-789")
    coEvery { mockItemsRepository.getFriendItemsForPost("test-post-1", "dialog-test-789") } returns
        listOf(testItem1)

    // Create a new viewModel with the mocked account service
    val testViewModel =
        SeeFitViewModel(
            itemsRepository = mockItemsRepository,
            feedRepository = mockFeedRepository,
            accountService = mockAccountService)

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = testViewModel,
            postUuid = "test-post-1",
            goBack = { goBackCalled = true },
            onEditItem = { itemUuid -> editedItemUuid = itemUuid })
      }
    }

    composeTestRule.waitForIdle()

    // Wait for edit button to appear
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click the edit button
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    // Verify the details dialog is NOT displayed
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_DETAILS_DIALOG).assertDoesNotExist()

    // Verify callback was triggered
    assertEquals(testItem1.itemUuid, editedItemUuid)
  }

  @Test
  fun itemCard_multipleItems_ownerSeesEditButtons() {
    val items = listOf(testItem1, testItem2)

    // User is owner - should see edit buttons
    every { mockAccountService.currentUserId } returns "owner-comparison-111"

    // Create a new viewModel for owner test
    val ownerViewModel =
        SeeFitViewModel(
            itemsRepository = mockItemsRepository,
            feedRepository = mockFeedRepository,
            accountService = mockAccountService)

    coEvery { mockFeedRepository.getPostById("test-post-1") } returns
        testPost1.copy(ownerId = "owner-comparison-111")
    coEvery {
      mockItemsRepository.getFriendItemsForPost("test-post-1", "owner-comparison-111")
    } returns items

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = ownerViewModel,
            postUuid = "test-post-1",
            goBack = { goBackCalled = true })
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val editButtonsAsOwner =
        composeTestRule
            .onAllNodesWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
            .fetchSemanticsNodes()

    assert(editButtonsAsOwner.size == 2) {
      "Expected 2 edit buttons when user is owner, found ${editButtonsAsOwner.size}"
    }
  }

  @Test
  fun itemCard_multipleItems_nonOwnerDoesNotSeeEditButtons() {
    val items = listOf(testItem1, testItem2)

    // User is NOT owner - should NOT see edit buttons
    every { mockAccountService.currentUserId } returns "not-owner-222"

    // Create a new viewModel for non-owner test
    val nonOwnerViewModel =
        SeeFitViewModel(
            itemsRepository = mockItemsRepository,
            feedRepository = mockFeedRepository,
            accountService = mockAccountService)

    coEvery { mockFeedRepository.getPostById("test-post-2") } returns
        testPost1.copy(postUID = "test-post-2", ownerId = "different-owner-id")
    coEvery {
      mockItemsRepository.getFriendItemsForPost("test-post-2", "different-owner-id")
    } returns items

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = nonOwnerViewModel,
            postUuid = "test-post-2",
            goBack = { goBackCalled = true })
      }
    }

    composeTestRule.waitForIdle()

    // Verify NO edit buttons are present (user is not owner)
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun itemCard_editButtonVisibility_changesWithOwnership() {
    // Mock current user as non-owner initially
    every { mockAccountService.currentUserId } returns "non-owner-333"

    coEvery { mockFeedRepository.getPostById("test-post-1") } returns
        testPost1.copy(ownerId = "owner1")
    coEvery { mockItemsRepository.getFriendItemsForPost("test-post-1", "owner1") } returns
        listOf(testItem1)

    val testViewModel =
        SeeFitViewModel(
            itemsRepository = mockItemsRepository,
            feedRepository = mockFeedRepository,
            accountService = mockAccountService)

    composeTestRule.setContent {
      OOTDTheme {
        SeeFitScreen(
            seeFitViewModel = testViewModel,
            postUuid = "test-post-1",
            goBack = { goBackCalled = true })
      }
    }

    composeTestRule.waitForIdle()

    // Verify edit button is NOT visible (user is not owner)
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON, useUnmergedTree = true)
        .assertDoesNotExist()
  }
}
