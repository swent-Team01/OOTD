package com.android.ootd.model.user

import java.util.UUID

class UserRepositoryInMemory : UserRepository {
  val nameList =
      listOf<String>(
          "alice_wonder", "bob_builder", "charlie_brown", "diana_prince", "edward_scissorhands")
  private val users =
      mutableMapOf(
          "user1" to
              User(
                  uid = "user1",
                  username = nameList[0],
                  friendList =
                      listOf(
                          Friend(uid = "user2", username = "bob_builder"),
                          Friend(uid = "user3", username = "charlie_brown"))),
          "user2" to
              User(
                  uid = "user2",
                  username = nameList[1],
                  friendList = listOf(Friend(uid = "user1", username = "alice_wonder"))),
          "user3" to User(uid = "user3", username = nameList[2], friendList = emptyList()),
          "user4" to
              User(
                  uid = "user4",
                  username = nameList[3],
                  friendList =
                      listOf(
                          Friend(uid = "user1", username = "alice_wonder"),
                          Friend(uid = "user2", username = "bob_builder"))),
          "user5" to User(uid = "user5", username = nameList[4], friendList = emptyList()),
          "nonRegisterUser" to User(uid = "nonRegisterUser", username = "", friendList = listOf()))

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

  override suspend fun addFriend(userID: String, friendID: String, friendUsername: String) {
    val user = getUser(userID)

    if (!users.containsKey(friendID)) {
      throw NoSuchElementException("Friend with ID $friendID not found")
    }

    // Check if friend already exists in the list
    if (user.friendList.any { it.uid == friendID }) {
      return // Already friends, do nothing (mimics arrayUnion behavior)
    }

    val updatedFriendList = user.friendList + Friend(uid = friendID, username = friendUsername)
    users[userID] = user.copy(friendList = updatedFriendList)
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
