package com.android.ootd.model.consent

import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ConsentRepositoryFirestoreTest : FirestoreTest() {

  private lateinit var consentRepository: ConsentRepository
  private lateinit var testUserId: String

  @Before
  override fun setUp() {
    super.setUp()
    consentRepository = ConsentRepositoryFirestore(FirebaseEmulator.firestore)
    testUserId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user-id"
  }

  @Test
  fun canGenerateNewConsentId() {
    val id1 = consentRepository.getNewConsentId()
    val id2 = consentRepository.getNewConsentId()

    assertTrue(id1.isNotBlank())
    assertTrue(id2.isNotBlank())
    assertTrue(id1 != id2) // Should be unique
  }

  @Test
  fun canAddAndRetrieveConsent() = runTest {
    val consentId = consentRepository.getNewConsentId()
    val consent =
        Consent(
            consentUuid = consentId,
            userId = testUserId,
            timestamp = System.currentTimeMillis(),
            version = "1.0")

    consentRepository.addConsent(consent)

    val retrieved = consentRepository.getConsentById(consentId)
    assertNotNull(retrieved)
    assertEquals(consentId, retrieved?.consentUuid)
    assertEquals(testUserId, retrieved?.userId)
    assertEquals("1.0", retrieved?.version)
  }

  @Test
  fun canRetrieveConsentByUserId() = runTest {
    val consentId = consentRepository.getNewConsentId()
    val timestamp = System.currentTimeMillis()
    val consent =
        Consent(
            consentUuid = consentId, userId = testUserId, timestamp = timestamp, version = "1.0")

    consentRepository.addConsent(consent)

    val retrieved = consentRepository.getConsentByUserId(testUserId)
    assertNotNull(retrieved)
    assertEquals(testUserId, retrieved?.userId)
    assertEquals(timestamp, retrieved?.timestamp)
  }

  @Test
  fun hasUserConsentedReturnsTrueWhenConsentExists() = runTest {
    val consentId = consentRepository.getNewConsentId()
    val consent =
        Consent(
            consentUuid = consentId,
            userId = testUserId,
            timestamp = System.currentTimeMillis(),
            version = "1.0")

    consentRepository.addConsent(consent)

    assertTrue(consentRepository.hasUserConsented(testUserId))
  }

  @Test
  fun hasUserConsentedReturnsFalseWhenNoConsent() = runTest {
    assertFalse(consentRepository.hasUserConsented("non-existent-user"))
  }

  @Test
  fun canDeleteConsentByUserId() = runTest {
    val consentId = consentRepository.getNewConsentId()
    val consent =
        Consent(
            consentUuid = consentId,
            userId = testUserId,
            timestamp = System.currentTimeMillis(),
            version = "1.0")

    consentRepository.addConsent(consent)
    assertTrue(consentRepository.hasUserConsented(testUserId))

    consentRepository.deleteConsentByUserId(testUserId)
    assertFalse(consentRepository.hasUserConsented(testUserId))
  }

  @Test
  fun canGetAllConsents() = runTest {
    // Get initial count to account for any existing data
    val initialConsents = consentRepository.getAllConsents()
    val initialCount = initialConsents.size

    val consent1 =
        Consent(
            consentUuid = consentRepository.getNewConsentId(),
            userId = testUserId,
            timestamp = System.currentTimeMillis(),
            version = "1.0")

    val consent2 =
        Consent(
            consentUuid = consentRepository.getNewConsentId(),
            userId = "another-user-${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            version = "1.0")

    consentRepository.addConsent(consent1)
    consentRepository.addConsent(consent2)

    val allConsents = consentRepository.getAllConsents()
    // Verify we have at least 2 more consents than we started with
    assertTrue(allConsents.size >= initialCount + 2)
    assertTrue(allConsents.any { it.userId == testUserId })
    assertTrue(allConsents.any { it.userId == consent2.userId })
  }

  @Test
  fun returnsNullForNonExistentConsent() = runTest {
    val retrieved = consentRepository.getConsentById("non-existent-id")
    assertNull(retrieved)
  }

  @Test
  fun returnsNullForBlankUserId() = runTest {
    val retrieved = consentRepository.getConsentByUserId("")
    assertNull(retrieved)
  }
}
