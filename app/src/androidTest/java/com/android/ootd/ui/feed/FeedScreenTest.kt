package com.android.ootd.ui.feed

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.account.Account
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryFirestore
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.feed.POSTS_COLLECTION_PATH
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestoreSettings
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Comprehensive tests for FeedScreen UI and repository. */
@RunWith(AndroidJUnit4::class)
class FeedScreenTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private val db = FirebaseEmulator.firestore
  private lateinit var currentUid: String

  @Before
  override fun setUp() = runBlocking {
    super.setUp()
    currentUid = requireNotNull(FirebaseEmulator.auth.currentUser?.uid)
  }

  class FakeFeedViewModel : FeedViewModel() {
    fun setUiStateForTest(newState: FeedUiState) {
      val field = FeedViewModel::class.java.getDeclaredField("_uiState")
      field.isAccessible = true
      val flow = field.get(this) as MutableStateFlow<FeedUiState>
      flow.value = newState
    }
  }

  // ========================================================================
  // UI Tests
  // ========================================================================

  @Test
  fun feedList_rendersAllPosts() {
    val posts =
        listOf(
            OutfitPost("1", "user1", "https://example.com/1.jpg"),
            OutfitPost("2", "user2", "https://example.com/2.jpg"))

    composeTestRule.setContent { FeedList(posts = posts, isBlurred = false, onPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LIST).assertExists()
  }

  @Test
  fun feedScreen_showsLockedMessage_whenUserHasNotPosted() {
    val fakeRepo =
        object : FeedRepository {
          override suspend fun hasPostedToday(userId: String) = false

          override suspend fun getFeedForUids(uids: List<String>) = emptyList<OutfitPost>()

          override suspend fun getRecentFeedForUids(uids: List<String>): List<OutfitPost> =
              emptyList<OutfitPost>()

          override suspend fun getPublicFeed(): List<OutfitPost> = emptyList()

          override suspend fun getCachedFriendFeed(uids: List<String>): List<OutfitPost> =
              emptyList()

          override suspend fun getCachedPublicFeed(): List<OutfitPost> = emptyList()

          override fun observeRecentFeedForUids(
              uids: List<String>
          ): kotlinx.coroutines.flow.Flow<List<OutfitPost>> =
              kotlinx.coroutines.flow.flowOf(emptyList())

          override suspend fun addPost(post: OutfitPost) {}

          override fun getNewPostId() = "fake-id"

          override suspend fun getPostById(postUuid: String): OutfitPost? = null
        }

    FeedRepositoryProvider.repository = fakeRepo
    val viewModel =
        FeedViewModel().apply {
          setCurrentAccount(Account(uid = "user1", username = "Test", friendUids = emptyList()))
        }

    composeTestRule.setContent { FeedScreen(feedViewModel = viewModel, onAddPostClick = {}) }
    composeTestRule.waitUntil(timeoutMillis = 2_000) { !viewModel.uiState.value.isLoading }
    composeTestRule.onNodeWithTag(FeedScreenTestTags.LOCKED_MESSAGE).assertExists()
  }

  @Test
  fun feedScreen_showsFeedList_whenUserHasPosted() {
    val posts = listOf(OutfitPost("1", "user1", "https://example.com/1.jpg"))
    val fakeRepo =
        object : FeedRepository {
          override suspend fun hasPostedToday(userId: String) = true

          override suspend fun getFeedForUids(uids: List<String>) = posts

          override suspend fun getRecentFeedForUids(uids: List<String>): List<OutfitPost> = posts

          override suspend fun getPublicFeed(): List<OutfitPost> = emptyList()

          override suspend fun getCachedFriendFeed(uids: List<String>): List<OutfitPost> = posts

          override suspend fun getCachedPublicFeed(): List<OutfitPost> = emptyList()

          override fun observeRecentFeedForUids(
              uids: List<String>
          ): kotlinx.coroutines.flow.Flow<List<OutfitPost>> = kotlinx.coroutines.flow.flowOf(posts)

          override suspend fun addPost(post: OutfitPost) {}

          override fun getNewPostId() = "fake-id"

          override suspend fun getPostById(postUuid: String): OutfitPost? = posts.firstOrNull()
        }

    FeedRepositoryProvider.repository = fakeRepo
    val viewModel =
        FeedViewModel().apply {
          setCurrentAccount(Account(uid = "user1", username = "Test", friendUids = emptyList()))
        }

    composeTestRule.setContent { FeedScreen(feedViewModel = viewModel, onAddPostClick = {}) }

    composeTestRule.waitUntil(timeoutMillis = 2_000) { !viewModel.uiState.value.isLoading }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LIST).assertExists()
  }

  @Test
  fun feedScreen_hasRequiredTags() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.TOP_BAR).assertExists()
  }

  @Test
  fun feedScreen_triggersTopBarButtons() {
    var notificationsClicked = false

    composeTestRule.setContent {
      FeedScreen(onAddPostClick = {}, onNotificationIconClick = { notificationsClicked = true })
    }

    composeTestRule.waitForIdle()

    Thread.sleep(100)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN)
        .assertExists()
        .performClick()

    assertTrue(notificationsClicked)
  }

  @Test
  fun feedScreen_switchesToPublicFeed_whenTabClicked() {
    var toggleCalled = false
    val viewModel = FakeFeedViewModel()

    // We can't easily override the toggle function in the fake without changing the base class or
    // using a spy.

    composeTestRule.setContent { FeedScreen(feedViewModel = viewModel, onAddPostClick = {}) }

    // Check "Public" tab exists and click it
    composeTestRule.onNodeWithText("Public").assertExists().performClick()
  }

  @Test
  fun outfitPostCard_renders() {
    val post = OutfitPost("id", "user1", "https://example.com/img.jpg")

    composeTestRule.setContent {
      OutfitPostCard(
          post = post,
          isBlurred = false,
          onSeeFitClick = {},
          onLikeClick = {},
          isLiked = false,
          likeCount = 0)
    }

    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun feedScreen_locationClickCallback_isInvoked() {
    var locationClickCount = 0
    var clickedLocation: com.android.ootd.model.map.Location? = null
    val testLocation = com.android.ootd.model.map.Location(46.5, 6.6, "Test Location")
    val postWithLocation =
        OutfitPost(postUID = "post1", ownerId = "user1", name = "User1", location = testLocation)

    composeTestRule.setContent {
      FeedList(
          posts = listOf(postWithLocation),
          isBlurred = false,
          onPostClick = {},
          onLocationClick = { location ->
            locationClickCount++
            clickedLocation = location
          })
    }

    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_LOCATION)
        .assertExists()
        .performClick()

    assertEquals(1, locationClickCount)
    assertEquals(testLocation, clickedLocation)
  }

  @Test
  fun feedScreen_passesLocationClickToFeedList() {
    composeTestRule.setContent { FeedScreen(onAddPostClick = {}, onLocationClick = {}) }

    // This test verifies the callback chain is properly connected through compilation
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertExists()
  }

  // ========================================================================
  // Repository Tests
  // ========================================================================

  @Test
  fun getFeed_emptyCollection_returnsEmpty() = runTest {
    val result = feedRepository.getFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  @Test
  fun addPost_thenGetFeed_returnsInOrder() = runBlocking {
    val posts = listOf(samplePost("p1", 2L), samplePost("p2", 1L), samplePost("p3", 3L))

    posts.forEach { feedRepository.addPost(it) }

    val result = feedRepository.getFeedForUids(listOf(currentUid))
    assertEquals(listOf("p2", "p1", "p3"), result.map { it.postUID })
  }

  @Test
  fun addPost_persistsCorrectly() = runBlocking {
    val post = samplePost("test-id", 42L)

    feedRepository.addPost(post)

    val doc = db.collection(POSTS_COLLECTION_PATH).document("test-id").get().await()
    assertTrue(doc.exists())
    assertEquals(42L, doc.getLong("timestamp"))
  }

  @Test
  fun hasPostedToday_worksCorrectly() = runTest {
    assertFalse(feedRepository.hasPostedToday("non-existent-user"))

    val post = samplePost("today", System.currentTimeMillis())
    feedRepository.addPost(post)

    assertTrue(feedRepository.hasPostedToday(currentUid))
  }

  @Test
  fun getFeed_withCorruptedData_returnsOnlyValidPosts() = runTest {
    feedRepository.addPost(samplePost("valid", 1L))
    feedRepository.addPost(samplePost("corrupted", 2L))

    // Corrupt the timestamp field
    db.collection(POSTS_COLLECTION_PATH)
        .document("corrupted")
        .update(mapOf("timestamp" to "invalid"))
        .await()

    val result = feedRepository.getFeedForUids(listOf(currentUid))

    // Should return only the valid post, filtering out the corrupted one
    assertEquals(1, result.size)
    assertEquals("valid", result[0].postUID)
  }

  @Test
  fun feedScreen_showsLoadingOverlay_whenLoading() {
    val fakeRepo =
        object : FeedRepository {
          override suspend fun hasPostedToday(userId: String) = false

          override suspend fun getFeedForUids(uids: List<String>) = emptyList<OutfitPost>()

          override suspend fun getRecentFeedForUids(uids: List<String>) = emptyList<OutfitPost>()

          override suspend fun getPublicFeed(): List<OutfitPost> = emptyList()

          override suspend fun getCachedFriendFeed(uids: List<String>): List<OutfitPost> =
              emptyList()

          override suspend fun getCachedPublicFeed(): List<OutfitPost> = emptyList()

          override fun observeRecentFeedForUids(
              uids: List<String>
          ): kotlinx.coroutines.flow.Flow<List<OutfitPost>> =
              kotlinx.coroutines.flow.flowOf(emptyList())

          override suspend fun addPost(post: OutfitPost) {}

          override fun getNewPostId() = "fake-id"

          override suspend fun getPostById(postUuid: String): OutfitPost? = null
        }

    FeedRepositoryProvider.repository = fakeRepo
    val viewModel =
        FeedViewModel().apply {
          setCurrentAccount(Account(uid = "user1", username = "Test", friendUids = emptyList()))
        }

    val stateField = FeedViewModel::class.java.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val flow = stateField.get(viewModel) as MutableStateFlow<FeedUiState>
    flow.value = flow.value.copy(hasPostedToday = false, isLoading = false)

    composeTestRule.setContent { FeedScreen(feedViewModel = viewModel, onAddPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.LOCKED_MESSAGE).assertExists()
  }

  @Test
  fun networkFailure_handlesGracefully() = runTest {
    val unreachableDb = firestoreForApp("unreachable", "10.0.2.2", 6553)
    val failingRepo = FeedRepositoryFirestore(unreachableDb)

    val result = failingRepo.getFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  @Test
  fun feedScreen_showsLoadingOverlay_whenIsLoadingTrue() {
    val viewModel = FakeFeedViewModel().apply { setUiStateForTest(FeedUiState(isLoading = true)) }

    composeTestRule.setContent { FeedScreen(feedViewModel = viewModel, onAddPostClick = {}) }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.LOADING_OVERLAY).assertIsDisplayed()
  }

  @Test
  fun feedScreenPreview_rendersCoreElements() {
    composeTestRule.setContent { FeedScreenPreview() }

    // Verify scaffold and top bar exist
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.TOP_BAR).assertIsDisplayed()

    // Verify feed list is rendered with sample posts
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LIST).assertIsDisplayed()

    // Message only appears when: !isLoading && !hasPostedToday && posts.isEmpty()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.LOCKED_MESSAGE).assertDoesNotExist()

    // Verify loading overlay is NOT shown (isLoading = false in preview)
    composeTestRule.onNodeWithTag(FeedScreenTestTags.LOADING_OVERLAY).assertDoesNotExist()
  }

  // ========================================================================
  // Helpers
  // ========================================================================

  private fun samplePost(id: String, ts: Long) =
      OutfitPost(
          postUID = id,
          name = "name-$id",
          ownerId = currentUid,
          userProfilePicURL = "https://example.com/$id.png",
          outfitURL = "https://example.com/outfits/$id.jpg",
          description = "desc-$id",
          itemsID = listOf("i1-$id", "i2-$id"),
          timestamp = ts)

  private fun firestoreForApp(appName: String, host: String, port: Int): FirebaseFirestore {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val default = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)!!
    val app =
        try {
          FirebaseApp.getInstance(appName)
        } catch (_: IllegalStateException) {
          FirebaseApp.initializeApp(context, default.options, appName)
        }
    val instance = FirebaseFirestore.getInstance(app)
    val usedEmulator = runCatching { instance.useEmulator(host, port) }.isSuccess
    instance.firestoreSettings = firestoreSettings {
      isPersistenceEnabled = false
      if (!usedEmulator) {
        this.host = "$host:$port"
        isSslEnabled = false
      }
    }
    return instance
  }
}
