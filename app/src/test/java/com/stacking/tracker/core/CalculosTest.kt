package com.stacking.tracker.core

import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.data.local.Peca
import com.stacking.tracker.data.local.TipoPeca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculosTest {

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
        val historico = listOf(
            Cotacao(id = 3, data = 300, precoOzUsd = 30.0, precoOzBrl = 165.0),
            Cotacao(id = 2, data = 200, precoOzUsd = 28.0, precoOzBrl = 154.0),
            Cotacao(id = 1, data = 100, precoOzUsd = 25.0, precoOzBrl = 137.5),
        )
        val resolvedor = ResolvedorSpot(historico)

        assertEquals(154.0, resolvedor.precoOzBrlEm(250), 1e-9)
        assertEquals(165.0, resolvedor.precoOzBrlEm(999), 1e-9)
        // Compra anterior a todo o historico cai na cotacao mais antiga.
        assertEquals(137.5, resolvedor.precoOzBrlEm(50), 1e-9)
        assertEquals(0.0, ResolvedorSpot.VAZIO.precoOzBrlEm(250), 1e-9)
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
    fun `leitura de numero aceita virgula e ponto`() {
        assertEquals(1234.56, paraDoubleOuNulo("1.234,56")!!, 1e-9)
        assertEquals(0.999, paraDoubleOuNulo("0.999")!!, 1e-9)
        assertEquals(0.999, paraDoubleOuNulo("0,999")!!, 1e-9)
        assertNull(paraDoubleOuNulo("  "))
        assertNull(paraDoubleOuNulo("abc"))
    }
}
