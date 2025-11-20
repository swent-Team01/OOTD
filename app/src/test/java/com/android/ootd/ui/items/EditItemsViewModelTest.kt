package com.android.ootd.ui.items

import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.Material
import com.android.ootd.ui.post.items.EditItemsUIState
import com.android.ootd.ui.post.items.EditItemsViewModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class EditItemsViewModelTest {

  private val viewModel =
      EditItemsViewModel(
          repository = FakeItemsRepository(), accountRepository = FakeAccountRepository())

  @Test
  fun loadItem_populatesStateFields() {
    val item =
        Item(
            itemUuid = "item-1",
            postUuids = listOf("post"),
            image = ImageData("img", "https://example.com/image.jpg"),
            category = "Clothing",
            type = "T-shirt",
            brand = "Brand",
            price = 49.99,
            currency = "USD",
            material = listOf(Material("Cotton", 80.0), Material("Polyester", 20.0)),
            link = "https://example.com/tshirt",
            ownerId = "owner",
            condition = "New",
            size = "M",
            fitType = "Regular",
            style = "Casual",
            notes = "Handmade")

    viewModel.loadItem(item)
    val state = viewModel.uiState.value

    assertEquals(item.itemUuid, state.itemId)
    assertEquals(item.category, state.category)
    assertEquals(item.type, state.type)
    assertEquals(item.brand, state.brand)
    assertEquals(item.price, state.price)
    assertEquals(item.currency, state.currency)
    assertEquals(item.link, state.link)
    assertEquals(item.material.size, state.material.size)
    assertEquals("Cotton 80.0%, Polyester 20.0%", state.materialText)
    assertEquals("New", state.condition)
    assertEquals("M", state.size)
    assertEquals("Regular", state.fitType)
    assertEquals("Casual", state.style)
    assertEquals("Handmade", state.notes)
  }

  @Test
  fun isEditValid_requiresPhotoAndCategory() {
    val emptyState = EditItemsUIState()
    assertFalse(emptyState.isEditValid)

    val withImage = emptyState.copy(image = ImageData("img", "url"))
    assertFalse(withImage.isEditValid) // category missing

    val valid = withImage.copy(category = "Clothing")
    assertTrue(valid.isEditValid)

    val invalidCategory = valid.copy(invalidCategory = "Please select a category.")
    assertFalse(invalidCategory.isEditValid)
  }

  @Test
  fun setFitType_updatesUiState() {
    viewModel.setFitType("Slim")
    assertEquals("Slim", viewModel.uiState.value.fitType)
  }

  @Test
  fun setStyle_updatesUiState() {
    viewModel.setStyle("Streetwear")
    assertEquals("Streetwear", viewModel.uiState.value.style)
  }
}
