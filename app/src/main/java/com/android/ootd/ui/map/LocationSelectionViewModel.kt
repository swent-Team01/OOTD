package com.android.ootd.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.map.LocationRepositoryProvider
import com.android.ootd.utils.LocationUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationSelectionViewState(
    val selectedLocation: Location? = null,
    val locationQuery: String = "",
    val locationSuggestions: List<Location> = emptyList(),
    val isLoadingLocations: Boolean = false
)

class LocationSelectionViewModel(
    private val locationRepository: LocationRepository = LocationRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(LocationSelectionViewState())

  /**
   * A [StateFlow] representing the current location selection UI state. Observers can collect this
   * flow to react to state changes.
   */
  val uiState: StateFlow<LocationSelectionViewState> = _uiState.asStateFlow()

  // Add this property to track the search job
  private var searchJob: Job? = null

  /**
   * Selects a location and updates the UI state.
   *
   * @param location The chosen Location; also sets the location query to the location's name.
   */
  fun setLocation(location: Location) {
    _uiState.value = _uiState.value.copy(selectedLocation = location, locationQuery = location.name)
  }

  /**
   * Updates the location search query and fetches suggestions.
   *
   * If the query is non-empty a background search is started and the resulting suggestions are
   * stored in the UI state; otherwise suggestions are cleared.
   *
   * Disclaimer: This method has been adapted from the bootcamp week 3 solution
   *
   * @param query The new location query string.
   */
  fun setLocationQuery(query: String) {
    _uiState.value = _uiState.value.copy(locationQuery = query, selectedLocation = null)

    // Cancel any pending search
    searchJob?.cancel()

    if (query.isNotEmpty()) {
      _uiState.value = _uiState.value.copy(isLoadingLocations = true)
      searchJob =
          viewModelScope.launch {
            delay(500) // Wait 500ms after user stops typing in order to not flood nomatim.
            try {
              val results = locationRepository.search(query)
              _uiState.value =
                  _uiState.value.copy(locationSuggestions = results, isLoadingLocations = false)
            } catch (e: Exception) {
              Log.e("LocationSelectionViewModel", "Error fetching location suggestions", e)
              _uiState.value =
                  _uiState.value.copy(locationSuggestions = emptyList(), isLoadingLocations = false)
            }
          }
    } else {
      _uiState.value =
          _uiState.value.copy(locationSuggestions = emptyList(), isLoadingLocations = false)
    }
  }

  /**
   * Clears the location suggestions from the UI state.
   *
   * This method can be called to reset the location suggestions, for example, when the user clears
   * the location query.
   */
  fun clearLocationSuggestions() {
    _uiState.value = _uiState.value.copy(locationSuggestions = emptyList())
  }

  /**
   * Sets location suggestions directly in the UI state.
   *
   * This is primarily used for testing purposes to inject mock location data.
   *
   * @param suggestions The list of locations to set as suggestions.
   */
  fun setLocationSuggestions(suggestions: List<Location>) {
    _uiState.value = _uiState.value.copy(locationSuggestions = suggestions)
  }

  /**
   * Sets the loading state for location operations.
   *
   * This is primarily used for GPS location retrieval where loading state needs to be managed
   * externally.
   *
   * @param isLoading True to show loading indicator, false to hide it.
   */
  fun setLoadingState(isLoading: Boolean) {
    _uiState.value = _uiState.value.copy(isLoadingLocations = isLoading)
  }

  fun isLoadingLocations(): Boolean {
    return uiState.value.locationSuggestions.isEmpty() && uiState.value.locationQuery.isNotEmpty()
  }

  /**
   * Called when location permission is granted by the user. Initiates GPS location retrieval.
   *
   * @param onError Optional callback to handle errors. Receives the error message as a parameter.
   */
  @Suppress("MissingPermission")
  fun onLocationPermissionGranted(onError: ((String) -> Unit)? = null) {
    // Set loading state before starting GPS retrieval
    setLoadingState(true)

    viewModelScope.launch {
      try {
        LocationUtils.getCurrentGPSLocation(
            onSuccess = { location: Location ->
              setLocation(location)
              setLoadingState(false)
            },
            onFailure = { errorMessage: String ->
              Log.e("LocationSelectionViewModel", "Error getting GPS location: $errorMessage")
              setLoadingState(false)
              onError?.invoke(errorMessage.ifBlank { "Failed to get current location" })
            })
      } catch (e: Exception) {
        Log.e("LocationSelectionViewModel", "Error initiating GPS location", e)
        setLoadingState(false)
        onError?.invoke(e.message ?: "Failed to get current location")
      }
    }
  }

  /**
   * Called when location permission is denied by the user.
   *
   * @param onDenied Optional callback to handle the permission denial. If not provided, no action
   *   is taken.
   */
  fun onLocationPermissionDenied(onDenied: (() -> Unit)? = null) {
    onDenied?.invoke()
  }
}
