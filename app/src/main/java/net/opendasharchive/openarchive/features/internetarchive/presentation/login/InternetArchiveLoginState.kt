package net.opendasharchive.openarchive.features.internetarchive.presentation.login

data class InternetArchiveLoginState(
    val email: String = "",
    val password: String = "",
    val isEmailError: Boolean = false,
    val isPasswordError: Boolean = false,
    val isLoginError: Boolean = false,
    val isBusy: Boolean = false
)
