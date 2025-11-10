package com.android.ootd.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ootd.model.map.Location
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary

// Test tags for location selection UI
object LocationSelectionTestTags {
  const val INPUT_LOCATION = "inputLocation"
  const val LOCATION_SUGGESTION = "locationSuggestion"
  const val LOCATION_MORE = "locationMore"
  const val LOCATION_GPS_BUTTON = "locationGpsButton"
}

/**
 * Location selection UI with a GPS button, text input and suggestion dropdown.
 *
 * @param textGPSbutton label for the GPS button
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
    textGPSbutton: String,
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
  Column(modifier = modifier) {
    // Local state to control dropdown visibility
    var showDropdown by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Automatically show dropdown when suggestions are available and field is focused
    LaunchedEffect(suggestions, isFocused) {
      if (isFocused && suggestions.isNotEmpty()) {
        showDropdown = true
      } else if (suggestions.isEmpty()) {
        showDropdown = false
      }
    }

    // GPS Button
    OutlinedButton(
        onClick = onGPSClick,
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag(LocationSelectionTestTags.LOCATION_GPS_BUTTON),
        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
        enabled = !isLoadingLocation) {
          Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "GPS",
              modifier = Modifier.padding(end = 8.dp))
          Text(textGPSbutton, fontFamily = Bodoni)
        }

    // Manual Input Field with custom dropdown
    Box {
      OutlinedTextField(
          value = locationQuery,
          onValueChange = {
            onLocationQueryChange(it)
            // Ensure dropdown is shown when user types and we're focused
            if (isFocused) {
              showDropdown = true
            }
          },
          textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Bodoni),
          label = { Text(textLocationField, color = textColor, fontFamily = Bodoni) },
          placeholder = {
            Text("Or enter address manually", color = textColor, fontFamily = Bodoni)
          },
          trailingIcon = {
            if (isLoadingLocation) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
          },
          modifier =
              Modifier.fillMaxWidth()
                  .testTag(LocationSelectionTestTags.INPUT_LOCATION)
                  .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    onFocusChanged(focusState.isFocused)

                    // Show dropdown when gaining focus if there are suggestions
                    if (focusState.isFocused && !wasFocused && suggestions.isNotEmpty()) {
                      showDropdown = true
                    }
                    // Hide dropdown when losing focus
                    if (!focusState.isFocused && wasFocused) {
                      showDropdown = false
                    }
                  },
          singleLine = true,
          isError = isError)

      // Dropdown menu for location suggestions
      DropdownMenu(
          expanded = showDropdown && suggestions.isNotEmpty() && isFocused,
          onDismissRequest = { showDropdown = false },
          modifier = Modifier.fillMaxWidth()) {
            suggestions.take(3).forEach { location ->
              DropdownMenuItem(
                  text = {
                    Text(
                        text =
                            location.name.take(30) + if (location.name.length > 30) "..." else "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = Bodoni)
                  },
                  onClick = {
                    onLocationQueryChange(location.name)
                    onLocationSelect(location)
                    onClearSuggestions()
                    showDropdown = false
                  },
                  modifier =
                      Modifier.padding(8.dp).testTag(LocationSelectionTestTags.LOCATION_SUGGESTION))
            }

            if (suggestions.size > 3) {
              DropdownMenuItem(
                  text = { Text("More...", fontFamily = Bodoni) },
                  onClick = { /* Optionally show more results */ },
                  modifier =
                      Modifier.padding(8.dp).testTag(LocationSelectionTestTags.LOCATION_MORE))
            }
          }
    }
  }
}

/** Preview of [LocationSelectionSection] with sample data. */
@Preview(showBackground = true)
@Composable
private fun LocationSelectionSectionPreview() {
  OOTDTheme {
    LocationSelectionSection(
        textGPSbutton = "Use GPS Location",
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
