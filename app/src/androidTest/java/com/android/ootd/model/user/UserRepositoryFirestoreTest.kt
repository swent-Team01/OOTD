package com.android.ootd.model.user

import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
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

  @Test
  fun getUserThrowsExceptionWhenUserNotFound() = runTest {
    val nonExistentUserId = "nonExistentUser123"

    val exception = runCatching { repository.getUser(nonExistentUserId) }.exceptionOrNull()

    assert(exception is NoSuchElementException)
    assert(exception?.message?.contains(nonExistentUserId) == true)
  }

  @Test
  fun getAllUsersReturnsEmptyListWhenNoUsers() = runTest {
    val users = repository.getAllUsers()
    assertEquals(0, users.size)
  }

  @Test
  fun canAddFriendToUser() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    repository.addFriend(user1.uid, user2.uid, user2.name)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(1, updatedUser.friendList.size)
    assertEquals(user2.uid, updatedUser.friendList.first().uid)
    assertEquals(user2.name, updatedUser.friendList.first().name)
  }

  @Test
  fun canAddMultipleFriendsToUser() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    val user3 = user1.copy(uid = "user3", name = "user3name")
    repository.addUser(user3)

    repository.addFriend(user1.uid, user2.uid, user2.name)
    repository.addFriend(user1.uid, user3.uid, user3.name)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(2, updatedUser.friendList.size)
  }

  @Test
  fun addingDuplicateFriendDoesNotCreateDuplicates() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    repository.addFriend(user1.uid, user2.uid, user2.name)
    repository.addFriend(user1.uid, user2.uid, user2.name)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(1, updatedUser.friendList.size)
  }

  @Test
  fun addFriendThrowsExceptionWhenUserNotFound() = runTest {
    val nonExistentUserId = "nonExistentUser123"

    val exception =
        runCatching { repository.addFriend(nonExistentUserId, user1.uid, user1.name) }
            .exceptionOrNull()

    assert(exception != null)
  }

  @Test
  fun addUserThrowsExceptionWhenUidAlreadyExists() = runTest {
    repository.addUser(user1)

    val exception = runCatching { repository.addUser(user1) }.exceptionOrNull()

    assert(exception is IllegalArgumentException)
    assertEquals(1, getUserCount())
  }

  @Test
  fun getUserReturnsCorrectUserFromMultipleUsers() = runTest {
    val user3 = user1.copy(uid = "user3", name = "user3name")
    val user4 = user1.copy(uid = "user4", name = "user4name")

    repository.addUser(user1)
    repository.addUser(user2)
    repository.addUser(user3)
    repository.addUser(user4)

    val retrievedUser = repository.getUser(user3.uid)
    assertEquals(user3, retrievedUser)
    assertEquals("user3name", retrievedUser.name)
  }

  @Test
  fun getUserHandlesCorruptedDocumentGracefully() = runTest {
    // Add a user first
    repository.addUser(user1)

    // Manually corrupt the document in Firestore by adding invalid data
    FirebaseEmulator.firestore
        .collection(USER_COLLECTION_PATH)
        .document(user1.uid)
        .update(mapOf("uid" to 12345)) // Wrong type - should be String
        .await()

    val exception = runCatching { repository.getUser(user1.uid) }.exceptionOrNull()

    assert(exception != null)
  }

  @Test
  fun getAllUsersSkipsCorruptedDocuments() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    // Corrupt one document
    FirebaseEmulator.firestore
        .collection(USER_COLLECTION_PATH)
        .document(user1.uid)
        .set(mapOf("invalidField" to "invalidData"))
        .await()

    val users = repository.getAllUsers()

    // Should return only the valid user (user2)
    assertEquals(1, users.size)
    assertEquals(user2.uid, users.first().uid)
  }

  @Test
  fun getUserHandlesNullDeserializationGracefully() = runTest {
    // Create a document with incomplete/invalid data
    val invalidUserId = "invalidUser"
    FirebaseEmulator.firestore
        .collection(USER_COLLECTION_PATH)
        .document(invalidUserId)
        .set(mapOf("uid" to "invalidUser"))
        .await()

    val exception = runCatching { repository.getUser(invalidUserId) }.exceptionOrNull()

    assert(exception is IllegalStateException)
  }

  @Test
  fun getAllUsersReturnsOnlyValidUsers() = runTest {
    repository.addUser(user1)

    // Add an invalid document
    FirebaseEmulator.firestore
        .collection(USER_COLLECTION_PATH)
        .document("invalidDoc")
        .set(mapOf("random" to "data"))
        .await()

    repository.addUser(user2)

    val users = repository.getAllUsers()

    // Should return only valid users
    assertEquals(2, users.size)
    assert(users.any { it.uid == user1.uid })
    assert(users.any { it.uid == user2.uid })
  }
}
