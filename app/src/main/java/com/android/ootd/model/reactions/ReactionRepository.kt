package com.android.ootd.model.reactions

interface ReactionRepository {
  /**
   * Uploads a reaction image to storage for a specific post and user.
   *
   * @param postId The unique ID of the post.
   * @param userId The UID of the user creating the reaction.
   * @param localPath Local file path or URI string of the reaction image.
   * @return Download URL of the uploaded reaction image.
   */
  suspend fun uploadReactionImage(postId: String, userId: String, localPath: String): String

  /**
   * Deletes a reaction for a specific post and user.
   *
   * @param postId The unique ID of the post.
   * @param userId The UID of the user whose reaction is to be deleted.
   */
  suspend fun deleteReaction(postId: String, userId: String)

  /**
   * Adds or replaces a reaction for a specific post and user.
   *
   * @param postId The unique ID of the post.
   * @param userId The UID of the user creating or replacing the reaction.
   * @param localImagePath Local file path or URI string of the reaction image.
   */
  suspend fun addOrReplaceReaction(postId: String, userId: String, localImagePath: String)

  /**
   * Retrieves the reaction of a specific user for a specific post.
   *
   * @param postId The unique ID of the post.
   * @param userId The UID of the user whose reaction is to be retrieved.
   * @return The Reaction object if found, or null otherwise.
   */
  suspend fun getUserReaction(postId: String, userId: String): Reaction?

  /**
   * Retrieves all reactions for a specific post.
   *
   * @param postId The unique ID of the post.
   * @return A list of Reaction objects associated with the post.
   */
  suspend fun getAllReactions(postId: String): List<Reaction>
}
