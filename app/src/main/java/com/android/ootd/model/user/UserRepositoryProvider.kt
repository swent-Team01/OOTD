package com.android.ootd.model.user

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object UserRepositoryProvider {
  private val _repository: UserRepository by lazy { UserRepositoryFirestore(Firebase.firestore) }

  var repository: UserRepository = _repository
}
