package com.android.ootd.model.account

import com.android.ootd.model.map.Location
import com.android.ootd.model.map.isValidLocation
import com.android.ootd.model.user.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

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
                  profilePicture = "u1.jpg",
                  friendUids = listOf("user2", "user3")),
          "user2" to
              Account(
                  uid = "user2",
                  ownerId = "user2",
                  username = nameList[1],
                  profilePicture = "u2.jpg",
                  friendUids = listOf("user1")),
          "user3" to
              Account(
                  uid = "user3",
                  ownerId = "user3",
                  username = nameList[2],
                  profilePicture = "u3.jpg",
                  friendUids = emptyList()),
          "user4" to
              Account(
                  uid = "user4",
                  ownerId = "user4",
                  username = nameList[3],
                  profilePicture = "u4.jpg",
                  friendUids = listOf("user1", "user2")),
          "user5" to
              Account(
                  uid = "user5",
                  ownerId = "user5",
                  username = nameList[4],
                  profilePicture = "u5.jpg",
                  friendUids = emptyList()),
          "nonRegisterUser" to
              Account(
                  uid = "nonRegisterUser",
                  ownerId = "nonRegisterUser",
                  username = "",
                  profilePicture = "u0.jpg",
                  friendUids = listOf()))

  private val accountUpdates = MutableStateFlow<Pair<String, Account?>>(Pair("", null))
  private val publicLocations = mutableMapOf<String, PublicLocation>()
  private val publicLocationUpdates = MutableStateFlow<List<PublicLocation>>(emptyList())

  override suspend fun createAccount(
      user: User,
      userEmail: String,
      dateOfBirth: String,
      location: Location
  ) {
    if (accounts.values.any { it.username == user.username && it.username.isNotBlank() }) {
      throw TakenUserException("Username already in use")
    }

    val newAccount =
        Account(
            uid = user.uid,
            ownerId = user.ownerId,
            googleAccountEmail = userEmail,
            username = user.username,
            birthday = dateOfBirth,
            profilePicture = user.profilePicture,
            location = location)
    addAccount(newAccount)
  }

  override suspend fun addAccount(account: Account) {
    require(!(accounts.containsKey(account.uid))) {
      "Account with UID ${account.uid} already exists"
    }
    accounts[account.uid] = account
    accountUpdates.value = Pair(account.uid, account)
  }

  override suspend fun getAccount(userID: String): Account {
    if (accounts.containsKey(userID)) {
      return accounts[userID] ?: throw NoSuchElementException("Account with ID $userID not found")
    } else {
      throw NoSuchElementException("Account with ID $userID not found")
    }
  }

  override suspend fun accountExists(userID: String): Boolean {
    val username = getAccount(userID).username
    return username.isNotBlank()
  }

  override suspend fun addFriend(userID: String, friendID: String): Boolean {
    val account = getAccount(userID)

    if (!accounts.containsKey(friendID)) {
      throw NoSuchElementException("Friend with ID $friendID not found")
    }

    if (account.friendUids.any { it == friendID }) {
      return true
    }

    val updatedFriendUids = account.friendUids + friendID
    val updatedAccount = account.copy(friendUids = updatedFriendUids)
    accounts[userID] = updatedAccount
    accountUpdates.value = Pair(userID, updatedAccount)
    return true
  }

  override suspend fun removeFriend(userID: String, friendID: String) {
    val account = getAccount(userID)

    if (!accounts.containsKey(friendID)) {
      throw NoSuchElementException("Friend with ID $friendID not found")
    }

    if (account.friendUids.none { it == friendID }) {
      return
    }

    val updatedFriendUids = account.friendUids - friendID
    val updatedAccount = account.copy(friendUids = updatedFriendUids)
    accounts[userID] = updatedAccount
    accountUpdates.value = Pair(userID, updatedAccount)
  }

  override suspend fun isMyFriend(userID: String, friendID: String): Boolean {
    val account = getAccount(currentUser)
    return account.friendUids.isNotEmpty() && account.friendUids.any { it == friendID }
  }

  override suspend fun deleteAccount(userID: String) {
    if (accounts.containsKey(userID)) {
      publicLocations.remove(userID)
      publicLocationUpdates.value = publicLocations.values.toList()
      accounts.remove(userID)
    } else {
      throw NoSuchElementException("Account with ID $userID not found")
    }
  }

  override suspend fun editAccount(
      userID: String,
      username: String,
      birthDay: String,
      picture: String,
      location: Location
  ) {
    val acc = getAccount(userID)
    val updatedAccount =
        acc.copy(
            username = username.takeIf { it.isNotBlank() } ?: acc.username,
            birthday = birthDay.takeIf { it.isNotBlank() } ?: acc.birthday,
            profilePicture = picture.takeIf { it.isNotBlank() } ?: acc.profilePicture,
            location = location.takeIf { isValidLocation(it) } ?: acc.location)
    accounts[userID] = updatedAccount
    accountUpdates.value = Pair(userID, updatedAccount)

    if (!updatedAccount.isPrivate) {
      val publicLocation =
          PublicLocation(
              ownerId = updatedAccount.ownerId,
              username = updatedAccount.username,
              location = updatedAccount.location)
      publicLocations[userID] = publicLocation
      publicLocationUpdates.value = publicLocations.values.toList()
    }
  }

  override suspend fun deleteProfilePicture(userID: String) {
    val user = getAccount(userID)
    val updatedAccount = user.copy(profilePicture = "")
    accounts[userID] = updatedAccount
    accountUpdates.value = Pair(userID, updatedAccount)
  }

  override suspend fun togglePrivacy(userID: String): Boolean {
    val account = getAccount(userID)

    if (account.isPrivate && !isValidLocation(account.location)) {
      throw InvalidLocationException()
    }

    val updatedAccount = account.copy(isPrivate = !account.isPrivate)
    accounts[userID] = updatedAccount
    accountUpdates.value = Pair(userID, updatedAccount)

    if (!updatedAccount.isPrivate) {
      val publicLocation =
          PublicLocation(
              ownerId = updatedAccount.ownerId,
              username = updatedAccount.username,
              location = updatedAccount.location)
      publicLocations[userID] = publicLocation
      publicLocationUpdates.value = publicLocations.values.toList()
    } else {
      publicLocations.remove(userID)
      publicLocationUpdates.value = publicLocations.values.toList()
    }

    return updatedAccount.isPrivate
  }

  override suspend fun getItemsList(userID: String): List<String> {
    val account = getAccount(userID)
    return account.itemsUids
  }

  override suspend fun addItem(itemUid: String): Boolean {
    val account = getAccount(currentUser)
    if (account.itemsUids.contains(itemUid)) {
      return true
    }
    val updatedItemsUids = account.itemsUids + itemUid
    val updatedAccount = account.copy(itemsUids = updatedItemsUids)
    accounts[currentUser] = updatedAccount
    accountUpdates.value = Pair(currentUser, updatedAccount)
    return true
  }

  override suspend fun removeItem(itemUid: String): Boolean {
    val account = getAccount(currentUser)
    if (!account.itemsUids.contains(itemUid)) {
      return true
    }
    val updatedItemsUids = account.itemsUids - itemUid
    val updatedAccount = account.copy(itemsUids = updatedItemsUids)
    accounts[currentUser] = updatedAccount
    accountUpdates.value = Pair(currentUser, updatedAccount)
    return true
  }

  override fun observeAccount(userID: String): Flow<Account> {
    val initialAccount =
        accounts[userID] ?: throw NoSuchElementException("Account with ID $userID not found")

    return accountUpdates
        .filter { (uid, _) -> uid == userID }
        .map { (_, account) ->
          account ?: throw NoSuchElementException("Account with ID $userID not found")
        }
        .onStart { emit(initialAccount) }
  }

  override suspend fun getStarredItems(userID: String): List<String> {
    val account = getAccount(userID)
    return account.starredItemUids
  }

  override suspend fun refreshStarredItems(userID: String): List<String> = getStarredItems(userID)

  override suspend fun addStarredItem(itemUid: String): Boolean {
    val account = getAccount(currentUser)
    if (account.starredItemUids.contains(itemUid)) return true
    accounts[currentUser] = account.copy(starredItemUids = account.starredItemUids + itemUid)
    return true
  }

  override suspend fun removeStarredItem(itemUid: String): Boolean {
    val account = getAccount(currentUser)
    if (!account.starredItemUids.contains(itemUid)) return true
    accounts[currentUser] = account.copy(starredItemUids = account.starredItemUids - itemUid)
    return true
  }

  override suspend fun toggleStarredItem(itemUid: String): List<String> {
    val account = getAccount(currentUser)
    val updatedList =
        if (account.starredItemUids.contains(itemUid)) {
          account.starredItemUids - itemUid
        } else {
          account.starredItemUids + itemUid
        }
    accounts[currentUser] = account.copy(starredItemUids = updatedList)
    return updatedList
  }

  override suspend fun getPublicLocations(): List<PublicLocation> {
    return publicLocations.values.toList()
  }

  override fun observePublicLocations(): Flow<List<PublicLocation>> {
    return publicLocationUpdates.onStart { emit(publicLocations.values.toList()) }
  }
}
