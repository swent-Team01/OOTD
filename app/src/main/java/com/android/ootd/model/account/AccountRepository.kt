package com.android.ootd.model.account

import com.android.ootd.model.user.User

/** Repository interface for managing account-related operations. */
interface AccountRepository {

  /** Create an account for the given user. */
  suspend fun createAccount(user: User, dateOfBirth: String)

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
  suspend fun getAccount(userId: String): Account

  /** Returns true if an account with [userId] exists and has a non-blank username. */
  suspend fun accountExists(userId: String): Boolean

  /**
   * Add [friendID] to the friend list of [userID].
   *
   * @throws NoSuchElementException if either user is not found
   */
  suspend fun addFriend(userID: String, friendID: String)

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
}
