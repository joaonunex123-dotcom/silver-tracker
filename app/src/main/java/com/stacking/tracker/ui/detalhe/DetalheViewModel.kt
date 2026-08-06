package com.stacking.tracker.ui.detalhe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacking.tracker.ContainerApp
import com.stacking.tracker.core.PecaCalculada
import com.stacking.tracker.core.ResolvedorSpot
import com.stacking.tracker.core.ResultadoVenda
import com.stacking.tracker.core.calcular
import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.data.local.Venda
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstadoDetalhe(
    val carregando: Boolean = true,
    val calculada: PecaCalculada? = null,
    val vendas: List<ResultadoVenda> = emptyList(),
    val cotacaoNaCompra: Cotacao? = null,
    val cotacaoAtual: Cotacao? = null,
)

class DetalheViewModel(
    private val container: ContainerApp,
    handle: SavedStateHandle,
) : ViewModel() {

    private val pecaId: Long = handle.get<Long>(ARG_PECA_ID) ?: 0L

    private val _eventos = MutableSharedFlow<EventoDetalhe>()
    val eventos: SharedFlow<EventoDetalhe> = _eventos.asSharedFlow()

    val estado: StateFlow<EstadoDetalhe> = combine(
        container.pecaRepository.observarPorId(pecaId),
        container.cotacaoRepository.observarHistorico(),
        container.vendaRepository.observarDaPeca(pecaId),
    ) { peca, historico, vendas ->
        if (peca == null) {
            EstadoDetalhe(carregando = false)
        } else {
            val resolvedor = ResolvedorSpot(historico)
            val atual = historico.firstOrNull()
            EstadoDetalhe(
                carregando = false,
                calculada = peca.calcular(atual, resolvedor, vendas),
                vendas = vendas.map { ResultadoVenda(it, custoUnitario = peca.precoPago) },
                cotacaoNaCompra = resolvedor.em(peca.dataCompra),
                cotacaoAtual = atual,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoDetalhe(),
    )

    /** Registra a saida de [quantidade] pecas a [precoUnitario] cada. */
    fun vender(quantidade: Int, precoUnitario: Double, data: Long) {
        val calculada = estado.value.calculada ?: return
        val limite = calculada.quantidadeEmEstoque
        if (quantidade !in 1..limite) {
            viewModelScope.launch {
                _eventos.emit(EventoDetalhe.Erro("Voce tem $limite peca(s) em estoque."))
            }
            return
        }

        viewModelScope.launch {
            val venda = Venda(
                pecaId = calculada.peca.id,
                quantidade = quantidade,
                precoUnitario = precoUnitario,
                data = data,
            )
            runCatching { container.vendaRepository.registrar(venda) }
                .onSuccess {
                    container.avisarWidget()
                    val r = ResultadoVenda(it, custoUnitario = calculada.peca.precoPago)
                    _eventos.emit(EventoDetalhe.Vendido(r))
                }
                .onFailure { _eventos.emit(EventoDetalhe.Erro(it.message ?: "Falha ao registrar a venda.")) }
        }
    }

    fun desfazerVenda(id: Long) {
        viewModelScope.launch {
            container.vendaRepository.excluir(id)
            container.avisarWidget()
        }
    }

    fun excluir() {
        val peca = estado.value.calculada?.peca ?: return
        viewModelScope.launch {
            runCatching { container.pecaRepository.excluir(peca) }
                .onSuccess {
                    container.avisarWidget()
                    _eventos.emit(EventoDetalhe.Excluido)
                }
                .onFailure { _eventos.emit(EventoDetalhe.Erro(it.message ?: "Falha ao excluir.")) }
        }
    }

    companion object {
        const val ARG_PECA_ID = "pecaId"
    }
}

sealed interface EventoDetalhe {
    data object Excluido : EventoDetalhe
    data class Vendido(val resultado: ResultadoVenda) : EventoDetalhe
    data class Erro(val mensagem: String) : EventoDetalhe
}
