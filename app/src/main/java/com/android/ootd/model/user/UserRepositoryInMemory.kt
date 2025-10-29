package com.android.ootd.model.user

import java.util.UUID

class UserRepositoryInMemory : UserRepository {
  var currentUser = "user1"
  val nameList =
      listOf<String>(
          "alice_wonder", "bob_builder", "charlie_brown", "diana_prince", "edward_scissorhands")
  private val users =
      mutableMapOf(
          "user1" to User(uid = "user1", username = nameList[0], profilePicture = "1"),
          "user2" to User(uid = "user2", username = nameList[1], profilePicture = "2"),
          "user3" to User(uid = "user3", username = nameList[2], profilePicture = "3"),
          "user4" to User(uid = "user4", username = nameList[3], profilePicture = "4"),
          "user5" to User(uid = "user5", username = nameList[4], profilePicture = "5"),
          "nonRegisterUser" to User(uid = "nonRegisterUser", username = "", profilePicture = "0"))

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

  override suspend fun createUser(username: String, uid: String, profilePicture: String) {
    // Check if username already exists
    if (users.values.any { it.username == username }) {
      throw TakenUsernameException("Username already in use")
    }

    val newUser = User(uid, username, profilePicture)
    addUser(newUser)
  }

  // Useful for testing
  fun removeUser(uid: String) {
    users.remove(uid)
  }
}
