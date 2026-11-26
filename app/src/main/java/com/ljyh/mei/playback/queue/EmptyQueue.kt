package com.ljyh.mei.playback.queue

object EmptyQueue : Queue {
    override val id: String = "empty_queue"
    override val title: String? = null
    override suspend fun getInitialStatus() = Queue.Status(null, emptyList(), -1)
}
