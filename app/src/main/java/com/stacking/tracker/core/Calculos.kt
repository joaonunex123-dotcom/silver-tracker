package com.stacking.tracker.core

import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.data.local.Peca
import com.stacking.tracker.data.local.Venda
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

// Uma linha do inventario pode representar N pecas identicas. Por convencao, as
// propriedades SEM sufixo sao o total da linha (ja multiplicado pela quantidade),
// porque e o que praticamente toda tela quer mostrar. As unitarias levam sufixo.

/** Peso bruto de UMA peca, em oncas troy. */
val Peca.pesoTroyOzUnidade: Double
    get() = gramasParaOzTroy(pesoGramas)

/** Prata pura de UMA peca, em oncas troy (ASW). */
val Peca.ozFinasUnidade: Double
    get() = pesoTroyOzUnidade * pureza

/** Peso bruto da linha inteira, em oncas troy. */
val Peca.pesoTroyOz: Double
    get() = pesoTroyOzUnidade * quantidade

/** Prata pura da linha inteira, em oncas troy (ASW). */
val Peca.ozFinas: Double
    get() = ozFinasUnidade * quantidade

/** Peso bruto da linha inteira, em gramas. */
val Peca.gramasTotal: Double
    get() = pesoGramas * quantidade

/** Prata pura da linha inteira, em gramas. */
val Peca.gramasFinos: Double
    get() = gramasTotal * pureza

/** Valor pago pela linha inteira. */
val Peca.precoPagoTotal: Double
    get() = precoPago * quantidade

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
    val vendas: List<Venda> = emptyList(),
) {
    /** Quantas foram compradas. Nao muda ao vender. */
    val quantidade: Int = peca.quantidade
    val quantidadeVendida: Int = vendas.sumOf { it.quantidade }
    val quantidadeEmEstoque: Int = (quantidade - quantidadeVendida).coerceAtLeast(0)
    val vendidaPorCompleto: Boolean = quantidadeEmEstoque == 0 && quantidade > 0

    /** Unitarios. */
    val ozFinasUnidade: Double = peca.ozFinasUnidade
    val pesoTroyOzUnidade: Double = peca.pesoTroyOzUnidade

    /** Totais do que ainda esta em maos — e o que vale para a carteira. */
    val ozFinas: Double = ozFinasUnidade * quantidadeEmEstoque
    val pesoTroyOz: Double = pesoTroyOzUnidade * quantidadeEmEstoque
    val custoEmEstoque: Double = peca.precoPago * quantidadeEmEstoque

    /** Da compra inteira, incluindo o que ja saiu. */
    val precoPagoTotal: Double = peca.precoPagoTotal

    val valorAtual: Double = ozFinas * precoOzBrlAtual

    // Premio descreve a COMPRA, entao usa a quantidade comprada: vender depois nao
    // muda o quanto se pagou acima do spot naquele dia.
    val valorSpotNaCompra: Double = peca.ozFinas * precoOzBrlNaCompra
    val premioPercent: Double? = calcularPremioPercent(precoPagoTotal, valorSpotNaCompra)
    val premioReais: Double? =
        if (valorSpotNaCompra > 0.0) precoPagoTotal - valorSpotNaCompra else null

    /** Ja embolsado: o que entrou nas vendas menos o custo do que saiu. */
    val recebidoEmVendas: Double = vendas.sumOf { it.valorRecebido }
    val custoVendido: Double = peca.precoPago * quantidadeVendida
    val lucroRealizado: Double = recebidoEmVendas - custoVendido
    val lucroRealizadoPercent: Double? =
        if (custoVendido > 0.0) lucroRealizado / custoVendido * 100.0 else null

    /** No papel: so sobre o que ainda esta em estoque. */
    val lucro: Double? = if (precoOzBrlAtual > 0.0) valorAtual - custoEmEstoque else null
    val lucroPercent: Double? =
        if (precoOzBrlAtual > 0.0 && custoEmEstoque > 0.0) {
            (valorAtual - custoEmEstoque) / custoEmEstoque * 100.0
        } else {
            null
        }
}

/** Resumo de uma venda isolada, para a tela mostrar logo depois de registrar. */
data class ResultadoVenda(
    val venda: Venda,
    /** Preco pago por UMA peca, na compra. */
    val custoUnitario: Double,
) {
    val recebido: Double = venda.valorRecebido
    val custo: Double = custoUnitario * venda.quantidade
    val lucro: Double = recebido - custo
    val lucroPercent: Double? = if (custo > 0.0) lucro / custo * 100.0 else null
    val ganhou: Boolean = lucro > 0.0
}

fun Peca.calcular(
    cotacaoAtual: Cotacao?,
    resolvedor: ResolvedorSpot,
    vendas: List<Venda> = emptyList(),
): PecaCalculada =
    PecaCalculada(
        peca = this,
        precoOzBrlAtual = cotacaoAtual?.precoOzBrl ?: 0.0,
        precoOzBrlNaCompra = resolvedor.precoOzBrlEm(dataCompra),
        vendas = vendas,
    )

/** Numeros agregados da carteira inteira. */
data class ResumoCarteira(
    /** Soma das quantidades: o numero de pecas que o usuario tem de fato. */
    val quantidadePecas: Int = 0,
    /** Linhas do inventario. Menor que [quantidadePecas] quando ha lotes. */
    val linhas: Int = 0,
    val totalGramas: Double = 0.0,
    val totalOzTroy: Double = 0.0,
    val totalOzFinas: Double = 0.0,
    val totalInvestido: Double = 0.0,
    val valorMercado: Double = 0.0,
    val lucro: Double = 0.0,
    val lucroPercent: Double? = null,
    val premioMedioPercent: Double? = null,
    val custoMedioPorOzFina: Double? = null,
    /** Ja embolsado nas vendas: recebido menos o custo do que saiu. */
    val lucroRealizado: Double = 0.0,
    val recebidoEmVendas: Double = 0.0,
    val quantidadeVendida: Int = 0,
    val cotacao: Cotacao? = null,
) {
    val temVendas: Boolean get() = quantidadeVendida > 0
    val temCotacao: Boolean get() = cotacao != null && cotacao.precoOzBrl > 0.0
}

fun resumirCarteira(pecas: List<PecaCalculada>, cotacao: Cotacao?): ResumoCarteira {
    if (pecas.isEmpty()) return ResumoCarteira(cotacao = cotacao)

    // Tudo daqui para baixo e sobre o que AINDA esta em maos: comparar valor de
    // mercado com o custo de pecas ja vendidas daria um lucro fantasma.
    val totalGramas = pecas.sumOf { it.peca.pesoGramas * it.quantidadeEmEstoque }
    val totalOzFinas = pecas.sumOf { it.ozFinas }
    val totalInvestido = pecas.sumOf { it.custoEmEstoque }
    val precoAtual = cotacao?.precoOzBrl ?: 0.0
    val valorMercado = if (precoAtual > 0.0) totalOzFinas * precoAtual else 0.0

    // Premio medio ponderado: soma dos pagamentos contra a soma dos spots das compras,
    // considerando apenas as pecas que tem spot de referencia no historico.
    // Premio medio olha as compras, entao entra tambem o que ja foi vendido.
    val comReferencia = pecas.filter { it.valorSpotNaCompra > 0.0 }
    val spotAcumulado = comReferencia.sumOf { it.valorSpotNaCompra }
    val pagoAcumulado = comReferencia.sumOf { it.precoPagoTotal }
    val premioMedio =
        if (spotAcumulado > 0.0) (pagoAcumulado - spotAcumulado) / spotAcumulado * 100.0 else null

    return ResumoCarteira(
        quantidadePecas = pecas.sumOf { it.quantidadeEmEstoque },
        linhas = pecas.count { it.quantidadeEmEstoque > 0 },
        lucroRealizado = pecas.sumOf { it.lucroRealizado },
        recebidoEmVendas = pecas.sumOf { it.recebidoEmVendas },
        quantidadeVendida = pecas.sumOf { it.quantidadeVendida },
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
