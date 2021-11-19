package net.pfiers.osmfocus.service.util

val Int.nullIfNegative: Int?
    get() = if (this < 0) null else this

fun Any?.discard() = Unit

fun discard(block: () -> Unit) = block()
