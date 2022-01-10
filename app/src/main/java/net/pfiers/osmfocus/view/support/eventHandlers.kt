package net.pfiers.osmfocus.view.support

import android.os.Bundle
import androidx.navigation.NavController
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.view.fragments.ElementDetailsFragment
import net.pfiers.osmfocus.view.fragments.NoteDetailsFragment
import net.pfiers.osmfocus.viewmodel.support.*

fun handleNavEvent(event: NavEvent, navController: NavController) {
    when (event) {
        is ShowElementDetailsEvent -> navController.navigate(
            R.id.elementDetailContainerFragment,
            Bundle().apply {
                putSerializable(
                    ElementDetailsFragment.ARG_ELEMENT_AND_CENTROID_AND_ID,
                    event.elementCentroidAndId
                )
            }
        )
        is ShowNoteDetailsEvent -> navController.navigate(
            R.id.noteDetailsFragment,
            Bundle().apply {
                putSerializable(
                    NoteDetailsFragment.ARG_NOTE_AND_ID,
                    event.noteAndId
                )
            }
        )
        is ShowSettingsEvent -> navController.navigate(R.id.settingsContainerFragment)
        is EditBaseMapsEvent -> navController.navigate(R.id.userBaseMapsFragment)
        is AddBaseMapEvent -> navController.navigate(R.id.addUserBaseMapFragment)
        is ShowAboutEvent -> navController.navigate(R.id.aboutFragment)
        is ShowMoreInfoEvent -> navController.navigate(R.id.moreInfoFragment)
        is NavigateUpEvent -> navController.navigateUp()
        else -> error("Unhandled NavEvent: $event")
    }
}
