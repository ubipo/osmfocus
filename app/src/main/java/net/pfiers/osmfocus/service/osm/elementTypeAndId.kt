package net.pfiers.osmfocus.service.osm

import java.io.Serializable
import java.net.URL
import java.util.*
import kotlin.reflect.KClass

/* ElementType is represented by an Enum and not by the actual Element classes (Node, Way, Relation)
because using the class would necessitate using KClass. Using KClass is not nice because it is not
Serializable. Using it would mean we have to implement Parcelable for all Element classes (Relation
uses ElementType in RelationMember). */
enum class ElementType {
    NODE, WAY, RELATION;

    val nameLower by lazy { name.lowercase() }
    val nameCapitalized by lazy {
        nameLower.replaceFirstChar {
            it.titlecase(Locale.ROOT)
        }
    }
}

data class TypedId(val id: Long, val type: ElementType) : Serializable {
    val url get() = URL("https://osm.org/${type.nameLower}/$id")
}

open class ElementAndId<T : Element>(
    val id: Long,
    val element: T
) : Serializable {
    val e = element
    val typedId = TypedId(id, element.type)
}

typealias AnyElementAndId = ElementAndId<*>

class ElementCentroidAndId<T : Element>(
    id: Long,
    element: T,
    val centroid: Coordinate
) : ElementAndId<T>(id, element), Serializable

typealias AnyElementCentroidAndId = ElementCentroidAndId<*>

val KClass<out Element>.name
    get() = simpleName!!.lowercase()

fun elementTypeFromString(string: String) = try {
    elementTypeFromChar(string[0])
} catch (ex: UnknownElementTypeException) {
    throw UnknownElementTypeException("From string \"$string\"")
}

fun elementTypeFromChar(char: Char) = when (char.lowercaseChar()) {
    'n' -> ElementType.NODE
    'w' -> ElementType.WAY
    'r' -> ElementType.RELATION
    else -> throw UnknownElementTypeException("From char \"$char\"")
}

class UnknownElementTypeException(message: String?) : RuntimeException(message)
