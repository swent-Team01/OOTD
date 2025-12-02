package com.android.ootd.model.items

import androidx.annotation.Keep

@Keep
data class Item(
    val itemUuid: String,
    val postUuids: List<String>,
    val image: ImageData,
    val category: String,
    val type: String? = null,
    val brand: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val material: List<Material?> = emptyList(),
    val link: String? = null,
    val ownerId: String,
    val condition: String? = null,
    val size: String? = null,
    val fitType: String? = null,
    val style: String? = null,
    val notes: String? = null,
    val isPublic: Boolean = false
)

@Keep data class Material(val name: String = "", val percentage: Double = 0.0)

@Keep data class ImageData(val imageId: String, val imageUrl: String)
