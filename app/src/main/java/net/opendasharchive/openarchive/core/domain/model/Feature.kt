package net.opendasharchive.openarchive.core.domain.model

import android.app.Activity

interface Feature {

    fun load()

    fun unload()

    suspend fun onLoad(activity: Activity)

    suspend fun onUnload(activity: Activity)
}
