package com.stacking.tracker.core

import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.data.local.Peca
import com.stacking.tracker.data.local.TipoPeca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CalculosTest {

    // Zona fixa: os testes nao podem depender do fuso da maquina que roda a suite.
    private val ZONA: ZoneId = ZoneId.of("America/Sao_Paulo")

    /** Compra do dia, como o app grava: meia-noite local. */
    private fun compraEm(dia: LocalDate): Long =
        dia.atStartOfDay(ZONA).toInstant().toEpochMilli()

    private fun cotacaoEm(dia: LocalDate, hora: Int, brl: Double) = Cotacao(
        id = dia.toEpochDay(),
        data = dia.atTime(hora, 0).atZone(ZONA).toInstant().toEpochMilli(),
        precoOzUsd = brl / 5.5,
        precoOzBrl = brl,
    )

    private fun peca(
        gramas: Double = 31.1035,
        pureza: Double = 0.999,
        preco: Double = 200.0,
        data: Long = 1_000L,
    ) = Peca(
        id = 1L,
        tipo = TipoPeca.MOEDA,
        nome = "Maple Leaf",
        marca = "RCM",
        pesoGramas = gramas,
        pureza = pureza,
        precoPago = preco,
        dataCompra = data,
    )

    @Test
    fun `uma onca troy equivale a 31,1035 gramas`() {
        assertEquals(1.0, gramasParaOzTroy(31.1035), 1e-9)
        assertEquals(31.1035, ozTroyParaGramas(1.0), 1e-9)
    }

    @Test
    fun `oncas finas descontam a pureza`() {
        val p = peca(gramas = 31.1035, pureza = 0.999)
        assertEquals(1.0, p.pesoTroyOz, 1e-9)
        assertEquals(0.999, p.ozFinas, 1e-9)
    }

    @Test
    fun `valor atual multiplica oncas finas pelo spot`() {
        val p = peca(gramas = 31.1035, pureza = 0.999)
        assertEquals(199.8, calcularValorAtual(p, precoOzBrl = 200.0), 1e-9)
    }

    @Test
    fun `premio compara preco pago com o metal contido`() {
        // Pagou 220 por algo que valia 200 de metal: 10% de premio.
        assertEquals(10.0, calcularPremioPercent(220.0, 200.0)!!, 1e-9)
        assertEquals(-5.0, calcularPremioPercent(190.0, 200.0)!!, 1e-9)
    }

    @Test
    fun `premio sem spot de referencia e nulo`() {
        assertNull(calcularPremioPercent(220.0, 0.0))
    }

    @Test
    fun `resolvedor usa a cotacao mais recente ate a data`() {
        val resolvedor = ResolvedorSpot(
            listOf(
                cotacaoEm(LocalDate.of(2026, 8, 5), 10, 165.0),
                cotacaoEm(LocalDate.of(2026, 8, 3), 10, 154.0),
                cotacaoEm(LocalDate.of(2026, 8, 1), 10, 137.5),
            ),
            ZONA,
        )

        assertEquals(154.0, resolvedor.precoOzBrlEm(compraEm(LocalDate.of(2026, 8, 4))), 1e-9)
        assertEquals(165.0, resolvedor.precoOzBrlEm(compraEm(LocalDate.of(2026, 8, 20))), 1e-9)
        // Compra anterior a todo o historico cai na cotacao mais antiga.
        assertEquals(137.5, resolvedor.precoOzBrlEm(compraEm(LocalDate.of(2026, 7, 20))), 1e-9)
        assertEquals(0.0, ResolvedorSpot.VAZIO.precoOzBrlEm(compraEm(LocalDate.of(2026, 8, 4))), 1e-9)
    }

    @Test
    fun `cotacao lancada mais tarde no mesmo dia vale para a compra do dia`() {
        // A compra e gravada como meia-noite local; a cotacao, as 14h.
        // Comparar pelo instante faria a peca herdar o spot da semana passada.
        val dia = LocalDate.of(2026, 8, 5)
        val resolvedor = ResolvedorSpot(
            listOf(
                cotacaoEm(dia, 14, 165.0),
                cotacaoEm(dia.minusDays(7), 10, 130.0),
            ),
            ZONA,
        )

        assertEquals(165.0, resolvedor.precoOzBrlEm(compraEm(dia)), 1e-9)
    }

    @Test
    fun `cotacao do dia seguinte nao vale para a compra de hoje`() {
        val dia = LocalDate.of(2026, 8, 5)
        val resolvedor = ResolvedorSpot(
            listOf(
                cotacaoEm(dia.plusDays(1), 9, 190.0),
                cotacaoEm(dia, 14, 165.0),
            ),
            ZONA,
        )

        assertEquals(165.0, resolvedor.precoOzBrlEm(compraEm(dia)), 1e-9)
    }

    @Test
    fun `resumo soma investido mercado e lucro`() {
        val cotacao = Cotacao(id = 1, data = 100, precoOzUsd = 30.0, precoOzBrl = 200.0)
        val resolvedor = ResolvedorSpot(listOf(cotacao))
        val pecas = listOf(
            peca(gramas = 31.1035, pureza = 1.0, preco = 180.0, data = 200),
            peca(gramas = 62.207, pureza = 1.0, preco = 420.0, data = 200),
        ).map { it.calcular(cotacao, resolvedor) }

        val resumo = resumirCarteira(pecas, cotacao)

        assertEquals(2, resumo.quantidadePecas)
        assertEquals(3.0, resumo.totalOzFinas, 1e-6)
        assertEquals(600.0, resumo.totalInvestido, 1e-6)
        assertEquals(600.0, resumo.valorMercado, 1e-6)
        assertEquals(0.0, resumo.lucro, 1e-6)
        // Pagou 600 por 600 de metal ao spot da compra: premio medio zero.
        assertEquals(0.0, resumo.premioMedioPercent!!, 1e-6)
    }

    @Test
    fun `preco por grama divide o spot pela onca troy`() {
        // A 320 BRL a onca troy, o grama sai a 320 / 31,1035.
        assertEquals(10.288, precoPorGrama(320.0), 1e-3)
        assertEquals(320.0, precoPorGrama(320.0) * GRAMAS_POR_OZ_TROY, 1e-9)
    }

    @Test
    fun `variacao do estoque mede movimento de preco`() {
        val v = variacaoDoEstoque(ozFinas = 10.0, precoOzBrlAgora = 330.0, precoOzBrlAntes = 300.0)!!
        assertEquals(3300.0, v.valorAgora, 1e-9)
        assertEquals(3000.0, v.valorAntes, 1e-9)
        assertEquals(300.0, v.diferenca, 1e-9)
        assertEquals(10.0, v.percentual!!, 1e-9)

        val queda = variacaoDoEstoque(ozFinas = 10.0, precoOzBrlAgora = 270.0, precoOzBrlAntes = 300.0)!!
        assertEquals(-300.0, queda.diferenca, 1e-9)
        assertEquals(-10.0, queda.percentual!!, 1e-9)
    }

    @Test
    fun `variacao e nula sem estoque ou sem referencia`() {
        assertNull(variacaoDoEstoque(ozFinas = 0.0, precoOzBrlAgora = 330.0, precoOzBrlAntes = 300.0))
        assertNull(variacaoDoEstoque(ozFinas = 10.0, precoOzBrlAgora = 0.0, precoOzBrlAntes = 300.0))
        assertNull(variacaoDoEstoque(ozFinas = 10.0, precoOzBrlAgora = 330.0, precoOzBrlAntes = 0.0))
    }

    @Test
    fun `unidade so muda a apresentacao, nao o valor`() {
        // 1 onca troy = 31,1035 g. O mesmo metal, escrito de dois jeitos.
        assertEquals("31,1 g", formatarQuantidade(1.0, UnidadePeso.GRAMAS))
        assertEquals("1,000 oz", formatarQuantidade(1.0, UnidadePeso.ONCAS))

        // O preco por grama vezes as gramas de uma onca reconstroi o preco da onca.
        val porGrama = precoPorUnidade(320.0, UnidadePeso.GRAMAS)
        val porOnca = precoPorUnidade(320.0, UnidadePeso.ONCAS)
        assertEquals(porOnca, porGrama * GRAMAS_POR_OZ_TROY, 1e-9)
    }

    @Test
    fun `rotulo de preco acompanha a unidade`() {
        assertEquals("BRL / g", rotuloPreco("BRL", UnidadePeso.GRAMAS))
        assertEquals("USD / oz", rotuloPreco("USD", UnidadePeso.ONCAS))
    }

    @Test
    fun `unidade desconhecida cai em gramas`() {
        assertEquals(UnidadePeso.GRAMAS, UnidadePeso.de(null))
        assertEquals(UnidadePeso.GRAMAS, UnidadePeso.de("LIBRAS"))
        assertEquals(UnidadePeso.ONCAS, UnidadePeso.de("ONCAS"))
    }

    @Test
    fun `leitura de numero aceita virgula e ponto`() {
        assertEquals(1234.56, paraDoubleOuNulo("1.234,56")!!, 1e-9)
        assertEquals(0.999, paraDoubleOuNulo("0.999")!!, 1e-9)
        assertEquals(0.999, paraDoubleOuNulo("0,999")!!, 1e-9)
        assertNull(paraDoubleOuNulo("  "))
        assertNull(paraDoubleOuNulo("abc"))
    }
}
