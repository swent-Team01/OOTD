package com.android.ootd.model.account

import android.util.Log
import com.android.ootd.model.items.ITEMS_COLLECTION
import com.android.ootd.model.items.ImageFilenameSanitizer
import com.android.ootd.model.items.OWNER_ATTRIBUTE_NAME
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.isValidLocation
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryProvider
import com.android.ootd.model.post.POSTS_COLLECTION
import com.android.ootd.model.user.BlankUserID
import com.android.ootd.model.user.USER_COLLECTION_PATH
import com.android.ootd.model.user.User
import com.android.ootd.utils.LocationUtils.locationFromMap
import com.android.ootd.utils.LocationUtils.mapFromLocation
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

const val ACCOUNT_COLLECTION_PATH = "accounts"
const val PUBLIC_LOCATIONS_COLLECTION_PATH = "publicLocations"
private const val ITEMS_PATH = "images/items"
const val USER_NOT_LOGGED = "User not logged in"

// Custom exception for taken user scenario
class TakenUserException(message: String) : Exception(message)

class TakenAccountException(message: String) : Exception(message)

class UnknowUserID : Exception("No account with such userID")

/** Convert domain PublicLocation to Firestore-friendly Map */
private fun PublicLocation.toFirestoreMap(): Map<String, Any> =
    mapOf("ownerId" to ownerId, "username" to username, "location" to mapFromLocation(location))

/** Convert Firestore DocumentSnapshot to domain PublicLocation */
private fun DocumentSnapshot.toPublicLocation(): PublicLocation {
  val ownerId = getString("ownerId") ?: id
  val username = getString("username") ?: ""

  val locationRaw = this["location"]
  val location =
      when (locationRaw) {
        null -> throw MissingLocationException()
        is Map<*, *> -> locationFromMap(locationRaw)
        else -> throw MissingLocationException()
      }

  return PublicLocation(ownerId = ownerId, username = username, location = location)
}

/** Convert domain Account to Firestore-friendly Map (excludes uid as it's the document id) */
private fun Account.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "username" to username,
        "birthday" to birthday,
        "googleAccountEmail" to googleAccountEmail,
        "profilePicture" to profilePicture,
        "friendUids" to friendUids,
        "isPrivate" to isPrivate,
        "ownerId" to ownerId,
        "location" to mapFromLocation(location),
        "itemsUids" to itemsUids,
        "starredItemUids" to starredItemUids)

/** Convert Firestore DocumentSnapshot to domain Account (uses document id as uid) */
private fun DocumentSnapshot.toAccount(): Account {
  val username = getString("username") ?: ""
  val birthday = getString("birthday") ?: ""
  val email = getString("googleAccountEmail") ?: ""
  val picture = getString("profilePicture") ?: ""
  val isPrivate = getBoolean("isPrivate") ?: false

  // Validate that friendUids is actually a List, throw if not
  val friendUidsRaw = this["friendUids"]
  val friends =
      when {
        friendUidsRaw == null -> emptyList()
        friendUidsRaw is List<*> -> friendUidsRaw.filterIsInstance<String>()
        else ->
            throw IllegalArgumentException(
                "friendUids field is not a List but ${friendUidsRaw::class.simpleName}")
      }

  // Parse location if present, otherwise throw MissingLocationException
  val locationRaw = this["location"]
  val location =
      when {
        locationRaw == null -> throw MissingLocationException()
        locationRaw is Map<*, *> -> locationFromMap(locationRaw)
        else -> throw MissingLocationException()
      }

  // Parse itemsUids if present
  val itemsUidsRaw = this["itemsUids"]
  val itemsUids =
      when {
        itemsUidsRaw == null -> emptyList()
        itemsUidsRaw is List<*> -> itemsUidsRaw.filterIsInstance<String>()
        else -> emptyList()
      }

  val starredRaw = this["starredItemUids"]
  val starred =
      when {
        starredRaw == null -> emptyList()
        starredRaw is List<*> -> starredRaw.filterIsInstance<String>()
        else -> emptyList()
      }

  val uid = id // document id is the user id

  return Account(
      uid = uid,
      ownerId = uid,
      username = username,
      birthday = birthday,
      googleAccountEmail = email,
      profilePicture = picture,
      friendUids = friends,
      location = location,
      isPrivate = isPrivate,
      itemsUids = itemsUids,
      starredItemUids = starred)
}

class AccountRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val outfitPostRepository: OutfitPostRepository = OutfitPostRepositoryProvider.repository
) : AccountRepository {

  companion object {

    private const val TIMEOUT = 2000L
    private const val TAG = "AccountRepositoryFirestore"
  }

  /** Helper method to check account data as firestore might add the default values */
  private fun checkAccountData(account: Account): Account? {
    if (account.uid.isBlank()) {
      Log.e(TAG, "Invalid account data: uid is blank")
      return null
    }
    return account
  }

  /** Helper method to transform a Firestore document into an Account object */
  private fun transformAccountDocument(document: DocumentSnapshot): Account? {
    return try {
      val account = document.toAccount()
      checkAccountData(account)
    } catch (e: Exception) {
      Log.e(TAG, "Error transforming document ${document.id}: ${e.message}", e)
      return null
    }
  }

  override suspend fun createAccount(
      user: User,
      userEmail: String,
      dateOfBirth: String,
      location: Location,
      isPrivate: Boolean
  ) {
    if (userExists(user)) {
      Log.e(TAG, "Username already in use")
      throw TakenUserException("Username already in use")
    }

    val newAccount =
        Account(
            uid = user.uid,
            ownerId = user.ownerId,
            googleAccountEmail = userEmail,
            username = user.username,
            birthday = dateOfBirth,
            profilePicture = user.profilePicture,
            location = location,
            isPrivate = isPrivate)
    try {
      addAccount(newAccount)
    } catch (e: Exception) {
      Log.e(TAG, "Error while creating account: ${e.message}", e)
      throw e
    }
  }

  override suspend fun getAccount(userID: String): Account {
    return try {
      if (userID.isBlank()) throw BlankUserID()
      val document = db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get().await()

      if (!document.exists()) {
        throw NoSuchElementException("Account with ID $userID not found")
      }

      transformAccountDocument(document)
          ?: throw IllegalStateException("Failed to transform document with ID $userID")
    } catch (e: Exception) {
      Log.e(TAG, "Error getting account $userID: ${e.message}", e)
      throw e
    }
  }

  override suspend fun accountExists(userID: String): Boolean {
    return try {
      if (userID.isBlank()) throw BlankUserID()
      // Here we need to check with the user collection where we look
      val accountDocument = db.collection(USER_COLLECTION_PATH).document(userID).get().await()

      if (!accountDocument.exists()) {
        false
      } else {
        val username = accountDocument.getString("username")
        !username.isNullOrBlank()
      }
    }
    // There is no way we can get permission denied because we are checking the public user
    // collection.
    catch (e: Exception) {
      Log.e(TAG, "Error checking account existence: ${e.message}", e)
      throw e
    }
  }

  override suspend fun addAccount(account: Account) {
    try {
      // Check if we can read this account (will throw if trying to create someone else's account)
      if (accountExists(account.uid)) {
        throw TakenAccountException("Account with UID ${account.uid} already exists")
      }

      db.collection(ACCOUNT_COLLECTION_PATH)
          .document(account.uid)
          .set(account.toFirestoreMap())
          .await()

      // If account is public, add to publicLocations collection
      if (!account.isPrivate && isValidLocation(account.location)) {
        val publicLocation =
            PublicLocation(
                ownerId = account.uid, username = account.username, location = account.location)
        db.collection(PUBLIC_LOCATIONS_COLLECTION_PATH)
            .document(account.uid)
            .set(publicLocation.toFirestoreMap())
            .await()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error adding account: ${e.message}", e)
      throw e
    }
  }

  override suspend fun addFriend(userID: String, friendID: String): Boolean {
    try {
      if (userID.isBlank()) throw BlankUserID()
      val friendUserDoc = db.collection(USER_COLLECTION_PATH).document(friendID).get().await()

      if (!friendUserDoc.exists()) {
        Log.e(TAG, "The user with id $friendID not found")
        throw NoSuchElementException("The user with id $friendID not found")
      }

      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(userID)

      // With arrayUnion there can be no duplicates
      // https://firebase.google.com/docs/firestore/manage-data/add-data , Update elements in an
      // array section

      // Add the friendID in your friend list
      userRef.update("friendUids", FieldValue.arrayUnion(friendID)).await()

      // Add yourself to the friendID's friend list.
      // This works because we have not deleted the follow notification yet.
      val friendRef = db.collection(ACCOUNT_COLLECTION_PATH).document(friendID)
      try {
        friendRef.update("friendUids", FieldValue.arrayUnion(userID)).await()
        return true
      } catch (e: Exception) {
        // If we can't update their account (maybe we're not in their friend list),
        // log it but don't fail the entire operation
        Log.w(TAG, "Could not add $userID to $friendID's friend list: ${e.message}")
        return false
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error adding friend $friendID to $userID: ${e.message}", e)
      throw e
    }
  }

  override suspend fun removeFriend(userID: String, friendID: String) {
    try {
      if (userID.isBlank()) throw BlankUserID()
      // Check if the friend exists in the public User collection
      val friendUserDoc = db.collection(USER_COLLECTION_PATH).document(friendID).get().await()

      if (!friendUserDoc.exists()) {
        throw NoSuchElementException("Friend with ID $friendID not found")
      }

      // Remove friendID from userID's friend list
      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(userID)
      userRef.update("friendUids", FieldValue.arrayRemove(friendID)).await()

      // Remove userID from friendID's friend list (if they have us as a friend)
      val friendRef = db.collection(ACCOUNT_COLLECTION_PATH).document(friendID)
      try {
        friendRef.update("friendUids", FieldValue.arrayRemove(userID)).await()
      } catch (e: Exception) {
        // If we can't update their account (maybe we're not in their friend list),
        // log it but don't fail the entire operation
        Log.w(TAG, "Could not remove $userID from $friendID's friend list: ${e.message}")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error removing friend $friendID from $userID: ${e.message}", e)
      throw e
    }
  }

  override suspend fun isMyFriend(userID: String, friendID: String): Boolean {
    if (userID.isBlank()) throw BlankUserID()
    val document = db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get().await()

    if (!document.exists()) {
      throw NoSuchElementException("The authenticated user has not been added to the database")
    }

    val userData = transformAccountDocument(document)

    return userData != null && userData.friendUids.contains(friendID)
  }

  override suspend fun togglePrivacy(userID: String): Boolean {
    if (userID.isBlank()) throw BlankUserID()
    val document = db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get().await()

    if (!document.exists()) {
      throw NoSuchElementException("The authenticated user has not been added to the database")
    }

    val myAccount =
        transformAccountDocument(document)
            ?: throw IllegalStateException("Failed to transform document with ID $userID")

    val newPrivacySetting = !myAccount.isPrivate

    // Update privacy setting in accounts collection
    db.collection(ACCOUNT_COLLECTION_PATH)
        .document(userID)
        .update("isPrivate", newPrivacySetting)
        .await()

    // Sync public location based on new privacy setting
    if (!newPrivacySetting) {
      // Account is now public - add to publicLocations only if location is valid
      if (isValidLocation(myAccount.location)) {
        val publicLocation =
            PublicLocation(
                ownerId = myAccount.ownerId,
                username = myAccount.username,
                location = myAccount.location)
        db.collection(PUBLIC_LOCATIONS_COLLECTION_PATH)
            .document(userID)
            .set(publicLocation.toFirestoreMap())
            .await()
      } else {
        throw InvalidLocationException()
      }
    } else {
      // Account is now private - remove from publicLocations
      db.collection(PUBLIC_LOCATIONS_COLLECTION_PATH).document(userID).delete().await()
    }

    return newPrivacySetting
  }

  override suspend fun deleteAccount(userID: String) {
    if (userID.isBlank()) throw BlankUserID()

    deleteProfilePicture(userID)
    deleteUserPosts(userID)
    deleteUserItems(userID)

    val account = getAccount(userID)
    deleteUserFriends(account)

    val publicLocationDoc =
        db.collection(PUBLIC_LOCATIONS_COLLECTION_PATH).document(userID).get().await()
    if (publicLocationDoc.exists()) {
      db.collection(PUBLIC_LOCATIONS_COLLECTION_PATH).document(userID).delete().await()
    }

    db.collection(ACCOUNT_COLLECTION_PATH).document(userID).delete().await()
  }

  override suspend fun editAccount(
      userID: String,
      username: String,
      birthDay: String,
      picture: String,
      location: Location
  ) {
    try {
      if (userID.isBlank()) throw BlankUserID()
      val user = getAccount(userID)

      val isNewUsername = username != user.username && username.isNotBlank()

      val newUsername = username.takeIf { isNewUsername } ?: user.username
      val newBirthDate = birthDay.takeIf { it.isNotBlank() } ?: user.birthday
      val newProfilePic = picture.takeIf { it.isNotBlank() } ?: user.profilePicture
      val newLocation = location.takeIf { isValidLocation(it) } ?: user.location

      if (isNewUsername) {
        val querySnapshot =
            db.collection(USER_COLLECTION_PATH).whereEqualTo("username", newUsername).get().await()

        if (querySnapshot.documents.any { it.id != userID }) {
          throw TakenUserException("Username '$newUsername' is already in use")
        }
      }

      db.collection(ACCOUNT_COLLECTION_PATH)
          .document(userID)
          .update(
              mapOf(
                  "username" to newUsername,
                  "birthday" to newBirthDate,
                  "profilePicture" to newProfilePic,
                  "location" to mapFromLocation(newLocation)))
          .await()

      // If account is public, sync the public location
      if (!user.isPrivate) {
        val publicLocation =
            PublicLocation(ownerId = userID, username = newUsername, location = newLocation)
        db.collection(PUBLIC_LOCATIONS_COLLECTION_PATH)
            .document(userID)
            .set(publicLocation.toFirestoreMap())
            .await()
      }
    } catch (_: NoSuchElementException) {
      throw UnknowUserID()
    } catch (e: Exception) {
      Log.e(TAG, "Error updating account: ${e.message}", e)
      throw e
    }
  }

  override suspend fun deleteProfilePicture(userID: String) {
    try {
      if (userID.isBlank()) throw BlankUserID()
      getAccount(userID)

      try {
        storage.reference.child("profile_pictures/$userID.jpg").delete().await()
      } catch (e: Exception) {
        Log.w(TAG, "Could not delete profile picture from storage (may not exist): ${e.message}")
      }

      db.collection(ACCOUNT_COLLECTION_PATH)
          .document(userID)
          .update(mapOf("profilePicture" to ""))
          .await()
    } catch (e: NoSuchElementException) {
      Log.e(TAG, "User with userID $userID not found", e)
      throw UnknowUserID()
    } catch (e: Exception) {
      Log.e(TAG, "Error deleting picture : ${e.message}", e)
      throw e
    }
  }

  override suspend fun getItemsList(userID: String): List<String> {
    return try {
      val document = db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get().await()

      if (!document.exists()) {
        Log.w(TAG, "Account not found for items list")
        emptyList()
      } else {
        @Suppress("UNCHECKED_CAST")
        (document["itemsUids"] as? List<String>) ?: emptyList()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting items list for $userID: ${e.message}", e)
      emptyList()
    }
  }

  override suspend fun addItem(itemUid: String): Boolean {
    return try {
      val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception(USER_NOT_LOGGED)
      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(currentUserId)

      withTimeout(TIMEOUT) { userRef.update("itemsUids", FieldValue.arrayUnion(itemUid)).await() }
      true
    } catch (_: TimeoutCancellationException) {
      Log.w(TAG, "Account item add timed out (offline), queued.")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Error adding item: ${e.message}", e)
      false
    }
  }

  override suspend fun removeItem(itemUid: String): Boolean {
    return try {
      val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception(USER_NOT_LOGGED)
      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(currentUserId)

      withTimeout(TIMEOUT) { userRef.update("itemsUids", FieldValue.arrayRemove(itemUid)).await() }
      Log.d("AccountRepositoryFirestore", "Item removed from Firestore")
      true
    } catch (_: TimeoutCancellationException) {
      Log.w("AccountRepositoryFirestore", "Account item remove timed out (offline), queued.")
      true
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error removing item: ${e.message}", e)
      false
    }
  }

  override suspend fun getStarredItems(userID: String): List<String> {
    return try {
      val document = db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get().await()
      if (document.exists()) {
        @Suppress("UNCHECKED_CAST")
        (document["starredItemUids"] as? List<String>) ?: emptyList()
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting starred items for $userID: ${e.message}", e)
      emptyList()
    }
  }

  override suspend fun refreshStarredItems(userID: String): List<String> {
    return try {
      // Force fetch from server
      val document =
          db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get(Source.SERVER).await()
      if (document.exists()) {
        @Suppress("UNCHECKED_CAST")
        (document["starredItemUids"] as? List<String>) ?: emptyList()
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing starred items for $userID: ${e.message}", e)
      // Fallback to default behavior (maybe cache or empty)
      getStarredItems(userID)
    }
  }

  override suspend fun addStarredItem(itemUid: String): Boolean {
    return try {
      val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception(USER_NOT_LOGGED)
      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(currentUserId)
      withTimeout(TIMEOUT) {
        userRef.update("starredItemUids", FieldValue.arrayUnion(itemUid)).await()
      }
      true
    } catch (_: TimeoutCancellationException) {
      Log.w(TAG, "Starred item add timed out (offline), queued.")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Error adding starred item: ${e.message}", e)
      false
    }
  }

  override suspend fun removeStarredItem(itemUid: String): Boolean {
    return try {
      val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception(USER_NOT_LOGGED)
      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(currentUserId)
      withTimeout(TIMEOUT) {
        userRef.update("starredItemUids", FieldValue.arrayRemove(itemUid)).await()
      }
      true
    } catch (_: TimeoutCancellationException) {
      Log.w(TAG, "Starred item remove timed out (offline), queued.")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Error removing starred item: ${e.message}", e)
      false
    }
  }

  override suspend fun toggleStarredItem(itemUid: String): List<String> {
    val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception(USER_NOT_LOGGED)
    val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(currentUserId)

    // We need to know if it's starred to toggle.
    // In offline mode, we rely on the cached document.
    // We use a short timeout for the read
    val document =
        try {
          withTimeout(1000L) { userRef.get().await() }
        } catch (e: Exception) {
          // If read fails/times out, we can't toggle reliably without cache.
          // But we can try to read from cache specifically
          try {
            userRef.get(Source.CACHE).await()
          } catch (_: Exception) {
            throw e // Give up if neither works
          }
        }

    @Suppress("UNCHECKED_CAST")
    val currentList = (document["starredItemUids"] as? List<String>) ?: emptyList()
    val isStarred = currentList.contains(itemUid)

    val operation =
        if (isStarred) FieldValue.arrayRemove(itemUid) else FieldValue.arrayUnion(itemUid)

    try {
      withTimeout(TIMEOUT) { userRef.update("starredItemUids", operation).await() }
    } catch (_: TimeoutCancellationException) {
      Log.w(TAG, "Starred item toggle timed out (offline), queued.")
    }

    // Return the updated list (optimistically calculated)
    return if (isStarred) currentList - itemUid else currentList + itemUid
  }

  private suspend fun userExists(user: User): Boolean {
    if (user.username.isBlank()) return false

    return try {
      val querySnapshot =
          db.collection(USER_COLLECTION_PATH).whereEqualTo("username", user.username).get().await()
      // If any document exists with a different id, the username is taken.
      querySnapshot.documents.any { it.id != user.uid }
    } catch (e: Exception) {
      Log.e(TAG, "Error checking user existence: ${e.message}", e)
      throw e
    }
  }

  /**
   * Deletes all posts owned by the specified user, including their documents and associated images.
   *
   * @param userID The ID of the user whose posts should be deleted
   */
  private suspend fun deleteUserPosts(userID: String) {
    try {
      val postsQuery = db.collection(POSTS_COLLECTION).whereEqualTo("ownerId", userID).get().await()
      for (doc in postsQuery.documents) {
        val postId = doc.id
        // Delete Firestore doc
        try {
          outfitPostRepository.deletePost(postId)
        } catch (e: Exception) {
          Log.w(TAG, "Error querying posts for user deletion (continuing): ${e.message}")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error deleting user's posts ${e.message}")
    }
  }

  /**
   * Deletes all items owned by the specified user, including their documents and associated images.
   *
   * @param userID The ID of the user whose items should be deleted
   */
  private suspend fun deleteUserItems(userID: String) {
    try {
      val itemsQuery =
          db.collection(ITEMS_COLLECTION).whereEqualTo(OWNER_ATTRIBUTE_NAME, userID).get().await()
      for (doc in itemsQuery.documents) {
        val itemId = doc.id
        // Extract imageId if present to delete storage file
        val imageMap = doc["image"] as? Map<*, *>
        val rawImageId = imageMap?.get("imageId") as? String ?: ""
        val sanitizedImageId = ImageFilenameSanitizer.sanitize(rawImageId)
        // Delete Firestore doc
        try {
          doc.reference.delete().await()
        } catch (e: Exception) {
          Log.w(TAG, "Failed deleting item doc $itemId: ${e.message}")
        }
        // Delete associated image (if any id)
        if (sanitizedImageId.isNotBlank()) {
          try {
            storage.reference.child("$ITEMS_PATH/$sanitizedImageId.jpg").delete().await()
          } catch (e: Exception) {
            Log.w(TAG, "Failed deleting item image $sanitizedImageId.jpg: ${e.message}")
          }
        }
      }
      Log.d(TAG, "Deleted ${itemsQuery.size()} items for user $userID")
    } catch (e: Exception) {
      Log.w(TAG, "Error querying items for user deletion (continuing): ${e.message}")
    }
  }

  private suspend fun deleteUserFriends(acc: Account) {
    val friendList = acc.friendUids
    if (friendList.isEmpty()) return
    for (friend in friendList) {
      try {
        removeFriend(acc.ownerId, friend)
      } catch (_: Exception) {
        Log.w(TAG, "Failed to delete user friend : $friend")
      }
    }
  }

  override fun observeAccount(userID: String): Flow<Account> = callbackFlow {
    if (userID.isBlank()) {
      close(BlankUserID())
      return@callbackFlow
    }

    val listener =
        db.collection(ACCOUNT_COLLECTION_PATH).document(userID).addSnapshotListener {
            snapshot,
            error ->
          if (error != null) {
            Log.e(TAG, "Error observing account $userID", error)
            close(error)
            return@addSnapshotListener
          }

          if (snapshot != null && snapshot.exists()) {
            val account = transformAccountDocument(snapshot)
            if (account != null) {
              trySend(account)
            } else {
              Log.e(TAG, "Failed to transform account document for $userID")
            }
          } else {
            Log.w(TAG, "Account document $userID does not exist")
          }
        }

    awaitClose { listener.remove() }
  }

  override suspend fun getPublicLocations(): List<PublicLocation> {
    return try {
      val snapshot = db.collection(PUBLIC_LOCATIONS_COLLECTION_PATH).get().await()
      snapshot.documents.mapNotNull { doc ->
        try {
          doc.toPublicLocation()
        } catch (e: Exception) {
          null
        }
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  override fun observePublicLocations(): Flow<List<PublicLocation>> = callbackFlow {
    val listener =
        db.collection(PUBLIC_LOCATIONS_COLLECTION_PATH).addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }

          if (snapshot != null) {
            val publicLocations =
                snapshot.documents.mapNotNull { doc ->
                  try {
                    doc.toPublicLocation()
                  } catch (e: Exception) {
                    null
                  }
                }
            trySend(publicLocations)
          }
        }

    awaitClose { listener.remove() }
  }
}
