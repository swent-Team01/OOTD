package com.android.ootd.ui.post.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Typography

data class AdditionalDetailsTags(
    val toggle: String,
    val section: String,
    val conditionField: String,
    val materialField: String,
    val fitTypeField: String,
    val fitTypeDropdown: String,
    val styleField: String,
    val styleDropdown: String,
    val notesField: String,
)

data class AdditionalDetailsState(
    val condition: String,
    val onConditionChange: (String) -> Unit,
    val material: String,
    val onMaterialChange: (String) -> Unit,
    val fitType: String,
    val onFitTypeChange: (String) -> Unit,
    val style: String,
    val onStyleChange: (String) -> Unit,
    val notes: String,
    val onNotesChange: (String) -> Unit,
    val expandedInitially: Boolean = false,
    val condExpandedInitially: Boolean = false
)

@Composable
fun AdditionalDetailsSection(
    state: AdditionalDetailsState,
    tags: AdditionalDetailsTags,
    title: String = "Enter additional details"
) {
  var expanded by remember { mutableStateOf(state.expandedInitially) }
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .testTag(tags.toggle)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          androidx.compose.material3.Icon(
              imageVector =
                  if (expanded) Icons.Filled.KeyboardArrowDown
                  else Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = if (expanded) "Collapse" else "Expand",
              tint = Primary)
          Text(text = title, style = Typography.bodyLarge.copy(color = Primary))
        }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()) {
          Column(modifier = Modifier.fillMaxWidth().testTag(tags.section)) {
            ConditionDropdown(
                condition = state.condition,
                onConditionChange = state.onConditionChange,
                testTag = tags.conditionField,
                expandedInitially = state.condExpandedInitially)
            MaterialField(
                materialText = state.material,
                onChange = state.onMaterialChange,
                testTag = tags.materialField)
            FitTypeField(
                fitType = state.fitType,
                onChange = state.onFitTypeChange,
                testTag = tags.fitTypeField,
                dropdownTestTag = tags.fitTypeDropdown)
            StyleField(
                style = state.style,
                onChange = state.onStyleChange,
                testTag = tags.styleField,
                dropdownTestTag = tags.styleDropdown)
            NotesField(
                notes = state.notes, onChange = state.onNotesChange, testTag = tags.notesField)
          }
        }
  }
}

data class ItemFieldsLayoutConfig(
    val horizontalAlignment: Alignment.Horizontal,
    val verticalArrangement: Arrangement.Vertical,
    val bottomSpacer: Dp = 100.dp
)

data class ItemFieldsListSlots(
    val imagePicker: @Composable () -> Unit,
    val categoryField: @Composable () -> Unit,
    val typeField: @Composable () -> Unit,
    val primaryFields: @Composable () -> Unit,
    val additionalDetails: @Composable () -> Unit,
    val actionButtons: @Composable () -> Unit
)

@Composable
fun ItemFieldsListLayout(
    modifier: Modifier,
    layoutConfig: ItemFieldsLayoutConfig,
    slots: ItemFieldsListSlots
) {
  LazyColumn(
      modifier = modifier,
      horizontalAlignment = layoutConfig.horizontalAlignment,
      verticalArrangement = layoutConfig.verticalArrangement) {
        item { slots.imagePicker() }
        item { slots.categoryField() }
        item { slots.typeField() }
        item { slots.primaryFields() }
        item { slots.additionalDetails() }
        item { slots.actionButtons() }
        item { Spacer(modifier = Modifier.height(layoutConfig.bottomSpacer)) }
      }
}
