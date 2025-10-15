package com.android.ootd.model.user

/** Represents a repository that manages User information. */
interface UserRepository {

  /** Generates and returns a new unique identifier for a User. */
  fun getNewUid(): String

  /**
   * Creates a new user with the username he chose
   *
   * @param username The chosen username
   */
  suspend fun createUser(username: String, uid: String)

  /**
   * Retrieves all Users from the repository.
   *
   * @return A list of all Users.
   */
  suspend fun getAllUsers(): List<User>

  /**
   * Adds a new User to the repository.
   *
   * @param user The user to add.
   */
  suspend fun addUser(user: User)

  /**
   * Retrieves a specific User by its unique identifier.
   *
   * @param userID The unique identifier of the User to retrieve.
   * @return The User with the specified identifier.
   * @throws Exception if the User is not found.
   */
  suspend fun getUser(userID: String): User

  /**
   * Asserts if a user has a username or not in the database
   *
   * @param userID The wanted user's ID
   * @return True if the user has a non empty/blank username
   * @throws Exception If the user is not found
   */
  suspend fun userExists(userID: String): Boolean

  /**
   * Adds the user with friendID in the friend list of userID.
   *
   * @param userID The unique identifier of the User which friend's list is updated.
   * @param friendID The unique identifier of the User which will be added to the friend list.
   * @param friendUsername The username associated to the User with friendID.
   * @throws Exception if userID or friendID is not found.
   */
  suspend fun addFriend(userID: String, friendID: String, friendUsername: String)

  /**
   * Removes the user with friendID from the friend list of userID.
   *
   * @param userID The unique identifier of the User which friend's list is updated.
   * @param friendID The unique identifier of the User which will be removed to the friend list.
   * @param friendUsername The username associated to the User with friendID.
   * @throws Exception if userID or friendID is not found
   */
  suspend fun removeFriend(userID: String, friendID: String, friendUsername: String)
  /**
   * Checks if for userID, the user with id friendID is his friend.
   *
   * @param userID The unique identifier of the user which friend list is checked.
   * @param friendID The unique identifier of the user looked for in the friend list.
   * @throws Exception if friendID is not found or user is not authenticated
   */
  suspend fun isMyFriend(userID: String, friendID: String): Boolean
}
