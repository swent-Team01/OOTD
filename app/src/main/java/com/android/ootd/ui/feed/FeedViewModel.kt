package com.android.ootd.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.OutfitPost
import com.android.ootd.model.feed.FeedRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

  private val repository = FeedRepositoryProvider.repository

  private val _feedPosts = MutableStateFlow<List<OutfitPost>>(emptyList())
  val feedPosts: StateFlow<List<OutfitPost>> = _feedPosts

  private val _hasPostedToday = MutableStateFlow(false)
  val hasPostedToday: StateFlow<Boolean> = _hasPostedToday

  init {
    viewModelScope.launch {
      // Always check if user has posted first
      _hasPostedToday.value = repository.hasPostedToday()

      // Only load posts if user has posted
      if (_hasPostedToday.value) {
        _feedPosts.value = repository.getFeed()
      }
    }
  }

  fun onPostUploaded() {
    viewModelScope.launch {
      _hasPostedToday.value = true
      _feedPosts.value = repository.getFeed()
    }
  }
}
