package net.opendasharchive.openarchive.util.extensions

import android.view.View
import android.view.ViewGroup
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
    if (state ?: !isVisible) {
        show()
    }
    else {
        hide()
    }
}

fun View.disableAnimation(around: () -> Unit) {
    val p = parent as? ViewGroup

    val original = p?.layoutTransition
    p?.layoutTransition = null

    around()

    p?.layoutTransition = original
}

val View.isVisible: Boolean
    get() = visibility == View.VISIBLE

fun View.makeSnackBar(message: CharSequence, duration: Int = Snackbar.LENGTH_INDEFINITE): Snackbar {
    return Snackbar.make(this, message, duration)
}
