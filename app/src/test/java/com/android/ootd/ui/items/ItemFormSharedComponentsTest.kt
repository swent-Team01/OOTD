package com.android.ootd.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.post.items.AdditionalDetailsSection
import com.android.ootd.ui.post.items.AdditionalDetailsState
import com.android.ootd.ui.post.items.AdditionalDetailsTags
import com.android.ootd.ui.post.items.BrandField
import com.android.ootd.ui.post.items.ItemFieldsLayoutConfig
import com.android.ootd.ui.post.items.ItemFieldsListLayout
import com.android.ootd.ui.post.items.ItemFieldsListSlots
import com.android.ootd.ui.post.items.NotesField
import kotlin.test.assertEquals
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

  @Test
  fun brandField_enforcesCharacterLimit() {
    val fieldTag = "brandField"
    var brand by mutableStateOf("")
    composeTestRule.setContent {
      BrandField(brand = brand, onChange = { brand = it }, testTag = fieldTag)
    }

    val expected = "B".repeat(40)
    repeat(50) { composeTestRule.onNodeWithTag(fieldTag).performTextInput("B") }
    composeTestRule.runOnIdle { assertEquals(expected, brand) }
  }

  @Test
  fun brandField_counterVisibleOnlyWhenFocused() {
    val fieldTag = "brandField"
    composeTestRule.setContent { BrandField(brand = "", onChange = {}, testTag = fieldTag) }

    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasText("${0}/${40}"), useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun notesField_enforcesCharacterLimit() {
    val fieldTag = "notesField"
    var notes by mutableStateOf("")
    composeTestRule.setContent {
      NotesField(notes = notes, onChange = { notes = it }, testTag = fieldTag)
    }

    val expected = "N".repeat(250)
    repeat(300) { composeTestRule.onNodeWithTag(fieldTag).performTextInput("N") }
    composeTestRule.runOnIdle { assertEquals(expected, notes) }
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
