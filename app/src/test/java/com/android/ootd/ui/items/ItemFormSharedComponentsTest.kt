package com.android.ootd.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.post.items.AdditionalDetailsSection
import com.android.ootd.ui.post.items.AdditionalDetailsState
import com.android.ootd.ui.post.items.AdditionalDetailsTags
import com.android.ootd.ui.post.items.ItemFieldsLayoutConfig
import com.android.ootd.ui.post.items.ItemFieldsListLayout
import com.android.ootd.ui.post.items.ItemFieldsListSlots
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemFormSharedComponentsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val tags =
      AdditionalDetailsTags(
          toggle = "detailsToggle",
          section = "detailsSection",
          conditionField = "conditionField",
          materialField = "materialField",
          fitTypeField = "fitTypeField",
          fitTypeDropdown = "fitTypeDropdown",
          styleField = "styleField",
          styleDropdown = "styleDropdown",
          notesField = "notesField")

  @Test
  fun additionalDetailsSection_collapsesByDefault_andExpandsOnToggle() {
    composeTestRule.setContent { AdditionalDetailsSection(state = sampleState(), tags = tags) }

    composeTestRule.onAllNodesWithTag(tags.section, useUnmergedTree = true).assertCountEquals(0)
    composeTestRule.onNodeWithTag(tags.toggle).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(tags.section, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tags.conditionField).assertIsDisplayed()
  }

  @Test
  fun additionalDetailsSection_respectsInitialExpansionFlag() {
    composeTestRule.setContent {
      AdditionalDetailsSection(
          state = sampleState(expandedInitially = true, condExpandedInitially = true), tags = tags)
    }

    composeTestRule.onNodeWithTag(tags.conditionField).assertIsDisplayed()
  }

  @Test
  fun itemFieldsListLayout_rendersAllSlots() {
    composeTestRule.setContent {
      ItemFieldsListLayout(
          modifier = Modifier,
          layoutConfig =
              ItemFieldsLayoutConfig(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  bottomSpacer = 0.dp),
          slots =
              ItemFieldsListSlots(
                  imagePicker = { Box(Modifier.size(1.dp).testTag("imageSlot")) },
                  categoryField = { Box(Modifier.size(1.dp).testTag("categorySlot")) },
                  typeField = { Box(Modifier.size(1.dp).testTag("typeSlot")) },
                  primaryFields = { Box(Modifier.size(1.dp).testTag("primarySlot")) },
                  additionalDetails = { Box(Modifier.size(1.dp).testTag("detailsSlot")) },
                  actionButtons = { Box(Modifier.size(1.dp).testTag("actionsSlot")) }))
    }

    composeTestRule.onNodeWithTag("imageSlot").assertIsDisplayed()
    composeTestRule.onNodeWithTag("categorySlot").assertIsDisplayed()
    composeTestRule.onNodeWithTag("typeSlot").assertIsDisplayed()
    composeTestRule.onNodeWithTag("primarySlot").assertIsDisplayed()
    composeTestRule.onNodeWithTag("detailsSlot").assertIsDisplayed()
    composeTestRule.onNodeWithTag("actionsSlot").assertIsDisplayed()
  }

  private fun sampleState(
      expandedInitially: Boolean = false,
      condExpandedInitially: Boolean = false
  ) =
      AdditionalDetailsState(
          condition = "Sample",
          onConditionChange = {},
          material = "Cotton",
          onMaterialChange = {},
          fitType = "Regular",
          onFitTypeChange = {},
          style = "Casual",
          onStyleChange = {},
          notes = "Notes",
          onNotesChange = {},
          expandedInitially = expandedInitially,
          condExpandedInitially = condExpandedInitially)
}
