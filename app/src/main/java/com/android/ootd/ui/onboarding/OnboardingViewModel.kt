package com.android.ootd.ui.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel for managing onboarding completion. */
class OnboardingViewModel(private val prefs: SharedPreferences) : ViewModel() {

  private val _hasConsented = MutableStateFlow(getConsentStatus())
  val hasConsented: StateFlow<Boolean> = _hasConsented.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _isInitializing = MutableStateFlow(true)
  val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

  private val _consentSaved = MutableStateFlow(false)
  val consentSaved: StateFlow<Boolean> = _consentSaved.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  companion object {
    const val PREFS_NAME = "ootd_onboarding"
    const val KEY_ONBOARDING_SEEN = "ootd_onboarding_seen"
    const val TAG = "OnboardingViewModel"
  }

  init {
    _isInitializing.value = false
  }

  /** Check if the user has previously given consent (from local cache) */
  fun getConsentStatus(): Boolean {
    return prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
  }

  /** Record that the user has completed onboarding */
  fun recordConsent() {
    viewModelScope.launch {
      _isLoading.value = true
      _consentSaved.value = false
      _error.value = null
      try {
        // Save to local preferences
        prefs.edit().apply {
          putBoolean(KEY_ONBOARDING_SEEN, true)
          apply()
        }

        _hasConsented.value = true
        _consentSaved.value = true
      } catch (e: Exception) {
        _error.value = "Failed to save onboarding state. Please try again."
        _consentSaved.value = false
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Reset the consent saved flag (for navigation purposes) */
  fun resetConsentSavedFlag() {
    _consentSaved.value = false
  }

  /** Clear the error message */
  fun clearError() {
    _error.value = null
  }

  /**
   * Clear consent (for testing or if user wants to reset) Removes from both Firebase and local
   * SharedPreferences
   */
  fun clearConsent() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null
      try {
        // Clear local preferences
        prefs.edit().apply {
          remove(KEY_ONBOARDING_SEEN)
          apply()
        }

        _hasConsented.value = false
      } catch (e: Exception) {
        _error.value = "Failed to reset onboarding flag. Please try again."
      } finally {
        _isLoading.value = false
      }
    }
  }
}
