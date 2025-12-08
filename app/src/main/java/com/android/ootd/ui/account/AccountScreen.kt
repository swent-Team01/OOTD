package com.android.ootd.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.ui.feed.SeeItemDetailsDialog
import com.android.ootd.ui.inventory.InventoryGrid
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.DisplayUserPosts
import com.android.ootd.utils.composables.LoadingScreen
import com.android.ootd.utils.composables.OOTDTabRow
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ProfilePicture
import com.android.ootd.utils.composables.SettingsButton
import com.android.ootd.utils.composables.ShowText

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
  const val OUTFIT_GRID_POST = "outfitGridPost"
}

@Composable
fun AccountPage(
    accountModel: AccountPageViewModel = viewModel(),
    onEditAccount: () -> Unit = {},
    onPostClick: (String) -> Unit = {}
) {
  val uiState by accountModel.uiState.collectAsState()
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      accountModel.clearErrorMsg()
    }
  }

  LaunchedEffect(uiState.selectedTab) {
    if (uiState.selectedTab == AccountTab.Starred) {
      accountModel.refreshUserData()
    }
  }

  DisposableEffect(lifecycleOwner) {
    // Refresh starred items whenever we return to the account screen so stars from friends'
    // See Fit dialogs show up immediately.
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        accountModel.refreshUserData()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
  val scrollState = rememberScrollState()
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(colorScheme.background)
              .verticalScroll(scrollState)
              .padding(horizontal = 22.dp, vertical = 10.dp)) {
        AccountHeader(
            username = uiState.username,
            profilePicture = uiState.profilePicture,
            friendCount = uiState.friends.size,
            onEditAccount = onEditAccount)

        AccountTabs(
            selectedTab = uiState.selectedTab,
            tabs = listOf(AccountTab.Posts, AccountTab.Starred),
            onSelectTab = onSelectTab)

        Spacer(modifier = Modifier.height(16.dp))

        AccountTabBody(
            uiState = uiState,
            onPostClick = onPostClick,
            onToggleStar = onToggleStar,
            screenHeight = screenHeight)
      }
}

@Composable
private fun AccountHeader(
    username: String,
    profilePicture: String,
    friendCount: Int,
    onEditAccount: () -> Unit
) {
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

  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    ProfilePicture(
        modifier =
            Modifier.testTag(
                AccountPageTestTags.AVATAR_LETTER.takeIf { profilePicture.isBlank() }
                    ?: AccountPageTestTags.AVATAR_IMAGE),
        size = 150.dp,
        profilePicture = profilePicture,
        username = username,
        shape = CircleShape)
  }

  Spacer(modifier = Modifier.height(18.dp))

  ShowText(
      text = username,
      style = Typography.displayLarge,
      modifier = Modifier.testTag(AccountPageTestTags.USERNAME_TEXT),
      color = colorScheme.primary)

  Spacer(modifier = Modifier.height(9.dp))

  val friendText = if (friendCount == 1) "friend" else "friends"
  ShowText(
      text = "$friendCount $friendText",
      style = Typography.bodyLarge,
      modifier = Modifier.testTag(AccountPageTestTags.FRIEND_COUNT_TEXT))

  Spacer(modifier = Modifier.height(30.dp))
}

@Composable
private fun AccountTabs(
    selectedTab: AccountTab,
    tabs: List<AccountTab>,
    onSelectTab: (AccountTab) -> Unit
) {
  OOTDTabRow(
      selectedTabIndex = tabs.indexOf(selectedTab),
      tabs = tabs.map { if (it == AccountTab.Starred) "Starred" else "Posts" },
      onTabClick = { index -> onSelectTab(tabs[index]) },
      tabModifiers =
          tabs.map { tab ->
            Modifier.testTag(
                if (tab == AccountTab.Posts) AccountPageTestTags.POSTS_TAB
                else AccountPageTestTags.STARRED_TAB)
          })
}

@Composable
private fun TabLabel(tab: AccountTab) {
  if (tab == AccountTab.Starred) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = "Starred Tab",
              tint = colorScheme.primary)
          Text("Starred")
        }
  } else {
    Text("Posts")
  }
}

@Composable
private fun AccountTabBody(
    uiState: AccountPageViewState,
    onPostClick: (String) -> Unit,
    onToggleStar: (Item) -> Unit,
    screenHeight: Dp
) {
  when (uiState.selectedTab) {
    AccountTab.Posts ->
        DisplayUserPosts(
            posts = uiState.posts,
            onPostClick = onPostClick,
            modifier = Modifier.testTag(AccountPageTestTags.POST_TAG),
            padding = 22.dp,
            spacing = 8.dp)
    AccountTab.Starred ->
        StarredTabContent(
            starredItems = uiState.starredItems,
            starredItemIds = uiState.starredItemIds,
            onToggleStar = onToggleStar,
            screenHeight = screenHeight)
  }
}

@Composable
private fun StarredTabContent(
    starredItems: List<Item>,
    starredItemIds: Set<String>,
    onToggleStar: (Item) -> Unit,
    screenHeight: Dp
) {
  var selectedItem by remember { mutableStateOf<Item?>(null) }
  if (starredItems.isEmpty()) {
    ShowText(
        text = "Star items from your inventory to build your wishlist.",
        style = Typography.bodyMedium,
        color = colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center)
  } else {
    InventoryGrid(
        items = starredItems,
        onItemClick = { selectedItem = it },
        starredItemIds = starredItemIds,
        onToggleStar = onToggleStar,
        modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight),
        showStarToggle = true)
    selectedItem?.let { item ->
      SeeItemDetailsDialog(item = item, onDismissRequest = { selectedItem = null })
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
