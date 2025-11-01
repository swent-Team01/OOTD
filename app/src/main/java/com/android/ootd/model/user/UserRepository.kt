package com.android.ootd.model.user

/** Represents a repository that manages User information. */
interface UserRepository {

  /** Generates and returns a new unique identifier for a User. */
  fun getNewUid(): String

  /**
   * Creates a new user with the username he chose
   *
   * @param username The chosen username
   * @param uid the user's ID
   * @param profilePicture the URL path to the user's profile picture. Blank by default
   */
  suspend fun createUser(username: String, uid: String, profilePicture: String = "")

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
   * Edits the user in the database.
   *
   * @param userID The user's ID
   * @param newUsername The new username to set
   * @param profilePicture The users profile picture
   * @throws IllegalArgumentException if userID is blank
   * @throws NoSuchElementException if the user with the given ID is not found
   * @throws TakenUsernameException if the new username is already taken by another user
   * @throws Exception for other Firestore-related errors
   */
  suspend fun editUser(userID: String, newUsername: String = "", profilePicture: String = "")

  /**
   * Deletes the user from the database.
   *
   * @param userID The user's ID
   * @throws IllegalArgumentException if userID is blank
   * @throws NoSuchElementException if the user with the given ID is not found
   * @throws Exception for other Firestore-related errors
   */
  suspend fun deleteUser(userID: String)
}
