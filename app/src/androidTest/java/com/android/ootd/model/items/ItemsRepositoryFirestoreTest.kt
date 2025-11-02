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
  fun idUniquenessAndCrudEndToEnd() = runBlocking {
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

    // Invalid id throws
    var threw = false
    try {
      itemsRepository.getItemById("nonExistingId")
    } catch (_: Exception) {
      threw = true
    }
    assertTrue(threw)
  }
}
