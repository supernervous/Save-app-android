package io.scal.secureshareui.login

import io.scal.secureshareui.lib.Util.clearWebviewAndCookies
import android.app.Activity
import android.webkit.WebView
import android.os.Bundle
import net.opendasharchive.openarchive.R
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.webkit.WebViewClient
import io.scal.secureshareui.controller.ArchiveSiteController
import android.webkit.JavascriptInterface
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import io.scal.secureshareui.controller.SiteController
import io.scal.secureshareui.lib.Util
import java.lang.Exception
import java.util.regex.Pattern

class ArchiveLoginActivity : Activity() {

    private var mAccessResult = RESULT_CANCELED
    private var mAccessKey: String? = null
    private var mSecretKey: String? = null
    private var mWebview: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_archive_login)
        val doRegister = intent.getBooleanExtra("register", false)
        val useTor = intent.getBooleanExtra("useTor", false)
        var proxyHost: String? = null
        var proxyPort = -1
        if (useTor) {
            proxyHost =
                if (intent.hasExtra("proxyHost")) intent.getStringExtra("proxyHost") else Util.ORBOT_HOST
            proxyPort = intent.getIntExtra("proxyPort", Util.ORBOT_HTTP_PORT)
        }
        if (doRegister) login(ARCHIVE_CREATE_ACCOUNT_URL, proxyHost, proxyPort) else login(
            ARCHIVE_LOGIN_URL, proxyHost, proxyPort
        )
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val obscuredTouch = event!!.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
            if (obscuredTouch) return false
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun login(currentURL: String, proxyHost: String?, proxyPort: Int) {
        mWebview = findViewById<View>(R.id.webView) as WebView
        mWebview!!.settings.javaScriptEnabled = true
        mWebview!!.visibility = View.VISIBLE
        mWebview!!.addJavascriptInterface(JSInterface(), "htmlout")

        //if Orbot is installed and running, then use it!
        /**
         * if (proxyHost != null) {
         *
         * try {
         * WebkitProxy.setProxy("android.app.Application", getApplicationContext(),mWebview,proxyHost,proxyPort) ;
         * } catch (Exception e) {
         * Log.e(TAG, "user selected \"use tor\" but an exception was thrown while setting the proxy: " + e.getLocalizedMessage());
         * return;
         * }
         *
         * } */
        mWebview!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                //if logged in, hide and redirect to credentials
                if (url == ARCHIVE_LOGGED_IN_URL) {
                    //		view.setVisibility(View.INVISIBLE);
                    view.loadUrl(ARCHIVE_CREDENTIALS_URL)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                //if credentials page, inject JS for scraping
                when (url) {
                    ARCHIVE_CREDENTIALS_URL -> {
                        sIsLoginScren = true
                        val jsCmd = StringBuffer()
                        jsCmd.append("javascript:(function(){")
                        jsCmd.append("window.htmlout.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                        jsCmd.append("document.getElementById('confirm').checked=true;")
                        jsCmd.append("document.getElementById('generateNewKeys').click();")
                        jsCmd.append("})();")
                        mWebview!!.loadUrl(jsCmd.toString())
                    }
                    ARCHIVE_CREATE_ACCOUNT_URL -> {
                        sIsLoginScren = false
                        //String jsSourceDump = "javascript:";
                        //mWebview.loadUrl(jsSourceDump);
                    }
                    ArchiveSiteController.ARCHIVE_BASE_URL -> {
                        view.loadUrl(ARCHIVE_CREDENTIALS_URL)
                    }
                }
            }
        }
        mWebview!!.loadUrl(currentURL)
    }

    private fun parseArchiveCredentials(rawHtml: String) {
        try {
            val pattern = Pattern.compile("<div class=\"alert alert-danger\">(.+?)<\\/div>")
            val matcher = pattern.matcher(rawHtml)
            if (matcher.find()) mAccessKey =
                matcher.group(1).split(":".toRegex()).toTypedArray()[1].trim { it <= ' ' }
            if (matcher.find()) mSecretKey =
                matcher.group(1).split(":".toRegex()).toTypedArray()[1].trim { it <= ' ' }
        } catch (e: Exception) {
            Log.d("Archive Login", "unable to get site S3 creds", e)
        }
    }

    internal inner class JSInterface {
        @JavascriptInterface
        fun processHTML(html: String?) {
            if (null == html) {
                return
            }
            if (sIsLoginScren) {
                parseArchiveCredentials(html)
                if (mAccessKey != null && mSecretKey != null) {
                    mAccessResult = RESULT_OK
                    finish()
                }
            } else if (html.contains("Verification Email Sent")) {
                showAccountCreatedDialog { dialog, which -> finish() }
            }
        }
    }

    private fun showAccountCreatedDialog(positiveBtnClickListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.archive_title))
            .setMessage(getString(R.string.archive_message))
            .setPositiveButton(R.string.lbl_ok, positiveBtnClickListener).show()
    }

    override fun finish() {
        Log.d(TAG, "finish()")
        val data = Intent()
        data.putExtra(SiteController.EXTRAS_KEY_USERNAME, mAccessKey)
        data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mSecretKey)
        setResult(mAccessResult, data)
        super.finish()
        clearWebviewAndCookies(mWebview, this)
    }

    companion object {
        private const val TAG = "ArchiveLoginActivity"
        private const val ARCHIVE_CREATE_ACCOUNT_URL =
            "https://archive.org/account/login.createaccount.php"
        private const val ARCHIVE_LOGIN_URL = "https://archive.org/account/login.php"
        private const val ARCHIVE_LOGGED_IN_URL = "https://archive.org/index.php"
        private const val ARCHIVE_CREDENTIALS_URL = "https://archive.org/account/s3.php"
        private var sIsLoginScren = false
    }
}