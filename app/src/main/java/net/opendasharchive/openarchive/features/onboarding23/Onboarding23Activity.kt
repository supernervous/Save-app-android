package net.opendasharchive.openarchive.features.onboarding23

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityOnboarding23Binding
import net.opendasharchive.openarchive.features.core.BaseActivity

class Onboarding23Activity : BaseActivity() {

    private lateinit var mBinding: ActivityOnboarding23Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        mBinding = ActivityOnboarding23Binding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.getStarted.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    Onboarding23InstructionsActivity::class.java
                )
            )
        }

        for (textView in arrayOf(
            mBinding.shareText,
            mBinding.archiveText,
            mBinding.verifyText,
            mBinding.encryptText
        )) {
            textView.text = colorizeFirstLetter(textView.text, R.color.colorPrimary)
        }
    }

    private fun colorizeFirstLetter(text: CharSequence, @ColorRes color: Int): Spanned {
        val colorHexString =
            Integer.toHexString(0xffffff and ContextCompat.getColor(this, color))
        val html =
            "<font color=\"#${colorHexString}\">${text.substring(0, 1)}</font>${text.substring(1)}"
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}