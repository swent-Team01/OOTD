package com.android.ootd.ui.map

import androidx.lifecycle.ViewModel
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.map.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MapViewModelFactory.
 *
 * Tests cover:
 * - Creating MapViewModel with a focus location
 * - Creating MapViewModel without a focus location (default behavior)
 * - Error handling for invalid ViewModel class types
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelFactoryTest {

  private lateinit var mockFeedRepository: FeedRepository
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private val testDispatcher = StandardTestDispatcher()

  private val testUserId = "testUser123"
  private val testFocusLocation = Location(46.5197, 6.6323, "Lausanne")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    mockFeedRepository = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    // Mock Firebase
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    every { mockFirebaseUser.uid } returns testUserId

    // Mock addAuthStateListener to invoke the callback immediately
    every { mockFirebaseAuth.addAuthStateListener(any()) } answers
        {
          val listener = firstArg<FirebaseAuth.AuthStateListener>()
          listener.onAuthStateChanged(mockFirebaseAuth)
          mockk(relaxed = true)
        }

    // Mock repositories
    every { mockAccountRepository.observeAccount(any()) } returns flowOf(mockk(relaxed = true))
    every { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun factory_withFocusLocation_createsViewModelWithFocusLocation() {
    val factory =
        MapViewModelFactory(
            focusLocation = testFocusLocation,
            initialMapType = MapType.FRIENDS_POSTS,
            feedRepository = mockFeedRepository,
            accountRepository = mockAccountRepository)

    val viewModel = factory.create(MapViewModel::class.java)

    assertNotNull(viewModel)
    // Verify the ViewModel has the correct focus location by checking its state
    assertEquals(testFocusLocation, viewModel.uiState.value.focusLocation)
  }

  @Test
  fun factory_withoutFocusLocation_createsViewModelWithNullFocus() {
    val factory =
        MapViewModelFactory(
            focusLocation = null,
            initialMapType = MapType.FRIENDS_POSTS,
            feedRepository = mockFeedRepository,
            accountRepository = mockAccountRepository)

    val viewModel = factory.create(MapViewModel::class.java)

    assertNotNull(viewModel)
    // Verify the ViewModel has null focus location (will use user location)
    assertEquals(null, viewModel.uiState.value.focusLocation)
  }

  @Test
  fun factory_defaultConstructor_createsViewModelWithNullFocus() {
    val factory =
        MapViewModelFactory(
            focusLocation = null,
            initialMapType = MapType.FRIENDS_POSTS,
            feedRepository = mockFeedRepository,
            accountRepository = mockAccountRepository)

    val viewModel = factory.create(MapViewModel::class.java)

    assertNotNull(viewModel)
    assertEquals(null, viewModel.uiState.value.focusLocation)
  }

  @Test(expected = IllegalArgumentException::class)
  fun factory_withInvalidViewModelClass_throwsException() {
    val factory =
        MapViewModelFactory(
            focusLocation = testFocusLocation,
            initialMapType = MapType.FRIENDS_POSTS,
            feedRepository = mockFeedRepository,
            accountRepository = mockAccountRepository)

    // Try to create a different ViewModel type - should throw
    factory.create(InvalidViewModel::class.java)
  }

  @Test
  fun factory_createsMultipleInstances_withSameFocusLocation() {
    val factory =
        MapViewModelFactory(
            focusLocation = testFocusLocation,
            initialMapType = MapType.FRIENDS_POSTS,
            feedRepository = mockFeedRepository,
            accountRepository = mockAccountRepository)

    val viewModel1 = factory.create(MapViewModel::class.java)
    val viewModel2 = factory.create(MapViewModel::class.java)

    // Both should be valid MapViewModel instances
    assertNotNull(viewModel1)
    assertNotNull(viewModel2)

    // Both should have the same focus location
    assertEquals(testFocusLocation, viewModel1.uiState.value.focusLocation)
    assertEquals(testFocusLocation, viewModel2.uiState.value.focusLocation)
  }

  /** Dummy ViewModel class for testing invalid class handling */
  class InvalidViewModel : ViewModel()
}
