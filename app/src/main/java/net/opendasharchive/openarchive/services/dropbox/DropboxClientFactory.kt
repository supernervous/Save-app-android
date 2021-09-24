package net.opendasharchive.openarchive.services.dropbox

import android.content.Context
import android.util.Log
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import info.guardianproject.netcipher.client.StrongBuilder
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.util.Prefs.getUseTor
import okhttp3.OkHttpClient

class DropboxClientFactory {

    private var sDbxClient: DbxClientV2? = null
    fun init(context: Context, accessToken: String?): DbxClientV2? {
        if (sDbxClient == null) {
            val requestConfig = DbxRequestConfig.newBuilder("dbc")
                .withHttpRequestor(OkHttp3Requestor(getOkClient(context)))
                .build()
            sDbxClient = DbxClientV2(requestConfig, accessToken)
        }
        return sDbxClient
    }

    fun getClient(): DbxClientV2? {
        checkNotNull(sDbxClient) { "Client not initialized." }
        return sDbxClient
    }

    private var client: OkHttpClient? = null

    private fun getOkClient(context: Context): OkHttpClient? {
        client = OkHttpClient.Builder().build()
        if (getUseTor() && OrbotHelper.isOrbotInstalled(context)) {
            try {
                OrbotHelper.requestStartTor(context)
                val builder = StrongOkHttpClientBuilder(context)
                builder.withBestProxy().build(object : StrongBuilder.Callback<OkHttpClient?> {
                    override fun onConnected(okHttpClient: OkHttpClient?) {
                        Log.i("NetCipherClient", "Connection to orbot established!")
                        client = okHttpClient
                    }

                    override fun onConnectionException(exc: Exception) {
                        Log.e("NetCipherClient", "onConnectionException()", exc)
                    }

                    override fun onTimeout() {
                        Log.e("NetCipherClient", "onTimeout()")
                    }

                    override fun onInvalid() {
                        Log.e("NetCipherClient", "onInvalid()")
                    }
                })
            } catch (exc: Exception) {
                Log.e("Error", "Error while initializing TOR Proxy OkHttpClient", exc)
            }
        }
        return client
    }
}