package com.android.ootd.model.user

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val USER_COLLECTION_PATH = "users"

// Custom exception for taken username scenario
class TakenUsernameException(message: String) : Exception(message)

@Keep private data class UserDto(val uid: String = "", val username: String = "")

private fun User.toDto(): UserDto {
  return UserDto(uid = this.uid, username = this.username)
}

private fun UserDto.toDomain(): User {
  return User(uid = this.uid, username = this.username)
}

class UserRepositoryFirestore(private val db: FirebaseFirestore) : UserRepository {

  /** Helper method to check user data as firestore might add the default values */
  private fun checkUserData(user: User): User? {
    if (user.uid.isBlank() || user.username.isBlank()) {
      Log.e("UserRepositoryFirestore", "Invalid user data in user: uid is blank")
      return null
    }
    return user
  }

  /** Helper method to transform a Firestore document into a User object */
  private fun transformUserDocument(document: DocumentSnapshot): User? {
    return try {
      // The document for sure exists because we only call it after we verified document existence.
      val userDto = document.toObject<UserDto>()
      if (userDto == null) {
        Log.e(
            "UserRepositoryFirestore",
            "Failed to deserialize document ${document.id} to User object. Data: ${document.data}")
        return null
      }
      checkUserData(userDto.toDomain())
    } catch (e: Exception) {
      Log.e(
          "UserRepositoryFirestore", "Error transforming document ${document.id}: ${e.message}", e)
      return null
    }
  }

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun createUser(username: String, uid: String) {

    if (usernameExists(username)) {
      Log.e("UserRepositoryFirestore", "Username already in use")
      throw TakenUsernameException("Username already in use")
    }

    val newUser = User(uid, username)
    try {
      addUser(newUser)
    } catch (e: Exception) {
      Log.e("UserRepositoryFirestore", "Error while creating user : ${e.message}", e)
      throw e
    }
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

  override suspend fun userExists(userID: String): Boolean {
    return try {
      val querySnapshot =
          db.collection(USER_COLLECTION_PATH).whereEqualTo("uid", userID).get().await()

      if (querySnapshot.documents.isEmpty()) {
        false
      } else {
        val username = querySnapshot.documents[0].getString("username")
        !username.isNullOrBlank()
      }
    } catch (e: Exception) {
      Log.e("UserRepositoryFirestore", "Error checking user existence: ${e.message}", e)
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

      db.collection(USER_COLLECTION_PATH).document(user.uid).set(user.toDto()).await()

      Log.d("UserRepositoryFirestore", "Successfully added user with UID: ${user.uid}")
    } catch (e: Exception) {
      Log.e("UserRepositoryFirestore", "Error adding user: ${e.message}", e)
      throw e
    }
  }

  private suspend fun usernameExists(username: String): Boolean {
    val querySnapshot =
        db.collection(USER_COLLECTION_PATH).whereEqualTo("username", username).get().await()
    return querySnapshot.documents.isNotEmpty()
  }
}
