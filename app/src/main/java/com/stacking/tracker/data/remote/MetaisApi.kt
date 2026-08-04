package com.stacking.tracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface MetaisApi {

    @GET("latest")
    suspend fun ultimaCotacao(
        @Query("api_key") chave: String,
        @Query("currency") moeda: String = "USD",
        @Query("unit") unidade: String = "toz",
    ): RespostaMetais
}
