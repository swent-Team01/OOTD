package com.android.ootd.model.post

import android.net.Uri
import com.android.ootd.model.OutfitPost

interface OutfitPostRepository {
  fun getNewPostId(): String

  suspend fun uploadOutfitPhoto(localUri: Uri, postId: String): String

  suspend fun addPost(post: OutfitPost)

  suspend fun getPostById(postId: String): OutfitPost?

  suspend fun updatePostItems(postId: String, newItemIds: List<String>)

  suspend fun deletePost(postId: String)
}
