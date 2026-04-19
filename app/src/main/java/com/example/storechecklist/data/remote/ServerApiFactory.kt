package com.example.storechecklist.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ServerApiFactory(
    private val converterFactory: MoshiConverterFactory,
) {
    fun create(
        baseUrl: String,
        authToken: String = "",
    ): ServerChecklistApi {
        val clientBuilder = OkHttpClient.Builder()
        val normalizedToken = authToken.trim()

        if (normalizedToken.isNotEmpty()) {
            clientBuilder.addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("Authorization", "Bearer $normalizedToken")
                    .build()
                chain.proceed(request)
            }
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(converterFactory)
            .build()
            .create(ServerChecklistApi::class.java)
    }
}
