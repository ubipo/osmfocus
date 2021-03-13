package net.pfiers.osmfocus.extensions

import java.io.File


// See https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io.path/java.nio.file.-path/div.html and https://docs.python.org/3/library/pathlib.html#operators
operator fun File.div(extraPart: String): File = File(this, extraPart)
