package com.android.ootd.model.user

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegiserUserInMemory {

  private lateinit var repository: UserRepositoryInMemory
  private val testUser1 = User(uid = "user1", name = "testUser1", friendList = emptyList())
  private val testUser2 = User(uid = "user2", name = "testUser2", friendList = emptyList())

  @Before
  fun setUp() {
    repository = UserRepositoryInMemory()
  }

  @Test
  fun getNewUid_returnsUniqueIds() {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    assertNotNull(uid1)
    assertNotNull(uid2)
    assertTrue(uid1 != uid2)
  }

  @Test
  fun createUser_addsUserToRepository() = runTest {
    val username = "newUser"
    val uid = repository.getNewUid()

    repository.createUser(username, uid)

    val user = repository.getUser(uid)
    assertEquals(username, user.name)
    assertEquals(uid, user.uid)
    assertTrue(user.friendList.isEmpty())
  }

  @Test(expected = IllegalArgumentException::class)
  fun createUser_throwsExceptionForDuplicateUsername() = runTest {
    val username = "duplicateUser"
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    repository.createUser(username, uid1)
    repository.createUser(username, uid2) // Should throw
  }

  @Test
  fun getAllUsers_returnsEmptyListInitially() = runTest {
    val users = repository.getAllUsers()
    assertTrue(users.isEmpty())
  }

  @Test
  fun getAllUsers_returnsAllAddedUsers() = runTest {
    repository.addUser(testUser1)
    repository.addUser(testUser2)

    val users = repository.getAllUsers()
    assertEquals(2, users.size)
    assertTrue(users.contains(testUser1))
    assertTrue(users.contains(testUser2))
  }

  @Test
  fun addUser_successfullyAddsUser() = runTest {
    repository.addUser(testUser1)

    val users = repository.getAllUsers()
    assertEquals(1, users.size)
    assertEquals(testUser1, users[0])
  }

  @Test(expected = IllegalArgumentException::class)
  fun addUser_throwsExceptionForDuplicateUid() = runTest {
    repository.addUser(testUser1)
    repository.addUser(testUser1) // Should throw
  }

  @Test
  fun getUser_returnsCorrectUser() = runTest {
    repository.addUser(testUser1)
    repository.addUser(testUser2)

    val user = repository.getUser(testUser1.uid)
    assertEquals(testUser1, user)
  }

  @Test(expected = NoSuchElementException::class)
  fun getUser_throwsExceptionWhenUserNotFound() = runTest { repository.getUser("nonexistentUid") }

  @Test
  fun addFriend_successfullyAddsFriend() = runTest {
    repository.addUser(testUser1)
    repository.addUser(testUser2)

    repository.addFriend(testUser1.uid, testUser2.uid, testUser2.name)

    val updatedUser = repository.getUser(testUser1.uid)
    assertEquals(1, updatedUser.friendList.size)
    assertEquals(testUser2.uid, updatedUser.friendList[0].uid)
    assertEquals(testUser2.name, updatedUser.friendList[0].name)
  }

  @Test
  fun addFriend_preventsAddingSameFriendTwice() = runTest {
    repository.addUser(testUser1)
    repository.addUser(testUser2)

    repository.addFriend(testUser1.uid, testUser2.uid, testUser2.name)
    repository.addFriend(testUser1.uid, testUser2.uid, testUser2.name)

    val updatedUser = repository.getUser(testUser1.uid)
    assertEquals(1, updatedUser.friendList.size)
  }

  @Test
  fun addFriend_allowsMultipleFriends() = runTest {
    val testUser3 = User(uid = "user3", name = "testUser3", friendList = emptyList())

    repository.addUser(testUser1)
    repository.addUser(testUser2)
    repository.addUser(testUser3)

    repository.addFriend(testUser1.uid, testUser2.uid, testUser2.name)
    repository.addFriend(testUser1.uid, testUser3.uid, testUser3.name)

    val updatedUser = repository.getUser(testUser1.uid)
    assertEquals(2, updatedUser.friendList.size)
  }

  @Test(expected = NoSuchElementException::class)
  fun addFriend_throwsExceptionWhenUserNotFound() = runTest {
    repository.addUser(testUser2)
    repository.addFriend("nonexistentUid", testUser2.uid, testUser2.name)
  }

  @Test(expected = NoSuchElementException::class)
  fun addFriend_throwsExceptionWhenFriendNotFound() = runTest {
    repository.addUser(testUser1)
    repository.addFriend(testUser1.uid, "nonexistentFriendUid", "nonexistentName")
  }

  @Test
  fun createUser_andRetrieve_worksCorrectly() = runTest {
    val username = "createdUser"
    val uid = repository.getNewUid()

    repository.createUser(username, uid)

    val users = repository.getAllUsers()
    assertEquals(1, users.size)

    val user = repository.getUser(uid)
    assertEquals(username, user.name)
  }

  @Test
  fun repositoryIsolation_multipleInstances() = runTest {
    val repo1 = UserRepositoryInMemory()
    val repo2 = UserRepositoryInMemory()

    repo1.addUser(testUser1)

    assertEquals(1, repo1.getAllUsers().size)
    assertEquals(0, repo2.getAllUsers().size)
  }
}
