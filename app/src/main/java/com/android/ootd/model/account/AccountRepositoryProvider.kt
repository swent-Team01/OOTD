package com.android.ootd.model.account

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the account repository in the app. `repository` is mutable for
 * testing purposes.
 */
object AccountRepositoryProvider {

  private fun defaultRepository(): AccountRepository {
    return AccountRepositoryFirestore(Firebase.firestore)
  }

  var repository: AccountRepository = defaultRepository()
}
