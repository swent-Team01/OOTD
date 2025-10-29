package com.android.ootd.model.account

import com.android.ootd.model.user.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Test generated with the help of AI
class AccountRepositoryInMemoryTest {
  private lateinit var repository: AccountRepositoryInMemory

  // Test date of birth constant
  private val testDateOfBirth = "2000-01-01"
  private val testEmail = "test@example.com"
  private val testProfilePicture = "https://example.com/profile.jpg"

  @Before
  fun setUp() {
    repository = AccountRepositoryInMemory()
  }

  @Test
  fun getAccount_returnsCorrectAccount() = runTest {
    val account = repository.getAccount("user1")

    assertEquals("user1", account.uid)
    assertEquals("user1", account.ownerId)
    assertEquals("alice_wonder", account.username)
    assertEquals(2, account.friendUids.size)
  }

  @Test
  fun getAccount_throwsExceptionWhenAccountNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.getAccount("nonexistent") }
        }

    assertEquals("Account with ID nonexistent not found", exception.message)
  }

  @Test
  fun accountExists_worksCorrectlyForAccountWithUsername() = runTest {
    val exists = repository.accountExists("user1")
    assertTrue(exists)
  }

  @Test
  fun accountExists_throwsIfUidDoesNotExist() {
    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.accountExists("I do not exist") }
    }
  }

  @Test
  fun accountExists_worksWithNonexistentUsername() = runTest {
    val exists = repository.accountExists("nonRegisterUser")
    assertTrue(!exists)
  }

  @Test
  fun addAccount_successfullyAddsNewAccount() = runTest {
    val newAccount =
        Account(
            uid = "user6", ownerId = "user6", username = "frank_sinatra", friendUids = emptyList())

    repository.addAccount(newAccount)
    val retrievedAccount = repository.getAccount("user6")

    assertEquals("user6", retrievedAccount.uid)
    assertEquals("user6", retrievedAccount.ownerId)
    assertEquals("frank_sinatra", retrievedAccount.username)
    assertTrue(retrievedAccount.friendUids.isEmpty())
  }

  @Test
  fun addAccount_throwsExceptionWhenAccountAlreadyExists() {
    val duplicateAccount =
        Account(
            uid = "user1", ownerId = "user1", username = "duplicate_user", friendUids = emptyList())

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runTest { repository.addAccount(duplicateAccount) }
        }

    assertEquals("Account with UID user1 already exists", exception.message)
  }

  @Test
  fun addFriend_successfullyAddsFriend() = runTest {
    repository.addFriend("user3", "user1")

    val account = repository.getAccount("user3")
    assertEquals(1, account.friendUids.size)
    assertEquals("user1", account.friendUids[0])
  }

  @Test
  fun addFriend_throwsExceptionWhenAccountNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.addFriend("nonexistent", "user1") }
        }

    assertEquals("Account with ID nonexistent not found", exception.message)
  }

  @Test
  fun addFriend_throwsExceptionWhenFriendNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.addFriend("user3", "nonexistent") }
        }

    assertEquals("Friend with ID nonexistent not found", exception.message)
  }

  @Test
  fun addFriend_doesNotAddDuplicateFriend() = runTest {
    val initialFriendCount = repository.getAccount("user1").friendUids.size

    // Try to add user2 who is already a friend of user1
    repository.addFriend("user1", "user2")

    val account = repository.getAccount("user1")
    assertEquals(initialFriendCount, account.friendUids.size)
  }

  @Test
  fun addFriend_preservesExistingFriends() = runTest {
    repository.addFriend("user3", "user1")
    repository.addFriend("user3", "user2")

    val account = repository.getAccount("user3")
    assertEquals(2, account.friendUids.size)
    assertTrue(account.friendUids.any { it == "user1" })
    assertTrue(account.friendUids.any { it == "user2" })
  }

  @Test
  fun addFriend_doesNotAffectOtherAccounts() = runTest {
    val account2BeforeAdd = repository.getAccount("user2")

    repository.addFriend("user3", "user1")

    val account2AfterAdd = repository.getAccount("user2")
    assertEquals(account2BeforeAdd.friendUids.size, account2AfterAdd.friendUids.size)
  }

  @Test
  fun addFriend_canAddMultipleFriendsSequentially() = runTest {
    repository.addFriend("user5", "user1")
    repository.addFriend("user5", "user2")
    repository.addFriend("user5", "user3")

    val account = repository.getAccount("user5")
    assertEquals(3, account.friendUids.size)
  }

  @Test
  fun removeFriend_successfullyRemovesFriend() = runTest {
    repository.removeFriend("user1", "user2")

    val account = repository.getAccount("user1")
    assertEquals(1, account.friendUids.size)
    assertTrue(account.friendUids.none { it == "user2" })
  }

  @Test
  fun removeFriend_throwsExceptionWhenAccountNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.removeFriend("nonexistent", "user1") }
        }

    assertEquals("Account with ID nonexistent not found", exception.message)
  }

  @Test
  fun removeFriend_throwsExceptionWhenFriendNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.removeFriend("user1", "nonexistent") }
        }

    assertEquals("Friend with ID nonexistent not found", exception.message)
  }

  @Test
  fun removeFriend_doesNothingWhenFriendNotInList() = runTest {
    val initialFriendCount = repository.getAccount("user3").friendUids.size

    // user3 has no friends, try to remove user1
    repository.removeFriend("user3", "user1")

    val account = repository.getAccount("user3")
    assertEquals(initialFriendCount, account.friendUids.size)
  }

  @Test
  fun isMyFriend_returnsTrueForExistingFriend() = runTest {
    // user1 has user2 as a friend
    val isFriend = repository.isMyFriend("user1", "user2")
    assertTrue(isFriend)
  }

  @Test
  fun isMyFriend_returnsFalseForNonFriend() = runTest {
    // user1 does not have user5 as a friend
    val isFriend = repository.isMyFriend("user1", "user5")
    assertTrue(!isFriend)
  }

  @Test
  fun createAccount_successfullyCreatesNewAccount() = runTest {
    val user =
        User(uid = "user6", username = "george_washington", profilePicture = testProfilePicture)

    repository.createAccount(user, testEmail, dateOfBirth = testDateOfBirth)
    val account = repository.getAccount("user6")

    assertEquals("user6", account.uid)
    assertEquals("user6", account.ownerId)
    assertEquals("george_washington", account.username)
    assertEquals(user.profilePicture, account.profilePicture)
    assertEquals(account.googleAccountEmail, testEmail)
    assertTrue(account.friendUids.isEmpty())
  }

  @Test
  fun createAccount_throwsExceptionForDuplicateUsername() {
    val user1 = User(uid = "newUser1", username = "duplicate_user", profilePicture = "")
    val user2 = User(uid = "newUser2", username = "duplicate_user", profilePicture = "")

    val exception =
        assertThrows(TakenUserException::class.java) {
          runTest {
            repository.createAccount(user1, testEmail, dateOfBirth = testDateOfBirth)
            repository.createAccount(
                user2, testEmail, dateOfBirth = testDateOfBirth) // Should throw
          }
        }

    assertEquals("Username already in use", exception.message)
  }

  @Test
  fun createAccount_allowsBlankUsernamesForDifferentUsers() = runTest {
    val user1 = User(uid = "tempUser1", username = "", profilePicture = "")
    val user2 = User(uid = "tempUser2", username = "", profilePicture = "")

    repository.createAccount(user1, testEmail, dateOfBirth = testDateOfBirth)
    repository.createAccount(
        user2, "another@example.com", dateOfBirth = testDateOfBirth) // Should not throw

    val account1 = repository.getAccount("tempUser1")
    val account2 = repository.getAccount("tempUser2")

    assertEquals("", account1.username)
    assertEquals("", account2.username)
  }

  @Test
  fun removeAccount_successfullyRemovesAccount() {
    // Perform removal inside a runTest to call suspend code if needed
    runTest { repository.removeAccount("user5") }

    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.getAccount("user5") }
        }

    assertEquals("Account with ID user5 not found", exception.message)
  }

  @Test
  fun createAccount_handlesEmptyEmailCorrectly() = runTest {
    val user = User(uid = "user8", username = "empty_email_user", profilePicture = "")
    val emptyEmail = ""

    repository.createAccount(user, emptyEmail, dateOfBirth = testDateOfBirth)
    val account = repository.getAccount("user8")

    assertEquals(emptyEmail, account.googleAccountEmail)
    assertEquals("user8", account.uid)
  }

  @Test
  fun createAccount_allowsSameEmailForDifferentUsers() = runTest {
    val user1 = User(uid = "user9", username = "first_user", profilePicture = "")
    val user2 = User(uid = "user10", username = "second_user", profilePicture = "")
    val sharedEmail = "shared@example.com"

    repository.createAccount(user1, sharedEmail, dateOfBirth = testDateOfBirth)
    repository.createAccount(user2, sharedEmail, dateOfBirth = testDateOfBirth) // Should not throw

    val account1 = repository.getAccount("user9")
    val account2 = repository.getAccount("user10")

    assertEquals(sharedEmail, account1.googleAccountEmail)
    assertEquals(sharedEmail, account2.googleAccountEmail)
    assertEquals("first_user", account1.username)
    assertEquals("second_user", account2.username)
  }
}
