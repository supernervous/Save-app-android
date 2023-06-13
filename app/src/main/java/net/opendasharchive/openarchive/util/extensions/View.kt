package net.opendasharchive.openarchive.util.extensions

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.cloak() {
    visibility = View.INVISIBLE
}

fun View.toggle(state: Boolean? = null) {
    if (state ?: isVisible) {
        hide()
    }
    else {
        show()
    }
}

val View.isVisible: Boolean
    get() = visibility == View.VISIBLE

fun View.makeSnackBar(message: CharSequence, duration: Int = Snackbar.LENGTH_INDEFINITE): Snackbar {
    return Snackbar.make(this, message, duration)
}
