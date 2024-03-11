package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import androidx.compose.runtime.Immutable

@Immutable
data class InternetArchiveLoginState(
    val username: String = "",
    val password: String = "",
    val isUsernameError: Boolean = false,
    val isPasswordError: Boolean = false,
    val isLoginError: Boolean = false,
    val isBusy: Boolean = false,
    val isValid: Boolean = false,
)
