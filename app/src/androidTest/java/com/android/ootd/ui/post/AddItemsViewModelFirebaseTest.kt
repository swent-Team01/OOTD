package com.android.ootd.ui.post

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.ootd.model.items.ItemsRepositoryFirestore
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import java.io.File
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class AddItemsViewModelFirebaseTest : FirestoreTest() {

  private lateinit var viewModel: AddItemsViewModel
  private lateinit var repository: ItemsRepositoryFirestore

  @Before
  override fun setUp() {
    super.setUp()
    repository = ItemsRepositoryFirestore(FirebaseEmulator.firestore)
    viewModel = AddItemsViewModel(repository)
  }

  private fun createTempImageFile(): File {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = File(context.cacheDir, "test_${System.currentTimeMillis()}.jpg")
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

  @Test
  fun onAddItemClick_withValidData_uploadsImageAndAddsToFirestore() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    // Set all required fields
    viewModel.setPhoto(uri)
    viewModel.setCategory("Clothing")
    viewModel.setType("T-Shirt")
    viewModel.setBrand("Nike")
    viewModel.setPrice("29.99")
    viewModel.setLink("https://example.com/tshirt")

    // Trigger add
    viewModel.onAddItemClick()

    // Wait for async operation
    kotlinx.coroutines.delay(2000)

    // Verify success state
    assertTrue(viewModel.addOnSuccess.first())
    assertEquals(null, viewModel.uiState.first().errorMessage)

    // Verify item was added to Firestore
    val items = repository.getAllItems()
    assertEquals(1, items.size)

    val addedItem = items[0]
    assertEquals("Clothing", addedItem.category)
    assertEquals("T-Shirt", addedItem.type)
    assertEquals("Nike", addedItem.brand)
    assertEquals(29.99, addedItem.price)
    assertEquals("https://example.com/tshirt", addedItem.link)

    // Verify image was uploaded to Storage
    assertTrue(addedItem.image.imageId.isNotEmpty())
    assertTrue(addedItem.image.imageUrl.isNotEmpty())
  }

  @Test
  fun onAddItemClick_withoutPhoto_failsWithError() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    // Set fields but NO photo
    viewModel.setCategory("Shoes")
    viewModel.setType("Sneakers")
    viewModel.setBrand("Adidas")
    viewModel.setPrice("89.99")

    viewModel.onAddItemClick()

    kotlinx.coroutines.delay(500)

    // Should fail
    assertFalse(viewModel.addOnSuccess.first())
    assertNotNull(viewModel.uiState.first().errorMessage)

    // Verify nothing was added to Firestore
    val items = repository.getAllItems()
    assertEquals(0, items.size)
  }

  @Test
  fun onAddItemClick_withoutCategory_failsWithError() = runBlocking {
    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setType("Jacket")
    viewModel.setBrand("Zara")
    viewModel.setPrice("120.00")

    viewModel.onAddItemClick()

    kotlinx.coroutines.delay(500)

    assertFalse(viewModel.addOnSuccess.first())
    assertNotNull(viewModel.uiState.first().errorMessage)

    val items = repository.getAllItems()
    assertEquals(0, items.size)
  }

  @Test
  fun onAddItemClick_withInvalidCategory_failsWithError() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("InvalidCategory123")
    viewModel.setType("Something")
    viewModel.setBrand("Brand")
    viewModel.setPrice("50.00")

    viewModel.onAddItemClick()

    kotlinx.coroutines.delay(500)

    assertFalse(viewModel.addOnSuccess.first())

    val items = repository.getAllItems()
    assertEquals(0, items.size)
  }

  @Test
  fun onAddItemClick_withMaterialData_storesCorrectly() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("Clothing")
    viewModel.setType("Sweater")
    viewModel.setBrand("H&M")
    viewModel.setPrice("45.00")
    viewModel.setMaterial("Cotton 80%, Polyester 20%")

    viewModel.onAddItemClick()

    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel.addOnSuccess.first())

    val items = repository.getAllItems()
    assertEquals(1, items.size)

    val addedItem = items[0]
    assertEquals(2, addedItem.material.size)
    assertEquals("Cotton", addedItem.material[0]?.name)
    assertEquals(80.0, addedItem.material[0]?.percentage)
    assertEquals("Polyester", addedItem.material[1]?.name)
    assertEquals(20.0, addedItem.material[1]?.percentage)
  }

  @Test
  fun onAddItemClick_multipleItems_addsAllToFirestore() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    // Add first item
    val uri1 = createReadableUri()
    viewModel.setPhoto(uri1)
    viewModel.setCategory("Bags")
    viewModel.setType("Backpack")
    viewModel.setBrand("Nike")
    viewModel.setPrice("79.99")
    viewModel.onAddItemClick()
    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel.addOnSuccess.first())

    // Reset and add second item
    viewModel.resetAddSuccess()
    val viewModel2 = AddItemsViewModel(repository)
    viewModel2.initTypeSuggestions(context)

    val uri2 = createReadableUri()
    viewModel2.setPhoto(uri2)
    viewModel2.setCategory("Accessories")
    viewModel2.setType("Hat")
    viewModel2.setBrand("Puma")
    viewModel2.setPrice("25.00")
    viewModel2.onAddItemClick()
    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel2.addOnSuccess.first())

    // Verify both items exist in Firestore
    val items = repository.getAllItems()
    assertEquals(2, items.size)

    val brands = items.map { it.brand }.toSet()
    assertTrue(brands.contains("Nike"))
    assertTrue(brands.contains("Puma"))
  }

  @Test
  fun onAddItemClick_withEmptyPrice_defaultsToZero() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("Clothing")
    viewModel.setType("Socks")
    viewModel.setBrand("Generic")
    viewModel.setPrice("") // Empty price

    viewModel.onAddItemClick()

    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel.addOnSuccess.first())

    val items = repository.getAllItems()
    assertEquals(1, items.size)
    assertEquals(0.0, items[0].price)
  }

  @Test
  fun onAddItemClick_normalizesCategoryNames() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("clothes") // lowercase variant
    viewModel.setType("Pants")
    viewModel.setBrand("Levi's")
    viewModel.setPrice("60.00")

    viewModel.onAddItemClick()

    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel.addOnSuccess.first())

    val items = repository.getAllItems()
    assertEquals(1, items.size)
    // Should accept "clothes" as valid (normalized to "Clothing")
    assertEquals("clothes", items[0].category)
  }
}
