package com.android.ootd.ui.items

import android.content.Context
import android.net.Uri
import com.android.ootd.model.image.ImageCompressor
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Material
import com.android.ootd.ui.post.items.AddItemsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddItemsViewModelTest {

  private val fakeRepository = FakeItemsRepository()
  private val fakeAccountRepository = FakeAccountRepository()
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var viewModel: AddItemsViewModel
  private lateinit var context: Context
  private lateinit var compressor: ImageCompressor

  private fun seedCategories(vararg categories: String) {
    val field =
        com.android.ootd.ui.post.items.BaseItemViewModel::class
            .java
            .getDeclaredField("typeSuggestions")
    field.isAccessible = true
    field.set(viewModel, categories.associateWith { emptyList<String>() })
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = mockk<Context>(relaxed = true)
    compressor = mockk<ImageCompressor>(relaxed = true)
    // Mock the suspend function with coEvery
    coEvery { compressor.compressImage(any(), any(), any()) } returns ByteArray(10)
    viewModel =
        AddItemsViewModel(
            repository = fakeRepository,
            accountRepository = fakeAccountRepository,
            overridePhoto = false,
            imageCompressor = compressor)
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun onAddItemClick_successUpdatesState_andPersistsItem() = runTest {
    mockkStatic(FirebaseAuth::class)
    mockkObject(Firebase)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"
    mockkObject(FirebaseImageUploader)
    coEvery { FirebaseImageUploader.uploadImage(any(), any(), any()) } returns
        ImageData("id", "url")

    seedCategories("Clothing")
    viewModel.setCategory("Clothing")
    viewModel.setType("Jacket")
    viewModel.setBrand("BrandX")
    viewModel.setPrice(120.0)
    viewModel.setCurrency("EUR")
    viewModel.setMaterial("Cotton 80%, Polyester 20%")
    viewModel.setLink("https://example.com")
    viewModel.setCondition("New")
    viewModel.setSize("M")
    viewModel.setFitType("Regular")
    viewModel.setStyle("Casual")
    viewModel.setNotes("Sample notes")

    val existingState = viewModel.uiState.value
    val mockUri = mockk<Uri>(relaxed = true)
    viewModel.setPhoto(mockUri)

    viewModel.onAddItemClick(context)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(viewModel.addOnSuccess.value)
    val addedItem = fakeRepository.lastAddedItem
    requireNotNull(addedItem)
    assertEquals("Clothing", addedItem.category)
    assertEquals("Jacket", addedItem.type)
    assertEquals("BrandX", addedItem.brand)
    assertEquals(120.0, addedItem.price)
    assertEquals("EUR", addedItem.currency)
    assertEquals("https://example.com", addedItem.link)
    assertEquals("New", addedItem.condition)
    assertEquals("M", addedItem.size)
    assertEquals("Regular", addedItem.fitType)
    assertEquals("Casual", addedItem.style)
    assertEquals("Sample notes", addedItem.notes)
    assertEquals(existingState.postUuid, addedItem.postUuids.firstOrNull() ?: "")
  }

  @Test
  fun onAddItemClick_missingFields_setsError() = runTest {
    mockkStatic(FirebaseAuth::class)
    mockkObject(Firebase)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"
    mockkObject(FirebaseImageUploader)
    coEvery { FirebaseImageUploader.uploadImage(any(), any(), any()) } returns
        ImageData("id", "url")

    seedCategories("Clothing")
    viewModel.setCategory("")
    val mockUri = mockk<Uri>(relaxed = true)
    viewModel.setPhoto(mockUri)

    viewModel.onAddItemClick(context)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(viewModel.addOnSuccess.value)
    assertEquals("Please enter a category before adding the item.", state.errorMessage)
  }

  @Test
  fun categoryValidation_flagsInvalidEntries() {
    seedCategories("Clothing")
    viewModel.setCategory("InvalidCategory")
    viewModel.validateCategory()
    assertEquals("Please enter a valid category.", viewModel.uiState.value.invalidCategory)
  }

  @Test
  fun setFitType_andStyle_updateState() {
    viewModel.setFitType("Slim")
    viewModel.setStyle("Streetwear")
    assertEquals("Slim", viewModel.uiState.value.fitType)
    assertEquals("Streetwear", viewModel.uiState.value.style)
  }

  @Test
  fun setMaterial_parsesEntries() {
    viewModel.setMaterial("Cotton 60%, Polyester 30%, Elastane 10%, InvalidEntry")
    val parsed = viewModel.uiState.value.material
    assertEquals(3, parsed.size)
    assertEquals(Material("Cotton", 60.0), parsed[0])
    assertEquals(Material("Polyester", 30.0), parsed[1])
    assertEquals(Material("Elastane", 10.0), parsed[2])
  }
}
