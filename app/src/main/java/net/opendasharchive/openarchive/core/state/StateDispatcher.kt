package net.opendasharchive.openarchive.core.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

typealias Reducer<T, A> = (T, A) -> T
typealias Effect<T, A> = suspend (T, A) -> Unit

typealias Dispatch<A> = (A) -> Unit

class StateDispatcher<T, A>(
    private val scope: CoroutineScope,
    initialState: T,
    private val reducer: Reducer<T, A>,
    private val effects: Effect<T, A>
) {
    private val _state = MutableStateFlow(initialState)
    val state = _state

    fun dispatch(action: A) {
        val state = _state.updateAndGet { reducer(it, action) }
        scope.launch(Dispatchers.Default) {
            effects(state, action)
        }
    }
}
