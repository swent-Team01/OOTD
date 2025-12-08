package com.android.ootd.model.post

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

object OutfitPostRepositoryProvider {
  private val _repository: OutfitPostRepository by lazy {
    OutfitPostRepositoryFirestore(db = Firebase.firestore, storage = Firebase.storage)
  }

  var repository: OutfitPostRepository = _repository
}
