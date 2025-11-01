package com.android.ootd.ui.post

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.Material
import com.android.ootd.model.post.OutfitPostRepositoryFirestore
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test

class OutfitPreviewViewModelFirebaseTest : FirestoreTest() {

  private lateinit var viewModel: OutfitPreviewViewModel
  private lateinit var postRepository: OutfitPostRepositoryFirestore
  private lateinit var auth: FirebaseAuth

  @Before
  override fun setUp() {
    super.setUp()
    auth = FirebaseEmulator.auth
    postRepository =
        OutfitPostRepositoryFirestore(FirebaseEmulator.firestore, FirebaseEmulator.storage)
    viewModel = OutfitPreviewViewModel(itemsRepository, postRepository)

    // Sign in a test user
    runBlocking {
      try {
        auth.signInAnonymously().await()
      } catch (_: Exception) {
        // User might already be signed in
      }
    }
  }

  private fun createTempImageFile(): File {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = File(context.cacheDir, "test_outfit_${System.currentTimeMillis()}.jpg")
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

  private suspend fun createTestItem(postUuid: String): Item {
    val item =
        Item(
            itemUuid = itemsRepository.getNewItemId(),
            postUuid = postUuid,
            image = ImageData("test_img", "https://example.com/test.jpg"),
            category = "Clothing",
            type = "T-Shirt",
            brand = "TestBrand",
            price = 29.99,
            material = listOf(Material("Cotton", 100.0)),
            link = "https://example.com/item",
            ownerId = auth.currentUser?.uid ?: "")
    itemsRepository.addItem(item)
    return item
  }

  @Test
  fun publishPost_withValidData_successfullySavesPostAndUpdatesState() = runBlocking {
    val imageUri = createReadableUri()
    val description = "My awesome outfit for today!"

    val beforeTimestamp = System.currentTimeMillis()
    // Initialize the viewModel
    viewModel.initFromFitCheck(imageUri.toString(), description)
    delay(500) // Wait for initialization

    val state = viewModel.uiState.first()
    val postUuid = state.postUuid

    // Create some test items associated with this post
    createTestItem(postUuid)
    createTestItem(postUuid)

    // Reload items
    viewModel.loadItemsForPost()
    delay(500)

    // Verify items are loaded
    val stateWithItems = viewModel.uiState.first()
    assertEquals(2, stateWithItems.items.size)

    // Now publish the post
    viewModel.publishPost()

    // Wait for the async operation to complete
    delay(3000)

    // Verify the UI state is updated correctly (this is the uncovered code)
    val finalState = viewModel.uiState.first()
    assertFalse(finalState.isLoading)
    assertTrue(finalState.isPublished)
    assertEquals("Post created successfully!", finalState.successMessage)

    val afterTimestamp = System.currentTimeMillis()
    // Verify the post was saved to Firestore
    val savedPost = postRepository.getPostById(postUuid)
    assertNotNull(savedPost)
    assertTrue(savedPost!!.timestamp >= beforeTimestamp)
    assertTrue(savedPost.timestamp <= afterTimestamp)
    assertEquals(postUuid, savedPost!!.postUID)
    assertEquals(description, savedPost.description)
    assertEquals(2, savedPost.itemsID.size)
    assertEquals(auth.currentUser?.uid, savedPost.ownerId) // check owner ID
    assertNotNull(savedPost.name) // User's display name
    assertTrue(savedPost!!.outfitURL.isNotEmpty()) // check outfit URL is set
  }

  @Test
  fun publishPost_withMissingRequiredData_failsWithErrorMessage() = runBlocking {
    // Test 1: Initialize with empty image URI
    viewModel.initFromFitCheck("", "Test description")
    delay(500)

    val state1 = viewModel.uiState.first()
    createTestItem(state1.postUuid)

    viewModel.publishPost()
    delay(500)

    val finalState1 = viewModel.uiState.first()
    assertFalse(finalState1.isPublished)
    assertNotNull(finalState1.errorMessage)
    assertTrue(finalState1.errorMessage!!.contains("Missing required post data"))

    // Clear error for next test
    viewModel.clearErrorMessage()
    val stateNullMessage = viewModel.uiState.first()
    assertNull(stateNullMessage.errorMessage)

    delay(100)

    // Test 2: Don't initialize, so postUuid will be empty
    // Create a new viewModel instance to ensure clean state
    val viewModel2 = OutfitPreviewViewModel(itemsRepository, postRepository)

    viewModel2.publishPost()
    delay(500)

    val finalState2 = viewModel2.uiState.first()
    assertFalse(finalState2.isPublished)
    assertNotNull(finalState2.errorMessage)
    assertTrue(finalState2.errorMessage!!.contains("Missing required post data"))
  }

  @Test
  fun publishPost_withNoItems_stillSucceeds() = runBlocking {
    val imageUri = createReadableUri()
    val description = "Outfit with no items"

    viewModel.initFromFitCheck(imageUri.toString(), description)
    delay(500)

    // Don't add any items, just publish
    viewModel.publishPost()
    delay(3000)

    val finalState = viewModel.uiState.first()
    assertTrue(finalState.isPublished)
    assertEquals("Post created successfully!", finalState.successMessage)

    val savedPost = postRepository.getPostById(finalState.postUuid)
    assertNotNull(savedPost)
    assertEquals(0, savedPost!!.itemsID.size)
  }

  @Test
  fun initFromFitCheck_andLoadItems_handlesInitializationCorrectly() = runBlocking {
    // Test 1: Verify loading items without initialization doesn't crash
    viewModel.loadItemsForPost()
    delay(500)

    val stateBeforeInit = viewModel.uiState.first()
    assertEquals(0, stateBeforeInit.items.size)
    assertTrue(stateBeforeInit.postUuid.isEmpty())

    // Test 2: Initialize and verify postUuid is generated and state is set correctly
    val imageUri = createReadableUri()
    val description = "Init test"

    viewModel.initFromFitCheck(imageUri.toString(), description)
    delay(500)

    val stateAfterInit = viewModel.uiState.first()
    assertTrue(stateAfterInit.postUuid.isNotEmpty())
    assertEquals(imageUri.toString(), stateAfterInit.imageUri)
    assertEquals(description, stateAfterInit.description)
    assertEquals(0, stateAfterInit.items.size) // Still no items since none were added

    // Test 3: Add an item and verify loading works after initialization
    val postUuid = stateAfterInit.postUuid
    createTestItem(postUuid)

    viewModel.loadItemsForPost()
    delay(500)

    val stateWithItems = viewModel.uiState.first()
    assertEquals(1, stateWithItems.items.size)
  }
}
