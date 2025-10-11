package com.android.ootd.model.user

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val USER_COLLECTION_PATH = "users"

class UserRepositoryFirestore(private val db: FirebaseFirestore) : UserRepository {

  /** Helper method to check user data as firestore might add the default values */
  private fun checkUserData(user: User): User? {
    if (user.uid.isBlank() || user.name.isBlank()) {
      Log.e("UserRepositoryFirestore", "Invalid user data in user: uid is blank")
      return null
    }
    return user
  }

  /** Helper method to transform a Firestore document into a User object */
  private fun transformUserDocument(document: DocumentSnapshot): User? {
    return try {
      // The document for sure exists because we only call it after we verified document existence.
      val user = document.toObject<User>()
      if (user == null) {
        Log.e(
            "UserRepositoryFirestore",
            "Failed to deserialize document ${document.id} to User object. Data: ${document.data}")
        return null
      }
      checkUserData(user)
    } catch (e: Exception) {
      Log.e(
          "UserRepositoryFirestore", "Error transforming document ${document.id}: ${e.message}", e)
      return null
    }
  }

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllUsers(): List<User> {

    return try {
      val querySnapshot = db.collection(USER_COLLECTION_PATH).get().await()

      querySnapshot.documents.mapNotNull { document -> transformUserDocument(document) }
    } catch (e: Exception) {
      Log.e("UserRepositoryFirestore", "Error getting users: ${e.message}", e)
      throw e
    }
  }

  override suspend fun getUser(userID: String): User {
    return try {
      val documentList =
          db.collection(USER_COLLECTION_PATH).whereEqualTo("uid", userID).get().await()

      if (documentList.documents.isEmpty()) {
        throw NoSuchElementException("User with ID $userID not found")
      }

      transformUserDocument(documentList.documents[0])
          ?: throw IllegalStateException("Failed to transform document with ID $userID")
    } catch (e: Exception) {
      Log.e("UserRepositoryFirestore", "Error getting user $userID: ${e.message}", e)
      throw e
    }
  }

  override suspend fun addUser(user: User) {
    try {
      val existingDoc =
          db.collection(USER_COLLECTION_PATH).whereEqualTo("uid", user.uid).get().await()

      if (!existingDoc.documents.isEmpty()) {
        throw IllegalArgumentException("User with UID ${user.uid} already exists")
      }

      db.collection(USER_COLLECTION_PATH).document(user.uid).set(user).await()

      Log.d("UserRepositoryFirestore", "Successfully added user with UID: ${user.uid}")
    } catch (e: Exception) {
      Log.e("UserRepositoryFirestore", "Error adding user: ${e.message}", e)
      throw e
    }
  }

  override suspend fun addFriend(userID: String, friendID: String, friendUsername: String) {
    try {
      val friendDocumentList =
          db.collection(USER_COLLECTION_PATH).whereEqualTo("uid", friendID).get().await()

      if (friendDocumentList.documents.isEmpty()) {
        throw NoSuchElementException("Friend with ID $friendID not found")
      }

      val userRef = db.collection(USER_COLLECTION_PATH).document(userID)

      // With arrayUnion there can be no duplicates
      // https://firebase.google.com/docs/firestore/manage-data/add-data , Update elements in an
      // array section

      userRef
          .update(
              "friendList", FieldValue.arrayUnion(Friend(uid = friendID, name = friendUsername)))
          .await()
    } catch (e: Exception) {
      Log.e("UserRepositoryFirestore", "Error adding friend $friendID to $userID ${e.message}", e)
      throw e
    }
  }

  override suspend fun removeFriend(userID: String, friendID: String, friendUsername: String) {
    try {
      val friendDocumentList =
          db.collection(USER_COLLECTION_PATH).whereEqualTo("uid", friendID).get().await()

      if (friendDocumentList.documents.isEmpty()) {
        throw NoSuchElementException("Friend with ID $friendID not found")
      }

      val userRef = db.collection(USER_COLLECTION_PATH).document(userID)

      // Use arrayRemove to remove the friend from the array
      userRef
          .update(
              "friendList", FieldValue.arrayRemove(Friend(uid = friendID, name = friendUsername)))
          .await()
    } catch (e: Exception) {
      Log.e(
          "UserRepositoryFirestore", "Error removing friend $friendID from $userID ${e.message}", e)
      throw e
    }
  }

  override suspend fun isMyFriend(friendID: String): Boolean {
    val myUID = Firebase.auth.currentUser?.uid
    val documentList = db.collection(USER_COLLECTION_PATH).whereEqualTo("uid", myUID).get().await()
    if (documentList.documents.isEmpty()) {
      throw NoSuchElementException("The authenticated user has not been added to the database")
    }
    if (documentList.documents.size != 1) {
      throw IllegalStateException("There are multiple users with the same uid")
    }
    val myUser = transformUserDocument(documentList.documents[0])

    return (myUser?.friendList?.any { it.uid == friendID } == true)
  }
}
