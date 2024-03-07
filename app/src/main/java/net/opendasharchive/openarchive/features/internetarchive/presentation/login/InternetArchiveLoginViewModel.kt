package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import net.opendasharchive.openarchive.core.presentation.StatefulViewModel
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.domain.usecase.InternetArchiveLoginUseCase
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.Cancel
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.CreateLogin
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.ErrorFade
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.Login
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.LoginError
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.LoginSuccess
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdateEmail
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdatePassword
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

class InternetArchiveLoginViewModel(
    private val space: Space,
) : StatefulViewModel<InternetArchiveLoginState, Action>(InternetArchiveLoginState()), KoinComponent {

    private val loginUseCase: InternetArchiveLoginUseCase by inject {
        parametersOf(space)
    }

    override fun reduce(
        state: InternetArchiveLoginState,
        action: Action
    ): InternetArchiveLoginState = when (action) {
        is UpdateEmail -> state.copy(email = action.value)
        is UpdatePassword -> state.copy(password = action.value)
        is Login -> state.copy(isBusy = true)
        is LoginError -> state.copy(isLoginError = true, isBusy = false)
        is LoginSuccess, is Cancel -> state.copy(isBusy = false)
        is ErrorFade -> state.copy(isLoginError = false)
        else -> state
    }

    override suspend fun effects(state: InternetArchiveLoginState, action: Action) {
        when (action) {
            is Login ->
                loginUseCase(state.email, state.password)
                    .onSuccess { ia ->
                        send(LoginSuccess(ia))
                    }
                    .onFailure { dispatch(LoginError(it)) }

            is CreateLogin, is Cancel -> send(action)
            else -> Unit
        }
    }

    sealed interface Action {
        data object Login : Action

        data object Cancel : Action

        data class LoginSuccess(val value: InternetArchive) : Action

        data class LoginError(val value: Throwable) : Action

        data object ErrorFade : Action

        data object CreateLogin : Action {
            const val URI = "https://archive.org/account/signup"
        }

        data class UpdateEmail(val value: String) : Action
        data class UpdatePassword(val value: String) : Action
    }

}
