package com.android.ootd.ui.post

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.items.ItemsRepositoryFirestore
import com.android.ootd.ui.post.items.AddItemsViewModel
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import io.mockk.coEvery
import io.mockk.mockk
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

  private lateinit var context: Context
  private lateinit var viewModel: AddItemsViewModel
  private lateinit var repository: ItemsRepositoryFirestore
  private lateinit var mockAccountRepository: AccountRepository

  @Before
  override fun setUp() {
    super.setUp()
    repository = ItemsRepositoryFirestore(FirebaseEmulator.firestore)
    mockAccountRepository = mockk(relaxed = true)
    // Mock successful inventory operations by default
    coEvery { mockAccountRepository.addItem(any()) } returns true
    viewModel = AddItemsViewModel(repository, mockAccountRepository)
    context = InstrumentationRegistry.getInstrumentation().targetContext
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
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    // Set all required fields
    viewModel.setPhoto(uri)
    viewModel.setCategory("Clothing")
    viewModel.setType("T-Shirt")
    viewModel.setBrand("Nike")
    viewModel.setPrice(29.99)
    viewModel.setCurrency("EUR")
    viewModel.setLink("https://example.com/tshirt")

    // Trigger add
    viewModel.onAddItemClick(context)

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
    assertEquals("EUR", addedItem.currency)
    assertEquals("https://example.com/tshirt", addedItem.link)

    // Verify image was uploaded to Storage
    assertTrue(addedItem.image.imageId.isNotEmpty())
    assertTrue(addedItem.image.imageUrl.isNotEmpty())
  }

  @Test
  fun onAddItemClick_withoutPhoto_failsWithError() = runBlocking {
    viewModel.initTypeSuggestions(context)

    // Set fields but NO photo
    viewModel.setCategory("Shoes")
    viewModel.setType("Sneakers")
    viewModel.setBrand("Adidas")
    viewModel.setPrice(89.99)

    viewModel.onAddItemClick(context)

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
    viewModel.setPrice(120.00)

    viewModel.onAddItemClick(context)

    kotlinx.coroutines.delay(500)

    assertFalse(viewModel.addOnSuccess.first())
    assertNotNull(viewModel.uiState.first().errorMessage)

    val items = repository.getAllItems()
    assertEquals(0, items.size)
  }

  @Test
  fun onAddItemClick_withInvalidCategory_failsWithError() = runBlocking {
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("InvalidCategory123")
    viewModel.setType("Something")
    viewModel.setBrand("Brand")
    viewModel.setPrice(50.00)

    viewModel.onAddItemClick(context)

    kotlinx.coroutines.delay(500)

    assertFalse(viewModel.addOnSuccess.first())

    val items = repository.getAllItems()
    assertEquals(0, items.size)
  }

  @Test
  fun onAddItemClick_withMaterialData_storesCorrectly() = runBlocking {
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("Clothing")
    viewModel.setType("Sweater")
    viewModel.setBrand("H&M")
    viewModel.setPrice(45.00)
    viewModel.setMaterial("Cotton 80%, Polyester 20%")

    viewModel.onAddItemClick(context)

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
    viewModel.initTypeSuggestions(context)

    // Add first item
    val uri1 = createReadableUri()
    viewModel.setPhoto(uri1)
    viewModel.setCategory("Bags")
    viewModel.setType("Backpack")
    viewModel.setBrand("Nike")
    viewModel.setPrice(79.99)
    viewModel.setCurrency("USD")
    viewModel.onAddItemClick(context)
    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel.addOnSuccess.first())

    // Reset and add second item
    viewModel.resetAddSuccess()
    val viewModel2 = AddItemsViewModel(repository, mockAccountRepository)
    viewModel2.initTypeSuggestions(context)

    val uri2 = createReadableUri()
    viewModel2.setPhoto(uri2)
    viewModel2.setCategory("Accessories")
    viewModel2.setType("Hat")
    viewModel2.setBrand("Puma")
    viewModel2.setPrice(25.00)
    viewModel2.setCurrency("CHF")
    viewModel2.onAddItemClick(context)
    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel2.addOnSuccess.first())

    // Verify both items exist in Firestore
    val items = repository.getAllItems()
    assertEquals(2, items.size)

    val brands = items.map { it.brand }.toSet()
    assertTrue(brands.contains("Nike"))
    assertTrue(brands.contains("Puma"))

    // Verify currencies persisted per item
    val first = items.first { it.brand == "Nike" }
    val second = items.first { it.brand == "Puma" }
    assertEquals("USD", first.currency)
    assertEquals("CHF", second.currency)
  }

  @Test
  fun onAddItemClick_withEmptyPrice_defaultsToZero() = runBlocking {
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("Clothing")
    viewModel.setType("Socks")
    viewModel.setBrand("Generic")
    // Intentionally do not call setPrice; default should be 0.0

    viewModel.onAddItemClick(context)

    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel.addOnSuccess.first())

    val items = repository.getAllItems()
    assertEquals(1, items.size)
    assertEquals(0.0, items[0].price)
  }

  @Test
  fun onAddItemClick_withExactCategoryFromDropdown_addsSuccessfully() = runBlocking {
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("Clothing") // Exact match from dropdown (no longer accepts "clothes")
    viewModel.setType("Pants")
    viewModel.setBrand("Levi's")
    viewModel.setPrice(60.00)

    viewModel.onAddItemClick(context)

    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel.addOnSuccess.first())

    val items = repository.getAllItems()
    assertEquals(1, items.size)
    // Category should be exactly as selected from dropdown
    assertEquals("Clothing", items[0].category)
  }

  @Test
  fun onAddItemClick_withAdditionalDetails_persistsAllFields() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    viewModel.initTypeSuggestions(context)

    val uri = createReadableUri()

    viewModel.setPhoto(uri)
    viewModel.setCategory("Clothing")
    viewModel.setType("Coat")
    viewModel.setBrand("Uniqlo")
    viewModel.setPrice(150.0)
    viewModel.setCondition("Like new")
    viewModel.setSize("M")
    viewModel.setFitType("Oversized")
    viewModel.setStyle("Minimalist")
    viewModel.setNotes("Worn twice")

    viewModel.onAddItemClick()
    kotlinx.coroutines.delay(2000)

    assertTrue(viewModel.addOnSuccess.first())

    val items = repository.getAllItems()
    assertEquals(1, items.size)

    val addedItem = items[0]
    assertEquals("Like new", addedItem.condition)
    assertEquals("M", addedItem.size)
    assertEquals("Oversized", addedItem.fitType)
    assertEquals("Minimalist", addedItem.style)
    assertEquals("Worn twice", addedItem.notes)
  }
}
