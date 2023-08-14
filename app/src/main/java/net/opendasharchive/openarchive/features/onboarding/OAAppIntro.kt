package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.util.Prefs

/**
 * Created by n8fr8 on 8/3/16.
 */
class OAAppIntro : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Prefs.prohibitScreenshots) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        addSlide(CustomSlideBigText.newInstance(R.string.onboarding_intro, R.string.app_tag_line))

        addSlide(CustomOnboardingScreen.newInstance(R.string.oa_title_1,
                R.string.oa_subtitle_1, R.drawable.onboarding1))

        addSlide(CustomOnboardingScreen.newInstance(R.string.oa_title_2,
                R.string.oa_subtitle_2, R.drawable.onboarding2))

        addSlide(CustomOnboardingScreen.newInstance(R.string.oa_title_3,
                R.string.oa_subtitle_3, R.drawable.onboarding3))

        addSlide(CustomOnboardingScreen.newInstance(R.string.oa_title_4,
                R.string.oa_subtitle_4, R.drawable.onboarding4))

        addSlide(TorOnboardingScreen())

        setColorDoneText(ContextCompat.getColor(this, R.color.colorOnPrimary))
        setSeparatorColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
        setBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        showStatusBar(true)
        setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        setNavBarColor(ContextCompat.getColor(this, R.color.colorPrimary))

        // Hide Skip/Done button.
        isSkipButtonEnabled = false

        isSystemBackButtonLocked = true
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)

        finish()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if ((event?.flags ?: 0) and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0) return false
        }

        return super.dispatchTouchEvent(event)
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        finish()
        startActivity(Intent(this, SpaceSetupActivity::class.java))
    }
}
