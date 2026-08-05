package com.stacking.tracker.data.remote

import com.stacking.tracker.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object Rede {

    /** URLs das fontes de reserva. So a primaria e configuravel. */
    private const val BASE_OURO = "https://api.gold-api.com/"
    private const val BASE_CAMBIO = "https://api.frankfurter.app/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    // Um unico OkHttpClient para as tres APIs: pool de conexoes e cache de DNS
    // compartilhados, e menos threads paradas.
    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun criarAwesome(baseUrl: String = BuildConfig.COTACAO_BASE_URL): AwesomeApi =
        retrofit(baseUrl).create(AwesomeApi::class.java)

    fun criarOuro(): OuroApi = retrofit(BASE_OURO).create(OuroApi::class.java)

    fun criarCambio(): CambioApi = retrofit(BASE_CAMBIO).create(CambioApi::class.java)
}
