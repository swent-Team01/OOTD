package com.android.ootd.utils

import android.util.Log
import com.android.ootd.model.ITEMS_COLLECTION
import com.android.ootd.model.ItemsRepository
import com.android.ootd.model.ItemsRepositoryFirestore
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before

open class FirestoreItemTest : ItemsTest {

  suspend fun countItems(): Int {
    return Firebase.firestore.collection(ITEMS_COLLECTION).get().await().size()
  }

  private suspend fun clearTestCollection() {
    val items = Firebase.firestore.collection(ITEMS_COLLECTION).get().await()
    items?.forEach { it.reference.delete() }
    assert(countItems() == 0) {
      "Test collection is not empty after clearing, count: ${countItems()}"
    }
  }

  override fun createInitializedRepository(): ItemsRepository {
    return ItemsRepositoryFirestore(db = Firebase.firestore)
  }

  @Before
  override fun setUp() {
    // ⚡ Ensure the emulator is configured BEFORE any Firebase.firestore access
    try {
      FirebaseEmulator
    } catch (e: IllegalStateException) {
      Log.w("FirestoreItemTest", "Emulator already initialized, skipping setup", e)
    }

    // Initialize repository AFTER emulator setup
    super.setUp()

    // Clear any leftover documents before running a test
    runTest {
      val existing = countItems()
      if (existing > 0) {
        Log.w("FirestoreItemTest", "Non-empty collection before test (count=$existing)")
        clearTestCollection()
      }
    }
  }

  @After
  open fun tearDown() {
    runTest {
      if (FirebaseEmulator.isRunning) {
        clearTestCollection()
        FirebaseEmulator.clearFirestoreEmulator()
      } else {
        Log.w("FirestoreItemTest", "Emulator not running, skipping clear in tearDown")
      }
    }
  }

  companion object {
    const val HOST = "10.0.2.2"
    const val EMULATORS = "http://10.0.2.2:4400/emulators"
    const val FIRESTORE_PORT = 8080
    const val AUTH_PORT = 9099
  }

  /* Emulator-dependent constants and methods */
  object Firestore {
    const val PORT = 8080

    fun clear() {
      val projectId = FirebaseApp.getInstance().options.projectId
      val endpoint =
          "http://$HOST:$PORT/emulator/v1/projects/$projectId/databases/(default)/documents"

      val client = OkHttpClient()
      val request = Request.Builder().url(endpoint).delete().build()
      val response = client.newCall(request).execute()
      Log.e("Firestore", "Cleared")
      if (!response.isSuccessful) {
        throw Exception("Failed to clear Firestore.")
      }
    }
  }
}
