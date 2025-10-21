package com.android.ootd.model.feed

import com.android.ootd.model.OutfitPost
import com.android.ootd.model.account.Account
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

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

  @get:Rule val mainRule = MainDispatcherRule()

  private fun account(uid: String, friendUids: List<String>) =
      Account(uid = uid, ownerId = uid, username = "U-$uid", friendUids = friendUids)

  private fun post(id: String, uid: String, ts: Long) =
      OutfitPost(
          postUID = id,
          name = "N",
          uid = uid,
          userProfilePicURL = "",
          outfitURL = "url_$id",
          description = "",
          itemsID = emptyList(),
          timestamp = ts)

  @Test
  fun default_state_is_empty() = runTest {
    val vm = FeedViewModel(FakeFeedRepository())
    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
    assertEquals(false, vm.uiState.value.hasPostedToday)
    assertEquals(null, vm.uiState.value.currentAccount)
  }

  @Test
  fun setCurrentUser_not_posted_keeps_feed_empty() = runTest {
    val repo = FakeFeedRepository().apply { setHasPostedToday("me", false) }
    val vm = FeedViewModel(repo)

    vm.setCurrentAccount(account("me", listOf("me")))
    advanceUntilIdle()

    assertEquals(false, vm.uiState.value.hasPostedToday)
    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
  }

  @Test
  fun setCurrentUser_posted_no_friends_results_empty_feed() = runTest {
    val repo = FakeFeedRepository().apply { setHasPostedToday("me", true) }
    val vm = FeedViewModel(repo)

    vm.setCurrentAccount(account("me", emptyList()))
    advanceUntilIdle()

    assertTrue(vm.uiState.value.hasPostedToday)
    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
  }

  @Test
  fun loads_posts_for_distinct_and_nonblank_friends_when_has_posted() = runTest {
    val repo =
        FakeFeedRepository().apply {
          setHasPostedToday("me", true)
          addPost(post("p1", "me", 1))
          addPost(post("p2", "me", 2))
        }
    val vm = FeedViewModel(repo)

    vm.setCurrentAccount(account("me", listOf("me", "  me  ", "")))
    advanceUntilIdle()

    assertEquals(listOf("p1", "p2"), vm.uiState.value.feedPosts.map { it.postUID })
  }

  @Test
  fun on_repo_error_feed_becomes_empty_no_crash() = runTest {
    val repo =
        FakeFeedRepository().apply {
          setHasPostedToday("me", true)
          throwOnGet = true
        }
    val vm = FeedViewModel(repo)

    vm.setCurrentAccount(account("me", listOf("me")))
    advanceUntilIdle()

    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
    assertTrue(vm.uiState.value.hasPostedToday)
  }

  @Test
  fun onPostUploaded_without_current_user_sets_flag_and_clears_feed() = runTest {
    val vm = FeedViewModel(FakeFeedRepository())

    vm.onPostUploaded()
    advanceUntilIdle()

    assertTrue(vm.uiState.value.hasPostedToday)
    assertEquals(emptyList<OutfitPost>(), vm.uiState.value.feedPosts)
  }

  // ------------ inline test helpers ------------

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

  private class FakeFeedRepository : FeedRepository {
    private val postsByUid = mutableMapOf<String, MutableList<OutfitPost>>()
    private val hasPosted = mutableMapOf<String, Boolean>()
    private var idSeq = 0
    var throwOnGet: Boolean = false

    override suspend fun addPost(p: OutfitPost) {
      postsByUid.getOrPut(p.uid) { mutableListOf() }.add(p)
    }

    override fun getNewPostId(): String = "test-${idSeq++}"

    fun setHasPostedToday(uid: String, value: Boolean) {
      hasPosted[uid] = value
    }

    override suspend fun hasPostedToday(userId: String): Boolean {
      return hasPosted[userId] ?: false
    }

    override suspend fun getFeed(): List<OutfitPost> {
      return postsByUid.values
          .flatten()
          .sortedWith(compareBy<OutfitPost> { it.timestamp }.thenBy { it.postUID })
    }

    override suspend fun getFeedForUids(uids: List<String>): List<OutfitPost> {
      if (throwOnGet) throw IllegalStateException("Injected failure")
      // tiny async to exercise coroutine scheduling deterministically
      delay(1)
      return uids
          .asSequence()
          .flatMap { (postsByUid[it] ?: emptyList()).asSequence() }
          .sortedWith(compareBy<OutfitPost> { it.timestamp }.thenBy { it.postUID })
          .toList()
    }
  }
}
