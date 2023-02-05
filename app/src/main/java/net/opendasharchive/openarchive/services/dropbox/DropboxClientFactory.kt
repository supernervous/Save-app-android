package net.opendasharchive.openarchive.services.dropbox

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import net.opendasharchive.openarchive.util.Utility

class DropboxClientFactory {

    private var sDbxClient: DbxClientV2? = null
    fun init(context: Context, accessToken: String?): DbxClientV2? {
        if (sDbxClient == null) {
            val requestConfig = DbxRequestConfig.newBuilder("dbc")
                .withHttpRequestor(OkHttp3Requestor(Utility.generateOkHttpClient(context)))
                .build()
            sDbxClient = DbxClientV2(requestConfig, accessToken)
        }
        return sDbxClient
    }

    fun getClient(): DbxClientV2? {
        checkNotNull(sDbxClient) { "Client not initialized." }
        return sDbxClient
    }
}