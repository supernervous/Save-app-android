package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import android.os.Bundle
import net.opendasharchive.openarchive.db.Space

data class InternetArchiveLoginState(
    val email: String = "",
    val password: String = "",
    val isEmailError: Boolean = false,
    val isPasswordError: Boolean = false,
    val isLoginError: Boolean = false,
)

const val ARG_VAL_NEW_SPACE = -1L
const val ARG_SPACE = "space"
fun Bundle?.getSpace(type: Space.Type): Pair<Space, Boolean> {
    val mSpaceId = this?.getLong(ARG_SPACE, ARG_VAL_NEW_SPACE) ?: ARG_VAL_NEW_SPACE

    val isNewSpace = ARG_VAL_NEW_SPACE == mSpaceId

    return if (isNewSpace) {
        Pair(Space(type), true)
    } else {
        Space.get(mSpaceId)?.let { Pair(it, false) } ?: Pair(Space(type), true)
    }
}
