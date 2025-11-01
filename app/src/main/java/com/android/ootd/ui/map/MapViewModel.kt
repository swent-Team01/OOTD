package com.android.ootd.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Map screen.
 *
 * @property userLocation The user's location from their account
 * @property isLoading Whether the location is being loaded
 */
data class MapUiState(val userLocation: Location = emptyLocation, val isLoading: Boolean = true)

/**
 * ViewModel for the Map screen.
 *
 * Loads the current user's location from their account and provides it for map centering.
 */
class MapViewModel(
    private val accountService: AccountService = AccountServiceFirebase(),
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(MapUiState())
  val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

  init {
    loadUserLocation()
  }

  /** Load the current user's location from their account. */
  private fun loadUserLocation() {
    viewModelScope.launch {
      try {
        val userId = accountService.currentUserId
        val account = accountRepository.getAccount(userId)
        _uiState.value = MapUiState(userLocation = account.location, isLoading = false)
      } catch (e: Exception) {
        // If loading fails, use empty location and stop loading
        _uiState.value = MapUiState(userLocation = emptyLocation, isLoading = false)
      }
    }
  }

  /** Convert the user's location to LatLng for Google Maps. */
  fun getUserLatLng(): LatLng {
    val location = _uiState.value.userLocation
    return LatLng(location.latitude, location.longitude)
  }
}
