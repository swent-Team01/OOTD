package com.android.ootd.ui.Inventory

import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

  private lateinit var accountRepository: AccountRepository
  private lateinit var itemsRepository: ItemsRepository
  private lateinit var viewModel: InventoryViewModel
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private val testDispatcher = StandardTestDispatcher()

  private val testUserId = "testUser123"
  private val testItemIds = listOf("item1", "item2", "item3")
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
              postUuids = listOf("post1"),
              image = ImageData("img2", "https://example.com/img2.jpg"),
              category = "Shoes",
              type = "Sneakers",
              brand = "Adidas",
              price = 79.99,
              material = emptyList(),
              link = null,
              ownerId = testUserId),
          Item(
              itemUuid = "item3",
              postUuids = listOf("post2"),
              image = ImageData("img3", "https://example.com/img3.jpg"),
              category = "Accessories",
              type = "Hat",
              brand = "Supreme",
              price = 49.99,
              material = emptyList(),
              link = null,
              ownerId = testUserId))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    accountRepository = mockk()
    itemsRepository = mockk()

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
    Dispatchers.resetMain()
    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun `init loads inventory successfully`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems

    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    Assert.assertFalse(viewModel.uiState.value.isLoading)
    Assert.assertEquals(testItems, viewModel.uiState.value.items)
    Assert.assertNull(viewModel.uiState.value.errorMessage)
    coVerify { accountRepository.getItemsList(testUserId) }
    coVerify { itemsRepository.getItemsByIds(testItemIds) }
  }

  @Test
  fun `empty inventory shows empty list`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns emptyList()
    coEvery { itemsRepository.getItemsByIds(emptyList()) } returns emptyList()

    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    Assert.assertFalse(viewModel.uiState.value.isLoading)
    Assert.assertTrue(viewModel.uiState.value.items.isEmpty())
    Assert.assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `handles error from accountRepository`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } throws Exception("Account error")

    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    Assert.assertFalse(viewModel.uiState.value.isLoading)
    Assert.assertTrue(viewModel.uiState.value.items.isEmpty())
    Assert.assertTrue(viewModel.uiState.value.errorMessage?.contains("Account error") == true)
  }

  @Test
  fun `handles error from itemsRepository`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } throws Exception("Items error")

    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    Assert.assertFalse(viewModel.uiState.value.isLoading)
    Assert.assertTrue(viewModel.uiState.value.items.isEmpty())
    Assert.assertTrue(viewModel.uiState.value.errorMessage?.contains("Items error") == true)
  }

  @Test
  fun `clearError clears error message`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } throws Exception("Test error")
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    Assert.assertTrue(viewModel.uiState.value.errorMessage != null)

    viewModel.clearError()

    Assert.assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `user not logged in throws exception`() = runTest {
    every { mockAuth.currentUser } returns null

    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    Assert.assertTrue(viewModel.uiState.value.errorMessage?.contains("not logged in") == true)
  }

  @Test
  fun `toggleSearch activates search mode`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    Assert.assertFalse(viewModel.uiState.value.isSearchActive)

    viewModel.toggleSearch()

    Assert.assertTrue(viewModel.uiState.value.isSearchActive)
  }

  @Test
  fun `toggleSearch deactivates search mode and clears query`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    viewModel.toggleSearch()
    viewModel.updateSearchQuery("Nike")

    viewModel.toggleSearch()

    Assert.assertFalse(viewModel.uiState.value.isSearchActive)
    Assert.assertEquals("", viewModel.uiState.value.searchQuery)
    Assert.assertEquals(testItems.size, viewModel.uiState.value.items.size)
  }

  @Test
  fun `updateSearchQuery filters by brand`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    viewModel.toggleSearch()

    viewModel.updateSearchQuery("Nike")

    Assert.assertEquals("Nike", viewModel.uiState.value.searchQuery)
    Assert.assertEquals(1, viewModel.uiState.value.items.size)
    Assert.assertEquals("Nike", viewModel.uiState.value.items[0].brand)
  }

  @Test
  fun `updateSearchQuery filters by type`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    viewModel.toggleSearch()

    viewModel.updateSearchQuery("Sneakers")

    Assert.assertEquals(1, viewModel.uiState.value.items.size)
    Assert.assertEquals("Sneakers", viewModel.uiState.value.items[0].type)
  }

  @Test
  fun `updateSearchQuery filters by category`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    viewModel.toggleSearch()

    viewModel.updateSearchQuery("Clothing")

    Assert.assertEquals(1, viewModel.uiState.value.items.size)
    Assert.assertEquals("Clothing", viewModel.uiState.value.items[0].category)
  }

  @Test
  fun `updateSearchQuery is case insensitive`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    viewModel.toggleSearch()

    viewModel.updateSearchQuery("nike")

    Assert.assertEquals(1, viewModel.uiState.value.items.size)
    Assert.assertEquals("Nike", viewModel.uiState.value.items[0].brand)
  }

  @Test
  fun `updateSearchQuery with empty string shows all items`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    viewModel.toggleSearch()
    viewModel.updateSearchQuery("Nike")

    viewModel.updateSearchQuery("")

    Assert.assertEquals(testItems.size, viewModel.uiState.value.items.size)
  }

  @Test
  fun `updateSearchQuery with no matches returns empty list`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()
    viewModel.toggleSearch()

    viewModel.updateSearchQuery("NonExistentBrand")

    Assert.assertTrue(viewModel.uiState.value.items.isEmpty())
  }
}
