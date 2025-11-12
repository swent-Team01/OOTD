package com.android.ootd.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
    val errorMessage: String? = null
)

/**
 * ViewModel for the FeedScreen.
 *
 * Responsible for managing the state by fetching and providing data for feed posts from the
 * [FeedRepository] and account data from the [AccountRepository].
 */
open class FeedViewModel(
    private val repository: FeedRepository = FeedRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(FeedUiState(isLoading = true))
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
      try {
        val hasPosted = repository.hasPostedToday(account.uid)
        val posts = repository.getRecentFeedForUids(account.friendUids + account.uid)

        val todayStart =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Filter posts to only include today's post for the current user and all posts for friends
        val filteredPost =
            posts.filter { post ->
              if (post.ownerId == account.uid) {
                post.timestamp >= todayStart
              } else true
            }

        _uiState.value =
            _uiState.value.copy(
                hasPostedToday = hasPosted, feedPosts = filteredPost, isLoading = false)
      } catch (e: Exception) {
        Log.e("FeedViewModel", "Failed to refresh feed", e)
        _uiState.value =
            _uiState.value.copy(isLoading = false, errorMessage = "Failed to load feed")
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

  fun setErrorMessage(message: String?) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }
}
