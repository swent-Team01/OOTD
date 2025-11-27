package com.android.ootd.model.account

import com.android.ootd.model.post.OutfitPostRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryFirestoreTest {

  @RelaxedMockK private lateinit var firestore: FirebaseFirestore
  @RelaxedMockK private lateinit var collection: CollectionReference
  @RelaxedMockK private lateinit var document: DocumentReference
  @RelaxedMockK private lateinit var snapshot: DocumentSnapshot
  @RelaxedMockK private lateinit var storage: FirebaseStorage
  @RelaxedMockK private lateinit var outfitPostRepository: OutfitPostRepository

  private lateinit var repository: AccountRepositoryFirestore

  @Before
  fun setup() {
    MockKAnnotations.init(this)
    repository = AccountRepositoryFirestore(firestore, storage, outfitPostRepository)

    mockkStatic(FirebaseAuth::class)
    mockkObject(Firebase)
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-1"

    every { firestore.collection(ACCOUNT_COLLECTION_PATH) } returns collection
    every { collection.document(any()) } returns document

    stubSnapshot(emptyList())
    every { document.update(any<String>(), any()) } returns Tasks.forResult(null)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun stubSnapshot(items: List<String>) {
    every { snapshot.exists() } returns true
    every { snapshot.get("starredItemUids") } returns items
    every { snapshot.data } returns mapOf("starredItemUids" to items)
    every { document.get(Source.CACHE) } returns Tasks.forResult(snapshot)
    every { document.get() } returns Tasks.forResult(snapshot)
  }

  @Test
  fun `getStarredItems uses cached firestore data`() = runTest {
    stubSnapshot(listOf("coat", "boots"))

    val result = repository.getStarredItems("user-1")

    assertEquals(listOf("coat", "boots"), result)
    // second call hits memory cache instead of querying again
    assertEquals(listOf("coat", "boots"), repository.getStarredItems("user-1"))
  }

  @Test
  fun `addStarredItem appends entry locally`() = runTest {
    stubSnapshot(listOf("coat"))

    assertTrue(repository.addStarredItem("hat"))
    val cached = repository.getStarredItems("user-1")

    assertEquals(listOf("coat", "hat"), cached)
  }

  @Test
  fun `removeStarredItem drops entry from cache`() = runTest {
    stubSnapshot(listOf("coat", "hat"))
    repository.getStarredItems("user-1") // prime cache

    assertTrue(repository.removeStarredItem("coat"))
    val cached = repository.getStarredItems("user-1")

    assertEquals(listOf("hat"), cached)
  }

  @Test
  fun `toggleStarredItem flips membership`() = runTest {
    stubSnapshot(listOf("coat"))
    repository.getStarredItems("user-1") // prime cache

    val removed = repository.toggleStarredItem("coat")
    assertTrue("coat" !in removed)

    val addedBack = repository.toggleStarredItem("coat")
    assertTrue("coat" in addedBack)
  }
}
