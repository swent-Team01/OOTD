package com.android.ootd.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.OutfitPost
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.user.Friend
import com.android.ootd.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

  private val repository = FeedRepositoryProvider.repository

  // Holds all posts from all users
  private val _allPosts = MutableStateFlow<List<OutfitPost>>(emptyList())

  // Exposed filtered posts (friends-only)
  private val _feedPosts = MutableStateFlow<List<OutfitPost>>(emptyList())
  val feedPosts: StateFlow<List<OutfitPost>> = _feedPosts

  private val _currentUser = MutableStateFlow<User?>(null)
  val currentUser: StateFlow<User?> = _currentUser
  private val _hasPostedToday = MutableStateFlow(false)
  val hasPostedToday: StateFlow<Boolean> = _hasPostedToday

  init {
    viewModelScope.launch {
      // Always check if user has posted first
      _hasPostedToday.value = repository.hasPostedToday()

      // Only load posts if user has posted
      if (_hasPostedToday.value) {
        _allPosts.value = repository.getFeed()
        recomputeFilteredFeed()
      }
    }
  }

  fun onPostUploaded() {
    viewModelScope.launch {
      _hasPostedToday.value = true
      // Refresh all posts from repository, then filter
      _allPosts.value = repository.getFeed()
      recomputeFilteredFeed()
    }
  }

  // Call this once the authenticated user is available so we can access their friends
  fun setCurrentUser(user: User) {
    _currentUser.value = user
    // If posts are already loaded, filter now
    if (_hasPostedToday.value && _allPosts.value.isNotEmpty()) {
      recomputeFilteredFeed()
    }
  }

  fun recomputeFilteredFeed() {
    val user = _currentUser.value
    if (user == null) {
      _feedPosts.value = emptyList()
      return
    }
    _feedPosts.value = filterPostsByFriends(_allPosts.value, user)
  }

  // Filters to include only posts from friends
  fun filterPostsByFriends(posts: List<OutfitPost>, user: User): List<OutfitPost> {
    val friendsUID = user.friendList.asSequence().map(Friend::uid).toHashSet()
    // If we want to include user's own posts, uncomment the next line
    // friendsUID.add(user.uid)
    return posts.filter { it.uid in friendsUID }
  }
}
