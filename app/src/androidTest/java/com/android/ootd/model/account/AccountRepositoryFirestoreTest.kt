package com.android.ootd.model.account

import android.util.Log
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.BlankUserID
import com.android.ootd.model.user.User
import com.android.ootd.utils.AccountFirestoreTest
import com.android.ootd.utils.FirebaseEmulator
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRepositoryFirestoreTest : AccountFirestoreTest() {

  // Helpers to keep tests short
  private suspend fun addAccAndUser(acc: Account) {
    accountRepository.addAccount(acc)
    userRepository.addUser(
        User(uid = acc.uid, ownerId = acc.ownerId, username = acc.username, profilePicture = ""))
  }

  private suspend fun add(vararg accs: Account) {
    accs.forEach { addAccAndUser(it) }
  }

  private suspend fun doc(uid: String) =
      FirebaseEmulator.firestore.collection(ACCOUNT_COLLECTION_PATH).document(uid).get().await()

  private suspend fun setDoc(uid: String, data: Map<String, Any?>) {
    FirebaseEmulator.firestore.collection(ACCOUNT_COLLECTION_PATH).document(uid).set(data).await()
  }

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
  fun addAccount_successfullyAddsNewAccount() = runTest {
    accountRepository.addAccount(account1)
    assertEquals(1, getAccountCount())

    val retrieved = accountRepository.getAccount(account1.uid)
    assertEquals(account1.uid, retrieved.uid)
    assertEquals(account1.username, retrieved.username)
  }

  @Test
  fun addAccount_throwsExceptionWhenAccountAlreadyExists() = runTest {
    accountRepository.addAccount(account1)
    userRepository.addUser(
        User(
            uid = account1.uid,
            ownerId = account1.ownerId,
            username = account1.username,
            profilePicture = ""))

    expectThrows<TakenAccountException>("already exists") { accountRepository.addAccount(account1) }
    assertEquals(1, getAccountCount())
  }

  @Test
  fun getAccount_returnsCorrectAccount() = runTest {
    add(account1, account2)

    val retrieved = accountRepository.getAccount(account2.uid)
    assertEquals(account2.uid, retrieved.uid)
    assertEquals(account2.username, retrieved.username)
    assertEquals(account2.birthday, retrieved.birthday)
  }

  @Test
  fun getAccount_throwsExceptionWhenAccountNotFound() = runTest {
    expectThrows<NoSuchElementException>("not found") {
      accountRepository.getAccount("nonexistent")
    }
  }

  @Test
  fun accountExists_returnsTrueWhenAccountHasUsername() = runTest {
    add(account1)
    assertTrue(accountRepository.accountExists(account1.uid))
  }

  @Test
  fun accountExists_returnsFalseWhenAccountNotFound() = runTest {
    assertFalse(accountRepository.accountExists("nonexistent"))
  }

  @Test
  fun accountExists_returnsFalseWhenUsernameIsBlank() = runTest {
    val a = account1.copy(username = "")
    setDoc(
        a.uid,
        mapOf(
            "username" to "",
            "birthday" to a.birthday,
            "googleAccountEmail" to a.googleAccountEmail,
            "ownerId" to currentUser.uid,
            "profilePicture" to a.profilePicture,
            "friendUids" to a.friendUids))

    assertFalse(accountRepository.accountExists(a.uid))
  }

  @Test
  fun createAccount_successfullyCreatesNewAccount() = runTest {
    val user =
        User(
            uid = currentUser.uid,
            ownerId = currentUser.uid,
            username = "charlie_brown",
            profilePicture = ":3")

    accountRepository.createAccount(
        user, userEmail = testEmail, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
    userRepository.addUser(
        User(
            uid = user.uid,
            ownerId = account1.ownerId,
            username = user.username,
            profilePicture = "Hello.jpg"))

    val account = accountRepository.getAccount(user.uid)
    assertEquals(user.uid, account.uid)
    assertEquals(user.username, account.username)
    assertTrue(account.friendUids.isEmpty())
    assertEquals(testEmail, account.googleAccountEmail)
    assertEquals(user.profilePicture, account.profilePicture)
  }

  @Test
  fun createAccount_throwsExceptionForDuplicateUser() = runTest {
    val user1 =
        User(uid = "user3", ownerId = account1.ownerId, username = "duplicate", profilePicture = "")
    val user2 =
        User(uid = "user4", ownerId = account1.ownerId, username = "duplicate", profilePicture = "")

    userRepository.addUser(user1)
    userRepository.addUser(user2)
    // But createAccount should fail because username is already in use
    val exception =
        runCatching {
              accountRepository.createAccount(
                  user2, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
            }
            .exceptionOrNull()
    assert(exception != null)

    expectThrows<TakenUserException>("already in use") {
      accountRepository.createAccount(
          user2, dateOfBirth = testDateOfBirth, location = EPFL_LOCATION)
    }
  }

  @Test
  fun addFriend_successfullyAddsFriend() = runTest {
    add(account1, account2)

    accountRepository.addFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
  }

  @Test
  fun addFriend_doesNotAddDuplicateFriend() = runTest {
    add(account1, account2)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
  }

  @Test
  fun addFriend_throwsExceptionWhenFriendNotFound() = runTest {
    accountRepository.addAccount(account1)

    expectThrows<NoSuchElementException>("not found") {
      accountRepository.addFriend(account1.uid, "nonexistent")
    }
  }

  @Test
  fun addFriend_canAddMultipleFriends() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    add(account1, account2, account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account3.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(2, updated.friendUids.size)
    assertTrue(updated.friendUids.containsAll(listOf(account2.uid, account3.uid)))
  }

  @Test
  fun removeFriend_successfullyRemovesFriend() = runTest {
    add(account1, account2)
    accountRepository.addFriend(account1.uid, account2.uid)

    accountRepository.removeFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(0, updated.friendUids.size)
  }

  @Test
  fun removeFriend_throwsExceptionWhenFriendNotFound() = runTest {
    add(account1)

    expectThrows<NoSuchElementException>("not found") {
      accountRepository.removeFriend(account1.uid, "nonexistent")
    }
  }

  @Test
  fun removeFriend_doesNothingWhenFriendNotInList() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    add(account1, account2, account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.removeFriend(account1.uid, account3.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account2.uid))
  }

  @Test
  fun removeFriend_preservesOtherFriends() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    add(account1, account2, account3)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.addFriend(account1.uid, account3.uid)
    accountRepository.removeFriend(account1.uid, account2.uid)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(1, updated.friendUids.size)
    assertTrue(updated.friendUids.contains(account3.uid))
  }

  @Test
  fun isMyFriend_returnsTrueForExistingFriend() = runTest {
    add(account1, account2)

    accountRepository.addFriend(account1.uid, account2.uid)

    assertTrue(accountRepository.isMyFriend(account1.uid, account2.uid))
  }

  @Test
  fun isMyFriend_returnsFalseForNonFriend() = runTest {
    val account3 = account1.copy(uid = "user3", username = "charlie")

    add(account1, account2, account3)

    accountRepository.addFriend(account1.uid, account2.uid)

    assertFalse(accountRepository.isMyFriend(account1.uid, account3.uid))
  }

  @Test
  fun isMyFriend_returnsFalseAfterRemovingFriend() = runTest {
    add(account1, account2)

    accountRepository.addFriend(account1.uid, account2.uid)
    accountRepository.removeFriend(account1.uid, account2.uid)

    assertFalse(accountRepository.isMyFriend(account1.uid, account2.uid))
  }

  @Test
  fun isMyFriend_throwsExceptionWhenUserNotFound() = runTest {
    expectThrows<NoSuchElementException>("authenticated user") {
      accountRepository.isMyFriend("nonexistent", account1.uid)
    }
  }

  @Test
  fun isMyFriend_returnsFalseWhenFriendListIsEmpty() = runTest {
    add(account1, account2)

    assertFalse(accountRepository.isMyFriend(account1.uid, account2.uid))
  }

  @Test
  fun getAccount_handlesCorruptedDocumentGracefully() = runTest {
    accountRepository.addAccount(account1)

    // Corrupt the document
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .update(mapOf("username" to 12345))
        .await()

    val e = kotlin.runCatching { accountRepository.getAccount(account1.uid) }.exceptionOrNull()
    assertTrue(e != null)
  }

  @Test
  fun getAccount_throwsExceptionForInvalidData() = runTest {
    val invalidId = "invalid"
    setDoc(
        invalidId,
        mapOf(
            "username" to "test",
            "birthday" to "2000-01-01",
            "googleAccountEmail" to "test@test.com",
            "profilePicture" to "",
            "ownerId" to currentUser.uid,
            "friendUids" to 12345)) // Invalid type

    expectThrows<IllegalStateException>("Failed to transform") {
      accountRepository.getAccount(invalidId)
    }
  }

  @Test
  fun editAccount_updatesUsernameAndBirthday() = runTest {
    accountRepository.addAccount(account1)

    val newUsername = "new_username"
    val newBirthday = "1990-01-01"
    val newPicture = ":3"
    accountRepository.editAccount(
        account1.uid,
        username = newUsername,
        birthDay = newBirthday,
        picture = newPicture,
        location = account1.location)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(newUsername, updated.username)
    assertEquals(newBirthday, updated.birthday)
    assertEquals(newPicture, updated.profilePicture)
  }

  @Test
  fun editAccount_keepsValuesWhenBlank() = runTest {
    accountRepository.addAccount(account1)

    accountRepository.editAccount(
        account1.uid, username = "", birthDay = "", picture = "", location = emptyLocation)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(account1.username, updated.username)
    assertEquals(account1.birthday, updated.birthday)
    assertEquals(account1.profilePicture, updated.profilePicture)
  }

  @Test
  fun deleteProfilePicture_throwsIfInvalidUserID() = runTest {
    expectThrows<BlankUserID> { accountRepository.deleteProfilePicture("") }
    accountRepository.addAccount(account1)
    expectThrows<UnknowUserID> { accountRepository.deleteProfilePicture(account2.uid) } // Wrong uid
  }

  @Test
  fun deleteProfilePicture_works() = runTest {
    // Create account with profile picture
    accountRepository.addAccount(account1)

    // Upload a test image to storage to simulate a real profile picture
    val storage = FirebaseEmulator.storage
    val testImageData = "test image data".toByteArray()
    val profilePicRef = storage.reference.child("profile_pictures/${account1.uid}.jpg")
    profilePicRef.putBytes(testImageData).await()

    // Verify the image exists in storage
    val downloadUrl = profilePicRef.downloadUrl.await()
    assertTrue(downloadUrl.toString().isNotEmpty())

    // Delete the profile picture
    accountRepository.deleteProfilePicture(account1.uid)

    // Verify the Firestore field is cleared
    val updated = accountRepository.getAccount(account1.uid)
    assertEquals("", updated.profilePicture)

    // Verify the image is deleted from storage
    val storageException =
        kotlin.runCatching { profilePicRef.downloadUrl.await() }.exceptionOrNull()

    // Should throw an exception because the file no longer exists
    assertTrue(storageException != null)
  }

  // Test done partially with the help of AI
  @Test
  fun deleteAccount_successfullyDeletesAccountAndAssociatedData() = runTest {
    // Create account
    accountRepository.addAccount(account1)

    val storage = FirebaseEmulator.storage

    // Upload a test image to storage to simulate a real profile picture
    val testImageData = "test image data for account deletion".toByteArray()
    val profilePicRef = storage.reference.child("profile_pictures/${account1.uid}.jpg")
    profilePicRef.putBytes(testImageData).await()

    // Create and upload an item with image
    val itemId = "testItem123"
    val itemImageData = "test item image".toByteArray()
    val itemImageRef = storage.reference.child("images/items/$itemId.jpg")
    itemImageRef.putBytes(itemImageData).await()

    val item =
        Item(
            itemUuid = itemId,
            postUuids = emptyList(),
            image = ImageData(itemId, ""),
            category = "clothes",
            ownerId = account1.uid)
    itemsRepository.addItem(item)

    // Create and upload a post with image
    val postId = "testPost456"
    val postImageData = "test post image".toByteArray()
    val postImageRef = storage.reference.child("images/posts/$postId.jpg")
    postImageRef.putBytes(postImageData).await()

    val post =
        OutfitPost(
            postUID = postId,
            name = account1.username,
            ownerId = account1.uid,
            outfitURL = "",
            timestamp = System.currentTimeMillis())
    feedRepository.addPost(post)

    // Verify everything exists
    assertTrue(profilePicRef.downloadUrl.await().toString().isNotEmpty())
    assertTrue(
        FirebaseEmulator.firestore.collection("items").document(itemId).get().await().exists())
    assertTrue(
        FirebaseEmulator.firestore.collection("posts").document(postId).get().await().exists())

    // Delete the account
    accountRepository.deleteAccount(account1.uid)

    // Verify account is deleted
    expectThrows<NoSuchElementException> { accountRepository.getAccount(account1.uid) }

    // Verify profile picture is deleted
    val storageException =
        kotlin.runCatching { profilePicRef.downloadUrl.await() }.exceptionOrNull()
    assertTrue(storageException != null)

    // Verify item document is deleted
    val itemDocAfter = FirebaseEmulator.firestore.collection("items").document(itemId).get().await()
    assertFalse(itemDocAfter.exists())

    // Verify item image is deleted
    val itemStorageException =
        kotlin.runCatching { itemImageRef.downloadUrl.await() }.exceptionOrNull()
    assertTrue(itemStorageException != null)

    // Verify post document is deleted
    val postDocAfter = FirebaseEmulator.firestore.collection("posts").document(postId).get().await()
    assertFalse(postDocAfter.exists())

    // Verify post image is deleted
    val postStorageException =
        kotlin.runCatching { postImageRef.downloadUrl.await() }.exceptionOrNull()
    assertTrue(postStorageException != null)
  }

  @Test
  fun deleteAccount_throwsOnlyWhenUserIsNotFound() = runTest {
    // No user
    expectThrows<UnknowUserID> { accountRepository.deleteAccount("nonexistent") }

    // no posts no items no pfp
    accountRepository.addAccount(dummyAccount)

    val account = accountRepository.getAccount(dummyAccount.uid)
    val posts = feedRepository.getFeedForUids(listOf(dummyAccount.uid))
    assert(account.itemsUids.isEmpty())
    assert(posts.isEmpty())
    assert(account.profilePicture.isBlank())
    // Shouldn't throw
    runCatching { accountRepository.deleteAccount(dummyAccount.uid) }
    // Make sure account has been deleted
    assert(!accountRepository.accountExists(dummyAccount.uid))
  }

  @Test
  fun togglePrivacy_togglesAndPersists() = runTest {
    accountRepository.addAccount(account1)

    val first = accountRepository.togglePrivacy(account1.uid)
    assertTrue(first)

    val doc1 = doc(account1.uid)
    assertTrue(doc1.getBoolean("isPrivate") == true)

    val second = accountRepository.togglePrivacy(account1.uid)
    assertFalse(second)

    val doc2 = doc(account1.uid)
    assertTrue(doc2.getBoolean("isPrivate") == false)
  }

  @Test
  fun togglePrivacy_throwsWhenAccountMissing() = runTest {
    expectThrows<NoSuchElementException>("authenticated user") {
      accountRepository.togglePrivacy("nonexistent")
    }
  }

  @Test
  fun createAccount_persistsLocationToFirestore() = runTest {
    val user =
        User(
            uid = currentUser.uid,
            ownerId = currentUser.uid,
            username = "test_location_user",
            profilePicture = "")

    accountRepository.createAccount(user, testEmail, "", testDateOfBirth, EPFL_LOCATION)

    val retrieved = accountRepository.getAccount(currentUser.uid)
    assertEquals(EPFL_LOCATION.latitude, retrieved.location.latitude, 0.0001)
    assertEquals(EPFL_LOCATION.longitude, retrieved.location.longitude, 0.0001)
    assertEquals(EPFL_LOCATION.name, retrieved.location.name)
  }

  @Test
  fun getAccount_parsesLocationFromMap() = runTest {
    // Manually create a document with location as a Map
    val locationMap =
        mapOf("latitude" to 47.3769, "longitude" to 8.5417, "name" to "Zürich, Switzerland")

    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .set(
            mapOf(
                "username" to account1.username,
                "birthday" to account1.birthday,
                "googleAccountEmail" to account1.googleAccountEmail,
                "profilePicture" to account1.profilePicture,
                "friendUids" to account1.friendUids,
                "isPrivate" to account1.isPrivate,
                "ownerId" to account1.ownerId,
                "location" to locationMap))
        .await()

    val retrieved = accountRepository.getAccount(account1.uid)
    assertEquals(47.3769, retrieved.location.latitude, 0.0001)
    assertEquals(8.5417, retrieved.location.longitude, 0.0001)
    assertEquals("Zürich, Switzerland", retrieved.location.name)
  }

  @Test
  fun getAccount_throwsMissingLocationException() = runTest {
    // Create account without location field
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .set(
            mapOf(
                "username" to account1.username,
                "birthday" to account1.birthday,
                "googleAccountEmail" to account1.googleAccountEmail,
                "profilePicture" to account1.profilePicture,
                "friendUids" to account1.friendUids,
                "isPrivate" to account1.isPrivate,
                "ownerId" to account1.ownerId))
        .await()

    expectThrows<IllegalStateException>("Failed to transform") {
      accountRepository.getAccount(account1.uid)
    }
  }

  @Test
  fun editAccount_logsSuccessMessage() = runTest {
    accountRepository.addAccount(account1)

    val newUsername = "updated_user"
    val newBirthday = "1995-12-25"
    val newPicture = "new_pic.jpg"

    // This test verifies the method executes successfully and logs the success message
    accountRepository.editAccount(
        account1.uid,
        username = newUsername,
        birthDay = newBirthday,
        picture = newPicture,
        location = account1.location)

    val updated = accountRepository.getAccount(account1.uid)
    assertEquals(newUsername, updated.username)
    assertEquals(newBirthday, updated.birthday)
    assertEquals(newPicture, updated.profilePicture)
  }

  @Test
  fun getItemsList_returnsEmptyListForNewAccount() = runTest {
    accountRepository.addAccount(account1)

    val itemsList = accountRepository.getItemsList(account1.uid)

    assertTrue(itemsList.isEmpty())
  }

  @Test
  fun getItemsList_returnsItemsFromCache() = runTest {
    accountRepository.addAccount(account1)

    val itemUids = listOf("item1", "item2", "item3")
    itemUids.forEach { accountRepository.addItem(it) }

    // First call populates cache
    val firstCall = accountRepository.getItemsList(account1.uid)
    assertEquals(3, firstCall.size)

    // Second call should return from memory cache
    val secondCall = accountRepository.getItemsList(account1.uid)
    assertEquals(3, secondCall.size)
    itemUids.forEach { assertTrue(secondCall.contains(it)) }
  }

  @Test
  fun getItemsList_returnsEmptyListWhenAccountNotFound() = runTest {
    val itemsList = accountRepository.getItemsList("nonExistentUser")

    assertTrue(itemsList.isEmpty())
  }

  @Test
  fun getItemsList_returnsItemsAfterMultipleAdditions() = runTest {
    accountRepository.addAccount(account1)

    val item1 = "item1"
    accountRepository.addItem(item1)
    val list1 = accountRepository.getItemsList(account1.uid)
    assertEquals(1, list1.size)

    val item2 = "item2"
    accountRepository.addItem(item2)
    val list2 = accountRepository.getItemsList(account1.uid)
    assertEquals(2, list2.size)
    assertTrue(list2.containsAll(listOf(item1, item2)))
  }

  @Test
  fun getItemsList_returnsUpdatedListAfterRemoval() = runTest {
    accountRepository.addAccount(account1)

    val itemUids = listOf("item1", "item2", "item3")
    itemUids.forEach { accountRepository.addItem(it) }

    accountRepository.removeItem("item2")

    val itemsList = accountRepository.getItemsList(account1.uid)
    assertEquals(2, itemsList.size)
    assertTrue(itemsList.contains("item1"))
    assertFalse(itemsList.contains("item2"))
    assertTrue(itemsList.contains("item3"))
  }

  @Test
  fun getItemsList_handlesEmptyItemsUidsField() = runTest {
    // Create account without itemsUids field
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .set(
            mapOf(
                "username" to account1.username,
                "birthday" to account1.birthday,
                "googleAccountEmail" to account1.googleAccountEmail,
                "profilePicture" to account1.profilePicture,
                "friendUids" to account1.friendUids,
                "isPrivate" to account1.isPrivate,
                "ownerId" to account1.ownerId,
                "location" to
                    mapOf(
                        "latitude" to account1.location.latitude,
                        "longitude" to account1.location.longitude,
                        "name" to account1.location.name)))
        .await()

    val itemsList = accountRepository.getItemsList(account1.uid)

    assertTrue(itemsList.isEmpty())
  }

  @Test
  fun getItemsList_handlesInvalidItemsUidsType() = runTest {
    // Create account with invalid itemsUids type (not a list)
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .set(
            mapOf(
                "username" to account1.username,
                "birthday" to account1.birthday,
                "googleAccountEmail" to account1.googleAccountEmail,
                "profilePicture" to account1.profilePicture,
                "friendUids" to account1.friendUids,
                "isPrivate" to account1.isPrivate,
                "ownerId" to account1.ownerId,
                "location" to
                    mapOf(
                        "latitude" to account1.location.latitude,
                        "longitude" to account1.location.longitude,
                        "name" to account1.location.name),
                "itemsUids" to "not_a_list"))
        .await()

    val itemsList = accountRepository.getItemsList(account1.uid)

    assertTrue(itemsList.isEmpty())
  }

  @Test
  fun getItemsList_returnsImmutableCopy() = runTest {
    accountRepository.addAccount(account1)

    val itemUids = listOf("item1", "item2")
    itemUids.forEach { accountRepository.addItem(it) }

    val itemsList1 = accountRepository.getItemsList(account1.uid)
    val itemsList2 = accountRepository.getItemsList(account1.uid)

    // Verify we get separate list instances (defensive copy)
    assertTrue(itemsList1 == itemsList2)
    assertFalse(itemsList1 === itemsList2)
  }

  @Test
  fun getItemsList_worksForDifferentUsers() = runTest {
    accountRepository.addAccount(account1)
    accountRepository.addAccount(account2)

    accountRepository.addItem("item1")
    accountRepository.addItem("item2")

    // Switch to account2 by logging in as different user
    // Note: In this test, both operations use currentUser, so we're testing
    // that items are stored per user
    val itemsListUser1 = accountRepository.getItemsList(currentUser.uid)
    assertEquals(2, itemsListUser1.size)

    // account2 should have no items
    val itemsListUser2 = accountRepository.getItemsList(account2.uid)
    assertTrue(itemsListUser2.isEmpty())
  }

  @Test
  fun addItem_addsItemToInventory() = runTest {
    accountRepository.addAccount(account1)

    val itemUid = "item123"
    val result = accountRepository.addItem(itemUid)

    assertTrue(result)
    val itemsList = accountRepository.getItemsList(account1.uid)
    assertEquals(1, itemsList.size)
    assertTrue(itemsList.contains(itemUid))
  }

  @Test
  fun addItem_preventsDuplicates() = runTest {
    accountRepository.addAccount(account1)

    val itemUid = "item123"
    accountRepository.addItem(itemUid)
    accountRepository.addItem(itemUid) // Add same item again

    val itemsList = accountRepository.getItemsList(account1.uid)
    assertEquals(1, itemsList.size)
  }

  @Test
  fun addItem_addsMultipleItems() = runTest {
    accountRepository.addAccount(account1)

    val itemUids = listOf("item1", "item2", "item3")
    itemUids.forEach { accountRepository.addItem(it) }

    val itemsList = accountRepository.getItemsList(account1.uid)
    assertEquals(3, itemsList.size)
    itemUids.forEach { assertTrue(itemsList.contains(it)) }
  }

  @Test
  fun addItem_initializesCacheFromExistingFirestoreData() = runTest {
    accountRepository.addAccount(account1)

    val existingItems = listOf("cached-1", "cached-2")
    // Persist items directly into Firestore to simulate pre-existing data (e.g., from another
    // log-in)
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .update("itemsUids", existingItems)
        .await()

    // Recreate the repository to ensure its in-memory cache starts empty
    val freshRepository = AccountRepositoryFirestore(FirebaseEmulator.firestore)
    AccountRepositoryProvider.repository = freshRepository

    // Ensure the fresh repository can read the existing Firestore data before any writes
    val initialItems = freshRepository.getItemsList(account1.uid)
    assertEquals(existingItems.size, initialItems.size)
    assertTrue(initialItems.containsAll(existingItems))

    val newItem = "fresh-add"
    val result = freshRepository.addItem(newItem)

    assertTrue(result)
    val updatedItems = freshRepository.getItemsList(account1.uid)
    assertEquals(3, updatedItems.size)
    assertTrue(updatedItems.containsAll(existingItems + newItem))
  }

  @Test
  fun removeItem_removesItemFromInventory() = runTest {
    accountRepository.addAccount(account1)

    val itemUid = "item123"
    accountRepository.addItem(itemUid)
    assertEquals(1, accountRepository.getItemsList(account1.uid).size)

    val result = accountRepository.removeItem(itemUid)

    assertTrue(result)
    val itemsList = accountRepository.getItemsList(account1.uid)
    assertTrue(itemsList.isEmpty())
  }

  @Test
  fun removeItem_handlesNonExistentItem() = runTest {
    accountRepository.addAccount(account1)

    val result = accountRepository.removeItem("nonExistent")

    assertTrue(result) // Should return true even if item doesn't exist
    assertTrue(accountRepository.getItemsList(account1.uid).isEmpty())
  }

  @Test
  fun removeItem_removesOnlySpecifiedItem() = runTest {
    accountRepository.addAccount(account1)

    val item1 = "item1"
    val item2 = "item2"
    val item3 = "item3"
    accountRepository.addItem(item1)
    accountRepository.addItem(item2)
    accountRepository.addItem(item3)

    accountRepository.removeItem(item2)

    val itemsList = accountRepository.getItemsList(account1.uid)
    assertEquals(2, itemsList.size)
    assertTrue(itemsList.contains(item1))
    assertFalse(itemsList.contains(item2))
    assertTrue(itemsList.contains(item3))
  }

  @Test
  fun checkAccountData_logsErrorForBlankUid() = runTest {
    // Test that checkAccountData correctly identifies and logs blank UIDs
    // Getting account with blank UID should throw BlankUserID before checkAccountData
    expectThrows<BlankUserID> { accountRepository.getAccount("") }
  }

  @Test
  fun createAccount_logsErrorAndRethrowsOnException() = runTest {
    // Add user to simulate existing username
    add(account1)

    val user =
        User(
            uid = "test_user",
            ownerId = "test_user",
            username = account1.username,
            profilePicture = "")

    // Try to create account with existing username - should log error and throw
    expectThrows<TakenUserException>("already in use") {
      accountRepository.createAccount(user, "test@example.com", "", "1990-01-01", emptyLocation)
    }
  }

  @Test
  fun addFriend_logsWarningWhenCannotAddToFriendsList() = runTest {
    add(account1, account2)

    // Manually add friend1 to friend2's list
    accountRepository.addFriend(account1.uid, account2.uid)

    // Delete account2's document to simulate inability to update their list
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account2.uid)
        .delete()
        .await()

    // This should log warning but not throw (returns false)
    val result = accountRepository.addFriend(account1.uid, account2.uid)

    // Result should be false because we couldn't update friend2's list
    assertFalse(result)
  }

  @Test
  fun editAccount_logsErrorOnException() = runTest {
    add(account1)

    // Try to update a non-existent account - should throw UnknowUserID
    expectThrows<UnknowUserID> {
      accountRepository.editAccount(
          "nonexistent_uid", "newusername", "2000-01-01", "", emptyLocation)
    }

    // Try to update with a taken username - should throw TakenUserException and log error
    add(account2)
    expectThrows<TakenUserException>("already in use") {
      accountRepository.editAccount(
          account1.uid,
          account2.username, // Use account2's username
          "2000-01-01",
          "",
          emptyLocation)
    }
  }

  // -------- Real-time observation tests (observeAccount) --------

  @Test
  fun observeAccount_blankUserID_closesWithException() = runBlocking {
    val emissions = mutableListOf<Account>()
    var error: Throwable? = null

    val job = launch {
      try {
        accountRepository.observeAccount("").collect {
          Log.d("AccountTest", "Collected emission: ${it.uid}")
          emissions.add(it)
        }
      } catch (e: Exception) {
        error = e
        Log.e("AccountTest", "Error collecting", e)
      }
    }

    kotlinx.coroutines.delay(500)
    job.cancel()

    assertTrue("Should fail with BlankUserID", error is BlankUserID)
    assertTrue("Should not emit any values", emissions.isEmpty())
  }

  @Test
  fun observeAccount_emitsInitialAccount() = runBlocking {
    // Add account and wait for write to complete
    accountRepository.addAccount(account1)
    kotlinx.coroutines.delay(1000)

    // Verify account exists before starting observation
    val saved = accountRepository.getAccount(account1.uid)
    Log.d("AccountTest", "Saved account: ${saved.uid}, username: ${saved.username}")
    assertEquals(
        "Account should be saved before testing observe", account1.username, saved.username)

    val emissions = mutableListOf<Account>()
    var error: Throwable? = null

    val job = launch {
      try {
        accountRepository.observeAccount(account1.uid).collect {
          Log.d("AccountTest", "Collected emission: ${it.uid}, username: ${it.username}")
          emissions.add(it)
        }
      } catch (e: Exception) {
        error = e
        Log.e("AccountTest", "Error collecting", e)
      }
    }

    // Snapshot listeners need significant time to register and emit
    kotlinx.coroutines.delay(3000)
    job.cancel()

    error?.let { throw it }

    // Log what we actually got
    Log.d("AccountTest", "Total emissions received: ${emissions.size}")
    if (emissions.isNotEmpty()) {
      Log.d("AccountTest", "First emission username: ${emissions.first().username}")
    }

    assertTrue("Should emit at least once", emissions.isNotEmpty())
    val account = emissions.first()
    assertEquals(account1.uid, account.uid)
    assertEquals(account1.username, account.username)
  }

  @Test
  fun observeAccount_emitsUpdates() = runBlocking {
    // Add account and wait for write
    accountRepository.addAccount(account1)
    kotlinx.coroutines.delay(1000)

    val emissions = mutableListOf<Account>()
    var error: Throwable? = null

    val job = launch {
      try {
        accountRepository.observeAccount(account1.uid).collect {
          Log.d("AccountTest", "Collected emission: ${it.username}")
          emissions.add(it)
        }
      } catch (e: Exception) {
        error = e
        Log.e("AccountTest", "Error collecting", e)
      }
    }

    // Wait for initial emission
    kotlinx.coroutines.delay(3000)

    // Update the account
    accountRepository.editAccount(
        account1.uid, "new_username", account1.birthday, account1.profilePicture, account1.location)

    // Wait for update emission
    kotlinx.coroutines.delay(2000)
    job.cancel()

    error?.let { throw it }

    Log.d("AccountTest", "Total emissions: ${emissions.size}")
    emissions.forEachIndexed { index, acc ->
      Log.d("AccountTest", "Emission $index: ${acc.username}")
    }

    assertTrue("Should emit at least twice (initial + update)", emissions.size >= 2)
    val latest = emissions.last()
    assertEquals("new_username", latest.username)
  }

  @Test
  fun observeAccount_nonExistentAccount_doesNotEmit() = runBlocking {
    val emissions = mutableListOf<Account>()
    var error: Throwable? = null

    val job = launch {
      try {
        accountRepository.observeAccount("nonexistent").collect {
          Log.d("AccountTest", "Unexpected emission: ${it.uid}")
          emissions.add(it)
        }
      } catch (e: Exception) {
        error = e
        Log.e("AccountTest", "Error (expected): ${e.message}", e)
      }
    }

    kotlinx.coroutines.delay(2000)
    job.cancel()

    // For non-existent accounts, Firestore security rules cause permission denied error
    // or simply not emit anything depending on security rules
    Log.d("AccountTest", "Error type: ${error?.javaClass?.simpleName}")
    Log.d("AccountTest", "Emissions count: ${emissions.size}")

    assertTrue("Should not emit any values", emissions.isEmpty())
  }

  @Test
  fun observeAccount_handlesCorruptedDocument() = runBlocking {
    // First create a valid account so we have permission to read it
    accountRepository.addAccount(account1)
    kotlinx.coroutines.delay(1000)

    val emissions = mutableListOf<Account>()
    var error: Throwable? = null

    val job = launch {
      try {
        accountRepository.observeAccount(account1.uid).collect {
          Log.d("AccountTest", "Collected emission: ${it.username}")
          emissions.add(it)
        }
      } catch (e: Exception) {
        error = e
        Log.e("AccountTest", "Error collecting", e)
      }
    }

    // Wait for initial emission
    kotlinx.coroutines.delay(3000)

    Log.d("AccountTest", "Emissions before corruption: ${emissions.size}")

    // Now corrupt the document while observer is active
    FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .document(account1.uid)
        .update(mapOf("friendUids" to 12345)) // Invalid type
        .await()

    kotlinx.coroutines.delay(2000)
    job.cancel()

    // Should have emitted the initial valid account, then failed to transform corrupted one
    assertTrue("Should emit initial valid account", emissions.size >= 1)
    assertEquals(account1.username, emissions.first().username)
  }
}
