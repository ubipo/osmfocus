package net.pfiers.osmfocus.kotlin

import kotlin.math.min


fun <T> Iterable<T>.ensureOne(): T {
    val iter = iterator()
    val elem = iter.next()
    if (iter.hasNext())
        throw Exception("Expected only one element")
    return elem
}

fun <K, V> Iterable<Pair<K, V>>.toLinkedMap(): LinkedHashMap<K, V> {
    return toMap(LinkedHashMap<K, V>().apply { putAll(this) })
}

fun <T> List<T>.subList(fromIndex: Int) =
    subList(fromIndex, size)

fun <T> List<T>.containedSubList(fromIndex: Int, toIndex: Int) =
    subList(fromIndex, min(size, toIndex))
