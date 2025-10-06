package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.ootd.model.OutfitPost

@Composable
fun OutfitPostCard(
    post: OutfitPost,
    isBlurred: Boolean, // will add blur later, dependencies need to be changed to include coil
    modifier: Modifier = Modifier,
    onSeeFitClick: () -> Unit = {}
) {
  Card(modifier = modifier.padding(8.dp).fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
      Text(text = post.name, style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))

      // just a placeholder for now, will add the real photo later
      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .height(200.dp)
                  .background(MaterialTheme.colorScheme.surfaceVariant)) {
            Text(text = "Image placeholder", modifier = Modifier.align(Alignment.Center))
          }

      Text(text = post.description)
      Spacer(modifier = Modifier.height(8.dp))
      Button(onClick = onSeeFitClick) { Text("See fit") }
    }
  }
}
