package com.android.ootd.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
 */
class MapViewModelFactory(private val focusLocation: Location? = null) : ViewModelProvider.Factory {
  // Suppress unchecked cast: runtime isAssignableFrom check makes this cast safe.
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
      return MapViewModel(focusLocation = focusLocation) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
