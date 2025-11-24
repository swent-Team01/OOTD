package com.android.ootd.model.notifications

import NotificationRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object NotificationRepositoryProvider {

  /** Default repository (real Firebase) */
  private fun defaultRepository(): NotificationRepository {
    return NotificationRepositoryFirestore(Firebase.firestore)
  }

  /** Mutable so JVM tests can override it */
  var repository: NotificationRepository = defaultRepository()
}
