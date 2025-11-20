package com.android.ootd.model.posts

interface LikesRepository {
  /**
   * Adds a like
   *
   * @param like The Like object containing details of the like action
   */
  suspend fun likePost(like: Like)

  /**
   * Removes a like
   *
   * @param postId The ID of the post to unlike
   * @param likerUserId The ID of the user who is unliking the post
   */
  suspend fun unlikePost(postId: String, likerUserId: String)

  /**
   * Checks if a user has liked a specific post
   *
   * @param postId The ID of the post
   * @param userId The ID of the user
   * @return True if the user has liked the post, false otherwise
   */
  suspend fun isPostLikedByUser(postId: String, userId: String): Boolean

  /**
   * Retrieves all likes for a specific post
   *
   * @param postId The ID of the post
   * @return A list of Like objects representing all likes for the post
   */
  suspend fun getLikesForPost(postId: String): List<Like>

  /**
   * Gets the total like count for a specific post
   *
   * @param postId The ID of the post
   * @return The total number of likes for the post
   */
  suspend fun getLikeCount(postId: String): Int
}
