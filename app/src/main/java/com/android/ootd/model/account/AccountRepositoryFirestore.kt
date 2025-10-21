package com.android.ootd.model.account

import android.util.Log
import com.android.ootd.model.user.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val ACCOUNT_COLLECTION_PATH = "accounts"

// Custom exception for taken user scenario
class TakenUserException(message: String) : Exception(message)

/** Convert domain Account to Firestore-friendly Map (excludes uid as it's the document id) */
private fun Account.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "username" to username,
        "birthday" to birthday,
        "googleAccountEmail" to googleAccountEmail,
        "profilePicture" to profilePicture,
        "friendUids" to friendUids)

/** Convert Firestore DocumentSnapshot to domain Account (uses document id as uid) */
private fun DocumentSnapshot.toAccount(): Account {
  val username = getString("username") ?: ""
  val birthday = getString("birthday") ?: ""
  val email = getString("googleAccountEmail") ?: ""
  val picture = getString("profilePicture") ?: ""
  val friends = (get("friendUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
  val uid = id // document id is the user id

  return Account(
      uid = uid,
      ownerId = uid,
      username = username,
      birthday = birthday,
      googleAccountEmail = email,
      profilePicture = picture,
      friendUids = friends)
}

class AccountRepositoryFirestore(private val db: FirebaseFirestore) : AccountRepository {

  /** Helper method to check account data as firestore might add the default values */
  private fun checkAccountData(account: Account): Account? {
    if (account.uid.isBlank()) {
      Log.e("AccountRepositoryFirestore", "Invalid account data: uid is blank")
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
      Log.e(
          "AccountRepositoryFirestore",
          "Error transforming document ${document.id}: ${e.message}",
          e)
      return null
    }
  }

  override suspend fun createAccount(user: User) {
    if (userExists(user)) {
      Log.e("AccountRepositoryFirestore", "Username already in use")
      throw TakenUserException("Username already in use")
    }

    val newAccount = Account(uid = user.uid, ownerId = user.uid, username = user.username)
    try {
      addAccount(newAccount)
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error while creating account: ${e.message}", e)
      throw e
    }
  }

  override suspend fun getAllAccounts(): List<Account> {
    return try {
      val querySnapshot = db.collection(ACCOUNT_COLLECTION_PATH).get().await()
      querySnapshot.documents.mapNotNull { document -> transformAccountDocument(document) }
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error getting accounts: ${e.message}", e)
      throw e
    }
  }

  override suspend fun getAccount(userId: String): Account {
    return try {
      val document = db.collection(ACCOUNT_COLLECTION_PATH).document(userId).get().await()

      if (!document.exists()) {
        throw NoSuchElementException("Account with ID $userId not found")
      }

      transformAccountDocument(document)
          ?: throw IllegalStateException("Failed to transform document with ID $userId")
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error getting account $userId: ${e.message}", e)
      throw e
    }
  }

  override suspend fun accountExists(userId: String): Boolean {
    return try {
      val document = db.collection(ACCOUNT_COLLECTION_PATH).document(userId).get().await()

      if (!document.exists()) {
        false
      } else {
        val username = document.getString("username")
        !username.isNullOrBlank()
      }
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error checking account existence: ${e.message}", e)
      throw e
    }
  }

  override suspend fun addAccount(account: Account) {
    try {
      val existingDoc = db.collection(ACCOUNT_COLLECTION_PATH).document(account.uid).get().await()

      if (existingDoc.exists()) {
        throw IllegalArgumentException("Account with UID ${account.uid} already exists")
      }

      db.collection(ACCOUNT_COLLECTION_PATH)
          .document(account.uid)
          .set(account.toFirestoreMap())
          .await()

      Log.d("AccountRepositoryFirestore", "Successfully added account with UID: ${account.uid}")
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error adding account: ${e.message}", e)
      throw e
    }
  }

  override suspend fun addFriend(userID: String, friendID: String) {
    try {
      val friendDocument = db.collection(ACCOUNT_COLLECTION_PATH).document(friendID).get().await()

      if (!friendDocument.exists()) {
        throw NoSuchElementException("Friend with ID $friendID not found")
      }

      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(userID)

      // With arrayUnion there can be no duplicates
      // https://firebase.google.com/docs/firestore/manage-data/add-data , Update elements in an
      // array section

      userRef.update("friendUids", FieldValue.arrayUnion(friendID)).await()
    } catch (e: Exception) {
      Log.e(
          "AccountRepositoryFirestore", "Error adding friend $friendID to $userID: ${e.message}", e)
      throw e
    }
  }

  override suspend fun removeFriend(userID: String, friendID: String) {
    try {
      val friendDocument = db.collection(ACCOUNT_COLLECTION_PATH).document(friendID).get().await()

      if (!friendDocument.exists()) {
        throw NoSuchElementException("Friend with ID $friendID not found")
      }

      val userRef = db.collection(ACCOUNT_COLLECTION_PATH).document(userID)
      userRef.update("friendUids", FieldValue.arrayRemove(friendID)).await()
    } catch (e: Exception) {
      Log.e(
          "AccountRepositoryFirestore",
          "Error removing friend $friendID from $userID: ${e.message}",
          e)
      throw e
    }
  }

  override suspend fun isMyFriend(userID: String, friendID: String): Boolean {
    val document = db.collection(ACCOUNT_COLLECTION_PATH).document(userID).get().await()

    if (!document.exists()) {
      throw NoSuchElementException("The authenticated user has not been added to the database")
    }

    val myAccount = transformAccountDocument(document)

    return (myAccount?.friendUids?.isNotEmpty() == true &&
        myAccount.friendUids.any { it == friendID })
  }

  private suspend fun userExists(user: User): Boolean {
    if (user.username.isBlank()) return false

    return try {
      val querySnapshot =
          db.collection(ACCOUNT_COLLECTION_PATH)
              .whereEqualTo("username", user.username)
              .get()
              .await()
      // If any document exists with a different id, the username is taken.
      querySnapshot.documents.any { it.id != user.uid }
    } catch (e: Exception) {
      Log.e("AccountRepositoryFirestore", "Error checking user existence: ${e.message}", e)
      throw e
    }
  }
}
