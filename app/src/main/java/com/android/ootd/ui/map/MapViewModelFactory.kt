package com.android.ootd.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.map.Location

/**
 * Factory for creating MapViewModel instances with a specific focus location.
 *
 * This factory is required when navigating to the map with a specific location (e.g., when a user
 * clicks on a post's location in the feed). Compose's viewModel() function cannot pass constructor
 * parameters directly, so this factory allows injecting the focusLocation parameter into the
 * MapViewModel.
 *
 * When focusLocation is provided, the map will center on that location instead of the user's
 * current location. This enables the "click location to view on map" feature throughout the app.
 *
 * Disclaimer: This code was written with the assistance of AI.
 *
 * @param focusLocation The location to focus on when the map loads. If null, the map will center on
 *   the user's account location (default behavior).
 * @param initialMapType The initial map type to display (FRIENDS_POSTS or FIND_FRIENDS).
 * @param feedRepository Optional feed repository for dependency injection (mainly for testing).
 * @param accountRepository Optional account repository for dependency injection (mainly for
 *   testing).
 */
class MapViewModelFactory(
    private val focusLocation: Location? = null,
    private val initialMapType: MapType = MapType.FRIENDS_POSTS,
    private val feedRepository: FeedRepository? = null,
    private val accountRepository: AccountRepository? = null
) : ViewModelProvider.Factory {
  // Suppress unchecked cast: runtime isAssignableFrom check makes this cast safe.
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
      return MapViewModel(
          feedRepository = feedRepository ?: FeedRepositoryProvider.repository,
          accountRepository = accountRepository ?: AccountRepositoryProvider.repository,
          focusLocation = focusLocation,
          initialMapType = initialMapType)
          as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
