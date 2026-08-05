package com.stacking.tracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fonte primaria: AwesomeAPI, `json/last/XAG-BRL,USD-BRL`.
 *
 * ```json
 * {
 *   "XAGBRL": { "bid": "319.76", "timestamp": "1785940081", ... },
 *   "USDBRL": { "bid": "5.1293",  "timestamp": "1785951301", ... }
 * }
 * ```
 *
 * Entrega prata ja em BRL por onca troy e nao exige chave. O limite anonimo e por
 * IP, e operadora de celular usa NAT compartilhado — daí o 429 em rede movel, o que
 * motivou a fonte de reserva abaixo.
 *
 * Os valores vem como **string**, nao como numero.
 */
@Serializable
data class RespostaAwesome(
    @SerialName("XAGBRL")
    val prata: ParCotado? = null,
    @SerialName("USDBRL")
    val dolar: ParCotado? = null,
)

@Serializable
data class ParCotado(
    val code: String? = null,
    val codein: String? = null,
    val bid: String? = null,
    val ask: String? = null,
    /** Epoch em segundos, tambem como string. */
    val timestamp: String? = null,
    @SerialName("create_date")
    val criadoEm: String? = null,
) {
    /** Preferimos o bid; ask serve de reserva quando o bid vem vazio. */
    val valor: Double?
        get() = bid?.toDoubleOrNull() ?: ask?.toDoubleOrNull()

    /** Momento da cotacao no mercado, em millis. Null quando ausente ou invalido. */
    val instante: Long?
        get() = timestamp?.toLongOrNull()?.times(1000L)
}

/**
 * Reserva, parte 1: gold-api.com `price/XAG`, prata em **USD por onca troy**.
 *
 * ```json
 * { "name": "Silver", "price": 62.12, "currency": "USD", "symbol": "XAG" }
 * ```
 */
@Serializable
data class RespostaOuroApi(
    val name: String? = null,
    val symbol: String? = null,
    val currency: String? = null,
    val price: Double? = null,
)

/**
 * Reserva, parte 2: frankfurter.app `latest?from=USD&to=BRL`, so o cambio.
 *
 * ```json
 * { "base": "USD", "date": "2026-08-05", "rates": { "BRL": 5.1153 } }
 * ```
 */
@Serializable
data class RespostaCambio(
    val base: String? = null,
    val date: String? = null,
    val rates: Map<String, Double> = emptyMap(),
) {
    val brl: Double? get() = rates["BRL"]
}
