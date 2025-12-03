package com.android.ootd.ui.inventory

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.theme.OnSurface
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary

@Composable
fun InventorySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Surface(
      modifier = modifier.fillMaxWidth().testTag(InventoryScreenTestTags.SEARCH_BAR),
      color = Color.Transparent) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(InventoryScreenTestTags.SEARCH_FIELD),
            placeholder = {
              Text(
                  "Search by brand, type, category...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Tertiary)
            },
            singleLine = true,
            leadingIcon = {
              Icon(
                  imageVector = Icons.Default.Search, contentDescription = "Search", tint = Primary)
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.testTag(InventoryScreenTestTags.CLOSE_SEARCH_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.Close,
                          contentDescription = "Clear search",
                          tint = Primary)
                    }
              }
            },
            shape = RoundedCornerShape(50),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Secondary,
                    unfocusedContainerColor = Secondary,
                    disabledContainerColor = Secondary,
                    cursorColor = Primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface))
      }
}
