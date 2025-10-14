package com.android.ootd.model.user

data class Friend(val uid: String = "", val name: String = "")

data class User(
    val uid: String = "",
    val name: String = "",
    val friendList: List<Friend> = emptyList(), // Holds a friend by their uid and their name.
    val friendUids: List<String> = emptyList() // UIDs used by security rules for mutual checks
)
