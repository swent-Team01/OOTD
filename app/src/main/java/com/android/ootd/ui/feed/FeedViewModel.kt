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

  fun onPostUploaded() {
    viewModelScope.launch {
      _hasPostedToday.value = true
      // Refresh posts only if we have a current user (so we can filter)
      val user = _currentUser.value
      if (user != null) {
        _allPosts.value = repository.getFeed()
        recomputeFilteredFeed()
      } else {
        _feedPosts.value = emptyList()
      }
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
    val friendsUID =
        user.friendList
            .asSequence()
            .map(Friend::uid)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toHashSet()
    // If we want to include user's own posts, uncomment the next line
    // friendsUID.add(user.uid)
    return posts.filter { it.uid in friendsUID }
  }

  /** Called after the authenticated user is available. */
  fun setCurrentUser(user: User) {
    _currentUser.value = user
    viewModelScope.launch {
      val repoHasPosted = repository.hasPostedToday(user.uid)
      val effectiveHasPosted = _hasPostedToday.value || repoHasPosted
      _hasPostedToday.value = effectiveHasPosted

      if (effectiveHasPosted) {
        if (_allPosts.value.isEmpty()) {
          _allPosts.value = repository.getFeed()
        }
        recomputeFilteredFeed()
      } else {
        _feedPosts.value = emptyList()
      }
    }
  }
}
