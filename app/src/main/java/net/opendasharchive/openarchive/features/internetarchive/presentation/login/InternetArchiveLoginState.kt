package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchiveAuth

data class InternetArchiveLoginState(
    val email: String = "",
    val password: String = "",
    val isEmailError: Boolean = false,
    val isPasswordError: Boolean = false,
    val isLoginError: Boolean = false,
    val auth: InternetArchiveAuth? = null
)
