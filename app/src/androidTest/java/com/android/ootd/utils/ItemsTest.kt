package com.android.ootd.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.ootd.model.Item
import com.android.ootd.model.ItemsRepository
import com.android.ootd.model.ItemsRepositoryProvider
import com.android.ootd.ui.post.AddItemScreenTestTags
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before

interface ItemsTest {

  fun createInitializedRepository(): ItemsRepository

  val repository: ItemsRepository
    get() = ItemsRepositoryProvider.repository

  @Before
  fun setUp() {
    ItemsRepositoryProvider.repository = createInitializedRepository()
  }

  fun ComposeTestRule.enterAddItemPhoto(viewModel: com.android.ootd.ui.post.AddItemsViewModel) {
    // Simulate selecting an image by setting a dummy URI in the ViewModel
    val uri = android.net.Uri.parse("content://dummy/photo.jpg")
    viewModel.setPhoto(uri)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed()
  }

  fun ComposeTestRule.checkImageUploadButtonIsDisplayed() {
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed()
  }

  fun ComposeTestRule.checkImageUploadButtonClickable() {
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.checkPhotoPreviewDisplayed() {
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PREVIEW).assertIsDisplayed()
  }

  fun ComposeTestRule.enterAddItemType(type: String) {
    onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput(type)
  }

  fun ComposeTestRule.enterAddItemCategory(category: String) {
    onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput(category)
  }

  fun ComposeTestRule.enterAddItemBrand(brand: String) {
    onNodeWithTag(AddItemScreenTestTags.INPUT_BRAND).performTextInput(brand)
  }

  fun ComposeTestRule.enterAddItemPrice(price: Double) {
    onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE).performTextInput(price.toString())
  }

  fun ComposeTestRule.enterAddItemLink(Link: String) {
    onNodeWithTag(AddItemScreenTestTags.INPUT_LINK).performTextInput(Link)
  }

  fun ComposeTestRule.enterAddItemMaterial(material: String) {
    onNodeWithTag(AddItemScreenTestTags.INPUT_MATERIAL).performTextInput(material)
  }

  fun ComposeTestRule.enterAddItemDetails(item: Item) {
    enterAddItemType(item.type)
    enterAddItemCategory(item.category)
    enterAddItemBrand(item.brand)
    enterAddItemPrice(item.price)
    enterAddItemLink(item.link)
    item.material.forEach { enterAddItemMaterial(it.name) }
  }

  fun ComposeTestRule.clickOnSaveForAddItem() {
    onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON).assertIsDisplayed().performClick()
  }

  fun checkNoItemWereAdded(action: () -> Unit) {
    val nbItems = runBlocking { repository.getAllItems().size }
    action()
    runTest { assertEquals(nbItems, runBlocking { repository.getAllItems().size }) }
  }
}
