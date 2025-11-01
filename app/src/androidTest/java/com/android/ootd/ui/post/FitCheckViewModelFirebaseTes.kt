package com.android.ootd.ui.post

import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.Material
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Integration test for [FitCheckViewModel] using the Firebase emulator. Verifies that
 * deleteItemsForPost() correctly removes associated items from Firestore.
 */
class FitCheckViewModelFirebaseTest : FirestoreTest() {

  private lateinit var viewModel: FitCheckViewModel

  @Before
  override fun setUp() {
    super.setUp()
    viewModel = FitCheckViewModel(repository = itemsRepository)
  }

  private suspend fun createTestItem(postUuid: String): Item {
    val uid = FirebaseEmulator.auth.currentUser?.uid ?: "anon"
    val item =
        Item(
            itemUuid = itemsRepository.getNewItemId(),
            postUuid = postUuid,
            image = ImageData("test_img", "https://example.com/test.jpg"),
            category = "Clothing",
            type = "T-Shirt",
            brand = "TestBrand",
            price = 19.99,
            material = listOf(Material("Cotton", 100.0)),
            link = "https://example.com/item",
            ownerId = uid)
    itemsRepository.addItem(item)
    return item
  }

  @Test
  fun deleteItemsForPost_removesAllItemsFromFirestore() = runBlocking {
    val postUuid = "post_to_delete"

    // Create multiple test items associated with the same post
    createTestItem(postUuid)
    createTestItem(postUuid)

    // Confirm they were saved
    val before = itemsRepository.getAssociatedItems(postUuid)
    assertEquals(2, before.size)

    // Perform deletion via the ViewModel
    viewModel.deleteItemsForPost(postUuid)

    // Allow async operations to finish
    delay(1000)

    // Verify that the items are gone
    val after = itemsRepository.getAssociatedItems(postUuid)
    assertTrue("Expected items to be deleted, but found ${after.size}", after.isEmpty())
  }

  @Test
  fun deleteItemsForPost_withEmptyUuid_doesNotCrash() = runBlocking {
    // Should safely return without throwing
    viewModel.deleteItemsForPost("")
    delay(300)

    assertTrue(true) // passes if no exception
  }

  @Test
  fun deleteItemsForPost_whenRepositoryThrows_logsErrorButDoesNotCrash() = runBlocking {
    // Create a fake throwing repository
    val throwingRepo =
        object : com.android.ootd.model.items.ItemsRepository by itemsRepository {
          override suspend fun deletePostItems(postUuid: String) {
            throw Exception("Simulated deletion failure")
          }
        }

    val failingVM = FitCheckViewModel(repository = throwingRepo)

    // Should not crash or throw to test
    failingVM.deleteItemsForPost("post_error")
    delay(500)

    assertTrue(true)
  }
}
