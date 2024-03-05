package net.opendasharchive.openarchive.features.internetarchive.presentation

import net.opendasharchive.openarchive.core.presentation.StatefulViewModel
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveViewModel.Action

class InternetArchiveViewModel(private val space: Space) :
    StatefulViewModel<InternetArchiveState, Action>(InternetArchiveState()) {

    override fun reduce(state: InternetArchiveState, action: Action) = when (action) {
        else -> state
    }

    override suspend fun effects(state: InternetArchiveState, action: Action) {
        when (action) {
            else -> Unit
        }
    }

    sealed interface Action {

    }
}
