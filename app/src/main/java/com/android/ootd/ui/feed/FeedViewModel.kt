package com.android.ootd.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.posts.Like
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.LikesRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the FeedScreen.
 *
 * This state holds the data needed to display the feed of outfit posts.
 */
data class FeedUiState(
    val feedPosts: List<OutfitPost> = emptyList(),
    val currentAccount: Account? = null,
    val hasPostedToday: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val likes: Map<String, Boolean> = emptyMap(),
    val likeCounts: Map<String, Int> = emptyMap(),
    val isPublicFeed: Boolean = false
)

/**
 * ViewModel for the FeedScreen.
 *
 * Responsible for managing the state by fetching and providing data for feed posts from the
 * [FeedRepository] and account data from the [AccountRepository].
 */
open class FeedViewModel(
    private val repository: FeedRepository = FeedRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val likesRepository: LikesRepository = LikesRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(FeedUiState())
  val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

  // Cache for feed posts to use when the app is in offline mode
  private var cachedFeed: List<OutfitPost> = emptyList()

  init {
    observeAuthAndLoadAccount()
  }

  /** Observes Firebase Auth state changes and loads the current account accordingly. */
  private fun observeAuthAndLoadAccount() {
    auth.addAuthStateListener { auth ->
      val user = auth.currentUser
      if (user == null) {
        _uiState.value = FeedUiState()
      } else {
        viewModelScope.launch {
          try {
            val acct = accountRepository.getAccount(user.uid)
            setCurrentAccount(acct)
          } catch (e: Exception) {
            Log.e("FeedViewModel", "Failed to load account", e)
            _uiState.value = _uiState.value.copy(currentAccount = null, feedPosts = emptyList())
          }
        }
      }
    }
  }

  /**
   * Loads cached feed posts from the local cache based on the current feed type (public or
   * friends).
   */
  fun loadCachedFeed() {
    viewModelScope.launch {
      // Retrieves the current account data
      val currentAccount = _uiState.value.currentAccount ?: return@launch

      // Load cached feed based on the feed type
      val cached =
          if (_uiState.value.isPublicFeed) {
            repository.getCachedPublicFeed()
          } else {
            repository.getCachedFriendFeed(currentAccount.friendUids + currentAccount.uid)
          }

      // Update UI state with cached feed if available
      if (cached.isNotEmpty()) {
        cachedFeed = cached
        _uiState.value = _uiState.value.copy(feedPosts = cached, isLoading = false)
      }
    }
  }

  /** Refreshes the feed posts from Firestore for the current account. */
  fun refreshFeedFromFirestore() {
    val account = _uiState.value.currentAccount ?: return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)

      if (cachedFeed.isNotEmpty() && _uiState.value.feedPosts.isEmpty()) {
        _uiState.value = _uiState.value.copy(feedPosts = cachedFeed, isLoading = false)
      }

      _uiState.value = _uiState.value.copy(errorMessage = null)

      try {

        _uiState.value = _uiState.value.copy(isLoading = true)

        val hasPostedToday = repository.hasPostedToday(account.uid)

        val allPosts =
            if (_uiState.value.isPublicFeed) {
              repository.getPublicFeed()
            } else {
              repository.getRecentFeedForUids(account.friendUids + account.uid)
            }

        val todayStart =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Check if user has posted today in the cached feed as well
        val localHasPostedToday =
            cachedFeed.any { it.ownerId == account.uid && it.timestamp >= todayStart }

        // Combine both checks
        val finalHasPostedToday = hasPostedToday || localHasPostedToday

        val filteredPost =
            if (_uiState.value.isPublicFeed) {
              allPosts
            } else {
              allPosts.filter { post ->
                if (post.ownerId == account.uid) {
                  post.timestamp >= todayStart
                } else true
              }
            }

        val likesMap = mutableMapOf<String, Boolean>()
        val likeCounts = mutableMapOf<String, Int>()

        for (post in filteredPost) {
          val postId = post.postUID
          try {
            val isLiked = likesRepository.isPostLikedByUser(postId, account.uid)
            val count = likesRepository.getLikeCount(postId)
            likesMap[postId] = isLiked
            likeCounts[postId] = count
          } catch (e: Exception) {
            Log.e("FeedViewModel", "Failed to load likes for post $postId", e)
          }
        }

        cachedFeed = filteredPost

        _uiState.value =
            _uiState.value.copy(
                hasPostedToday = finalHasPostedToday,
                feedPosts = filteredPost,
                isLoading = false,
                likes = likesMap,
                likeCounts = likeCounts)
      } catch (e: Exception) {
        Log.e("FeedViewModel", "Failed to load initial feed", e)
        // Only show error if we have no posts at all
        if (cachedFeed.isNotEmpty()) {
          _uiState.value = _uiState.value.copy(isLoading = false, feedPosts = cachedFeed)
        } else {
          _uiState.value =
              _uiState.value.copy(
                  errorMessage = "Failed to load feed. Please try again.", isLoading = false)
        }
      }
    }
  }

  /** Toggles between public and private feed. */
  fun toggleFeedType() {
    _uiState.value = _uiState.value.copy(isPublicFeed = !_uiState.value.isPublicFeed)

    viewModelScope.launch { loadCachedFeed() }

    refreshFeedFromFirestore()
  }

  /**
   * Sets the current account in the UI state.
   *
   * @param account The account to set as the current account.
   */
  fun setCurrentAccount(account: Account) {
    _uiState.value = _uiState.value.copy(currentAccount = account)
    loadCachedFeed()
  }

  /**
   * Sets an error message in the UI state.
   *
   * @param message The error message to set, or null to clear it.
   */
  fun setErrorMessage(message: String?) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }

  /**
   * Toggle like for a specific post for the current user.
   *
   * @param postId The ID of the post to toggle like for.
   */
  fun onToggleLike(postId: String) {
    val account = _uiState.value.currentAccount ?: return
    viewModelScope.launch {
      val current = _uiState.value
      val currentlyLiked = current.likes[postId] == true
      val currentCount = current.likeCounts[postId] ?: 0

      try {
        if (currentlyLiked) {
          likesRepository.unlikePost(postId, account.uid)
        } else {
          likesRepository.likePost(
              Like(
                  postId = postId,
                  postLikerId = account.uid,
                  timestamp = System.currentTimeMillis()))
        }

        val newLikes = current.likes.toMutableMap()
        val newCounts = current.likeCounts.toMutableMap()

        newLikes[postId] = !currentlyLiked
        newCounts[postId] = (currentCount + if (currentlyLiked) -1 else 1).coerceAtLeast(0)

        _uiState.value = current.copy(likes = newLikes, likeCounts = newCounts)
      } catch (e: Exception) {
        Log.e("FeedViewModel", "Failed to toggle like for post $postId", e)
      }
    }
  }
}
