package io.scal.secureshareui.controller

import android.os.Message

///changes
interface SiteControllerListener {
    fun success(msg: Message?)
    fun progress(msg: Message?)
    fun failure(msg: Message?)
}