package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.state.StateDispatcher
import net.opendasharchive.openarchive.core.state.StateListener
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchiveAuth
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository.InternetArchiveRepository
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.CreateLogin
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.Login
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.LoginError
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.LoginSuccess
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdateEmail
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdatePassword

class InternetArchiveLoginViewModel(
    private val repository: InternetArchiveRepository
) : ViewModel() {
    private val dispatcher = StateDispatcher(InternetArchiveLoginState(), ::reduce, ::effects)
    private val listener = StateListener<Action>()

    val state = dispatcher.state
    val effects = listener.actions

    private fun reduce(state: InternetArchiveLoginState, action: Action): InternetArchiveLoginState = when(action) {
        is UpdateEmail -> state.copy(email = action.value)
        is UpdatePassword -> state.copy(password = action.value)
        is LoginError -> state.copy(isLoginError = true)
        is LoginSuccess -> state.copy(auth = action.value)
        else -> state
    }

    private suspend fun effects(state: InternetArchiveLoginState, action: Action) {
        when(action) {
            is Login -> withContext(Dispatchers.IO) {
                repository.login(state.email, state.password)
                    .onSuccess {
                        dispatcher.dispatch(LoginSuccess(it))
                    }.onFailure {
                        dispatcher.dispatch(LoginError(it))
                    }
            }
            is CreateLogin -> listener.send(action)
            else -> Unit
        }
    }

    fun dispatch(action: Action) {
        viewModelScope.launch {
            dispatcher.dispatch(action)
        }
    }

    sealed interface Action {
        data object Login: Action

        data class LoginSuccess(val value: InternetArchiveAuth): Action

        data class LoginError(val value: Throwable): Action

        data object CreateLogin: Action {
            const val URI = "https://archive.org/account/signup"
        }

        data class UpdateEmail(val value: String): Action
        data class UpdatePassword(val value: String): Action
    }
}
