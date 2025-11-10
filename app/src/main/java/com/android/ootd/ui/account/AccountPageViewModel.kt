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
import com.android.ootd.model.posts.OutfitPost
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
    val errorMsg: String? = null
)

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
    private val feedRepository: FeedRepository = FeedRepositoryProvider.repository
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
    try {
      viewModelScope.launch {
        val currentUserID = accountService.currentUserId
        val user = userRepository.getUser(currentUserID)
        val account = accountRepository.getAccount(currentUserID)
        val usersPosts = feedRepository.getFeedForUids(listOf(currentUserID))
        _uiState.update {
          it.copy(
              username = user.username,
              profilePicture = user.profilePicture,
              posts = usersPosts,
              friends = account.friendUids)
        }
        clearLoading()
      }
    } catch (e: Exception) {
      Log.e(currentLog, "Error fetching user data : ${e.message}")
      _uiState.update { it.copy(errorMsg = e.message) }
    }
  }

  /**
   * Loads a specific post by its ID.
   *
   * @param postID The unique identifier of the post to load.
   */
  fun loadPost(postID: String) {
    // TODO
  }

  /**
   * Clears any transient error message shown in the UI.
   *
   * Resets the [AccountPageViewState.errorMsg] to null.
   */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /**
   * Clears the loading state in the UI.
   *
   * Sets [AccountPageViewState.isLoading] to false to indicate that loading has completed.
   */
  fun clearLoading() {
    _uiState.update { it.copy(isLoading = false) }
  }
}
