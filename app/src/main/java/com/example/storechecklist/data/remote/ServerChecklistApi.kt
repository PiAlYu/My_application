package com.example.storechecklist.data.remote

import retrofit2.http.GET

interface ServerChecklistApi {
    @GET("checklists")
    suspend fun getChecklists(): List<RemoteChecklistDto>
}

