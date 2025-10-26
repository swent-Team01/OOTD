package com.android.ootd.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Placeholder Inventory screen. Will be filled later. */
@Composable
fun InventoryScreen() {
  Box(
      modifier = Modifier.fillMaxSize().testTag("inventoryScreen"),
      contentAlignment = Alignment.Center) {
        Text("Inventory (placeholder)")
      }
}
