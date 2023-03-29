package net.opendasharchive.openarchive.features.onboarding

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityEulaBinding
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Globals

/**
 * Created by micahjlucas on 12/15/14.
 */

/**
 * Displays an EULA ("End User License Agreement") that the user has to accept
 * before using the application. Your application should call
 * {@link EulaActivity#show(android.app.Activity)} in the onCreate() method of the first
 * activity. If the user accepts the EULA, it will never be shown again. If the
 * user refuses, {@link android.app.Activity#finish()} is invoked on your
 * activity.
 */
class EulaActivity(private val mActivity: AppCompatActivity) {

    //callback to let the activity know when the user has accepted the EULA.
    internal interface OnEulaAgreedTo {
        fun onEulaAgreedTo()
    }

    /**
     * Displays the EULA if necessary. This method should be called from the
     * onCreate() method of your main Activity.
     *
     * @return Whether the user has agreed already.
     */
    fun show(): Boolean {
        val preferences = mActivity.getSharedPreferences(Globals.PREF_FILE_KEY, Activity.MODE_PRIVATE)

        if (preferences.getBoolean(Globals.PREF_EULA_ACCEPTED, false)) {
            return true
        }

        val binding = ActivityEulaBinding.inflate(LayoutInflater.from(mActivity))

        val alert = AlertHelper.build(mActivity, title = R.string.eula_title, buttons = listOf(
            AlertHelper.positiveButton(R.string.eula_accept) { _, _ ->
                preferences.edit().putBoolean(Globals.PREF_EULA_ACCEPTED, true).apply()

                (mActivity as? OnEulaAgreedTo)?.onEulaAgreedTo()
            },
            AlertHelper.negativeButton(R.string.eula_refuse) { _, _ ->
                mActivity.finish()
            }))

        alert.setView(binding.root)
        alert.show()

        return false
    }
}