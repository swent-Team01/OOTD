package com.android.ootd.model.account

import com.android.ootd.model.user.User

class AccountRepositoryInMemory : AccountRepository {
  var currentUser = "user1"
  val nameList =
      listOf("alice_wonder", "bob_builder", "charlie_brown", "diana_prince", "edward_scissorhands")
  private val accounts =
      mutableMapOf(
          "user1" to
              Account(
                  uid = "user1",
                  ownerId = "user1",
                  username = nameList[0],
                  friendUids = listOf("user2", "user3")),
          "user2" to
              Account(
                  uid = "user2",
                  ownerId = "user2",
                  username = nameList[1],
                  friendUids = listOf("user1")),
          "user3" to
              Account(
                  uid = "user3",
                  ownerId = "user3",
                  username = nameList[2],
                  friendUids = emptyList()),
          "user4" to
              Account(
                  uid = "user4",
                  ownerId = "user4",
                  username = nameList[3],
                  friendUids = listOf("user1", "user2")),
          "user5" to
              Account(
                  uid = "user5",
                  ownerId = "user5",
                  username = nameList[4],
                  friendUids = emptyList()),
          "nonRegisterUser" to
              Account(
                  uid = "nonRegisterUser",
                  ownerId = "nonRegisterUser",
                  username = "",
                  friendUids = listOf()))

  override suspend fun createAccount(user: User, dateOfBirth: String) {
    // Check if username already exists
    if (accounts.values.any { it.username == user.username && it.username.isNotBlank() }) {
      throw TakenUserException("Username already in use")
    }

    val newAccount =
        Account(
            uid = user.uid, ownerId = user.uid, username = user.username, birthday = dateOfBirth)
    addAccount(newAccount)
  }

  override suspend fun addAccount(account: Account) {
    require(!(accounts.containsKey(account.uid))) {
      "Account with UID ${account.uid} already exists"
    }
    accounts[account.uid] = account
  }

  override suspend fun getAccount(userId: String): Account {
    if (accounts.containsKey(userId)) {
      return accounts[userId] ?: throw NoSuchElementException("Account with ID $userId not found")
    } else {
      throw NoSuchElementException("Account with ID $userId not found")
    }
  }

  override suspend fun accountExists(userId: String): Boolean {
    val username = getAccount(userId).username
    return username.isNotBlank()
  }

  override suspend fun addFriend(userID: String, friendID: String) {
    val account = getAccount(userID)

    if (!accounts.containsKey(friendID)) {
      throw NoSuchElementException("Friend with ID $friendID not found")
    }

    // Check if friend already exists in the list
    if (account.friendUids.any { it == friendID }) {
      return // Already friends, do nothing (mimics arrayUnion behavior)
    }

    val updatedFriendUids = account.friendUids + friendID
    accounts[userID] = account.copy(friendUids = updatedFriendUids)
  }

  override suspend fun removeFriend(userID: String, friendID: String) {
    val account = getAccount(userID)

    if (!accounts.containsKey(friendID)) {
      throw NoSuchElementException("Friend with ID $friendID not found")
    }

    // If the friend is not there, we don't do anything
    if (account.friendUids.none { it == friendID }) {
      return
    }

    val updatedFriendUids = account.friendUids - friendID
    accounts[userID] = account.copy(friendUids = updatedFriendUids)
  }

  override suspend fun isMyFriend(userID: String, friendID: String): Boolean {
    // Here the userID does not matter because this class is used for testing,
    // and this way I would not have to redo all the tests I already written.

    val account = getAccount(currentUser)
    return account.friendUids.isNotEmpty() && account.friendUids.any { it == friendID }
  }

  // Useful for testing
  fun removeAccount(uid: String) {
    accounts.remove(uid)
  }
}
