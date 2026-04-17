package com.example.storechecklist.data.remote

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ServerApiFactory(
    private val converterFactory: MoshiConverterFactory,
) {
    fun create(baseUrl: String): ServerChecklistApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(converterFactory)
            .build()
            .create(ServerChecklistApi::class.java)
    }
}
