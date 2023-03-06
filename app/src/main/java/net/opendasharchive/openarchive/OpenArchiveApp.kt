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
import info.guardianproject.netcipher.client.StrongBuilder
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.publish.PublishService
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Prefs.TRACK_LOCATION
import net.opendasharchive.openarchive.util.Prefs.getCurrentSpaceId
import okhttp3.OkHttpClient
import timber.log.Timber

class OpenArchiveApp : SugarApp() {

    @Volatile
    var orbotConnected = false
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

        if (getUseTor() && OrbotHelper.isOrbotInstalled(this))
            initNetCipher(this)

        uploadQueue()
    }

    fun uploadQueue() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, PublishService::class.java))
        } else {
            startService(Intent(this, PublishService::class.java))
        }
    }

    private fun initNetCipher(context: Context) {
        Timber.d( "Initializing NetCipher client")
        val appContext = context.applicationContext
        val oh = OrbotHelper.get(appContext)
        if (BuildConfig.DEBUG) {
            oh.skipOrbotValidation()
        }
        if (oh.init()) {
            orbotConnected = true
        }
        try {
            StrongOkHttpClientBuilder.forMaxSecurity(appContext)
                .build(object : StrongBuilder.Callback<OkHttpClient?> {
                    override fun onConnected(okHttpClient: OkHttpClient?) {
                        orbotConnected = true
                        Timber.tag("NetCipherClient").d( "Connection to orbot established!")
                    }

                    override fun onConnectionException(exc: Exception) {
                        orbotConnected = false
                        Timber.tag("NetCipherClient").d( "onConnectionException() $exc")
                    }

                    override fun onTimeout() {
                        orbotConnected = false
                        Timber.tag("NetCipherClient").d( "onTimeout()")
                    }

                    override fun onInvalid() {
                        orbotConnected = false
                        Timber.tag("NetCipherClient").d( "onInvalid()")
                    }
                })
        } catch (exc: Exception) {
            Timber.tag("Error").d( "Error while initializing TOR Proxy OkHttpClient $exc")
        }
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

    private fun getUseTor(): Boolean {
        return orbotConnected
    }

    fun hasCleanInsightsConsent(): Boolean? {
        return mCleanInsights.hasConsent()
    }

    fun showCleanInsightsConsent(activity: Activity) {
        mCleanInsights.getConsent(activity)
    }
}