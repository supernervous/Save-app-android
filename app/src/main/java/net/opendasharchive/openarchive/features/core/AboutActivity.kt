package net.opendasharchive.openarchive.features.core

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity(){

    private lateinit var mBinding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        mBinding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.aboutView.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val obscuredTouch = event!!.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
            if (obscuredTouch) return false
        }
        return super.dispatchTouchEvent(event)
    }

}