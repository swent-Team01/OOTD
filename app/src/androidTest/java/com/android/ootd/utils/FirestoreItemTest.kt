package com.android.ootd.utils

import android.util.Log
import com.android.ootd.model.ITEMS_COLLECTION
import com.android.ootd.model.ItemsRepository
import com.android.ootd.model.ItemsRepositoryFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

open class FirestoreItemTest : ItemsTest {

  suspend fun countItems(): Int {
    // FirebaseEmulator put
    return FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).get().await().size()
  }

  private suspend fun clearTestCollection() {
    // FirebaseEmulator put
    val items = FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).get().await()
    items?.forEach { it.reference.delete() }
    assert(countItems() == 0) {
      "Test collection is not empty after clearing, count: ${countItems()}"
    }
  }

  override fun createInitializedRepository(): ItemsRepository {
    return ItemsRepositoryFirestore(db = FirebaseEmulator.firestore)
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
}
