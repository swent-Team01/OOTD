package com.android.ootd.ui.consent

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.consent.Consent
import com.android.ootd.model.consent.ConsentRepository
import com.android.ootd.model.consent.ConsentRepositoryProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing beta consent state.
 *
 * Uses both Firebase Firestore (for permanent storage) and SharedPreferences (for local caching) to
 * persist the user's consent decision across app sessions.
 *
 * @param prefs SharedPreferences instance for local consent caching
 * @param consentRepository Repository for Firebase Firestore operations
 */
class BetaConsentViewModel(
    private val prefs: SharedPreferences,
    private val consentRepository: ConsentRepository = ConsentRepositoryProvider.repository
) : ViewModel() {

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
    const val PREFS_NAME = "ootd_beta_consent"
    const val KEY_CONSENT_GIVEN = "ootd_consent_given"
    const val KEY_CONSENT_TIMESTAMP = "ootd_consent_timestamp"
    const val KEY_CONSENT_UUID = "ootd_consent_uuid"
    const val TERMS_VERSION = "1.0"
    const val TAG = "BetaConsentViewModel"
  }

  init {
    // Check Firebase for consent status on initialization
    checkFirebaseConsent()
  }

  /**
   * Check if the user has previously given consent in Firebase and sync with local
   * SharedPreferences
   */
  private fun checkFirebaseConsent() {
    viewModelScope.launch {
      _isInitializing.value = true
      try {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
          val consent = consentRepository.getConsentByUserId(userId)
          if (consent != null) {
            // Sync Firebase data to local preferences
            syncToLocalPreferences(consent)
            _hasConsented.value = true
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error checking Firebase consent: ${e.message}", e)
        _error.value = "Failed to load consent status. Please check your connection."
      } finally {
        _isInitializing.value = false
      }
    }
  }

  /** Sync consent data from Firebase to local SharedPreferences */
  private fun syncToLocalPreferences(consent: Consent) {
    prefs.edit().apply {
      putBoolean(KEY_CONSENT_GIVEN, true)
      putLong(KEY_CONSENT_TIMESTAMP, consent.timestamp)
      putString(KEY_CONSENT_UUID, consent.consentUuid)
      apply()
    }
  }

  /** Check if the user has previously given consent (from local cache) */
  fun getConsentStatus(): Boolean {
    return prefs.getBoolean(KEY_CONSENT_GIVEN, false)
  }

  /**
   * Record that the user has agreed to beta terms Saves to both Firebase and local
   * SharedPreferences
   */
  fun recordConsent() {
    viewModelScope.launch {
      _isLoading.value = true
      _consentSaved.value = false
      _error.value = null
      try {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
          Log.e(TAG, "Cannot record consent: User not authenticated")
          _error.value = "You must be signed in to provide consent"
          _isLoading.value = false
          return@launch
        }

        val timestamp = System.currentTimeMillis()
        val consentUuid = consentRepository.getNewConsentId()

        val consent =
            Consent(
                consentUuid = consentUuid,
                userId = userId,
                timestamp = timestamp,
                version = TERMS_VERSION)

        // Save to Firebase
        consentRepository.addConsent(consent)

        // Save to local preferences
        prefs.edit().apply {
          putBoolean(KEY_CONSENT_GIVEN, true)
          putLong(KEY_CONSENT_TIMESTAMP, timestamp)
          putString(KEY_CONSENT_UUID, consentUuid)
          apply()
        }

        _hasConsented.value = true
        _consentSaved.value = true
        Log.i(TAG, "Consent recorded successfully: $consentUuid")
      } catch (e: Exception) {
        Log.e(TAG, "Error recording consent: ${e.message}", e)
        _error.value = "Failed to save consent. Please try again."
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
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
          // Delete from Firebase
          consentRepository.deleteConsentByUserId(userId)
        }

        // Clear local preferences
        prefs.edit().apply {
          remove(KEY_CONSENT_GIVEN)
          remove(KEY_CONSENT_TIMESTAMP)
          remove(KEY_CONSENT_UUID)
          apply()
        }

        _hasConsented.value = false
        Log.i(TAG, "Consent cleared successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Error clearing consent: ${e.message}", e)
        _error.value = "Failed to withdraw consent. Please try again."
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Get the timestamp when consent was given (or null if not given) */
  fun getConsentTimestamp(): Long? {
    val timestamp = prefs.getLong(KEY_CONSENT_TIMESTAMP, -1L)
    return if (timestamp != -1L) timestamp else null
  }

  /** Get the UUID of the consent record (or null if not given) */
  fun getConsentUuid(): String? {
    val uuid = prefs.getString(KEY_CONSENT_UUID, null)
    return if (uuid?.isNotBlank() == true) uuid else null
  }
}
