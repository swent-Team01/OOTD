package com.android.ootd.ui.map

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.android.ootd.model.map.Location
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.utils.LocationUtils.toLatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Test tags for ProfilePictureMarker components */
object ProfilePictureMarkerTestTags {
  const val MARKER_CONTENT = "profilePictureMarkerContent"
  const val PROFILE_IMAGE = "profilePictureMarkerImage"
  const val PROFILE_LETTER = "profilePictureMarkerLetter"

  fun getMarkerTag(username: String): String = "profilePictureMarker_$username"
}

/**
 * Marker content that displays user profile picture or initials.
 *
 * Uses pre-loaded bitmap for profile pictures to work within MarkerComposable's bitmap rendering
 * context.
 *
 * @param profileBitmap Pre-loaded bitmap of the user's profile picture, or null for initials
 * @param username Name of the user, used for initials fallback
 */
@Composable
fun MarkerContent(profileBitmap: Bitmap?, username: String) {
  Box(
      modifier = Modifier.size(60.dp).testTag(ProfilePictureMarkerTestTags.MARKER_CONTENT),
      contentAlignment = Alignment.Center) {
        if (profileBitmap != null) {
          // Display the pre-loaded bitmap
          Image(
              bitmap = profileBitmap.asImageBitmap(),
              contentDescription = "Profile Picture",
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.size(60.dp)
                      .clip(CircleShape)
                      .testTag(ProfilePictureMarkerTestTags.PROFILE_IMAGE))
        } else {
          // Display initials as fallback
          Box(
              modifier =
                  Modifier.size(60.dp)
                      .clip(CircleShape)
                      .background(Primary)
                      .testTag(ProfilePictureMarkerTestTags.PROFILE_LETTER),
              contentAlignment = Alignment.Center) {
                Text(
                    text = username.firstOrNull()?.uppercase() ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = Bodoni,
                    color = Secondary)
              }
        }
      }
}

/**
 * Marker composable that displays user profile picture or initials at a given location.
 *
 * Pre-loads profile pictures as bitmaps to work within MarkerComposable's bitmap rendering context.
 * Falls back to initials if the profile picture fails to load.
 *
 * Disclaimer: Inspiration for this code comes from
 * https://medium.com/@cp-megh-l/how-to-set-custom-marker-in-google-map-jetpack-compose-2529f562b649
 *
 * @param userRepository The user repository to fetch user data from
 * @param userId The user ID (UID) to fetch profile picture from
 * @param username Name of the user, used for initials and marker title
 * @param location Location where the marker should be placed
 * @param tag Unique tag for the marker for testing purposes
 * @param onClick Lambda to be invoked when the marker is clicked
 */
@Composable
fun ProfilePictureMarker(
    userRepository: UserRepository = UserRepositoryProvider.repository,
    userId: String,
    username: String,
    location: Location,
    tag: String,
    onClick: () -> Unit
) {
  val context = LocalContext.current
  var profileBitmap by remember(userId) { mutableStateOf<Bitmap?>(null) }

  // Pre-load the profile picture as a bitmap
  LaunchedEffect(userId) {
    try {
      if (userId.isNotBlank()) {
        // Fetch user data
        val user = userRepository.getUser(userId)

        if (user.profilePicture.isNotBlank()) {
          // Load the image as a bitmap using Coil
          withContext(Dispatchers.IO) {
            val imageLoader = ImageLoader(context)
            val request =
                ImageRequest.Builder(context)
                    .data(user.profilePicture)
                    .size(120) // Load at 3x the display size for better quality
                    .allowHardware(false) // Disable hardware bitmaps for compose compatibility
                    .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
              profileBitmap = (result.drawable as? BitmapDrawable)?.bitmap
            }
          }
        }
      }
    } catch (e: Exception) {
      // If loading fails, profileBitmap remains null and initials will be shown
      profileBitmap = null
    }
  }

  val markerState = remember { MarkerState(position = location.toLatLng()) }

  MarkerComposable(
      keys = arrayOf<Any>(username, profileBitmap?.hashCode() ?: 0),
      state = markerState,
      title = username,
      tag = tag,
      anchor = Offset(0.5f, 1f),
      onClick = {
        onClick()
        true
      }) {
        MarkerContent(profileBitmap = profileBitmap, username = username)
      }
}
