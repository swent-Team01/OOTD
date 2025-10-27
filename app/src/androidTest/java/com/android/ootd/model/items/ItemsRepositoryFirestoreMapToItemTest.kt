package com.android.ootd.model.items

import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ItemsRepositoryFirestoreMapToItemTest : FirestoreTest() {

  private lateinit var repository: ItemsRepositoryFirestore

  @Before
  override fun setUp() {
    super.setUp()
    repository = ItemsRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Test
  fun mapToItem_withValidDocument_returnsCorrectItem() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf(
                    "imageId" to "test_image_123", "imageUrl" to "https://example.com/image.jpg"),
            "category" to "Clothing",
            "type" to "T-Shirt",
            "brand" to "Nike",
            "price" to 49.99,
            "link" to "https://nike.com/tshirt",
            "material" to
                listOf(
                    hashMapOf("name" to "Cotton", "percentage" to 80.0),
                    hashMapOf("name" to "Polyester", "percentage" to 20.0)))

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    val item = repository.getItemById(itemId)

    assertNotNull(item)
    assertEquals(itemId, item.itemUuid)
    assertEquals("test_image_123", item.image.imageId)
    assertEquals("https://example.com/image.jpg", item.image.imageUrl)
    assertEquals("Clothing", item.category)
    assertEquals("T-Shirt", item.type)
    assertEquals("Nike", item.brand)
    assertEquals(49.99, item.price)
    assertEquals("https://nike.com/tshirt", item.link)
    assertEquals(2, item.material.size)
    assertEquals("Cotton", item.material[0]?.name)
    assertEquals(80.0, item.material[0]?.percentage)
  }

  @Test
  fun mapToItem_withMissingItemUuid_returnsNullAndDoesNotCrash() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            // Missing "itemUuid"
            "image" to
                hashMapOf("imageId" to "img_123", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Shoes",
            "type" to "Sneakers",
            "brand" to "Adidas",
            "price" to 89.99,
            "link" to "https://adidas.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    // Should return null when itemUuid is missing
    try {
      val item = repository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      // Expected to throw because mapToItem returns null
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItem_withMissingImageMap_returnsNullAndDoesNotCrash() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            // Missing "image" field
            "category" to "Bags",
            "type" to "Backpack",
            "brand" to "Nike",
            "price" to 120.0,
            "link" to "https://nike.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    try {
      val item = repository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItem_withMissingImageId_usesEmptyString() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf(
                    // Missing "imageId"
                    "imageUrl" to "https://example.com/image.jpg"),
            "category" to "Accessories",
            "type" to "Hat",
            "brand" to "Puma",
            "price" to 25.0,
            "link" to "https://puma.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    val item = repository.getItemById(itemId)

    assertNotNull(item)
    assertEquals("", item.image.imageId) // Should default to empty string
    assertEquals("https://example.com/image.jpg", item.image.imageUrl)
  }

  @Test
  fun mapToItem_withMissingImageUrl_usesEmptyString() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf(
                    "imageId" to "img_456"
                    // Missing "imageUrl"
                    ),
            "category" to "Clothing",
            "type" to "Jacket",
            "brand" to "Zara",
            "price" to 150.0,
            "link" to "https://zara.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    val item = repository.getItemById(itemId)

    assertNotNull(item)
    assertEquals("img_456", item.image.imageId)
    assertEquals("", item.image.imageUrl) // Should default to empty string
  }

  @Test
  fun mapToItem_withMissingCategory_returnsNullAndDoesNotCrash() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_789", "imageUrl" to "https://example.com/img.jpg"),
            // Missing "category"
            "type" to "Dress",
            "brand" to "H&M",
            "price" to 60.0,
            "link" to "https://hm.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    try {
      val item = repository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItem_withMissingType_returnsNullAndDoesNotCrash() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_101", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Shoes",
            // Missing "type"
            "brand" to "Reebok",
            "price" to 95.0,
            "link" to "https://reebok.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    try {
      val item = repository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItem_withMissingBrand_returnsNullAndDoesNotCrash() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_202", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Bags",
            "type" to "Tote",
            // Missing "brand"
            "price" to 45.0,
            "link" to "https://example.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    try {
      val item = repository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItem_withMissingPrice_returnsNullAndDoesNotCrash() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_303", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Accessories",
            "type" to "Scarf",
            "brand" to "Gucci",
            // Missing "price"
            "link" to "https://gucci.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    try {
      val item = repository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItem_withMissingLink_returnsNullAndDoesNotCrash() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_404", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Clothing",
            "type" to "Pants",
            "brand" to "Levi's",
            "price" to 80.0
            // Missing "link"
            )

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    try {
      val item = repository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItem_withEmptyMaterialList_returnsEmptyList() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_505", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Clothing",
            "type" to "Shirt",
            "brand" to "Uniqlo",
            "price" to 30.0,
            "link" to "https://uniqlo.com",
            "material" to emptyList<Map<String, Any>>())

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    val item = repository.getItemById(itemId)

    assertNotNull(item)
    assertTrue(item.material.isEmpty())
  }

  @Test
  fun mapToItem_withNullMaterialList_returnsEmptyList() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_606", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Shoes",
            "type" to "Boots",
            "brand" to "Timberland",
            "price" to 180.0,
            "link" to "https://timberland.com"
            // Missing "material" field
            )

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    val item = repository.getItemById(itemId)

    assertNotNull(item)
    assertTrue(item.material.isEmpty())
  }

  @Test
  fun mapToItem_withMaterialNameOnly_defaultsPercentageToZero() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_808", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Bags",
            "type" to "Messenger",
            "brand" to "Fossil",
            "price" to 110.0,
            "link" to "https://fossil.com",
            "material" to listOf(hashMapOf("name" to "Leather")))

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    val item = repository.getItemById(itemId)

    assertNotNull(item)
    assertEquals(1, item.material.size)
    assertEquals("Leather", item.material[0]?.name)
    assertEquals(0.0, item.material[0]?.percentage)
  }

  @Test
  fun mapToItem_withWrongDataTypes_handlesGracefully() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to "not_a_map", // Wrong type - should be a map
            "category" to "Clothing",
            "type" to "Shirt",
            "brand" to "Brand",
            "price" to 50.0,
            "link" to "https://example.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    try {
      val item = repository.getItemById(itemId)
      assertNull(item)
    } catch (e: Exception) {
      // Should handle gracefully
      assertTrue(e.message!!.contains("Item not found"))
    }
  }

  @Test
  fun mapToItem_withIntegerPrice_convertsToDouble() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_909", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Accessories",
            "type" to "Belt",
            "brand" to "Coach",
            "price" to 100, // Integer instead of Double
            "link" to "https://coach.com")

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    val item = repository.getItemById(itemId)

    assertNotNull(item)
    assertEquals(100.0, item.price)
  }

  @Test
  fun mapToItem_withComplexMaterialPercentages_handlesNumbers() = runTest {
    val itemId = repository.getNewItemId()
    val docData =
        hashMapOf(
            "itemUuid" to itemId,
            "image" to
                hashMapOf("imageId" to "img_1010", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "Clothing",
            "type" to "Dress",
            "brand" to "Zara",
            "price" to 99.99,
            "link" to "https://zara.com",
            "material" to
                listOf(
                    hashMapOf("name" to "Silk", "percentage" to 50), // Integer
                    hashMapOf("name" to "Cotton", "percentage" to 30.5), // Double
                    hashMapOf("name" to "Elastane", "percentage" to 19.5f) // Float
                    ))

    FirebaseEmulator.firestore.collection(ITEMS_COLLECTION).document(itemId).set(docData).await()

    val item = repository.getItemById(itemId)

    assertNotNull(item)
    assertEquals(3, item.material.size)
    assertEquals(50.0, item.material[0]?.percentage)
    assertEquals(30.5, item.material[1]?.percentage)
    assertTrue(item.material[2]?.percentage!! > 19.0 && item.material[2]?.percentage!! < 20.0)
  }
}
