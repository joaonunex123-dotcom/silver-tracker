package com.stacking.tracker.ui.cotacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacking.tracker.ContainerApp
import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.data.repo.ResultadoCotacao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstadoCotacao(
    val carregando: Boolean = true,
    val atual: Cotacao? = null,
    val historico: List<Cotacao> = emptyList(),
    val atualizando: Boolean = false,
    val mensagem: String? = null,
)

class CotacaoViewModel(private val container: ContainerApp) : ViewModel() {

    private val atualizando = MutableStateFlow(false)
    private val mensagem = MutableStateFlow<String?>(null)

    val estado: StateFlow<EstadoCotacao> = combine(
        container.cotacaoRepository.observarHistorico(),
        atualizando,
        mensagem,
    ) { historico, ocupado, aviso ->
        EstadoCotacao(
            carregando = false,
            atual = historico.firstOrNull(),
            historico = historico,
            atualizando = ocupado,
            mensagem = aviso,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoCotacao(),
    )

    fun atualizar() {
        if (atualizando.value) return
        viewModelScope.launch {
            atualizando.value = true
            mensagem.value = when (val resultado = container.cotacaoRepository.atualizar()) {
                is ResultadoCotacao.Sucesso -> "Cotacao atualizada."
                is ResultadoCotacao.Falha -> resultado.mensagem
            }
            atualizando.value = false
            container.avisarWidget()
        }
    }

    /** Escape hatch para quem nao tem chave de API: lancar o spot na mao. */
    fun registrarManual(precoOzUsd: Double, usdBrl: Double) {
        viewModelScope.launch {
            runCatching { container.cotacaoRepository.registrarManual(precoOzUsd, usdBrl) }
                .onSuccess {
                    mensagem.value = "Cotacao manual registrada."
                    container.avisarWidget()
                }
                .onFailure { mensagem.value = it.message ?: "Falha ao registrar a cotacao." }
        }
    }

    fun excluir(id: Long) {
        viewModelScope.launch {
            container.cotacaoRepository.excluir(id)
            container.avisarWidget()
        }
    }

    fun limparMensagem() {
        mensagem.value = null
    }
}
