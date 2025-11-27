package com.android.ootd.model.notifications

import androidx.annotation.Keep

@Keep
data class Notification(
    val uid: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val type: String,
    val content: String,
    val wasPushed: Boolean = false
) {

  fun getNotificationMessage(): String {
    return when (type) {
      "FOLLOW_REQUEST" -> "$senderName wants to follow you"
      else -> "New notification available"
    }
  }
}
