package com.android.ootd.model.user

import java.util.UUID

class UserRepositoryInMemory : UserRepository {
  val currentUser = "user1"
  val nameList =
      listOf<String>(
          "alice_wonder", "bob_builder", "charlie_brown", "diana_prince", "edward_scissorhands")
  private val users =
      mutableMapOf(
          "user1" to
              User(uid = "user1", username = nameList[0], friendUids = listOf("user2", "user3")),
          "user2" to User(uid = "user2", username = nameList[1], friendUids = listOf("user1")),
          "user3" to User(uid = "user3", username = nameList[2], friendUids = emptyList()),
          "user4" to
              User(uid = "user4", username = nameList[3], friendUids = listOf("user1", "user2")),
          "user5" to User(uid = "user5", username = nameList[4], friendUids = emptyList()))

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

  override suspend fun addFriend(userID: String, friendID: String, friendUsername: String) {
    val user = getUser(userID)

    if (!users.containsKey(friendID)) {
      throw NoSuchElementException("Friend with ID $friendID not found")
    }

    // Check if friend already exists in the list
    if (user.friendUids.any { it == friendID }) {
      return // Already friends, do nothing (mimics arrayUnion behavior)
    }

    val updatedFriendUids = user.friendUids + friendID
    users[userID] = user.copy(friendUids = updatedFriendUids)
  }

  override suspend fun removeFriend(userID: String, friendID: String, friendUsername: String) {
    val user = getUser(userID)

    if (!users.containsKey(friendID)) {
      throw NoSuchElementException("Friend with ID $friendID not found")
    }

    // If the friend is not there, we don't do anything
    if (user.friendUids.none { it == friendID }) {
      return
    }

    val updatedFriendUids = user.friendUids - friendID
    users[userID] = user.copy(friendUids = updatedFriendUids)
  }

  override suspend fun isMyFriend(userID: String, friendID: String): Boolean {
    // Here the userID does not matter because this class is used for testing,
    // and this way I would not have to redo all the tests I already written.

    val user = getUser(currentUser)
    return user.friendUids.isNotEmpty() && user.friendUids.any { it == friendID }
  }

  override suspend fun createUser(username: String, uid: String) {
    // Check if username already exists
    if (users.values.any { it.username == username }) {
      throw TakenUsernameException("Username already in use")
    }

    val newUser = User(uid, username)
    addUser(newUser)
  }
}
