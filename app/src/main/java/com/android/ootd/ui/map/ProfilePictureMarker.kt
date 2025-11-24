package com.android.ootd.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.toLatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState

/**
 * Marker content that displays a profile picture or initials. Extracted for testability - this can
 * be tested without Google Maps rendering.
 *
 * @param username Name of the user, used for initials
 * @param imageUrl URL of the profile picture; if null or empty, initials will be shown instead
 */
@Composable
fun MarkerContent(username: String, imageUrl: String?) {
  val shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 0.dp)
  val painter =
      rememberAsyncImagePainter(
          ImageRequest.Builder(LocalContext.current).data(imageUrl).allowHardware(false).build())

  Box(
      modifier =
          Modifier.size(48.dp)
              .clip(shape)
              .background(MaterialTheme.colorScheme.secondary)
              .padding(4.dp),
      contentAlignment = Alignment.Center) {
        if (!imageUrl.isNullOrEmpty()) {
          Image(
              painter = painter,
              contentDescription = "Profile Picture for $username",
              modifier = Modifier.fillMaxSize().clip(shape),
              contentScale = ContentScale.Crop)
        } else {
          Text(
              text = username.take(1).uppercase(),
              color = MaterialTheme.colorScheme.primary,
              style = MaterialTheme.typography.bodyLarge)
        }
      }
}

/**
 * Marker composable that displays a profile picture or initials at a given location.
 *
 * Disclaimer: Inspiration for this code comes from
 * https://medium.com/@cp-megh-l/how-to-set-custom-marker-in-google-map-jetpack-compose-2529f562b649
 *
 * @param username Name of the user, used for initials and marker title
 * @param imageUrl URL of the profile picture; if null or empty, initials will be shown instead
 * @param location Location where the marker should be placed
 * @param tag Unique tag for the marker for testing purposes
 * @param onClick Lambda to be invoked when the marker is clicked
 */
@Composable
fun ProfilePictureMarker(
    username: String,
    imageUrl: String?,
    location: Location,
    tag: String,
    onClick: () -> Unit
) {
  val markerState = remember { MarkerState(position = location.toLatLng()) }

  MarkerComposable(
      keys = arrayOf(username, imageUrl ?: ""),
      state = markerState,
      title = username,
      tag = tag,
      anchor = Offset(0.5f, 1f),
      onClick = {
        onClick()
        true
      }) {
        MarkerContent(username = username, imageUrl = imageUrl)
      }
}
