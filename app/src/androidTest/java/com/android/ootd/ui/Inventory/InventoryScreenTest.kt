package com.android.ootd.ui.Inventory

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.theme.OOTDTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockItemsRepository: ItemsRepository
  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var viewModel: InventoryViewModel
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

  private val testUserId = "testUser123"
  private val testItems =
      listOf(
          Item(
              itemUuid = "item1",
              postUuids = listOf("post1"),
              image = ImageData("img1", "https://example.com/img1.jpg"),
              category = "Clothing",
              type = "T-Shirt",
              brand = "Nike",
              price = 29.99,
              material = emptyList(),
              link = null,
              ownerId = testUserId),
          Item(
              itemUuid = "item2",
              postUuids = listOf("post2"),
              image = ImageData("img2", "https://example.com/img2.jpg"),
              category = "Shoes",
              type = "Sneakers",
              brand = "Adidas",
              price = 79.99,
              material = emptyList(),
              link = null,
              ownerId = testUserId))

  private fun createInventoryViewModel(): InventoryViewModel =
      InventoryViewModel(mockAccountRepository, mockItemsRepository)

  @Before
  fun setup() {
    mockAccountRepository = mockk(relaxed = true)
    mockItemsRepository = mockk(relaxed = true)
    mockNavigationActions = mockk(relaxed = true)

    // Mock Firebase Auth
    mockkStatic(FirebaseAuth::class)
    mockAuth = mockk()
    mockUser = mockk()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun topBarIsDisplayed() {
    coEvery { mockAccountRepository.getItemsList(any()) } returns emptyList()
    coEvery { mockItemsRepository.getItemsByIds(any()) } returns emptyList()

    val testViewModel = createInventoryViewModel()
    viewModel = testViewModel
    composeTestRule.setContent {
      OOTDTheme {
        InventoryScreen(
            inventoryViewModel = testViewModel, navigationActions = mockNavigationActions)
      }
    }

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.TITLE).assertTextEquals("INVENTORY")
  }

  @Test
  fun showsLoadingIndicatorWhileLoading() {
    val uiStateFlow = MutableStateFlow(InventoryUIState(isLoading = true))
    viewModel = mockk(relaxed = true)
    every { viewModel.uiState } returns uiStateFlow

    composeTestRule.setContent {
      OOTDTheme {
        InventoryScreen(inventoryViewModel = viewModel, navigationActions = mockNavigationActions)
      }
    }

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun showsEmptyStateWhenNoItems() {
    coEvery { mockAccountRepository.getItemsList(any()) } returns emptyList()
    coEvery { mockItemsRepository.getItemsByIds(any()) } returns emptyList()

    val testViewModel = createInventoryViewModel()
    viewModel = testViewModel
    composeTestRule.setContent {
      OOTDTheme {
        InventoryScreen(
            inventoryViewModel = testViewModel, navigationActions = mockNavigationActions)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(InventoryScreenTestTags.EMPTY_STATE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.EMPTY_STATE).assertIsDisplayed()
  }

  @Test
  fun displaysItemsInGrid() {
    coEvery { mockAccountRepository.getItemsList(any()) } returns listOf("item1", "item2")
    coEvery { mockItemsRepository.getItemsByIds(any()) } returns testItems

    val testViewModel = createInventoryViewModel()
    viewModel = testViewModel
    composeTestRule.setContent {
      OOTDTheme {
        InventoryScreen(
            inventoryViewModel = testViewModel, navigationActions = mockNavigationActions)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(InventoryScreenTestTags.ITEMS_GRID)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.ITEMS_GRID).assertIsDisplayed()
    composeTestRule.onNodeWithTag("${InventoryScreenTestTags.ITEM_CARD}_item1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("${InventoryScreenTestTags.ITEM_CARD}_item2").assertIsDisplayed()
  }

  @Test
  fun itemCardClickNavigatesToEditScreen() {
    coEvery { mockAccountRepository.getItemsList(any()) } returns listOf("item1")
    coEvery { mockItemsRepository.getItemsByIds(any()) } returns listOf(testItems[0])

    val testViewModel = createInventoryViewModel()
    viewModel = testViewModel
    composeTestRule.setContent {
      OOTDTheme {
        InventoryScreen(
            inventoryViewModel = testViewModel, navigationActions = mockNavigationActions)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag("${InventoryScreenTestTags.ITEM_CARD}_item1")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag("${InventoryScreenTestTags.ITEM_CARD}_item1").performClick()
    composeTestRule.waitForIdle()

    verify(timeout = 2_000) { mockNavigationActions.navigateTo(Screen.EditItem("item1")) }
  }

  @Test
  fun handlesNullNavigationActions() {
    coEvery { mockAccountRepository.getItemsList(any()) } returns emptyList()
    coEvery { mockItemsRepository.getItemsByIds(any()) } returns emptyList()

    val testViewModel = createInventoryViewModel()
    viewModel = testViewModel
    composeTestRule.setContent {
      OOTDTheme { InventoryScreen(inventoryViewModel = testViewModel, navigationActions = null) }
    }

    // Should not crash when navigationActions is null
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun searchFabIsDisplayed() {
    coEvery { mockAccountRepository.getItemsList(any()) } returns emptyList()
    coEvery { mockItemsRepository.getItemsByIds(any()) } returns emptyList()

    val testViewModel = createInventoryViewModel()
    viewModel = testViewModel
    composeTestRule.setContent {
      OOTDTheme {
        InventoryScreen(
            inventoryViewModel = testViewModel, navigationActions = mockNavigationActions)
      }
    }

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_FAB).assertIsDisplayed()
  }

  @Test
  fun searchBarAppearsWhenSearchFabClicked() {
    coEvery { mockAccountRepository.getItemsList(any()) } returns emptyList()
    coEvery { mockItemsRepository.getItemsByIds(any()) } returns emptyList()

    val testViewModel = createInventoryViewModel()
    viewModel = testViewModel
    composeTestRule.setContent {
      OOTDTheme {
        InventoryScreen(
            inventoryViewModel = testViewModel, navigationActions = mockNavigationActions)
      }
    }

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_FAB).performClick()
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_FIELD).assertIsDisplayed()
  }

  @Test
  fun searchBarDisappearsWhenSearchFabClickedAgain() {
    coEvery { mockAccountRepository.getItemsList(any()) } returns emptyList()
    coEvery { mockItemsRepository.getItemsByIds(any()) } returns emptyList()

    val testViewModel = createInventoryViewModel()
    viewModel = testViewModel
    composeTestRule.setContent {
      OOTDTheme {
        InventoryScreen(
            inventoryViewModel = testViewModel, navigationActions = mockNavigationActions)
      }
    }

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_FAB).performClick()
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_BAR).assertIsDisplayed()

    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_FAB).performClick()
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_BAR).assertDoesNotExist()
  }
}
