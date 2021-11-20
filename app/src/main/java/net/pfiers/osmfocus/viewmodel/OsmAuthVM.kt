package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.oauth.OsmAuthRepository
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

class OsmAuthVM(
    private val osmAuthRepository: OsmAuthRepository
) : ViewModel() {
    val events = createEventChannel()


}
