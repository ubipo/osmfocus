package net.pfiers.osmfocus.service.util

fun Any?.discard() = Unit

fun discard(block: () -> Unit) = block()
