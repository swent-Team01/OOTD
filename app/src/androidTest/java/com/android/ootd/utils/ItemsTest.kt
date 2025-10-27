package com.android.ootd.utils

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.ui.post.AddItemScreenTestTags
import com.android.ootd.ui.post.AddItemsViewModel
import org.junit.Before

interface ItemsTest {

  fun createInitializedRepository(): ItemsRepository

  val repository: ItemsRepository

  @Before
  fun setUp() {
    ItemsRepositoryProvider.setRepository(createInitializedRepository())
  }

  // Robustly bring a tagged node into view across devices where the list may or may not be
  // scrollable.
  fun ComposeTestRule.ensureVisible(tag: String) {
    // Already displayed? Nothing to do.
    val alreadyVisible =
        runCatching { onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed() }.isSuccess
    if (alreadyVisible) return

    // 3) Fallback: manual swipes a few times (helps when semantics are quirky but visual scroll
    // still works).
    repeat(5) {
      onNodeWithTag(AddItemScreenTestTags.ALL_FIELDS).performTouchInput {
        swipeUp(startY = bottom, endY = top)
      }
      waitForIdle()
      if (runCatching { onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed() }.isSuccess)
          return
    }
  }

  // UI check for the button upload photo
  fun ComposeTestRule.enterAddItemPhoto() {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed()
  }

  fun ComposeTestRule.checkImageUploadButtonIsDisplayed() {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed()
  }

  fun ComposeTestRule.checkImageUploadButtonClickable() {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.checkPhotoPreviewDisplayed() {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PREVIEW).assertIsDisplayed()
  }

  fun ComposeTestRule.enterAddItemType(type: String) {
    ensureVisible(AddItemScreenTestTags.INPUT_TYPE)
    onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE, useUnmergedTree = true).performTextInput(type)
  }

  fun ComposeTestRule.enterAddItemCategory(category: String) {
    ensureVisible(AddItemScreenTestTags.INPUT_CATEGORY)
    onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY, useUnmergedTree = true)
        .performTextInput(category)
  }

  fun ComposeTestRule.enterAddItemBrand(brand: String) {
    ensureVisible(AddItemScreenTestTags.INPUT_BRAND)
    onNodeWithTag(AddItemScreenTestTags.INPUT_BRAND, useUnmergedTree = true).performTextInput(brand)
  }

  fun ComposeTestRule.enterAddItemPrice(price: Double) {
    ensureVisible(AddItemScreenTestTags.INPUT_PRICE)
    onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE, useUnmergedTree = true)
        .performTextInput(price.toString())
  }

  fun ComposeTestRule.enterAddItemLink(link: String) {
    ensureVisible(AddItemScreenTestTags.INPUT_LINK)

    onNodeWithTag(AddItemScreenTestTags.INPUT_LINK, useUnmergedTree = true).performTextInput(link)
  }

  fun ComposeTestRule.enterAddItemMaterial(material: String) {

    ensureVisible(AddItemScreenTestTags.INPUT_MATERIAL)

    onNodeWithTag(AddItemScreenTestTags.INPUT_MATERIAL, useUnmergedTree = true)
        .performTextInput(material)
  }

  /**
   * Waits until a node with the given test tag exists in the semantics tree. Prevents race
   * conditions when the UI is still composing.
   */
  fun ComposeTestRule.waitForNodeWithTag(
      tag: String,
      timeoutMillis: Long = 5_000,
      useUnmergedTree: Boolean = true
  ) {
    waitUntil(timeoutMillis) {
      onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
    }
  }

  fun ComposeTestRule.enterAddItemDetails(item: Item, viewModel: AddItemsViewModel, testUri: Uri) {
    item.type?.let { enterAddItemType(it) }
    enterAddItemCategory(item.category)
    item.brand?.let { enterAddItemBrand(it) }
    item.price?.let { enterAddItemPrice(it) }
    item.link?.let { enterAddItemLink(it) }

    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER, useUnmergedTree = true).performClick()

    waitForIdle()
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG).assertIsDisplayed()
    onNodeWithTag(AddItemScreenTestTags.PICK_FROM_GALLERY).performClick()

    runOnIdle { viewModel.setPhoto(testUri) }
    waitForIdle()

    onAllNodesWithContentDescription("Placeholder icon").assertCountEquals(0)
    // onNodeWithContentDescription("Selected photo").assertExists().assertIsDisplayed()

    item.material.forEach {
      it?.let { material -> enterAddItemMaterial("${material.name} ${material.percentage}%") }
    }
  }

  fun ComposeTestRule.verifyImageUploadFlow(viewModel: AddItemsViewModel, uri: Uri) {
    // Step 1: Verify placeholder icon visible initially
    onNodeWithContentDescription("Placeholder icon").assertExists().assertIsDisplayed()

    // Step 2: Simulate user tapping "Upload a picture"
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).performClick()
    waitForIdle()

    // Step 3: Dialog should appear
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG).assertIsDisplayed()

    // Step 4: Simulate selecting "Choose from Gallery"
    onNodeWithTag(AddItemScreenTestTags.PICK_FROM_GALLERY).performClick()

    // Step 5: Simulate picking a photo (what the gallery launcher would do)
    runOnIdle { viewModel.setPhoto(uri) }

    // Step 6: Wait for recomposition
    waitForIdle()

    // Step 7: Check placeholder disappears
    onAllNodesWithContentDescription("Placeholder icon").assertCountEquals(0)

    // Step 8: Verify selected image preview is now displayed
    // onNodeWithContentDescription("Selected photo").assertExists().assertIsDisplayed()
  }

  fun Item.isEqual(other: Item): Boolean {
    return this.category == other.category &&
        this.type == other.type &&
        this.brand == other.brand &&
        this.price == other.price &&
        this.link == other.link &&
        this.material.size == other.material.size &&
        this.material.zip(other.material).all { (m1, m2) ->
          m1?.name == m2?.name && m1?.percentage == m2?.percentage
        }
  }

  companion object {
    val item1 =
        Item(
            itemUuid = "0",
            image = ImageData("0", "https://example.com/image1.jpg"),
            category = "Clothing",
            type = "t-shirt",
            brand = "Mango",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item1")

    val item2 =
        Item(
            itemUuid = "1",
            image = ImageData("2", "https://example.com/image1.jpg"),
            category = "shoes",
            type = "high heels",
            brand = "Zara",
            price = 30.0,
            material = listOf(),
            link = "https://example.com/item2")

    val item3 =
        Item(
            itemUuid = "2",
            image = ImageData("3", "https://example.com/image1.jpg"),
            category = "bags",
            type = "handbag",
            brand = "Vakko",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item3")

    val item4 =
        Item(
            itemUuid = "4",
            image = ImageData("4", "https://example.com/image4.jpg"),
            category = "Clothing",
            type = "jacket",
            brand = "Mango",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item4")
  }
}
