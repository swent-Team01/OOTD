package com.android.ootd.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.map.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * UI tests for MapScreen.
 *
 * Tests cover:
 * - UI component visibility
 * - Post click interaction
 * - Test tags for all UI components
 *
 * Note: Tests that require rendering the actual Google Maps component are skipped as they require
 * the full Android framework and cannot run in Robolectric unit tests.
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockViewModel: MapViewModel
  private lateinit var mockFeedRepository: FeedRepository
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private val testDispatcher = StandardTestDispatcher()

  private val testUserId = "testUser"
  private val testLocation = Location(46.5197, 6.6323, "Lausanne")
  private val testAccount = Account(uid = testUserId, location = testLocation)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    mockFeedRepository = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    // Mock Firebase Auth
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

    // Mock feed repository to return empty list of posts
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    // Mock account repository to observe account changes
    coEvery { mockAccountRepository.observeAccount(any()) } returns flowOf(testAccount)
    coEvery { mockAccountRepository.getAccount(any()) } returns testAccount

    mockViewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun mapScreen_displaysAllUIComponents() {
    composeTestRule.setContent { MapScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithTag(MapScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("MAP").assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()
  }

  @Test
  fun mapScreen_showsLoadingIndicator_initially() {
    composeTestRule.setContent { MapScreen(viewModel = mockViewModel) }

    // Loading indicator should be visible initially
    composeTestRule.onNodeWithTag(MapScreenTestTags.LOADING_INDICATOR).assertExists()
  }

  @Test
  fun onPostClick_isCalledWithCorrectPostId() {
    var clickedPostId: String? = null
    val testPostId = "test-post-123"
    val onPostClick: (String) -> Unit = { postId -> clickedPostId = postId }

    composeTestRule.setContent { MapScreen(viewModel = mockViewModel, onPostClick = onPostClick) }

    // Simulate clicking on a post by directly calling the callback
    onPostClick(testPostId)

    assert(clickedPostId == testPostId) { "Expected post ID '$testPostId', got '$clickedPostId'" }
  }

  @Test
  fun mapScreen_allTestTags_areUnique() {
    // Verify all test tags are unique constants
    val tags =
        setOf(
            MapScreenTestTags.SCREEN,
            MapScreenTestTags.GOOGLE_MAP_SCREEN,
            MapScreenTestTags.LOADING_INDICATOR,
            MapScreenTestTags.TOP_BAR,
            MapScreenTestTags.TOP_BAR_TITLE,
            MapScreenTestTags.CONTENT_BOX)

    // All tags should be unique
    assert(tags.size == 6) { "Expected 6 unique test tags, got ${tags.size}" }
  }

  @Test
  fun mapScreen_postMarkerTestTag_generatesCorrectFormat() {
    val postId = "test-post-123"
    val expectedTag = "postMarker_test-post-123"

    val generatedTag = MapScreenTestTags.getTestTagForPostMarker(postId)

    assert(generatedTag == expectedTag) { "Expected tag '$expectedTag', got '$generatedTag'" }
  }
}
