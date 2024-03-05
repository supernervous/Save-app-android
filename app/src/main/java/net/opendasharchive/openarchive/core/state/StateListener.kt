package net.opendasharchive.openarchive.core.state

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class StateListener<T> {
    private val _actions = Channel<T>()
    val actions = _actions.receiveAsFlow()

    suspend fun send(action: T) {
        _actions.send(action)
    }
}
