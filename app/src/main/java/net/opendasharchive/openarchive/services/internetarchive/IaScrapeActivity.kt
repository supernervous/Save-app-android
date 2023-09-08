package net.opendasharchive.openarchive.services.internetarchive

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import info.guardianproject.netcipher.webkit.WebkitProxy
import kotlinx.coroutines.*
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityIaScrapeBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.internetarchive.Util.clearWebviewAndCookies
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.extensions.show
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.regex.Pattern

class IaScrapeActivity : BaseActivity() {

    private lateinit var mBinding: ActivityIaScrapeBinding

    private var mAccessResult = RESULT_CANCELED
    private var mAccessKey: String? = null
    private var mSecretKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityIaScrapeBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val doRegister = intent.getBooleanExtra("register", false)

        login(if (doRegister) { ARCHIVE_CREATE_ACCOUNT_URL } else { ARCHIVE_LOGIN_URL })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_refresh, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()

                true
            }

            R.id.action_refresh -> {
                mBinding.webView.reload()

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun login(currentURL: String) {
        mBinding.webView.settings.javaScriptEnabled = true
        mBinding.webView.addJavascriptInterface(JSInterface(), "htmlout")
        mBinding.webView.show()

        mBinding.webView.webViewClient = object : WebViewClient() {

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

                // If logged in, redirect to credentials.
                if (url == ARCHIVE_LOGGED_IN_URL) {
                    view.loadUrl(ARCHIVE_CREDENTIALS_URL)
                    return true
                }

                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                //if credentials page, inject JS for scraping.
                when (url) {
                    ARCHIVE_CREDENTIALS_URL -> {
                        sIsLoginScreen = true
                        val jsCmd = StringBuffer()
                        jsCmd.append("javascript:(function(){")
                        jsCmd.append("window.htmlout.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                        jsCmd.append("document.getElementById('confirm').checked=true;")
                        jsCmd.append("document.getElementById('generateNewKeys').click();")
                        jsCmd.append("})();")
                        mBinding.webView.loadUrl(jsCmd.toString())
                    }
                    ARCHIVE_CREATE_ACCOUNT_URL -> {
                        sIsLoginScreen = false
                    }
                    IaConduit.ARCHIVE_BASE_URL -> {
                        view.loadUrl(ARCHIVE_CREDENTIALS_URL)
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = SaveClient.get(this@IaScrapeActivity)

                if (client.proxy?.type() == Proxy.Type.HTTP || client.proxy?.type() == Proxy.Type.SOCKS) {
                    val address = client.proxy?.address() as? InetSocketAddress

                    if (address != null) {
                        WebkitProxy.setProxy(applicationContext,address.hostString, address.port)
                    }
                }

                MainScope().launch {
                    mBinding.webView.loadUrl(currentURL)
                }
            }
            catch (e: Exception) {
                MainScope().launch {
                    Toast.makeText(this@IaScrapeActivity, e.localizedMessage, Toast.LENGTH_LONG).show()

                    finish()
                }
            }
        }
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
            Timber.d(e, "Unable to get site S3 credentials.")
        }
    }

    internal inner class JSInterface {
        @JavascriptInterface
        fun processHTML(html: String?) {
            if (null == html) return

            if (sIsLoginScreen) {
                parseArchiveCredentials(html)
                if (mAccessKey != null && mSecretKey != null) {
                    mAccessResult = RESULT_OK
                    finish()
                }
            }
            else if (html.contains("Verification Email Sent")) {
                showAccountCreatedDialog { _, _ -> finish() }
            }
        }
    }

    private fun showAccountCreatedDialog(positiveBtnClickListener: DialogInterface.OnClickListener) {
        AlertHelper.show(this, R.string.archive_message, R.string.archive_title, buttons = listOf(
            AlertHelper.positiveButton { dialog, which ->
                positiveBtnClickListener.onClick(dialog, which)
            }))
    }

    override fun finish() {
        Timber.d("finish()")

        val data = Intent()
        data.putExtra(EXTRAS_KEY_USERNAME, mAccessKey)
        data.putExtra(EXTRAS_KEY_CREDENTIALS, mSecretKey)
        setResult(mAccessResult, data)

        super.finish()

        clearWebviewAndCookies(mBinding.webView, this)
    }

    companion object {
        private const val ARCHIVE_CREATE_ACCOUNT_URL = "https://archive.org/account/login.createaccount.php"
        private const val ARCHIVE_LOGIN_URL = "https://archive.org/account/login.php"
        private const val ARCHIVE_LOGGED_IN_URL = "https://archive.org/index.php"
        private const val ARCHIVE_CREDENTIALS_URL = "https://archive.org/account/s3.php"

        const val EXTRAS_KEY_USERNAME = "username"
        const val EXTRAS_KEY_CREDENTIALS = "credentials"

        private var sIsLoginScreen = false
    }
}