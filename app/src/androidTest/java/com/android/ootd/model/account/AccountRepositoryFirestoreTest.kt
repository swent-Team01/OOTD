package com.android.ootd.model.account

import com.android.ootd.model.user.User
import com.android.ootd.utils.AccountFirestoreTest
import com.android.ootd.utils.FirebaseEmulator
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRepositoryFirestoreTest : AccountFirestoreTest() {

  @Test
  fun addAccount_successfullyAddsNewAccount() = runBlocking {
    accountRepository.addAccount(account1)
    assertEquals(1, getAccountCount())

    val retrieved = accountRepository.getAccount(account1.uid)
    assertEquals(account1.uid, retrieved.uid)
    assertEquals(account1.username, retrieved.username)
  }

  @Test
  fun addAccount_throwsExceptionWhenAccountAlreadyExists() = runBlocking {
    accountRepository.addAccount(account1)
    userRepository.addUser(User(uid = account1.uid, username = account1.username))

    val exception = runCatching { accountRepository.addAccount(account1) }.exceptionOrNull()

    assertTrue(exception is TakenAccountException)
    assertTrue(exception?.message?.contains("already exists") == true)
    assertEquals(1, getAccountCount())
  }

  @Test
  fun getAccount_returnsCorrectAccount() = runBlocking {
    accountRepository.addAccount(account1)
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    accountRepository.addAccount(account2)
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    val retrieved = accountRepository.getAccount(account2.uid)
    assertEquals(account2.uid, retrieved.uid)
    assertEquals(account2.username, retrieved.username)
    assertEquals(account2.birthday, retrieved.birthday)
  }

  @Test
  fun getAccount_throwsExceptionWhenAccountNotFound() = runBlocking {
    val exception = runCatching { accountRepository.getAccount("nonexistent") }.exceptionOrNull()

    assertTrue(exception is NoSuchElementException)
    assertTrue(exception?.message?.contains("not found") == true)
  }

  @Test
  fun accountExists_returnsTrueWhenAccountHasUsername() = runBlocking {
    accountRepository.addAccount(account1)
    userRepository.addUser(User(uid = account1.uid, username = account1.username))

    val exists = accountRepository.accountExists(account1.uid)
    assertTrue(exists)
  }

  @Test
  fun accountExists_returnsFalseWhenAccountNotFound() = runBlocking {
    val exists = accountRepository.accountExists("nonexistent")
    assertTrue(!exists)
  }

  @Test
  fun accountExists_returnsFalseWhenUsernameIsBlank() = runBlocking {
    val accountWithBlankUsername = account1.copy(username = "")
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(accountWithBlankUsername.uid)
        .set(
            mapOf(
                "username" to "",
                "birthday" to accountWithBlankUsername.birthday,
                "googleAccountEmail" to accountWithBlankUsername.googleAccountEmail,
                "ownerId" to currentUser.uid,
                "profilePicture" to accountWithBlankUsername.profilePicture,
                "friendUids" to accountWithBlankUsername.friendUids))
        .await()

    val exists = accountRepository.accountExists(accountWithBlankUsername.uid)
    assertTrue(!exists)
  }

  @Test
  fun createAccount_successfullyCreatesNewAccount() = runBlocking {
    val user = User(uid = currentUser.uid, username = "charlie_brown")

    accountRepository.createAccount(user, testDateOfBirth)
    userRepository.addUser(User(uid = user.uid, username = user.username))

    val account = accountRepository.getAccount(user.uid)
    assertEquals(user.uid, account.uid)
    assertEquals(user.username, account.username)
    assertTrue(account.friendUids.isEmpty())
  }

  @Test
  fun createAccount_throwsExceptionForDuplicateUser() = runBlocking {
    val user1 = User(uid = "user3", username = "duplicate")
    val user2 = User(uid = "user4", username = "duplicate")

    userRepository.addUser(user1)

    // Add user2 to users collection - this should be allowed since different uid
    userRepository.addUser(user2)
    // But createAccount should fail because username is already in use
    val exception =
        runCatching { accountRepository.createAccount(user2, testDateOfBirth) }.exceptionOrNull()

    assertTrue(exception is TakenUserException)
    assertTrue(exception?.message?.contains("already in use") == true)
  }

  @Test
  fun addFriend_successfullyAddsFriend() = runBlocking {
    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    // We add the users after because it is hard to mock two users creating their accounts with
    // different uids.

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
  }

  @Test
  fun addFriend_doesNotAddDuplicateFriend() = runBlocking {
    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
  }

  @Test
  fun addFriend_throwsExceptionWhenFriendNotFound() = runBlocking {
    accountRepository.addAccount(account1)

    val exception =
        runCatching { accountRepository.addFriend(account1.uid, "nonexistent") }.exceptionOrNull()

    assertTrue(exception is NoSuchElementException)
    assertTrue(exception?.message?.contains("not found") == true)
  }

  @Test
  fun addFriend_canAddMultipleFriends() = runBlocking {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    // Create User records first

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addAccount(account3)

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))
    userRepository.addUser(User(uid = account3.uid, username = account3.username))

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account3.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(2, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
    assertTrue(updated.friendUids.contains(account3.uid))
  }

  @Test
  fun removeFriend_successfullyRemovesFriend() = runBlocking {
    // Create User records first

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addFriend(account1.uid, account2.uid)

    accountRepository.removeFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(0, updated.friendUids.size)
  }

  @Test
  fun removeFriend_throwsExceptionWhenFriendNotFound() = runBlocking {
    accountRepository.addAccount(account1)
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    val exception =
        runCatching { accountRepository.removeFriend(account1.uid, "nonexistent") }
            .exceptionOrNull()

    assertTrue(exception is NoSuchElementException)
    assertTrue(exception?.message?.contains("not found") == true)
  }

  @Test
  fun removeFriend_doesNothingWhenFriendNotInList() = runBlocking {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addAccount(account3)

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))
    userRepository.addUser(User(uid = account3.uid, username = account3.username))

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.removeFriend(account1.uid, account3.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
  }

  @Test
  fun removeFriend_preservesOtherFriends() = runBlocking {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addAccount(account3)

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))
    userRepository.addUser(User(uid = account3.uid, username = account3.username))

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account3.uid)
    accountRepository.removeFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account3.uid))
  }

  @Test
  fun isMyFriend_returnsTrueForExistingFriend() = runBlocking {
    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addFriend(account1.uid, account2.uid)

    val isFriend = accountRepository.isMyFriend(account1.uid, account2.uid)
    assertTrue(isFriend)
  }

  @Test
  fun isMyFriend_returnsFalseForNonFriend() = runBlocking {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addAccount(account3)

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))
    userRepository.addUser(User(uid = account3.uid, username = account3.username))

    accountRepository.addFriend(account1.uid, account2.uid)

    val isFriend = accountRepository.isMyFriend(account1.uid, account3.uid)
    assertTrue(!isFriend)
  }

  @Test
  fun isMyFriend_returnsFalseAfterRemovingFriend() = runBlocking {
    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.removeFriend(account1.uid, account2.uid)

    val isFriend = accountRepository.isMyFriend(account1.uid, account2.uid)
    assertTrue(!isFriend)
  }

  @Test
  fun isMyFriend_throwsExceptionWhenUserNotFound() = runBlocking {
    val exception =
        runCatching { accountRepository.isMyFriend("nonexistent", account1.uid) }.exceptionOrNull()

    assertTrue(exception is NoSuchElementException)
    assertTrue(exception?.message?.contains("authenticated user") == true)
  }

  @Test
  fun isMyFriend_returnsFalseWhenFriendListIsEmpty() = runBlocking {
    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    val isFriend = accountRepository.isMyFriend(account1.uid, account2.uid)
    assertTrue(!isFriend)
  }

  @Test
  fun getAccount_handlesCorruptedDocumentGracefully() = runBlocking {
    accountRepository.addAccount(account1)

    // Corrupt the document
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .update(mapOf("username" to 12345))
        .await()

    val exception = runCatching { accountRepository.getAccount(account1.uid) }.exceptionOrNull()

    assertTrue(exception != null)
  }

  @Test
  fun getAccount_throwsExceptionForInvalidData() = runBlocking {
    val invalidAccountId = "invalid"
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(invalidAccountId)
        .set(
            mapOf(
                "username" to "test",
                "birthday" to "2000-01-01",
                "googleAccountEmail" to "test@test.com",
                "profilePicture" to "",
                "ownerId" to currentUser.uid,
                "friendUids" to 12345 // Invalid type - not a List
                ))
        .await()

    val exception = runCatching { accountRepository.getAccount(invalidAccountId) }.exceptionOrNull()

    assertTrue(exception is IllegalStateException)
    assertTrue(exception?.message?.contains("Failed to transform") == true)
  }

  // New tests for edit and delete account
  @Test
  fun editAccount_updatesUsernameAndBirthday() = runTest {
    accountRepository.addAccount(account1)

    val newUsername = "new_username"
    val newBirthday = "1990-01-01"

    accountRepository.editAccount(account1.uid, username = newUsername, birthDay = newBirthday)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(newUsername, updated.username)
    assertEquals(newBirthday, updated.birthday)
  }

  @Test
  fun editAccount_keepsValuesWhenBlank() = runTest {
    accountRepository.addAccount(account1)

    // Pass blank values - should preserve existing data
    accountRepository.editAccount(account1.uid, username = "", birthDay = "")

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(account1.username, updated.username)
    assertEquals(account1.birthday, updated.birthday)
  }

  @Test
  fun deleteAccount_successfullyDeletesAccount() = runTest {
    accountRepository.addAccount(account1)

    accountRepository.deleteAccount(account1.uid)

    val exception = runCatching { accountRepository.getAccount(account1.uid) }.exceptionOrNull()
    assertTrue(exception is NoSuchElementException)
  }

  @Test
  fun deleteAccount_throwsWhenAccountNotFound() = runTest {
    val exception = runCatching { accountRepository.deleteAccount("nonexistent") }.exceptionOrNull()
    assertTrue(exception is UnknowUserID)
  }
}
