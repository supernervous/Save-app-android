package net.opendasharchive.openarchive.util

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class SelectiveViewPager : ViewPager {

    constructor(context: Context): super(context)
    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)

    private var paging = true

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        return if (paging) {
            super.onInterceptTouchEvent(e)
        } else false
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return super.onTouchEvent(ev)
    }

}