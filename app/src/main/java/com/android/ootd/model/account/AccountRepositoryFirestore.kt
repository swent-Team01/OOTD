package com.android.ootd.model.account

import android.util.Log
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.isValidLocation
import com.android.ootd.model.user.BlankUserID
import com.android.ootd.model.user.USER_COLLECTION_PATH
import com.android.ootd.model.user.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

const val ACCOUNT_COLLECTION_PATH = "accounts"

// Custom exception for taken user scenario
class TakenUserException(message: String) : Exception(message)

class TakenAccountException(message: String) : Exception(message)

class UnknowUserID : Exception("No account with such userID")

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
        "location" to
            mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "name" to location.name),
        "itemsUids" to itemsUids)

/** Convert Firestore DocumentSnapshot to domain Account (uses document id as uid) */
private fun DocumentSnapshot.toAccount(): Account {
  val username = getString("username") ?: ""
  val birthday = getString("birthday") ?: ""
  val email = getString("googleAccountEmail") ?: ""
  val picture = getString("profilePicture") ?: ""
  val isPrivate = getBoolean("isPrivate") ?: false

  // Validate that friendUids is actually a List, throw if not
  val friendUidsRaw = get("friendUids")
  val friends =
      when {
        friendUidsRaw == null -> emptyList()
        friendUidsRaw is List<*> -> friendUidsRaw.filterIsInstance<String>()
        else ->
            throw IllegalArgumentException(
                "friendUids field is not a List but ${friendUidsRaw::class.simpleName}")
      }

  // Parse location if present, otherwise throw MissingLocationException
  val locationRaw = get("location")
  val location =
      when {
        locationRaw == null -> throw MissingLocationException()
        locationRaw is Map<*, *> -> {
          val lat = (locationRaw["latitude"] as? Number)?.toDouble() ?: 0.0
          val lon = (locationRaw["longitude"] as? Number)?.toDouble() ?: 0.0
          val name = locationRaw["name"] as? String ?: ""
          Location(lat, lon, name)
        }
        else -> throw MissingLocationException()
      }

  // Parse itemsUids if present
  val itemsUidsRaw = get("itemsUids")
  val itemsUids =
      when {
        itemsUidsRaw == null -> emptyList()
        itemsUidsRaw is List<*> -> itemsUidsRaw.filterIsInstance<String>()
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
      itemsUids = itemsUids)
}

class AccountRepositoryFirestore(private val db: FirebaseFirestore) : AccountRepository {

  // In-memory cache for items list - updated optimistically for offline support
  private val itemsListCache = mutableMapOf<String, MutableList<String>>()

  companion object {

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
      location: Location
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
            location = location)
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
        Log.e(TAG, "The user with id ${friendID} not found")
        throw NoSuchElementException("The user with id ${friendID} not found")
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
    db.collection(ACCOUNT_COLLECTION_PATH)
        .document(userID)
        .update("isPrivate", newPrivacySetting)
        .await()

    return newPrivacySetting
  }

  override suspend fun deleteAccount(userID: String) {
    try {
      if (userID.isBlank()) throw BlankUserID()
      getAccount(userID)

      db.collection(ACCOUNT_COLLECTION_PATH).document(userID).delete().await()
      Log.d(TAG, "Successfully deleted account with UID: $userID")
    } catch (_: NoSuchElementException) {
      throw UnknowUserID()
    } catch (e: Exception) {
      Log.e(TAG, "Error deleting account: ${e.message}", e)
      throw e
    }
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
                  "location" to
                      mapOf(
                          "latitude" to newLocation.latitude,
                          "longitude" to newLocation.longitude,
                          "name" to newLocation.name)))
          .await()

      Log.d(
          TAG,
          "Successfully updated account with UID: $userID, new username $newUsername, " +
              "birthdate $newBirthDate, profilePic $newProfilePic, location $newLocation")
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
      db.collection(ACCOUNT_COLLECTION_PATH).document(userID).update(mapOf("profilePicture" to ""))
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
      // First check in-memory cache (for offline optimistic updates)
      if (itemsListCache.containsKey(userID)) {
        Log.d(TAG, "Returning items list from memory cache")
        return itemsListCache[userID]?.toList() ?: emptyList()
      }

      // Try to read from Firestore cache first (offline-first pattern)
      val document =
          try {
            db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get(Source.CACHE).await()
          } catch (e: Exception) {
            Log.w(TAG, "Cache read failed, trying default source: ${e.message}")
            // If cache fails, try with default source (network or cache) with timeout
            kotlinx.coroutines.withTimeoutOrNull(2_000L) {
              db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get().await()
            }
          }

      if (document == null || !document.exists()) {
        Log.w(
            TAG,
            "Account not found in cache or network for items list, returning cached or empty list")
        return itemsListCache[userID]?.toList() ?: emptyList()
      } else {
        // Extract itemsUids directly from document
        @Suppress("UNCHECKED_CAST")
        val itemsList = (document.get("itemsUids") as? List<String>) ?: emptyList()
        // Update memory cache with fetched data
        itemsListCache[userID] = itemsList.toMutableList()
        return itemsList
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting items list for $userID: ${e.message}", e)
      // Return cached list if available, otherwise empty
      return itemsListCache[userID]?.toList() ?: emptyList()
    }
  }

  override suspend fun addItem(itemUid: String): Boolean {
    return try {
      val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception("User not logged in")

      // Optimistically update memory cache immediately (synchronous)
      if (!itemsListCache.containsKey(currentUserId)) {
        // Initialize cache by fetching current list from Firestore
        val currentList =
            try {
              // Try to get from cache first with short timeout
              kotlinx.coroutines.withTimeoutOrNull(1_000L) {
                val doc =
                    db.collection(ACCOUNT_COLLECTION_PATH)
                        .document(currentUserId)
                        .get(Source.CACHE)
                        .await()
                @Suppress("UNCHECKED_CAST")
                (doc.get("itemsUids") as? List<String>) ?: emptyList()
              } ?: emptyList()
            } catch (e: Exception) {
              Log.w(
                  "AccountRepositoryFirestore",
                  "Could not fetch current items, starting with empty: ${e.message}")
              emptyList()
            }
        itemsListCache[currentUserId] = currentList.toMutableList()
        Log.d(
            "AccountRepositoryFirestore",
            "Initialized cache with ${currentList.size} existing items")
      }
      val itemsList = itemsListCache[currentUserId] ?: mutableListOf()
      if (!itemsList.contains(itemUid)) {
        itemsList.add(itemUid)
        Log.d("AccountRepositoryFirestore", "Added item to memory cache (total: ${itemsList.size})")
      }

      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(currentUserId)

      // Queue Firestore update with timeout (will sync when online)
      try {
        withTimeoutOrNull(2_000L) {
          userRef.update("itemsUids", FieldValue.arrayUnion(itemUid)).await()
        }
        Log.d("AccountRepositoryFirestore", "Item added to Firestore (or queued if offline)")
      } catch (e: Exception) {
        // Acceptable when offline - cache is already updated
        Log.w("AccountRepositoryFirestore", "Firestore update queued (offline): ${e.message}")
      }

      true // Cache is updated, that's what matters
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error adding item: ${e.message}", e)
      false
    }
  }

  override suspend fun removeItem(itemUid: String): Boolean {
    return try {
      val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception("User not logged in")

      // Optimistically update memory cache immediately
      if (itemsListCache.containsKey(currentUserId)) {
        itemsListCache[currentUserId]!!.remove(itemUid)
        Log.d("AccountRepositoryFirestore", "Removed item from memory cache")
      }

      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(currentUserId)

      // Queue Firestore update with timeout (will sync when online)
      try {
        withTimeoutOrNull(2_000L) {
          userRef.update("itemsUids", FieldValue.arrayRemove(itemUid)).await()
        }
        Log.d("AccountRepositoryFirestore", "Item removed from Firestore (or queued if offline)")
      } catch (e: Exception) {
        // Acceptable when offline - cache is already updated
        Log.w("AccountRepositoryFirestore", "Firestore update queued (offline): ${e.message}")
      }

      true // Cache is updated, that's what matters
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error removing item: ${e.message}", e)
      false
    }
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
}
