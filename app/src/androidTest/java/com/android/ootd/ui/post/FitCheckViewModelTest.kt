package com.android.ootd.ui.post

import android.net.Uri
import com.android.ootd.model.items.ItemsRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FitCheckViewModelTest {

  private lateinit var mockRepository: FakeItemsRepository
  private lateinit var viewModel: FitCheckViewModel

  @Before
  fun setup() {
    mockRepository = FakeItemsRepository()
    viewModel = FitCheckViewModel(mockRepository)
  }

  @Test
  fun defaultUiState_isEmptyAndInvalidPhoto() = runTest {
    val state = viewModel.uiState.first()
    assertEquals(Uri.EMPTY, state.image)
    assertEquals("", state.description)
    assertNull(state.errorMessage)
    assertNull(state.invalidPhotoMsg)
    assertFalse(state.isPhotoValid)
  }

  @Test
  fun setPhoto_withValidUri_updatesStateAndClearsError() = runTest {
    val testUri = Uri.parse("content://test/photo.jpg")
    viewModel.setPhoto(testUri)

    val state = viewModel.uiState.first()
    assertEquals(testUri, state.image)
    assertNull(state.errorMessage)
    assertTrue(state.isPhotoValid)
  }

  @Test
  fun setPhoto_withEmptyUri_setsErrorMessage() = runTest {
    viewModel.setPhoto(Uri.EMPTY)

    val state = viewModel.uiState.first()
    assertEquals(Uri.EMPTY, state.image)
    assertNotNull(state.errorMessage)
    assertTrue(state.errorMessage!!.contains("Please select a photo"))
    assertFalse(state.isPhotoValid)
  }

  @Test
  fun setDescription_updatesUiStateCorrectly() = runTest {
    viewModel.setDescription("My cool outfit")
    val state = viewModel.uiState.first()
    assertEquals("My cool outfit", state.description)
  }

  @Test
  fun setErrorMsg_and_clearError_workAsExpected() = runTest {
    viewModel.setErrorMsg("Something went wrong")
    var state = viewModel.uiState.first()
    assertEquals("Something went wrong", state.errorMessage)

    viewModel.clearError()
    state = viewModel.uiState.first()
    assertNull(state.errorMessage)
  }

  @Test
  fun deleteItemsForPost_callsRepositoryDelete() = runBlocking {
    viewModel.deleteItemsForPost("post123")
    delay(100) // allow coroutine to run
    assertTrue(mockRepository.deleteCalled)
    assertEquals(listOf("post123"), mockRepository.deletedPosts)
  }

  //  @Test
  //  fun deleteItemsForPost_handlesExceptionGracefully() = runTest {
  //    mockRepository.shouldThrow = true
  //    viewModel.deleteItemsForPost("bad_post")
  //    delay(100)
  //    assertTrue(mockRepository.deleteCalled)
  //    // still shouldn't crash or stop execution
  //    assertEquals(0, mockRepository.deletedPosts.size)
  //  }

  @Test
  fun isPhotoValid_returnsFalseWhenInvalidPhotoMsgPresent() {
    val state =
        FitCheckUIState(image = Uri.parse("content://photo.jpg"), invalidPhotoMsg = "Bad photo")
    assertFalse(state.isPhotoValid)
  }

  @Test
  fun isPhotoValid_returnsTrueWhenValidImageAndNoError() {
    val state = FitCheckUIState(image = Uri.parse("content://good.jpg"))
    assertTrue(state.isPhotoValid)
  }

  @Test
  fun uiStateUpdatesAreIndependent() = runTest {
    val uri = Uri.parse("content://image1.jpg")
    viewModel.setPhoto(uri)
    var state = viewModel.uiState.first()
    assertEquals(uri, state.image)

    viewModel.setDescription("awesome outfit")
    state = viewModel.uiState.first()
    assertEquals(uri, state.image)
    assertEquals("awesome outfit", state.description)
  }
}

/** Simple fake repository to track delete calls and simulate failures. */
private class FakeItemsRepository : ItemsRepository {
  val deletedPosts = mutableListOf<String>()
  var shouldThrow = false
  var deleteCalled = false

  override fun getNewItemId(): String = "fake-id"

  override suspend fun getAllItems() = emptyList<com.android.ootd.model.items.Item>()

  override suspend fun getAssociatedItems(postUuid: String) =
      emptyList<com.android.ootd.model.items.Item>()

  override suspend fun getItemById(uuid: String) =
      throw UnsupportedOperationException("Not used in this test")

  override suspend fun addItem(item: com.android.ootd.model.items.Item) =
      throw UnsupportedOperationException("Not used in this test")

  override suspend fun editItem(itemUUID: String, newItem: com.android.ootd.model.items.Item) =
      throw UnsupportedOperationException("Not used in this test")

  override suspend fun deleteItem(uuid: String) =
      throw UnsupportedOperationException("Not used in this test")

  override suspend fun deletePostItems(postUuid: String) {
    deleteCalled = true
    if (shouldThrow) throw Exception("Simulated delete failure")
    deletedPosts.add(postUuid)
  }
}
