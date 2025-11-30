package com.android.ootd.model.items

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ItemsRepositoryLocalTest {

  private lateinit var repository: ItemsRepositoryLocal
  private val baseItem =
      Item(
          itemUuid = "seed",
          postUuids = listOf("post"),
          image = ImageData("img", "https://example.com"),
          category = "clothing",
          type = "t-shirt",
          ownerId = "owner")

  @Before
  fun setup() {
    repository = ItemsRepositoryLocal()
  }

  @Test
  fun `getItemsByIdsAcrossOwners mirrors getItemsByIds`() = runBlocking {
    repository.addItem(baseItem.copy(itemUuid = "item-1"))
    repository.addItem(baseItem.copy(itemUuid = "item-2"))

    val results = repository.getItemsByIdsAcrossOwners(listOf("item-1", "missing"))

    assertEquals(listOf("item-1"), results.map { it.itemUuid })
  }

  @Test
  fun `deletePostItems removes only matching posts`() = runBlocking {
    repository.addItem(baseItem.copy(itemUuid = "keep", postUuids = listOf("keep")))
    repository.addItem(baseItem.copy(itemUuid = "delete", postUuids = listOf("remove")))

    repository.deletePostItems("remove")

    assertEquals(listOf("keep"), repository.getAllItems().map { it.itemUuid })
  }
}
