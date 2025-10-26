package com.android.ootd.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.Account
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val feedPosts: List<OutfitPost> = emptyList(),
    val currentAccount: Account? = null,
    val hasPostedToday: Boolean = false
)

class FeedViewModel(private val repository: FeedRepository = FeedRepositoryProvider.repository) :
    ViewModel() {

  private val _uiState = MutableStateFlow(FeedUiState())
  val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  fun onPostUploaded() {
    _uiState.value = _uiState.value.copy(hasPostedToday = true)
    // Refresh for the current account if present
    _uiState.value.currentAccount?.let { loadFriendFeedFor(it) }
        ?: run { _uiState.value = _uiState.value.copy(feedPosts = emptyList()) }
  }

  /** Called after the authenticated user's account is available. */
  fun setCurrentAccount(account: Account) {
    _uiState.value = _uiState.value.copy(currentAccount = account)
    viewModelScope.launch {
      val repoHasPosted = repository.hasPostedToday(account.uid)
      val effectiveHasPosted = _uiState.value.hasPostedToday || repoHasPosted
      _uiState.value = _uiState.value.copy(hasPostedToday = effectiveHasPosted)

      if (effectiveHasPosted) {
        loadFriendFeedFor(account)
      } else {
        _uiState.value = _uiState.value.copy(feedPosts = emptyList())
      }
    }
  }

  private fun loadFriendFeedFor(account: Account) {
    loadJob?.cancel()
    loadJob =
        viewModelScope.launch {
          val friendUids = account.friendUids
          if (friendUids.isEmpty()) {
            _uiState.value = _uiState.value.copy(feedPosts = emptyList())
            return@launch
          }
          try {
            val posts = repository.getFeedForUids(friendUids)
            if (posts != _uiState.value.feedPosts) {
              _uiState.value = _uiState.value.copy(feedPosts = posts)
            }
          } catch (e: Exception) {
            Log.e("FeedViewModel", "Failed to load friend feed", e)
            _uiState.value = _uiState.value.copy(feedPosts = emptyList())
          }
        }
  }
}
