package net.pfiers.osmfocus.view.support

import kotlinx.coroutines.CoroutineExceptionHandler

interface ExceptionHandler {
    fun handleException(ex: Throwable)

    val coroutineExceptionHandler: CoroutineExceptionHandler
}
