package com.android.ootd.model.user

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
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

    assertEquals(5, users.size)
    assertTrue(users.any { it.uid == "user1" })
    assertTrue(users.any { it.uid == "user2" })
    assertTrue(users.any { it.uid == "user3" })
    assertTrue(users.any { it.uid == "user4" })
    assertTrue(users.any { it.uid == "user5" })
  }

  @Test
  fun getAllUsers_returnsUsersWithCorrectNames() = runTest {
    val users = repository.getAllUsers()

    val user1 = users.find { it.uid == "user1" }
    assertEquals("alice_wonder", user1?.name)

    val user2 = users.find { it.uid == "user2" }
    assertEquals("bob_builder", user2?.name)
  }

  @Test
  fun getUser_returnsCorrectUser() = runTest {
    val user = repository.getUser("user1")

    assertEquals("user1", user.uid)
    assertEquals("alice_wonder", user.name)
    assertEquals(2, user.friendList.size)
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
  fun addUser_successfullyAddsNewUser() = runTest {
    val newUser = User(uid = "user6", name = "frank_sinatra", friendList = emptyList())

    repository.addUser(newUser)
    val retrievedUser = repository.getUser("user6")

    assertEquals("user6", retrievedUser.uid)
    assertEquals("frank_sinatra", retrievedUser.name)
    assertTrue(retrievedUser.friendList.isEmpty())
  }

  @Test
  fun addUser_throwsExceptionWhenUserAlreadyExists() {
    val duplicateUser = User(uid = "user1", name = "duplicate_user", friendList = emptyList())

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runTest { repository.addUser(duplicateUser) }
        }

    assertEquals("User with UID user1 already exists", exception.message)
  }

  @Test
  fun addUser_increasesUserCount() = runTest {
    val initialCount = repository.getAllUsers().size

    val newUser = User(uid = "user6", name = "new_user", friendList = emptyList())
    repository.addUser(newUser)

    assertEquals(initialCount + 1, repository.getAllUsers().size)
  }

  @Test
  fun addFriend_successfullyAddsFriend() = runTest {
    repository.addFriend("user3", "user1", "alice_wonder")

    val user = repository.getUser("user3")
    assertEquals(1, user.friendList.size)
    assertEquals("user1", user.friendList[0].uid)
    assertEquals("alice_wonder", user.friendList[0].name)
  }

  @Test
  fun addFriend_throwsExceptionWhenUserNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.addFriend("nonexistent", "user1", "alice_wonder") }
        }

    assertEquals("User with ID nonexistent not found", exception.message)
  }

  @Test
  fun addFriend_throwsExceptionWhenFriendNotFound() {
    val exception =
        assertThrows(NoSuchElementException::class.java) {
          runTest { repository.addFriend("user3", "nonexistent", "fake_user") }
        }

    assertEquals("Friend with ID nonexistent not found", exception.message)
  }

  @Test
  fun addFriend_doesNotAddDuplicateFriend() = runTest {
    val initialFriendCount = repository.getUser("user1").friendList.size

    // Try to add user2 who is already a friend of user1
    repository.addFriend("user1", "user2", "bob_builder")

    val user = repository.getUser("user1")
    assertEquals(initialFriendCount, user.friendList.size)
  }

  @Test
  fun addFriend_preservesExistingFriends() = runTest {
    repository.addFriend("user3", "user1", "alice_wonder")
    repository.addFriend("user3", "user2", "bob_builder")

    val user = repository.getUser("user3")
    assertEquals(2, user.friendList.size)
    assertTrue(user.friendList.any { it.uid == "user1" })
    assertTrue(user.friendList.any { it.uid == "user2" })
  }

  @Test
  fun addFriend_doesNotAffectOtherUsers() = runTest {
    val user2BeforeAdd = repository.getUser("user2")

    repository.addFriend("user3", "user1", "alice_wonder")

    val user2AfterAdd = repository.getUser("user2")
    assertEquals(user2BeforeAdd.friendList.size, user2AfterAdd.friendList.size)
  }

  @Test
  fun getAllUsers_returnsCorrectFriendListsForPreloadedUsers() = runTest {
    val user1 = repository.getUser("user1")
    val user4 = repository.getUser("user4")
    val user5 = repository.getUser("user5")

    assertEquals(2, user1.friendList.size)
    assertEquals(2, user4.friendList.size)
    assertTrue(user5.friendList.isEmpty())
  }

  @Test
  fun addFriend_canAddMultipleFriendsSequentially() = runTest {
    repository.addFriend("user5", "user1", "alice_wonder")
    repository.addFriend("user5", "user2", "bob_builder")
    repository.addFriend("user5", "user3", "charlie_brown")

    val user = repository.getUser("user5")
    assertEquals(3, user.friendList.size)
  }

  @Test(expected = IllegalArgumentException::class)
  fun createUser_throwsExceptionForDuplicateUsername() = runTest {
    val username = "duplicateUser"
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    repository.createUser(username, uid1)
    repository.createUser(username, uid2) // Should throw
  }
}
