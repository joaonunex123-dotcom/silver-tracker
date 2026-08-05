package com.stacking.tracker.core

import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.data.local.Peca
import java.time.ZoneId

/** Gramas em uma onca troy. */
const val GRAMAS_POR_OZ_TROY = 31.1035

fun gramasParaOzTroy(gramas: Double): Double = gramas / GRAMAS_POR_OZ_TROY

fun ozTroyParaGramas(oz: Double): Double = oz * GRAMAS_POR_OZ_TROY

/** Preco de um grama de prata pura, derivado do spot por onca troy. */
fun precoPorGrama(precoOzBrl: Double): Double = precoOzBrl / GRAMAS_POR_OZ_TROY

/**
 * Variacao do estoque entre duas cotacoes, mantendo a quantidade de metal fixa.
 * Mede movimento de **preco**, nao compras: e o que o widget mostra como "hoje".
 */
data class VariacaoEstoque(
    val valorAgora: Double,
    val valorAntes: Double,
) {
    val diferenca: Double = valorAgora - valorAntes
    val percentual: Double? =
        if (valorAntes > 0.0) diferenca / valorAntes * 100.0 else null
}

fun variacaoDoEstoque(
    ozFinas: Double,
    precoOzBrlAgora: Double,
    precoOzBrlAntes: Double,
): VariacaoEstoque? {
    if (ozFinas <= 0.0 || precoOzBrlAgora <= 0.0 || precoOzBrlAntes <= 0.0) return null
    return VariacaoEstoque(
        valorAgora = ozFinas * precoOzBrlAgora,
        valorAntes = ozFinas * precoOzBrlAntes,
    )
}

/** Peso bruto da peca em oncas troy. */
val Peca.pesoTroyOz: Double
    get() = gramasParaOzTroy(pesoGramas)

/** Prata pura contida na peca, em oncas troy (ASW). */
val Peca.ozFinas: Double
    get() = pesoTroyOz * pureza

/** Prata pura contida na peca, em gramas. */
val Peca.gramasFinos: Double
    get() = pesoGramas * pureza

/** valorAtualPeca = pesoTroyOz * pureza * precoOzAtual */
fun calcularValorAtual(peca: Peca, precoOzBrl: Double): Double = peca.ozFinas * precoOzBrl

/** Quanto o metal contido na peca valia ao preco spot informado (mesma conta, outro contexto). */
fun calcularValorSpot(peca: Peca, precoOzBrl: Double): Double = calcularValorAtual(peca, precoOzBrl)

/**
 * premioPercent = (precoPago - valorSpotNaCompra) / valorSpotNaCompra * 100
 *
 * Retorna null quando nao ha spot de referencia (sem cotacao registrada ate a data).
 */
fun calcularPremioPercent(precoPago: Double, valorSpotNaCompra: Double): Double? =
    if (valorSpotNaCompra <= 0.0) null
    else (precoPago - valorSpotNaCompra) / valorSpotNaCompra * 100.0

/**
 * Resolve qual cotacao usar como spot de referencia para uma data.
 *
 * A entity [Peca] nao guarda o spot do dia da compra, entao o premio e calculado
 * contra o historico: pega a cotacao mais recente que nao seja posterior a compra
 * e, se a compra for anterior a todo o historico, usa a cotacao mais antiga
 * disponivel. Sem historico algum, nao ha premio a calcular.
 *
 * Espera [historicoDesc] ordenado da mais recente para a mais antiga.
 */
class ResolvedorSpot(
    private val historicoDesc: List<Cotacao>,
    private val zona: ZoneId = ZoneId.systemDefault(),
) {

    fun em(data: Long): Cotacao? {
        if (historicoDesc.isEmpty()) return null
        // Compara contra o FIM do dia da compra, nao contra o instante dela.
        // A compra e gravada como meia-noite local; uma cotacao lancada as 14h do
        // mesmo dia seria descartada por "ser posterior", e a peca acabaria
        // avaliada pelo spot de dias atras.
        val limiteDoDia = fimDoDia(data, zona)
        return historicoDesc.firstOrNull { it.data <= limiteDoDia } ?: historicoDesc.last()
    }

    fun precoOzBrlEm(data: Long): Double = em(data)?.precoOzBrl ?: 0.0

    companion object {
        val VAZIO = ResolvedorSpot(emptyList())
    }
}

/** Peca com os numeros derivados ja calculados, pronta para a UI. */
data class PecaCalculada(
    val peca: Peca,
    val precoOzBrlAtual: Double,
    val precoOzBrlNaCompra: Double,
) {
    val pesoTroyOz: Double = peca.pesoTroyOz
    val ozFinas: Double = peca.ozFinas
    val valorAtual: Double = calcularValorAtual(peca, precoOzBrlAtual)
    val valorSpotNaCompra: Double = calcularValorSpot(peca, precoOzBrlNaCompra)
    val premioPercent: Double? = calcularPremioPercent(peca.precoPago, valorSpotNaCompra)
    val premioReais: Double? = if (valorSpotNaCompra > 0.0) peca.precoPago - valorSpotNaCompra else null
    val lucro: Double? = if (precoOzBrlAtual > 0.0) valorAtual - peca.precoPago else null
    val lucroPercent: Double? =
        if (precoOzBrlAtual > 0.0 && peca.precoPago > 0.0) {
            (valorAtual - peca.precoPago) / peca.precoPago * 100.0
        } else {
            null
        }
}

fun Peca.calcular(cotacaoAtual: Cotacao?, resolvedor: ResolvedorSpot): PecaCalculada =
    PecaCalculada(
        peca = this,
        precoOzBrlAtual = cotacaoAtual?.precoOzBrl ?: 0.0,
        precoOzBrlNaCompra = resolvedor.precoOzBrlEm(dataCompra),
    )

/** Numeros agregados da carteira inteira. */
data class ResumoCarteira(
    val quantidadePecas: Int = 0,
    val totalGramas: Double = 0.0,
    val totalOzTroy: Double = 0.0,
    val totalOzFinas: Double = 0.0,
    val totalInvestido: Double = 0.0,
    val valorMercado: Double = 0.0,
    val lucro: Double = 0.0,
    val lucroPercent: Double? = null,
    val premioMedioPercent: Double? = null,
    val custoMedioPorOzFina: Double? = null,
    val cotacao: Cotacao? = null,
) {
    val temCotacao: Boolean get() = cotacao != null && cotacao.precoOzBrl > 0.0
}

fun resumirCarteira(pecas: List<PecaCalculada>, cotacao: Cotacao?): ResumoCarteira {
    if (pecas.isEmpty()) return ResumoCarteira(cotacao = cotacao)

    val totalGramas = pecas.sumOf { it.peca.pesoGramas }
    val totalOzFinas = pecas.sumOf { it.ozFinas }
    val totalInvestido = pecas.sumOf { it.peca.precoPago }
    val precoAtual = cotacao?.precoOzBrl ?: 0.0
    val valorMercado = if (precoAtual > 0.0) totalOzFinas * precoAtual else 0.0

    // Premio medio ponderado: soma dos pagamentos contra a soma dos spots das compras,
    // considerando apenas as pecas que tem spot de referencia no historico.
    val comReferencia = pecas.filter { it.valorSpotNaCompra > 0.0 }
    val spotAcumulado = comReferencia.sumOf { it.valorSpotNaCompra }
    val pagoAcumulado = comReferencia.sumOf { it.peca.precoPago }
    val premioMedio =
        if (spotAcumulado > 0.0) (pagoAcumulado - spotAcumulado) / spotAcumulado * 100.0 else null

    return ResumoCarteira(
        quantidadePecas = pecas.size,
        totalGramas = totalGramas,
        totalOzTroy = pecas.sumOf { it.pesoTroyOz },
        totalOzFinas = totalOzFinas,
        totalInvestido = totalInvestido,
        valorMercado = valorMercado,
        lucro = if (precoAtual > 0.0) valorMercado - totalInvestido else 0.0,
        lucroPercent =
            if (precoAtual > 0.0 && totalInvestido > 0.0) {
                (valorMercado - totalInvestido) / totalInvestido * 100.0
            } else {
                null
            },
        premioMedioPercent = premioMedio,
        custoMedioPorOzFina = if (totalOzFinas > 0.0) totalInvestido / totalOzFinas else null,
        cotacao = cotacao,
    )
}
