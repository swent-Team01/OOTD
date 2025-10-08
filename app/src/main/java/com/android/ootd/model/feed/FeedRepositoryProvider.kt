package com.android.ootd.model.feed

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object FeedRepositoryProvider {
  private val _repository: FeedRepository by lazy {
    FeedRepositoryFirestore(Firebase.firestore)
    // FeedRepositoryMock()
  }

  var repository: FeedRepository = _repository
}
