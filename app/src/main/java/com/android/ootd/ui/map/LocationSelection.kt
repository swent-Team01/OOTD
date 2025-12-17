package com.android.ootd.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.android.ootd.model.map.Location
import com.android.ootd.ui.theme.NotoSans
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.utils.composables.CommonTextField

// Test tags for location selection UI
object LocationSelectionTestTags {
  const val INPUT_LOCATION = "inputLocation"
  const val LOCATION_SUGGESTION = "locationSuggestion"
  const val LOCATION_MORE = "locationMore"
  const val LOCATION_GPS_BUTTON = "locationGpsButton"
  const val LOCATION_CLEAR_BUTTON = "locationClearButton"
}

/** GPS Button component for location selection */
@Composable
private fun GPSButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
  OutlinedButton(
      onClick = onClick,
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 8.dp)
              .testTag(LocationSelectionTestTags.LOCATION_GPS_BUTTON),
      colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
      enabled = !isLoading) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "GPS",
            modifier = Modifier.padding(end = 8.dp))
        Text(text, fontFamily = NotoSans)
      }
}

/** Trailing icon for location input field (loading, clear, or none) */
@Composable
private fun LocationInputTrailingIcon(
    isLoadingLocation: Boolean,
    locationQuery: String,
    onClear: () -> Unit
) {
  when {
    isLoadingLocation -> {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
    }
    locationQuery.isNotEmpty() -> {
      IconButton(
          onClick = onClear,
          modifier = Modifier.testTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON)) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear location",
                tint = Primary)
          }
    }
  }
}

/** Dropdown menu showing location suggestions */
@Composable
private fun LocationSuggestionsDropdown(
    showDropdown: Boolean,
    suggestions: List<Location>,
    isFocused: Boolean,
    onDismiss: () -> Unit,
    onLocationSelect: (Location) -> Unit,
    onClearSuggestions: () -> Unit
) {
  DropdownMenu(
      expanded = showDropdown && suggestions.isNotEmpty() && isFocused,
      onDismissRequest = onDismiss,
      properties = PopupProperties(focusable = false),
      modifier = Modifier.fillMaxWidth()) {
        suggestions.take(3).forEach { location ->
          DropdownMenuItem(
              text = {
                Text(
                    text = location.name.take(30) + if (location.name.length > 30) "..." else "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = NotoSans)
              },
              onClick = {
                onLocationSelect(location)
                onClearSuggestions()
                onDismiss()
              },
              modifier =
                  Modifier.padding(8.dp).testTag(LocationSelectionTestTags.LOCATION_SUGGESTION))
        }

        if (suggestions.size > 3) {
          DropdownMenuItem(
              text = { Text("More...", fontFamily = NotoSans) },
              onClick = { /* Nothing */ },
              modifier = Modifier.padding(8.dp).testTag(LocationSelectionTestTags.LOCATION_MORE))
        }
      }
}

/**
 * Location selection UI with a GPS button, text input and suggestion dropdown.
 *
 * @param viewModel the LocationSelectionViewModel managing location state and actions
 * @param textGPSButton label for the GPS button
 * @param textLocationField label for the location input field
 * @param onLocationSelect callback invoked when a suggestion is selected (optional, for custom
 *   handling)
 * @param onGPSClick callback invoked when the GPS button is clicked
 * @param modifier optional Compose modifier for the section
 * @param isError whether the input should show an error state
 * @param onFocusChanged callback invoked when the input focus changes
 */
@Composable
fun LocationSelectionSection(
    modifier: Modifier = Modifier,
    viewModel: LocationSelectionViewModel,
    textGPSButton: String,
    textLocationField: String,
    onLocationSelect: ((Location) -> Unit)? = null,
    onGPSClick: () -> Unit,
    isError: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {}
) {
  val locationUiState by viewModel.uiState.collectAsState()

  Column(modifier = modifier) {
    var showDropdown by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Automatically show dropdown when suggestions are available and field is focused
    LaunchedEffect(locationUiState.locationSuggestions, isFocused) {
      showDropdown = isFocused && locationUiState.locationSuggestions.isNotEmpty()
    }

    // GPS Button
    GPSButton(
        text = textGPSButton, isLoading = locationUiState.isLoadingLocations, onClick = onGPSClick)

    // Manual Input Field with dropdown
    Box {
      CommonTextField(
          value = locationUiState.locationQuery,
          onChange = {
            viewModel.setLocationQuery(it)
            if (isFocused) {
              showDropdown = true
            }
          },
          label = textLocationField,
          placeholder = "Or enter address manually",
          trailingIcon = {
            LocationInputTrailingIcon(
                isLoadingLocation = locationUiState.isLoadingLocations,
                locationQuery = locationUiState.locationQuery,
                onClear = {
                  viewModel.setLocationQuery("")
                  viewModel.clearLocationSuggestions()
                })
          },
          modifier =
              Modifier.fillMaxWidth()
                  .testTag(LocationSelectionTestTags.INPUT_LOCATION)
                  .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    onFocusChanged(focusState.isFocused)

                    if (focusState.isFocused &&
                        !wasFocused &&
                        locationUiState.locationSuggestions.isNotEmpty()) {
                      showDropdown = true
                    }
                    if (!focusState.isFocused && wasFocused) {
                      showDropdown = false
                    }
                  },
          singleLine = true,
          isError = isError,
          readOnly = locationUiState.selectedLocation?.name?.isNotEmpty() == true)

      // Dropdown menu for location suggestions
      LocationSuggestionsDropdown(
          showDropdown = showDropdown,
          suggestions = locationUiState.locationSuggestions,
          isFocused = isFocused,
          onDismiss = { showDropdown = false },
          onLocationSelect = { location ->
            viewModel.setLocation(location)
            onLocationSelect?.invoke(location)
          },
          onClearSuggestions = viewModel::clearLocationSuggestions)
    }
  }
}

/** Preview of [LocationSelectionSection] with sample data. */
@Suppress("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
private fun LocationSelectionSectionPreview() {
  OOTDTheme {
    LocationSelectionSection(
        viewModel = LocationSelectionViewModel(),
        textGPSButton = "Use GPS Location",
        textLocationField = "Enter Location",
        onGPSClick = {},
        onLocationSelect = {})
  }
}
