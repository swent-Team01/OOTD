package com.android.ootd.ui.consent

import android.content.Context
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
 */
class BetaConsentViewModel(
    private val context: Context,
    private val consentRepository: ConsentRepository = ConsentRepositoryProvider.repository
) : ViewModel() {

  private val prefs: SharedPreferences =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _hasConsented = MutableStateFlow(getConsentStatus())
  val hasConsented: StateFlow<Boolean> = _hasConsented.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _consentSaved = MutableStateFlow(false)
  val consentSaved: StateFlow<Boolean> = _consentSaved.asStateFlow()

  companion object {
    private const val PREFS_NAME = "OOTD_beta_consent"
    private const val KEY_CONSENT_GIVEN = "consent_given"
    private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"
    private const val KEY_CONSENT_UUID = "consent_uuid"
    private const val TERMS_VERSION = "1.0"
    private const val TAG = "BetaConsentViewModel"
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
      try {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
          Log.e(TAG, "Cannot record consent: User not authenticated")
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
        Log.d(TAG, "Consent recorded successfully: $consentUuid")
      } catch (e: Exception) {
        Log.e(TAG, "Error recording consent: ${e.message}", e)
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

  /**
   * Clear consent (for testing or if user wants to reset) Removes from both Firebase and local
   * SharedPreferences
   */
  fun clearConsent() {
    viewModelScope.launch {
      _isLoading.value = true
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
        Log.d(TAG, "Consent cleared successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Error clearing consent: ${e.message}", e)
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
