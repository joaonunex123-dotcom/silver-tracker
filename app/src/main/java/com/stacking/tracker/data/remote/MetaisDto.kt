package com.stacking.tracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resposta da AwesomeAPI para `json/last/XAG-BRL,USD-BRL`.
 *
 * ```json
 * {
 *   "XAGBRL": { "bid": "319.76", "ask": "319.82", "timestamp": "1785940081", ... },
 *   "USDBRL": { "bid": "5.1293",  "ask": "5.1347",  "timestamp": "1785951301", ... }
 * }
 * ```
 *
 * Escolhida por nao exigir chave de API: o APK e publicado num Release publico, e
 * qualquer chave embutida nele estaria publicada junto. De quebra, ja entrega a
 * prata em BRL por onca troy, que e a moeda em que o app calcula tudo.
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
