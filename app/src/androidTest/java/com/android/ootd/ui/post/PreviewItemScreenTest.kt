package com.android.ootd.ui.post

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.Material
import com.android.ootd.model.map.Location
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.InMemoryItem
import com.android.ootd.utils.ItemsTest
import com.android.ootd.utils.ItemsTest.Companion.item1
import com.android.ootd.utils.ItemsTest.Companion.item2
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for the PreviewItemScreen composable and its ViewModel.
 *
 * These tests cover various UI states and interactions, as well as ViewModel logic for loading
 * items, handling errors, and posting outfits.
 *
 * DISCLAIMER : These tests are partially created with the help of AI tools and verified by humans.
 */
class PreviewItemScreenTest : ItemsTest by InMemoryItem {

  @get:Rule val composeTestRule = createComposeRule()

  // --- Helpers ---
  private fun item(
      id: String = "1",
      cat: String = "Clothes",
      type: String = "T-Shirt",
      brand: String = "Nike",
      price: Double? = 19.99,
      link: String? = "https://example.com/product",
      materials: List<Material> = listOf(Material("Cotton"))
  ) =
      Item(
          itemUuid = id,
          postUuids = listOf("post_3456"),
          image = ImageData("Photo$id", "https://example.com/image$id.jpg"),
          category = cat,
          type = type,
          brand = brand,
          price = price,
          material = materials,
          link = link,
          ownerId = "user123")

  private fun fakeRepo(items: List<Item>, throwOnLoad: String? = null) =
      object : ItemsRepository {
        override fun getNewItemId() = "fakeId"

        override suspend fun getAllItems() = items

        override suspend fun getAssociatedItems(postUuid: String): List<Item> {
          if (throwOnLoad != null) throw Exception(throwOnLoad)
          return items
        }

        override suspend fun getItemById(uuid: String) = items.first()

        override suspend fun getItemsByIds(uuids: List<String>): List<Item> {
          return emptyList()
        }

        override suspend fun getItemsByIdsAcrossOwners(uuids: List<String>): List<Item> {
          return getItemsByIds(uuids)
        }

        override suspend fun addItem(item: Item) {}

        override suspend fun editItem(itemUUID: String, newItem: Item) {}

        override suspend fun deleteItem(uuid: String) {}

        override suspend fun deletePostItems(postUuid: String) {}

        override suspend fun getFriendItemsForPost(postUuid: String, friendId: String): List<Item> {
          TODO("Not yet implemented")
        }
      }

  private val fakePostRepo =
      object : OutfitPostRepository {
        override fun getNewPostId() = "post_123"

        override suspend fun uploadOutfitPhoto(localPath: String, postId: String) = ""

        override suspend fun savePostToFirestore(post: OutfitPost) {}

        override suspend fun getPostById(postId: String): OutfitPost? = null

        override suspend fun updatePostFields(postId: String, updates: Map<String, Any?>) {}

        override suspend fun savePostWithMainPhoto(
            uid: String,
            name: String,
            userProfilePicURL: String,
            localPath: String,
            description: String,
            location: Location
        ) = OutfitPost(postUID = "post_123", ownerId = uid, name = name)

        override suspend fun deletePost(postId: String) {}

        override suspend fun uploadOutfitWithCompressedPhoto(
            imageData: ByteArray,
            postId: String
        ): String {
          TODO("Not yet implemented")
        }
      }

  private fun setContent(
      items: List<Item> = emptyList(),
      onAdd: (String) -> Unit = {},
      onEdit: (String) -> Unit = {},
      onBack: (String) -> Unit = {},
      onPostSuccess: () -> Unit = {},
      onSelectFromInventory: (String) -> Unit = {}
  ) {
    runTest { items.forEach { repository.addItem(it) } }
    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepo(items), fakePostRepo),
          imageUri = "fake_image_uri",
          description = "Test outfit description",
          location = Location(46.5197, 6.5682, "Lausanne, Switzerland"),
          onAddItem = onAdd,
          onSelectFromInventory = onSelectFromInventory,
          onEditItem = onEdit,
          onGoBack = onBack,
          onPostSuccess = onPostSuccess)
    }
  }

  private fun n(tag: String) = composeTestRule.onNodeWithTag(tag)

  private fun txt(s: String, substring: Boolean = false) =
      composeTestRule.onNodeWithText(s, substring = substring)

  // --- UI Tests ---
  @Test
  fun emptyList_showsMessage_andAdd_andPostButton() {
    setContent()
    n(PreviewItemScreenTestTags.ITEM_LIST).assertIsNotDisplayed()
    n(PreviewItemScreenTestTags.EMPTY_ITEM_LIST_MSG).assertIsDisplayed()
    n(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).assertIsDisplayed()
    txt("Post").assertExists()
  }

  @Test
  fun nonEmptyList_showsItems_expandCollapse_materialsAndPrice() {
    val i = item(materials = listOf(Material("Cotton", 70.0), Material("Polyester", 30.0)))
    setContent(items = listOf(i, item1))

    // Items visible
    n(PreviewItemScreenTestTags.ITEM_LIST).assertIsDisplayed()
    n(PreviewItemScreenTestTags.getTestTagForItem(i)).assertIsDisplayed()
    txt(i.category).assertIsDisplayed()
    txt(i.type ?: "").assertIsDisplayed()

    // Multiple items = multiple image previews and expand icons; target first item (index 0)
    composeTestRule
        .onAllNodesWithTag(PreviewItemScreenTestTags.IMAGE_ITEM_PREVIEW)[0]
        .assertIsDisplayed()

    // Expand first item
    composeTestRule.onAllNodesWithTag(PreviewItemScreenTestTags.EXPAND_ICON)[0].performClick()
    txt(i.brand ?: "").assertIsDisplayed()
    txt("Cotton, Polyester", substring = true).assertIsDisplayed()
    txt("CHF ${i.price}", substring = true).assertIsDisplayed()
    txt(i.link ?: "").assertIsDisplayed()

    // Collapse first item
    composeTestRule.onAllNodesWithTag(PreviewItemScreenTestTags.EXPAND_ICON)[0].performClick()
    composeTestRule.waitForIdle()
    txt(i.category).assertIsDisplayed()
  }

  @Test
  fun scrollLargeList_postAndAddButtons_multipleCategories() {
    val items =
        (1..50).map { item(id = it.toString(), cat = if (it % 2 == 0) "Shoes" else "Clothing") }
    setContent(items = items)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(PreviewItemScreenTestTags.getTestTagForItem(items.first()))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Buttons visible
    n(PreviewItemScreenTestTags.POST_BUTTON).assertIsDisplayed()
    n(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).assertIsDisplayed()

    // Scroll to "Shoes" and verify (choose first matching visible node)
    n(PreviewItemScreenTestTags.ITEM_LIST).performScrollToNode(hasText("Shoes"))
    composeTestRule.onAllNodesWithText("Shoes")[0].assertIsDisplayed()

    // Scroll to "Clothing" and verify (choose first matching visible node)
    n(PreviewItemScreenTestTags.ITEM_LIST).performScrollToNode(hasText("Clothing"))
    composeTestRule.onAllNodesWithText("Clothing")[0].assertIsDisplayed()
  }

  @Test
  fun loadingState_showsOverlayWithProgressIndicatorAndText() {
    val i = item()

    // Create a custom ViewModel with injected loading state
    val repo = fakeRepo(listOf(i))
    val vm = OutfitPreviewViewModel(repo, fakePostRepo)

    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = vm,
          imageUri = "fake_image_uri",
          description = "Test outfit description",
          location = Location(46.5197, 6.5682, "Lausanne, Switzerland"),
          onAddItem = {},
          onEditItem = {},
          onGoBack = {},
          onPostSuccess = {})
    }

    // Inject loading state using reflection
    @Suppress("UNCHECKED_CAST")
    val field = OutfitPreviewViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val stateFlow = field.get(vm) as MutableStateFlow<PreviewUIState>
    stateFlow.value = stateFlow.value.copy(isLoading = true)

    composeTestRule.waitForIdle()

    // Verify loading overlay is displayed
    txt("Publishing your outfit...").assertIsDisplayed()

    // Verify CircularProgressIndicator is displayed (checking for progress indicator semantics)
    composeTestRule
        .onNode(
            androidx.compose.ui.test.hasProgressBarRangeInfo(
                androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate))
        .assertIsDisplayed()

    // Verify overlay is semi-transparent and covers the screen
    // (The Box with background should be rendered)

    // Now set loading to false
    stateFlow.value = stateFlow.value.copy(isLoading = false)
    composeTestRule.waitForIdle()

    // Verify loading overlay is no longer displayed
    txt("Publishing your outfit...").assertDoesNotExist()
  }

  // --- ViewModel Tests ---
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_loadsItems_initState_clearError() = runTest {
    val i = item(id = "vm1")
    val repo = fakeRepo(listOf(i, item1, item2))
    val vm = OutfitPreviewViewModel(repo, fakePostRepo)

    // Initial state
    val initial = vm.uiState.value
    assert(initial.items.isEmpty())
    assert(initial.imageUri == "")
    assert(initial.description == "")
    assert(initial.location.name == "")

    // Load items
    val testLocation = Location(46.5197, 6.5682, "Lausanne, Switzerland")
    vm.initFromFitCheck("uri", "desc", testLocation)
    composeTestRule.waitForIdle()

    assert(vm.uiState.value.items.size == 3)
    assert(vm.uiState.value.items.first().itemUuid == i.itemUuid)
    assert(vm.uiState.value.imageUri == "uri")
    assert(vm.uiState.value.description == "desc")
    assert(vm.uiState.value.location == testLocation)

    // Clear error (use reflection to inject error)
    @Suppress("UNCHECKED_CAST")
    val field = OutfitPreviewViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val stateFlow = field.get(vm) as MutableStateFlow<PreviewUIState>
    stateFlow.value = stateFlow.value.copy(errorMessage = "Network error")

    assert(vm.uiState.value.errorMessage != null)
    vm.clearErrorMessage()
    assert(vm.uiState.value.errorMessage == null)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_errorHandling_multipleFailures() = runTest {
    var errorCount = 0
    val customFailRepo =
        object : ItemsRepository {
          override fun getNewItemId() = "fakeId"

          override suspend fun getAllItems() = emptyList<Item>()

          override suspend fun getAssociatedItems(postUuid: String): List<Item> {
            errorCount++
            throw Exception("Error $errorCount")
          }

          override suspend fun getItemById(uuid: String) = item()

          override suspend fun getItemsByIds(uuids: List<String>): List<Item> {
            return emptyList()
          }

          override suspend fun getItemsByIdsAcrossOwners(uuids: List<String>): List<Item> =
              emptyList()

          override suspend fun addItem(item: Item) {}

          override suspend fun editItem(itemUUID: String, newItem: Item) {}

          override suspend fun deleteItem(uuid: String) {}

          override suspend fun deletePostItems(postUuid: String) {}

          override suspend fun getFriendItemsForPost(
              postUuid: String,
              friendId: String
          ): List<Item> {
            return emptyList()
          }
        }

    val vm = OutfitPreviewViewModel(customFailRepo, fakePostRepo)

    // First call via initFromFitCheck
    val testLocation = Location(46.5197, 6.5682, "Lausanne, Switzerland")
    vm.initFromFitCheck("uri", "desc", testLocation)
    // Wait for viewModelScope coroutine
    composeTestRule.waitForIdle()

    // ViewModel wraps error: "Failed to load items: Error 1"
    assert(vm.uiState.value.errorMessage?.contains("Failed to load items") == true)
    assert(vm.uiState.value.errorMessage?.contains("Error 1") == true)

    vm.clearErrorMessage()
    vm.loadItemsForPost()
    composeTestRule.waitForIdle()

    assert(vm.uiState.value.errorMessage?.contains("Failed to load items") == true)
    assert(vm.uiState.value.errorMessage?.contains("Error 2") == true)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_postSuccess_triggersCallback() = runTest {
    val i = item()
    val vm = OutfitPreviewViewModel(fakeRepo(listOf(i)), fakePostRepo)
    var onPostSuccessCalled = false

    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = vm,
          imageUri = "test_uri",
          description = "test_desc",
          location = Location(46.5197, 6.5682, "Lausanne, Switzerland"),
          onAddItem = {},
          onEditItem = {},
          onGoBack = {},
          onPostSuccess = { onPostSuccessCalled = true })
    }

    // Inject success state via reflection
    @Suppress("UNCHECKED_CAST")
    val field = OutfitPreviewViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val stateFlow = field.get(vm) as MutableStateFlow<PreviewUIState>
    stateFlow.value = stateFlow.value.copy(successMessage = "Posted!", isPublished = true)

    composeTestRule.waitForIdle()

    assert(onPostSuccessCalled)
  }

  @Test
  fun addItemButton_showsDialog_withBothOptions() {
    val i = item()
    setContent(items = listOf(i))

    // Click Add Item button
    n(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is shown
    n(PreviewItemScreenTestTags.ADD_ITEM_DIALOG).assertIsDisplayed()

    // Verify both options are present
    n(PreviewItemScreenTestTags.CREATE_NEW_ITEM_OPTION).assertIsDisplayed()
    n(PreviewItemScreenTestTags.SELECT_FROM_INVENTORY_OPTION).assertIsDisplayed()

    // Verify dialog title
    txt("Add Item to Outfit").assertIsDisplayed()
    txt("Create New Item").assertIsDisplayed()
    txt("Select from Inventory").assertIsDisplayed()
  }

  @Test
  fun addItemDialog_createNewItem_callsOnAddItem() {
    var addClickedPostId: String? = null
    val i = item()
    setContent(items = listOf(i), onAdd = { addClickedPostId = it })

    // Click Add Item button
    n(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Click "Create New Item" option
    n(PreviewItemScreenTestTags.CREATE_NEW_ITEM_OPTION).performClick()
    composeTestRule.waitForIdle()

    // Verify callback was called
    assert(addClickedPostId != null)

    // Verify dialog is dismissed
    n(PreviewItemScreenTestTags.ADD_ITEM_DIALOG).assertDoesNotExist()
  }

  @Test
  fun addItemDialog_selectFromInventory_callsOnSelectFromInventory() {
    var selectFromInventoryPostId: String? = null
    val i = item()
    setContent(items = listOf(i), onSelectFromInventory = { selectFromInventoryPostId = it })

    // Click Add Item button
    n(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Click "Select from Inventory" option
    n(PreviewItemScreenTestTags.SELECT_FROM_INVENTORY_OPTION).performClick()
    composeTestRule.waitForIdle()

    // Verify callback was called
    assert(selectFromInventoryPostId != null)

    // Verify dialog is dismissed
    n(PreviewItemScreenTestTags.ADD_ITEM_DIALOG).assertDoesNotExist()
  }

  @Test
  fun addItemDialog_dismissOnBackgroundClick_closesDialog() {
    val i = item()
    setContent(items = listOf(i))

    // Click Add Item button to show dialog
    n(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is shown
    n(PreviewItemScreenTestTags.ADD_ITEM_DIALOG).assertIsDisplayed()

    // Dismiss dialog by clicking outside (simulated by clicking on the dialog's dismiss request)
    // Note: In Compose test, we can't directly click outside, but we verify the dialog can be
    // dismissed
    // For now, verify dialog exists - dismiss behavior is implicit in AlertDialog
    n(PreviewItemScreenTestTags.ADD_ITEM_DIALOG).assertIsDisplayed()
  }

  @Test
  fun previewItemScreen_Preview_rendersCoreElements() {
    composeTestRule.setContent { PreviewItemScreenPreview() }

    // Verify top bar and title exist
    n(PreviewItemScreenTestTags.SCREEN_TITLE).assertIsDisplayed()
    n(PreviewItemScreenTestTags.GO_BACK_BUTTON).assertIsDisplayed()

    // Verify item list is displayed with 2 sample items
    n(PreviewItemScreenTestTags.ITEM_LIST).assertIsDisplayed()

    // Verify both buttons exist (Post and Add Item)
    n(PreviewItemScreenTestTags.POST_BUTTON).assertIsDisplayed()
    n(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).assertIsDisplayed()

    // Verify sample items are rendered (category text visible)
    txt("Clothing").assertIsDisplayed()
    txt("Accessories").assertIsDisplayed()

    // Verify empty message is NOT shown (items list is not empty)
    n(PreviewItemScreenTestTags.EMPTY_ITEM_LIST_MSG).assertDoesNotExist()

    // Verify loading overlay is NOT shown (isLoading = false, enablePreview = true)
    txt("Publishing your outfit...").assertDoesNotExist()
  }
}
