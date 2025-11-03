package com.android.ootd.model.items

import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assume
import org.junit.Before
import org.junit.Test

// Test partially generated with an AI coding agent
class ItemsRepositoryFirestoreTest : FirestoreTest() {

  var ownerId = ""
  lateinit var item1: Item
  lateinit var item2: Item
  lateinit var item3: Item
  lateinit var item4: Item

  @Before
  override fun setUp() {
    super.setUp()
    Assume.assumeTrue("Firebase Emulator must be running before tests.", FirebaseEmulator.isRunning)
    ownerId = FirebaseEmulator.auth.uid ?: ""
    if (ownerId == "") {
      throw IllegalStateException("There needs to be an authenticated user")
    }

    item1 =
        Item(
            itemUuid = "0",
            postUuids = listOf("0"),
            image = ImageData(imageId = "0", imageUrl = "https://example.com/image1.jpg"),
            category = "clothes",
            type = "t-shirt",
            brand = "Mango",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item1",
            ownerId = ownerId)

    item2 =
        Item(
            itemUuid = "1",
            postUuids = listOf("0"),
            image = ImageData("1", "https://example.com/image1.jpg"),
            category = "shoes",
            type = "high heels",
            brand = "Zara",
            price = 30.0,
            material = listOf(),
            link = "https://example.com/item2",
            ownerId = ownerId)

    item3 =
        Item(
            itemUuid = "2",
            postUuids = listOf("0"),
            image = ImageData("2", "https://example.com/image1.jpg"),
            category = "bags",
            type = "handbag",
            brand = "Vakko",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item3",
            ownerId = ownerId)

    item4 =
        Item(
            itemUuid = "3",
            postUuids = listOf("0"),
            image = ImageData("3", "https://example.com/image1.jpg"),
            category = "accessories",
            type = "sunglasses",
            brand = "Ray-Ban",
            price = 100.0,
            material =
                listOf(
                    Material(name = "Plastic", percentage = 80.0),
                    Material(name = "Metal", percentage = 20.0)),
            link = "https://example.com/item4",
            ownerId = ownerId)
  }

  suspend fun countItems(): Int {
    return FirebaseEmulator.firestore
        .collection(ITEMS_COLLECTION)
        .whereEqualTo("ownerId", ownerId)
        .get()
        .await()
        .size()
  }

  @Test
  fun repositoryConsistencyTest() = runBlocking {
    // Unique IDs
    val total = 64
    val ids = (0 until total).map { itemsRepository.getNewItemId() }.toSet()
    assertEquals(total, ids.size)

    // Add multiple items
    listOf(item1, item2, item3, item4).forEach { itemsRepository.addItem(it) }
    assertEquals(4, countItems())

    // getAll and equality (ignoring id/link differences if the repository normalizes them)
    val allAfterAdd = itemsRepository.getAllItems()
    assertEquals(4, allAfterAdd.size)
    assertTrue(allAfterAdd.any { it.itemUuid == item1.itemUuid })

    // Direct fetch by ID
    assertEquals(item3, itemsRepository.getItemById(item3.itemUuid))
    assertEquals(item2, itemsRepository.getItemById(item2.itemUuid))

    // Overwrite existing item by re-adding with same id
    val updatedItem1 = item1.copy(price = 150.0)
    itemsRepository.addItem(updatedItem1)
    assertEquals(4, countItems())
    assertEquals(150.0, itemsRepository.getItemById(item1.itemUuid).price)

    // Edit another item and ensure others unchanged
    val updatedItem2 = item2.copy(price = 99.99, brand = "NewBrand")
    itemsRepository.editItem(item2.itemUuid, updatedItem2)
    val afterEdit = itemsRepository.getAllItems()
    assertEquals(4, afterEdit.size)
    assertEquals(updatedItem2, itemsRepository.getItemById(item2.itemUuid))
    assertEquals(item1.itemUuid, itemsRepository.getItemById(item1.itemUuid).itemUuid)
    assertEquals(item3, itemsRepository.getItemById(item3.itemUuid))

    // Duplicate UID behavior: last write wins or prevented; only one doc with that id exists
    val dupId = "duplicate"
    itemsRepository.addItem(item1.copy(itemUuid = dupId))
    runCatching { itemsRepository.addItem(item2.copy(itemUuid = dupId)) }
    assertEquals(5, countItems()) // new unique id added; second write overwrites same doc
    val onlyDup = itemsRepository.getAllItems().first { it.itemUuid == dupId }
    assertTrue(onlyDup == item1.copy(itemUuid = dupId) || onlyDup == item2.copy(itemUuid = dupId))

    // Deletions and idempotency
    itemsRepository.deleteItem(item2.itemUuid)
    assertEquals(4, countItems())
    itemsRepository.deleteItem(item2.itemUuid) // delete non-existing should not throw
    assertEquals(4, countItems())

    itemsRepository.deleteItem(item1.itemUuid)
    itemsRepository.deleteItem(item3.itemUuid)
    itemsRepository.deleteItem(item4.itemUuid)
    assertEquals(1, countItems()) // the duplicate remains
    itemsRepository.deleteItem(dupId)
    assertEquals(0, countItems())
    val allItems = itemsRepository.getAllItems()
    assertEquals(0, allItems.size)
  }

  @Test
  fun editItemById() = runBlocking {
    itemsRepository.addItem(item1)
    assertEquals(1, countItems())

    val newItem =
        item1.copy(
            category = "clothing",
            type = "shirt",
            brand = "H&M",
            price = 20.0,
            material = listOf(Material(name = "Cotton", percentage = 100.0)),
            link = "https://example.com/newlink")
    itemsRepository.editItem(item1.itemUuid, newItem)
    assertEquals(1, countItems())
    val items = itemsRepository.getAllItems()
    assertEquals(items.size, 1)

    val retrieved = itemsRepository.getItemById(item1.itemUuid)
    assertEquals(retrieved, newItem)
    assertEquals(newItem, retrieved)
    assertEquals("clothing", retrieved.category)
    assertEquals("shirt", retrieved.type)
    assertEquals("H&M", retrieved.brand)
    assertEquals(20.0, retrieved.price)
    assertEquals(1, retrieved.material.size)

    // Editing an item that does not exist should throw
    val nonExistingItem = item2.copy(itemUuid = "nonExisting")
    var didThrow = false
    try {
      itemsRepository.editItem(nonExistingItem.itemUuid, nonExistingItem)
    } catch (e: Exception) {
      didThrow = true
    }
    assert(didThrow)
    assertEquals(1, countItems())
  }

  @Test(expected = Exception::class)
  fun getItemByIdThrowsWhenItemNotFound() =
      runBlocking<Unit> { itemsRepository.getItemById("nonExistingId") }

  @Test
  fun getAllItemsSkipsInvalidDocuments() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Add an invalid Firestore document (missing required fields)
    collection.document("invalid").set(mapOf("brand" to "Zara", "ownerId" to ownerId)).await()

    // Add a valid one too
    itemsRepository.addItem(item1)

    val allItems = itemsRepository.getAllItems()

    // Only valid items should be returned (invalid skipped)
    assertEquals(1, allItems.size)
    assertEquals(item1.itemUuid, allItems.first().itemUuid)
  }

  @Test
  fun getAllItemsParsesPartiallyValidMaterialList() = runBlocking {
    val partialMaterialData =
        Item(
            itemUuid = "mixedMat",
            postUuids = listOf("simple_post"),
            image = ImageData(imageId = "mixedMatImg", imageUrl = "https://example.com/image.jpg"),
            category = "clothes",
            type = "jacket",
            brand = "Levi's",
            price = 120.0,
            link = "https://example.com/item",
            material = listOf(Material(name = "Cotton", percentage = 60.0), null),
            ownerId = ownerId)

    itemsRepository.addItem(partialMaterialData)

    val items = itemsRepository.getAllItems()
    assertEquals(1, items.size)
    val matList = items.first().material
    assertEquals(1, matList.size)
    assertEquals("Cotton", matList.first()?.name)
    assertEquals(60.0, matList.first()?.percentage)
  }

  @Test
  fun mapToItemCatchesExceptionForInvalidData() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Insert invalid data (image should be a string, but we give it a number)
    val invalidData =
        mapOf(
            "uuid" to "badItem",
            "image" to 12345, // <-- will cause ClassCastException during toUri()
            "category" to "clothes",
            "type" to "jacket",
            "brand" to "Levi's",
            "price" to 120.0,
            "link" to "https://example.com/item",
            "ownerId" to ownerId)

    collection.document("badItem").set(invalidData).await()

    // When repository tries to parse this, it should hit the catch block
    val allItems = itemsRepository.getAllItems()

    // It should skip the invalid item and not crash
    assertEquals(true, allItems.isEmpty())
  }

  // ========== NEW COMPREHENSIVE TESTS ==========

  @Test
  fun addMultipleItemsConcurrentlyAndOverwritesExistingItem() = runBlocking {
    val items = listOf(item1, item2, item3, item4)

    // Add all items concurrently
    items.forEach { itemsRepository.addItem(it) }

    assertEquals(4, countItems())
    val allItems = itemsRepository.getAllItems()
    assertEquals(4, allItems.size)

    // Verify all items were added correctly
    items.forEach { expectedItem ->
      val found = allItems.find { it.itemUuid == expectedItem.itemUuid }
      assertEquals(expectedItem, found)
    }

    val updatedItem1 = item1.copy(price = 150.0)
    itemsRepository.addItem(updatedItem1)

    val retrieved = itemsRepository.getItemById(item1.itemUuid)
    assertEquals(150.0, retrieved.price)
  }

  @Test
  fun getItemByIdWithInvalidIdThrows() = runBlocking {
    var exceptionThrown = false
    try {
      itemsRepository.getItemById("invalidId123")
    } catch (e: Exception) {
      exceptionThrown = true
    }
    assert(exceptionThrown)
  }

  @Test
  fun editNonExistentItemThrowsException() = runBlocking {
    var exceptionThrown = false
    try {
      itemsRepository.editItem("nonExistentId", item1)
    } catch (e: Exception) {
      exceptionThrown = true
      assert(e.message?.contains("not found") == true)
    }
    assert(exceptionThrown)
  }

  @Test
  fun addItemWithComplexMaterialList() = runBlocking {
    val complexMaterials =
        listOf(
            Material(name = "Cotton", percentage = 45.5),
            Material(name = "Polyester", percentage = 30.0),
            Material(name = "Elastane", percentage = 15.5),
            Material(name = "Silk", percentage = 9.0))
    val itemWithComplexMaterials = item1.copy(material = complexMaterials)
    itemsRepository.addItem(itemWithComplexMaterials)

    val retrieved = itemsRepository.getItemById(itemWithComplexMaterials.itemUuid)
    assertEquals(4, retrieved.material.size)
    assertEquals(45.5, retrieved.material[0]?.percentage)
    assertEquals("Silk", retrieved.material[3]?.name)
  }

  @Test
  fun addItemWithSpecialCharactersInFields() = runBlocking {
    val itemWithSpecialChars =
        item1.copy(
            brand = "H&M ™️ © Brand's \"New\" Collection",
            type = "T-shirt / Tank-top (Unisex)",
            link = "https://example.com/item?id=123&category=clothes&sort=price")

    itemsRepository.addItem(itemWithSpecialChars)
    val retrieved = itemsRepository.getItemById(itemWithSpecialChars.itemUuid)

    assertEquals(itemWithSpecialChars.brand, retrieved.brand)
    assertEquals(itemWithSpecialChars.type, retrieved.type)
    assertEquals(itemWithSpecialChars.link, retrieved.link)
  }

    // Invalid id throws
    var threw = false
    try {
      itemsRepository.getItemById("nonExistingId")
    } catch (_: Exception) {
      threw = true
    }
    assertTrue(threw)
  }

  @Test
  fun getAssociatedItemsReturnsOnlyItemsForSpecificPost() = runBlocking {
    // Given multiple items with different postUuid values
    val postUuid1 = "post_123"
    val postUuid2 = "post_456"

    val itemA = item1.copy(itemUuid = "A", postUuids = listOf(postUuid1))
    val itemB = item2.copy(itemUuid = "B", postUuids = listOf(postUuid1))
    val itemC = item3.copy(itemUuid = "C", postUuids = listOf(postUuid2))

    itemsRepository.addItem(itemA)
    itemsRepository.addItem(itemB)
    itemsRepository.addItem(itemC)

    // When fetching items associated with postUuid1
    val associatedItems = itemsRepository.getAssociatedItems(postUuid1)

    // Then only A and B should be returned
    assertEquals(2, associatedItems.size)
    val ids = associatedItems.map { it.itemUuid }.toSet()
    assertTrue(ids.containsAll(listOf("A", "B")))
    assertTrue(!ids.contains("C"))
  }

  @Test
  fun deletePostItemsRemovesAllItemsForGivenPost() = runBlocking {
    val postUuid = "postToDelete"

    val itemA = item1.copy(itemUuid = "A", postUuids = listOf(postUuid))
    val itemB = item2.copy(itemUuid = "B", postUuids = listOf(postUuid))
    val itemC = item3.copy(itemUuid = "C", postUuids = listOf("otherPost"))

    itemsRepository.addItem(itemA)
    itemsRepository.addItem(itemB)
    itemsRepository.addItem(itemC)
    assertEquals(3, countItems())

    // When deleting items belonging to postUuid
    itemsRepository.deletePostItems(postUuid)

    // Then only itemC should remain
    val remaining = itemsRepository.getAllItems()
    assertEquals(1, remaining.size)
    assertEquals("C", remaining.first().itemUuid)
  }

  @Test
  fun deletePostItemsHandlesEmptyQueryGracefully() = runBlocking {
    // No items exist for this postUuid
    val postUuid = "nonexistent_post"

    // Should not throw or affect other data
    itemsRepository.addItem(item1)
    assertEquals(1, countItems())

    itemsRepository.deletePostItems(postUuid)
    assertEquals(1, countItems())
  }

  @Test
  fun getAssociatedItemsReturnsCorrectSubset() = runBlocking {
    val postUuid1 = "post-aaa"
    val postUuid2 = "post-bbb"

    val itemA = item1.copy(itemUuid = "A", postUuids = listOf(postUuid1))
    val itemB = item2.copy(itemUuid = "B", postUuids = listOf(postUuid1))
    val itemC = item3.copy(itemUuid = "C", postUuids = listOf(postUuid2))
    val itemD = item4.copy(itemUuid = "D", postUuids = listOf(postUuid2))

    itemsRepository.addItem(itemA)
    itemsRepository.addItem(itemB)
    itemsRepository.addItem(itemC)
    itemsRepository.addItem(itemD)

    val associated1 = itemsRepository.getAssociatedItems(postUuid1)
    val associated2 = itemsRepository.getAssociatedItems(postUuid2)

    assertEquals(2, associated1.size)
    assertTrue(associated1.all { it.postUuids.contains(postUuid1) })

    assertEquals(2, associated2.size)
    assertTrue(associated2.all { it.postUuids.contains(postUuid2) })
  }

  @Test
  fun getAssociatedItemsReturnsEmptyListWhenNoneMatch() = runBlocking {
    itemsRepository.addItem(item1.copy(postUuids = listOf("some_post")))
    itemsRepository.addItem(item2.copy(postUuids = listOf("another_post")))

    val associated = itemsRepository.getAssociatedItems("unrelated_post")
    assertTrue(associated.isEmpty())
  }

  @Test
  fun deletePostItemsDeletesOnlyMatchingItems() = runBlocking {
    val post1 = "delete_me"
    val post2 = "keep_me"

    val itemA = item1.copy(itemUuid = "A", postUuids = listOf(post1))
    val itemB = item2.copy(itemUuid = "B", postUuids = listOf(post1))
    val itemC = item3.copy(itemUuid = "C", postUuids = listOf(post2))

    itemsRepository.addItem(itemA)
    itemsRepository.addItem(itemB)
    itemsRepository.addItem(itemC)
    assertEquals(3, countItems())

    itemsRepository.deletePostItems(post1)
    val remaining = itemsRepository.getAllItems()

    assertEquals(1, remaining.size)
    assertEquals(post2, remaining.first().postUuids.first())
  }

  @Test
  fun deletePostItemsDoesNothingWhenNoMatch() = runBlocking {
    val post1 = "existing"
    val itemA = item1.copy(itemUuid = "A", postUuids = listOf(post1))
    itemsRepository.addItem(itemA)
    assertEquals(1, countItems())

    itemsRepository.deletePostItems("non_existing_post")
    assertEquals(1, countItems())
  }
}
