package com.android.ootd.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary

object AccountPageTestTags {
  const val TITLE_TEXT = "accountPageTitleText"
  const val SETTINGS_BUTTON = "accountPageSettingsButton"
  const val AVATAR_IMAGE = "accountPageAvatarImage"
  const val AVATAR_LETTER = "accountPageAvatarLetter"
  const val USERNAME_TEXT = "accountPageUsernameText"
  const val FRIEND_COUNT_TEXT = "accountPageFriendCountText"
  const val LOADING = "accountPageLoading"

  const val YOUR_POST_SECTION = "yourPostsStart"
  const val POST_TAG = "postTag"
}

@Composable
fun AccountPage(
    accountModel: AccountPageViewModel = viewModel(),
    onEditAccount: () -> Unit = {},
    onPostClick: (String) -> Unit = {}
) {
  val uiState by accountModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      accountModel.clearErrorMsg()
    }
  }

  AccountPageContent(uiState, onEditAccount, onPostClick)
}

@Composable
fun AccountPageContent(
    uiState: AccountPageViewState,
    onEditAccount: () -> Unit,
    onPostClick: (String) -> Unit = {}
) {
  val friendList = uiState.friends
  val friendListSize = friendList.size
  val username = uiState.username
  val posts = uiState.posts
  val scrollState = rememberScrollState()

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(colorScheme.background)
              .verticalScroll(scrollState)
              .padding(horizontal = 22.dp, vertical = 10.dp)) {
        // Setting button
        EditButton(onEditAccount, Modifier.align(Alignment.End))

        Spacer(modifier = Modifier.height(36.dp))

        // Avatar
        AvatarHolder(uiState, username)

        Spacer(modifier = Modifier.height(18.dp))
        // Username
        AccountText(
            text = username,
            style = typography.displayLarge,
            color = colorScheme.primary,
            testTag = AccountPageTestTags.USERNAME_TEXT)

        Spacer(modifier = Modifier.height(9.dp))

        // Friend count
        AccountText(
            text = "$friendListSize friends",
            style = typography.bodyLarge,
            testTag = AccountPageTestTags.FRIEND_COUNT_TEXT)

        Spacer(modifier = Modifier.height(30.dp))

        AccountText(
            text = "Your posts :",
            style = typography.bodyLarge,
            textAlign = TextAlign.Left,
            fontFamily = Bodoni,
            testTag = AccountPageTestTags.YOUR_POST_SECTION)

        Spacer(modifier = Modifier.height(16.dp))

        DisplayUsersPosts(posts, onPostClick)
      }

  if (uiState.isLoading) {
    LoadingOverlay()
  }
}

@Composable
private fun AccountText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Center,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    testTag: String = ""
) {
  Text(
      text = text,
      style = style,
      color = color,
      textAlign = textAlign,
      fontFamily = fontFamily,
      modifier = modifier.fillMaxWidth().testTag(testTag))
}

@Composable
fun DisplayUsersPosts(posts: List<OutfitPost>, onPostClick: (String) -> Unit) {
  val color = colorScheme
  val defaultPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }
  val configuration = androidx.compose.ui.platform.LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val itemWidth = (screenWidth - 44.dp - 16.dp) / 3 // subtract padding and spacing
  val itemHeight = itemWidth
  val rowCount = (posts.size + 2) / 3
  val totalHeight = rowCount * itemHeight.value + (rowCount - 1) * 8

  LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().height(totalHeight.dp)) {
        items(posts) { post ->
          AsyncImage(
              model = post.outfitURL,
              contentDescription = "Post image",
              placeholder = defaultPainter,
              error = defaultPainter,
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.size(itemWidth)
                      .clip(RoundedCornerShape(8.dp))
                      .clickable(onClick = { onPostClick(post.postUID) })
                      .background(color.surfaceVariant)
                      .testTag(AccountPageTestTags.POST_TAG))
        }
      }
}

@Composable
private fun LoadingOverlay() {
  val colors = colorScheme
  Box(
      modifier = Modifier.fillMaxSize().background(colors.onBackground.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(AccountPageTestTags.LOADING), color = colors.primary)
      }
}

@Composable
private fun AvatarHolder(uiState: AccountPageViewState, username: String) {
  val profilePicture = uiState.profilePicture
  val color = colorScheme
  val defaultAvatarPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }

  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    if (profilePicture.isNotBlank() && !uiState.isLoading) {
      AsyncImage(
          model = profilePicture,
          contentDescription = "Profile Picture",
          placeholder = defaultAvatarPainter,
          error = defaultAvatarPainter,
          contentScale = ContentScale.Crop,
          modifier =
              Modifier.size(150.dp).clip(CircleShape).testTag(AccountPageTestTags.AVATAR_IMAGE))
    } else {
      Box(
          modifier = Modifier.size(150.dp).clip(CircleShape).background(Primary),
          contentAlignment = Alignment.Center) {
            Text(
                text = username.firstOrNull()?.uppercase() ?: "",
                style = typography.headlineLarge,
                color = Secondary,
                modifier = Modifier.testTag(AccountPageTestTags.AVATAR_LETTER))
          }
    }
  }
}

@Composable
private fun EditButton(onEditAccount: () -> Unit, modifier: Modifier) {
  IconButton(
      onClick = onEditAccount, modifier = modifier.testTag(AccountPageTestTags.SETTINGS_BUTTON)) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = colorScheme.onBackground,
            modifier = Modifier.size(32.dp))
      }

  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Your account",
            style = typography.displayMedium,
            fontFamily = Bodoni,
            color = colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(AccountPageTestTags.TITLE_TEXT))
      }
}
