package net.opendasharchive.openarchive.features.internetarchive.presentation

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.presentation.StatefulViewModel
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository.InternetArchiveRepository
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveViewModel.Action

class InternetArchiveViewModel(
    private val repository: InternetArchiveRepository,
    private val space: Space
) : StatefulViewModel<InternetArchiveState, Action>(InternetArchiveState()) {

    init {
        viewModelScope.launch {
            repository.subscribe().collect {
                dispatch(Action.Loaded(it))
            }
        }
    }

    override fun reduce(state: InternetArchiveState, action: Action) = when(action) {
        is Action.Loaded -> state.copy(
            userName = action.value.userName,
            email = action.value.email,
            screenName = action.value.screenName
        )
        else -> state
    }

    override suspend fun effects(state: InternetArchiveState, action: Action) {
        when (action) {
            is Action.Remove -> {
                space.delete()
                send(action)
            }

            is Action.Cancel -> send(action)
            else -> Unit
        }
    }

    sealed interface Action {

        data class Loaded(val value: InternetArchive) : Action

        data object Remove : Action

        data object Cancel : Action
    }
}
