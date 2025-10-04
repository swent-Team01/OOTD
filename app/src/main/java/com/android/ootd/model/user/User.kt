package com.android.ootd.model.user

data class Friend(val uid: String, val name: String)

data class User(
    val uid: String,
    val name: String,
    val friendList: List<Friend> // Holds a friend by their uid and their name.
)
