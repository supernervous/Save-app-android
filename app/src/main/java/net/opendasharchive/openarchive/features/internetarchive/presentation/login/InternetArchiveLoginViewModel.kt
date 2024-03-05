package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.presentation.StatefulViewModel
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository.InternetArchiveRepository
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.CreateLogin
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.Login
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.LoginError
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.LoginSuccess
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdateEmail
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdatePassword

class InternetArchiveLoginViewModel(
    private val repository: InternetArchiveRepository,
    private val space: Space,
) : StatefulViewModel<InternetArchiveLoginState, Action>(InternetArchiveLoginState()) {

    override fun reduce(
        state: InternetArchiveLoginState,
        action: Action
    ): InternetArchiveLoginState = when (action) {
        is UpdateEmail -> state.copy(email = action.value)
        is UpdatePassword -> state.copy(password = action.value)
        is LoginError -> state.copy(isLoginError = true)
        else -> state
    }

    override suspend fun effects(state: InternetArchiveLoginState, action: Action) {
        when (action) {
            is Login -> withContext(Dispatchers.IO) {
                repository.login(state.email, state.password)
                    .onSuccess {
                        space.username = it.auth.access
                        space.password = it.auth.secret
                        space.save()
                        send(LoginSuccess(it))
                    }.onFailure {
                        dispatch(LoginError(it))
                    }
            }

            is CreateLogin -> send(action)
            else -> Unit
        }
    }

    sealed interface Action {
        data object Login : Action

        data class LoginSuccess(val value: InternetArchive) : Action

        data class LoginError(val value: Throwable) : Action

        data object CreateLogin : Action {
            const val URI = "https://archive.org/account/signup"
        }

        data class UpdateEmail(val value: String) : Action
        data class UpdatePassword(val value: String) : Action
    }
}
