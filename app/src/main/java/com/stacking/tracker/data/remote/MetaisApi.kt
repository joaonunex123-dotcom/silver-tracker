package com.stacking.tracker.data.remote

import retrofit2.http.GET

interface MetaisApi {

    /**
     * Caminho fixo de proposito: a virgula entre os pares nao pode ser
     * percent-encoded, e deixar isso como parametro so criaria armadilha.
     * Para trocar de provedor, muda-se este arquivo e o mapeamento no repositorio.
     */
    @GET("json/last/XAG-BRL,USD-BRL")
    suspend fun ultimaCotacao(): RespostaAwesome
}
