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
  fun `initial state handles both no consent and existing consent scenarios`() = runTest {
    // Test no consent exists
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    assertFalse(viewModel.hasConsented.value)
    assertFalse(viewModel.isLoading.value)
    assertFalse(viewModel.consentSaved.value)
    assertFalse(viewModel.isInitializing.value)
    assertNull(viewModel.error.value)

    // Test existing consent syncs from Firebase
    val existingConsent = Consent(testConsentUuid, testUserId, testTimestamp, "1.0")
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns existingConsent
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    assertTrue(viewModel.hasConsented.value)
    verify { mockEditor.putBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, true) }
    verify { mockEditor.putLong(BetaConsentViewModel.KEY_CONSENT_TIMESTAMP, testTimestamp) }
    verify { mockEditor.putString(BetaConsentViewModel.KEY_CONSENT_UUID, testConsentUuid) }
  }

  @Test
  fun `getConsentStatus returns correct value for both scenarios`() = runTest {
    // Test false when no consent
    every { mockPrefs.getBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, false) } returns false
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    assertFalse(viewModel.getConsentStatus())

    // Test true when consent given
    every { mockPrefs.getBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, false) } returns true
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    assertTrue(viewModel.getConsentStatus())
  }

  @Test
  fun `recordConsent handles success and error scenarios`() = runTest {
    // Test successful consent recording
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    every { mockRepository.getNewConsentId() } returns testConsentUuid
    coEvery { mockRepository.addConsent(any()) } just Runs
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    viewModel.recordConsent()
    advanceUntilIdle()

    assertTrue(viewModel.hasConsented.value)
    assertTrue(viewModel.consentSaved.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
    coVerify { mockRepository.addConsent(match { it.userId == testUserId }) }
    verify { mockEditor.putBoolean(BetaConsentViewModel.KEY_CONSENT_GIVEN, true) }

    // Test error when user not authenticated
    every { mockFirebaseAuth.currentUser } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    viewModel.recordConsent()
    advanceUntilIdle()

    assertFalse(viewModel.consentSaved.value)
    assertEquals("You must be signed in to provide consent", viewModel.error.value)

    // Test error on repository failure
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    coEvery { mockRepository.addConsent(any()) } throws Exception("Network error")
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    viewModel.recordConsent()
    advanceUntilIdle()

    assertFalse(viewModel.consentSaved.value)
    assertEquals("Failed to save consent. Please try again.", viewModel.error.value)
  }

  @Test
  fun `clearConsent handles success and error scenarios`() = runTest {
    // Test successful consent clearing
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    coEvery { mockRepository.deleteConsentByUserId(testUserId) } just Runs
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    viewModel.clearConsent()
    advanceUntilIdle()

    assertFalse(viewModel.hasConsented.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
    coVerify { mockRepository.deleteConsentByUserId(testUserId) }
    verify { mockEditor.remove(BetaConsentViewModel.KEY_CONSENT_GIVEN) }
    verify { mockEditor.remove(BetaConsentViewModel.KEY_CONSENT_TIMESTAMP) }
    verify { mockEditor.remove(BetaConsentViewModel.KEY_CONSENT_UUID) }

    // Test error on repository failure
    coEvery { mockRepository.deleteConsentByUserId(testUserId) } throws Exception("Delete failed")
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    viewModel.clearConsent()
    advanceUntilIdle()

    assertFalse(viewModel.isLoading.value)
    assertEquals("Failed to withdraw consent. Please try again.", viewModel.error.value)
  }

  @Test
  fun `error management works correctly`() = runTest {
    // Test clearError clears error state
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    coEvery { mockRepository.addConsent(any()) } throws Exception("Error")
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    viewModel.recordConsent()
    advanceUntilIdle()
    viewModel.clearError()
    assertNull(viewModel.error.value)

    // Test initialization sets error on Firebase failure
    coEvery { mockRepository.getConsentByUserId(testUserId) } throws Exception("Network error")
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    assertFalse(viewModel.isInitializing.value)
    assertEquals(
        "Failed to load consent status. Please check your connection.", viewModel.error.value)
  }

  @Test
  fun `resetConsentSavedFlag and timestamp retrieval work correctly`() = runTest {
    // Test resetConsentSavedFlag
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    every { mockRepository.getNewConsentId() } returns testConsentUuid
    coEvery { mockRepository.addConsent(any()) } just Runs
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()
    viewModel.recordConsent()
    advanceUntilIdle()
    viewModel.resetConsentSavedFlag()
    assertFalse(viewModel.consentSaved.value)

    // Test getConsentTimestamp returns null when no timestamp exists
    every { mockPrefs.getLong(BetaConsentViewModel.KEY_CONSENT_TIMESTAMP, -1L) } returns -1L
    assertNull(viewModel.getConsentTimestamp())

    // Test getConsentTimestamp returns timestamp when it exists
    every { mockPrefs.getLong(BetaConsentViewModel.KEY_CONSENT_TIMESTAMP, -1L) } returns
        testTimestamp
    assertEquals(testTimestamp, viewModel.getConsentTimestamp())
  }

  @Test
  fun `getConsentUuid returns correct values for both scenarios`() = runTest {
    coEvery { mockRepository.getConsentByUserId(testUserId) } returns null
    viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
    advanceUntilIdle()

    // Test returns null when no uuid exists
    every { mockPrefs.getString(BetaConsentViewModel.KEY_CONSENT_UUID, null) } returns null
    assertNull(viewModel.getConsentUuid())

    // Test returns uuid when it exists
    every { mockPrefs.getString(BetaConsentViewModel.KEY_CONSENT_UUID, null) } returns
        testConsentUuid
    assertEquals(testConsentUuid, viewModel.getConsentUuid())
  }

  @Test
  fun `recordConsent handles user without Firebase account and isLoading state correctly`() =
      runTest {
        // Test user without Firebase account
        every { mockFirebaseAuth.currentUser } returns null
        coEvery { mockRepository.getConsentByUserId(any()) } returns null
        viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
        advanceUntilIdle()
        viewModel.recordConsent()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.addConsent(any()) }
        assertNotNull(viewModel.error.value)

        // Test isLoading is true during operation
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockRepository.getNewConsentId() } returns testConsentUuid
        coEvery { mockRepository.addConsent(any()) } coAnswers { kotlinx.coroutines.delay(100) }
        viewModel = BetaConsentViewModel(mockPrefs, mockRepository)
        advanceUntilIdle()
        viewModel.recordConsent()
        testDispatcher.scheduler.advanceTimeBy(50)

        assertTrue(viewModel.isLoading.value)
      }
}
