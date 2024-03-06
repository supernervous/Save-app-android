package net.opendasharchive.openarchive.features.internetarchive.presentation

import net.opendasharchive.openarchive.core.presentation.StatefulViewModel
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveViewModel.Action

class InternetArchiveViewModel(private val space: Space) :
    StatefulViewModel<InternetArchiveState, Action>(InternetArchiveState()) {

    override fun reduce(state: InternetArchiveState, action: Action) = state

    override suspend fun effects(state: InternetArchiveState, action: Action) {
        when (action) {
            is Action.Remove -> {
                space.delete()
                send(action)
            }
            is Action.Cancel -> send(action)
        }
    }

    sealed interface Action {
        data object Remove : Action

        data object Cancel : Action
    }
}
