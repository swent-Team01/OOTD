package com.android.ootd.model.posts

import androidx.annotation.Keep

/**
 * Represents a like on a post
 *
 * @property postId ID of the liked post
 * @property postLikerId UID of the user who liked the post
 * @property timestamp time passed since liking
 */
@Keep
data class Like(val postId: String = "", val postLikerId: String = "", val timestamp: Long = 0L)
