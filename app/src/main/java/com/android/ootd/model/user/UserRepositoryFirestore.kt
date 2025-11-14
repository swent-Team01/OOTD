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

class BlankUserID : Exception("UserID cannot be blank")

@Keep
private data class UserDto(
    val uid: String = "",
    val ownerId: String = "",
    val username: String = "",
    val profilePicture: String = ""
)

private const val username_taken_exception = "Username already in use"
private const val TAG = "UserRepositoryFirestore"
private const val FIELD_USERNAME = "username"
private const val FIELD_PROFILE_PICTURE = "profilePicture"
private const val ERROR_USER_ID_BLANK = "UserID cannot be blank"

private fun User.toDto(): UserDto {
  return UserDto(
      uid = this.uid,
      ownerId = this.ownerId,
      username = this.username,
      profilePicture = this.profilePicture)
}

private fun UserDto.toDomain(): User {
  return User(
      uid = this.uid,
      ownerId = this.ownerId,
      username = this.username,
      profilePicture = this.profilePicture)
}

class UserRepositoryFirestore(private val db: FirebaseFirestore) : UserRepository {

  /** Helper method to check user data as firestore might add the default values */
  private fun checkUserData(user: User): User? {
    if (user.uid.isBlank() || user.username.isBlank()) {
      Log.e(TAG, "Invalid user data in user: uid is blank")
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
            TAG,
            "Failed to deserialize document ${document.id} to User object. Data: ${document.data}")
        return null
      }
      checkUserData(userDto.toDomain())
    } catch (e: Exception) {
      Log.e(TAG, "Error transforming document ${document.id}: ${e.message}", e)
      return null
    }
  }

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun createUser(
      username: String,
      uid: String,
      ownerId: String,
      profilePicture: String
  ) {

    if (uid.isBlank()) throw BlankUserID()
    if (usernameExists(username)) {
      Log.e(TAG, username_taken_exception)
      throw TakenUsernameException(username_taken_exception)
    }
    val newUser = User(uid, ownerId.takeIf { it.isNotBlank() } ?: uid, username, profilePicture)
    try {
      addUser(newUser)
    } catch (e: Exception) {
      Log.e(TAG, "Error while creating user : ${e.message}", e)
      throw e
    }
  }

  override suspend fun getAllUsers(): List<User> {

    return try {
      val querySnapshot = db.collection(USER_COLLECTION_PATH).get().await()

      querySnapshot.documents.mapNotNull { document -> transformUserDocument(document) }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting users: ${e.message}", e)
      throw e
    }
  }

  override suspend fun getUser(userID: String): User {
    return try {
      if (userID.isBlank()) throw BlankUserID()
      val userDoc = db.collection(USER_COLLECTION_PATH).document(userID).get().await()

      if (!userDoc.exists()) {
        throw NoSuchElementException("User with ID $userID not found")
      }

      transformUserDocument(userDoc)
          ?: throw IllegalStateException("Failed to transform document with ID $userID")
    } catch (e: Exception) {
      Log.e(TAG, "Error getting user $userID: ${e.message}", e)
      throw e
    }
  }

  override suspend fun userExists(userID: String): Boolean {
    return try {
      if (userID.isBlank()) throw BlankUserID()
      val querySnapshot = db.collection(USER_COLLECTION_PATH).document(userID).get().await()

      if (!querySnapshot.exists()) {
        false
      } else {
        val username = querySnapshot.getString(FIELD_USERNAME)
        !username.isNullOrBlank()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error checking user existence: ${e.message}", e)
      throw e
    }
  }

  override suspend fun addUser(user: User) {
    try {
      require(!userExists(user.uid)) { "User with UID ${user.uid} already exists" }

      db.collection(USER_COLLECTION_PATH).document(user.uid).set(user.toDto()).await()
      Log.d(TAG, "Successfully added user with UID: ${user.uid}")
    } catch (e: Exception) {
      Log.e(TAG, "Error adding user: ${e.message}", e)
      throw e
    }
  }

  override suspend fun editUser(userID: String, newUsername: String, profilePicture: String) {
    try {
      require(!(userID.isBlank())) { ERROR_USER_ID_BLANK }
      val user = getUser(userID)

      val isNewUsername = user.username != newUsername && newUsername.isNotBlank()

      val newUname = newUsername.takeIf { isNewUsername } ?: user.username
      val newPicture = profilePicture.takeIf { it.isNotBlank() } ?: user.profilePicture

      if (isNewUsername && usernameExists(newUname)) {
        throw TakenUsernameException("Username $newUname already in use")
      }

      db.collection(USER_COLLECTION_PATH)
          .document(userID)
          .update(mapOf(FIELD_PROFILE_PICTURE to newPicture, FIELD_USERNAME to newUname))
          .await()

      Log.d(TAG, "Successfully updated username for user $userID to $newUsername")
    } catch (e: TakenUsernameException) {
      Log.e(TAG, "Username already taken: ${e.message}", e)
      throw e
    } catch (e: NoSuchElementException) {
      Log.e(TAG, "User not found: ${e.message}", e)
      throw e
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, "Invalid argument: ${e.message}", e)
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Error updating username: ${e.message}", e)
      throw e
    }
  }

  override suspend fun deleteProfilePicture(userID: String) {
    if (userID.isBlank()) throw BlankUserID()
    try {
      getUser(userID)
      db.collection(USER_COLLECTION_PATH).document(userID).update(mapOf("profilePicture" to ""))
    } catch (e: NoSuchElementException) {
      Log.e("UserRepositoryFirestore", "User not found: ${e.message}", e)
      throw e
    } catch (e: Exception) {
      Log.e("UserRepositoryFirestore", "Error deleting users profilepicture: ${e.message}", e)
      throw e
    }
  }

  override suspend fun deleteUser(userID: String) {
    try {
      // Validate input
      require(!(userID.isBlank())) { ERROR_USER_ID_BLANK }
      getUser(userID)
      db.collection(USER_COLLECTION_PATH).document(userID).delete().await()

      Log.d(TAG, "Successfully deleted user with ID: $userID")
    } catch (e: NoSuchElementException) {
      Log.e(TAG, "User not found: ${e.message}", e)
      throw e
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, "Invalid argument: ${e.message}", e)
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Error deleting user: ${e.message}", e)
      throw e
    }
  }

  private suspend fun usernameExists(username: String): Boolean {
    val querySnapshot =
        db.collection(USER_COLLECTION_PATH).whereEqualTo(FIELD_USERNAME, username).get().await()
    return querySnapshot.documents.isNotEmpty()
  }
}
