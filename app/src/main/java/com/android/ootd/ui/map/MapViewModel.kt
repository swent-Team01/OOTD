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
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing a post with an adjusted location to prevent marker overlap.
 *
 * @property post The original outfit post
 * @property adjustedLocation The location adjusted to prevent overlap with other markers
 * @property overlappingCount The number of posts at the same original location
 */
data class PostWithAdjustedLocation(
    val post: OutfitPost,
    val adjustedLocation: Location,
    val overlappingCount: Int
)

/**
 * UI state for the Map screen.
 *
 * @property posts The list of outfit posts to display on the map
 * @property userLocation The user's location from their account
 * @property isLoading Whether the location is being loaded
 */
data class MapUiState(
    val posts: List<OutfitPost> = emptyList(),
    val currentAccount: Account? = null,
    val userLocation: Location = emptyLocation,
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
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(MapUiState())
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

  /**
   * Get posts with adjusted locations to prevent overlapping markers. When multiple posts share the
   * same location, they are offset in a circular pattern.
   */
  fun getPostsWithAdjustedLocations(): List<PostWithAdjustedLocation> {
    val posts = _uiState.value.posts
    val locationGroups = posts.groupBy { "${it.location.latitude},${it.location.longitude}" }

    return posts.map { post ->
      val locationKey = "${post.location.latitude},${post.location.longitude}"
      val postsAtSameLocation = locationGroups[locationKey] ?: listOf(post)
      val overlappingCount = postsAtSameLocation.size

      if (overlappingCount == 1) {
        // No overlap, use original location
        PostWithAdjustedLocation(post, post.location, 1)
      } else {
        // Multiple posts at same location - offset them in a circle
        val index = postsAtSameLocation.indexOf(post)
        val adjustedLocation = offsetLocation(post.location, index, overlappingCount)
        PostWithAdjustedLocation(post, adjustedLocation, overlappingCount)
      }
    }
  }

  /**
   * Offset a location by a small amount to prevent marker overlap. Markers are arranged in a
   * circular pattern around the original location.
   *
   * @param location The original location
   * @param index The index of this marker among overlapping markers
   * @param total The total number of overlapping markers
   * @return The adjusted location
   */
  private fun offsetLocation(location: Location, index: Int, total: Int): Location {
    // Offset by approximately 30 meters (0.0003 degrees is roughly 30m at equator)
    val offsetDegrees = 0.0003
    val angle = (2 * Math.PI * index) / total

    val latOffset = offsetDegrees * sin(angle)
    // longitude does not directly corresponds to ground distance and must thus be scaled
    val lonOffset = offsetDegrees * cos(angle) / cos(Math.toRadians(location.latitude))

    return Location(
        latitude = location.latitude + latOffset,
        longitude = location.longitude + lonOffset,
        name = location.name)
  }
}
