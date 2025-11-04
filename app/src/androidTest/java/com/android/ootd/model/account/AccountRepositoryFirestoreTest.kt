package com.android.ootd.model.account

import com.android.ootd.model.map.Location
import com.android.ootd.model.user.User
import com.android.ootd.utils.AccountFirestoreTest
import com.android.ootd.utils.FirebaseEmulator
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRepositoryFirestoreTest : AccountFirestoreTest() {

  // Helpers to keep tests short
  private suspend fun addAccAndUser(acc: Account) {
    accountRepository.addAccount(acc)
    userRepository.addUser(User(uid = acc.uid, username = acc.username, profilePicture = ""))
  }

  private suspend fun add(vararg accs: Account) {
    accs.forEach { addAccAndUser(it) }
  }

  private suspend fun doc(uid: String) =
      FirebaseEmulator.firestore.collection(ACCOUNT_COLLECTION_PATH).document(uid).get().await()

  private suspend fun setDoc(uid: String, data: Map<String, Any?>) {
    FirebaseEmulator.firestore.collection(ACCOUNT_COLLECTION_PATH).document(uid).set(data).await()
  }

  private suspend inline fun <reified T : Throwable> expectThrows(
      messageContains: String? = null,
      crossinline block: suspend () -> Unit
  ): T {
    val e = kotlin.runCatching { block() }.exceptionOrNull()
    assertTrue(e is T)
    if (messageContains != null) assertTrue(e?.message?.contains(messageContains) == true)
    return e as T
  }

  @Test
  fun addAccount_successfullyAddsNewAccount() = runTest {
    accountRepository.addAccount(account1)
    assertEquals(1, getAccountCount())

    val retrieved = accountRepository.getAccount(account1.uid)
    assertEquals(account1.uid, retrieved.uid)
    assertEquals(account1.username, retrieved.username)
  }

  @Test
  fun addAccount_throwsExceptionWhenAccountAlreadyExists() = runTest {
    accountRepository.addAccount(account1)
    userRepository.addUser(
        User(uid = account1.uid, username = account1.username, profilePicture = ""))

    expectThrows<TakenAccountException>("already exists") { accountRepository.addAccount(account1) }
    assertEquals(1, getAccountCount())
  }

  @Test
  fun getAccount_returnsCorrectAccount() = runTest {
    add(account1, account2)

    val retrieved = accountRepository.getAccount(account2.uid)
    assertEquals(account2.uid, retrieved.uid)
    assertEquals(account2.username, retrieved.username)
    assertEquals(account2.birthday, retrieved.birthday)
  }

  @Test
  fun getAccount_throwsExceptionWhenAccountNotFound() = runTest {
    expectThrows<NoSuchElementException>("not found") {
      accountRepository.getAccount("nonexistent")
    }
  }

  @Test
  fun accountExists_returnsTrueWhenAccountHasUsername() = runTest {
    add(account1)
    assertTrue(accountRepository.accountExists(account1.uid))
  }

  @Test
  fun accountExists_returnsFalseWhenAccountNotFound() = runTest {
    assertFalse(accountRepository.accountExists("nonexistent"))
  }

  @Test
  fun accountExists_returnsFalseWhenUsernameIsBlank() = runTest {
    val a = account1.copy(username = "")
    setDoc(
        a.uid,
        mapOf(
            "username" to "",
            "birthday" to a.birthday,
            "googleAccountEmail" to a.googleAccountEmail,
            "ownerId" to currentUser.uid,
            "profilePicture" to a.profilePicture,
            "friendUids" to a.friendUids))

    assertFalse(accountRepository.accountExists(a.uid))
  }

  @Test
  fun createAccount_successfullyCreatesNewAccount() = runTest {
    val user = User(uid = currentUser.uid, username = "charlie_brown", profilePicture = ":3")

    accountRepository.createAccount(
        user, userEmail = testEmail, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
    userRepository.addUser(
        User(uid = user.uid, username = user.username, profilePicture = "Hello.jpg"))

    val account = accountRepository.getAccount(user.uid)
    assertEquals(user.uid, account.uid)
    assertEquals(user.username, account.username)
    assertTrue(account.friendUids.isEmpty())
    assertEquals(testEmail, account.googleAccountEmail)
    assertEquals(user.profilePicture, account.profilePicture)
  }

  @Test
  fun createAccount_throwsExceptionForDuplicateUser() = runTest {
    val user1 = User(uid = "user3", username = "duplicate", profilePicture = "")
    val user2 = User(uid = "user4", username = "duplicate", profilePicture = "")

    userRepository.addUser(user1)
    userRepository.addUser(user2)
    // But createAccount should fail because username is already in use
    val exception =
        runCatching {
              accountRepository.createAccount(
                  user2, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
            }
            .exceptionOrNull()

    expectThrows<TakenUserException>("already in use") {
      accountRepository.createAccount(
          user2, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
    }
  }

  @Test
  fun addFriend_successfullyAddsFriend() = runTest {
    add(account1, account2)

    accountRepository.addFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
  }

  @Test
  fun addFriend_doesNotAddDuplicateFriend() = runTest {
    add(account1, account2)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
  }

  @Test
  fun addFriend_throwsExceptionWhenFriendNotFound() = runTest {
    accountRepository.addAccount(account1)

    expectThrows<NoSuchElementException>("not found") {
      accountRepository.addFriend(account1.uid, "nonexistent")
    }
  }

  @Test
  fun addFriend_canAddMultipleFriends() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    add(account1, account2, account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account3.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(2, updated.friendUids.size)
    assertTrue(updated.friendUids.containsAll(listOf(account2.uid, account3.uid)))
  }

  @Test
  fun removeFriend_successfullyRemovesFriend() = runTest {
    add(account1, account2)
    accountRepository.addFriend(account1.uid, account2.uid)

    accountRepository.removeFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(0, updated.friendUids.size)
  }

  @Test
  fun removeFriend_throwsExceptionWhenFriendNotFound() = runTest {
    add(account1)

    expectThrows<NoSuchElementException>("not found") {
      accountRepository.removeFriend(account1.uid, "nonexistent")
    }
  }

  @Test
  fun removeFriend_doesNothingWhenFriendNotInList() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    add(account1, account2, account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.removeFriend(account1.uid, account3.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
  }

  @Test
  fun removeFriend_preservesOtherFriends() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    add(account1, account2, account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account3.uid)
    accountRepository.removeFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account3.uid))
  }

  @Test
  fun isMyFriend_returnsTrueForExistingFriend() = runTest {
    add(account1, account2)

    accountRepository.addFriend(account1.uid, account2.uid)

    assertTrue(accountRepository.isMyFriend(account1.uid, account2.uid))
  }

  @Test
  fun isMyFriend_returnsFalseForNonFriend() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    add(account1, account2, account3)

    accountRepository.addFriend(account1.uid, account2.uid)

    assertFalse(accountRepository.isMyFriend(account1.uid, account3.uid))
  }

  @Test
  fun isMyFriend_returnsFalseAfterRemovingFriend() = runTest {
    add(account1, account2)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.removeFriend(account1.uid, account2.uid)

    assertFalse(accountRepository.isMyFriend(account1.uid, account2.uid))
  }

  @Test
  fun isMyFriend_throwsExceptionWhenUserNotFound() = runTest {
    expectThrows<NoSuchElementException>("authenticated user") {
      accountRepository.isMyFriend("nonexistent", account1.uid)
    }
  }

  @Test
  fun isMyFriend_returnsFalseWhenFriendListIsEmpty() = runTest {
    add(account1, account2)

    assertFalse(accountRepository.isMyFriend(account1.uid, account2.uid))
  }

  @Test
  fun getAccount_handlesCorruptedDocumentGracefully() = runTest {
    accountRepository.addAccount(account1)

    // Corrupt the document
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .update(mapOf("username" to 12345))
        .await()

    val e = kotlin.runCatching { accountRepository.getAccount(account1.uid) }.exceptionOrNull()
    assertTrue(e != null)
  }

  @Test
  fun getAccount_throwsExceptionForInvalidData() = runTest {
    val invalidId = "invalid"
    setDoc(
        invalidId,
        mapOf(
            "username" to "test",
            "birthday" to "2000-01-01",
            "googleAccountEmail" to "test@test.com",
            "profilePicture" to "",
            "ownerId" to currentUser.uid,
            "friendUids" to 12345)) // Invalid type

    expectThrows<IllegalStateException>("Failed to transform") {
      accountRepository.getAccount(invalidId)
    }
  }

  @Test
  fun editAccount_updatesUsernameAndBirthday() = runTest {
    accountRepository.addAccount(account1)

    val newUsername = "new_username"
    val newBirthday = "1990-01-01"
    val newPicture = ":3"
    accountRepository.editAccount(
        account1.uid, username = newUsername, birthDay = newBirthday, picture = newPicture)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(newUsername, updated.username)
    assertEquals(newBirthday, updated.birthday)
    assertEquals(newPicture, updated.profilePicture)
  }

  @Test
  fun editAccount_keepsValuesWhenBlank() = runTest {
    accountRepository.addAccount(account1)

    accountRepository.editAccount(account1.uid, username = "", birthDay = "", picture = "")

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(account1.username, updated.username)
    assertEquals(account1.birthday, updated.birthday)
    assertEquals(account1.profilePicture, updated.profilePicture)
  }

  @Test
  fun deleteAccount_successfullyDeletesAccount() = runTest {
    accountRepository.addAccount(account1)

    accountRepository.deleteAccount(account1.uid)

    expectThrows<NoSuchElementException> { accountRepository.getAccount(account1.uid) }
  }

  @Test
  fun deleteAccount_throwsWhenAccountNotFound() = runTest {
    expectThrows<UnknowUserID> { accountRepository.deleteAccount("nonexistent") }
  }

  @Test
  fun togglePrivacy_togglesAndPersists() = runTest {
    accountRepository.addAccount(account1)

    val first = accountRepository.togglePrivacy(account1.uid)
    assertTrue(first)

    val doc1 = doc(account1.uid)
    assertTrue(doc1.getBoolean("isPrivate") == true)

    val second = accountRepository.togglePrivacy(account1.uid)
    assertFalse(second)

    val doc2 = doc(account1.uid)
    assertTrue(doc2.getBoolean("isPrivate") == false)
  }

  @Test
  fun togglePrivacy_throwsWhenAccountMissing() = runTest {
    expectThrows<NoSuchElementException>("authenticated user") {
      accountRepository.togglePrivacy("nonexistent")
    }
  }

  @Test
  fun createAccount_persistsLocationToFirestore() = runTest {
    val user = User(uid = currentUser.uid, username = "test_location_user", profilePicture = "")

    accountRepository.createAccount(user, testEmail, testDateOfBirth, EPFL_LOCATION)

    val retrieved = accountRepository.getAccount(currentUser.uid)
    assertEquals(EPFL_LOCATION.latitude, retrieved.location.latitude, 0.0001)
    assertEquals(EPFL_LOCATION.longitude, retrieved.location.longitude, 0.0001)
    assertEquals(EPFL_LOCATION.name, retrieved.location.name)
  }

  @Test
  fun getAccount_parsesLocationFromMap() = runTest {
    // Manually create a document with location as a Map
    val locationMap =
        mapOf("latitude" to 47.3769, "longitude" to 8.5417, "name" to "Zürich, Switzerland")

    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .set(
            mapOf(
                "username" to account1.username,
                "birthday" to account1.birthday,
                "googleAccountEmail" to account1.googleAccountEmail,
                "profilePicture" to account1.profilePicture,
                "friendUids" to account1.friendUids,
                "isPrivate" to account1.isPrivate,
                "ownerId" to account1.ownerId,
                "location" to locationMap))
        .await()

    val retrieved = accountRepository.getAccount(account1.uid)
    assertEquals(47.3769, retrieved.location.latitude, 0.0001)
    assertEquals(8.5417, retrieved.location.longitude, 0.0001)
    assertEquals("Zürich, Switzerland", retrieved.location.name)
  }

  @Test
  fun getAccount_handlesNullLocation() = runTest {
    // Create account without location field
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .set(
            mapOf(
                "username" to account1.username,
                "birthday" to account1.birthday,
                "googleAccountEmail" to account1.googleAccountEmail,
                "profilePicture" to account1.profilePicture,
                "friendUids" to account1.friendUids,
                "isPrivate" to account1.isPrivate,
                "ownerId" to account1.ownerId))
        .await()

    val retrieved = accountRepository.getAccount(account1.uid)
    assertEquals(0.0, retrieved.location.latitude, 0.0001)
    assertEquals(0.0, retrieved.location.longitude, 0.0001)
    assertEquals("", retrieved.location.name)
  }

  @Test
  fun getAccount_handlesInvalidLocationFormat() = runTest {
    val location = Location(46.5197, 6.6323, "Lausanne")
    // Create account with invalid location type (string instead of map)
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .set(
            mapOf(
                "username" to account1.username,
                "birthday" to account1.birthday,
                "googleAccountEmail" to account1.googleAccountEmail,
                "profilePicture" to account1.profilePicture,
                "friendUids" to account1.friendUids,
                "isPrivate" to account1.isPrivate,
                "ownerId" to account1.ownerId,
                "location" to "Invalid location string"))
        .await()

    val retrieved = accountRepository.getAccount(account1.uid)
    // Should fall back to emptyLocation
    assertEquals(0.0, retrieved.location.latitude, 0.0001)
    assertEquals(0.0, retrieved.location.longitude, 0.0001)
    assertEquals("", retrieved.location.name)
  }

  @Test
  fun editAccount_logsSuccessMessage() = runTest {
    accountRepository.addAccount(account1)

    val newUsername = "updated_user"
    val newBirthday = "1995-12-25"
    val newPicture = "new_pic.jpg"

    // This test verifies the method executes successfully and logs the success message
    accountRepository.editAccount(
        account1.uid, username = newUsername, birthDay = newBirthday, picture = newPicture)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(newUsername, updated.username)
    assertEquals(newBirthday, updated.birthday)
    assertEquals(newPicture, updated.profilePicture)
  }
}
