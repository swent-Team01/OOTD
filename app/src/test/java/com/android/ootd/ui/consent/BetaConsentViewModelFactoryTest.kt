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
  fun `factory creates ViewModel and uses application context with correct preferences`() {
    // When
    factory = BetaConsentViewModelFactory(mockContext, mockRepository)
    val viewModel = factory.create(BetaConsentViewModel::class.java)

    // Then
    assertNotNull(viewModel)
    verify { mockContext.applicationContext }
    verify {
      mockContext.getSharedPreferences(
          eq(BetaConsentViewModel.PREFS_NAME), eq(Context.MODE_PRIVATE))
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `factory throws exception for unknown ViewModel class`() {
    factory = BetaConsentViewModelFactory(mockContext, mockRepository)
    class UnknownViewModel : ViewModel()
    factory.create(UnknownViewModel::class.java)
  }

  @Test
  fun `factory uses provided repository and creates multiple instances`() {
    // Test with custom repository
    val customRepository: ConsentRepository = mockk(relaxed = true)
    factory = BetaConsentViewModelFactory(mockContext, customRepository)
    val viewModel = factory.create(BetaConsentViewModel::class.java)
    assertNotNull(viewModel)

    // Test creating multiple instances
    factory = BetaConsentViewModelFactory(mockContext, mockRepository)
    val viewModel1 = factory.create(BetaConsentViewModel::class.java)
    val viewModel2 = factory.create(BetaConsentViewModel::class.java)
    assertNotNull(viewModel1)
    assertNotNull(viewModel2)
    assertNotSame(viewModel1, viewModel2)
  }
}
