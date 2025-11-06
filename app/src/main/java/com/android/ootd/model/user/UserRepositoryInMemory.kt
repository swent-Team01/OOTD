package com.android.ootd.model.user

import java.util.UUID

class UserRepositoryInMemory : UserRepository {
  var currentUser = "user1"
  val nameList =
      listOf<String>(
          "alice_wonder", "bob_builder", "charlie_brown", "diana_prince", "edward_scissorhands")
  private val users =
      mutableMapOf(
          "user1" to
              User(uid = "user1", ownerId = "user1", username = nameList[0], profilePicture = "1"),
          "user2" to
              User(uid = "user2", ownerId = "user2", username = nameList[1], profilePicture = "2"),
          "user3" to
              User(uid = "user3", ownerId = "user3", username = nameList[2], profilePicture = "3"),
          "user4" to
              User(uid = "user4", ownerId = "user4", username = nameList[3], profilePicture = "4"),
          "user5" to
              User(uid = "user5", ownerId = "user5", username = nameList[4], profilePicture = "5"),
          "nonRegisterUser" to
              User(
                  uid = "nonRegisterUser",
                  ownerId = "nonRegisterUser",
                  username = "",
                  profilePicture = "0"))

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllUsers(): List<User> {
    return users.values.toList()
  }

  override suspend fun addUser(user: User) {
    require(!(users.containsKey(user.uid))) { "User with UID ${user.uid} already exists" }
    users[user.uid] = user
  }

  override suspend fun getUser(userID: String): User {
    if (users.containsKey(userID)) {
      return users[userID] ?: throw NoSuchElementException("User with ID $userID not found")
    } else {
      throw NoSuchElementException("User with ID $userID not found")
    }
  }

  override suspend fun userExists(userID: String): Boolean {
    val username = getUser(userID).username
    return username.isNotBlank()
  }

  override suspend fun createUser(
      username: String,
      uid: String,
      ownerId: String,
      profilePicture: String
  ) {
    // Check if username already exists
    if (users.values.any { it.username == username }) {
      throw TakenUsernameException("Username already in use")
    }

    val newUser = User(uid, ownerId, username, profilePicture)
    addUser(newUser)
  }

  override suspend fun editUser(userID: String, newUsername: String, profilePicture: String) {

    val currentUser = getUser(userID)
    val newUname = newUsername.takeIf { it.isNotBlank() } ?: currentUser.username
    val newPicture = profilePicture.takeIf { it.isNotBlank() } ?: currentUser.profilePicture

    // Only check for taken username if we're actually changing to a new non-blank username
    if (newUsername.isNotBlank() && newUsername != currentUser.username) {
      if (users.values.any { it.username == newUsername && it.uid != userID }) {
        throw TakenUsernameException("Username already in use")
      }
    }

    users[userID] = currentUser.copy(username = newUname, profilePicture = newPicture)
  }

  override suspend fun deleteProfilePicture(userID: String) {
    if (userID.isBlank()) throw IllegalArgumentException("User ID cannot be blank")
    val user = getUser(userID)
    users[userID] = user.copy(profilePicture = "")
  }

  // replaces old "removeUser"
  override suspend fun deleteUser(userID: String) {
    if (userID.isBlank()) throw IllegalArgumentException("User ID cannot be blank")
    users.remove(userID)
  }
}
