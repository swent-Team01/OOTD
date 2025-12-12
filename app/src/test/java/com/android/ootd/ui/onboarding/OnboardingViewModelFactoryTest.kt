package com.android.ootd.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelFactoryTest {

  private lateinit var mockContext: Context
  private lateinit var mockSharedPreferences: SharedPreferences
  private lateinit var factory: OnboardingViewModelFactory

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockSharedPreferences = mockk(relaxed = true)

    every { mockContext.applicationContext } returns mockContext
    every {
      mockContext.getSharedPreferences(OnboardingViewModel.PREFS_NAME, Context.MODE_PRIVATE)
    } returns mockSharedPreferences
  }

  @Test
  fun `factory creates ViewModel and uses application context with correct preferences`() {
    // When
    factory = OnboardingViewModelFactory(mockContext)
    val viewModel = factory.create(OnboardingViewModel::class.java)

    // Then
    assertNotNull(viewModel)
    verify { mockContext.applicationContext }
    verify {
      mockContext.getSharedPreferences(eq(OnboardingViewModel.PREFS_NAME), eq(Context.MODE_PRIVATE))
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `factory throws exception for unknown ViewModel class`() {
    factory = OnboardingViewModelFactory(mockContext)
    class UnknownViewModel : ViewModel()
    factory.create(UnknownViewModel::class.java)
  }

  @Test
  fun `factory uses provided repository and creates multiple instances`() {
    // Test creating multiple instances
    factory = OnboardingViewModelFactory(mockContext)
    val viewModel1 = factory.create(OnboardingViewModel::class.java)
    val viewModel2 = factory.create(OnboardingViewModel::class.java)
    assertNotNull(viewModel1)
    assertNotNull(viewModel2)
    assertNotSame(viewModel1, viewModel2)
  }
}
