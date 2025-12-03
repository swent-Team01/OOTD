package com.android.ootd.model.account

import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.user.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests generated with the help of AI
class AccountRepositoryInMemoryTest {
  private lateinit var repository: AccountRepositoryInMemory

  private val testDateOfBirth = "2000-01-01"
  private val testEmail = "test@example.com"
  private val testProfilePicture = "https://example.com/profile.jpg"
  private val EPFL_LOCATION =
      Location(46.5191, 6.5668, "École Polytechnique Fédérale de Lausanne (EPFL), Switzerland")

  @Before
  fun setUp() {
    repository = AccountRepositoryInMemory()
  }

  private fun acc(uid: String, username: String = uid) =
      Account(uid = uid, ownerId = uid, username = username, friendUids = emptyList())

  // Small helper for suspending exception assertions
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
  fun getAccount_returnsCorrectAccount() = runTest {
    val account = repository.getAccount("user1")
    assertEquals("user1", account.uid)
    assertEquals("alice_wonder", account.username)
    assertEquals(2, account.friendUids.size)
  }

  @Test
  fun getAccount_throwsWhenNotFound() = runTest {
    expectThrows<NoSuchElementException>("Account with ID nonexistent not found") {
      repository.getAccount("nonexistent")
    }
  }

  @Test
  fun accountExists_basicCases() = runTest {
    assertTrue(repository.accountExists("user1"))
    expectThrows<NoSuchElementException>("Account with ID I do not exist not found") {
      repository.accountExists("I do not exist")
    }
    assertTrue(!repository.accountExists("nonRegisterUser"))
  }

  @Test
  fun addAccount_addsAndRejectsDuplicates() = runTest {
    val a = acc("user6", "frank_sinatra")
    repository.addAccount(a)
    val r = repository.getAccount("user6")
    assertEquals("frank_sinatra", r.username)

    val dup = acc("user1", "duplicate_user")
    expectThrows<IllegalArgumentException>("Account with UID user1 already exists") {
      repository.addAccount(dup)
    }
  }

  @Test
  fun addFriend_variants() = runTest {
    repository.addFriend("user3", "user1")
    assertEquals(1, repository.getAccount("user3").friendUids.size)

    expectThrows<NoSuchElementException>("Account with ID nonexistent not found") {
      repository.addFriend("nonexistent", "user1")
    }

    expectThrows<NoSuchElementException>("Friend with ID nonexistent not found") {
      repository.addFriend("user3", "nonexistent")
    }

    val before = repository.getAccount("user1").friendUids.size
    repository.addFriend("user1", "user2") // duplicate
    assertEquals(before, repository.getAccount("user1").friendUids.size)

    repository.addFriend("user3", "user2")
    val f = repository.getAccount("user3").friendUids
    assertEquals(2, f.size)
    assertTrue("user1" in f && "user2" in f)
  }

  @Test
  fun removeFriend_variants() = runTest {
    repository.removeFriend("user1", "user2")
    assertTrue("user2" !in repository.getAccount("user1").friendUids)

    expectThrows<NoSuchElementException>("Account with ID nonexistent not found") {
      repository.removeFriend("nonexistent", "user1")
    }

    expectThrows<NoSuchElementException>("Friend with ID nonexistent not found") {
      repository.removeFriend("user1", "nonexistent")
    }

    val before = repository.getAccount("user3").friendUids.size
    repository.removeFriend("user3", "user1") // no-op
    assertEquals(before, repository.getAccount("user3").friendUids.size)
  }

  @Test
  fun isMyFriend_trueFalse() = runTest {
    assertTrue(repository.isMyFriend("user1", "user2"))
    assertTrue(!repository.isMyFriend("user1", "user5"))
  }

  @Test
  fun createAccount_variants() = runTest {
    val user =
        User(
            uid = "user6",
            ownerId = "user6",
            username = "george_washington",
            profilePicture = testProfilePicture)
    repository.createAccount(user, testEmail, dateOfBirth = testDateOfBirth, EPFL_LOCATION)
    val acc = repository.getAccount("user6")
    assertEquals(user.uid, acc.uid)
    assertEquals(user.username, acc.username)
    assertEquals(user.profilePicture, acc.profilePicture)
    assertEquals(testEmail, acc.googleAccountEmail)
    assertTrue(acc.friendUids.isEmpty())

    // Use a different user with different username for second test
    val user7 =
        User(
            uid = "user7",
            ownerId = "user7",
            username = "john_adams",
            profilePicture = testProfilePicture)
    repository.createAccount(
        user7, testEmail, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
    val account = repository.getAccount("user7")

    assertEquals("user7", account.uid)
    assertEquals("user7", account.ownerId)
    assertEquals("john_adams", account.username)
    assertEquals(user7.profilePicture, account.profilePicture)
    assertEquals(account.googleAccountEmail, testEmail)
    assertTrue(account.friendUids.isEmpty())
  }

  @Test
  fun createAccount_throwsExceptionForDuplicateUsername() {
    val user1 =
        User(
            uid = "newUser1",
            ownerId = "newUser1",
            username = "duplicate_user",
            profilePicture = "")
    val user2 =
        User(
            uid = "newUser2",
            ownerId = "newUser2",
            username = "duplicate_user",
            profilePicture = "")

    val exception =
        assertThrows(TakenUserException::class.java) {
          runTest {
            repository.createAccount(
                user1, testEmail, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
            repository.createAccount(
                user2,
                testEmail,
                dateOfBirth = testDateOfBirth,
                location = EPFL_LOCATION) // Should throw
          }
        }

    assertEquals("Username already in use", exception.message)
  }

  @Test
  fun createAccount_allowsBlankUsernamesForDifferentUsers() = runTest {
    val user1 = User(uid = "tempUser1", ownerId = "tempUser1", username = "", profilePicture = "")
    val user2 = User(uid = "tempUser2", ownerId = "tempUser2", username = "", profilePicture = "")

    repository.createAccount(
        user1, testEmail, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
    repository.createAccount(
        user2,
        "another@example.com",
        dateOfBirth = testDateOfBirth,
        location = EPFL_LOCATION) // Should not throw

    // Verify both blank username accounts were created
    assertEquals("", repository.getAccount("tempUser1").username)
    assertEquals("", repository.getAccount("tempUser2").username)

    // Test that duplicate non-blank usernames throw exception
    val u1 =
        User(
            uid = "newUser1",
            ownerId = "newUser1",
            username = "duplicate_user",
            profilePicture = "")
    val u2 =
        User(
            uid = "newUser2",
            ownerId = "newUser2",
            username = "duplicate_user",
            profilePicture = "")
    expectThrows<TakenUserException>("Username already in use") {
      repository.createAccount(u1, testEmail, dateOfBirth = testDateOfBirth, EPFL_LOCATION)
      repository.createAccount(u2, testEmail, dateOfBirth = testDateOfBirth, EPFL_LOCATION)
    }
  }

  @Test
  fun deleteAccount_variants() = runTest {
    repository.deleteAccount("user5")
    expectThrows<NoSuchElementException>("Account with ID user5 not found") {
      repository.getAccount("user5")
    }

    expectThrows<NoSuchElementException>("Account with ID nonexistent not found") {
      repository.deleteAccount("nonexistent")
    }
  }

  @Test
  fun editAccount_updatesAndKeepsOnBlank() = runTest {
    repository.getAccount("user1") // ensure account exists

    repository.editAccount(
        "user1",
        username = "new_alice",
        birthDay = "1995-05-15",
        picture = ":3",
        location = emptyLocation)
    val updated = repository.getAccount("user1")
    assertEquals("new_alice", updated.username)
    assertEquals("1995-05-15", updated.birthday)
    assertEquals(":3", updated.profilePicture)

    repository.editAccount(
        "user1", username = "", birthDay = "", picture = "", location = emptyLocation)
    val kept = repository.getAccount("user1")
    assertEquals(updated.username, kept.username)
    assertEquals(updated.birthday, kept.birthday)
    assertEquals(updated.profilePicture, kept.profilePicture)
  }

  @Test
  fun editAccount_throwsWhenNotFound() = runTest {
    expectThrows<NoSuchElementException>("Account with ID nonexistent not found") {
      repository.editAccount(
          "nonexistent", username = "new", birthDay = "", picture = "", location = emptyLocation)
    }
  }

  @Test
  fun deleteProfilePicture_successfullyDeletesProfilePicture() = runTest {
    // Create account with profile picture
    val user1 =
        User(
            uid = "user6",
            ownerId = "user6",
            username = "test_user",
            profilePicture = testProfilePicture)
    val user2 =
        User(uid = "user7", ownerId = "user7", username = "no_pic_user", profilePicture = "")
    repository.createAccount(
        user1, testEmail, dateOfBirth = testDateOfBirth, EPFL_LOCATION)
    repository.createAccount(user2, testEmail, dateOfBirth = testDateOfBirth, EPFL_LOCATION)

    // Verify profile picture exists
    val accountBefore = repository.getAccount("user6")
    assertEquals(testProfilePicture, accountBefore.profilePicture)
    assertTrue(accountBefore.profilePicture.isNotBlank())

    // Delete profile picture
    repository.deleteProfilePicture("user6")

    // Verify profile picture is deleted
    val accountAfter = repository.getAccount("user6")
    assertEquals("", accountAfter.profilePicture)

    val accountBefore2 = repository.getAccount("user7")
    assertEquals("", accountBefore2.profilePicture)

    // Delete profile picture (should do nothing but not fail)
    repository.deleteProfilePicture("user7")

    val accountAfter2 = repository.getAccount("user7")
    assertEquals("", accountAfter2.profilePicture)
  }

  @Test
  fun deleteProfilePicture_throwsWhenAccountNotFound() = runTest {
    expectThrows<NoSuchElementException>("Account with ID nonexistent not found") {
      repository.deleteProfilePicture("nonexistent")
    }
  }

  @Test
  fun togglePrivacy_variants() = runTest {
    val initial = repository.getAccount("user1").isPrivate
    assertEquals(!initial, repository.togglePrivacy("user1"))
    assertEquals(!initial, repository.getAccount("user1").isPrivate)

    repository.togglePrivacy("user1")
    assertEquals(initial, repository.getAccount("user1").isPrivate)

    val u2Initial = repository.getAccount("user2").isPrivate
    assertEquals(u2Initial, repository.getAccount("user2").isPrivate)
  }

  @Test
  fun createAccount_emailEdgeCases() = runTest {
    val user =
        User(uid = "user8", ownerId = "user8", username = "empty_email_user", profilePicture = "")
    val emptyEmail = ""
    repository.createAccount(user, emptyEmail, dateOfBirth = testDateOfBirth, EPFL_LOCATION)
    val account = repository.getAccount("user8")
    assertEquals(emptyEmail, account.googleAccountEmail)
    assertEquals("user8", account.uid)

    val a = User(uid = "user9", ownerId = "user9", username = "first_user", profilePicture = "")
    val b = User(uid = "user10", ownerId = "user10", username = "second_user", profilePicture = "")
    val email = "shared@example.com"
    repository.createAccount(a, email, dateOfBirth = testDateOfBirth, EPFL_LOCATION)
    repository.createAccount(b, email, dateOfBirth = testDateOfBirth, EPFL_LOCATION)
    assertEquals(email, repository.getAccount("user9").googleAccountEmail)
    assertEquals(email, repository.getAccount("user10").googleAccountEmail)
  }

  @Test
  fun createAccount_allowsSameEmailForDifferentUsers() = runTest {
    val user1 = User(uid = "user9", ownerId = "user9", username = "first_user", profilePicture = "")
    val user2 =
        User(uid = "user10", ownerId = "user10", username = "second_user", profilePicture = "")
    val sharedEmail = "shared@example.com"

    repository.createAccount(
        user1, sharedEmail, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
    repository.createAccount(
        user2,
        sharedEmail,
        dateOfBirth = testDateOfBirth,
        location = EPFL_LOCATION) // Should not throw

    val account1 = repository.getAccount("user9")
    val account2 = repository.getAccount("user10")

    assertEquals(sharedEmail, account1.googleAccountEmail)
    assertEquals(sharedEmail, account2.googleAccountEmail)
    assertEquals("first_user", account1.username)
    assertEquals("second_user", account2.username)
  }

  @Test
  fun getItemsList_returnsEmptyAndPopulated() = runTest {
    assertTrue(repository.getItemsList("user1").isEmpty())
    repository.addItem("item1")
    assertEquals(listOf("item1"), repository.getItemsList("user1"))
  }

  @Test
  fun addItem_variants() = runTest {
    assertTrue(repository.addItem("item1"))
    assertEquals(1, repository.getAccount("user1").itemsUids.size)
    assertTrue(repository.addItem("item1")) // duplicate
    assertEquals(1, repository.getAccount("user1").itemsUids.size)
    repository.addItem("item2")
    val items = repository.getAccount("user1").itemsUids
    assertEquals(2, items.size)
    assertTrue("item1" in items && "item2" in items)
  }

  @Test
  fun removeItem_variants() = runTest {
    repository.addItem("item1")
    repository.addItem("item2")
    assertTrue(repository.removeItem("item1"))
    assertTrue("item1" !in repository.getAccount("user1").itemsUids)
    assertTrue(repository.removeItem("item1")) // no-op
    assertEquals(1, repository.getAccount("user1").itemsUids.size)
  }

  @Test
  fun observeAccount_emitsInitialAccountImmediately() = runTest {
    val initialAccount = repository.observeAccount("user1").first()

    assertEquals("user1", initialAccount.uid)
    assertEquals("alice_wonder", initialAccount.username)
  }

  @Test
  fun observeAccount_throwsWhenAccountNotFound() = runTest {
    expectThrows<NoSuchElementException>("Account with ID nonexistent not found") {
      repository.observeAccount("nonexistent").first()
    }
  }

  @Test
  fun observeAccount_reflectsAccountUpdates() = runTest {
    // Get initial state
    val initial = repository.observeAccount("user1").first()
    assertEquals("alice_wonder", initial.username)

    // Update the account
    repository.editAccount(
        "user1",
        username = "updated_alice",
        birthDay = "2000-01-01",
        picture = "new.jpg",
        location = EPFL_LOCATION)

    // Verify the observation reflects the update
    val updated = repository.observeAccount("user1").first()
    assertEquals("updated_alice", updated.username)
  }

  @Test
  fun observeAccount_filtersToSpecificUser() = runTest {
    // Update a different account
    repository.editAccount(
        "user2",
        username = "updated_bob",
        birthDay = "1990-01-01",
        picture = "bob.jpg",
        location = EPFL_LOCATION)

    // Verify observing user1 still returns user1 data (not user2)
    val user1Account = repository.observeAccount("user1").first()
    assertEquals("user1", user1Account.uid)
    assertEquals("alice_wonder", user1Account.username)
  }
}
