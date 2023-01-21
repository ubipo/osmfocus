package net.pfiers.osmfocus.service.util

import kotlin.math.min


fun <T> Iterable<T>.ensureOne(): T {
    val iter = iterator()
    val elem = iter.next()
    if (iter.hasNext())
        throw Exception("Expected only one element")
    return elem
}

fun <K, V> Iterable<Pair<K, V>>.toLinkedMap(): LinkedHashMap<K, V> {
    return LinkedHashMap<K, V>().also { it.putAll(this) }
}

fun <T> List<T>.subList(fromIndex: Int) =
    subList(fromIndex, size)

fun <T> List<T>.boundedSubList(fromIndex: Int) =
    subList(min(size, fromIndex), size)

fun <T> List<T>.boundedSubList(fromIndex: Int, toIndex: Int) =
    subList(fromIndex, min(size, toIndex))

fun <T, S> Collection<T>.cartesianProduct(other: Iterable<S>): List<Pair<T, S>> {
    return flatMap { first -> other.map { second -> Pair(first, second) } }
}

/**
 * I will give €1 to whoever can make this functionally satisfying.
 *
 * In:
 * a a a b b c a c a b c c c...
 * 1 2 3 4 5 5 4 4 5 3 3 1 2...
 *
 * Select first (a, 1) -> can't reuse a, nor 1 -> skip (a, 2), (a, 3) -> select (b, 4) ...
 *
 * Out:
 * a b c...
 * 1 4 5...
 */
fun <A, B> Iterable<Pair<A, B>>.noIndividualValueReuse(): List<Pair<A, B>> {
    val processedAs = HashSet<A>()
    val processedBs = HashSet<B>()

    return mapNotNull { (a, b) ->
        if (a !in processedAs && b !in processedBs) {
            processedAs.add(a)
            processedBs.add(b)
            Pair(a, b)
        } else null
    }
}

fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V>?): Map<K, V> =
    mapOf(*pairs.filterNotNull().toTypedArray())

fun <K, V> mergeMapsBy(a: Map<K, V>, b: Map<K, V>, decider: (a: V, b: V) -> V) = buildMap {
    mergeMapsBy(a, b, decider, this::put)
}

fun <K, V> mergeMapsBy(a: Map<K, V>, b: Map<K, V>, decider: (a: V, b: V) -> V, put: (K, V) -> Unit) {
    for (key in a.keys + b.keys) {
        val aVal = a[key]
        val bVal = b[key]
        val mergedVal = when {
            aVal == null -> bVal!!
            bVal == null -> aVal
            else -> decider(aVal, bVal)
        }
        put(key, mergedVal)
    }
}

fun <T : Any> cycle(vararg xs: T): Sequence<T> {
    var i = 0
    return generateSequence { xs[i++ % xs.size] }
}
