package com.android.ootd.utils

import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost

/**
 * Base test class for tests that need to work with both accounts and posts.
 *
 * Provides helper methods for creating test posts with sensible defaults, useful for integration
 * tests involving feeds, maps, and user interactions.
 *
 * Disclaimer: This test helper was written with the assistance of AI
 */
open class PostFirestoreTest : AccountFirestoreTest() {

  /**
   * Creates a test outfit post with the given parameters.
   *
   * @param postUID Unique identifier for the post
   * @param name Username of the post creator
   * @param ownerId User ID of the post owner (defaults to currentUser.uid)
   * @param profilePicURL URL of the user's profile picture
   * @param outfitURL URL of the outfit image
   * @param description Post description
   * @param location Geographic location of the post
   * @return OutfitPost instance
   */
  fun createTestPost(
      postUID: String,
      name: String,
      ownerId: String = currentUser.uid,
      profilePicURL: String = "",
      outfitURL: String = "https://example.com/outfit.jpg",
      description: String = "",
      location: Location = LAUSANNE_LOCATION
  ): OutfitPost {
    return OutfitPost(
        postUID = postUID,
        name = name,
        ownerId = ownerId,
        userProfilePicURL = profilePicURL,
        outfitURL = outfitURL,
        description = description,
        itemsID = emptyList(),
        timestamp = System.currentTimeMillis(),
        location = location)
  }

  /**
   * Creates multiple test posts at nearby locations for testing clustering.
   *
   * @param count Number of posts to create
   * @param baseLocation Base location for the posts (default: Lausanne)
   * @param offset Geographic offset between posts in degrees (default: 0.0001)
   * @return List of OutfitPost instances
   */
  fun createTestPostsForClustering(
      count: Int,
      baseLocation: Location = LAUSANNE_LOCATION,
      offset: Double = 0.0001
  ): List<OutfitPost> {
    return (1..count).map { i ->
      createTestPost(
          postUID = "cluster-post-$i",
          name = "User$i",
          profilePicURL = if (i % 2 == 0) "https://example.com/pic$i.jpg" else "",
          outfitURL = "https://example.com/outfit$i.jpg",
          description = "Post $i",
          location =
              Location(
                  baseLocation.latitude + (i * offset),
                  baseLocation.longitude + (i * offset),
                  "Location $i"))
    }
  }
}
