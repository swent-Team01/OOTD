package com.android.ootd.model.user

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object UserRepositoryProvider {
  private val _repository: UserRepository by lazy { UserRepositoryFirestore(Firebase.firestore) }

  var repository: UserRepository = _repository
}
