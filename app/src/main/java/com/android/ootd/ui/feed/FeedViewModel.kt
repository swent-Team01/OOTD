package com.android.ootd.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryProvider
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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

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
    private val outfitPostRepository: OutfitPostRepository =
        OutfitPostRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val likesRepository: LikesRepository = LikesRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private companion object {
    const val NETWORK_TIMEOUT_MILLIS = 2000L
  }

  private val _uiState = MutableStateFlow(FeedUiState())
  val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

  // Indicates whether a refresh operation is in progress
  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  // Cache for public feed posts to use when the app is in offline mode
  private var cachedPublicFeed: List<OutfitPost> = emptyList()

  // Cache for private feed posts to use when the app is in offline mode
  private var cachedPrivateFeed: List<OutfitPost> = emptyList()

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
   * Triggers a refresh of the feed posts. This function launches a coroutine in the ViewModel scope
   * to perform the refresh operation. If an exception occurs during the refresh, it logs the error
   * and updates the UI state with an error message.
   */
  fun doRefreshFeed() {
    viewModelScope.launch {
      try {
        refreshFeed()
      } catch (e: Exception) {
        Log.e("FeedViewModel", "Failed to refresh feed", e)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = "Failed to refresh feed: ${e.message}")
      }
    }
  }

  /**
   * Refreshes the feed posts from Firestore and fetches the posts locally for the current account
   * and updates the UI state accordingly. So that, it can also work in offline mode using cached
   * data. This function performs the following steps:
   * 1. Loads cached posts from Firestore local cache and updates the UI state immediately.
   * 2. Filter the posts based on whether we are in public or private feed mode and based on user
   *    ID.
   * 3. Checks if the user has posted today based on cached data.
   * 4. Attempts to fetch fresh posts from Firestore with a short timeout. If successful, updates
   *    the UI state with the fresh data.
   * 5. Filters the fresh posts similarly and checks if the user has posted today based on database
   *    data.
   * 6. Fetches like status and counts for the posts.
   * 7. Updates the UI state with the final data including whether the user has posted today.
   */
  private suspend fun refreshFeed() {
    val account = _uiState.value.currentAccount ?: return

    val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // 1) Prefill from Firestore local cache immediately
    val publicCached = repository.getCachedPublicFeed()
    val privateCached = repository.getCachedFriendFeed(account.friendUids + account.uid)

    val cached = filterPosts(publicCached, account.uid, todayStart, privateCached)

    if (cached.isEmpty() && !_isRefreshing.value) {
      _uiState.value = _uiState.value.copy(isLoading = true)
    }

    cachedPublicFeed = publicCached
    cachedPrivateFeed = privateCached

    val hasPostedTodayLocal =
        (cachedPublicFeed + cachedPrivateFeed).any { post ->
          post.ownerId == account.uid && post.timestamp >= todayStart
        }

    if (cached.isNotEmpty()) {
      _uiState.value =
          _uiState.value.copy(
              feedPosts = cached, isLoading = false, hasPostedToday = hasPostedTodayLocal)
    }

    val allPosts =
        withTimeoutOrNull(NETWORK_TIMEOUT_MILLIS) {
          if (_uiState.value.isPublicFeed) {
            repository.getPublicFeed()
          } else {
            repository.getRecentFeedForUids(account.friendUids + account.uid)
          }
        }

    if (allPosts == null) {
      _uiState.value = _uiState.value.copy(hasPostedToday = hasPostedTodayLocal, isLoading = false)
      return
    }

    val filteredPost = filterPosts(allPosts, account.uid, todayStart, allPosts)

    updateCachedPosts(filteredPost)

    val finalHasPostedToday = computeHasPostedToday(account.uid, hasPostedTodayLocal)

    val (likesMap, likeCounts) = fetchLikesForPosts(filteredPost, account)

    _uiState.value =
        _uiState.value.copy(
            hasPostedToday = finalHasPostedToday,
            feedPosts = filteredPost,
            isLoading = false,
            likes = likesMap,
            likeCounts = likeCounts)
  }

  /**
   * Updates the cached posts based on in which screen we are.
   *
   * @param posts The list of posts to cache.
   */
  private fun updateCachedPosts(posts: List<OutfitPost>) {
    if (_uiState.value.isPublicFeed) {
      cachedPublicFeed = posts
    } else {
      cachedPrivateFeed = posts
    }
  }

  /**
   * Fetches like status and counts for the given posts and account.
   *
   * @param posts The list of posts to fetch likes for.
   * @param account The account for which to fetch like status.
   * @return A pair of maps: the first map contains post IDs to like status (Boolean), and the
   *   second map contains post IDs to like counts (Int).
   */
  private suspend fun fetchLikesForPosts(
      posts: List<OutfitPost>,
      account: Account
  ): Pair<Map<String, Boolean>, Map<String, Int>> {
    val likesMap = mutableMapOf<String, Boolean>()
    val likeCounts = mutableMapOf<String, Int>()

    for (post in posts) {
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
    return Pair(likesMap, likeCounts)
  }

  /**
   * Filters posts based on the feed type and user ID.
   *
   * @param publicPost The list of posts to filter.
   * @param uid The user ID of the current account.
   * @param time The timestamp representing the start of today.
   * @param privatePost The list of private posts to filter (optional).
   * @return The filtered list of posts.
   */
  private fun filterPosts(
      publicPost: List<OutfitPost>,
      uid: String,
      time: Long,
      privatePost: List<OutfitPost> = emptyList()
  ): List<OutfitPost> {
    return if (_uiState.value.isPublicFeed) publicPost
    else
        privatePost.filter { post ->
          if (post.ownerId == uid) {
            post.timestamp >= time
          } else true
        }
  }

  /**
   * Computes whether the user has posted today considering both cached and database data.
   *
   * @param uid The user ID of the current account.
   * @return True if the user has posted today, false otherwise.
   */
  private suspend fun computeHasPostedToday(uid: String, hasPostedTodayLocal: Boolean): Boolean {
    val hasPostedToday =
        withTimeoutOrNull(NETWORK_TIMEOUT_MILLIS) { repository.hasPostedToday(uid) } ?: false

    return hasPostedToday || hasPostedTodayLocal
  }

  /** Toggles between public and private feed. */
  fun toggleFeedType() {
    _uiState.value = _uiState.value.copy(isPublicFeed = !_uiState.value.isPublicFeed)

    viewModelScope.launch { refreshFeed() }
  }

  /**
   * Sets the current account in the UI state.
   *
   * @param account The account to set as the current account.
   */
  fun setCurrentAccount(account: Account) {
    _uiState.value = _uiState.value.copy(currentAccount = account)
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

  /**
   * Refreshes a single post in the feed after comment changes.
   *
   * @param postId The ID of the post to refresh
   */
  fun refreshPost(postId: String) {
    viewModelScope.launch {
      try {
        val updatedPost = outfitPostRepository.getPostById(postId) ?: return@launch
        val currentPosts = _uiState.value.feedPosts.toMutableList()
        val index = currentPosts.indexOfFirst { it.postUID == postId }

        if (index != -1) {
          currentPosts[index] = updatedPost
          _uiState.value = _uiState.value.copy(feedPosts = currentPosts)
        }
      } catch (e: Exception) {
        Log.e("FeedViewModel", "Failed to refresh post $postId", e)
      }
    }
  }

  /**
   * Handles pull-to-refresh action by the user.
   *
   * Sets the refreshing state and triggers a feed refresh. If a refresh is already in progress, it
   * avoids starting another one. This call avoids multiple simultaneous refreshes.
   */
  fun onPullToRefreshTrigger() {
    // Avoid multiple simultaneous refreshes
    if (_isRefreshing.value) return
    _isRefreshing.value = true
    viewModelScope.launch {
      try {
        withTimeout(NETWORK_TIMEOUT_MILLIS) { refreshFeed() }
      } catch (e: Exception) {
        Log.e("FeedViewModel", "Pull-to-refresh failed", e)
      } finally {
        _isRefreshing.value = false
      }
    }
  }
}
