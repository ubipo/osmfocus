package net.pfiers.osmfocus.service

import java.io.Serializable
import java.util.*

/**
 * A data class to contain information about a throwable.
 * Because not all throwables are serializable.
 */
class ThrowableInfo(throwable: Throwable) : Serializable {
    val message = throwable.message
    val qualifiedName = throwable::class.qualifiedName
    val simpleName = throwable::class.simpleName
    val stackTrace: Array<StackTraceElement> = throwable.stackTrace
    val stackTraceAsString = throwable.stackTraceToString()

    override fun equals(other: Any?): Boolean = this === other || (other is ThrowableInfo
            && message == other.message
            && qualifiedName == other.qualifiedName
            && stackTrace.contentEquals(other.stackTrace))

    override fun hashCode() = Objects.hash(message, qualifiedName, stackTrace)
}
