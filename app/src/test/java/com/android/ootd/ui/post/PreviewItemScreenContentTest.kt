package com.android.ootd.ui.post

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewItemScreenContentTest {

  @get:Rule val composeRule = createComposeRule()

  @OptIn(ExperimentalMaterial3Api::class)
  private fun setContentWithState(
      uiState: PreviewUIState,
      onEditItem: (String) -> Unit = {},
      onRemoveItem: (String) -> Unit = {},
      onAddItem: (String) -> Unit = {},
      onSelectFromInventory: (String) -> Unit = {},
      onPublish: () -> Unit = {},
      onGoBack: (String) -> Unit = {},
      enablePreview: Boolean = true,
      onTogglePublic: (Boolean) -> Unit = {}
  ) {
    composeRule.setContent {
      OOTDTheme {
        PreviewItemScreenContent(
            ui = uiState,
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
            onEditItem = onEditItem,
            onRemoveItem = onRemoveItem,
            onAddItem = onAddItem,
            onSelectFromInventory = onSelectFromInventory,
            onPublish = onPublish,
            onGoBack = onGoBack,
            enablePreview = enablePreview,
            onTogglePublic = onTogglePublic)
      }
    }
  }

  private fun createEmptyState() =
      PreviewUIState(
          postUuid = "post-1",
          imageUri = "file:///preview.png",
          description = "Test outfit",
          items = emptyList(),
          isLoading = false,
          isPublished = false)

  @Test
  fun postButton_withoutItems_showsMissingItemsWarning() {
    setContentWithState(createEmptyState())
    composeRule.onNodeWithTag(PreviewItemScreenTestTags.POST_BUTTON).performClick()

    composeRule.onNodeWithTag(PreviewItemScreenTestTags.MISSING_ITEMS_WARNING).assertIsDisplayed()
    composeRule
        .onNodeWithText("Please add at least one item before posting your outfit.")
        .assertIsDisplayed()
  }
}
