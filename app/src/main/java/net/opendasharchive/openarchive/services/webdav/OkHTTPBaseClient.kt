package net.opendasharchive.openarchive.services.webdav

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OkHTTPBaseClient {

    var okHttpClient: OkHttpClient

    private val protocols = object : ArrayList<Protocol>() {
        init {
            add(Protocol.HTTP_1_1)
        }
    }

    private val cacheInterceptor = Interceptor { chain ->
        val request: Request =
                chain.request().newBuilder().addHeader("Connection", "close").build()
        chain.proceed(request)
    }

    init {
        okHttpClient = OkHttpClient.Builder()
                .addInterceptor(cacheInterceptor)
                .connectTimeout(20L, TimeUnit.SECONDS)
                .writeTimeout(20L, TimeUnit.SECONDS)
                .readTimeout(20L, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .protocols(protocols)
                .build()
    }
}
