package com.android.ootd.ui.consent

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.ootd.model.consent.ConsentRepository
import com.android.ootd.model.consent.ConsentRepositoryProvider

/**
 * Factory for creating BetaConsentViewModel instances with proper dependency injection.
 *
 * This factory ensures SharedPreferences is properly injected without holding a Context reference
 * in the ViewModel.
 *
 * @param context Application context (safe to use as it lives for the app lifetime)
 * @param consentRepository Repository for consent operations
 */
class BetaConsentViewModelFactory(
    context: Context,
    private val consentRepository: ConsentRepository = ConsentRepositoryProvider.repository
) : ViewModelProvider.Factory {

  private val sharedPreferences: SharedPreferences =
      context.applicationContext.getSharedPreferences(
          BetaConsentViewModel.PREFS_NAME, Context.MODE_PRIVATE)

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(BetaConsentViewModel::class.java)) {
      return BetaConsentViewModel(sharedPreferences, consentRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
