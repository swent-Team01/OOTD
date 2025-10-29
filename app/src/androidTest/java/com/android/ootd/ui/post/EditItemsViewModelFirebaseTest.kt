package com.android.ootd.ui.post

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepositoryFirestore
import com.android.ootd.model.items.Material
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import java.io.File
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before
import org.junit.Test

class EditItemsViewModelFirebaseTest : FirestoreTest() {

  private lateinit var viewModel: EditItemsViewModel
  private lateinit var repository: ItemsRepositoryFirestore

  var ownerId = ""

  @Before
  override fun setUp() {
    super.setUp()
    repository = ItemsRepositoryFirestore(FirebaseEmulator.firestore)
    viewModel = EditItemsViewModel(repository)
    Assume.assumeTrue("Firebase Emulator must be running before tests.", FirebaseEmulator.isRunning)
    ownerId = FirebaseEmulator.auth.uid ?: ""
    if (ownerId == "") {
      throw IllegalStateException("There needs to be an authenticated user")
    }
  }

  private fun createTempImageFile(): File {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = File(context.cacheDir, "test_edit_${System.currentTimeMillis()}.jpg")
    // Write a small JPEG-like header + dummy data
    file.writeBytes(
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) +
            ByteArray(1024) { 0xAB.toByte() })
    return file
  }

  private fun createReadableUri(): Uri {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = createTempImageFile()
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
  }

  private suspend fun createTestItem(
      category: String = "Clothing",
      type: String = "T-Shirt",
      brand: String = "TestBrand",
      price: Double = 50.0
  ): Item {
    val itemId = repository.getNewItemId()
    val item =
        Item(
            itemUuid = itemId,
            postUuid = "test_post2",
            image = ImageData("test_image_id", "https://example.com/image.jpg"),
            category = category,
            type = type,
            brand = brand,
            price = price,
            material = emptyList(),
            link = "https://example.com",
            ownerId = ownerId)
    repository.addItem(item)
    return item
  }

  @Test
  fun onSaveItemClick_withValidChanges_updatesFirestore() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    // Create an existing item
    val existingItem = createTestItem()
    val oldImageUuid = existingItem.itemUuid
    val oldImageId = existingItem.image.imageId

    // Load it into the viewModel
    viewModel.loadItem(existingItem)

    // Modify fields
    viewModel.setCategory("Shoes")
    viewModel.setType("Sneakers")
    viewModel.setBrand("Nike")
    viewModel.setPrice(99.99)
    viewModel.setLink("https://nike.com/sneakers")
    // Set a new photo
    val newPhotoUri = createReadableUri()
    viewModel.setPhoto(newPhotoUri)

    // Save changes
    viewModel.onSaveItemClick()

    kotlinx.coroutines.delay(1000)

    // Verify save was successful
    assertTrue(viewModel.uiState.first().isSaveSuccessful)

    // Verify changes persisted in Firestore
    val updatedItem = repository.getItemById(existingItem.itemUuid)
    assertEquals("Shoes", updatedItem.category)
    assertEquals("Sneakers", updatedItem.type)
    assertEquals("Nike", updatedItem.brand)
    assertEquals(99.99, updatedItem.price)
    assertEquals("https://nike.com/sneakers", updatedItem.link)
    assertEquals(oldImageUuid, updatedItem.itemUuid)
    assertTrue(updatedItem.image.imageId.isNotEmpty())
    assertFalse(oldImageId == updatedItem.image.imageId)
    assertTrue(updatedItem.image.imageUrl.isNotEmpty())
    // Image should be different from the old one
    assertTrue(updatedItem.image.imageUrl.contains("firebasestorage"))
  }

  @Test
  fun onSaveItemClick_validationFailures_returnErrorMessages() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val existingItem = createTestItem()

    // Case 1: no photo -> validation fails
    viewModel.loadItem(existingItem)
    viewModel.setPhoto(Uri.EMPTY)
    viewModel.setCategory("Bags")
    viewModel.onSaveItemClick()
    kotlinx.coroutines.delay(500)
    assertFalse(viewModel.uiState.first().isSaveSuccessful)
    assertNotNull(viewModel.uiState.first().errorMessage)

    // Case 2: invalid URL -> validation fails with URL message
    viewModel.loadItem(existingItem)
    viewModel.setLink("not-a-valid-url")
    viewModel.onSaveItemClick()
    kotlinx.coroutines.delay(500)
    assertNotNull(viewModel.uiState.first().errorMessage)
    assertTrue(viewModel.uiState.first().errorMessage!!.contains("valid URL"))

    // Case 3: missing category -> validation fails with required fields message
    viewModel.loadItem(existingItem)
    viewModel.setCategory("")
    viewModel.onSaveItemClick()
    kotlinx.coroutines.delay(500)
    assertNotNull(viewModel.uiState.first().errorMessage)
    assertTrue(viewModel.uiState.first().errorMessage!!.contains("required fields"))
  }

  @Test
  fun onSaveItemClick_multipleEdits_persistsLatestChanges() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val existingItem = createTestItem()
    viewModel.loadItem(existingItem)

    // First edit
    viewModel.setBrand("FirstBrand")
    viewModel.setPrice(100.0)
    viewModel.onSaveItemClick()
    kotlinx.coroutines.delay(1000)

    // Second edit
    viewModel.setBrand("SecondBrand")
    viewModel.setPrice(200.0)
    val materials =
        listOf(Material("Wool", 60.0), Material("Cotton", 30.0), Material("Elastane", 10.0))
    viewModel.setMaterial(materials)
    viewModel.onSaveItemClick()
    kotlinx.coroutines.delay(1000)

    assertTrue(viewModel.uiState.first().isSaveSuccessful)
    // Verify latest changes
    val updatedItem = repository.getItemById(existingItem.itemUuid)
    assertEquals("SecondBrand", updatedItem.brand)
    assertEquals(3, updatedItem.material.size)
    assertEquals("Wool", updatedItem.material[0]?.name)
    assertEquals(60.0, updatedItem.material[0]?.percentage)
    assertEquals("Cotton", updatedItem.material[1]?.name)
    assertEquals(30.0, updatedItem.material[1]?.percentage)
    assertEquals(200.0, updatedItem.price)
  }

  @Test
  fun onSaveItemClick_withExistingImageAndNoNewPhoto_keepsOriginalImage() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val existingItem = createTestItem()
    val originalImageUrl = existingItem.image.imageUrl

    viewModel.loadItem(existingItem)

    // Modify only text fields, no new photo
    viewModel.setBrand("UpdatedBrand")
    viewModel.setPrice(150.0)

    viewModel.onSaveItemClick()

    kotlinx.coroutines.delay(1000)

    assertTrue(viewModel.uiState.first().isSaveSuccessful)

    // Verify image URL remains the same
    val updatedItem = repository.getItemById(existingItem.itemUuid)
    assertEquals(originalImageUrl, updatedItem.image.imageUrl)
    assertEquals("UpdatedBrand", updatedItem.brand)
  }

  @Test
  fun deleteItem_removesFromFirestore() = runBlocking {
    val existingItem = createTestItem()
    viewModel.loadItem(existingItem)

    viewModel.deleteItem()

    kotlinx.coroutines.delay(1000)

    // Verify item was deleted
    val items = repository.getAllItems()
    assertEquals(0, items.size)
  }

  @Test
  fun deleteItem_withNoItemLoaded_failsWithError() = runBlocking {
    // Don't load any item
    viewModel.deleteItem()

    kotlinx.coroutines.delay(500)

    assertNotNull(viewModel.uiState.first().errorMessage)
    assertTrue(viewModel.uiState.first().errorMessage!!.contains("No item"))
  }

  @Test
  fun loadItemById_loadsItemFromFirestore() = runBlocking {
    val existingItem = createTestItem(brand = "SpecialBrand", price = 333.33)

    viewModel.loadItemById(existingItem.itemUuid)

    kotlinx.coroutines.delay(500)

    val state = viewModel.uiState.first()
    assertEquals(existingItem.itemUuid, state.itemId)
    assertEquals("SpecialBrand", state.brand)
    assertEquals(333.33, state.price)
    assertEquals("Clothing", state.category)
  }

  @Test
  fun loadItemById_withInvalidId_setsErrorMessage() = runBlocking {
    viewModel.loadItemById("non-existent-id")

    kotlinx.coroutines.delay(500)

    assertNotNull(viewModel.uiState.first().errorMessage)
    assertTrue(viewModel.uiState.first().errorMessage!!.contains("Failed to load"))
  }
}
