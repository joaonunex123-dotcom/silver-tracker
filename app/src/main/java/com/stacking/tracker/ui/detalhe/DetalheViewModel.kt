package com.stacking.tracker.ui.detalhe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacking.tracker.ContainerApp
import com.stacking.tracker.core.PecaCalculada
import com.stacking.tracker.core.ResolvedorSpot
import com.stacking.tracker.core.calcular
import com.stacking.tracker.data.local.Cotacao
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
    ) { peca, historico ->
        if (peca == null) {
            EstadoDetalhe(carregando = false)
        } else {
            val resolvedor = ResolvedorSpot(historico)
            val atual = historico.firstOrNull()
            EstadoDetalhe(
                carregando = false,
                calculada = peca.calcular(atual, resolvedor),
                cotacaoNaCompra = resolvedor.em(peca.dataCompra),
                cotacaoAtual = atual,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoDetalhe(),
    )

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
    data class Erro(val mensagem: String) : EventoDetalhe
}
