package net.opendasharchive.openarchive.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.opendasharchive.openarchive.core.state.StateDispatcher
import net.opendasharchive.openarchive.core.state.StateListener

abstract class StatefulViewModel<State, Action>(
    initialState: State,
) : ViewModel() {
    private val dispatcher =
        StateDispatcher(viewModelScope, initialState, ::reduce, ::effects)
    private val listener = StateListener<Action>()

    val state = dispatcher.state
    val effects = listener.actions

    abstract fun reduce(state: State, action: Action): State

    abstract suspend fun effects(state: State, action: Action)

    fun dispatch(action: Action) = dispatcher.dispatch(action)

    suspend fun send(action: Action) = listener.send(action)
}
