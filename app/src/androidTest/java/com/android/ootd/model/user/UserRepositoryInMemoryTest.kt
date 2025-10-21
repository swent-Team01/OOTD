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

    assertEquals("user1", user.uid)
    assertEquals("alice_wonder", user.username)
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
    val newUser = User(uid = "user6", username = "frank_sinatra")

    repository.addUser(newUser)
    val retrievedUser = repository.getUser("user6")

    assertEquals("user6", retrievedUser.uid)
    assertEquals("frank_sinatra", retrievedUser.username)
  }

  @Test
  fun addUser_throwsExceptionWhenUserAlreadyExists() {
    val duplicateUser = User(uid = "user1", username = "duplicate_user")

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runTest { repository.addUser(duplicateUser) }
        }

    assertEquals("User with UID user1 already exists", exception.message)
  }

  @Test
  fun addUser_increasesUserCount() = runTest {
    val initialCount = repository.getAllUsers().size

    val newUser = User(uid = "user6", username = "new_user")
    repository.addUser(newUser)

    assertEquals(initialCount + 1, repository.getAllUsers().size)
  }

  @Test(expected = TakenUsernameException::class)
  fun createUser_throwsExceptionForDuplicateUsername() = runTest {
    val username = "duplicateUser"
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    repository.createUser(username, uid1)
    repository.createUser(username, uid2) // Should throw
  }
}
