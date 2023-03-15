package net.opendasharchive.openarchive.services
import android.os.Message

interface SiteControllerListener {
    fun success(msg: Message?)
    fun progress(msg: Message?)
    fun failure(msg: Message?)
}