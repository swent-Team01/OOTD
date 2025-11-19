package com.android.ootd.model.image

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Provides CoroutineDispatchers for various threading needs. */
interface DispatcherProvider {
  val main: CoroutineDispatcher
  val io: CoroutineDispatcher
  val default: CoroutineDispatcher
  val unconfined: CoroutineDispatcher
}

/** Default implementation of [DispatcherProvider] using standard Dispatchers. */
object DefaultDispatcherProvider : DispatcherProvider {
  override val main: CoroutineDispatcher = Dispatchers.Main

  override val io: CoroutineDispatcher = Dispatchers.IO

  override val default: CoroutineDispatcher = Dispatchers.Default

  override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
