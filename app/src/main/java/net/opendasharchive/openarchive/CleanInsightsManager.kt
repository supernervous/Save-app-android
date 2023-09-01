package net.opendasharchive.openarchive

import android.app.Activity
import android.content.Context
import com.maxkeppeler.sheets.info.InfoSheet
import org.cleaninsights.sdk.*

@Suppress("unused")
object CleanInsightsManager  {

    private const val CI_CAMPAIGN = "main"

    private var mCi: CleanInsights? = null

    fun init(context: Context) {
        mCi = CleanInsights(
            context.assets.open("cleaninsights.json").reader().use { it.readText() },
            context.filesDir)
    }

    fun hasConsent(): Boolean {
        return mCi?.isCampaignCurrentlyGranted(CI_CAMPAIGN) ?: false
    }

    fun getConsent(context: Activity, completed: (granted: Boolean) -> Unit) {
        if (mCi == null) {
            return completed(false)
        }

        mCi?.requestConsent(CI_CAMPAIGN, object : ConsentRequestUi {
            override fun show(
                campaignId: String,
                campaign: Campaign,
                complete: ConsentRequestUiComplete
            ) {
                // TODO: See iOS - this screen needs to get way better.
                InfoSheet().show(context) {
                    title(context.getString(R.string.ci_title))
                    content(context.getString(R.string.clean_insight_consent_prompt))
                    onNegative(context.getString(R.string.ci_negative)) {
                        complete(false)
                    }
                    onPositive(context.getString(R.string.ci_confirm)) {
                        complete(true)
                    }
                }
            }

            override fun show(feature: Feature, complete: ConsentRequestUiComplete) {
                complete(true)
            }
        }, completed)
    }

    fun measureView(view: String) {
        mCi?.measureVisit(listOf(view), CI_CAMPAIGN)
    }

    fun measureEvent(category: String, action: String, name: String? = null, value: Double? = null) {
        mCi?.measureEvent(category, action, CI_CAMPAIGN, name, value)
    }

    fun persist() {
        mCi?.persist()
    }
}
