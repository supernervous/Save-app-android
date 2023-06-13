package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.CustomOnboardingMainBinding
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.show

class TorOnboardingScreen : Fragment(), View.OnClickListener {

    private lateinit var mBinding: CustomOnboardingMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = CustomOnboardingMainBinding.inflate(inflater, container, false)

        mBinding.customSlideBigText.setText(R.string.onboarding_archive_over_tor)

        mBinding.customSlideBigTextSub.show()

        mBinding.customSlideButton.show()
        mBinding.customSlideButton.setOnClickListener(this)

        setDynamicTexts()

        mBinding.customSlideImage.setImageResource(R.drawable.onboarding5)

        return mBinding.root
    }

    override fun onResume() {
        super.onResume()

        setDynamicTexts()
    }

    override fun onClick(view: View?) {
        if (OrbotHelper.isOrbotInstalled(context)) {
            if (OrbotHelper.requestStartTor(context)) {
                Prefs.useTor = true
            }

            activity?.finish()
            activity?.startActivity(Intent(context, SpaceSetupActivity::class.java))
        }
        else {
            startActivity(OrbotHelper.getOrbotInstallIntent(context))
        }
    }

    private fun setDynamicTexts() {
        if (!OrbotHelper.isOrbotInstalled(context)) {
            mBinding.customSlideBigTextSub.setText(R.string.onboarding_archive_over_tor_install_orbot)

            mBinding.customSlideButton.setText(R.string.action_install)
        }
        else {
            mBinding.customSlideBigTextSub.setText(R.string.archive_over_tor_enable_orbot)

            mBinding.customSlideButton.setText(R.string.action_start)
        }
    }
}