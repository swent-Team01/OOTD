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

  // --- tiny helpers ---
  private suspend fun expectCount(n: Int) = assertEquals(n, getUserCount())

  private suspend fun put(uid: String, data: Map<String, Any?>) {
    FirebaseEmulator.firestore.collection(USER_COLLECTION_PATH).document(uid).set(data).await()
  }

  private suspend fun patch(uid: String, data: Map<String, Any?>) {
    FirebaseEmulator.firestore.collection(USER_COLLECTION_PATH).document(uid).update(data).await()
  }

  @Test
  fun endToEnd_create_list_edit_add_delete() = runTest {
    // Create two via createUser (covers getNewUid + creation + profile pic handling)
    val uid1 = userRepository.getNewUid()
    val uid2 = userRepository.getNewUid()
    userRepository.createUser(user1.username, uid1, profilePicture = "profile1.jpg")
    userRepository.createUser(user2.username, uid2, profilePicture = "")

    expectCount(2)

    // List + membership
    val users = userRepository.getAllUsers()
    assertEquals(2, users.size)
    assert(users.any { it.uid == uid1 && it.username == user1.username })
    assert(users.any { it.uid == uid2 && it.username == user2.username })

    // Exists + get by id
    assert(userRepository.userExists(uid1))
    assert(userRepository.userExists(uid2))
    val u1 = userRepository.getUser(uid1)
    assertEquals(user1.username, u1.username)

    // Edit usernames
    userRepository.editUsername(uid1, "updatedUser1")
    userRepository.editUsername(uid2, "updatedUser2")
    assertEquals("updatedUser1", userRepository.getUser(uid1).username)
    assertEquals("updatedUser2", userRepository.getUser(uid2).username)

    // Add another via addUser (covers add with fixed uid)
    val user3 = user1.copy(uid = "user3", username = "user3name")
    userRepository.addUser(user3)
    expectCount(3)
    assertEquals(user3, userRepository.getUser("user3"))

    // Delete one, check remaining
    userRepository.deleteUser(uid1)
    expectCount(2)
    assert(!userRepository.userExists(uid1))
    assertEquals("updatedUser2", userRepository.getUser(uid2).username)
  }

  @Test
  fun constraints_validation_and_errors() = runTest {
    // Duplicate uid rejected
    userRepository.addUser(user1)
    val dupUid = runCatching { userRepository.addUser(user1) }.exceptionOrNull()
    assert(dupUid is IllegalArgumentException)
    expectCount(1)

    // Same username not allowed across different uids
    val anotherUid = userRepository.getNewUid()
    val sameName =
        runCatching { userRepository.createUser(user1.username, anotherUid, profilePicture = "") }
            .exceptionOrNull()
    assert(sameName != null)

    // Add a second distinct user to test editUsername branches
    userRepository.addUser(user2)

    // Edit username: changing to existing username should fail (TakenUsernameException)
    val taken =
        runCatching { userRepository.editUsername(user1.uid, user2.username) }.exceptionOrNull()
    assert(taken is TakenUsernameException)

    // Edit username: changing to the same current username should no-op
    val before = userRepository.getUser(user2.uid).username
    userRepository.editUsername(user2.uid, before)
    val after = userRepository.getUser(user2.uid).username
    assertEquals(before, after)

    // Get non-existent
    val nf = runCatching { userRepository.getUser("nonExistentUser123") }.exceptionOrNull()
    assert(nf is NoSuchElementException)

    // Edit username invalid args and not found
    val blankId = runCatching { userRepository.editUsername("", "newUsername") }.exceptionOrNull()
    assert(
        blankId is IllegalArgumentException && blankId.message?.contains("cannot be blank") == true)
    val blankName = runCatching { userRepository.editUsername(user1.uid, "") }.exceptionOrNull()
    assert(
        blankName is IllegalArgumentException &&
            blankName.message?.contains("cannot be blank") == true)
    val editNf = runCatching { userRepository.editUsername("missing", "new") }.exceptionOrNull()
    assert(editNf is NoSuchElementException)

    // Delete invalid and not found
    val delBlank = runCatching { userRepository.deleteUser("") }.exceptionOrNull()
    assert(
        delBlank is IllegalArgumentException &&
            delBlank.message?.contains("cannot be blank") == true)
    val delNf = runCatching { userRepository.deleteUser("nonExistentUser123") }.exceptionOrNull()
    assert(delNf is NoSuchElementException)

    // getNewUid uniqueness
    val count = 50
    val unique = (0 until count).map { userRepository.getNewUid() }.toSet()
    assertEquals(count, unique.size)
  }

  @Test
  fun invalidData_handling_filtersList_and_failsGet() = runTest {
    // Seed valid users
    userRepository.addUser(user1)
    userRepository.addUser(user2)

    // Invalid/random doc should be ignored in listing
    put("invalidDoc", mapOf("random" to "data"))

    // Blank and null names => userExists false
    put("userWithBlankName", mapOf("uid" to "userWithBlankName", "username" to ""))
    put("userWithNullName", mapOf("uid" to "userWithNullName"))
    assert(!userRepository.userExists("userWithBlankName"))
    assert(!userRepository.userExists("userWithNullName"))

    // Corrupt one existing user so it cannot deserialize properly
    patch(user1.uid, mapOf("uid" to 12345)) // wrong type
    val corruptedGet = runCatching { userRepository.getUser(user1.uid) }.exceptionOrNull()
    assert(corruptedGet != null)

    // Incomplete user doc should cause getUser to fail with IllegalStateException
    put("invalidUser", mapOf("uid" to "invalidUser"))
    val incomplete = runCatching { userRepository.getUser("invalidUser") }.exceptionOrNull()
    assert(incomplete is IllegalStateException)

    // Listing should skip invalid/corrupted and keep valid ones only
    val listed = userRepository.getAllUsers()
    assert(listed.any { it.uid == user2.uid })
    assert(listed.none { it.uid == user1.uid }) // corrupted
    assert(listed.none { it.uid == "invalidDoc" })
  }
}
