package net.opendasharchive.openarchive.services.webdav

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

fun OkHttpClient.Companion.base(user: String = "", password: String = ""): OkHttpClient {
    val cacheInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().addHeader("Connection", "close").build()
        chain.proceed(request)
    }

    val builder = OkHttpClient.Builder()
        .addInterceptor(cacheInterceptor)
        .connectTimeout(20L, TimeUnit.SECONDS)
        .writeTimeout(20L, TimeUnit.SECONDS)
        .readTimeout(20L, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .protocols(arrayListOf(Protocol.HTTP_1_1))

    if (user.isNotEmpty() || password.isNotEmpty()) {
        builder.addInterceptor(BasicAuthInterceptor(user, password))
    }

    return builder.build()
}