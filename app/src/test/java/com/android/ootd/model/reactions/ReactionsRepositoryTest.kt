package com.android.ootd.model.reactions

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.*

class ReactionRepositoryTest {

  private lateinit var repo: ReactionFirestoreRepository

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockStorage: FirebaseStorage
  private lateinit var mockStorageRoot: StorageReference

  // reactions/{postId}
  private lateinit var mockReactionsRoot: CollectionReference
  private lateinit var mockPostDoc: DocumentReference

  // reactions/{postId}/users/{userId}
  private lateinit var mockUsersSubcollection: CollectionReference
  private lateinit var mockUserDoc: DocumentReference

  @Before
  fun setup() {
    mockkStatic(Tasks::class)

    mockDb = mockk(relaxed = true)
    mockStorage = mockk(relaxed = true)
    mockStorageRoot = mockk(relaxed = true)

    every { mockStorage.reference } returns mockStorageRoot
    every { mockStorageRoot.child(any()) } returns mockStorageRoot

    // reactions/
    mockReactionsRoot = mockk(relaxed = true)
    every { mockDb.collection("reactions") } returns mockReactionsRoot

    // reactions/{postId}
    mockPostDoc = mockk(relaxed = true)
    every { mockReactionsRoot.document(any()) } returns mockPostDoc

    // reactions/{postId}/users
    mockUsersSubcollection = mockk(relaxed = true)
    every { mockPostDoc.collection("users") } returns mockUsersSubcollection

    // reactions/{postId}/users/{userId}
    mockUserDoc = mockk(relaxed = true)
    every { mockUsersSubcollection.document(any()) } returns mockUserDoc

    repo = ReactionFirestoreRepository(mockDb, mockStorage)
  }

  @After fun tearDown() = unmockkAll()

  // -------------------------------------------------------------
  // TESTS
  // -------------------------------------------------------------

  @Test
  fun getUserReaction_readsDocumentAndMapsItCorrectly() = runTest {
    val postId = "post123"
    val userId = "userABC"

    val mockSnap: DocumentSnapshot = mockk(relaxed = true)

    // FIX: correct reaction path
    every { mockUserDoc.get() } returns Tasks.forResult(mockSnap)

    every { mockSnap.exists() } returns true
    every { mockSnap.getString("postId") } returns postId
    every { mockSnap.getString("ownerId") } returns userId
    every { mockSnap.getString("reactionURL") } returns "http://image.url"

    val result = repo.getUserReaction(postId, userId)

    assertEquals(postId, result!!.postUID)
    assertEquals(userId, result.ownerId)
    assertEquals("http://image.url", result.reactionURL)
  }

  @Test
  fun getAllReactions_returnsMappedList() = runTest {
    val postId = "post123"

    val mockSnap: QuerySnapshot = mockk(relaxed = true)
    val mockDoc1: DocumentSnapshot = mockk(relaxed = true)
    val mockDoc2: DocumentSnapshot = mockk(relaxed = true)

    every { mockUsersSubcollection.get() } returns Tasks.forResult(mockSnap)
    every { mockSnap.documents } returns listOf(mockDoc1, mockDoc2)

    every { mockDoc1.getString("postId") } returns postId
    every { mockDoc1.getString("ownerId") } returns "u1"
    every { mockDoc1.getString("reactionURL") } returns "url1"

    every { mockDoc2.getString("postId") } returns postId
    every { mockDoc2.getString("ownerId") } returns "u2"
    every { mockDoc2.getString("reactionURL") } returns "url2"

    val result = repo.getAllReactions(postId)

    assertEquals(2, result.size)
    assertTrue(result.any { it.ownerId == "u1" })
    assertTrue(result.any { it.ownerId == "u2" })
  }

  @Test
  fun deleteReaction_deletesFirestoreAndStorage() = runTest {
    val postId = "post123"
    val userId = "u1"

    val mockStorageFile: StorageReference = mockk(relaxed = true)
    every { mockStorageRoot.child(any()) } returns mockStorageFile
    every { mockStorageFile.delete() } returns Tasks.forResult(null)

    every { mockUserDoc.delete() } returns Tasks.forResult(null)

    repo.deleteReaction(postId, userId)

    verify { mockUserDoc.delete() }
    verify { mockStorageRoot.child("images/reactions/$postId/$userId.jpg") }
    verify { mockStorageFile.delete() }
  }
}
