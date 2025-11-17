package com.android.ootd.model.items

import androidx.annotation.Keep

@Keep
data class Item(
    val itemUuid: String,
    val postUuids: List<String>,
    val image: ImageData,
    val category: String,
    val type: String?,
    val brand: String?,
    val price: Double?,
    val currency: String? = null,
    val material: List<Material?>,
    val link: String?,
    val ownerId: String
)

@Keep data class Material(val name: String = "", val percentage: Double = 0.0)

@Keep data class ImageData(val imageId: String, val imageUrl: String)
