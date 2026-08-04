package com.stacking.tracker.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacking.tracker.ContainerApp
import com.stacking.tracker.core.PecaCalculada
import com.stacking.tracker.core.ResolvedorSpot
import com.stacking.tracker.core.calcular
import com.stacking.tracker.data.local.TipoPeca
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class CriterioOrdem(val rotulo: String) {
    DATA("Data"),
    VALOR("Valor"),
    PESO("Peso"),
}

data class FiltroInventario(
    val busca: String = "",
    val tipo: TipoPeca? = null,
    val criterio: CriterioOrdem = CriterioOrdem.DATA,
    val decrescente: Boolean = true,
)

data class EstadoInventario(
    val carregando: Boolean = true,
    val filtro: FiltroInventario = FiltroInventario(),
    val pecas: List<PecaCalculada> = emptyList(),
    val totalNoBanco: Int = 0,
    val temCotacao: Boolean = false,
    val valorFiltrado: Double = 0.0,
    val ozFiltradas: Double = 0.0,
)

class InventarioViewModel(container: ContainerApp) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroInventario())

    val estado: StateFlow<EstadoInventario> = combine(
        container.pecaRepository.observarTodas(),
        container.cotacaoRepository.observarHistorico(),
        filtro,
    ) { pecas, historico, f ->
        val resolvedor = ResolvedorSpot(historico)
        val atual = historico.firstOrNull()
        val calculadas = pecas.map { it.calcular(atual, resolvedor) }

        val termo = f.busca.trim().lowercase()
        val filtradas = calculadas
            .filter { f.tipo == null || it.peca.tipo == f.tipo }
            .filter {
                termo.isEmpty() ||
                    it.peca.nome.lowercase().contains(termo) ||
                    it.peca.marca.lowercase().contains(termo)
            }
            .let { lista -> ordenar(lista, f) }

        EstadoInventario(
            carregando = false,
            filtro = f,
            pecas = filtradas,
            totalNoBanco = pecas.size,
            temCotacao = (atual?.precoOzBrl ?: 0.0) > 0.0,
            valorFiltrado = filtradas.sumOf { it.valorAtual },
            ozFiltradas = filtradas.sumOf { it.ozFinas },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoInventario(),
    )

    private fun ordenar(lista: List<PecaCalculada>, f: FiltroInventario): List<PecaCalculada> {
        val comparador: Comparator<PecaCalculada> = when (f.criterio) {
            CriterioOrdem.DATA -> compareBy({ it.peca.dataCompra }, { it.peca.id })
            // Sem cotacao o valor atual e zero para todas: cai no preco pago,
            // que e a melhor aproximacao disponivel offline.
            CriterioOrdem.VALOR -> compareBy {
                if (it.valorAtual > 0.0) it.valorAtual else it.peca.precoPago
            }
            CriterioOrdem.PESO -> compareBy { it.peca.pesoGramas }
        }
        return if (f.decrescente) lista.sortedWith(comparador.reversed()) else lista.sortedWith(comparador)
    }

    fun definirBusca(texto: String) {
        filtro.value = filtro.value.copy(busca = texto)
    }

    fun definirTipo(tipo: TipoPeca?) {
        filtro.value = filtro.value.copy(tipo = tipo)
    }

    fun definirCriterio(criterio: CriterioOrdem) {
        filtro.value = filtro.value.copy(criterio = criterio)
    }

    fun alternarDirecao() {
        filtro.value = filtro.value.copy(decrescente = !filtro.value.decrescente)
    }
}
