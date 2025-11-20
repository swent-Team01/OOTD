package com.android.ootd

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

/**
 * Custom Application class for OOTD.
 *
 * This class is responsible for initializing Firebase Firestore with offline persistence enabled.
 * When disk persistence is enabled, Firestore automatically caches data locally, allowing the app
 * to work offline. Any writes made while offline are queued and sent to the server when
 * connectivity is restored.
 */
class OOTDApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    try {
      // Enable Firestore offline persistence with disk cache
      val settings = firestoreSettings { setLocalCacheSettings(persistentCacheSettings {}) }

      Firebase.firestore.firestoreSettings = settings

      Log.d(TAG, "Firestore offline persistence enabled successfully")
    } catch (e: Exception) {
      // Log error but don't crash the app
      Log.e(TAG, "Failed to enable Firestore offline persistence", e)
    }
  }

  companion object {
    private const val TAG = "OOTDApplication"
  }
}
