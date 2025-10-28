package com.android.ootd.model.items

import android.util.Log
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import com.android.ootd.utils.ItemsTest.Companion.item4
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
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
  fun getNewItemIdReturnsUniqueIDs() = runBlocking {
    val numberIDs = 100
    val uids = (0 until 100).toSet<Int>().map { itemsRepository.getNewItemId() }.toSet()
    assertEquals(uids.size, numberIDs)
  }

  @Test
  fun addItemWithTheCorrectID() = runBlocking {
    itemsRepository.addItem(item1)
    val snapshot =
        FirebaseEmulator.firestore
            .collection(ITEMS_COLLECTION)
            .document(item1.itemUuid)
            .get()
            .await()

    Log.d("FirestoreTest", "Firestore stored document: ${snapshot.data}")
    assertEquals(1, countItems())
    val storedTodo = itemsRepository.getItemById(item1.itemUuid)
    assertEquals(storedTodo, item1)
  }

  @Test
  fun canAddItemToRepository() = runBlocking {
    itemsRepository.addItem(item1)
    assertEquals(1, countItems())
    val todos = itemsRepository.getAllItems()

    assertEquals(1, todos.size)
    val expectedTodo = item1.copy(itemUuid = "None", link = "None")
    val storedTodo = todos.first().copy(itemUuid = expectedTodo.itemUuid, link = expectedTodo.link)

    assertEquals(expectedTodo, storedTodo)
  }

  @Test
  fun retrieveItemById() = runBlocking {
    itemsRepository.addItem(item1)
    itemsRepository.addItem(item2)
    itemsRepository.addItem(item3)
    assertEquals(3, countItems())
    val storedTodo = itemsRepository.getItemById(item3.itemUuid)
    assertEquals(storedTodo, item3)

    val storedTodo2 = itemsRepository.getItemById(item2.itemUuid)
    assertEquals(storedTodo2, item2)
  }

  @Test
  fun checkUidsAreUniqueInTheCollection() = runBlocking {
    val uid = "duplicate"
    val item1Modified = item1.copy(itemUuid = uid)
    val itemDuplicatedUID = item2.copy(itemUuid = uid)
    // Depending on your implementation, adding a Item with an existing UID
    // might not be permitted
    runCatching {
      itemsRepository.addItem(item1Modified)
      itemsRepository.addItem(itemDuplicatedUID)
    }

    assertEquals(1, countItems())

    val items = itemsRepository.getAllItems()
    assertEquals(items.size, 1)
    val storedItem = items.first()
    assertEquals(storedItem.itemUuid, uid)
  }

  @Test
  fun deleteItemById() = runBlocking {
    itemsRepository.addItem(item1)
    itemsRepository.addItem(item2)
    itemsRepository.addItem(item3)
    assertEquals(3, countItems())

    itemsRepository.deleteItem(item2.itemUuid)
    assertEquals(2, countItems())
    val items = itemsRepository.getAllItems()
    assertEquals(items.size, 2)
    assert(!items.contains(item2))

    // Deleting an item that does not exist should not throw
    itemsRepository.deleteItem(item2.itemUuid)
    assertEquals(2, countItems())
  }

  @Test
  fun editItemById() = runBlocking {
    itemsRepository.addItem(item1)
    assertEquals(1, countItems())

    val newItem =
        item1.copy(
            type = "shirt",
            brand = "H&M",
            price = 20.0,
            material = listOf(Material(name = "Cotton", percentage = 100.0)))
    itemsRepository.editItem(item1.itemUuid, newItem)
    assertEquals(1, countItems())
    val items = itemsRepository.getAllItems()
    assertEquals(items.size, 1)
    val storedItem = items.first()
    assertEquals(storedItem, newItem)

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
  fun addMultipleItemsConcurrently() = runBlocking {
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
  }

  @Test
  fun editItemUpdatesAllFields() = runBlocking {
    itemsRepository.addItem(item1)

    val updatedItem =
        item1.copy(
            category = "accessories",
            type = "watch",
            brand = "Rolex",
            price = 5000.0,
            material =
                listOf(
                    Material(name = "Gold", percentage = 70.0),
                    Material(name = "Steel", percentage = 30.0)),
            link = "https://example.com/newlink")

    itemsRepository.editItem(item1.itemUuid, updatedItem)

    val retrieved = itemsRepository.getItemById(item1.itemUuid)
    assertEquals(updatedItem, retrieved)
    assertEquals("accessories", retrieved.category)
    assertEquals("watch", retrieved.type)
    assertEquals("Rolex", retrieved.brand)
    assertEquals(5000.0, retrieved.price)
    assertEquals(2, retrieved.material.size)
  }

  @Test
  fun deleteAllItems() = runBlocking {
    itemsRepository.addItem(item1)
    itemsRepository.addItem(item2)
    itemsRepository.addItem(item3)
    itemsRepository.addItem(item4)
    assertEquals(4, countItems())

    itemsRepository.deleteItem(item1.itemUuid)
    itemsRepository.deleteItem(item2.itemUuid)
    itemsRepository.deleteItem(item3.itemUuid)
    itemsRepository.deleteItem(item4.itemUuid)

    assertEquals(0, countItems())
    val allItems = itemsRepository.getAllItems()
    assertEquals(0, allItems.size)
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
  fun addItemWithMaxPriceValue() = runBlocking {
    val itemWithMaxPrice = item1.copy(price = Double.MAX_VALUE)
    itemsRepository.addItem(itemWithMaxPrice)

    val retrieved = itemsRepository.getItemById(itemWithMaxPrice.itemUuid)
    assertEquals(Double.MAX_VALUE, retrieved.price)
  }

  @Test
  fun addItemWithZeroPrice() = runBlocking {
    val itemWithZeroPrice = item1.copy(price = 0.0)
    itemsRepository.addItem(itemWithZeroPrice)

    val retrieved = itemsRepository.getItemById(itemWithZeroPrice.itemUuid)
    assertEquals(0.0, retrieved.price)
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
  fun addItemWithLongStrings() = runBlocking {
    val longBrand = "A".repeat(500)
    val longLink = "https://example.com/" + "path/".repeat(100)
    val itemWithLongStrings = item1.copy(brand = longBrand, link = longLink)

    itemsRepository.addItem(itemWithLongStrings)
    val retrieved = itemsRepository.getItemById(itemWithLongStrings.itemUuid)

    assertEquals(longBrand, retrieved.brand)
    assertEquals(longLink, retrieved.link)
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

  @Test
  fun getAllItemsReturnsEmptyListWhenNoItems() = runBlocking {
    val items = itemsRepository.getAllItems()
    assertEquals(0, items.size)
  }

  @Test
  fun editItemPreservesOtherItems() = runBlocking {
    itemsRepository.addItem(item1)
    itemsRepository.addItem(item2)
    itemsRepository.addItem(item3)

    val updatedItem2 = item2.copy(price = 99.99)
    itemsRepository.editItem(item2.itemUuid, updatedItem2)

    val allItems = itemsRepository.getAllItems()
    assertEquals(3, allItems.size)

    // Verify item1 and item3 are unchanged
    val retrieved1 = itemsRepository.getItemById(item1.itemUuid)
    val retrieved3 = itemsRepository.getItemById(item3.itemUuid)
    assertEquals(item1, retrieved1)
    assertEquals(item3, retrieved3)
  }

  @Test
  fun deleteItemPreservesOtherItems() = runBlocking {
    itemsRepository.addItem(item1)
    itemsRepository.addItem(item2)
    itemsRepository.addItem(item3)

    itemsRepository.deleteItem(item2.itemUuid)

    val allItems = itemsRepository.getAllItems()
    assertEquals(2, allItems.size)

    val retrieved1 = itemsRepository.getItemById(item1.itemUuid)
    val retrieved3 = itemsRepository.getItemById(item3.itemUuid)
    assertEquals(item1, retrieved1)
    assertEquals(item3, retrieved3)
  }

  @Test
  fun addSameItemTwiceOverwrites() = runBlocking {
    itemsRepository.addItem(item1)
    val updatedItem1 = item1.copy(price = 150.0)
    itemsRepository.addItem(updatedItem1)

    assertEquals(1, countItems())
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
  fun addItemWithEmptyStrings() = runBlocking {
    val itemWithEmptyStrings = item1.copy(brand = "", type = "", link = "")

    itemsRepository.addItem(itemWithEmptyStrings)
    val retrieved = itemsRepository.getItemById(itemWithEmptyStrings.itemUuid)

    assertEquals("", retrieved.brand)
    assertEquals("", retrieved.type)
    assertEquals("", retrieved.link)
  }

  @Test
  fun addItemWithDifferentCategories() = runBlocking {
    val categories = listOf("clothes", "shoes", "bags", "accessories")
    categories.forEachIndexed { index, category ->
      val item = item1.copy(itemUuid = "item_$index", category = category)
      itemsRepository.addItem(item)
    }

    assertEquals(4, countItems())
    val allItems = itemsRepository.getAllItems()

    categories.forEach { category -> assert(allItems.any { it.category == category }) }
  }

  @Test
  fun materialListWithNullEntriesIsFiltered() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Manually insert data with null material entries
    val dataWithNullMaterial =
        mapOf(
            "itemUuid" to "itemWithNulls",
            "image" to mapOf("imageId" to "id1", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "clothes",
            "type" to "jacket",
            "brand" to "Brand",
            "price" to 100.0,
            "link" to "https://example.com/item",
            "material" to
                listOf(
                    mapOf("name" to "Cotton", "percentage" to 70.0),
                    null,
                    mapOf("name" to "Polyester", "percentage" to 30.0)),
            "ownerId" to ownerId)

    collection.document("itemWithNulls").set(dataWithNullMaterial).await()

    val retrieved = itemsRepository.getItemById("itemWithNulls")
    assertEquals(2, retrieved.material.size)
    assertEquals("Cotton", retrieved.material[0]?.name)
    assertEquals("Polyester", retrieved.material[1]?.name)
  }

  @Test
  fun documentWithMissingImageFieldIsSkipped() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Add item without image field
    collection
        .document("noImage")
        .set(
            mapOf(
                "itemUuid" to "noImage",
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>()))
        .await()

    // Add valid item
    itemsRepository.addItem(item1)

    val allItems = itemsRepository.getAllItems()
    assertEquals(1, allItems.size)
    assertEquals(item1.itemUuid, allItems[0].itemUuid)
  }

  @Test
  fun documentWithMissingCategoryFieldIsSkipped() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    collection
        .document("noCategory")
        .set(
            mapOf(
                "itemUuid" to "noCategory",
                "image" to mapOf("imageId" to "id", "imageUrl" to "url"),
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>()))
        .await()

    itemsRepository.addItem(item1)

    val allItems = itemsRepository.getAllItems()
    assertEquals(1, allItems.size)
  }

  @Test
  fun documentWithInvalidPriceTypeIsSkipped() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    collection
        .document("invalidPrice")
        .set(
            mapOf(
                "itemUuid" to "invalidPrice",
                "image" to mapOf("imageId" to "id", "imageUrl" to "url"),
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to "notANumber", // Invalid price type
                "link" to "https://example.com/item",
                "material" to emptyList<Material>()))
        .await()

    itemsRepository.addItem(item1)

    val allItems = itemsRepository.getAllItems()
    assertEquals(1, allItems.size)
  }

  @Test
  fun editItemMultipleTimes() = runBlocking {
    itemsRepository.addItem(item1)

    // Edit 1
    val edit1 = item1.copy(price = 10.0)
    itemsRepository.editItem(item1.itemUuid, edit1)
    var retrieved = itemsRepository.getItemById(item1.itemUuid)
    assertEquals(10.0, retrieved.price)

    // Edit 2
    val edit2 = edit1.copy(price = 20.0, brand = "NewBrand")
    itemsRepository.editItem(item1.itemUuid, edit2)
    retrieved = itemsRepository.getItemById(item1.itemUuid)
    assertEquals(20.0, retrieved.price)
    assertEquals("NewBrand", retrieved.brand)

    // Edit 3
    val edit3 = edit2.copy(category = "shoes", type = "sneakers")
    itemsRepository.editItem(item1.itemUuid, edit3)
    retrieved = itemsRepository.getItemById(item1.itemUuid)
    assertEquals("shoes", retrieved.category)
    assertEquals("sneakers", retrieved.type)
  }

  @Test
  fun getAllItemsWithMixedValidAndInvalidDocuments() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Add valid items
    itemsRepository.addItem(item1)
    itemsRepository.addItem(item2)

    // Add invalid document (missing required field)
    collection.document("invalid1").set(mapOf("brand" to "OnlyBrand")).await()

    // Add another valid item
    itemsRepository.addItem(item3)

    // Add another invalid document
    collection
        .document("invalid2")
        .set(mapOf("itemUuid" to "invalid2", "category" to "clothes"))
        .await()

    val allItems = itemsRepository.getAllItems()
    assertEquals(3, allItems.size)

    val validIds = allItems.map { it.itemUuid }
    assert(validIds.contains(item1.itemUuid))
    assert(validIds.contains(item2.itemUuid))
    assert(validIds.contains(item3.itemUuid))
  }

  @Test
  fun imageDataWithEmptyStrings() = runBlocking {
    val itemWithEmptyImage = item1.copy(image = ImageData(imageId = "", imageUrl = ""))

    itemsRepository.addItem(itemWithEmptyImage)
    val retrieved = itemsRepository.getItemById(itemWithEmptyImage.itemUuid)

    assertEquals("", retrieved.image.imageId)
    assertEquals("", retrieved.image.imageUrl)
  }

  @Test
  fun addItemsWithSequentialIds() = runBlocking {
    val items =
        (0 until 10).map { index ->
          item1.copy(itemUuid = "sequential_$index", price = index.toDouble())
        }

    items.forEach { itemsRepository.addItem(it) }

    assertEquals(10, countItems())

    val allItems = itemsRepository.getAllItems()
    assertEquals(10, allItems.size)

    // Verify each item exists and has correct price
    items.forEach { expectedItem ->
      val found = allItems.find { it.itemUuid == expectedItem.itemUuid }
      assertEquals(expectedItem.price, found?.price)
    }
  }

  // ========== COMPREHENSIVE TESTS FOR mapToItem FUNCTION ==========

  @Test
  fun mapToItemWithMissingRequiredFieldsReturnsNullOrEmptyList() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Test 1: Document without itemUuid
    collection
        .document("noUuid")
        .set(
            mapOf(
                "image" to mapOf("imageId" to "id", "imageUrl" to "url"),
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>(),
                "ownerId" to ownerId))
        .await()

    // Test 2: Document without image field
    collection
        .document("noImage")
        .set(
            mapOf(
                "itemUuid" to "test1",
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>(),
                "ownerId" to ownerId))
        .await()

    // Test 3: Document without category (using getItemById)
    val itemId = itemsRepository.getNewItemId()
    collection
        .document(itemId)
        .set(
            hashMapOf(
                "itemUuid" to itemId,
                "image" to
                    hashMapOf("imageId" to "img_789", "imageUrl" to "https://example.com/img.jpg"),
                // Missing "category"
                "type" to "Dress",
                "brand" to "H&M",
                "price" to 60.0,
                "link" to "https://hm.com",
                "ownerId" to ownerId))
        .await()

    // Test 4: Document without type field
    collection
        .document("noType")
        .set(
            mapOf(
                "itemUuid" to "test2",
                "image" to mapOf("imageId" to "id", "imageUrl" to "url"),
                "category" to "clothes",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>(),
                "ownerId" to ownerId))
        .await()

    // Test 5: Document without brand field
    collection
        .document("noBrand")
        .set(
            mapOf(
                "itemUuid" to "test3",
                "image" to mapOf("imageId" to "id", "imageUrl" to "url"),
                "category" to "clothes",
                "type" to "shirt",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>(),
                "ownerId" to ownerId))
        .await()

    // Test 6: Document without price field
    collection
        .document("noPrice")
        .set(
            mapOf(
                "itemUuid" to "test4",
                "image" to mapOf("imageId" to "id", "imageUrl" to "url"),
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "link" to "https://example.com/item",
                "material" to emptyList<Material>(),
                "ownerId" to ownerId))
        .await()

    // Test 7: Document without link field
    collection
        .document("noLink")
        .set(
            mapOf(
                "itemUuid" to "test5",
                "image" to mapOf("imageId" to "id", "imageUrl" to "url"),
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "material" to emptyList<Material>(),
                "ownerId" to ownerId))
        .await()

    // Test 8: Document without material field (this should succeed with empty list)
    collection
        .document("noMaterial")
        .set(
            mapOf(
                "itemUuid" to "test6",
                "image" to mapOf("imageId" to "id", "imageUrl" to "url"),
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "ownerId" to ownerId))
        .await()

    // Add one valid item for comparison
    itemsRepository.addItem(item1)

    // Verify getAllItems skips all documents with missing required fields except noMaterial
    val allItems = itemsRepository.getAllItems()
    assertEquals(2, allItems.size) // Only item1 and noMaterial should be present

    // Verify the valid item is present
    assertTrue(allItems.any { it.itemUuid == item1.itemUuid })

    // Verify noMaterial document is present with empty material list
    val noMaterialItem = allItems.find { it.itemUuid == "test6" }
    assertNotNull(noMaterialItem)
    assertEquals(0, noMaterialItem!!.material.size)

    // Verify getItemById throws for document with missing category
    try {
      val item = itemsRepository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItemWithNonStringImageFieldsUsesEmptyStrings() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Case 1: Non-string imageId (integer)
    collection
        .document("invalidImageId")
        .set(
            mapOf(
                "itemUuid" to "test1",
                "image" to mapOf("imageId" to 123, "imageUrl" to "url"),
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>(),
                "ownerId" to ownerId))
        .await()

    // Case 2: Non-string imageUrl (boolean)
    collection
        .document("invalidImageUrl")
        .set(
            mapOf(
                "itemUuid" to "test2",
                "image" to mapOf("imageId" to "id", "imageUrl" to false),
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>(),
                "ownerId" to ownerId))
        .await()

    val allItems = itemsRepository.getAllItems()
    assertEquals(2, allItems.size)

    val invalidImageIdItem = allItems.first { it.image.imageUrl == "url" }
    assertEquals("", invalidImageIdItem.image.imageId)
    assertEquals("url", invalidImageIdItem.image.imageUrl)

    val invalidImageUrlItem = allItems.first { it.image.imageId == "id" }
    assertEquals("id", invalidImageUrlItem.image.imageId)
    assertEquals("", invalidImageUrlItem.image.imageUrl)
  }

  @Test
  fun mapToItemCatchesExceptionAndLogsError() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    // Create a document that will cause an exception during parsing
    collection
        .document("exceptionDoc")
        .set(
            mapOf(
                "itemUuid" to "test",
                "image" to listOf("notAMap"), // This will cause cast exception
                "category" to "clothes",
                "type" to "shirt",
                "brand" to "Brand",
                "price" to 50.0,
                "link" to "https://example.com/item",
                "material" to emptyList<Material>()))
        .await()

    // Add a valid item too
    itemsRepository.addItem(item1)

    // getAllItems should catch the exception and skip the bad document
    val allItems = itemsRepository.getAllItems()
    assertEquals(1, allItems.size)
    assertEquals(item1.itemUuid, allItems[0].itemUuid)
  }

  @Test
  fun mapToItemWithCompleteValidDataReturnsItem() = runBlocking {
    val db = FirebaseEmulator.firestore
    val collection = db.collection(ITEMS_COLLECTION)

    collection
        .document("complete")
        .set(
            mapOf(
                "itemUuid" to "completeItem",
                "image" to
                    mapOf("imageId" to "img123", "imageUrl" to "https://example.com/img.jpg"),
                "category" to "clothes",
                "type" to "jacket",
                "brand" to "Nike",
                "price" to 99.99,
                "link" to "https://example.com/item",
                "material" to
                    listOf(
                        mapOf("name" to "Cotton", "percentage" to 70.0),
                        mapOf("name" to "Polyester", "percentage" to 30.0)),
                "ownerId" to ownerId))
        .await()

    val allItems = itemsRepository.getAllItems()
    assertEquals(1, allItems.size)
    assertEquals("completeItem", allItems[0].itemUuid)
    assertEquals("img123", allItems[0].image.imageId)
    assertEquals("https://example.com/img.jpg", allItems[0].image.imageUrl)
    assertEquals("clothes", allItems[0].category)
    assertEquals("jacket", allItems[0].type)
    assertEquals("Nike", allItems[0].brand)
    assertEquals(99.99, allItems[0].price)
    assertEquals("https://example.com/item", allItems[0].link)
    assertEquals(2, allItems[0].material.size)
  }
}
