package com.android.ootd.model.user

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryInMemoryTest {
  // Tests generated with AI, verified by a human
  private lateinit var repository: UserRepositoryInMemory

  @Before
  fun setUp() {
    repository = UserRepositoryInMemory()
  }

  @Test
  fun getNewUid_returnsUniqueUid() {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    assertNotNull(uid1)
    assertNotNull(uid2)
    assertNotEquals(uid1, uid2)
  }

  @Test
  fun getNewUid_returnsValidUuidFormat() {
    val uid = repository.getNewUid()

    // UUID format: 8-4-4-4-12 characters
    val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    assertTrue(uuidRegex.matches(uid))
  }

  @Test
  fun getAllUsers_returnsAllPreloadedUsers() = runTest {
    val users = repository.getAllUsers()

    assertEquals(6, users.size)
    assertTrue(users.any { it.uid == "user1" })
    assertTrue(users.any { it.uid == "user2" })
    assertTrue(users.any { it.uid == "user3" })
    assertTrue(users.any { it.uid == "user4" })
    assertTrue(users.any { it.uid == "user5" })
    assert(users.any { it.uid == "nonRegisterUser" })
  }

  @Test
  fun getAllUsers_returnsUsersWithCorrectNames() = runTest {
    val users = repository.getAllUsers()

    val user1 = users.find { it.uid == "user1" }
    assertEquals("alice_wonder", user1?.username)

    val user2 = users.find { it.uid == "user2" }
    assertEquals("bob_builder", user2?.username)
  }

  @Test
  fun getUser_returnsCorrectUser() = runTest {
    val user = repository.getUser("user1")

    Assert.assertEquals("user1", user.uid)
    Assert.assertEquals("alice_wonder", user.username)
    Assert.assertEquals(2, user.friendUids.size)
  }

  @Test
  fun getUser_throwsExceptionWhenUserNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.getUser("nonexistent") }
        }

    assertEquals("User with ID nonexistent not found", exception.message)
  }

  @Test
  fun userExists_worksCorrectlyForUserWithUsername() = runTest {
    val user = repository.userExists("user1")
    assertTrue(user)
  }

  @Test
  fun userExists_throwsIfUidDoesNotExist() {
    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.userExists("I do not exist") }
    }
  }

  @Test
  fun userExists_worksWithUnexistantUseranme() = runTest {
    val user = repository.userExists("nonRegisterUser")
    assertTrue(!user)
  }

  @Test
  fun addUser_successfullyAddsNewUser() = runTest {
    val newUser = User(uid = "user6", username = "frank_sinatra", friendUids = emptyList())

    repository.addUser(newUser)
    val retrievedUser = repository.getUser("user6")

    Assert.assertEquals("user6", retrievedUser.uid)
    Assert.assertEquals("frank_sinatra", retrievedUser.username)
    Assert.assertTrue(retrievedUser.friendUids.isEmpty())
  }

  @Test
  fun addUser_throwsExceptionWhenUserAlreadyExists() {
    val duplicateUser = User(uid = "user1", username = "duplicate_user", friendUids = emptyList())

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runTest { repository.addUser(duplicateUser) }
        }

    assertEquals("User with UID user1 already exists", exception.message)
  }

  @Test
  fun addUser_increasesUserCount() = runTest {
    val initialCount = repository.getAllUsers().size

    val newUser = User(uid = "user6", username = "new_user", friendUids = emptyList())
    repository.addUser(newUser)

    assertEquals(initialCount + 1, repository.getAllUsers().size)
  }

  @Test
  fun addFriend_successfullyAddsFriend() = runTest {
    repository.addFriend("user3", "user1")

    val user = repository.getUser("user3")
    Assert.assertEquals(1, user.friendUids.size)
    Assert.assertEquals("user1", user.friendUids[0])
  }

  @Test
  fun addFriend_throwsExceptionWhenUserNotFound() {
    val exception =
        Assert.assertThrows(NoSuchElementException::class.java) {
          runTest { repository.addFriend("nonexistent", "user1") }
        }

    assertEquals("User with ID nonexistent not found", exception.message)
  }

  @Test
  fun addFriend_throwsExceptionWhenFriendNotFound() {
    val exception =
        Assert.assertThrows(NoSuchElementException::class.java) {
          runTest { repository.addFriend("user3", "nonexistent") }
        }

    assertEquals("Friend with ID nonexistent not found", exception.message)
  }

  @Test
  fun addFriend_doesNotAddDuplicateFriend() = runTest {
    val initialFriendCount = repository.getUser("user1").friendUids.size

    // Try to add user2 who is already a friend of user1
    repository.addFriend("user1", "user2")

    val user = repository.getUser("user1")
    Assert.assertEquals(initialFriendCount, user.friendUids.size)
  }

  @Test
  fun addFriend_preservesExistingFriends() = runTest {
    repository.addFriend("user3", "user1")
    repository.addFriend("user3", "user2")

    val user = repository.getUser("user3")
    Assert.assertEquals(2, user.friendUids.size)
    Assert.assertTrue(user.friendUids.any { it == "user1" })
    Assert.assertTrue(user.friendUids.any { it == "user2" })
  }

  @Test
  fun addFriend_doesNotAffectOtherUsers() = runTest {
    val user2BeforeAdd = repository.getUser("user2")

    repository.addFriend("user3", "user1")

    val user2AfterAdd = repository.getUser("user2")
    Assert.assertEquals(user2BeforeAdd.friendUids.size, user2AfterAdd.friendUids.size)
  }

  @Test
  fun getAllUsers_returnsCorrectFriendListsForPreloadedUsers() = runTest {
    val user1 = repository.getUser("user1")
    val user4 = repository.getUser("user4")
    val user5 = repository.getUser("user5")

    Assert.assertEquals(2, user1.friendUids.size)
    Assert.assertEquals(2, user4.friendUids.size)
    Assert.assertTrue(user5.friendUids.isEmpty())
  }

  @Test
  fun addFriend_canAddMultipleFriendsSequentially() = runTest {
    repository.addFriend("user5", "user1")
    repository.addFriend("user5", "user2")
    repository.addFriend("user5", "user3")

    val user = repository.getUser("user5")
    Assert.assertEquals(3, user.friendUids.size)
  }

  @Test(expected = TakenUsernameException::class)
  fun createUser_throwsExceptionForDuplicateUsername() = runTest {
    val username = "duplicateUser"
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    repository.createUser(username, uid1)
    repository.createUser(username, uid2) // Should throw
  }

  @Test
  fun removeFriend_successfullyRemovesFriend() = runTest {
    repository.removeFriend("user1", "user2")

    val user = repository.getUser("user1")
    Assert.assertEquals(1, user.friendUids.size)
    Assert.assertFalse(user.friendUids.any { it == "user2" })
  }

  @Test
  fun removeFriend_throwsExceptionWhenUserNotFound() {
    val exception =
        Assert.assertThrows(NoSuchElementException::class.java) {
          runTest { repository.removeFriend("nonexistent", "user1") }
        }

    Assert.assertEquals("User with ID nonexistent not found", exception.message)
  }

  @Test
  fun removeFriend_throwsExceptionWhenFriendNotFound() {
    val exception =
        Assert.assertThrows(NoSuchElementException::class.java) {
          runTest { repository.removeFriend("user1", "nonexistent") }
        }

    Assert.assertEquals("Friend with ID nonexistent not found", exception.message)
  }

  @Test
  fun removeFriend_doesNothingWhenFriendNotInList() = runTest {
    val initialFriendCount = repository.getUser("user3").friendUids.size

    repository.removeFriend("user3", "user1")

    val user = repository.getUser("user3")
    Assert.assertEquals(initialFriendCount, user.friendUids.size)
  }

  @Test
  fun removeFriend_preservesOtherFriends() = runTest {
    repository.removeFriend("user1", "user2")

    val user = repository.getUser("user1")
    Assert.assertEquals(1, user.friendUids.size)
    Assert.assertTrue(user.friendUids.any { it == "user3" })
  }

  @Test
  fun removeFriend_doesNotAffectOtherUsers() = runTest {
    val user2BeforeRemove = repository.getUser("user2")

    repository.removeFriend("user1", "user2")

    val user2AfterRemove = repository.getUser("user2")
    Assert.assertEquals(user2BeforeRemove.friendUids.size, user2AfterRemove.friendUids.size)
  }

  @Test
  fun removeFriend_canRemoveAllFriends() = runTest {
    repository.removeFriend("user1", "user2")
    repository.removeFriend("user1", "user3")

    val user = repository.getUser("user1")
    Assert.assertTrue(user.friendUids.isEmpty())
  }
}
