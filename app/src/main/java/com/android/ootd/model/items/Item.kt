package com.android.ootd.model.items

import android.net.Uri

data class Item(
    val uuid: String,
    val image: Uri,
    val category: String,
    val type: String?,
    val brand: String?,
    val price: Double?,
    val material: List<Material?>,
    val link: String?,
)

data class Material(val name: String = "", val percentage: Double = 0.0)
