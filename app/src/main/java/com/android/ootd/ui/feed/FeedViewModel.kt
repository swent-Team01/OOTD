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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val likeCounts: Map<String, Int> = emptyMap()
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
    private val likesRepository: LikesRepository = LikesRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(FeedUiState())
  val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

  init {
    observeAuthAndLoadAccount()
  }

  /** Observes Firebase Auth state changes and loads the current account accordingly. */
  private fun observeAuthAndLoadAccount() {
    Firebase.auth.addAuthStateListener { auth ->
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

  /** Refreshes the feed posts from Firestore for the current account. */
  fun refreshFeedFromFirestore() {
    val account = _uiState.value.currentAccount ?: return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)

      val allUids = account.friendUids + account.uid
      try {
        // STEP 1 : Load initial feed posts
        val hasPosted = repository.hasPostedToday(account.uid)
        val posts = repository.getRecentFeedForUids(allUids)

        val filteredPost = filterPosts(account.uid, posts)
        updateLikesForPosts(filteredPost)

        _uiState.value =
            _uiState.value.copy(
                hasPostedToday = hasPosted, feedPosts = filteredPost, isLoading = false)

        // STEP 2 - Background refresh with timeout
        viewModelScope.launch {
          try {
            val freshPosts =
                withTimeoutOrNull(2000L) { repository.getRecentFeedForUids(allUids) }
                    ?: return@launch // keep cached data if timeout / offline

            // If nothing changed, no need to touch UI

            if (freshPosts == posts) return@launch

            val filteredFresh = filterPosts(account.uid, freshPosts)
            updateLikesForPosts(filteredFresh)

            _uiState.value = _uiState.value.copy(feedPosts = filteredFresh)
          } catch (e: Exception) {
            Log.e("FeedViewModel", "Failed to refresh feed likes/counts", e)
            // User already sees cached datta, so we don't surface an error
          }
        }
      } catch (e: Exception) {
        Log.e("FeedViewModel", "Failed to load initial feed", e)
        // Only show error if we have no posts at all
        val current = _uiState.value
        _uiState.value =
            current.copy(
                isLoading = false,
                errorMessage =
                    if (current.feedPosts.isEmpty()) "Failed to load feed"
                    else current.errorMessage)
        return@launch
      }
    }
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

  private fun filterPosts(userId: String, posts: List<OutfitPost>): List<OutfitPost> {
    val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Filter posts to only include today's post for the current user and all posts for friends
    return posts.filter { post ->
      if (post.ownerId == userId) {
        post.timestamp >= todayStart
      } else true
    }
  }

  private suspend fun updateLikesForPosts(posts: List<OutfitPost>) {
    val account = _uiState.value.currentAccount ?: return

    // Load likes and like counts for these posts for the current user
    val likesMap = mutableMapOf<String, Boolean>()
    val countsMap = mutableMapOf<String, Int>()
    for (post in posts) {
      val postId = post.postUID
      try {
        val isLiked = likesRepository.isPostLikedByUser(postId, account.uid)
        val count = likesRepository.getLikeCount(postId)
        likesMap[postId] = isLiked
        countsMap[postId] = count
      } catch (e: Exception) {
        Log.e("FeedViewModel", "Failed to load likes for post $postId", e)
      }
    }

    _uiState.value = _uiState.value.copy(likes = likesMap, likeCounts = countsMap)
  }
}
