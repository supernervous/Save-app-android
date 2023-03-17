package net.opendasharchive.openarchive

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.orm.SugarApp
import com.orm.SugarRecord.findById
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.publish.PublishService
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Prefs.TRACK_LOCATION
import net.opendasharchive.openarchive.util.Prefs.getCurrentSpaceId
import timber.log.Timber

class OpenArchiveApp : SugarApp() {

    private val mCleanInsights = CleanInsightsManager()
    private var mCurrentSpace: Space? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

    }

    override fun onCreate() {
        super.onCreate()

        val config = ImagePipelineConfig.newBuilder(this)
            .setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
            .setResizeAndRotateEnabledForNetwork(true)
            .setDownsampleEnabled(true)
            .build()

        Fresco.initialize(this, config)
        Prefs.setContext(this)

        //disable proofmode GPS dat tracking by default
        Prefs.putBoolean(TRACK_LOCATION, false)

        if (Prefs.getUseTor()) initNetCipher()

        uploadQueue()
    }

    fun uploadQueue() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, PublishService::class.java))
        } else {
            startService(Intent(this, PublishService::class.java))
        }
    }

    private fun initNetCipher() {
        Timber.d( "Initializing NetCipher client")
        val oh = OrbotHelper.get(this)

        if (BuildConfig.DEBUG) {
            oh.skipOrbotValidation()
        }

        oh.init()
    }


    @Synchronized
    fun getCurrentSpace(): Space? {
        if (mCurrentSpace == null) {
            val spaceId = getCurrentSpaceId()
            if (spaceId != -1L) {
                mCurrentSpace = findById(
                    Space::class.java, spaceId
                )
            }
        }
        return mCurrentSpace
    }

    fun hasCleanInsightsConsent(): Boolean? {
        return mCleanInsights.hasConsent()
    }

    fun showCleanInsightsConsent(activity: Activity) {
        mCleanInsights.getConsent(activity)
    }
}