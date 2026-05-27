package net.pfiers.osmfocus.view.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import net.pfiers.osmfocus.view.screens.AboutScreen
import net.pfiers.osmfocus.view.screens.AddUserBaseMapScreen
import net.pfiers.osmfocus.view.screens.BaseMapsScreen
import net.pfiers.osmfocus.view.screens.ElementDetailsRouteScreen
import net.pfiers.osmfocus.view.screens.MapScreen
import net.pfiers.osmfocus.view.screens.MoreInfoScreen
import net.pfiers.osmfocus.view.screens.NoteDetailsRouteScreen
import net.pfiers.osmfocus.view.screens.SettingsScreen

@Serializable
sealed interface AppNavKey : NavKey

@Serializable
data object MapRoute : AppNavKey

@Serializable
data object SettingsRoute : AppNavKey

@Serializable
data object BaseMapsRoute : AppNavKey

@Serializable
data object AddUserBaseMapRoute : AppNavKey

@Serializable
data object AboutRoute : AppNavKey

@Serializable
data object MoreInfoRoute : AppNavKey

@Serializable
data class ElementDetailsRoute(
    val elementType: String,
    val elementId: Long,
) : AppNavKey

@Serializable
data class NoteDetailsRoute(
    val noteId: Long,
) : AppNavKey

private const val navTransitionDurationMillis = 400

class Navigator(private val backStack: NavBackStack<NavKey>) {
    fun navigate(route: NavKey) {
        backStack.add(route)
    }

    fun goBack(): Boolean {
        if (backStack.size <= 1) {
            return false
        }
        backStack.removeLastOrNull()
        return true
    }
}

private fun slideForwardTransition(): ContentTransform =
    slideInHorizontally(
        animationSpec = tween(navTransitionDurationMillis),
        initialOffsetX = { fullWidth -> fullWidth },
    ) togetherWith slideOutHorizontally(
        animationSpec = tween(navTransitionDurationMillis),
        targetOffsetX = { fullWidth -> -fullWidth },
    )

private fun slideBackTransition(): ContentTransform =
    slideInHorizontally(
        animationSpec = tween(navTransitionDurationMillis),
        initialOffsetX = { fullWidth -> -fullWidth },
    ) togetherWith slideOutHorizontally(
        animationSpec = tween(navTransitionDurationMillis),
        targetOffsetX = { fullWidth -> fullWidth },
    )

@Composable
fun OsmFocusNavHost(
    onRequireOsmAccessToken: (Int, (String) -> Unit) -> Unit,
    onExitApp: () -> Unit,
) {
    val backStack = rememberNavBackStack(MapRoute)
    val navigator = remember(backStack) { Navigator(backStack) }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<MapRoute> {
            MapScreen(
                onRequireOsmAccessToken = onRequireOsmAccessToken,
                onShowSettings = { navigator.navigate(SettingsRoute) },
                onShowElementDetails = { typedId ->
                    navigator.navigate(
                        ElementDetailsRoute(
                            elementType = typedId.type.nameLower,
                            elementId = typedId.id,
                        ),
                    )
                },
                onShowNoteDetails = { noteId -> navigator.navigate(NoteDetailsRoute(noteId)) },
            )
        }
        entry<SettingsRoute> {
            SettingsScreen(
                onShowBaseMaps = { navigator.navigate(BaseMapsRoute) },
                onShowAbout = { navigator.navigate(AboutRoute) },
            )
        }
        entry<BaseMapsRoute> {
            BaseMapsScreen(
                onNavigateUp = { navigator.goBack() },
                onAddBaseMap = { navigator.navigate(AddUserBaseMapRoute) },
            )
        }
        entry<AddUserBaseMapRoute> {
            AddUserBaseMapScreen(onNavigateUp = { navigator.goBack() })
        }
        entry<AboutRoute> {
            AboutScreen(
                onNavigateUp = { navigator.goBack() },
                onShowMoreInfo = { navigator.navigate(MoreInfoRoute) },
            )
        }
        entry<MoreInfoRoute> {
            MoreInfoScreen(onNavigateUp = { navigator.goBack() })
        }
        entry<ElementDetailsRoute> { route ->
            ElementDetailsRouteScreen(
                route = route,
                onNavigateUp = { navigator.goBack() },
            )
        }
        entry<NoteDetailsRoute> { route ->
            NoteDetailsRouteScreen(
                route = route,
                onNavigateUp = { navigator.goBack() },
            )
        }
    }

    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider,
    )

    NavDisplay(
        entries = entries,
        transitionSpec = { slideForwardTransition() },
        popTransitionSpec = { slideBackTransition() },
        predictivePopTransitionSpec = { slideBackTransition() },
        onBack = {
            if (!navigator.goBack()) {
                onExitApp()
            }
        },
    )
}

