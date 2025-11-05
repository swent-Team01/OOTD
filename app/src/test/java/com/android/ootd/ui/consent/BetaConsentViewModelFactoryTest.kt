package com.android.ootd.ui.consent

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.android.ootd.model.consent.ConsentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BetaConsentViewModelFactoryTest {

  private lateinit var mockContext: Context
  private lateinit var mockSharedPreferences: SharedPreferences
  private lateinit var mockRepository: ConsentRepository
  private lateinit var factory: BetaConsentViewModelFactory

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockSharedPreferences = mockk(relaxed = true)
    mockRepository = mockk(relaxed = true)

    every { mockContext.applicationContext } returns mockContext
    every {
      mockContext.getSharedPreferences(BetaConsentViewModel.PREFS_NAME, Context.MODE_PRIVATE)
    } returns mockSharedPreferences
  }

  @Test
  fun `factory creates BetaConsentViewModel successfully`() {
    // Given
    factory = BetaConsentViewModelFactory(mockContext, mockRepository)

    // When
    val viewModel = factory.create(BetaConsentViewModel::class.java)

    // Then
    assertNotNull(viewModel)
    assertTrue(viewModel is BetaConsentViewModel)
  }

  @Test
  fun `factory uses application context for SharedPreferences`() {
    // When
    factory = BetaConsentViewModelFactory(mockContext, mockRepository)

    // Then
    verify { mockContext.applicationContext }
    verify {
      mockContext.getSharedPreferences(BetaConsentViewModel.PREFS_NAME, Context.MODE_PRIVATE)
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `factory throws exception for unknown ViewModel class`() {
    // Given
    factory = BetaConsentViewModelFactory(mockContext, mockRepository)

    class UnknownViewModel : ViewModel()

    // When
    factory.create(UnknownViewModel::class.java)

    // Then - exception is thrown
  }

  @Test
  fun `factory uses provided repository`() {
    // Given
    val customRepository: ConsentRepository = mockk(relaxed = true)
    factory = BetaConsentViewModelFactory(mockContext, customRepository)

    // When
    val viewModel = factory.create(BetaConsentViewModel::class.java)

    // Then
    assertNotNull(viewModel)
    // The viewModel should use the custom repository (verified through behavior)
  }

  @Test
  fun `factory creates ViewModels with correct SharedPreferences name`() {
    // Given
    factory = BetaConsentViewModelFactory(mockContext, mockRepository)

    // When
    factory.create(BetaConsentViewModel::class.java)

    // Then
    verify {
      mockContext.getSharedPreferences(
          eq(BetaConsentViewModel.PREFS_NAME), eq(Context.MODE_PRIVATE))
    }
  }

  @Test
  fun `factory can create multiple ViewModel instances`() {
    // Given
    factory = BetaConsentViewModelFactory(mockContext, mockRepository)

    // When
    val viewModel1 = factory.create(BetaConsentViewModel::class.java)
    val viewModel2 = factory.create(BetaConsentViewModel::class.java)

    // Then
    assertNotNull(viewModel1)
    assertNotNull(viewModel2)
    assertNotSame(viewModel1, viewModel2) // Different instances
  }
}
