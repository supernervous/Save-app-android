package net.opendasharchive.openarchive

import android.app.Activity
import android.content.Context
import android.util.Log
import com.maxkeppeler.sheets.info.InfoSheet
import org.cleaninsights.sdk.*
import java.io.IOException
import java.util.ArrayList

open class CleanInsightsManager {

    val CI_CAMPAIGN = "upload-failures"

    private var mMeasure: CleanInsights? = null
    private var mHasConsent = true

    fun initMeasurement(context : Context) {
        if (mMeasure == null) {
            // Instantiate with configuration and directory to write store to, best in an `Application` subclass.
            try {

                mMeasure = CleanInsights(
                    context.assets.open("cleaninsights.json").reader().readText(),
                    context.filesDir
                )

                mHasConsent = mMeasure!!.isCampaignCurrentlyGranted(CI_CAMPAIGN)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        mMeasure?.testServer {
            Log.d("Clean Insights", "test server: " + it.toString())
        }
    }

    fun hasConsent() : Boolean? {
        return mMeasure?.isCampaignCurrentlyGranted(CI_CAMPAIGN)
    }

    public fun getConsent(context: Activity) {

        var success = mMeasure!!.requestConsent(CI_CAMPAIGN!!, object : JavaConsentRequestUi {
            override fun show(
                s: String,
                campaign: Campaign,
                consentRequestUiCompletionHandler: ConsentRequestUiCompletionHandler
            ) {

                InfoSheet().show(context) {
                    title(context.getString(R.string.ci_title))
                    content(context.getString(R.string.clean_insight_consent_prompt))
                    onNegative(context.getString(R.string.ci_negative)) {
                        // Handle event
                        consentRequestUiCompletionHandler.completed(false)
                    }
                    onPositive(context.getString(R.string.ci_confirm)) {
                        // Handle event
                        mHasConsent = true
                        consentRequestUiCompletionHandler.completed(true)
                        mMeasure!!.grant(CI_CAMPAIGN)

                    }
                }

            }

            override fun show(
                feature: Feature,
                consentRequestUiCompletionHandler: ConsentRequestUiCompletionHandler
            ) {

            }


        })

        return success
    }

    fun measureView(view: String) {
        if (mMeasure != null && mHasConsent) {
            val alPath = ArrayList<String>()
            alPath.add(view)
            mMeasure!!.measureVisit(alPath, CI_CAMPAIGN)
            mMeasure!!.persist()
        }
    }

    fun measureEvent(key: String?, value: String?) {
        if (mMeasure != null && mHasConsent) {
            mMeasure!!.measureEvent(key!!, value!!, CI_CAMPAIGN)
            mMeasure!!.persist()
        }
    }




}
