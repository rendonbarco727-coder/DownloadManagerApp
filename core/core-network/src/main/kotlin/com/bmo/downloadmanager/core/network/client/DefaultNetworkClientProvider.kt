package com.bmo.downloadmanager.core.network.client

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNetworkClientProvider @Inject constructor() : NetworkClientProvider {

    private val cachedBaseClient: OkHttpClient by lazy { buildBaseClient() }

    override fun baseClient(): OkHttpClient = cachedBaseClient

    override fun clientForDomain(domainConfig: DomainHeaderConfig): OkHttpClient {
        return cachedBaseClient.newBuilder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                domainConfig.referer?.let { requestBuilder.addHeader("Referer", it) }
                domainConfig.userAgent?.let { requestBuilder.addHeader("User-Agent", it) }
                domainConfig.extraHeaders.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    override fun retrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cachedBaseClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun buildBaseClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }
}
