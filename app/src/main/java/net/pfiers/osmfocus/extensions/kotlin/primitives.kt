package net.pfiers.osmfocus.extensions.kotlin

val Int.nullIfNegative: Int?
    get() = if (this < 0) null else this
