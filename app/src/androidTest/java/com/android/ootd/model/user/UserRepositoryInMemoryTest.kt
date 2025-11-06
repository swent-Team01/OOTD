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
  fun getAllUsers_returnsUsersWithCorrectNamesAndProfilePic() = runTest {
    val users = repository.getAllUsers()

    val user1 = users.find { it.uid == "user1" }
    assertEquals("alice_wonder", user1?.username)
    assertEquals("1", user1?.profilePicture)

    val user2 = users.find { it.uid == "user2" }
    assertEquals("bob_builder", user2?.username)
    assertEquals("2", user2?.profilePicture)
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
    val newUser = User(uid = "user6", username = "frank_sinatra", profilePicture = "Hello.jpg")

    repository.addUser(newUser)
    val retrievedUser = repository.getUser("user6")

    assertEquals("user6", retrievedUser.uid)
    assertEquals("frank_sinatra", retrievedUser.username)
    assertEquals("Hello.jpg", retrievedUser.profilePicture)
  }

  @Test
  fun addUser_throwsExceptionWhenUserAlreadyExists() {
    val duplicateUser = User(uid = "user1", username = "duplicate_user", profilePicture = "")

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runTest { repository.addUser(duplicateUser) }
        }

    assertEquals("User with UID user1 already exists", exception.message)
  }

  @Test
  fun addUser_increasesUserCount() = runTest {
    val initialCount = repository.getAllUsers().size

    val newUser = User(uid = "user6", username = "new_user", profilePicture = "")
    repository.addUser(newUser)

    assertEquals(initialCount + 1, repository.getAllUsers().size)
  }

  @Test(expected = TakenUsernameException::class)
  fun createUser_throwsExceptionForDuplicateUsername() = runTest {
    val username = "duplicateUser"
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    repository.createUser(username, uid1, profilePicture = "")
    repository.createUser(username, uid2, profilePicture = "") // Should throw
  }

  @Test
  fun createUser_handlesEmptyProfilePictureCorrectly() = runTest {
    val username = "user_no_pic"
    val uid = repository.getNewUid()

    repository.createUser(username, uid, profilePicture = "")
    val user = repository.getUser(uid)

    assertEquals(username, user.username)
    assertEquals(uid, user.uid)
    assertEquals("", user.profilePicture)
  }

  @Test
  fun editUserSuccessfullyUpdatesUser() = runTest {
    val newUsername = "new_alice_wonder"
    val picture = "image.img"

    repository.editUser("user1", newUsername, picture)

    val updatedUser = repository.getUser("user1")
    assertEquals(newUsername, updatedUser.username)
    assertEquals("user1", updatedUser.uid)
    assertEquals(picture, updatedUser.profilePicture)
  }

  @Test
  fun editUserThrowsExceptionWhenUserIdBlankOrUsernameTaken() = runTest {
    val exception1 = runCatching { repository.editUser("", "newUsername") }.exceptionOrNull()
    assertTrue(exception1 is NoSuchElementException)
    assertTrue(exception1?.message?.contains("not found") == true)

    val exception2 = runCatching { repository.editUser("user1", "bob_builder") }.exceptionOrNull()
    assertTrue(exception2 is TakenUsernameException)
    assertTrue(exception2?.message?.contains("already in use") == true)
  }

  @Test
  fun editUserDoesNothingIfFieldsAreBlank() = runTest {
    val user = repository.getUser("user1")
    repository.editUser(user.uid, "", "")
    val unchangedUser = repository.getUser(user.uid)
    assertEquals(user.uid, unchangedUser.uid)
    assertEquals(user.username, unchangedUser.username)
    assertEquals(user.profilePicture, unchangedUser.profilePicture)
  }

  @Test
  fun editUserAllowsUpdatingToSameUsernameAndProfilePicture() = runTest {
    repository.editUser("user1", "updated_alice", "Picture1.jpg")
    repository.editUser("user2", "updated_bob", "Picture2.jpg")

    val user1 = repository.getUser("user1")
    val user2 = repository.getUser("user2")

    assertEquals("updated_alice", user1.username)
    assertEquals("updated_bob", user2.username)
    assertEquals("Picture1.jpg", user1.profilePicture)
    assertEquals("Picture2.jpg", user2.profilePicture)
  }

  @Test
  fun deleteUserSuccessfullyRemovesUser() = runTest {
    val initialCount = repository.getAllUsers().size

    repository.deleteUser("user1")

    assertEquals(initialCount - 1, repository.getAllUsers().size)

    val exception = runCatching { repository.getUser("user1") }.exceptionOrNull()
    assertTrue(exception is NoSuchElementException)
  }

  @Test
  fun deleteUserThrowsExceptionWhenUserIdIsBlank() = runTest {
    val exception = runCatching { repository.deleteUser("") }.exceptionOrNull()
    assertTrue(exception is IllegalArgumentException)
  }

  @Test
  fun deleteUserDoesNotThrowWhenUserNotFound() = runTest {
    val initialCount = repository.getAllUsers().size

    // In-memory implementation doesn't throw for non-existent users
    repository.deleteUser("nonExistentUser")

    assertEquals(initialCount, repository.getAllUsers().size)
  }

  @Test
  fun deleteUserDoesNotAffectOtherUsers() = runTest {
    repository.deleteUser("user1")

    val user2 = repository.getUser("user2")
    assertEquals("bob_builder", user2.username)

    val user3 = repository.getUser("user3")
    assertEquals("charlie_brown", user3.username)
  }

  @Test
  fun deleteMultipleUsersSuccessfully() = runTest {
    val initialCount = repository.getAllUsers().size

    repository.deleteUser("user1")
    repository.deleteUser("user2")
    repository.deleteUser("user3")

    assertEquals(initialCount - 3, repository.getAllUsers().size)

    // Verify users are deleted
    val exception1 = runCatching { repository.getUser("user1") }.exceptionOrNull()
    assertTrue(exception1 is NoSuchElementException)

    val exception2 = runCatching { repository.getUser("user2") }.exceptionOrNull()
    assertTrue(exception2 is NoSuchElementException)

    val exception3 = runCatching { repository.getUser("user3") }.exceptionOrNull()
    assertTrue(exception3 is NoSuchElementException)
  }
}
