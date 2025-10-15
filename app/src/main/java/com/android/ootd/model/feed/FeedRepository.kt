package com.android.ootd.model.feed

import com.android.ootd.model.OutfitPost

/** Repository that manages the posts on the feed */
interface FeedRepository {

  /**
   * Retrieves all posts in the feed
   *
   * @return A list of all posts.
   */
  suspend fun getFeed(): List<OutfitPost>

  /**
   * Retrieves posts authored by any of the provided user IDs. Implementations should query using
   * whereIn in chunks of 10 and sort by timestamp ascending.
   */
  suspend fun getFeedForUids(uids: List<String>): List<OutfitPost>

  /**
   * Indicates whether the user has posted or not in the current day
   *
   * @return false if hasn't posted, true if he has posted
   */
  suspend fun hasPostedToday(userId: String): Boolean
  // This should be in the user data, but I leave it here for the moment for testing the screen

  /**
   * Adds user's post to the feed
   *
   * @param post the user's post for the day
   */
  suspend fun addPost(post: OutfitPost)
  // I am really not sure if this function should be here

  /** Generates a unique post ID. */
  fun getNewPostId(): String
}
