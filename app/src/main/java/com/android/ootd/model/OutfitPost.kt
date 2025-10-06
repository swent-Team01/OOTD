package com.android.ootd.model

/**
 * Represents a single post in the feed
 *
 * @property postUID unique ID of the post
 * @property name name of the user who posted
 * @property uid UID of the user who created the post
 * @property userProfilePicURL URL of the user's profile picture
 * @property outfitURL uploaded outfit image
 * @property description Optional post caption
 * @property itemsID of the items the user is wearing
 * @property timestamp time passed since posting
 */
data class OutfitPost(
    val postUID: String,
    val name: String,
    val uid: String,
    val userProfilePicURL: String, // firebase url of the user's profile picture
    val outfitURL: String,
    val description: String,
    val itemsID: List<String>,
    val timestamp: Long
    // val reactionList - for when we implement reactions/commentaries to posts
    // val location - for when we implement location
)
