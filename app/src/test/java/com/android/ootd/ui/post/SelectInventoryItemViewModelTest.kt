package com.android.ootd.ui.post

import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelectInventoryItemViewModelTest {

  private lateinit var mockAccountRepo: AccountRepository
  private lateinit var mockItemsRepo: ItemsRepository
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var viewModel: SelectInventoryItemViewModel
  private val testDispatcher = StandardTestDispatcher()

  private fun createItem(id: String, postUuids: List<String> = emptyList()) =
      Item(
          itemUuid = id,
          postUuids = postUuids,
          image = ImageData("img$id", "url$id"),
          category = "Clothing",
          type = "Shirt",
          brand = "Brand",
          price = 10.0,
          material = emptyList(),
          link = null,
          ownerId = "user123")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    mockAccountRepo = mockk()
    mockItemsRepo = mockk()
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    // Mock Firebase auth
    mockkStatic(FirebaseAuth::class)
    mockkStatic("com.google.firebase.auth.ktx.AuthKt")
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    every { mockFirebaseUser.uid } returns "user123"

    viewModel = SelectInventoryItemViewModel(mockAccountRepo, mockItemsRepo)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `initPostUuid edge cases - empty and duplicate calls`() = runTest {
    // Test empty postUuid - should reset state without loading
    viewModel.initPostUuid("")
    advanceUntilIdle()

    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(emptyList<Item>(), viewModel.uiState.value.availableItems)
    assertNull(viewModel.uiState.value.errorMessage)
    coVerify(exactly = 0) { mockAccountRepo.getItemsList(any()) }

    // Test blank postUuid - should reset state without loading
    viewModel.initPostUuid("   ")
    advanceUntilIdle()

    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(emptyList<Item>(), viewModel.uiState.value.availableItems)
    assertNull(viewModel.uiState.value.errorMessage)
    coVerify(exactly = 0) { mockAccountRepo.getItemsList(any()) }

    // Test duplicate calls with same postUuid
    val items = listOf(createItem("1"), createItem("2"))
    coEvery { mockAccountRepo.getItemsList(any()) } returns listOf("1", "2")
    coEvery { mockItemsRepo.getItemsByIds(any()) } returns items
    coEvery { mockItemsRepo.getAssociatedItems(any()) } returns emptyList()

    viewModel.initPostUuid("post1")
    advanceUntilIdle()
    viewModel.initPostUuid("post1")
    advanceUntilIdle()

    // Should only load once
    coVerify(exactly = 1) { mockAccountRepo.getItemsList(any()) }
  }

  @Test
  fun `loadAvailableItems success - filters items and updates state`() = runTest {
    // Test filtering items already in post
    val item1 = createItem("1")
    val item2 = createItem("2", listOf("post1"))
    val item3 = createItem("3")

    coEvery { mockAccountRepo.getItemsList(any()) } returns listOf("1", "2", "3")
    coEvery { mockItemsRepo.getItemsByIds(any()) } returns listOf(item1, item2, item3)
    coEvery { mockItemsRepo.getAssociatedItems("post1") } returns listOf(item2)

    viewModel.initPostUuid("post1")
    advanceUntilIdle()

    // Then - item2 should be filtered out, loading should be false
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(2, viewModel.uiState.value.availableItems.size)
    assertTrue(viewModel.uiState.value.availableItems.none { it.itemUuid == "2" })
  }

  @Test
  fun `loadAvailableItems error handling - not logged in and repository exception`() = runTest {
    // Test user not logged in
    every { mockFirebaseAuth.currentUser } returns null
    viewModel.initPostUuid("post1")
    advanceUntilIdle()

    assertEquals(false, viewModel.uiState.value.isLoading)
    assertNotNull(viewModel.uiState.value.errorMessage)
    assertTrue(viewModel.uiState.value.errorMessage?.contains("Failed to load inventory") ?: false)

    // Reset and test repository exception
    setUp()
    coEvery { mockAccountRepo.getItemsList(any()) } throws Exception("Network error")
    viewModel.initPostUuid("post1")
    advanceUntilIdle()

    assertEquals(false, viewModel.uiState.value.isLoading)
    assertNotNull(viewModel.uiState.value.errorMessage)
    assertTrue(viewModel.uiState.value.errorMessage?.contains("Network error") ?: false)
  }

  @Test
  fun `addItemToPost scenarios - empty postUuid, success, and error handling`() = runTest {
    val item = createItem("1")

    // Test empty postUuid error
    viewModel.addItemToPost(item)
    advanceUntilIdle()
    assertEquals("Invalid post ID", viewModel.uiState.value.errorMessage)

    // Test successful add
    coEvery { mockAccountRepo.getItemsList(any()) } returns listOf("1")
    coEvery { mockItemsRepo.getItemsByIds(any()) } returns listOf(item)
    coEvery { mockItemsRepo.getAssociatedItems(any()) } returns emptyList()
    coEvery { mockItemsRepo.editItem(any(), any()) } just Runs

    viewModel.initPostUuid("post1")
    advanceUntilIdle()
    viewModel.addItemToPost(item)
    advanceUntilIdle()

    coVerify { mockItemsRepo.editItem("1", match { it.postUuids.contains("post1") }) }
    assertEquals("Item added to outfit successfully!", viewModel.uiState.value.successMessage)
    assertEquals(false, viewModel.uiState.value.isLoading)

    // Test exception handling
    setUp()
    coEvery { mockAccountRepo.getItemsList(any()) } returns listOf("1")
    coEvery { mockItemsRepo.getItemsByIds(any()) } returns listOf(item)
    coEvery { mockItemsRepo.getAssociatedItems(any()) } returns emptyList()
    coEvery { mockItemsRepo.editItem(any(), any()) } throws Exception("Database error")

    viewModel.initPostUuid("post1")
    advanceUntilIdle()
    viewModel.addItemToPost(item)
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMessage)
    assertTrue(viewModel.uiState.value.errorMessage?.contains("Failed to add item") ?: false)
    assertTrue(viewModel.uiState.value.errorMessage?.contains("Database error") ?: false)
  }

  @Test
  fun `clearMessages and initial state`() = runTest {
    // Test initial state
    assertEquals("", viewModel.uiState.value.postUuid)
    assertEquals(emptyList<Item>(), viewModel.uiState.value.availableItems)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMessage)
    assertNull(viewModel.uiState.value.successMessage)

    // Test clearMessages functionality
    val item = createItem("1")
    coEvery { mockAccountRepo.getItemsList(any()) } returns listOf("1")
    coEvery { mockItemsRepo.getItemsByIds(any()) } returns listOf(item)
    coEvery { mockItemsRepo.getAssociatedItems(any()) } returns emptyList()
    coEvery { mockItemsRepo.editItem(any(), any()) } just Runs

    viewModel.initPostUuid("post1")
    advanceUntilIdle()
    viewModel.addItemToPost(item)
    advanceUntilIdle()

    assertEquals("Item added to outfit successfully!", viewModel.uiState.value.successMessage)

    viewModel.clearMessages()

    assertNull(viewModel.uiState.value.errorMessage)
    assertNull(viewModel.uiState.value.successMessage)
  }

  @Test
  fun `toggleSearch toggles state and clears query`() {
    assertFalse(viewModel.uiState.value.isSearchActive)
    viewModel.toggleSearch()
    assertTrue(viewModel.uiState.value.isSearchActive)

    viewModel.updateSearchQuery("test")
    assertEquals("test", viewModel.uiState.value.searchQuery)

    viewModel.toggleSearch()
    assertFalse(viewModel.uiState.value.isSearchActive)
    assertEquals("", viewModel.uiState.value.searchQuery)
  }

  @Test
  fun `updateSearchQuery filters items correctly`() = runTest {
    val item1 = createItem("1").copy(category = "Tops", brand = "Nike", type = "T-Shirt")
    val item2 = createItem("2").copy(category = "Bottoms", brand = "Adidas", type = "Pants")
    val item3 = createItem("3").copy(category = "Shoes", brand = "Puma", type = "Sneakers")

    coEvery { mockAccountRepo.getItemsList(any()) } returns listOf("1", "2", "3")
    coEvery { mockItemsRepo.getItemsByIds(any()) } returns listOf(item1, item2, item3)
    coEvery { mockItemsRepo.getAssociatedItems(any()) } returns emptyList()

    viewModel.initPostUuid("post1")
    advanceUntilIdle()

    viewModel.updateSearchQuery("")
    assertEquals(3, viewModel.uiState.value.filteredItems.size)

    viewModel.updateSearchQuery("Tops")
    assertEquals(1, viewModel.uiState.value.filteredItems.size)
    assertEquals("1", viewModel.uiState.value.filteredItems[0].itemUuid)

    viewModel.updateSearchQuery("adidas")
    assertEquals(1, viewModel.uiState.value.filteredItems.size)
    assertEquals("2", viewModel.uiState.value.filteredItems[0].itemUuid)

    viewModel.updateSearchQuery("Sneakers")
    assertEquals(1, viewModel.uiState.value.filteredItems.size)
    assertEquals("3", viewModel.uiState.value.filteredItems[0].itemUuid)

    viewModel.updateSearchQuery("NonExistent")
    assertEquals(0, viewModel.uiState.value.filteredItems.size)
  }
}
