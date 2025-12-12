package com.android.ootd.ui.account

import android.accounts.NetworkErrorException
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
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

    private data class ViewModelCache(
        val account: Account,
        val posts: List<OutfitPost>,
        val starredIds: List<String>,
        val friendDetails: List<User>,
        val viewState: AccountPageViewState
    )

    private var staticCache: ViewModelCache? = null
    private var staticCacheUserId: String? = null

    @VisibleForTesting
    fun clearStaticCache() {
      staticCache = null
      staticCacheUserId = null
    }
  }

  private var cachedAccount: Account = Account()

  private var cachedPosts: List<OutfitPost> = emptyList()
  private var cachedStarredIds: List<String> = emptyList()
  private var cachedFriendDetails: List<User> = emptyList()
  private val _uiState = MutableStateFlow(AccountPageViewState())
  val uiState: StateFlow<AccountPageViewState> = _uiState.asStateFlow()

  init {
    val currentUserId = accountService.currentUserId
    if (currentUserId.isNotBlank() && currentUserId == staticCacheUserId && staticCache != null) {
      val cache = staticCache!!
      cachedAccount = cache.account
      cachedPosts = cache.posts
      cachedStarredIds = cache.starredIds
      cachedFriendDetails = cache.friendDetails
      _uiState.value = cache.viewState
    }
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
        val loadedData = uiState.value.hasLoadedInitialData
        if (!loadedData) _uiState.update { it.copy(isLoading = true) }
        val currentUserID = accountService.currentUserId
        val cachedUser = userRepository.getUser(currentUserID)

        handleCachedDataState(loadedData, cachedUser)

        // Fetch fresh data in background
        viewModelScope.launch { fetchFreshData(currentUserID) }
      } catch (e: Exception) {
        Log.e(currentLog, "Error fetching cached user data: ${e.message}")
        _uiState.update {
          it.copy(errorMsg = e.localizedMessage ?: "Failed to load account data", isLoading = false)
        }
      }
    }
  }

  private suspend fun handleCachedDataState(loadedData: Boolean, cachedUser: User) {
    val isOffline = cachedUser.uid.isBlank()
    when {
      loadedData && isOffline -> loadFromCache()
      !loadedData && isOffline ->
          throw NetworkErrorException("User is offline and has no cached data !")
    // Other cases covered by loading fresh data
    }
  }

  private suspend fun loadFromCache() {
    val cachedStarredItems = resolveCachedStarredItems()
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

  private suspend fun resolveCachedStarredItems(): List<Item> {
    return when {
      uiState.value.starredItems.isNotEmpty() -> uiState.value.starredItems
      cachedStarredIds.isEmpty() -> emptyList()
      else -> itemsRepository.getItemsByIdsAcrossOwners(cachedStarredIds)
    }
  }

  private suspend fun fetchFreshData(currentUserID: String) {
    try {
      coroutineScope {
        val freshAccount = async {
          fetchWithTimeout { accountRepository.getAccount(currentUserID) }
        }
        val freshPosts = async {
          fetchWithTimeout {
            feedRepository.getFeedForUids(listOf(currentUserID)).sortedBy { it.timestamp }
          }
        }
        val freshStarredIds = async {
          fetchWithTimeout { accountRepository.refreshStarredItems(currentUserID) }
        }

        val account = freshAccount.await()
        val posts = freshPosts.await()
        val starredIds = freshStarredIds.await()

        updateCachedData(account, posts, starredIds)
        val freshStarredItems = fetchStarredItems(starredIds)
        updateUiWithFreshData(currentUserID, account, posts, starredIds, freshStarredItems)
      }
    } catch (e: Exception) {
      handleFreshDataError(e)
    }
  }

  private suspend fun <T> fetchWithTimeout(block: suspend () -> T): T? {
    return withTimeoutOrNull(NETWORK_TIMEOUT_MS) { block() }
  }

  private suspend fun updateCachedData(
      freshAccount: Account?,
      freshPosts: List<OutfitPost>?,
      freshStarredIds: List<String>?
  ) {
    cachedAccount = freshAccount ?: cachedAccount
    cachedPosts = freshPosts ?: cachedPosts
    cachedStarredIds = freshStarredIds ?: cachedStarredIds
    cachedFriendDetails =
        cachedAccount.friendUids.mapNotNull { friendId ->
          runCatching { userRepository.getUser(friendId) }.getOrNull()
        }
  }

  private suspend fun fetchStarredItems(starredIds: List<String>?): List<Item>? {
    if (starredIds.isNullOrEmpty()) return emptyList()
    return fetchWithTimeout { itemsRepository.getItemsByIdsAcrossOwners(starredIds) }
  }

  private fun updateUiWithFreshData(
      currentUserID: String,
      freshAccount: Account?,
      freshPosts: List<OutfitPost>?,
      freshStarredIds: List<String>?,
      freshStarredItems: List<Item>?
  ) {
    if (freshAccount == null || cachedAccount.uid.isBlank()) return

    Log.d(
        currentLog,
        "Loaded fresh data, starred items: ${freshStarredIds?.joinToString() ?: "none"}")
    _uiState.update {
      val newState =
          it.copy(
              username = cachedAccount.username,
              profilePicture = cachedAccount.profilePicture,
              posts = freshPosts ?: cachedPosts,
              friends = cachedAccount.friendUids,
              friendDetails = cachedFriendDetails,
              starredItems = freshStarredItems ?: it.starredItems,
              starredItemIds = freshStarredIds?.toSet() ?: it.starredItemIds,
              isLoading = false,
              hasLoadedInitialData = true)

      updateStaticCache(currentUserID, newState)
      newState
    }
  }

  private fun updateStaticCache(currentUserID: String, newState: AccountPageViewState) {
    if (currentUserID.isBlank()) return
    staticCache =
        ViewModelCache(
            account = cachedAccount,
            posts = cachedPosts,
            starredIds = cachedStarredIds,
            friendDetails = cachedFriendDetails,
            viewState = newState)
    staticCacheUserId = currentUserID
  }

  private fun handleFreshDataError(e: Exception) {
    // Silently fail background refresh -> user already sees cached data
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
    _uiState.update {
      val newState = it.copy(selectedTab = tab)
      if (staticCache != null && staticCacheUserId == accountService.currentUserId) {
        staticCache = staticCache!!.copy(viewState = newState)
      }
      newState
    }
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
          val newState =
              state.copy(
                  starredItemIds = updatedIds.toSet(),
                  starredItems = computeUpdatedStarredItems(state.starredItems, item, updatedIds))

          updateStaticCacheForToggle(currentUserID, newState, updatedIds)
          newState
        }
      } catch (e: Exception) {
        Log.e(currentLog, "Failed to toggle starred item: ${e.message}")
      }
    }
  }

  private fun computeUpdatedStarredItems(
      currentItems: List<Item>,
      item: Item,
      updatedIds: List<String>
  ): List<Item> {
    val isNowStarred = updatedIds.contains(item.itemUuid)
    if (!isNowStarred) return currentItems

    val alreadyInList = currentItems.any { it.itemUuid == item.itemUuid }
    return if (alreadyInList) currentItems else currentItems + item
  }

  private fun updateStaticCacheForToggle(
      currentUserID: String,
      newState: AccountPageViewState,
      updatedIds: List<String>
  ) {
    if (currentUserID == staticCacheUserId && staticCache != null) {
      staticCache = staticCache!!.copy(viewState = newState, starredIds = updatedIds)
    }
  }
}
