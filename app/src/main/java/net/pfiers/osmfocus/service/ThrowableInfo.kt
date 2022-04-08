package net.pfiers.osmfocus.service

import java.io.Serializable
import java.util.*

/**
 * A data class to serialize information about a throwable.
 * Because not all throwables are serializable.
 */
class ThrowableInfo(throwable: Throwable) : Serializable {
    val message = throwable.message
    val qualifiedName = throwable::class.qualifiedName ?: ANONYMOUS_NAME
    val simpleName = throwable::class.simpleName ?: ANONYMOUS_NAME
    val stackTrace: Array<StackTraceElement> = throwable.stackTrace
    val stackTraceAsString = throwable.stackTraceToString()

    override fun equals(other: Any?): Boolean = this === other || (other is ThrowableInfo
            && message == other.message
            && qualifiedName == other.qualifiedName
            && stackTrace.contentEquals(other.stackTrace))

    override fun hashCode() = Objects.hash(message, qualifiedName, stackTrace)

    companion object {
        const val ANONYMOUS_NAME = "<anonymous>"
    }
}
