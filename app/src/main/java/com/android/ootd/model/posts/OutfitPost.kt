package com.android.ootd.model.posts

import androidx.annotation.Keep
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation

/**
 * Represents a single post in the feed
 *
 * @property postUID unique ID of the post
 * @property name name of the user who posted
 * @property ownerId UID of the user who created the post
 * @property userProfilePicURL URL of the user's profile picture
 * @property outfitURL uploaded outfit image
 * @property description Optional post caption
 * @property itemsID of the items the user is wearing
 * @property timestamp time passed since posting
 * @property location user's location
 */
@Keep
data class OutfitPost(
    val postUID: String = "",
    val name: String = "",
    val ownerId: String = "",
    val userProfilePicURL: String = "",
    val outfitURL: String = "",
    val description: String = "",
    val itemsID: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val location: Location = emptyLocation,
    val comments: List<Comment> = emptyList()
)

/**
 * Represents a comment on a post
 *
 * @property commentId unique ID of the comment
 * @property ownerId UID of the user who made the comment
 * @property text content of the comment
 * @property timestamp time passed since commenting
 * @property reactionImage optional reaction image URL
 */
@Keep
data class Comment(
    val commentId: String = "",
    val ownerId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val reactionImage: String = "",
)
