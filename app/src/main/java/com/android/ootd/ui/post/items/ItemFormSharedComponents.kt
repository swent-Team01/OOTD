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
import androidx.compose.material3.MaterialTheme
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

@Composable
fun AdditionalDetailsSection(
    condition: String,
    onConditionChange: (String) -> Unit,
    material: String,
    onMaterialChange: (String) -> Unit,
    fitType: String,
    onFitTypeChange: (String) -> Unit,
    style: String,
    onStyleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    tags: AdditionalDetailsTags,
    expandedInitially: Boolean = false,
    condExpandedInitially: Boolean = false,
    title: String = "Enter additional details"
) {
  var expanded by remember { mutableStateOf(expandedInitially) }
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
              tint = MaterialTheme.colorScheme.primary)
          Text(
              text = title,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.primary))
        }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()) {
          Column(modifier = Modifier.fillMaxWidth().testTag(tags.section)) {
            ConditionDropdown(
                condition = condition,
                onConditionChange = onConditionChange,
                testTag = tags.conditionField,
                expandedInitially = condExpandedInitially)
            MaterialField(
                materialText = material, onChange = onMaterialChange, testTag = tags.materialField)
            FitTypeField(
                fitType = fitType,
                onChange = onFitTypeChange,
                testTag = tags.fitTypeField,
                dropdownTestTag = tags.fitTypeDropdown)
            StyleField(
                style = style,
                onChange = onStyleChange,
                testTag = tags.styleField,
                dropdownTestTag = tags.styleDropdown)
            NotesField(notes = notes, onChange = onNotesChange, testTag = tags.notesField)
          }
        }
  }
}

@Composable
fun ItemFieldsListLayout(
    modifier: Modifier,
    horizontalAlignment: Alignment.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    imagePickerContent: @Composable () -> Unit,
    categoryFieldContent: @Composable () -> Unit,
    typeFieldContent: @Composable () -> Unit,
    primaryFieldsContent: @Composable () -> Unit,
    additionalDetailsContent: @Composable () -> Unit,
    actionButtonsContent: @Composable () -> Unit,
    bottomSpacer: Dp = 100.dp
) {
  LazyColumn(
      modifier = modifier,
      horizontalAlignment = horizontalAlignment,
      verticalArrangement = verticalArrangement) {
        item { imagePickerContent() }
        item { categoryFieldContent() }
        item { typeFieldContent() }
        item { primaryFieldsContent() }
        item { additionalDetailsContent() }
        item { actionButtonsContent() }
        item { Spacer(modifier = Modifier.height(bottomSpacer)) }
      }
}
