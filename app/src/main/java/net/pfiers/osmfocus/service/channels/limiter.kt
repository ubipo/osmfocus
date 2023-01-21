package net.pfiers.osmfocus.service.channels

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.Duration

class LimiterChannel(
    requestChannel: SendChannel<Unit>,
    grantChannel: ReceiveChannel<CompletableJob>
): SendChannel<Unit> by requestChannel, ReceiveChannel<CompletableJob> by grantChannel {
    fun requestRun() = trySend(Unit).getOrThrow()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun limiter(
    minimumDelay: Duration,
    scope: CoroutineScope
): LimiterChannel {
    val requestChannel = Channel<Unit>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val grantChannel = scope.produce(capacity = 0) {
        requestChannel.receiveAsFlow().conflate().collect {
            val job = Job()
            send(job)
            job.join()
            delay(minimumDelay)
        }
    }
    return LimiterChannel(requestChannel, grantChannel)
}
