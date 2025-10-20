package com.thevoidkeeper.skflipper.util

import kotlinx.coroutines.delay
import java.util.concurrent.Semaphore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RateLimiter(
    maxConcurrent: Int,
    private val minDelay: Duration
) {
    private val sem = Semaphore(maxConcurrent, true)
    @Volatile private var lastTime = 0L

    suspend fun <T> run(block: suspend () -> T): T {
        sem.acquire()
        try {
            val now = System.currentTimeMillis()
            val wait = (lastTime + minDelay.inWholeMilliseconds) - now
            if (wait > 0) delay(wait)
            val result = block()
            lastTime = System.currentTimeMillis()
            return result
        } finally {
            sem.release()
        }
    }
}
