package com.android.ootd.ui.post

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.image.ImageCompressor
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.ui.post.items.AddItemsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AddItemsViewModelTest {

  private lateinit var viewModel: AddItemsViewModel
  private lateinit var mockRepository: ItemsRepository
  private lateinit var mockAccountRepository: AccountRepository
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var context: Context
  private lateinit var mockCompressor: ImageCompressor

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockCompressor = mockk(relaxed = true)
    context = ApplicationProvider.getApplicationContext()
    // Recreate ViewModel with mocked compressor
    viewModel =
        AddItemsViewModel(
            repository = mockRepository,
            accountRepository = mockAccountRepository,
            overridePhoto = false,
            imageCompressor = mockCompressor)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `initPostUuid updates postUuid`() {
    viewModel.initPostUuid("test-post")
    assertEquals("test-post", viewModel.uiState.value.postUuid)
  }

  @Test
  fun `setPhoto with valid uri updates state`() {
    val mockUri = mockk<Uri>()
    every { mockUri.toString() } returns "content://test"
    viewModel.setPhoto(mockUri)
    assertEquals(mockUri, viewModel.uiState.value.localPhotoUri)
    assertNull(viewModel.uiState.value.invalidPhotoMsg)
  }

  @Test
  fun `setPhoto with Uri EMPTY sets error`() {
    viewModel.setPhoto(Uri.EMPTY)
    assertEquals("Please select a photo.", viewModel.uiState.value.invalidPhotoMsg)
  }

  @Test
  fun `setCategory updates category`() {
    viewModel.setCategory("Clothing")
    assertEquals("Clothing", viewModel.uiState.value.category)
  }

  @Test
  fun `setCategory with invalid sets error`() {
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setCategory("Invalid")
    assertNotNull(viewModel.uiState.value.invalidCategory)
  }

  @Test
  fun `setPrice updates price`() {
    viewModel.setPrice(50.0)
    assertEquals(50.0, viewModel.uiState.value.price, 0.0)
  }

  @Test
  fun `validateCategory with valid clears error`() {
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setCategory("Clothing")
    viewModel.validateCategory()
    assertNull(viewModel.uiState.value.invalidCategory)
  }

  @Test
  fun `validateCategory with invalid sets error`() {
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setCategory("Invalid")
    viewModel.validateCategory()
    assertNotNull(viewModel.uiState.value.invalidCategory)
  }

  @Test
  fun `onAddItemClick with overridePhoto succeeds`() = runTest {
    val vm = AddItemsViewModel(mockRepository, mockAccountRepository, true)
    vm.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    vm.setCategory("Clothing")

    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"

    coEvery { mockRepository.getNewItemId() } returns "id"
    coEvery { mockRepository.addItem(any()) } returns Unit
    coEvery { mockAccountRepository.addItem(any()) } returns true

    vm.onAddItemClick(context)
    advanceUntilIdle()
    assertTrue(vm.addOnSuccess.value)
  }

  @Test
  fun `onAddItemClick without photo fails`() = runTest {
    viewModel.onAddItemClick(context)
    advanceUntilIdle()
    assertFalse(viewModel.addOnSuccess.value)
    assertNotNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `onAddItemClick with valid input succeeds`() = runTest {
    // Mock FirebaseAuth so FirebaseApp isn't required
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"

    // Mock compressor and uploader
    coEvery { mockCompressor.compressImage(any(), any(), any()) } returns ByteArray(8)
    mockkObject(FirebaseImageUploader)

    val mockUri = mockk<Uri>()
    coEvery { mockRepository.getNewItemId() } returns "id"
    coEvery { FirebaseImageUploader.uploadImage(any(), any(), any()) } returns
        ImageData("id", "url")
    coEvery { mockRepository.addItem(any()) } returns Unit
    coEvery { mockAccountRepository.addItem(any()) } returns true

    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setPhoto(mockUri)
    viewModel.setCategory("Clothing")
    viewModel.onAddItemClick(context)
    advanceUntilIdle()

    assertTrue(viewModel.addOnSuccess.value)
    coVerify(exactly = 1) { mockRepository.addItem(any()) }
  }

  @Test
  fun `onAddItemClick with empty image url fails`() = runTest {
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"

    mockkObject(FirebaseImageUploader)
    val mockUri = mockk<Uri>()
    every { mockUri.toString() } returns "content://test"

    coEvery { mockRepository.getNewItemId() } returns "id"
    coEvery { FirebaseImageUploader.uploadImage(any(), any(), any()) } returns ImageData("", "")

    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setPhoto(mockUri)
    viewModel.setCategory("Clothing")
    viewModel.onAddItemClick(context)
    advanceUntilIdle()

    assertFalse(viewModel.addOnSuccess.value)
  }

  @Test
  fun `onAddItemClick with repository exception still succeeds optimistically`() = runTest {
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"

    mockkObject(FirebaseImageUploader)
    coEvery { mockCompressor.compressImage(any(), any(), any()) } returns ByteArray(8)

    val mockUri = mockk<Uri>()

    coEvery { mockRepository.getNewItemId() } returns "id"
    coEvery { FirebaseImageUploader.uploadImage(any(), any(), any()) } returns
        ImageData("id", "url")
    coEvery { mockRepository.addItem(any()) } throws Exception("error")

    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setPhoto(mockUri)
    viewModel.setCategory("Clothing")
    viewModel.onAddItemClick(context)
    advanceUntilIdle()

    assertTrue(viewModel.addOnSuccess.value)
  }

  @Test
  fun `onAddItemClick with invalid price defaults to zero`() = runTest {
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"

    mockkObject(FirebaseImageUploader)
    coEvery { mockCompressor.compressImage(any(), any(), any()) } returns ByteArray(8)

    val mockUri = mockk<Uri>()

    coEvery { mockRepository.getNewItemId() } returns "id"
    coEvery { FirebaseImageUploader.uploadImage(any(), any(), any()) } returns
        ImageData("id", "url")
    coEvery { mockRepository.addItem(any()) } returns Unit
    coEvery { mockAccountRepository.addItem(any()) } returns true

    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setPhoto(mockUri)
    viewModel.setCategory("Clothing")
    viewModel.setPrice(0.0)
    viewModel.onAddItemClick(context)
    advanceUntilIdle()

    assertTrue(viewModel.addOnSuccess.value)
  }

  @Test
  fun `onAddItemClick with empty postUuid creates empty postUuids list`() = runTest {
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"

    mockkObject(FirebaseImageUploader)
    coEvery { mockCompressor.compressImage(any(), any(), any()) } returns ByteArray(8)

    val mockUri = mockk<Uri>()
    every { mockUri.toString() } returns "content://test"

    coEvery { mockRepository.getNewItemId() } returns "id"
    coEvery { FirebaseImageUploader.uploadImage(any(), any(), any()) } returns
        ImageData("id", "url")
    coEvery { mockRepository.addItem(any()) } returns Unit
    coEvery { mockAccountRepository.addItem(any()) } returns true

    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setPhoto(mockUri)
    viewModel.setCategory("Clothing")
    viewModel.onAddItemClick(context)
    advanceUntilIdle()

    assertTrue(viewModel.addOnSuccess.value)
  }

  @Test
  fun `resetAddSuccess resets flag`() {
    viewModel.resetAddSuccess()
    assertFalse(viewModel.addOnSuccess.value)
  }

  @Test
  fun `addItemAndUpdateInventory handles exception in outer catch`() = runTest {
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "uid"
    mockkObject(FirebaseImageUploader)

    coEvery { mockCompressor.compressImage(any(), any(), any()) } returns ByteArray(8)

    val mockUri = mockk<Uri>()
    coEvery { mockRepository.getNewItemId() } returns "id"
    coEvery { FirebaseImageUploader.uploadImage(any(), any(), any()) } returns
        ImageData("id", "url")
    coEvery { mockRepository.addItem(any()) } throws RuntimeException("Critical error")
    coEvery { mockAccountRepository.addItem(any()) } throws RuntimeException("Critical error")
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.setPhoto(mockUri)
    viewModel.setCategory("Clothing")
    viewModel.onAddItemClick(context)
    advanceUntilIdle()

    assertTrue(viewModel.addOnSuccess.value)
  }

  @Test
  fun `updateCategorySuggestions returns all when input blank`() {
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.updateCategorySuggestions("")
    val suggestions = viewModel.uiState.value.categorySuggestion
    assertTrue(suggestions.size >= 6)
  }

  @Test
  fun `updateCategorySuggestions filters by prefix`() {
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.updateCategorySuggestions("Sho")
    assertEquals(listOf("Shoes"), viewModel.uiState.value.categorySuggestion)
  }

  @Test
  fun `updateCategorySuggestions case insensitive`() {
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    viewModel.updateCategorySuggestions("bag")
    assertEquals(listOf("Bags"), viewModel.uiState.value.categorySuggestion)
  }
}
