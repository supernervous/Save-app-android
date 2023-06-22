package net.opendasharchive.openarchive

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.orm.SugarApp
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.publish.PublishService
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import timber.log.Timber

class OpenArchiveApp : SugarApp() {

    private val mCleanInsights = CleanInsightsManager()

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
        Prefs.load(this)

        if (Prefs.useTor) initNetCipher()

        Theme.set(Prefs.theme)
    }

    /**
     * This needs to be called from the foreground (from an activity in the foreground),
     * otherwise, `#startForegroundService` will crash!
     * See
     * https://developer.android.com/guide/components/foreground-services#background-start-restrictions
     */
    fun startUploadService() {
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


    fun hasCleanInsightsConsent(): Boolean? {
        return mCleanInsights.hasConsent()
    }

    fun showCleanInsightsConsent(activity: Activity) {
        mCleanInsights.getConsent(activity)
    }
}