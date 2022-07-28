package net.opendasharchive.openarchive.services.webdav

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

class OkHttpBaseClient(var username: String = "",var password: String = "") {

    var okHttpClient: OkHttpClient

    private val protocols = object : ArrayList<Protocol>() {
        init {
            add(Protocol.HTTP_1_1)
        }
    }

    private val cacheInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().addHeader("Connection", "close").build()
        chain.proceed(request)
    }

    init {
        if(username.isEmpty() && password.isEmpty()){
            okHttpClient = OkHttpClient.Builder()
                .addInterceptor(cacheInterceptor)
                .connectTimeout(20L, TimeUnit.SECONDS)
                .writeTimeout(20L, TimeUnit.SECONDS)
                .readTimeout(20L, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .protocols(protocols)
                .build()
        }else{
            okHttpClient = OkHttpClient.Builder()
                .addInterceptor(cacheInterceptor)
                .addInterceptor(BasicAuthInterceptor(user = username, password = password))
                .connectTimeout(20L, TimeUnit.SECONDS)
                .writeTimeout(20L, TimeUnit.SECONDS)
                .readTimeout(20L, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .protocols(protocols)
                .build()
        }
    }
}