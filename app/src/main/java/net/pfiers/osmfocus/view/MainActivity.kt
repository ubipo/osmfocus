package net.pfiers.osmfocus.view

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.appcompat.app.AppCompatActivity
import net.pfiers.osmfocus.view.support.OsmFocusTheme
import net.pfiers.osmfocus.view.support.UncaughtExceptionHandler.Companion.uncaughtExceptionHandler
import net.pfiers.osmfocus.view.support.timberInit
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        timberInit()

        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    OsmFocusTheme {
                        OsmFocusApp(onExitApp = ::finish)
                    }
                }
            }
        )
    }
}
