package com.android.ootd.ui.account

import android.util.Log
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

data class AccountPageViewState(
    val username: String = "",
    val profilePicture: String = "",
    val posts: List<OutfitPost> = emptyList(),
    val friends: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)

private const val currentLog = "AccountPageViewModel"

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

  fun loadPost(postID: String) {
    // TODO
  }

  /** Clear any transient error message shown in the UI. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /** Clear any transient error message shown in the UI. */
  fun clearLoading() {
    _uiState.update { it.copy(isLoading = false) }
  }
}
