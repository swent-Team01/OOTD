package com.android.ootd.model.items

import android.net.Uri
import android.util.Log
import com.android.ootd.model.ITEMS_COLLECTION
import com.android.ootd.model.Item
import com.android.ootd.model.ItemsRepositoryProvider.repository
import com.android.ootd.model.Material
import com.android.ootd.utils.FirebaseEmulator
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ItemsRepositoryFirestoreTest() {

  @Before
  fun setup() {
    FirebaseEmulator
  }

  val item1 =
      Item(
          uuid = "0",
          image = Uri.parse("https://example.com/image1.jpg"),
          category = "clothes",
          type = "t-shirt",
          brand = "Mango",
          price = 0.0,
          material = listOf(),
          link = "https://example.com/item1")

  val item2 =
      Item(
          uuid = "1",
          image = Uri.parse("https://example.com/image1.jpg"),
          category = "shoes",
          type = "high heels",
          brand = "Zara",
          price = 30.0,
          material = listOf(),
          link = "https://example.com/item2")

  val item3 =
      Item(
          uuid = "2",
          image = Uri.parse("https://example.com/image1.jpg"),
          category = "bags",
          type = "handbag",
          brand = "Vakko",
          price = 0.0,
          material = listOf(),
          link = "https://example.com/item3")

  val item4 =
      Item(
          uuid = "3",
          image = Uri.parse("https://example.com/image1.jpg"),
          category = "accessories",
          type = "sunglasses",
          brand = "Ray-Ban",
          price = 100.0,
          material =
              listOf(
                  Material(name = "Plastic", percentage = 80.0),
                  Material(name = "Metal", percentage = 20.0)),
          link = "https://example.com/item4")

  suspend fun countItems(): Int {
    return FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).get().await().size()
  }

  @Test
  fun getNewItemIdReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val uids = (0 until 100).toSet<Int>().map { repository.getNewItemId() }.toSet()
    assertEquals(uids.size, numberIDs)
  }

  @Test
  fun addItemWithTheCorrectID() = runTest {
    repository.addItem(item1)
    val snapshot =
        FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(item1.uuid).get().await()

    Log.d("FirestoreTest", "Firestore stored document: ${snapshot.data}")
    assertEquals(1, countItems())
    val storedTodo = repository.getItemById(item1.uuid)
    assertEquals(storedTodo, item1)
  }

  @Test
  fun canAddItemToRepository() = runTest {
    repository.addItem(item1)
    assertEquals(1, countItems())
    val todos = repository.getAllItems()

    assertEquals(1, todos.size)
    val expectedTodo = item1.copy(uuid = "None", link = "None")
    val storedTodo = todos.first().copy(uuid = expectedTodo.uuid, link = expectedTodo.link)

    assertEquals(expectedTodo, storedTodo)
  }

  @Test
  fun retrieveItemById() = runTest {
    repository.addItem(item1)
    repository.addItem(item2)
    repository.addItem(item3)
    assertEquals(3, countItems())
    val storedTodo = repository.getItemById(item3.uuid)
    assertEquals(storedTodo, item3)

    val storedTodo2 = repository.getItemById(item2.uuid)
    assertEquals(storedTodo2, item2)
  }

  @Test
  fun checkUidsAreUniqueInTheCollection() = runTest {
    val uid = "duplicate"
    val item1Modified = item1.copy(uuid = uid)
    val itemDuplicatedUID = item2.copy(uuid = uid)
    // Depending on your implementation, adding a Item with an existing UID
    // might not be permitted
    runCatching {
      repository.addItem(item1Modified)
      repository.addItem(itemDuplicatedUID)
    }

    assertEquals(1, countItems())

    val items = repository.getAllItems()
    assertEquals(items.size, 1)
    val storedItem = items.first()
    assertEquals(storedItem.uuid, uid)
  }

  @Test
  fun deleteItemById() = runTest {
    repository.addItem(item1)
    repository.addItem(item2)
    repository.addItem(item3)
    assertEquals(3, countItems())

    repository.deleteItem(item2.uuid)
    assertEquals(2, countItems())
    val items = repository.getAllItems()
    assertEquals(items.size, 2)
    assert(!items.contains(item2))

    // Deleting an item that does not exist should not throw
    repository.deleteItem(item2.uuid)
    assertEquals(2, countItems())
  }

  @Test
  fun editItemById() = runTest {
    repository.addItem(item1)
    assertEquals(1, countItems())

    val newItem =
        item1.copy(
            type = "shirt",
            brand = "H&M",
            price = 20.0,
            material = listOf(Material(name = "Cotton", percentage = 100.0)))
    repository.editItem(item1.uuid, newItem)
    assertEquals(1, countItems())
    val items = repository.getAllItems()
    assertEquals(items.size, 1)
    val storedItem = items.first()
    assertEquals(storedItem, newItem)

    // Editing an item that does not exist should throw
    val nonExistingItem = item2.copy(uuid = "nonExisting")
    var didThrow = false
    try {
      repository.editItem(nonExistingItem.uuid, nonExistingItem)
    } catch (e: Exception) {
      didThrow = true
    }
    assert(didThrow)
    assertEquals(1, countItems())
  }

  @Test(expected = Exception::class)
  fun getItemByIdThrowsWhenItemNotFound() = runTest { repository.getItemById("nonExistingId") }

  @Test
  fun getAllItemsSkipsInvalidDocuments() = runTest {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Add an invalid Firestore document (missing required fields)
    collection.document("invalid").set(mapOf("brand" to "Zara")).await()

    // Add a valid one too
    repository.addItem(item1)

    val allItems = repository.getAllItems()

    // Only valid items should be returned (invalid skipped)
    assertEquals(1, allItems.size)
    assertEquals(item1.uuid, allItems.first().uuid)
  }

  @Test
  fun getAllItemsParsesPartiallyValidMaterialList() = runTest {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    val partialMaterialData =
        mapOf(
            "uuid" to "mixedMat",
            "image" to "https://example.com/image.jpg",
            "category" to "clothes",
            "type" to "jacket",
            "brand" to "Levi's",
            "price" to 120.0,
            "link" to "https://example.com/item",
            "material" to
                listOf(
                    mapOf("name" to "Cotton", "percentage" to 60.0), mapOf("invalidKey" to "oops")))
    collection.document("mixedMat").set(partialMaterialData).await()

    val items = repository.getAllItems()
    assertEquals(1, items.size)
    val matList = items.first().material
    assertEquals(2, matList.size)
    assertEquals("Cotton", matList.first().name)
    assertEquals(60.0, matList.first().percentage)
  }

  @Test
  fun mapToItemCatchesExceptionForInvalidData() = runTest {
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
            "link" to "https://example.com/item")

    collection.document("badItem").set(invalidData).await()

    // When repository tries to parse this, it should hit the catch block
    val allItems = repository.getAllItems()

    // It should skip the invalid item and not crash
    assertEquals(true, allItems.isEmpty())
  }

  @After
  fun tearDown() {
    FirebaseEmulator.clearFirestoreEmulator()
  }
}
