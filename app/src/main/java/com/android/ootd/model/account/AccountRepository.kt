package com.android.ootd.model.account

import com.android.ootd.model.user.User

/** Repository interface for managing account-related operations. */
interface AccountRepository {

  /** Create an account for the given user. */
  suspend fun createAccount(user: User)

  /**
   * Retrieves all accounts.
   *
   * @return list of accounts
   */
  suspend fun getAllAccounts(): List<Account>

  /** Add a new account. */
  suspend fun addAccount(account: Account)

  /** Retrieve the account corresponding to the given user. */
  suspend fun getAccount(user: User): Account

  /**
   * Returns true if an account with [userID] exists and has a non-blank username.
   *
   * @throws Exception if the user is not found
   */
  suspend fun accountExists(userID: String): Boolean

  /**
   * Add [friendID] to the friend list of [userID].
   *
   * @throws Exception if either user is not found
   */
  suspend fun addFriend(userID: String, friendID: String)

  /**
   * Remove [friendID] from the friend list of [userID].
   *
   * @throws Exception if either user is not found
   */
  suspend fun removeFriend(userID: String, friendID: String)
  /**
   * Check whether [friendID] is in the friend list of [userID].
   *
   * @throws Exception if friendID is not found or user is not authenticated
   */
  suspend fun isMyFriend(userID: String, friendID: String): Boolean
}
