package com.android.ootd.ui.items

import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Material
import com.android.ootd.ui.post.items.AddItemsUIState
import com.android.ootd.ui.post.items.AddItemsViewModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AddItemsViewModelTest {

  private val viewModel =
      AddItemsViewModel(
          repository = FakeItemsRepository(),
          accountRepository = FakeAccountRepository(),
          overridePhoto = true)

  @Test
  fun materialParsing_handlesMultipleValidAndInvalidEntries() {
    viewModel.setMaterial("Cotton 60%, Polyester 30%, Elastane 10%, InvalidEntry")
    val parsed = viewModel.uiState.value.material

    assertEquals(3, parsed.size)
    assertEquals(Material("Cotton", 60.0), parsed[0])
    assertEquals(Material("Polyester", 30.0), parsed[1])
    assertEquals(Material("Elastane", 10.0), parsed[2])

    viewModel.setMaterial("Wool 100%")
    val single = viewModel.uiState.value.material
    assertEquals(1, single.size)
    assertEquals(Material("Wool", 100.0), single[0])
  }

  @Test
  fun isAddingValid_reflectsRequiredFields() {
    val emptyState = AddItemsUIState()
    assertFalse(emptyState.isAddingValid)

    val withImage = emptyState.copy(image = ImageData(imageId = "img", imageUrl = "remote"))
    assertFalse(withImage.isAddingValid) // missing category

    val valid = withImage.copy(category = "Clothing")
    assertTrue(valid.isAddingValid)

    val invalidCategory = valid.copy(invalidCategory = "error")
    assertFalse(invalidCategory.isAddingValid)
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
