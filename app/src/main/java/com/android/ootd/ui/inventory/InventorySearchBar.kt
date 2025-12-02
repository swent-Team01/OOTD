package com.android.ootd.ui.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.theme.Primary

@Composable
fun InventorySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Surface(
      modifier = modifier.fillMaxWidth().testTag(InventoryScreenTestTags.SEARCH_BAR),
      shadowElevation = 4.dp,
      color = White) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                  value = searchQuery,
                  onValueChange = onSearchQueryChange,
                  modifier = Modifier.weight(1f).testTag(InventoryScreenTestTags.SEARCH_FIELD),
                  placeholder = { Text("Search by brand, type, category...") },
                  singleLine = true,
                  leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                  },
                  trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                      IconButton(
                          onClick = { onSearchQueryChange("") },
                          modifier =
                              Modifier.testTag(InventoryScreenTestTags.CLOSE_SEARCH_BUTTON)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search")
                          }
                    }
                  },
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Primary,
                          focusedLabelColor = Primary,
                          cursorColor = Primary))
            }
      }
}
