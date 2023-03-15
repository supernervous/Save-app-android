package net.opendasharchive.openarchive.services.internetarchive

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import net.opendasharchive.openarchive.services.SiteController
import net.opendasharchive.openarchive.services.internetarchive.Util.clearWebviewAndCookies
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.core.BaseActivity
import timber.log.Timber
import java.util.regex.Pattern

class ArchiveLoginActivity : BaseActivity() {

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
            @Deprecated("Deprecated in Java")
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
            val pattern = Pattern.compile("<div class=\"alert alert-danger\">(.+?)</div>")
            val matcher = pattern.matcher(rawHtml)

            if (matcher.find()) {
                mAccessKey = matcher.group(1)?.split(":".toRegex())?.get(1)?.trim { it <= ' ' }
            }

            if (matcher.find()) {
                mSecretKey = matcher.group(1)?.split(":".toRegex())?.get(1)?.trim { it <= ' ' }
            }
        }
        catch (e: Exception) {
            Timber.tag("Archive Login").d(e, "unable to get site S3 creds")
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
                showAccountCreatedDialog { _, _ -> finish() }
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