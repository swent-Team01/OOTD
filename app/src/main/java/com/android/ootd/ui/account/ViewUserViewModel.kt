package com.android.ootd.ui.account

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViewUserData(
    val username: String = "",
    val profilePicture: String = "",
    val isFriend: Boolean = false,
    val friendPosts: List<OutfitPost> = emptyList(),
    val friendCount: Int = 0,
    val error: Boolean = false,
    val errorMsg: String? = null,
    val isLoading: Boolean = true
)

private const val currentLog = "ViewUserProfile"

class ViewUserViewModel(
    private val accountService: AccountService = AccountServiceFirebase(),
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val feedRepository: FeedRepository = FeedRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(ViewUserData())
  val uiState: StateFlow<ViewUserData> = _uiState.asStateFlow()

  fun update(friendId: String) {
    if (friendId.isNotBlank()) {
      refresh(friendId)
    }
  }

  private fun refresh(friendId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val currentUserId = accountService.currentUserId
        val isFriend = isMyFriend(currentUserId, friendId)
        val friend = userRepository.getUser(friendId)
        val friendPosts = postsToShow(friendId, isFriend)
        //        val friendCount = accountRepository.getFriends(friendId).size
        _uiState.update {
          it.copy(
              username = friend.username,
              profilePicture = friend.profilePicture,
              friendPosts = friendPosts,
              //              friendCount = friendCount,
              isLoading = false)
        }
      } catch (e: Exception) {
        Log.e(currentLog, "Failed to fetch user data ${e.message}")
        _uiState.update { it.copy(error = true, errorMsg = e.localizedMessage, isLoading = false) }
      }
    }
  }

  private suspend fun isMyFriend(userId: String, friendId: String): Boolean {
    val isFriend = accountRepository.isMyFriend(userId, friendId)
    _uiState.update { it.copy(isFriend = isFriend) }
    return isFriend
  }

  private suspend fun postsToShow(friendId: String, isFriend: Boolean): List<OutfitPost> {
    if (isFriend) {
      return feedRepository.getFeedForUids(listOf(friendId))
    }
    return emptyList()
  }
}
