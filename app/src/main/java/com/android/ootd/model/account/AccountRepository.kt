package com.android.ootd.model.account

import com.android.ootd.model.map.Location
import com.android.ootd.model.user.User

/** Repository interface for managing account-related operations. */
interface AccountRepository {

  /** Create an account for the given user. */
  suspend fun createAccount(
      user: User,
      userEmail: String = "",
      dateOfBirth: String,
      location: Location
  )

  /**
   * Add a new account.
   *
   * @throws IllegalArgumentException if account with the same UID already exists
   */
  suspend fun addAccount(account: Account)

  /**
   * Retrieve the account corresponding to the given user ID.
   *
   * @throws NoSuchElementException if account is not found
   */
  suspend fun getAccount(userID: String): Account

  /** Returns true if an account with [userID] exists and has a non-blank username. */
  suspend fun accountExists(userID: String): Boolean

  /**
   * Add [friendID] to the friend list of [userID].
   *
   * Return true if the operation added both users to each other's friend list.
   *
   * @throws NoSuchElementException if either user is not found
   */
  suspend fun addFriend(userID: String, friendID: String): Boolean

  /**
   * Remove [friendID] from the friend list of [userID].
   *
   * @throws NoSuchElementException if either user is not found
   */
  suspend fun removeFriend(userID: String, friendID: String)

  /**
   * Check whether [friendID] is in the friend list of [userID].
   *
   * @throws NoSuchElementException if user is not found
   * @throws IllegalStateException if multiple users have the same uid
   */
  suspend fun isMyFriend(userID: String, friendID: String): Boolean

  /**
   * Toggle the privacy setting of the account corresponding to [userID].
   *
   * @return the new privacy setting after toggling
   * @throws NoSuchElementException if account is not found
   */
  suspend fun togglePrivacy(userID: String): Boolean

  /**
   * Allows the User to delete his account
   *
   * @param userID Said users ID
   * @throws NoSuchElementException If the account does not exist
   */
  suspend fun deleteAccount(userID: String)

  /**
   * Allows the user to edit his attributes, ie. username, date of birth, profile picture and
   * location
   *
   * @param userID Said users ID
   * @param username Users new username, blank by default
   * @param birthDay Users updated date of birth, blank by default
   * @param picture Users profile picture URL, blank by default
   * @param location Users updated location
   * @throws TakenUserException If the username already exists
   * @throws IllegalStateException The userID does not match the users ID
   */
  suspend fun editAccount(
      userID: String,
      username: String,
      birthDay: String,
      picture: String,
      location: Location
  )

  /**
   * Delete the profile picture of the account corresponding to [userID].
   *
   * Sets the profile picture to an empty string. If the account has no profile picture, this
   * operation completes successfully without any changes.
   *
   * @param userID The ID of the user whose profile picture should be deleted
   * @throws NoSuchElementException if account is not found
   */
  suspend fun deleteProfilePicture(userID: String)

  /**
   * Allow the user to retrieve his items list for his inventory
   *
   * @param userID Said user ID
   * @return The said list of item of the user
   */
  suspend fun getItemsList(userID: String): List<String>

  /**
   * Allow the user to add a new item from his inventory
   *
   * @param itemUid Uid of the item he wants to add to his inventory
   * @return True if the item was added successfully, false otherwise
   */
  suspend fun addItem(itemUid: String): Boolean

  /**
   * Allow the user to remove an item from his inventory
   *
   * @param itemUid Uid of the item he wants to remove from his inventory
   * @return True if the item was removed successfully, false otherwise
   */
  suspend fun removeItem(itemUid: String): Boolean
}
