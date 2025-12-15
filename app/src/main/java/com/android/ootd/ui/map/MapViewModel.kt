package com.android.ootd.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.account.PublicLocation
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Enum representing the type of map to display. */
enum class MapType {
  FRIENDS_POSTS,
  FIND_FRIENDS
}

/**
 * UI state for the Map screen.
 *
 * @property posts The list of outfit posts to display on the map
 * @property publicLocations The list of public locations to display on the find friends map
 * @property userLocation The user's location from their account
 * @property focusLocation The location to focus on (if provided via navigation)
 * @property isLoading Whether the location is being loaded
 * @property selectedMapType The currently selected map type
 */
data class MapUiState(
    val posts: List<OutfitPost> = emptyList(),
    val publicLocations: List<PublicLocation> = emptyList(),
    val currentAccount: Account? = null,
    val userLocation: Location = emptyLocation,
    val focusLocation: Location? = null,
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
    val selectedMapType: MapType = MapType.FRIENDS_POSTS,
    val snackbarMessage: String? = null
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
    focusLocation: Location? = null,
    initialMapType: MapType = MapType.FRIENDS_POSTS
) : ViewModel() {

  private val _uiState =
      MutableStateFlow(MapUiState(focusLocation = focusLocation, selectedMapType = initialMapType))
  val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

  // Job to observe posts; allows cancellation when account changes
  private var postsObserverJob: Job? = null
  private var publicLocationsObserverJob: Job? = null

  init {
    observeAuthAndLoadAccount()
    observePublicLocations()
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

  /** Observe public locations in real-time for the Find Friends map. */
  private fun observePublicLocations() {
    publicLocationsObserverJob =
        viewModelScope.launch {
          accountRepository
              .observePublicLocations()
              .catch { e ->
                _uiState.value =
                    _uiState.value.copy(errorMsg = "Failed to load public locations: ${e.message}")
              }
              .combine(_uiState) { publicLocations, state ->
                val currentAccount = state.currentAccount
                val excludedUids =
                    if (currentAccount != null) {
                      // Exclude current user and their friends
                      setOf(currentAccount.uid) + currentAccount.friendUids
                    } else {
                      emptySet()
                    }

                publicLocations.filter {
                  isValidLocation(it.location) && it.ownerId !in excludedUids
                }
              }
              .collect { filteredPublicLocations ->
                _uiState.value = _uiState.value.copy(publicLocations = filteredPublicLocations)
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

  /**
   * Get posts with adjusted locations to prevent overlapping markers. When multiple posts share the
   * same location, they are offset in a circular pattern.
   */
  fun getPostsWithAdjustedLocations(posts: List<OutfitPost>): List<PostMarker> {
    return posts
        .map { it.asLocatable }
        .withAdjustedLocations()
        .zip(posts)
        .map { (adjusted, post) -> PostMarker(post, adjusted.adjustedLocation) }
  }

  /**
   * Get public locations with adjusted positions to prevent overlapping markers. When multiple
   * public locations share the same location, they are offset in a circular pattern.
   */
  fun getPublicLocationsWithAdjusted(
      publicLocations: List<PublicLocation>
  ): List<PublicProfileMarker> {
    return publicLocations
        .map { it.asLocatable }
        .withAdjustedLocations()
        .zip(publicLocations)
        .map { (adjusted, publicLocation) ->
          PublicProfileMarker(publicLocation, adjusted.adjustedLocation)
        }
  }

  /**
   * Generic function to adjust locations for any list of locatable items. When multiple items share
   * the same location, they are offset in a circular pattern.
   */
  private fun <T : Locatable> List<T>.withAdjustedLocations(): List<ItemWithAdjustedLocation<T>> {
    val locationGroups = this.groupBy { "${it.location.latitude},${it.location.longitude}" }

    return this.map { item ->
      val locationKey = "${item.location.latitude},${item.location.longitude}"
      val itemsAtSameLocation = locationGroups[locationKey] ?: listOf(item)
      val overlappingCount = itemsAtSameLocation.size

      if (overlappingCount == 1) {
        // No overlap, use original location
        ItemWithAdjustedLocation(item, item.location, 1)
      } else {
        // Multiple items at same location - offset them in a circle
        val index = itemsAtSameLocation.indexOf(item)
        val adjustedLocation = offsetLocation(item.location, index, overlappingCount)
        ItemWithAdjustedLocation(item, adjustedLocation, overlappingCount)
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

  /** Switch between Friends Posts and Public Profiles map. */
  fun setMapType(mapType: MapType) {
    _uiState.value = _uiState.value.copy(selectedMapType = mapType)
  }

  /** Check if the user has posted today based on the current account's posts. */
  suspend fun hasUserPostedToday(): Boolean {
    val currentUserId = Firebase.auth.currentUser?.uid ?: return false
    return try {
      feedRepository.hasPostedToday(currentUserId)
    } catch (_: Exception) {
      false
    }
  }

  /** Show a snackbar message. */
  fun showSnackbar(message: String) {
    _uiState.value = _uiState.value.copy(snackbarMessage = message)
  }

  /** Clear the snackbar message. */
  fun clearSnackbar() {
    _uiState.value = _uiState.value.copy(snackbarMessage = null)
  }
}

/** Interface for items that have a location and can be adjusted to prevent overlap. */
interface Locatable {
  val location: Location
}

/**
 * Generic wrapper for items with adjusted locations to prevent marker overlap.
 *
 * @property item The original item with a location
 * @property adjustedLocation The location adjusted to prevent overlap with other markers
 * @property overlappingCount The number of items at the same original location
 */
data class ItemWithAdjustedLocation<T : Locatable>(
    val item: T,
    val adjustedLocation: Location,
    val overlappingCount: Int
)

// Extension to make OutfitPost locatable
private val OutfitPost.asLocatable: Locatable
  get() =
      object : Locatable {
        override val location: Location = this@asLocatable.location
      }

// Extension to make PublicLocation locatable
private val PublicLocation.asLocatable: Locatable
  get() =
      object : Locatable {
        override val location: Location = this@asLocatable.location
      }
