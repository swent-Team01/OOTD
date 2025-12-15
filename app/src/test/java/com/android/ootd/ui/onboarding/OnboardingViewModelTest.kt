package com.android.ootd.ui.onboarding

import android.content.SharedPreferences
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

  private lateinit var viewModel: OnboardingViewModel
  private lateinit var mockPrefs: SharedPreferences
  private lateinit var mockEditor: SharedPreferences.Editor
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    mockPrefs = mockk(relaxed = true)
    mockEditor = mockk(relaxed = true)
    every { mockPrefs.edit() } returns mockEditor
    every { mockEditor.putBoolean(any(), any()) } returns mockEditor
    every { mockEditor.remove(any()) } returns mockEditor
    every { mockEditor.apply() } just Runs

    every { mockPrefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_SEEN, false) } returns false
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state reflects not seen and no errors`() = runTest {
    viewModel = OnboardingViewModel(mockPrefs)
    advanceUntilIdle()

    assertFalse(viewModel.hasConsented.value)
    assertFalse(viewModel.isLoading.value)
    assertFalse(viewModel.consentSaved.value)
    assertFalse(viewModel.isInitializing.value)
    assertNull(viewModel.error.value)
  }

  @Test
  fun `getConsentStatus reads from preferences`() = runTest {
    every { mockPrefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_SEEN, false) } returns false
    viewModel = OnboardingViewModel(mockPrefs)
    advanceUntilIdle()
    assertFalse(viewModel.getConsentStatus())

    every { mockPrefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_SEEN, false) } returns true
    viewModel = OnboardingViewModel(mockPrefs)
    advanceUntilIdle()
    assertTrue(viewModel.getConsentStatus())
  }

  @Test
  fun `recordConsent sets flag and emits completion`() = runTest {
    viewModel = OnboardingViewModel(mockPrefs)
    advanceUntilIdle()

    viewModel.recordConsent()
    advanceUntilIdle()

    assertTrue(viewModel.hasConsented.value)
    assertTrue(viewModel.consentSaved.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
    verify { mockEditor.putBoolean(OnboardingViewModel.KEY_ONBOARDING_SEEN, true) }
  }

  @Test
  fun `recordConsent surfaces preference errors`() = runTest {
    every { mockEditor.putBoolean(any(), any()) } throws RuntimeException("Prefs error")
    viewModel = OnboardingViewModel(mockPrefs)
    advanceUntilIdle()

    viewModel.recordConsent()
    advanceUntilIdle()

    assertFalse(viewModel.consentSaved.value)
    assertEquals("Failed to save onboarding state. Please try again.", viewModel.error.value)
  }

  @Test
  fun `clearConsent removes flag`() = runTest {
    viewModel = OnboardingViewModel(mockPrefs)
    advanceUntilIdle()

    viewModel.clearConsent()
    advanceUntilIdle()

    assertFalse(viewModel.hasConsented.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
    verify { mockEditor.remove(OnboardingViewModel.KEY_ONBOARDING_SEEN) }
  }

  @Test
  fun `clearConsent surfaces preference errors`() = runTest {
    every { mockEditor.remove(any()) } throws RuntimeException("Prefs error")
    viewModel = OnboardingViewModel(mockPrefs)
    advanceUntilIdle()

    viewModel.clearConsent()
    advanceUntilIdle()

    assertFalse(viewModel.isLoading.value)
    assertEquals("Failed to reset onboarding flag. Please try again.", viewModel.error.value)
  }

  @Test
  fun `resetConsentSavedFlag clears completion marker`() = runTest {
    viewModel = OnboardingViewModel(mockPrefs)
    advanceUntilIdle()
    viewModel.recordConsent()
    advanceUntilIdle()
    viewModel.resetConsentSavedFlag()
    assertFalse(viewModel.consentSaved.value)
  }
}
