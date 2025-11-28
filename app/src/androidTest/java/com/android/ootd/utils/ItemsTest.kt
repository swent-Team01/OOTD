package com.android.ootd.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.ui.post.items.AddItemScreenTestTags
import org.junit.Before

// Test partially generated with an AI coding agent
interface ItemsTest {

  fun createInitializedRepository(): ItemsRepository

  val repository: ItemsRepository

  @Before
  fun setUp() {
    ItemsRepositoryProvider.setRepository(createInitializedRepository())
  }

  fun ComposeTestRule.ensureVisible(tag: String) {
    onNodeWithTag(AddItemScreenTestTags.ALL_FIELDS).performScrollToNode(hasTestTag(tag))
    onNodeWithTag(tag, useUnmergedTree = true).assertExists()
  }

  fun ComposeTestRule.enterAddItemType(type: String) {
    ensureVisible(AddItemScreenTestTags.INPUT_TYPE)
    onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE, useUnmergedTree = true).performTextInput(type)
  }

  fun ComposeTestRule.enterAddItemCategory(category: String) {
    ensureVisible(AddItemScreenTestTags.INPUT_CATEGORY)
    // Click the dropdown field to open it
    onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY, useUnmergedTree = true).performClick()
    waitForIdle()
    // Click the category option in the dropdown
    onNodeWithText(category, useUnmergedTree = true).performClick()
    waitForIdle()
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
    val materialVisible =
        runCatching {
              onNodeWithTag(AddItemScreenTestTags.INPUT_MATERIAL, useUnmergedTree = true)
                  .assertIsDisplayed()
            }
            .isSuccess

    if (!materialVisible) {
      ensureVisible(AddItemScreenTestTags.ADDITIONAL_DETAILS_TOGGLE)
      onNodeWithTag(AddItemScreenTestTags.ADDITIONAL_DETAILS_TOGGLE, useUnmergedTree = true)
          .performClick()

      ensureVisible(AddItemScreenTestTags.INPUT_MATERIAL)
    }

    onNodeWithTag(AddItemScreenTestTags.INPUT_MATERIAL, useUnmergedTree = true)
        .performTextReplacement(material)
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
            postUuids = listOf("post_123"),
            image = ImageData("0", "https://example.com/image1.jpg"),
            category = "Clothing",
            type = "t-shirt",
            brand = "Mango",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item1",
            ownerId = "user123")

    val item2 =
        Item(
            itemUuid = "1",
            postUuids = listOf("post_123"),
            image = ImageData("2", "https://example.com/image1.jpg"),
            category = "shoes",
            type = "high heels",
            brand = "Zara",
            price = 30.0,
            material = listOf(),
            link = "https://example.com/item2",
            ownerId = "user123")

    val item3 =
        Item(
            itemUuid = "2",
            postUuids = listOf("post_123"),
            image = ImageData("3", "https://example.com/image1.jpg"),
            category = "bags",
            type = "handbag",
            brand = "Vakko",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item3",
            ownerId = "user123")

    val item4 =
        Item(
            itemUuid = "4",
            postUuids = listOf("post_123"),
            image = ImageData("4", "https://example.com/image4.jpg"),
            category = "Clothing",
            type = "jacket",
            brand = "Mango",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item4",
            ownerId = "user123")
  }
}
