package com.android.ootd.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.epflLocation
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.LightColorScheme
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography

// Test tags for location selection UI
object LocationSelectionTestTags {
  const val INPUT_LOCATION = "inputLocation"
  const val LOCATION_SUGGESTION = "locationSuggestion"
  const val LOCATION_MORE = "locationMore"
  const val LOCATION_GPS_BUTTON = "locationGpsButton"
  const val LOCATION_CLEAR_BUTTON = "locationClearButton"
  const val LOCATION_DEFAULT_EPFL = "locationDefaultEpfl"
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
        Text(text, fontFamily = Bodoni)
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
                    fontFamily = Bodoni)
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
              text = { Text("More...", fontFamily = Bodoni) },
              onClick = { /* Nothing */ },
              modifier = Modifier.padding(8.dp).testTag(LocationSelectionTestTags.LOCATION_MORE))
        }
      }
}

/** Default EPFL location selector text */
@Composable
private fun DefaultLocationSelector(onSelectDefault: () -> Unit) {
  val colors = LightColorScheme
  val typography = Typography

  Text(
      text = "or select default location (EPFL)",
      color = Primary,
      style =
          typography.bodyMedium.copy(
              fontFamily = Bodoni, textDecoration = TextDecoration.Underline),
      modifier =
          Modifier.padding(top = 8.dp, bottom = 4.dp)
              .clickable { onSelectDefault() }
              .testTag(LocationSelectionTestTags.LOCATION_DEFAULT_EPFL))
}

/**
 * Location selection UI with a GPS button, text input and suggestion dropdown.
 *
 * @param textGPSButton label for the GPS button
 * @param textLocationField label for the location input field
 * @param locationQuery current text value of the location input
 * @param selectedLocation currently selected Location, or null
 * @param suggestions list of location suggestions shown in the dropdown
 * @param isLoadingLocation whether a location lookup is in progress
 * @param onLocationQueryChange callback invoked when the input text changes
 * @param onLocationSelect callback invoked when a suggestion is selected
 * @param onGPSClick callback invoked when the GPS button is clicked
 * @param onClearSuggestions callback to clear the suggestion list
 * @param modifier optional Compose modifier for the section
 * @param textColor color used for labels and placeholders
 * @param isError whether the input should show an error state
 * @param onFocusChanged callback invoked when the input focus changes
 */
@Composable
fun LocationSelectionSection(
    textGPSButton: String,
    textLocationField: String,
    locationQuery: String,
    selectedLocation: Location?,
    suggestions: List<Location>,
    isLoadingLocation: Boolean,
    onLocationQueryChange: (String) -> Unit,
    onLocationSelect: (Location) -> Unit,
    onGPSClick: () -> Unit,
    onClearSuggestions: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Tertiary,
    isError: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {}
) {
  val colors = LightColorScheme
  val typography = Typography

  Column(modifier = modifier) {
    var showDropdown by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Automatically show dropdown when suggestions are available and field is focused
    LaunchedEffect(suggestions, isFocused) { showDropdown = isFocused && suggestions.isNotEmpty() }

    // GPS Button
    GPSButton(text = textGPSButton, isLoading = isLoadingLocation, onClick = onGPSClick)

    // Manual Input Field with dropdown
    Box {
      OutlinedTextField(
          value = locationQuery,
          onValueChange = {
            onLocationQueryChange(it)
            if (isFocused) {
              showDropdown = true
            }
          },
          textStyle = typography.bodyLarge.copy(fontFamily = Bodoni, color = colors.primary),
          label = {
            Box(
                modifier =
                    Modifier.background(colors.secondary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                  Text(
                      text = textLocationField,
                      style = typography.bodySmall.copy(fontFamily = Bodoni),
                      color = colors.tertiary)
                }
          },
          placeholder = {
            Text("Or enter address manually", color = textColor, fontFamily = Bodoni)
          },
          colors =
              OutlinedTextFieldDefaults.colors(
                  focusedTextColor = colors.primary,
                  unfocusedTextColor = colors.primary,
                  cursorColor = colors.primary),
          trailingIcon = {
            LocationInputTrailingIcon(
                isLoadingLocation = isLoadingLocation,
                locationQuery = locationQuery,
                onClear = {
                  onLocationQueryChange("")
                  onClearSuggestions()
                })
          },
          modifier =
              Modifier.fillMaxWidth()
                  .testTag(LocationSelectionTestTags.INPUT_LOCATION)
                  .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    onFocusChanged(focusState.isFocused)

                    if (focusState.isFocused && !wasFocused && suggestions.isNotEmpty()) {
                      showDropdown = true
                    }
                    if (!focusState.isFocused && wasFocused) {
                      showDropdown = false
                    }
                  },
          singleLine = true,
          isError = isError,
          readOnly = selectedLocation != null && selectedLocation.name.isNotEmpty())

      // Dropdown menu for location suggestions
      LocationSuggestionsDropdown(
          showDropdown = showDropdown,
          suggestions = suggestions,
          isFocused = isFocused,
          onDismiss = { showDropdown = false },
          onLocationSelect = onLocationSelect,
          onClearSuggestions = onClearSuggestions)
    }

    // Default EPFL location selector
    DefaultLocationSelector(
        onSelectDefault = {
          onLocationQueryChange(epflLocation.name)
          onLocationSelect(epflLocation)
          onClearSuggestions()
        })
  }
}

/** Preview of [LocationSelectionSection] with sample data. */
@Preview(showBackground = true)
@Composable
private fun LocationSelectionSectionPreview() {
  OOTDTheme {
    LocationSelectionSection(
        textGPSButton = "Use GPS Location",
        textLocationField = "Enter Location",
        locationQuery = "Zurich",
        selectedLocation = Location(47.3769, 8.5417, "Zürich, Switzerland"),
        suggestions =
            listOf(
                Location(47.3769, 8.5417, "Zürich, Switzerland"),
                Location(46.2044, 6.1432, "Lausanne, Switzerland"),
                Location(46.5197, 6.6323, "Geneva, Switzerland")),
        isLoadingLocation = false,
        onLocationQueryChange = {},
        onLocationSelect = {},
        onGPSClick = {},
        onClearSuggestions = {})
  }
}
