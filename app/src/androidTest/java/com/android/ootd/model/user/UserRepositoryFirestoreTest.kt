package com.android.ootd.model.user

import com.android.ootd.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UserRepositoryFirestoreTest : FirestoreTest() {

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun canAddUsersToRepository() = runTest {
    repository.addUser(user1)
    assertEquals(1, getUserCount())
    val users = repository.getAllUsers()

    assertEquals(1, users.size)
    // Discard uid and ownerId for comparison since they don't matter in this test
    val expectedUser = user1.copy(uid = "None")
    val storedUser = users.first().copy(uid = expectedUser.uid)

    assertEquals(expectedUser, storedUser)
  }

  @Test
  fun addUserWithTheCorrectID() = runTest {
    repository.addUser(user1)
    assertEquals(1, getUserCount())
    val storedUser = repository.getUser(user1.uid)
    assertEquals(storedUser, user1)
  }

  @Test
  fun canAddMultipleUsersToRepository() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    assertEquals(2, getUserCount())
    val users = repository.getAllUsers()

    assertEquals(users.size, 2)
    // Discard the ordering of the users
    val expectedUsers = setOf(user1, user2)
    val storedUsers = users.toSet()

    assertEquals(expectedUsers, storedUsers)
  }

  @Test
  fun uidAreUniqueInTheCollection() = runTest {
    val uid = "duplicate"
    val user1Modified = user1.copy(uid = uid)
    val userDuplicatedUID = user2.copy(uid = uid)

    // Depending on your implementation, adding a User with an existing UID
    // might not be permitted
    runCatching {
      repository.addUser(user1Modified)
      repository.addUser(userDuplicatedUID)
    }

    assertEquals(1, getUserCount())

    val users = repository.getAllUsers()
    assertEquals(users.size, 1)
    val storedUser = users.first()
    assertEquals(storedUser.uid, uid)
  }

  @Test
  fun getNewUidReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val uids = (0 until 100).toSet<Int>().map { repository.getNewUid() }.toSet()
    assertEquals(uids.size, numberIDs)
  }

  @Test
  fun canRetrieveAUserByID() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    assertEquals(2, getUserCount())
    val storedUser = repository.getUser(user2.uid)
    assertEquals(storedUser, user2)
  }
}
