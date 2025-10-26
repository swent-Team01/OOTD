package com.android.ootd.model.items

data class Item(
    val itemUuid: String,
    val image: ImageData,
    val category: String,
    val type: String?,
    val brand: String?,
    val price: Double?,
    val material: List<Material?>,
    val link: String?,
)

data class Material(val name: String = "", val percentage: Double = 0.0)

data class ImageData(val imageId: String, val imageUrl: String)
