package com.android.ootd.model

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object ItemsRepositoryProvider {
  private val _repository: ItemsRepository by lazy { ItemsRepositoryFirestore(Firebase.firestore) }

  var repository: ItemsRepository = _repository
}
