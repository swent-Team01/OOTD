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
  fun `loadInventory can be called manually`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    coEvery { itemsRepository.getItemsByIds(testItemIds) } returns testItems
    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    coEvery { accountRepository.getItemsList(testUserId) } returns emptyList()
    coEvery { itemsRepository.getItemsByIds(emptyList()) } returns emptyList()
    viewModel.loadInventory()
    advanceUntilIdle()

    Assert.assertTrue(viewModel.uiState.value.items.isEmpty())
    coVerify(exactly = 2) { accountRepository.getItemsList(testUserId) }
  }

  @Test
  fun `user not logged in throws exception`() = runTest {
    every { mockAuth.currentUser } returns null

    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    Assert.assertTrue(viewModel.uiState.value.errorMessage?.contains("not logged in") == true)
  }

  @Test
  fun `loadInventory uses empty list when getItemsList times out`() = runTest {
    // Simulate timeout by making getItemsList suspend indefinitely
    coEvery { accountRepository.getItemsList(testUserId) } coAnswers
        {
          kotlinx.coroutines.delay(5000) // Longer than the 2s timeout
          testItemIds
        }
    coEvery { itemsRepository.getItemsByIds(emptyList()) } returns emptyList()

    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    Assert.assertFalse(viewModel.uiState.value.isLoading)
    Assert.assertTrue(viewModel.uiState.value.items.isEmpty())
    Assert.assertNull(viewModel.uiState.value.errorMessage)
    coVerify { accountRepository.getItemsList(testUserId) }
    coVerify { itemsRepository.getItemsByIds(emptyList()) }
  }

  @Test
  fun `loadInventory uses empty list when getItemsByIds times out`() = runTest {
    coEvery { accountRepository.getItemsList(testUserId) } returns testItemIds
    // Simulate timeout by making getItemsByIds suspend indefinitely
    coEvery { itemsRepository.getItemsByIds(testItemIds) } coAnswers
        {
          kotlinx.coroutines.delay(5000) // Longer than the 2s timeout
          testItems
        }

    viewModel = InventoryViewModel(accountRepository, itemsRepository)
    advanceUntilIdle()

    Assert.assertFalse(viewModel.uiState.value.isLoading)
    Assert.assertTrue(viewModel.uiState.value.items.isEmpty())
    Assert.assertNull(viewModel.uiState.value.errorMessage)
    coVerify { accountRepository.getItemsList(testUserId) }
    coVerify { itemsRepository.getItemsByIds(testItemIds) }
  }
}
