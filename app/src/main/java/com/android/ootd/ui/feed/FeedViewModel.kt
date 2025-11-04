package com.android.ootd.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

data class FeedUiState(
    val feedPosts: List<OutfitPost> = emptyList(),
    val currentAccount: Account? = null,
    val hasPostedToday: Boolean = false
)

class FeedViewModel(
    private val repository: FeedRepository = FeedRepositoryProvider.repository,
    private val accountService: AccountService = AccountServiceFirebase(),
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(FeedUiState())
  val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

  init {
    observeAuthAndLoadAccount()
  }

  private fun observeAuthAndLoadAccount() {
    viewModelScope.launch {
      accountService.currentUser
          .distinctUntilChangedBy { it?.uid }
          .collect { user ->
            if (user == null) {
              _uiState.value = FeedUiState()
            } else {
              try {
                val acct = accountRepository.getAccount(user.uid)
                setCurrentAccount(acct)
              } catch (e: Exception) {
                Log.e("FeedViewModel", "Failed to load account for uid=${user.uid}", e)
                _uiState.value = _uiState.value.copy(currentAccount = null, feedPosts = emptyList())
              }
            }
          }
    }
  }

  fun refreshFeedFromFirestore() {
    val account = _uiState.value.currentAccount
    if (account != null) {
      viewModelScope.launch {
        val repoHasPosted = repository.hasPostedToday(account.uid)
        val effectiveHasPosted = _uiState.value.hasPostedToday || repoHasPosted

        // Always load the posts, they will be blurred if user has not posted today
        val posts = repository.getFeedForUids(account.friendUids + account.uid)

        _uiState.value = _uiState.value.copy(hasPostedToday = effectiveHasPosted, feedPosts = posts)
      }
    }
  }

  fun setCurrentAccount(account: Account) {
    _uiState.value = _uiState.value.copy(currentAccount = account)
  }
}
