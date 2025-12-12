package com.android.ootd.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating OnboardingViewModel instances with proper dependency injection.
 *
 * This factory ensures SharedPreferences is properly injected without holding a Context reference
 * in the ViewModel.
 *
 * @param context Application context (safe to use as it lives for the app lifetime)
 */
class OnboardingViewModelFactory(context: Context) : ViewModelProvider.Factory {

  private val sharedPreferences: SharedPreferences =
      context.applicationContext.getSharedPreferences(
          OnboardingViewModel.PREFS_NAME, Context.MODE_PRIVATE)

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
      return OnboardingViewModel(sharedPreferences) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
