package com.android.ootd.model.user

import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import kotlin.text.set
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

    repository.addFriend(user1.uid, user2.uid)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(1, updatedUser.friendUids.size)
    assertEquals(user2.uid, updatedUser.friendUids.first())
  }

  @Test
  fun canAddMultipleFriendsToUser() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    val user3 = user1.copy(uid = "user3", username = "user3name")
    repository.addUser(user3)

    repository.addFriend(user1.uid, user2.uid)
    repository.addFriend(user1.uid, user3.uid)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(2, updatedUser.friendUids.size)
  }

  @Test
  fun addingDuplicateFriendDoesNotCreateDuplicates() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    repository.addFriend(user1.uid, user2.uid)
    repository.addFriend(user1.uid, user2.uid)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(1, updatedUser.friendUids.size)
  }

  @Test
  fun addFriendThrowsExceptionWhenUserNotFound() = runTest {
    val nonExistentUserId = "nonExistentUser123"

    val exception =
        runCatching { repository.addFriend(nonExistentUserId, user1.uid) }.exceptionOrNull()

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
  fun userExistsReturnsTrueWhenUserHasName() = runTest {
    repository.addUser(user1)

    val exists = repository.userExists(user1.uid)

    assert(exists)
  }

  @Test
  fun userExistsReturnsFalseWhenUserNotFound() = runTest {
    val nonExistentUserId = "nonExistentUser123"

    val exists = repository.userExists(nonExistentUserId)

    assert(!exists)
  }

  @Test
  fun userExistsReturnsFalseWhenUserHasBlankName() = runTest {
    // Add user document with blank username field
    FirebaseEmulator.firestore
        .collection(USER_COLLECTION_PATH)
        .document("userWithBlankName")
        .set(mapOf("uid" to "userWithBlankName", "username" to ""))
        .await()

    val exists = repository.userExists("userWithBlankName")

    assert(!exists)
  }

  @Test
  fun userExistsReturnsFalseWhenUserHasNullName() = runTest {
    // Add user document with null name field
    FirebaseEmulator.firestore
        .collection(USER_COLLECTION_PATH)
        .document("userWithNullName")
        .set(mapOf("uid" to "userWithNullName"))
        .await()

    val exists = repository.userExists("userWithNullName")

    assert(!exists)
  }

  @Test
  fun userExistsHandlesMultipleUsersCorrectly() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    assert(repository.userExists(user1.uid))
    assert(repository.userExists(user2.uid))
    assert(!repository.userExists("nonExistentUser"))
  }

  @Test
  fun getUserReturnsCorrectUserFromMultipleUsers() = runTest {
    val user3 = user1.copy(uid = "user3", username = "user3name")
    val user4 = user1.copy(uid = "user4", username = "user4name")

    repository.addUser(user1)
    repository.addUser(user2)
    repository.addUser(user3)
    repository.addUser(user4)

    val retrievedUser = repository.getUser(user3.uid)
    assertEquals(user3, retrievedUser)
    assertEquals("user3name", retrievedUser.username)
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

  @Test
  fun creatingNewUserWorks() = runTest {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    repository.createUser(user1.username, uid1)
    repository.createUser(user2.username, uid2)

    val users = repository.getAllUsers()

    assertEquals(2, users.size)
    assert(users.any { user -> user.username == user1.username })
    assert(users.any { user -> user.username == user2.username })
    assert(users.none { user -> user.uid == user1.uid || user.uid == user2.uid })
  }

  @Test
  fun cannotHaveSameUsername() = runTest {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    repository.createUser(user1.username, uid1)
    val exception = runCatching { repository.createUser(user1.username, uid2) }.exceptionOrNull()

    assert(exception != null)
  }

  @Test
  fun canRemoveFriendFromUser() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    repository.addFriend(user1.uid, user2.uid)

    repository.removeFriend(user1.uid, user2.uid)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(0, updatedUser.friendUids.size)
  }

  @Test
  fun removeFriendThrowsExceptionWhenUserNotFound() = runTest {
    val nonExistentUserId = "nonExistentUser123"

    val exception =
        runCatching { repository.removeFriend(nonExistentUserId, user1.uid) }.exceptionOrNull()

    assert(exception is NoSuchElementException)
  }

  @Test
  fun removeFriendThrowsExceptionWhenFriendNotFound() = runTest {
    repository.addUser(user1)
    val nonExistentFriendId = "nonExistentFriend123"

    val exception =
        runCatching { repository.removeFriend(user1.uid, nonExistentFriendId) }.exceptionOrNull()

    assert(exception is NoSuchElementException)
    assert(exception?.message?.contains(nonExistentFriendId) == true)
  }

  @Test
  fun removeFriendDoesNothingWhenFriendNotInList() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    val user3 = user1.copy(uid = "user3", username = "user3name")
    repository.addUser(user3)

    repository.addFriend(user1.uid, user2.uid)

    repository.removeFriend(user1.uid, user3.uid)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(1, updatedUser.friendUids.size)
    assertEquals(user2.uid, updatedUser.friendUids.first())
  }

  @Test
  fun removeFriendPreservesOtherFriends() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    val user3 = user1.copy(uid = "user3", username = "user3name")
    repository.addUser(user3)

    repository.addFriend(user1.uid, user2.uid)
    repository.addFriend(user1.uid, user3.uid)

    repository.removeFriend(user1.uid, user2.uid)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(1, updatedUser.friendUids.size)
    assertEquals(user3.uid, updatedUser.friendUids.first())
  }

  @Test
  fun canRemoveMultipleFriendsFromUser() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    val user3 = user1.copy(uid = "user3", username = "user3name")
    repository.addUser(user3)

    repository.addFriend(user1.uid, user2.uid)
    repository.addFriend(user1.uid, user3.uid)

    repository.removeFriend(user1.uid, user2.uid)
    repository.removeFriend(user1.uid, user3.uid)

    val updatedUser = repository.getUser(user1.uid)
    assertEquals(0, updatedUser.friendUids.size)
  }

  @Test
  fun isMyFriendReturnsTrueForExistingFriend() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    repository.addFriend(user1.uid, user2.uid)

    val isFriend = repository.isMyFriend(user1.uid, user2.uid)

    assert(isFriend)
  }

  @Test
  fun isMyFriendReturnsFalseForNonFriend() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    val user3 = user1.copy(uid = "user3", username = "user3name")
    repository.addUser(user3)

    repository.addFriend(user1.uid, user2.uid)

    val isFriend = repository.isMyFriend(user1.uid, user3.uid)

    assert(!isFriend)
  }

  @Test
  fun isMyFriendReturnsFalseAfterRemovingFriend() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    repository.addFriend(user1.uid, user2.uid)

    repository.removeFriend(user1.uid, user2.uid)

    val isFriend = repository.isMyFriend(user1.uid, user2.uid)

    assert(!isFriend)
  }

  @Test
  fun isMyFriendReturnsTrueAfterAddingFriend() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    val wasNotFriend = repository.isMyFriend(user1.uid, user2.uid)
    repository.addFriend(user1.uid, user2.uid)
    val isFriend = repository.isMyFriend(user1.uid, user2.uid)

    assert(!wasNotFriend)
    assert(isFriend)
  }

  @Test
  fun isMyFriendThrowsExceptionWhenUserNotFound() = runTest {
    val nonExistentUserId = "nonExistentUser123"

    val exception =
        runCatching { repository.isMyFriend(nonExistentUserId, user1.uid) }.exceptionOrNull()

    assert(exception is NoSuchElementException)
    assert(exception?.message?.contains("authenticated user has not been added") == true)
  }

  @Test
  fun isMyFriendReturnsFalseWhenFriendUserDoesNotExist() = runTest {
    repository.addUser(user1)
    val nonExistentFriendId = "nonExistentFriend123"

    val isFriend = repository.isMyFriend(user1.uid, nonExistentFriendId)

    assert(!isFriend)
  }

  @Test
  fun isMyFriendWorksWithEmptyFriendList() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    val isFriend = repository.isMyFriend(user1.uid, user2.uid)

    assert(!isFriend)
  }
}
