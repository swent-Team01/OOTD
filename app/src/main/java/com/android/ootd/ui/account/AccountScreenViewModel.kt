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
 * @property isLoading Indicates whether data is currently being loaded.
 * @property errorMsg An optional error message to display in the UI.
 */
@Keep
data class AccountPageViewState(
    val username: String = "",
    val profilePicture: String = "",
    val posts: List<OutfitPost> = emptyList(),
    val friends: List<String> = emptyList(),
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

  companion object {
    const val NETWORK_TIMEOUT_MS = 2000L
  }

  private val _uiState = MutableStateFlow(AccountPageViewState())
  val uiState: StateFlow<AccountPageViewState> = _uiState.asStateFlow()

  private var hasLoadedInitialData = false

  init {
    retrieveUserData()
  }

  /**
   * Retrieves the current user's data from repositories using offline-first pattern.
   *
   * Uses parallel fetching with timeouts to handle offline scenarios gracefully. Firestore's
   * offline cache provides immediate data while network calls timeout when offline. Sets
   * [AccountPageViewState.isLoading] to true during fetch and false when complete.
   */
  private fun retrieveUserData() {
    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        val currentUserID = accountService.currentUserId

        // Fetch all data in parallel with timeouts for offline resilience
        coroutineScope {
          val userDeferred = async {
            withTimeoutOrNull(NETWORK_TIMEOUT_MS) { userRepository.getUser(currentUserID) }
          }
          val accountDeferred = async {
            withTimeoutOrNull(NETWORK_TIMEOUT_MS) { accountRepository.getAccount(currentUserID) }
          }
          val postsDeferred = async {
            withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
              feedRepository.getFeedForUids(listOf(currentUserID)).sortedBy { it.timestamp }
            }
          }
          val starredIdsDeferred = async {
            withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
              accountRepository.refreshStarredItems(currentUserID)
            }
          }

          val user = userDeferred.await()
          val account = accountDeferred.await()
          val posts = postsDeferred.await()
          val starredIds = starredIdsDeferred.await()

          // Fetch starred items only if we have IDs (also parallel-safe since it depends on
          // starredIds)
          val starredItems =
              if (starredIds.isNullOrEmpty()) {
                emptyList()
              } else {
                withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                  itemsRepository.getItemsByIdsAcrossOwners(starredIds)
                }
              }

          if (user != null) {
            Log.d(currentLog, "Loaded data, starred items: ${starredIds?.joinToString() ?: "none"}")
            hasLoadedInitialData = true
            _uiState.update {
              it.copy(
                  username = user.username,
                  profilePicture = user.profilePicture,
                  posts = posts ?: emptyList(),
                  friends = account?.friendUids ?: emptyList(),
                  starredItems = starredItems ?: emptyList(),
                  starredItemIds = starredIds?.toSet() ?: emptySet(),
                  isLoading = false)
            }
          } else {
            // User data timed out - likely offline with no cache
            _uiState.update {
              it.copy(errorMsg = "Unable to load account - check connection", isLoading = false)
            }
          }
        }
      } catch (e: Exception) {
        Log.e(currentLog, "Error fetching user data: ${e.message}")
        _uiState.update {
          it.copy(errorMsg = e.localizedMessage ?: "Failed to load account data", isLoading = false)
        }
      }
    }
  }

  /** Refreshes the user's account data only if not already loaded. */
  fun refreshUserDataIfNeeded() {
    if (!hasLoadedInitialData) {
      retrieveUserData()
    }
  }

  /** Force refreshes the user's account data (for pull-to-refresh scenarios). */
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
