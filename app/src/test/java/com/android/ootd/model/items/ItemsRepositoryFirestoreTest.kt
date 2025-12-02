package com.android.ootd.model.items

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
  fun `fetchCrossOwnerBatch fetches items`() = runTest {
    val ids = listOf("item-1", "item-2")
    val item1 = sampleItem("item-1")
    val item2 = sampleItem("item-2")

    val docRef1: DocumentReference = mockk(relaxed = true)
    val docRef2: DocumentReference = mockk(relaxed = true)
    val docSnap1: DocumentSnapshot = mockk(relaxed = true)
    val docSnap2: DocumentSnapshot = mockk(relaxed = true)

    every { collection.document("item-1") } returns docRef1
    every { collection.document("item-2") } returns docRef2

    every { docRef1.get() } returns Tasks.forResult(docSnap1)
    every { docRef2.get() } returns Tasks.forResult(docSnap2)

    every { docSnap1.exists() } returns true
    every { docSnap2.exists() } returns true
    every { docSnap1.data } returns ItemsMappers.toMap(item1)
    every { docSnap2.data } returns ItemsMappers.toMap(item2)

    val firstFetch = repository.getItemsByIdsAcrossOwners(ids)

    assertEquals(ids.toSet(), firstFetch.map { it.itemUuid }.toSet())
    verify(exactly = 1) { collection.document("item-1") }
    verify(exactly = 1) { collection.document("item-2") }
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
