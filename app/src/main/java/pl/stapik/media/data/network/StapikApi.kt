package pl.stapik.media.data.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface StapikApi {
    @GET("documents/{slotKey}")
    suspend fun getDocument(
        @Path("slotKey") slotKey: String,
        @Header("x-api-key") apiKey: String,
    ): DocumentResponse
}