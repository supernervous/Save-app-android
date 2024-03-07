package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
data class InternetArchiveLoginState(
    val email: String = "",
    val password: String = "",
    val isEmailError: Boolean = false,
    val isPasswordError: Boolean = false,
    val isLoginError: Boolean = false,
    val isBusy: Boolean = false
)
