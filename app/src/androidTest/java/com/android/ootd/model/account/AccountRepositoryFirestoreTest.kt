package com.android.ootd.model.account

import com.android.ootd.model.user.User
import com.android.ootd.utils.AccountFirestoreTest
import com.android.ootd.utils.FirebaseEmulator
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRepositoryFirestoreTest : AccountFirestoreTest() {

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

    val exception = runCatching { accountRepository.addAccount(account1) }.exceptionOrNull()

    assertTrue(exception is TakenAccountException)
    assertTrue(exception?.message?.contains("already exists") == true)
    assertEquals(1, getAccountCount())
  }

  @Test
  fun getAccount_returnsCorrectAccount() = runTest {
    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    val retrieved = accountRepository.getAccount(account2.uid)
    assertEquals(account2.uid, retrieved.uid)
    assertEquals(account2.username, retrieved.username)
    assertEquals(account2.birthday, retrieved.birthday)
  }

  @Test
  fun getAccount_throwsExceptionWhenAccountNotFound() = runTest {
    val exception = runCatching { accountRepository.getAccount("nonexistent") }.exceptionOrNull()

    assertTrue(exception is NoSuchElementException)
    assertTrue(exception?.message?.contains("not found") == true)
  }

  @Test
  fun accountExists_returnsTrueWhenAccountHasUsername() = runTest {
    accountRepository.addAccount(account1)

    val exists = accountRepository.accountExists(account1.uid)
    assertTrue(exists)
  }

  @Test
  fun accountExists_returnsFalseWhenAccountNotFound() = runTest {
    val exists = accountRepository.accountExists("nonexistent")
    assertTrue(!exists)
  }

  @Test
  fun accountExists_returnsFalseWhenUsernameIsBlank() = runTest {
    val accountWithBlankUsername = account1.copy(username = "")
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(accountWithBlankUsername.uid)
        .set(
            mapOf(
                "username" to "",
                "birthday" to accountWithBlankUsername.birthday,
                "googleAccountEmail" to accountWithBlankUsername.googleAccountEmail,
                "profilePicture" to accountWithBlankUsername.profilePicture,
                "friendUids" to accountWithBlankUsername.friendUids))
        .await()

    val exists = accountRepository.accountExists(accountWithBlankUsername.uid)
    assertTrue(!exists)
  }

  @Test
  fun createAccount_successfullyCreatesNewAccount() = runTest {
    val user = User(uid = "user3", username = "charlie_brown")

    accountRepository.createAccount(user, testDateOfBirth)

    val account = accountRepository.getAccount(user.uid)
    assertEquals(user.uid, account.uid)
    assertEquals(user.username, account.username)
    assertTrue(account.friendUids.isEmpty())
  }

  @Test
  fun createAccount_throwsExceptionForDuplicateUser() = runTest {
    val user1 = User(uid = "user3", username = "duplicate")
    val user2 = User(uid = "user4", username = "duplicate")

    // Add user1 to users collection first, then create account
    userRepository.addUser(user1)
    accountRepository.createAccount(user1, testDateOfBirth)

    // Add user2 to users collection - this should be allowed since different uid
    userRepository.addUser(user2)
    // But createAccount should fail because username is already in use
    val exception =
        runCatching { accountRepository.createAccount(user2, testDateOfBirth) }.exceptionOrNull()

    assertTrue(exception is TakenUserException)
    assertTrue(exception?.message?.contains("already in use") == true)
  }

  @Test
  fun createAccount_allowsBlankUsernamesForDifferentUsers() = runTest {
    val user1 = User(uid = "user3", username = "")
    val user2 = User(uid = "user4", username = "")

    accountRepository.createAccount(user1, testDateOfBirth)
    accountRepository.createAccount(user2, testDateOfBirth)

    assertEquals(2, getAccountCount())
  }

  @Test
  fun addFriend_successfullyAddsFriend() = runTest {
    // Create User records first (required for addFriend to work with privacy rules)
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    accountRepository.addFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
  }

  @Test
  fun addFriend_doesNotAddDuplicateFriend() = runTest {
    // Create User records first
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
  }

  @Test
  fun addFriend_throwsExceptionWhenFriendNotFound() = runTest {
    accountRepository.addAccount(account1)

    val exception =
        runCatching { accountRepository.addFriend(account1.uid, "nonexistent") }.exceptionOrNull()

    assertTrue(exception is NoSuchElementException)
    assertTrue(exception?.message?.contains("not found") == true)
  }

  @Test
  fun addFriend_canAddMultipleFriends() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    // Create User records first
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))
    userRepository.addUser(User(uid = account3.uid, username = account3.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addAccount(account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account3.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(2, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
    assertTrue(updated.friendUids.contains(account3.uid))
  }

  @Test
  fun removeFriend_successfullyRemovesFriend() = runTest {
    // Create User records first
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addFriend(account1.uid, account2.uid)

    accountRepository.removeFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(0, updated.friendUids.size)
  }

  @Test
  fun removeFriend_throwsExceptionWhenFriendNotFound() = runTest {
    accountRepository.addAccount(account1)

    val exception =
        runCatching { accountRepository.removeFriend(account1.uid, "nonexistent") }
            .exceptionOrNull()

    assertTrue(exception is NoSuchElementException)
    assertTrue(exception?.message?.contains("not found") == true)
  }

  @Test
  fun removeFriend_doesNothingWhenFriendNotInList() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    // Create User records first
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))
    userRepository.addUser(User(uid = account3.uid, username = account3.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addAccount(account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.removeFriend(account1.uid, account3.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
  }

  @Test
  fun removeFriend_preservesOtherFriends() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    // Create User records first
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))
    userRepository.addUser(User(uid = account3.uid, username = account3.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addAccount(account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account3.uid)
    accountRepository.removeFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account3.uid))
  }

  @Test
  fun isMyFriend_returnsTrueForExistingFriend() = runTest {
    // Create User records first
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addFriend(account1.uid, account2.uid)

    val isFriend = accountRepository.isMyFriend(account1.uid, account2.uid)
    assertTrue(isFriend)
  }

  @Test
  fun isMyFriend_returnsFalseForNonFriend() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    // Create User records first
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))
    userRepository.addUser(User(uid = account3.uid, username = account3.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addAccount(account3)

    accountRepository.addFriend(account1.uid, account2.uid)

    val isFriend = accountRepository.isMyFriend(account1.uid, account3.uid)
    assertTrue(!isFriend)
  }

  @Test
  fun isMyFriend_returnsFalseAfterRemovingFriend() = runTest {
    // Create User records first
    userRepository.addUser(User(uid = account1.uid, username = account1.username))
    userRepository.addUser(User(uid = account2.uid, username = account2.username))

    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)
    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.removeFriend(account1.uid, account2.uid)

    val isFriend = accountRepository.isMyFriend(account1.uid, account2.uid)
    assertTrue(!isFriend)
  }

  @Test
  fun isMyFriend_throwsExceptionWhenUserNotFound() = runTest {
    val exception =
        runCatching { accountRepository.isMyFriend("nonexistent", account1.uid) }.exceptionOrNull()

    assertTrue(exception is NoSuchElementException)
    assertTrue(exception?.message?.contains("authenticated user") == true)
  }

  @Test
  fun isMyFriend_returnsFalseWhenFriendListIsEmpty() = runTest {
    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    val isFriend = accountRepository.isMyFriend(account1.uid, account2.uid)
    assertTrue(!isFriend)
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

    val exception = runCatching { accountRepository.getAccount(account1.uid) }.exceptionOrNull()

    assertTrue(exception != null)
  }

  @Test
  fun getAccount_throwsExceptionForInvalidData() = runTest {
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
                "friendUids" to 12345 // Invalid type - not a List
                ))
        .await()

    val exception = runCatching { accountRepository.getAccount(invalidAccountId) }.exceptionOrNull()

    assertTrue(exception is IllegalStateException)
    assertTrue(exception?.message?.contains("Failed to transform") == true)
  }
}
