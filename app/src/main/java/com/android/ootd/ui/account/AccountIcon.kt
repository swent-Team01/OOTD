package com.android.ootd.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary

/**
 * Circular Material3 avatar showing the image at [imageUrl] or a default icon.
 *
 * @param imageUrl Non-null URL string; if empty the default icon is shown.
 * @param modifier Modifier applied to the avatar surface.
 * @param size Avatar diameter.
 * @param contentDescription Accessibility text for the avatar.
 * @param onClick Called when the avatar is clicked.
 */
@Composable
fun AccountIcon(
    imageUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = "Account avatar",
    onClick: () -> Unit
) {
  Surface(
      modifier = modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
      shape = CircleShape,
      tonalElevation = 2.dp,
      color = Primary) {
        if (imageUrl.isNotBlank()) {
          val painter = rememberAsyncImagePainter(model = imageUrl)
          Image(
              painter = painter,
              contentDescription = contentDescription,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize())
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = Secondary,
                modifier = Modifier.size(size * 0.6f))
          }
        }
      }
}

@Preview(showBackground = true)
@Composable
private fun AccountIconPreviewEmpty() {
  AccountIcon(imageUrl = "", onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun AccountIconPreviewSampleUrl() {
  // Preview uses a placeholder URL; actual image may not load in preview.
  AccountIcon(imageUrl = "https://via.placeholder.com/150", onClick = {})
}
