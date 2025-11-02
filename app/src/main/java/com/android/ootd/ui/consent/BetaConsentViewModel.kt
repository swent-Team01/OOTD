package com.android.ootd.ui.consent

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing beta consent state.
 *
 * Uses SharedPreferences to persist the user's consent decision across app sessions.
 */
class BetaConsentViewModel(private val context: Context) : ViewModel() {

  private val prefs: SharedPreferences =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _hasConsented = MutableStateFlow(getConsentStatus())
  val hasConsented: StateFlow<Boolean> = _hasConsented.asStateFlow()

  companion object {
    private const val PREFS_NAME = "OOTD_beta_consent"
    private const val KEY_CONSENT_GIVEN = "consent_given"
    private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"
  }

  /** Check if the user has previously given consent */
  fun getConsentStatus(): Boolean {
    return prefs.getBoolean(KEY_CONSENT_GIVEN, false)
  }

  /** Record that the user has agreed to beta terms */
  fun recordConsent() {
    prefs.edit().apply {
      putBoolean(KEY_CONSENT_GIVEN, true)
      putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
      apply()
    }
    _hasConsented.value = true
  }

  /** Clear consent (for testing or if user wants to reset) */
  fun clearConsent() {
    prefs.edit().apply {
      remove(KEY_CONSENT_GIVEN)
      remove(KEY_CONSENT_TIMESTAMP)
      apply()
    }
    _hasConsented.value = false
  }

  /** Get the timestamp when consent was given (or null if not given) */
  fun getConsentTimestamp(): Long? {
    val timestamp = prefs.getLong(KEY_CONSENT_TIMESTAMP, -1L)
    return if (timestamp != -1L) timestamp else null
  }
}
