package net.pfiers.osmfocus.service.osm

import java.util.*
import kotlin.reflect.KClass

//class UnknownElementTypeException(override val message: String): RuntimeException(message)
//
//enum class ElementType {
//    NODE,
//    WAY,
//    RELATION;
//
//    val lower get() = name.lowercase(Locale.ROOT)
//    val capitalized get() = lower.replaceFirstChar { it.titlecase(Locale.ROOT) }
//    val oneLetter get() = name[0]
//
//    companion object {
//        fun fromLetter(letter: Char) = when (letter) {
//            'n' -> NODE
//            'w' -> WAY
//            'r' -> RELATION
//            else -> throw UnknownElementTypeException("ElementType for letter: $letter")
//        }
//
//        fun fromClass(kClass: KClass<out Element>) = when (kClass) {
//            Node::class -> NODE
//            Way::class -> WAY
//            Relation::class -> RELATION
//            else -> throw UnknownElementTypeException("ElementType for class $kClass")
//        }
//
//        fun fromString(string: String) = fromLetter(string[0].lowercaseChar())
//    }
//}
