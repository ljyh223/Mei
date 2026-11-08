package com.ljyh.mei.ui.screen.comment

import com.ljyh.mei.data.model.domain.FloorCommentEntry
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class CommentReplyState(
    val replyCount: Int,
    val comments: Resource<List<FloorCommentEntry>> = Resource.Loading,
    val hasMore: Boolean = false,
    val waitingForMore: Boolean = false,
    val loadMoreError: Boolean = false,
)

/** Owns the open reply threads for one comment list, confined to the ViewModel's main scope. */
internal class CommentReplies(
    private val scope: CoroutineScope,
    private val loadPage: suspend (String, Long, Int, Long) -> Resource<List<FloorCommentEntry>>,
) {
    private class Thread(val songId: String, val commentId: Long) {
        var job: Job? = null
        var nextTime = -1L
        var prefetched: List<FloorCommentEntry>? = null
        var serverHasMore = false
    }

    private val threads = mutableMapOf<Long, Thread>()
    private val _expanded = MutableStateFlow<Map<Long, CommentReplyState>>(emptyMap())
    val expanded = _expanded.asStateFlow()

    fun toggle(songId: String, commentId: Long, replyCount: Int) {
        val previous = threads.remove(commentId)
        if (previous != null) {
            previous.job?.cancel()
            _expanded.value -= commentId
            return
        }
        val thread = Thread(songId, commentId)
        threads[commentId] = thread
        _expanded.value += commentId to CommentReplyState(replyCount)
        request(thread, initial = true)
    }

    fun collapseAll() {
        threads.values.forEach { it.job?.cancel() }
        threads.clear()
        _expanded.value = emptyMap()
    }

    fun retry(commentId: Long) {
        val thread = threads[commentId] ?: return
        if (state(thread).comments !is Resource.Error) return
        update(thread, state(thread).copy(comments = Resource.Loading))
        request(thread, initial = true)
    }

    fun showMore(commentId: Long) {
        val thread = threads[commentId] ?: return
        val current = state(thread)
        if (!current.hasMore) return
        val prefetched = thread.prefetched
        if (prefetched != null) {
            reveal(thread, prefetched)
        } else {
            update(thread, current.copy(waitingForMore = true, loadMoreError = false))
            if (thread.job?.isActive != true) request(thread, initial = false)
        }
    }

    private fun state(thread: Thread) = _expanded.value.getValue(thread.commentId)

    private fun update(thread: Thread, state: CommentReplyState) {
        _expanded.value += thread.commentId to state
    }

    private fun reveal(thread: Thread, page: List<FloorCommentEntry>) {
        thread.prefetched = null
        val current = state(thread)
        val existing = (current.comments as? Resource.Success)?.data.orEmpty()
        update(thread, current.copy(
            comments = Resource.Success(existing + page),
            hasMore = thread.serverHasMore,
            waitingForMore = false,
            loadMoreError = false,
        ))
        if (thread.serverHasMore) request(thread, initial = false)
    }

    private fun request(thread: Thread, initial: Boolean) {
        val cursor = thread.nextTime
        // Store the job before it runs, since revealing a page can start the next prefetch.
        thread.job = scope.launch(start = CoroutineStart.LAZY) {
            val result = loadPage(thread.songId, thread.commentId, PAGE_SIZE, cursor)
            ensureActive()
            when (result) {
                is Resource.Success -> {
                    val page = result.data
                    val lastTime = page.lastOrNull()?.time ?: -1L
                    thread.serverHasMore = page.size == PAGE_SIZE && lastTime > 0 && lastTime != cursor
                    thread.nextTime = lastTime
                    if (initial || state(thread).waitingForMore) {
                        reveal(thread, page)
                    } else {
                        thread.prefetched = page
                        update(thread, state(thread).copy(hasMore = page.isNotEmpty()))
                    }
                }
                is Resource.Error -> {
                    val current = state(thread)
                    update(thread, if (initial) current.copy(comments = result)
                    else current.copy(waitingForMore = false, loadMoreError = true))
                }
                Resource.Loading -> Unit
            }
        }
        thread.job?.start()
    }

    private companion object {
        const val PAGE_SIZE = 6
    }
}
