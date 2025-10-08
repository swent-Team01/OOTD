package com.android.ootd.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.OutfitPost
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

  private val repository = FeedRepositoryProvider.repository

  private val _feedPosts = MutableStateFlow<List<OutfitPost>>(emptyList())
  val feedPosts: StateFlow<List<OutfitPost>> = _feedPosts

  private val _currentUser = MutableStateFlow<User?>(null)
  val currentUser: StateFlow<User?> = _currentUser

  private val _hasPostedToday = MutableStateFlow(false)
  val hasPostedToday: StateFlow<Boolean> = _hasPostedToday

  init {
    // We’ll only load once currentUser is set
    // So init stays lightweight and safe
  }

  /** Called after the authenticated user is available. */
  fun setCurrentUser(user: User) {
    _currentUser.value = user
    viewModelScope.launch {
      // Check if this user has posted today
      _hasPostedToday.value = repository.hasPostedToday(user.uid)
      if (_hasPostedToday.value) {
        _feedPosts.value = repository.getFeed()
        // for future merging purposes
        // recomputeFilteredFeed()
      }
    }
  }

  /** Called when the user uploads a new post. */
  fun onPostUploaded() {
    viewModelScope.launch {
      _hasPostedToday.value = true
      _feedPosts.value = repository.getFeed()
      // placeholder for future friend filtering
      // recomputeFilteredFeed()
    }
  }

  //  //Placeholder - Marc you can uncomment this
  //  fun recomputeFilteredFeed() {
  //    // For now, just ensure feedPosts always contains the latest feed
  //    _feedPosts.value = _feedPosts.value
  //  }
}
