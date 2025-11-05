package com.android.ootd.model.feed

import com.android.ootd.model.account.Account
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.feed.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Unit tests for FeedViewModel using a fake repository. Tests ViewModel state management and
 * business logic without Firebase dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

  @get:Rule val mainRule = MainDispatcherRule()

  // -------- Initial state --------

  @Test
  fun initialState_isEmpty() = runTest {
    val vm = FeedViewModel(FakeFeedRepository())

    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
    assertEquals(false, vm.uiState.value.hasPostedToday)
    assertEquals(null, vm.uiState.value.currentAccount)
  }

  // -------- Account setup scenarios --------

  @Test
  fun setAccount_notPostedToday_feedRemainsEmpty() = runTest {
    val repo = FakeFeedRepository(hasPosted = false)
    val vm = FeedViewModel(repo)

    vm.setCurrentAccount(account("me", friends = listOf("me")))
    advanceUntilIdle()

    assertEquals(false, vm.uiState.value.hasPostedToday)
    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
  }

  @Test
  fun setAccount_postedButNoFriends_feedIsEmpty() = runTest {
    val repo = FakeFeedRepository(hasPosted = true)
    val vm = FeedViewModel(repo)

    vm.setCurrentAccount(account("me", friends = emptyList()))
    advanceUntilIdle()

    assertTrue(vm.uiState.value.hasPostedToday)
    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
  }

  @Test
  fun setAccount_postedWithFriends_loadsFeedAndDeduplicates() = runTest {
    val repo =
        FakeFeedRepository(
            hasPosted = true,
            posts = listOf(post("p1", uid = "me", ts = 1), post("p2", uid = "me", ts = 2)))
    val vm = FeedViewModel(repo)

    // Friends list has duplicates and blanks - should be cleaned up
    vm.setCurrentAccount(account("me", friends = listOf("me", "  me  ", "")))
    advanceUntilIdle()

    assertEquals(listOf("p1", "p2"), vm.uiState.value.feedPosts.map { it.postUID })
    assertTrue(vm.uiState.value.hasPostedToday)
  }

  // -------- Error handling --------

  @Test
  fun repositoryError_feedBecomesEmpty_noException() = runTest {
    val repo = FakeFeedRepository(hasPosted = true, throwOnGet = true)
    val vm = FeedViewModel(repo)

    vm.setCurrentAccount(account("me", friends = listOf("me")))
    advanceUntilIdle()

    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
    assertTrue(vm.uiState.value.hasPostedToday)
  }

  // -------- Post upload callback --------

  @Test
  fun onPostUploaded_setsFlagAndClearsFeed() = runTest {
    val vm = FeedViewModel(FakeFeedRepository())

    vm.onPostUploaded()
    advanceUntilIdle()

    assertTrue(vm.uiState.value.hasPostedToday)
    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
  }

  // -------- Test helpers --------

  private fun account(uid: String, friends: List<String>) =
      Account(uid = uid, ownerId = uid, username = "User-$uid", friendUids = friends)

  private fun post(id: String, uid: String, ts: Long) =
      OutfitPost(
          postUID = id,
          name = "Post $id",
          ownerId = uid,
          userProfilePicURL = "https://example.com/$uid.jpg",
          outfitURL = "https://example.com/outfit_$id.jpg",
          description = "Description for $id",
          itemsID = emptyList(),
          timestamp = ts)

  @OptIn(ExperimentalCoroutinesApi::class)
  class MainDispatcherRule(private val dispatcher: TestDispatcher = StandardTestDispatcher()) :
      TestRule {
    override fun apply(base: Statement, description: Description): Statement {
      return object : Statement() {
        override fun evaluate() {
          Dispatchers.setMain(dispatcher)
          try {
            base.evaluate()
          } finally {
            Dispatchers.resetMain()
          }
        }
      }
    }
  }

  /** Fake repository for testing ViewModel logic without Firebase dependencies */
  private class FakeFeedRepository(
      private val hasPosted: Boolean = false,
      private val posts: List<OutfitPost> = emptyList(),
      val throwOnGet: Boolean = false
  ) : FeedRepository {

    private val allPosts = posts.toMutableList()
    private var idCounter = 0

    override suspend fun addPost(p: OutfitPost) {
      allPosts.add(p)
    }

    override fun getNewPostId(): String = "test-post-${idCounter++}"

    override suspend fun hasPostedToday(userId: String): Boolean = hasPosted

    override suspend fun getFeedForUids(uids: List<String>): List<OutfitPost> {
      if (throwOnGet) throw IllegalStateException("Simulated repository failure")

      delay(1) // Simulate async operation for coroutine testing

      return allPosts
          .filter { it.ownerId in uids }
          .sortedWith(compareBy<OutfitPost> { it.timestamp }.thenBy { it.postUID })
    }
  }
}
