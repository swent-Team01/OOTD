package com.android.ootd.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.map.isValidLocation
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.LocationUtils.toLatLng
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Map screen.
 *
 * @property posts The list of outfit posts to display on the map
 * @property userLocation The user's location from their account
 * @property focusLocation The location to focus on (if provided via navigation)
 * @property isLoading Whether the location is being loaded
 */
data class MapUiState(
    val posts: List<OutfitPost> = emptyList(),
    val currentAccount: Account? = null,
    val userLocation: Location = emptyLocation,
    val focusLocation: Location? = null,
    val isLoading: Boolean = true,
    val errorMsg: String? = null
)

/**
 * ViewModel for the Map screen.
 *
 * Loads the current user's location from their account and provides it for map centering. Observes
 * real-time updates to posts and user location from Firebase.
 */
class MapViewModel(
    private val feedRepository: FeedRepository = FeedRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    focusLocation: Location? = null
) : ViewModel() {

  private val _uiState = MutableStateFlow(MapUiState(focusLocation = focusLocation))
  val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

  // Job to observe posts; allows cancellation when account changes
  private var postsObserverJob: Job? = null

  init {
    observeAuthAndLoadAccount()
  }

  /** Observes Firebase Auth state changes and loads the current account accordingly. */
  private fun observeAuthAndLoadAccount() {
    Firebase.auth.addAuthStateListener { auth ->
      val user = auth.currentUser
      if (user != null) {
        loadAccountAndObservePosts(user.uid)
      }
    }
  }

  /** Loads account and starts observing posts in real-time. */
  private fun loadAccountAndObservePosts(userId: String) {
    viewModelScope.launch {
      try {
        // Observe account changes (including location updates)
        accountRepository.observeAccount(userId).collect { account ->
          _uiState.value =
              _uiState.value.copy(
                  currentAccount = account, userLocation = account.location, isLoading = false)

          // Cancel previous posts observer and start a new one
          postsObserverJob?.cancel()
          postsObserverJob = observeLocalizablePosts(account)
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                userLocation = emptyLocation,
                isLoading = false,
                errorMsg = "Failed to load account: ${e.message}")
      }
    }
  }

  /** Observe posts from friends in real-time, filtering for valid locations. */
  private fun observeLocalizablePosts(account: Account): Job {
    return viewModelScope.launch {
      try {
        val friendsAndAccountUid = account.friendUids + account.uid

        feedRepository.observeRecentFeedForUids(friendsAndAccountUid).collect { posts ->
          val validPosts = posts.filter { isValidLocation(it.location) }
          _uiState.value = _uiState.value.copy(posts = validPosts)
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(errorMsg = "Failed to load posts for map: ${e.message}")
      }
    }
  }

  /** Get user's current location as LatLng for map centering. */
  fun getUserLatLng(): LatLng {
    return _uiState.value.userLocation.toLatLng()
  }

  /** Get the location to focus on (either the provided focus location or user's location). */
  fun getFocusLatLng(): LatLng {
    return _uiState.value.focusLocation?.toLatLng() ?: _uiState.value.userLocation.toLatLng()
  }
}
