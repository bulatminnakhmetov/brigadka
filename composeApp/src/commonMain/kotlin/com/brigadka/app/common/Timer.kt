package com.brigadka.app.common

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = Logger.withTag("Timer")

class Timer(
    private val start: Instant,
    private val scope: CoroutineScope
) {
    // Public read-only flow of the elapsed duration.
    private val _elapsed = MutableStateFlow(Duration.ZERO)
    val elapsed: StateFlow<Duration> = _elapsed.asStateFlow()

    private var tickerJob: Job? = null

    /** Start emitting ticks.  */
    fun start() {
        if (tickerJob?.isActive == true) return        // already running
        tickerJob = scope.launch {
            while (isActive) {
                _elapsed.value = Clock.System.now() - start
                delay(1.seconds)
                logger.d { "Elapsed time: ${_elapsed.value.inWholeMilliseconds} ms" }
            }
        }
    }

    /** Stop the ticks. */
    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
