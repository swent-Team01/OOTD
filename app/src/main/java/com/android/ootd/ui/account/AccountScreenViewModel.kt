package com.android.ootd.ui.account

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class representing the UI state for the Account Page.
 *
 * @property username The username of the current user.
 * @property profilePicture The URL or path to the user's profile picture.
 * @property posts The list of outfit posts created by the user.
 * @property friends The list of friend user IDs.
 * @property friendDetails The list of friend user details.
 * @property isLoading Indicates whether data is currently being loaded.
 * @property errorMsg An optional error message to display in the UI.
 * @property starredItems The list of items marked as starred by the user.
 * @property starredItemIds The set of item IDs marked as starred by the user.
 * @property selectedTab The currently selected tab in the UI.
 */
@Keep
data class AccountPageViewState(
    val username: String = "",
    val profilePicture: String = "",
    val posts: List<OutfitPost> = emptyList(),
    val friends: List<String> = emptyList(),
    val friendDetails: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val starredItems: List<Item> = emptyList(),
    val starredItemIds: Set<String> = emptySet(),
    val selectedTab: AccountTab = AccountTab.Posts
)

enum class AccountTab {
  Posts,
  Starred
}

private const val currentLog = "AccountPageViewModel"

/**
 * ViewModel for the Account Page screen.
 *
 * This ViewModel manages the state and business logic for displaying the current user's account
 * information, including their profile details, posts, and friends list.
 *
 * @property accountService Service for authentication and account management.
 * @property accountRepository Repository for accessing account data.
 * @property userRepository Repository for accessing user profile data.
 * @property feedRepository Repository for accessing user posts.
 */
class AccountPageViewModel(
    private val accountService: AccountService = AccountServiceFirebase(),
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val feedRepository: FeedRepository = FeedRepositoryProvider.repository,
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(AccountPageViewState())
  val uiState: StateFlow<AccountPageViewState> = _uiState.asStateFlow()

  init {
    retrieveUserData()
  }

  /**
   * Retrieves the current user's data from repositories.
   *
   * Fetches the user's profile information, account details, and posts, then updates the UI state.
   * Sets [AccountPageViewState.isLoading] to true during the fetch operation and false when
   * complete. If an error occurs, sets [AccountPageViewState.errorMsg] with the error message.
   */
  private fun retrieveUserData() {
    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      try {
        val currentUserID = accountService.currentUserId
        val user = userRepository.getUser(currentUserID)
        val account = accountRepository.getAccount(currentUserID)
        val usersPosts =
            feedRepository.getFeedForUids(listOf(currentUserID)).sortedBy { it.timestamp }
        val starredIds = accountRepository.refreshStarredItems(currentUserID)
        val starredItems =
            if (starredIds.isEmpty()) emptyList()
            else itemsRepository.getItemsByIdsAcrossOwners(starredIds)
        val friendDetails =
            account.friendUids.mapNotNull { friendId ->
              runCatching { userRepository.getUser(friendId) }.getOrNull()
            }
        Log.d(currentLog, "Refreshed starred items: ${starredIds.joinToString()}")
        _uiState.update {
          it.copy(
              username = user.username,
              profilePicture = user.profilePicture,
              posts = usersPosts,
              friends = account.friendUids,
              friendDetails = friendDetails,
              starredItems = starredItems,
              starredItemIds = starredIds.toSet(),
              isLoading = false)
        }
        clearErrorMsg()
      } catch (e: Exception) {
        Log.e(currentLog, "Error fetching user data : ${e.message}")
        _uiState.update {
          it.copy(errorMsg = e.localizedMessage ?: "Failed to load account data", isLoading = false)
        }
      }
    }
  }

  /** Refreshes the user's account data. */
  fun refreshUserData() {
    retrieveUserData()
  }

  /**
   * Clears any transient error message shown in the UI.
   *
   * Resets the [AccountPageViewState.errorMsg] to null.
   */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  fun selectTab(tab: AccountTab) {
    _uiState.update { it.copy(selectedTab = tab) }
  }

  /**
   * Adds or removes [item] from the starred list.
   *
   * We optimistically keep [AccountPageViewState.starredItems] unchanged so the card stays visible
   * while browsing the wishlist; only the underlying id set is updated. The tab will re-fetch the
   * authoritative list when the screen resumes.
   */
  fun toggleStar(item: Item) {
    viewModelScope.launch {
      try {
        val currentUserID = accountService.currentUserId
        if (currentUserID.isBlank()) return@launch
        val updatedIds = accountRepository.toggleStarredItem(item.itemUuid)
        _uiState.update { state ->
          state.copy(
              starredItemIds = updatedIds.toSet(),
              starredItems =
                  if (updatedIds.contains(item.itemUuid)) {
                    if (state.starredItems.any { it.itemUuid == item.itemUuid }) {
                      state.starredItems
                    } else {
                      state.starredItems + item
                    }
                  } else {
                    state.starredItems
                  })
        }
      } catch (e: Exception) {
        Log.e(currentLog, "Failed to toggle starred item: ${e.message}")
      }
    }
  }
}
