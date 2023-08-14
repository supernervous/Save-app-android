package net.opendasharchive.openarchive.features.onboarding23

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityOnboarding23InstructionsBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity

class Onboarding23InstructionsActivity : BaseActivity() {

    private lateinit var mBinding: ActivityOnboarding23InstructionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        mBinding = ActivityOnboarding23InstructionsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.skipButton.setOnClickListener {
            done()
        }

        mBinding.viewPager.adapter =
            Onboarding23FragmentStateAdapter(supportFragmentManager, lifecycle, this)

        mBinding.dotsIndicator.attachTo(mBinding.viewPager)

        mBinding.fab.setOnClickListener {
            if (isLastPage()) {
                done()
            } else {
                mBinding.coverImage.alpha = 0F
                mBinding.viewPager.currentItem++
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFirstPage()) {
                    finish()
                } else {
                    mBinding.viewPager.currentItem--
                }
            }
        })

        mBinding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (isLastPage()) {
                    mBinding.skipButton.visibility = View.INVISIBLE
                    mBinding.fab.setImageDrawable(
                        ContextCompat.getDrawable(
                            mBinding.fab.context, com.github.appintro.R.drawable.ic_appintro_done,
                        )
                    )
                } else {
                    mBinding.skipButton.visibility = View.VISIBLE
                    val icon = ContextCompat.getDrawable(
                        mBinding.fab.context, com.github.appintro.R.drawable.ic_appintro_arrow,
                    )
                    icon?.isAutoMirrored = true
                    mBinding.fab.setImageDrawable(
                        icon
                    )
                }

            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> mBinding.coverImage.alpha = 0F
                    ViewPager2.SCROLL_STATE_IDLE -> updateCoverImage()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        updateCoverImage()
    }

    private fun updateCoverImage() {
        when (mBinding.viewPager.currentItem) {
            0 -> mBinding.coverImage.setImageResource(R.drawable.onboarding23_cover_share)
            1 -> mBinding.coverImage.setImageResource(R.drawable.onboarding23_cover_archive)
            2 -> mBinding.coverImage.setImageResource(R.drawable.onboarding23_cover_verify)
            3 -> mBinding.coverImage.setImageResource(R.drawable.onboarding23_cover_encrypt)
        }
        mBinding.coverImage.alpha = 0F
        mBinding.coverImage.animate().setDuration(200L).alpha(1F).start()
    }

    private fun isFirstPage(): Boolean {
        return mBinding.viewPager.currentItem <= 0
    }

    private fun isLastPage(): Boolean {
        val pageCount: Int =
            if (mBinding.viewPager.adapter == null) 0 else mBinding.viewPager.adapter?.itemCount!!
        return mBinding.viewPager.currentItem + 1 >= pageCount
    }

    private fun done() {
        startActivity(Intent(this, SpaceSetupActivity::class.java))
    }
}