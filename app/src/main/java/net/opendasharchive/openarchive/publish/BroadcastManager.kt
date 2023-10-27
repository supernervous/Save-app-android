package net.opendasharchive.openarchive.publish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

object BroadcastManager {

    private const val MEIDA_CHANGE_INTENT = "media_change_intent"
    private const val MEDIA_ID = "media_id"

    fun advertiseChange(context: Context, mediaId: Long) {
        val i = Intent(MEIDA_CHANGE_INTENT)
        i.putExtra(MEDIA_ID, mediaId)

        LocalBroadcastManager.getInstance(context).sendBroadcast(i)
    }

    fun getMediaId(intent: Intent): Long {
        return intent.getLongExtra(MEDIA_ID, -1)
    }

    fun register(context: Context, receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(receiver, IntentFilter(MEIDA_CHANGE_INTENT))
    }

    fun unregister(context: Context, receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
    }
}