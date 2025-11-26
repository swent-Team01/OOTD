package com.android.ootd.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.ui.Inventory.InventoryGrid
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.utils.DisplayUserPosts
import com.android.ootd.utils.LoadingScreen
import com.android.ootd.utils.OOTDTopBar
import com.android.ootd.utils.ProfilePicture
import com.android.ootd.utils.SettingsButton
import com.android.ootd.utils.ShowText

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
  const val POSTS_TAB = "accountPagePostsTab"
  const val STARRED_TAB = "accountPageStarredTab"
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

  if (uiState.isLoading) {
    LoadingScreen(
        modifier = Modifier.testTag(AccountPageTestTags.LOADING),
        contentDescription = "Loading profile")
  } else {
    AccountPageContent(
        uiState = uiState,
        onEditAccount = onEditAccount,
        onPostClick = onPostClick,
        onSelectTab = accountModel::selectTab,
        onToggleStar = accountModel::toggleStar)
  }
}

@Composable
fun AccountPageContent(
    uiState: AccountPageViewState,
    onEditAccount: () -> Unit,
    onPostClick: (String) -> Unit = {},
    onSelectTab: (AccountTab) -> Unit,
    onToggleStar: (Item) -> Unit
) {
  val friendList = uiState.friends
  val friendListSize = friendList.size
  val username = uiState.username
  val posts = uiState.posts
  val profilePicture = uiState.profilePicture
  val scrollState = rememberScrollState()
  val starredItems = uiState.starredItems
  val tabs = listOf(AccountTab.Posts, AccountTab.Starred)
  val selectedIndex = tabs.indexOf(uiState.selectedTab)
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(colorScheme.background)
              .verticalScroll(scrollState)
              .padding(horizontal = 22.dp, vertical = 10.dp)) {
        // Setting button
        OOTDTopBar(
            textModifier = Modifier.testTag(AccountPageTestTags.TITLE_TEXT),
            rightComposable = {
              SettingsButton(
                  onEditAccount = onEditAccount,
                  modifier = Modifier.testTag(AccountPageTestTags.SETTINGS_BUTTON),
                  size = 32.dp)
            },
            leftComposable = {})
        Spacer(modifier = Modifier.height(36.dp))

        // Avatar
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          ProfilePicture(
              modifier =
                  Modifier.testTag(
                      AccountPageTestTags.AVATAR_LETTER.takeIf { profilePicture.isBlank() }
                          ?: AccountPageTestTags.AVATAR_IMAGE),
              size = 150.dp,
              profilePicture = profilePicture,
              username = uiState.username,
              shape = CircleShape)
        }

        Spacer(modifier = Modifier.height(18.dp))
        // Username
        ShowText(
            text = username,
            style = typography.displayLarge,
            modifier = Modifier.testTag(AccountPageTestTags.USERNAME_TEXT),
            color = colorScheme.primary)

        Spacer(modifier = Modifier.height(9.dp))

        // Friend count
        ShowText(
            text = "$friendListSize friends",
            style = typography.bodyLarge,
            modifier = Modifier.testTag(AccountPageTestTags.FRIEND_COUNT_TEXT))

        Spacer(modifier = Modifier.height(30.dp))

        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = colorScheme.secondary,
            contentColor = colorScheme.onSecondaryContainer) {
              tabs.forEach { tab ->
                Tab(
                    selected = tab == uiState.selectedTab,
                    onClick = { onSelectTab(tab) },
                    modifier =
                        Modifier.testTag(
                            if (tab == AccountTab.Posts) AccountPageTestTags.POSTS_TAB
                            else AccountPageTestTags.STARRED_TAB),
                    text = {
                      if (tab == AccountTab.Starred) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                              imageVector = Icons.Filled.Star,
                              contentDescription = "Starred Tab",
                              tint = colorScheme.primary)
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Starred")
                        }
                      } else {
                        Text("Posts")
                      }
                    })
              }
            }

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.selectedTab) {
          AccountTab.Posts -> {
            ShowText(
                text = "Your posts :",
                style = typography.bodyLarge,
                modifier = Modifier.testTag(AccountPageTestTags.YOUR_POST_SECTION),
                textAlign = TextAlign.Left,
                fontFamily = Bodoni)

            Spacer(modifier = Modifier.height(16.dp))

            DisplayUserPosts(
                posts = posts,
                onPostClick = onPostClick,
                modifier = Modifier.testTag(AccountPageTestTags.POST_TAG),
                padding = 22.dp,
                spacing = 8.dp)
          }
          AccountTab.Starred -> {
            if (starredItems.isEmpty()) {
              ShowText(
                  text = "Star items from your inventory to build your wishlist.",
                  style = typography.bodyMedium,
                  color = colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center)
            } else {
              InventoryGrid(
                  items = starredItems,
                  onItemClick = {},
                  starredItemIds = starredItems.map { it.itemUuid }.toSet(),
                  onToggleStar = onToggleStar,
                  modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight))
            }
          }
        }
      }
}

@Preview(showBackground = true)
@Composable
fun AccountPagePreview() {
  val sampleState =
      AccountPageViewState(
          username = "JohnDoe",
          profilePicture = "",
          friends = listOf("friend1", "friend2", "friend3"),
          posts = listOf(),
          isLoading = false,
          errorMsg = null,
          starredItems =
              listOf(
                  Item(
                      itemUuid = "1",
                      postUuids = emptyList(),
                      image = ImageData("1", ""),
                      category = "Clothing",
                      ownerId = "user")))

  OOTDTheme {
    AccountPageContent(
        uiState = sampleState,
        onEditAccount = {},
        onPostClick = {},
        onSelectTab = {},
        onToggleStar = {})
  }
}
