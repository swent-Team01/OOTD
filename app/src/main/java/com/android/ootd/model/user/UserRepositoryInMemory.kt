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
                  name = nameList[0],
                  friendList =
                      listOf(
                          Friend(uid = "user2", name = "bob_builder"),
                          Friend(uid = "user3", name = "charlie_brown"))),
          "user2" to
              User(
                  uid = "user2",
                  name = nameList[1],
                  friendList = listOf(Friend(uid = "user1", name = "alice_wonder"))),
          "user3" to User(uid = "user3", name = nameList[2], friendList = emptyList()),
          "user4" to
              User(
                  uid = "user4",
                  name = nameList[3],
                  friendList =
                      listOf(
                          Friend(uid = "user1", name = "alice_wonder"),
                          Friend(uid = "user2", name = "bob_builder"))),
          "user5" to User(uid = "user5", name = nameList[4], friendList = emptyList()))

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
    if (user.friendList.any { it.uid == friendID }) {
      return // Already friends, do nothing (mimics arrayUnion behavior)
    }

    val updatedFriendList = user.friendList + Friend(uid = friendID, name = friendUsername)
    users[userID] = user.copy(friendList = updatedFriendList)
  }
}
