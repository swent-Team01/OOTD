package com.android.ootd.ui.consent

import android.content.SharedPreferences
import com.android.ootd.model.consent.Consent
import com.android.ootd.model.consent.ConsentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BetaConsentViewModelTest {

  private lateinit var viewModel: BetaConsentViewModel
  private lateinit var mockPrefs: SharedPreferences
  private lateinit var mockEditor: SharedPreferences.Editor
  private lateinit var mockRepository: ConsentRepository
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private val testDispatcher = StandardTestDispatcher()

  private val testUserId = "test-user-123"
  private val testConsentUuid = "consent-uuid-456"
  private val testTimestamp = 1234567890L

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Mock SharedPreferences
    mockPrefs = mockk(relaxed = true)
    mockEditor = mockk(relaxed = true)
    every { mockPrefs.edit() } returns mockEditor
    every { mockEditor.putBoolean(any(), any()) } returns mockEditor
    every { mockEditor.putLong(any(), any()) } returns mockEditor
    every { mockEditor.putString(any(), any()) } returns mockEditor
    every { mockEditor.remove(any()) } returns mockEditor
    every { mockEditor.apply() } just Runs

    // Mock Repository
    mockRepository = mockk(relaxed = true)

    // Mock Firebase Auth
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    every { mockFirebaseUser.uid } returns testUserId

    // Default: no consent given initially
    every { mockPrefs.getBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, false) } returns false
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `initial state has correct defaults when no consent exists`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null

    // When
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // Then
    assertFalse(viewModel.hasConsented.value)
    assertFalse(viewModel.isLoading.value)
    assertFalse(viewModel.consentSaved.value)
    assertFalse(viewModel.isInitializing.value)
    assertNull(viewModel.error.value)
  }

  @Test
  fun `initial state syncs from Firebase when consent exists`() = runTest {
    // Given
    val existingConsent =
        Consent(
            consentUuid = testConsentUuid,
            userId = testUserId,
            timestamp = testTimestamp,
            version = "1.0")
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns existingConsent

    // When
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.hasConsented.value)
    verify { mockEditor.putBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, true) }
    verify { mockEditor.putLong(BetaConsentViewModel.KEY_CONSENT_TIMESTAMP, testTimestamp) }
    verify { mockEditor.putString(BetaConsentViewModel.KEY_CONSENT_UUID, testConsentUuid) }
  }

  @Test
  fun `getConsentStatus returns false when no consent given`() = runTest {
    // Given
    every { mockPrefs.getBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, false) } returns false
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    val status = viewModel.getConsentStatus()

    // Then
    assertFalse(status)
  }

  @Test
  fun `getConsentStatus returns true when consent given`() = runTest {
    // Given
    every { mockPrefs.getBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, false) } returns true
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    val status = viewModel.getConsentStatus()

    // Then
    assertTrue(status)
  }

  @Test
  fun `recordConsent saves to both Firebase and SharedPreferences`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    every { mockRepository.getNewConsentId() } returns testConsentUuid
    coEvery { mockRepository.addConsent(any()) } just Runs
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    viewModel.recordConsent()
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.hasConsented.value)
    assertTrue(viewModel.consentSaved.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)

    coVerify { mockRepository.addConsent(match { it.userId == testUserId }) }
    verify { mockEditor.putBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, true) }
    verify { mockEditor.putString(BetaConsentViewModel.KEY_CONSENT_UUID, testConsentUuid) }
  }

  @Test
  fun `recordConsent sets error when user not authenticated`() = runTest {
    // Given
    every { mockFirebaseAuth.currentUser } returns null
    coEvery { mockRepository.getConsentByUserId(any()) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    viewModel.recordConsent()
    advanceUntilIdle()

    // Then
    assertFalse(viewModel.consentSaved.value)
    assertFalse(viewModel.isLoading.value)
    assertEquals("You must be signed in to provide consent", viewModel.error.value)
  }

  @Test
  fun `recordConsent sets error on repository failure`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    every { mockRepository.getNewConsentId() } returns testConsentUuid
    coEvery { mockRepository.addConsent(any()) } throws Exception("Network error")
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    viewModel.recordConsent()
    advanceUntilIdle()

    // Then
    assertFalse(viewModel.consentSaved.value)
    assertFalse(viewModel.isLoading.value)
    assertEquals("Failed to save consent. Please try again.", viewModel.error.value)
  }

  @Test
  fun `clearConsent removes from Firebase and SharedPreferences`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    coEvery { mockRepository.deleteConsentByUserId(testUserId) } just Runs
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    viewModel.clearConsent()
    advanceUntilIdle()

    // Then
    assertFalse(viewModel.hasConsented.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)

    coVerify { mockRepository.deleteConsentByUserId(testUserId) }
    verify { mockEditor.remove(BetaConsentViewModel.KEY_CONSENT_GIVEN) }
    verify { mockEditor.remove(BetaConsentViewModel.KEY_CONSENT_TIMESTAMP) }
    verify { mockEditor.remove(BetaConsentViewModel.KEY_CONSENT_UUID) }
  }

  @Test
  fun `clearConsent sets error on repository failure`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    coEvery { mockRepository.deleteConsentByUserId(testUserId) } throws Exception("Delete failed")
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    viewModel.clearConsent()
    advanceUntilIdle()

    // Then
    assertFalse(viewModel.isLoading.value)
    assertEquals("Failed to withdraw consent. Please try again.", viewModel.error.value)
  }

  @Test
  fun `clearError clears error state`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    coEvery { mockRepository.addConsent(any()) } throws Exception("Error")
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    viewModel.recordConsent()
    advanceUntilIdle()

    // When
    viewModel.clearError()

    // Then
    assertNull(viewModel.error.value)
  }

  @Test
  fun `resetConsentSavedFlag resets consentSaved state`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    every { mockRepository.getNewConsentId() } returns testConsentUuid
    coEvery { mockRepository.addConsent(any()) } just Runs
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    viewModel.recordConsent()
    advanceUntilIdle()

    // When
    viewModel.resetConsentSavedFlag()

    // Then
    assertFalse(viewModel.consentSaved.value)
  }

  @Test
  fun `getConsentTimestamp returns null when no timestamp exists`() = runTest {
    // Given
    every { mockPrefs.getLong(BetaConsentViewModel.KEY_CONSENT_TIMESTAMP, -1L) } returns -1L
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    val timestamp = viewModel.getConsentTimestamp()

    // Then
    assertNull(timestamp)
  }

  @Test
  fun `getConsentTimestamp returns timestamp when it exists`() = runTest {
    // Given
    every { mockPrefs.getLong(BetaConsentViewModel.KEY_CONSENT_TIMESTAMP, -1L) } returns
        testTimestamp
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    val timestamp = viewModel.getConsentTimestamp()

    // Then
    assertEquals(testTimestamp, timestamp)
  }

  @Test
  fun `getConsentUuid returns null when no uuid exists`() = runTest {
    // Given
    every { mockPrefs.getString(BetaConsentViewModel.KEY_CONSENT_UUID, null) } returns null
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    val uuid = viewModel.getConsentUuid()

    // Then
    assertNull(uuid)
  }

  @Test
  fun `getConsentUuid returns uuid when it exists`() = runTest {
    // Given
    every { mockPrefs.getString(BetaConsentViewModel.KEY_CONSENT_UUID, null) } returns
        testConsentUuid
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    val uuid = viewModel.getConsentUuid()

    // Then
    assertEquals(testConsentUuid, uuid)
  }

  @Test
  fun `initialization sets error on Firebase failure`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } throws Exception("Network error")

    // When
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // Then
    assertFalse(viewModel.isInitializing.value)
    assertEquals(
        "Failed to load consent status. Please check your connection.", viewModel.error.value)
  }

  @Test
  fun `recordConsent handles user without Firebase account`() = runTest {
    // Given
    every { mockFirebaseAuth.currentUser } returns null
    coEvery { mockRepository.getConsentByUserId(any()) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    viewModel.recordConsent()
    advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { mockRepository.addConsent(any()) }
    assertNotNull(viewModel.error.value)
  }

  @Test
  fun `isLoading is true during recordConsent operation`() = runTest {
    // Given
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    every { mockRepository.getNewConsentId() } returns testConsentUuid
    coEvery { mockRepository.addConsent(any()) } coAnswers
        {
          kotlinx.coroutines.delay(100) // Simulate delay
        }
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // When
    viewModel.recordConsent()
    testDispatcher.scheduler.advanceTimeBy(50) // Advance partway through

    // Then
    assertTrue(viewModel.isLoading.value)
  }
}
