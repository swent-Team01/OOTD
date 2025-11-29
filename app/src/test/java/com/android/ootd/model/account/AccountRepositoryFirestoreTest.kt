package com.android.ootd.model.account

import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.user.USER_COLLECTION_PATH
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
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

  @Test
  fun `getItemsList falls back to cache on firestore failure`() = runTest {
    val cacheField =
        repository.javaClass.getDeclaredField("itemsListCache").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val cache =
        cacheField.get(repository)
            as
            java.util.concurrent.ConcurrentHashMap<
                String, java.util.concurrent.CopyOnWriteArrayList<String>>
    cache["user-1"] = java.util.concurrent.CopyOnWriteArrayList(listOf("cached-1", "cached-2"))

    every { document.get(Source.CACHE) } returns Tasks.forException(Exception("cache down"))
    every { document.get() } returns Tasks.forException(Exception("network down"))

    val result = repository.getItemsList("user-1")

    assertEquals(listOf("cached-1", "cached-2"), result)
  }

  @Test
  fun `accountExists rethrows firestore exceptions`() = runTest {
    val userCollection = mockk<CollectionReference>()
    val userDoc = mockk<DocumentReference>()
    every { firestore.collection(USER_COLLECTION_PATH) } returns userCollection
    every { userCollection.document("user-err") } returns userDoc
    every { userDoc.get() } returns Tasks.forException(Exception("permission denied"))

    assertFailsWith<Exception> { repository.accountExists("user-err") }
  }

  @Test
  fun `removeFriend ignores secondary update failures`() = runTest {
    val userCollection = mockk<CollectionReference>()
    val userDoc = mockk<DocumentReference>()
    val friendDoc = mockk<DocumentReference>()
    val friendSnapshot = mockk<DocumentSnapshot>()
    every { friendSnapshot.exists() } returns true
    every { firestore.collection(USER_COLLECTION_PATH) } returns userCollection
    every { userCollection.document("friend-1") } returns userDoc
    every { userDoc.get() } returns Tasks.forResult(friendSnapshot)

    val userAccountDoc = mockk<DocumentReference>()
    val friendAccountDoc = mockk<DocumentReference>()
    every { collection.document("user-1") } returns userAccountDoc
    every { collection.document("friend-1") } returns friendAccountDoc
    every { userAccountDoc.update(any<String>(), any()) } returns Tasks.forResult(null)
    every { friendAccountDoc.update(any<String>(), any()) } returns
        Tasks.forException(Exception("not allowed"))

    repository.removeFriend("user-1", "friend-1")
  }

  @Test
  fun `deleteAccount converts missing user to UnknowUserID`() = runTest {
    val spyRepo = io.mockk.spyk(repository)
    coEvery { spyRepo.deleteProfilePicture(any()) } throws NoSuchElementException()

    assertFailsWith<UnknowUserID> { spyRepo.deleteAccount("user-1") }
  }

  @Test
  fun `editAccount converts missing user to UnknowUserID`() = runTest {
    every { document.get() } returns Tasks.forResult(snapshot)
    every { snapshot.exists() } returns false

    assertFailsWith<UnknowUserID> {
      repository.editAccount(
          userID = "missing",
          username = "new",
          birthDay = "",
          picture = "",
          location = com.android.ootd.model.map.emptyLocation)
    }
  }
}
