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
import android.view.View
import io.scal.secureshareui.controller.SiteController
import net.opendasharchive.openarchive.util.Utility.clearWebviewAndCookies
import timber.log.Timber
import java.lang.Exception
import java.util.regex.Pattern

class ArchiveLoginActivity : Activity() {

    private var mAccessResult = RESULT_CANCELED
    private var mAccessKey: String? = null
    private var mSecretKey: String? = null
    private var mWebview: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive_login)
        val doRegister = intent.getBooleanExtra("register", false)
        if (doRegister) login(ARCHIVE_CREATE_ACCOUNT_URL) else login(
            ARCHIVE_LOGIN_URL
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun login(currentURL: String) {
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
                if (url == ARCHIVE_LOGGED_IN_URL) {
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
                matcher.group(1)?.split(":".toRegex())!!.toTypedArray()[1].trim { it <= ' ' }
            if (matcher.find()) mSecretKey =
                matcher.group(1)?.split(":".toRegex())!!.toTypedArray()[1].trim { it <= ' ' }
        } catch (e: Exception) {
            Timber.tag(TAG).d("Archive Login unable to get site S3 creds $e")
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
        Timber.tag(TAG).d("finish()")
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