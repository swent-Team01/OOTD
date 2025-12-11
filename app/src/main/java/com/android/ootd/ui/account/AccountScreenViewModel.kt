package com.android.ootd.ui.account

import android.accounts.NetworkErrorException
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.Account
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
 * @property starredItems The list of items the user has starred/wishlisted.
 * @property starredItemIds The set of item UUIDs that are starred for quick lookup.
 * @property selectedTab The currently selected tab in the account page (Posts or Starred).
 */
@Keep
data class AccountPageViewState(
    val username: String = "",
    val profilePicture: String = "",
    val posts: List<OutfitPost> = emptyList(),
    val friends: List<String> = emptyList(),
    val friendDetails: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoadedInitialData: Boolean = false,
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

  companion object {
    const val NETWORK_TIMEOUT_MS = 2000L
  }

  private var cachedAccount: Account = Account()

  private var cachedPosts: List<OutfitPost> = emptyList()
  private var cachedStarredIds: List<String> = emptyList()
  private var cachedFriendDetails: List<User> = emptyList()
  private val _uiState = MutableStateFlow(AccountPageViewState())
  val uiState: StateFlow<AccountPageViewState> = _uiState.asStateFlow()

  init {
    loadAccountData()
  }

  /**
   * Loads the user's account data using offline-first pattern.
   *
   * First loads from cache immediately (non-blocking), then fetches fresh data in the background.
   * This provides instant UI updates from cache while ensuring data freshness.
   */
  fun loadAccountData() {
    viewModelScope.launch {
      _uiState.update { it.copy(errorMsg = null) }
      try {
        val currentUserID = accountService.currentUserId
        val cachedUser = userRepository.getUser(currentUserID)
        handleCachedData(cachedUser)
        fetchFreshDataInBackground(currentUserID)
      } catch (e: Exception) {
        Log.e(currentLog, "Error fetching cached user data: ${e.message}")
        _uiState.update {
          it.copy(errorMsg = e.localizedMessage ?: "Failed to load account data", isLoading = false)
        }
      }
    }
  }

  private suspend fun handleCachedData(cachedUser: User) {
    val hasLoadedInitial = _uiState.value.hasLoadedInitialData
    val isOffline = cachedUser.uid.isBlank()

    when {
      hasLoadedInitial && isOffline -> updateStateFromCache()
      !hasLoadedInitial && isOffline ->
          throw NetworkErrorException("User is offline and has no cached data !")
    }
  }

  private suspend fun updateStateFromCache() {
    val cachedStarredItems =
        if (cachedStarredIds.isEmpty()) emptyList()
        else itemsRepository.getItemsByIdsAcrossOwners(cachedStarredIds)

    _uiState.update {
      it.copy(
          username = cachedAccount.username,
          profilePicture = cachedAccount.profilePicture,
          posts = cachedPosts,
          friends = cachedAccount.friendUids,
          friendDetails = cachedFriendDetails,
          starredItems = cachedStarredItems,
          starredItemIds = cachedStarredIds.toSet(),
          isLoading = false)
    }
  }

  private fun fetchFreshDataInBackground(currentUserID: String) {
    viewModelScope.launch {
      try {
        val freshData = fetchFreshData(currentUserID)
        updateCachedData(freshData)

        val freshStarredItems = fetchStarredItems(freshData.starredIds)
        updateStateWithFreshData(freshData, freshStarredItems)
      } catch (e: Exception) {
        handleBackgroundFetchError(e)
      }
    }
  }

  private data class FreshData(
      val account: Account?,
      val posts: List<OutfitPost>?,
      val starredIds: List<String>?
  )

  private suspend fun fetchFreshData(currentUserID: String): FreshData = coroutineScope {
    val accountDeferred = async {
      withTimeoutOrNull(NETWORK_TIMEOUT_MS) { accountRepository.getAccount(currentUserID) }
    }
    val postsDeferred = async {
      withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
        feedRepository.getFeedForUids(listOf(currentUserID)).sortedBy { it.timestamp }
      }
    }
    val starredIdsDeferred = async {
      withTimeoutOrNull(NETWORK_TIMEOUT_MS) { accountRepository.refreshStarredItems(currentUserID) }
    }

    FreshData(
        account = accountDeferred.await(),
        posts = postsDeferred.await(),
        starredIds = starredIdsDeferred.await())
  }

  private suspend fun updateCachedData(freshData: FreshData) {
    cachedAccount = freshData.account ?: cachedAccount
    cachedPosts = freshData.posts ?: cachedPosts
    cachedStarredIds = freshData.starredIds ?: cachedStarredIds
    cachedFriendDetails =
        cachedAccount.friendUids.mapNotNull { friendId ->
          runCatching { userRepository.getUser(friendId) }.getOrNull()
        }
  }

  private suspend fun fetchStarredItems(starredIds: List<String>?): List<Item>? {
    if (starredIds.isNullOrEmpty()) return emptyList()
    return withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
      itemsRepository.getItemsByIdsAcrossOwners(starredIds)
    }
  }

  private fun updateStateWithFreshData(freshData: FreshData, freshStarredItems: List<Item>?) {
    if (freshData.account == null || cachedAccount.uid.isBlank()) return

    Log.d(
        currentLog,
        "Loaded fresh data, starred items: ${freshData.starredIds?.joinToString() ?: "none"}")
    _uiState.update {
      it.copy(
          username = cachedAccount.username,
          profilePicture = cachedAccount.profilePicture,
          posts = freshData.posts ?: cachedPosts,
          friends = cachedAccount.friendUids,
          friendDetails = cachedFriendDetails,
          starredItems = freshStarredItems ?: it.starredItems,
          starredItemIds = freshData.starredIds?.toSet() ?: it.starredItemIds,
          isLoading = false,
          hasLoadedInitialData = true)
    }
  }

  private fun handleBackgroundFetchError(e: Exception) {
    if (_uiState.value.username.isEmpty()) {
      Log.e(currentLog, "Error fetching fresh user data: ${e.message}")
      _uiState.update {
        it.copy(errorMsg = e.localizedMessage ?: "Failed to load account data", isLoading = false)
      }
    }
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
