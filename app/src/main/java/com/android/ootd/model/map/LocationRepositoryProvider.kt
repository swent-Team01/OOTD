package com.android.ootd.model.map

import com.android.ootd.HttpClientProvider

/**
 * Singleton provider for [LocationRepository].
 *
 * This allows tests to inject mock implementations while the app uses the real implementation.
 * Similar pattern to UserRepositoryProvider and AccountRepositoryProvider.
 */
object LocationRepositoryProvider {
  var repository: LocationRepository = NominatimLocationRepository(HttpClientProvider.client)
}
