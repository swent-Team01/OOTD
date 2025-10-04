package com.android.ootd.model

import android.net.Uri

data class Item(
    val itemId: String,
    val image : Uri,
    val category: String,
    val type: String,
    val brand : String,
    val price: Double,
    val material: String,
    val link: String,
)