package com.android.ootd.utils

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import io.mockk.InternalPlatformDsl.toArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * An object to manage the connection to Firebase Emulators for Android tests.
 *
 * This object will automatically use the emulators if they are running when the tests start.
 */
object FirebaseEmulator {
  const val HOST = "10.0.2.2"
  const val EMULATORS_PORT = 4400
  const val FIRESTORE_PORT = 8080
  const val AUTH_PORT = 9099
  const val STORAGE_PORT = 9199

  private val httpClient = OkHttpClient()

  private val emulatorsEndpoint = "http://$HOST:$EMULATORS_PORT/emulators"

  private fun areEmulatorsRunning(): Boolean =
      runCatching {
            val client = httpClient
            val request = Request.Builder().url(emulatorsEndpoint).build()
            client.newCall(request).execute().isSuccessful
          }
          .getOrNull() == true

  val isRunning = areEmulatorsRunning()

  private var initialized = false

  init {
    if (isRunning && !initialized) {
      try {
        // Try to configure emulators directly
        Firebase.auth.useEmulator(HOST, AUTH_PORT)
        Firebase.firestore.useEmulator(HOST, FIRESTORE_PORT)
        FirebaseStorage.getInstance().useEmulator(HOST, STORAGE_PORT)
        initialized = true

        assert(Firebase.firestore.firestoreSettings.host.contains(HOST)) {
          "Failed to connect to Firebase Firestore Emulator."
        }
      } catch (e: IllegalStateException) {
        // If Firebase was already initialized, we need to delete and recreate the app
        if (e.message?.contains("useEmulator") == true ||
            e.message?.contains("initialized") == true) {
          Log.w(
              "FirebaseEmulator",
              "Firebase already initialized, reinitializing with emulator...",
              e)

          // Get the application context
          val context =
              androidx.test.core.app.ApplicationProvider.getApplicationContext<
                  android.content.Context>()

          // Clean up DataStore files BEFORE deleting Firebase apps
          cleanupDataStoreFiles(context)

          // Delete all Firebase app instances
          val apps = FirebaseApp.getApps(context).toList()
          apps.forEach { app ->
            try {
              app.delete()
            } catch (ex: Exception) {
              Log.w("FirebaseEmulator", "Error deleting Firebase app: ${app.name}", ex)
            }
          }

          // Give Firebase MUCH more time to clean up DataStore instances
          System.gc()
          System.runFinalization()
          Thread.sleep(2000)

          // Clean up DataStore files again after deletion
          cleanupDataStoreFiles(context)
          System.gc()
          Thread.sleep(500)

          // Reinitialize the default Firebase app
          val newApp = FirebaseApp.initializeApp(context)
          requireNotNull(newApp) { "Failed to reinitialize Firebase app" }

          // Now configure emulators on the fresh instance
          Firebase.auth.useEmulator(HOST, AUTH_PORT)
          Firebase.firestore.useEmulator(HOST, FIRESTORE_PORT)
          FirebaseStorage.getInstance().useEmulator(HOST, STORAGE_PORT)
          initialized = true

          assert(Firebase.firestore.firestoreSettings.host.contains(HOST)) {
            "Failed to connect to Firebase Firestore Emulator after reinitialize."
          }
        } else {
          throw e
        }
      }
    }
  }

  val projectID by lazy { FirebaseApp.getInstance().options.projectId }

  private val firestoreEndpoint by lazy {
    "http://${HOST}:$FIRESTORE_PORT/emulator/v1/projects/$projectID/databases/(default)/documents"
  }

  private val authEndpoint by lazy {
    "http://${HOST}:$AUTH_PORT/emulator/v1/projects/$projectID/accounts"
  }

  val auth
    get() = Firebase.auth

  val firestore
    get() = Firebase.firestore

  val storage
    get() = FirebaseStorage.getInstance()

  private fun clearEmulator(endpoint: String) {
    val client = httpClient
    val request = Request.Builder().url(endpoint).delete().build()
    val response = client.newCall(request).execute()

    assert(response.isSuccessful) { "Failed to clear emulator at $endpoint" }
  }

  fun clearAuthEmulator() {
    clearEmulator(authEndpoint)
  }

  fun clearFirestoreEmulator() {
    clearEmulator(firestoreEndpoint)
  }

  /**
   * Helper function to clean up DataStore files that may be locking Firebase resources. This is
   * called both before and after Firebase app deletion to ensure clean state.
   */
  private fun cleanupDataStoreFiles(context: android.content.Context) {
    try {
      val dataStoreDir = java.io.File(context.filesDir, "datastore")
      if (dataStoreDir.exists() && dataStoreDir.isDirectory) {
        var deletedCount = 0
        dataStoreDir.listFiles()?.forEach { file ->
          // Delete all Firebase-related DataStore files
          if (file.name.contains("Firebase") || file.name.contains("HeartBeat")) {
            try {
              if (file.delete()) {
                deletedCount++
                Log.d("FirebaseEmulator", "Deleted DataStore file: ${file.name}")
              }
            } catch (ex: Exception) {
              Log.w("FirebaseEmulator", "Could not delete ${file.name}: ${ex.message}")
            }
          }
        }
        if (deletedCount > 0) {
          Log.d("FirebaseEmulator", "Cleaned up $deletedCount DataStore files")
        }
      }
    } catch (e: Exception) {
      Log.w("FirebaseEmulator", "Error cleaning DataStore files", e)
    }
  }

  /**
   * Seeds a Google user in the Firebase Auth Emulator using a fake JWT id_token.
   *
   * @param fakeIdToken A JWT-shaped string, must contain at least "sub".
   * @param email The email address to associate with the account.
   */
  fun createGoogleUser(fakeIdToken: String) {
    val url =
        "http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=fake-api-key"

    // postBody must be x-www-form-urlencoded style string, wrapped in JSON
    val postBody = "id_token=$fakeIdToken&providerId=google.com"

    val requestJson =
        JSONObject().apply {
          put("postBody", postBody)
          put("requestUri", "http://localhost")
          put("returnIdpCredential", true)
          put("returnSecureToken", true)
        }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = requestJson.toString().toRequestBody(mediaType)

    val request =
        Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build()

    val response = httpClient.newCall(request).execute()
    assert(response.isSuccessful) {
      "Failed to create user in Auth Emulator: ${response.code} ${response.message}"
    }
  }

  fun changeEmail(fakeIdToken: String, newEmail: String) {
    val response =
        httpClient
            .newCall(
                Request.Builder()
                    .url(
                        "http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:update?key=fake-api-key")
                    .post(
                        """
            {
                "idToken": "$fakeIdToken",
                "email": "$newEmail",
                "returnSecureToken": true
            }
        """
                            .trimIndent()
                            .toRequestBody())
                    .build())
            .execute()
    assert(response.isSuccessful) {
      "Failed to change email in Auth Emulator: ${response.code} ${response.message}"
    }
  }

  val users: String
    get() {
      val request =
          Request.Builder()
              .url(
                  "http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:query?key=fake-api-key")
              .build()

      Log.d("FirebaseEmulator", "Fetching users with request: ${request.url.toString()}")
      val response = httpClient.newCall(request).execute()
      Log.d("FirebaseEmulator", "Response received: ${response.toArray()}")
      return response.body.toString()
    }
}
