package com.android.ootd.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.OutfitPost
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.user.Friend
import com.android.ootd.model.user.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val feedPosts: List<OutfitPost> = emptyList(),
    val currentUser: User? = null,
    val hasPostedToday: Boolean = false
)

class FeedViewModel(private val repository: FeedRepository = FeedRepositoryProvider.repository) :
    ViewModel() {

  private val _uiState = MutableStateFlow(FeedUiState())
  val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  fun onPostUploaded() {
    _uiState.value = _uiState.value.copy(hasPostedToday = true)
    // Refresh for the current user if present
    _uiState.value.currentUser?.let { loadFriendFeedFor(it) }
        ?: run { _uiState.value = _uiState.value.copy(feedPosts = emptyList()) }
  }

  /** Called after the authenticated user is available. */
  fun setCurrentUser(user: User) {
    _uiState.value = _uiState.value.copy(currentUser = user)
    viewModelScope.launch {
      val repoHasPosted = repository.hasPostedToday(user.uid)
      val effectiveHasPosted = _uiState.value.hasPostedToday || repoHasPosted
      _uiState.value = _uiState.value.copy(hasPostedToday = effectiveHasPosted)

      if (effectiveHasPosted) {
        loadFriendFeedFor(user)
      } else {
        _uiState.value = _uiState.value.copy(feedPosts = emptyList())
      }
    }
  }

  private fun loadFriendFeedFor(user: User) {
    loadJob?.cancel()
    loadJob =
        viewModelScope.launch {
          val friendUids =
              user.friendList
                  .asSequence()
                  .map(Friend::uid)
                  .map(String::trim)
                  .filter { it.isNotEmpty() }
                  .distinct()
                  .toList()
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
