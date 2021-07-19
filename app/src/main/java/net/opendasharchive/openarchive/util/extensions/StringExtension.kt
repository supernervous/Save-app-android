package net.opendasharchive.openarchive.util.extensions

import android.text.TextUtils

fun String.isEmailValid(): Boolean {
    return !TextUtils.isEmpty(this)
}

fun String.isPasswordValid(): Boolean {
    return this.isNotEmpty()
}