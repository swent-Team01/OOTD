package com.android.ootd.model.feed

import com.android.ootd.model.posts.OutfitPost
import kotlinx.coroutines.flow.Flow

/** Repository that manages the posts on the feed */
interface FeedRepository {

  /**
   * Retrieves posts authored by any of the provided user IDs. Implementations should query using
   * whereIn in chunks of 10 and sort by timestamp ascending.
   *
   * @param uids List of user IDs whose posts are to be retrieved
   */
  suspend fun getFeedForUids(uids: List<String>): List<OutfitPost>

  /**
   * Indicates whether the user has posted or not in the current day
   *
   * @return false if hasn't posted, true if he has posted
   */
  suspend fun hasPostedToday(userId: String): Boolean

  /**
   * Adds user's post to the feed
   *
   * @param post the user's post for the day
   */
  suspend fun addPost(post: OutfitPost)

  /**
   * Retrieves posts authored by any of the provided user IDs within the last 24 hours.
   *
   * @param uids List of user IDs whose recent posts are to be retrieved
   */
  suspend fun getRecentFeedForUids(uids: List<String>): List<OutfitPost>

  /** Generates a unique post ID. */
  fun getNewPostId(): String

  /**
   * Retrieves a specific post by its unique identifier.
   *
   * @param postUuid The unique identifier of the post to retrieve.
   * @return The outfit post with the specified identifier, or null if not found.
   */
  suspend fun getPostById(postUuid: String): OutfitPost?

  /**
   * Observes posts authored by any of the provided user IDs within the last 24 hours. Returns a
   * Flow that emits the updated list whenever the posts change in Firebase.
   *
   * @param uids List of user IDs whose recent posts are to be observed
   * @return Flow emitting lists of recent posts
   */
  fun observeRecentFeedForUids(uids: List<String>): Flow<List<OutfitPost>>

  /**
   * Retrieves the public feed posts.
   *
   * @return List of public posts
   */
  suspend fun getPublicFeed(): List<OutfitPost>
}
