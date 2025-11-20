package com.android.ootd.model.posts

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object LikesRepositoryProvider {
  private val _repository: LikesRepository by lazy {
    LikesFirestoreRepository(db = Firebase.firestore)
  }

  var repository: LikesRepository = _repository
}
