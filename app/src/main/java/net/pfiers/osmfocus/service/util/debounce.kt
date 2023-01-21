package net.pfiers.osmfocus.service.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class Debouncer(private val delay: Duration, private val scope: CoroutineScope) {
    private val mutex = Mutex()
    private var previousJob: Job? = null

    fun debounce(action: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                previousJob?.cancel()
                previousJob = scope.launch {
                    delay(delay)
                    action()
                }
            }
        }
    }
}
