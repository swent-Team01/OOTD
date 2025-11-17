package com.android.ootd.model.reactions

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.ktx.storage

object ReactionRepositoryProvider {
  private val _repository: ReactionRepository by lazy {
    ReactionFirestoreRepository(
        db = Firebase.firestore, storage = com.google.firebase.ktx.Firebase.storage)
  }

  var repository: ReactionRepository = _repository
}
