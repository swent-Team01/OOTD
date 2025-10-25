package com.android.ootd.model.post

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.ktx.storage

object OutfitPostRepositoryProvider {
  private val _repository: OutfitPostRepository by lazy {
    OutfitPostRepositoryFirestore(
        db = Firebase.firestore,
        // Firebase.storage comes from the Kotlin Extensions (KTX) library.
        // Import `com.google.firebase.ktx.Firebase` instead of the base
        // `com.google.firebase.Firebase`
        // so we can access this extension property directly, as it provides the default
        // FirebaseStorage
        // instance associated with the default FirebaseApp.
        storage = com.google.firebase.ktx.Firebase.storage)
  }

  var repository: OutfitPostRepository = _repository
}
