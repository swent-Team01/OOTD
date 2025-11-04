package com.android.ootd.model.notifications

import NotificationRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object NotificationRepositoryProvider {
  val repository: NotificationRepository by lazy {
    NotificationRepositoryFirestore(Firebase.firestore)
  }
}
