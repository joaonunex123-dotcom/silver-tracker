package com.stacking.tracker.data.remote

import retrofit2.http.GET

/**
 * Fonte primaria. Caminho fixo de proposito: a virgula entre os pares nao pode ser
 * percent-encoded, e deixar isso como parametro so criaria armadilha.
 */
interface AwesomeApi {

    @GET("json/last/XAG-BRL,USD-BRL")
    suspend fun ultimaCotacao(): RespostaAwesome
}

/** Reserva: preco da prata em USD por onca troy, sem chave. */
interface OuroApi {

    @GET("price/XAG")
    suspend fun prata(): RespostaOuroApi
}

/** Reserva: cambio USD para BRL, sem chave. */
interface CambioApi {

    @GET("latest?from=USD&to=BRL")
    suspend fun usdParaBrl(): RespostaCambio
}
