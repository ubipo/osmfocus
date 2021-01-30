package net.pfiers.osmfocus

import com.google.common.base.Stopwatch
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration


private val tu = TimeUnit.NANOSECONDS

@ExperimentalTime
val Stopwatch.elapsed get() =
    elapsed(tu).toDuration(tu)

fun Stopwatch.restart(): Stopwatch {
    if (isRunning)
        stop()
    reset()
    start()
    return this
}

@Suppress("UnstableApiUsage")
fun <T> EventBus.listen(listener: (T) -> Unit) = register(object {
    @Subscribe
    fun recordCustomerChange(e: T) {
        listener(e)
    }
})
