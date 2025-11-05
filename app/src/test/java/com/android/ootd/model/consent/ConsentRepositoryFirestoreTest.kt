package com.android.ootd.model.consent

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConsentRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocumentRef: DocumentReference
  private lateinit var mockQuery: Query
  private lateinit var repository: ConsentRepositoryFirestore

  @Before
  fun setup() {
    mockDb = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocumentRef = mockk(relaxed = true)
    mockQuery = mockk(relaxed = true)
    every { mockDb.collection(CONSENT_COLLECTION_PATH) } returns mockCollection
    repository = ConsentRepositoryFirestore(mockDb)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun getNewConsentIdReturnsNonEmptyString() {
    every { mockCollection.document() } returns mockDocumentRef
    every { mockDocumentRef.id } returns "generated-uuid-123"

    val id = repository.getNewConsentId()

    assertNotNull(id)
    assertEquals("generated-uuid-123", id)
  }

  @Test
  fun addConsentSuccessfullyAddsValidConsent() = runTest {
    val consent = Consent("uuid-123", "user-456", 1234567890L, "1.0")
    every { mockCollection.document(consent.consentUuid) } returns mockDocumentRef
    every { mockDocumentRef.set(any()) } returns Tasks.forResult(null)

    repository.addConsent(consent)

    verify { mockDocumentRef.set(any()) }
  }

  @Test
  fun addConsentIgnoresInvalidConsentWithBlankFields() = runTest {
    val invalidConsents =
        listOf(
            Consent("", "user-456", 1234567890L, "1.0"),
            Consent("uuid-123", "", 1234567890L, "1.0"))

    invalidConsents.forEach { consent -> repository.addConsent(consent) }

    verify(exactly = 0) { mockDocumentRef.set(any()) }
  }

  @Test
  fun getConsentByUserIdReturnsNullWhenNotFoundOrBlankUserId() = runTest {
    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns emptyList()
    every { mockCollection.whereEqualTo(USER_ID_FIELD, "user-123") } returns mockQuery
    every { mockQuery.limit(1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    val resultNotFound = repository.getConsentByUserId("user-123")
    val resultBlank = repository.getConsentByUserId("")

    assertNull(resultNotFound)
    assertNull(resultBlank)
  }

  @Test
  fun deleteConsentByUserIdDeletesAllConsentRecordsForUser() = runTest {
    val userId = "user-123"
    val mockDoc1: DocumentSnapshot = mockk(relaxed = true)
    val mockDoc2: DocumentSnapshot = mockk(relaxed = true)
    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    val mockBatch: WriteBatch = mockk(relaxed = true)
    val mockRef1: DocumentReference = mockk(relaxed = true)
    val mockRef2: DocumentReference = mockk(relaxed = true)

    every { mockDoc1.reference } returns mockRef1
    every { mockDoc2.reference } returns mockRef2
    every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)
    every { mockQuerySnapshot.size() } returns 2
    every { mockCollection.whereEqualTo(USER_ID_FIELD, userId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockDb.batch() } returns mockBatch
    every { mockBatch.delete(any()) } returns mockBatch
    every { mockBatch.commit() } returns Tasks.forResult(null)

    repository.deleteConsentByUserId(userId)

    verify { mockBatch.delete(mockRef1) }
    verify { mockBatch.delete(mockRef2) }
    verify { mockBatch.commit() }
  }

  @Test
  fun deleteConsentByUserIdIgnoresBlankUserId() = runTest {
    repository.deleteConsentByUserId("")

    verify(exactly = 0) { mockQuery.get() }
  }

  @Test
  fun getAllConsentsReturnsEmptyListOnError() = runTest {
    every { mockCollection.get() } returns Tasks.forException(Exception("Firestore error"))

    val result = repository.getAllConsents()

    assertTrue(result.isEmpty())
  }

  @Test
  fun repositoryHandlesFirestoreExceptionsGracefully() = runTest {
    every { mockCollection.whereEqualTo(USER_ID_FIELD, "error-user") } returns mockQuery
    every { mockQuery.limit(1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forException(Exception("Network error"))

    val result = repository.getConsentByUserId("error-user")

    assertNull(result)
  }
}
