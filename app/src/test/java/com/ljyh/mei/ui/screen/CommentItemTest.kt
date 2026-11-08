package com.ljyh.mei.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ljyh.mei.data.model.domain.CommentEntry
import com.ljyh.mei.data.model.domain.CommentUser
import com.ljyh.mei.data.model.domain.FloorCommentEntry
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.screen.comment.CommentReplies
import com.ljyh.mei.ui.screen.comment.component.CommentItem
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h800dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class CommentItemTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun openingSecondThreadKeepsFirstOpenUntilManuallyCollapsed() {
        val first = CommentEntry(1, "Comment A", user, null, false, 0, 1, 0, "Now")
        val second = first.copy(id = 2, content = "Comment B", replyCount = 2)
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        val replies = CommentReplies(scope) { _, id, _, _ -> Resource.Success(listOf(reply(id))) }
        TabletContentTestActivity.content = {
            val expanded by replies.expanded.collectAsState()
            MaterialTheme {
                Column {
                    for (comment in listOf(first, second)) {
                        CommentItem(
                            comment = comment,
                            replies = expanded[comment.id],
                            onToggleFloor = { id, count -> replies.toggle("song", id, count) },
                            onRetryFloor = replies::retry,
                            onShowMoreFloor = replies::showMore,
                            modifier = Modifier.testTag("comment-${comment.id}"),
                        )
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.onNodeWithText("展开 1 条回复").performClick()
            compose.onNodeWithText("Reply 1").assertIsDisplayed()
            compose.onNodeWithText("展开 2 条回复").performClick()
            compose.onNodeWithText("Reply 1").assertIsDisplayed()
            compose.onNodeWithText("Reply 2").assertIsDisplayed()

            compose.onNode(hasText("收起回复") and hasAnyAncestor(hasTestTag("comment-1"))).performClick()
            compose.onNodeWithText("Reply 1").assertDoesNotExist()
            compose.onNodeWithText("Reply 2").assertIsDisplayed()
        } finally {
            scope.cancel()
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test
    fun openThreadsKeepSeparateRequestsAndPagination() = runBlocking {
        val nextPage = CompletableDeferred<Resource<List<FloorCommentEntry>>>()
        val secondPage = CompletableDeferred<Resource<List<FloorCommentEntry>>>()
        val requests = mutableListOf<Pair<Long, Long>>()
        val replies = CommentReplies(this) { _, id, _, cursor ->
            requests += id to cursor
            when {
                id == 2L -> secondPage.await()
                cursor == -1L -> Resource.Success((1L..6L).map(::reply))
                else -> nextPage.await()
            }
        }
        replies.toggle("song", 1, 12)
        replies.toggle("song", 2, 2)
        yield()
        yield()
        assertEquals(listOf(1L to -1L, 2L to -1L, 1L to 6L), requests)
        assertEquals(Resource.Loading, replies.expanded.value.getValue(2).comments)

        replies.showMore(1)
        assertTrue(replies.expanded.value.getValue(1).waitingForMore)
        assertFalse(replies.expanded.value.getValue(2).waitingForMore)
        nextPage.complete(Resource.Success(listOf(reply(7))))
        yield()
        assertEquals(Resource.Success((1L..7L).map(::reply)), replies.expanded.value.getValue(1).comments)

        replies.toggle("song", 2, 2)
        secondPage.complete(Resource.Success(listOf(reply(99))))
        yield()
        assertEquals(setOf(1L), replies.expanded.value.keys)
        replies.collapseAll()
        assertTrue(replies.expanded.value.isEmpty())
    }

    private val user = CommentUser("", "Listener")

    private fun reply(id: Long) = FloorCommentEntry("Reply $id", user, "", 0, "Now", id)
}
