package com.android.ootd.model.items

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provider object for ItemsRepository instances.
 *
 * This object manages the repository instance used throughout the app. By default, it uses Firebase
 * Firestore, but can be switched to a local in-memory implementation for testing purposes.
 *
 * Usage:
 * ```kotlin
 * // Use Firestore (default)
 * ItemsRepositoryProvider.useFirestore()
 *
 * // Use local storage for testing
 * ItemsRepositoryProvider.useLocal()
 *
 * // Set a custom repository
 * ItemsRepositoryProvider.setRepository(myCustomRepo)
 * ```
 */
object ItemsRepositoryProvider {

  private val firestoreRepository: ItemsRepository by lazy {
    ItemsRepositoryFirestore(Firebase.firestore)
  }

  private val localRepository: ItemsRepository by lazy { ItemsRepositoryLocal() }

  /** The current repository instance being used. Defaults to Firestore repository. */
  var repository: ItemsRepository = firestoreRepository
    private set

  /**
   * Switches to using the Firestore repository. This is the default repository for production use.
   */
  fun useFirestore() {
    repository = firestoreRepository
  }

  /**
   * Switches to using the local in-memory repository. Useful for testing without network calls or
   * Firebase dependencies.
   */
  fun useLocal() {
    repository = localRepository
  }

  /**
   * Sets a custom repository implementation. Useful for testing with mock repositories.
   *
   * @param customRepository The repository instance to use.
   */
  fun setRepository(customRepository: ItemsRepository) {
    repository = customRepository
  }

  /**
   * Gets the local repository instance if currently using local storage. This allows access to
   * additional testing methods like clearAll().
   *
   * @return The local repository instance, or null if not using local storage.
   */
  fun getLocalRepository(): ItemsRepositoryLocal? {
    return repository as? ItemsRepositoryLocal
  }

  /**
   * Resets the repository to the default Firestore implementation. Useful for cleaning up after
   * tests.
   */
  fun reset() {
    repository = firestoreRepository
  }
}
