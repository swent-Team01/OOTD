package com.android.ootd.model.notifications

data class Notification(
    val uid: String,
    val senderId: String,
    val receiverId: String,
    val type: String,
    val content: String
)
