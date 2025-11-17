package com.android.ootd.model.reactions

import androidx.annotation.Keep

/**
 * Represents a reaction to a post
 *
 * @property ownerId UID of the user who created the reaction
 * @property postUID unique ID of the post being reacted to
 * @property reactionURL URL of the reaction image
 */
@Keep
data class Reaction(
    val ownerId: String = "",
    val postUID: String = "",
    val reactionURL: String = "" // photo of the reaction
)
