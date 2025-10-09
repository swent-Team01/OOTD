package com.android.ootd.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.OutfitPost
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.user.Friend
import com.android.ootd.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val allPosts: List<OutfitPost> = emptyList(),
    val feedPosts: List<OutfitPost> = emptyList(),
    val currentUser: User? = null,
    val hasPostedToday: Boolean = false
)

class FeedViewModel(private val repository: FeedRepository = FeedRepositoryProvider.repository) :
    ViewModel() {

  private val _uiState = MutableStateFlow(FeedUiState())
  val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

  fun onPostUploaded() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(hasPostedToday = true)

      val user = _uiState.value.currentUser
      if (user != null) {
        val all = repository.getFeed()
        _uiState.value =
            _uiState.value.copy(allPosts = all, feedPosts = filterPostsByFriends(all, user))
      } else {
        _uiState.value = _uiState.value.copy(feedPosts = emptyList())
      }
    }
  }

  private fun recomputeFilteredFeed() {
    val user = _uiState.value.currentUser
    if (user == null) {
      _uiState.value = _uiState.value.copy(feedPosts = emptyList())
      return
    }
    _uiState.value =
        _uiState.value.copy(feedPosts = filterPostsByFriends(_uiState.value.allPosts, user))
  }

  // Filters to include only posts from friends
  internal fun filterPostsByFriends(posts: List<OutfitPost>, user: User): List<OutfitPost> {
    val friendsUID =
        user.friendList
            .asSequence()
            .map(Friend::uid)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toHashSet()
    return posts.filter { it.uid in friendsUID }
  }

  /** Called after the authenticated user is available. */
  fun setCurrentUser(user: User) {
    _uiState.value = _uiState.value.copy(currentUser = user)
    viewModelScope.launch {
      // Combine repository state with local session state to avoid races
      val repoHasPosted = repository.hasPostedToday(user.uid)
      val effectiveHasPosted = _uiState.value.hasPostedToday || repoHasPosted
      _uiState.value = _uiState.value.copy(hasPostedToday = effectiveHasPosted)

      if (effectiveHasPosted) {
        val all = _uiState.value.allPosts.ifEmpty { repository.getFeed() }
        _uiState.value = _uiState.value.copy(allPosts = all)
        recomputeFilteredFeed()
      } else {
        _uiState.value = _uiState.value.copy(feedPosts = emptyList())
      }
    }
  }
}
