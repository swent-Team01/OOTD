package com.android.ootd.model.consent

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the consent repository in the app. `repository` is mutable for
 * testing purposes.
 */
object ConsentRepositoryProvider {
  private val _repository: ConsentRepository by lazy {
    ConsentRepositoryFirestore(Firebase.firestore)
  }

  var repository: ConsentRepository = _repository
}
