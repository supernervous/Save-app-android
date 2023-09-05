package net.opendasharchive.openarchive.services.internetarchive

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebView
import androidx.annotation.ColorRes
import java.security.SecureRandom
import java.util.*

object Util {

    // netcipher
    const val ORBOT_HOST = "127.0.0.1"
    const val ORBOT_HTTP_PORT = 8118

    @JvmStatic
    fun clearWebviewAndCookies(webview: WebView?, activity: Activity?) {
        CookieSyncManager.createInstance(activity)
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        if (webview != null) {
            webview.clearHistory()
            webview.clearCache(true)
            webview.clearFormData()
            webview.loadUrl("about:blank")
            webview.destroy()
        }
    }

    fun isEmpty(string: String?): Boolean {
        return string == null || string.trim { it <= ' ' }.isEmpty()
    }

    // TODO audit code for security since we use the to generate random strings for url slugs
    class RandomString(length: Int) {
        private val random: Random = SecureRandom()
        private val buf: CharArray
        fun nextString(): String {
            for (idx in buf.indices) buf[idx] = symbols[random.nextInt(symbols.length)]
            return String(buf)
        }

        companion object {
            /* Assign a string that contains the set of characters you allow. */
            private const val symbols = "abcdefghijklmnopqrstuvwxyz0123456789"
        }

        init {
            require(length >= 1) { "length < 1: $length" }
            buf = CharArray(length)
        }
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    @JvmStatic
    fun setBackgroundTint(view: View, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.backgroundTintList = view.resources.getColorStateList(color, view.context.theme)
        } else {
            view.backgroundTintList = view.resources.getColorStateList(color)
        }
    }
}