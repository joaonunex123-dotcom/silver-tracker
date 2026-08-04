package com.stacking.tracker.data.remote

import kotlinx.serialization.Serializable

/**
 * Formato esperado da resposta da API de cotacao.
 *
 * Modelado sobre o endpoint `latest?api_key=...&currency=USD&unit=toz`, que devolve:
 *
 * ```json
 * {
 *   "status": "success",
 *   "currency": "USD",
 *   "unit": "toz",
 *   "metals":     { "silver": 31.42, "gold": 2650.10 },
 *   "currencies": { "BRL": 5.42, "EUR": 0.95 }
 * }
 * ```
 *
 * Se a sua API responder em outro formato, este arquivo e o mapeamento em
 * `CotacaoRepository.atualizar()` sao os unicos pontos a ajustar.
 */
@Serializable
data class RespostaMetais(
    val status: String? = null,
    val currency: String? = null,
    val unit: String? = null,
    val metals: Map<String, Double> = emptyMap(),
    val currencies: Map<String, Double> = emptyMap(),
) {
    /** Preco da prata em USD por onca troy. */
    val prataUsdPorOz: Double?
        get() = metals["silver"] ?: metals["Silver"] ?: metals["XAG"]

    /** Quantos BRL valem 1 USD. */
    val usdParaBrl: Double?
        get() = currencies["BRL"] ?: currencies["brl"]
}
