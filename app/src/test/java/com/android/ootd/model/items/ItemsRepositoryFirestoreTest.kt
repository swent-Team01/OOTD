package com.android.ootd.model.items

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemsRepositoryFirestoreTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var collection: CollectionReference
  private lateinit var query: Query
  private lateinit var repository: ItemsRepositoryFirestore

  @Before
  fun setup() {
    db = mockk(relaxed = true)
    collection = mockk(relaxed = true)
    query = mockk(relaxed = true)
    repository = ItemsRepositoryFirestore(db)
    every { db.collection(ITEMS_COLLECTION) } returns collection
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `fetchCrossOwnerBatch caches fetched items`() = runTest {
    val ids = listOf("item-1", "item-2")
    val item1 = sampleItem("item-1")
    val item2 = sampleItem("item-2")

    val doc1: QueryDocumentSnapshot = mockk(relaxed = true)
    val doc2: QueryDocumentSnapshot = mockk(relaxed = true)
    every { doc1.data } returns ItemsMappers.toMap(item1)
    every { doc2.data } returns ItemsMappers.toMap(item2)

    val querySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { querySnapshot.iterator() } returns mutableListOf(doc1, doc2).iterator()
    every { querySnapshot.documents } returns listOf(doc1, doc2)

    every { collection.whereIn(any<FieldPath>(), ids) } returns query
    every { query.get() } returns Tasks.forResult(querySnapshot)

    val firstFetch = repository.getItemsByIdsAcrossOwners(ids)
    val secondFetch = repository.getItemsByIdsAcrossOwners(ids)

    assertEquals(ids, firstFetch.map { it.itemUuid })
    assertEquals(ids, secondFetch.map { it.itemUuid })
    verify(exactly = 1) { collection.whereIn(any<FieldPath>(), ids) }
    verify(exactly = 1) { query.get() }
  }

  private fun sampleItem(id: String): Item =
      Item(
          itemUuid = id,
          postUuids = listOf("post-$id"),
          image = ImageData("image-$id", "https://example.com/$id.jpg"),
          category = "category",
          type = "type",
          brand = "brand",
          price = 10.0,
          currency = "USD",
          material = listOf(Material("cotton", 100.0)),
          link = "https://shop/$id",
          ownerId = "owner",
          condition = "new",
          size = "M",
          fitType = "regular",
          style = "casual",
          notes = "note")
}
