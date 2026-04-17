package com.example.storechecklist.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ServerChecklistApi {
    @GET("checklists")
    suspend fun getChecklists(): List<RemoteChecklistDto>

    @PUT("checklists")
    suspend fun replaceChecklists(@Body checklists: List<RemoteChecklistDto>): List<RemoteChecklistDto>
}
