package com.android.ootd.ui.post

import android.net.Uri
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.Material
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

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
            postUuids = listOf(postUuid),
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

    createTestItem(postUuid)
    createTestItem(postUuid)

    val before = itemsRepository.getAssociatedItems(postUuid)
    assertEquals(2, before.size)

    viewModel.deleteItemsForPost(postUuid)

    delay(1000)

    val after = itemsRepository.getAssociatedItems(postUuid)
    assertTrue("Expected items to be deleted, but found ${after.size}", after.isEmpty())
  }

  @Test
  fun deleteItemsForPost_withEmptyUuid_doesNotCrash() = runBlocking {
    viewModel.deleteItemsForPost("")
    delay(300)

    assertTrue(true)
  }

  @Test
  fun deleteItemsForPost_whenRepositoryThrows_logsErrorButDoesNotCrash() = runBlocking {
    val throwingRepo =
        object : com.android.ootd.model.items.ItemsRepository by itemsRepository {
          override suspend fun deletePostItems(postUuid: String) {
            throw Exception("Simulated deletion failure")
          }
        }

    val failingVM = FitCheckViewModel(repository = throwingRepo)

    failingVM.deleteItemsForPost("post_error")
    delay(500)

    assertTrue(true)
  }

  @Test
  fun setPhoto_withValidUri_updatesUiState() = runBlocking {
    val uri = Uri.parse("content://test/photo.jpg")

    viewModel.setPhoto(uri)

    val state = viewModel.uiState.value
    assertEquals(uri, state.image)
    assertNull(state.errorMessage)
  }

  @Test
  fun setPhoto_withEmptyUri_setsErrorMessage() = runBlocking {
    viewModel.setPhoto(Uri.EMPTY)

    val state = viewModel.uiState.value
    assertEquals(Uri.EMPTY, state.image)
    assertEquals("Please select a photo before continuing.", state.errorMessage)
  }

  @Test
  fun setDescription_updatesDescriptionField() = runBlocking {
    val desc = "OOTD Look Description"

    viewModel.setDescription(desc)

    val state = viewModel.uiState.value
    assertEquals(desc, state.description)
  }

  @Test
  fun setLocation_updatesLocationField() = runBlocking {
    val location = Location(47.3769, 8.5417, "Zürich, Switzerland")

    viewModel.setLocation(location)

    val state = viewModel.uiState.value
    assertEquals(location, state.location)
    assertEquals("Zürich, Switzerland", state.location.name)
  }

  @Test
  fun setLocation_withEmptyLocation_works() = runBlocking {
    viewModel.setLocation(emptyLocation)

    val state = viewModel.uiState.value
    assertEquals(emptyLocation, state.location)
  }

  @Test
  fun setLocation_replacesExistingLocation() = runBlocking {
    val location1 = Location(47.3769, 8.5417, "Zürich, Switzerland")
    val location2 = Location(46.2044, 6.1432, "Lausanne, Switzerland")

    viewModel.setLocation(location1)
    assertEquals(location1, viewModel.uiState.value.location)

    viewModel.setLocation(location2)
    assertEquals(location2, viewModel.uiState.value.location)
  }

  @Test
  fun setLocation_doesNotAffectOtherFields() = runBlocking {
    val uri = Uri.parse("content://test/photo.jpg")
    val description = "Test description"
    val location = Location(47.3769, 8.5417, "Zürich, Switzerland")

    viewModel.setPhoto(uri)
    viewModel.setDescription(description)
    viewModel.setLocation(location)

    val state = viewModel.uiState.value
    assertEquals(uri, state.image)
    assertEquals(description, state.description)
    assertEquals(location, state.location)
  }

  @Test
  fun fullState_photoDescriptionAndLocation_allPersist() = runBlocking {
    val uri = Uri.parse("content://test/photo.jpg")
    val description = "Winter outfit in Zürich"
    val location = Location(47.3769, 8.5417, "Zürich, Switzerland")

    viewModel.setPhoto(uri)
    viewModel.setDescription(description)
    viewModel.setLocation(location)

    val state = viewModel.uiState.value
    assertEquals(uri, state.image)
    assertEquals(description, state.description)
    assertEquals(location, state.location)
    assertNull(state.errorMessage)
  }
}
