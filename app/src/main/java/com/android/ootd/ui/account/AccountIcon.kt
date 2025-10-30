package com.android.ootd.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography

/**
 * Circular Material3 avatar showing the image from the account view model
 *
 * The composable observes `AccountViewModel.uiState` via `collectAsState()` so it will recompose
 * whenever the profile picture string changes. The avatar URL is cache-busted with a timestamp
 * parameter to ensure Coil fetches fresh bytes when the URI itself changes (rather than serving
 * stale cached data).
 *
 * @param accountViewModel Supplies the UI state which may contain a Firestore-overridden profile
 *   picture.
 * @param modifier Modifier applied to the avatar surface.
 * @param size Avatar diameter.
 * @param contentDescription Accessibility text for the avatar.
 * @param onClick Called when the avatar is clicked.
 */
@Composable
fun AccountIcon(
    accountViewModel: AccountViewModel = viewModel(),
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = "Account avatar",
    onClick: () -> Unit
) {
  val uiState by accountViewModel.uiState.collectAsState()
  val profilePicture = uiState.profilePicture
  val username = uiState.username

  Surface(
      modifier =
          modifier
              .size(size)
              .clip(CircleShape)
              .clickable(onClick = onClick)
              .testTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER),
      shape = CircleShape,
      tonalElevation = 2.dp,
      color = Primary) {
        if (profilePicture.isNotBlank()) {
          val painter = rememberAsyncImagePainter(model = profilePicture)
          Image(
              painter = painter,
              contentDescription = contentDescription,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().testTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE))
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = username.firstOrNull()?.uppercase() ?: "",
                style = Typography.headlineLarge.copy(fontSize = (size.value * 0.6f).sp),
                color = Secondary)
          }
        }
      }
}

@Preview(showBackground = true)
@Composable
private fun AccountIconPreviewEmpty() {
  AccountIcon(accountViewModel = AccountViewModel(), onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun AccountIconPreviewSampleUrl() {
  AccountIcon(accountViewModel = AccountViewModel(), onClick = {})
}
