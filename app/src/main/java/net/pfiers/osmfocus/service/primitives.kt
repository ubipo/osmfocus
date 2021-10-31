package net.pfiers.osmfocus.service

import android.icu.util.LocaleData
import android.icu.util.ULocale
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*

val Int.nullIfNegative: Int?
    get() = if (this < 0) null else this

fun Any?.discard() = Unit

fun discard(block: () -> Unit) = block()
