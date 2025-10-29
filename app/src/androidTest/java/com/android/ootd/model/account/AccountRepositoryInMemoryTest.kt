package com.android.ootd.model.account

import com.android.ootd.model.user.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests generated with the help of AI
class AccountRepositoryInMemoryTest {
  private lateinit var repository: AccountRepositoryInMemory

  // Test date of birth constant
  private val testDateOfBirth = "2000-01-01"

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
    val user = User(uid = "user6", username = "george_washington")

    repository.createAccount(user, testDateOfBirth)
    val account = repository.getAccount("user6")

    assertEquals("user6", account.uid)
    assertEquals("user6", account.ownerId)
    assertEquals("george_washington", account.username)
    assertTrue(account.friendUids.isEmpty())
  }

  @Test
  fun createAccount_throwsExceptionForDuplicateUsername() {
    val user1 = User(uid = "newUser1", username = "duplicate_user")
    val user2 = User(uid = "newUser2", username = "duplicate_user")

    val exception =
        assertThrows(TakenUserException::class.java) {
          runTest {
            repository.createAccount(user1, testDateOfBirth)
            repository.createAccount(user2, testDateOfBirth) // Should throw
          }
        }

    assertEquals("Username already in use", exception.message)
  }

  @Test
  fun createAccount_allowsBlankUsernamesForDifferentUsers() = runTest {
    val user1 = User(uid = "tempUser1", username = "")
    val user2 = User(uid = "tempUser2", username = "")

    repository.createAccount(user1, testDateOfBirth)
    repository.createAccount(user2, testDateOfBirth) // Should not throw

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
  fun togglePrivacy_togglesAndReturnsNewValue() = runTest {
    val initial = repository.getAccount("user1").isPrivate

    val returned = repository.togglePrivacy("user1")

    assertEquals(!initial, returned)
    assertEquals(!initial, repository.getAccount("user1").isPrivate)
  }

  @Test
  fun togglePrivacy_doubleToggleRestoresOriginalState() = runTest {
    val initial = repository.getAccount("user1").isPrivate

    repository.togglePrivacy("user1")
    repository.togglePrivacy("user1")

    val current = repository.getAccount("user1").isPrivate
    assertEquals(initial, current)
  }

  @Test
  fun togglePrivacy_doesNotAffectOtherAccounts() = runTest {
    val user1Initial = repository.getAccount("user1").isPrivate
    val user2Initial = repository.getAccount("user2").isPrivate

    repository.togglePrivacy("user1")

    assertEquals(!user1Initial, repository.getAccount("user1").isPrivate)
    assertEquals(user2Initial, repository.getAccount("user2").isPrivate)
  }

  @Test
  fun togglePrivacy_throwsExceptionWhenAccountNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.togglePrivacy("nonexistent") }
        }
    assertEquals("Account with ID nonexistent not found", exception.message)
  }
}
