package com.stacking.tracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacking.tracker.ContainerApp
import com.stacking.tracker.core.ResolvedorSpot
import com.stacking.tracker.core.ResumoCarteira
import com.stacking.tracker.core.calcular
import com.stacking.tracker.core.resumirCarteira
import com.stacking.tracker.data.local.TipoPeca
import com.stacking.tracker.data.repo.ResultadoCotacao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LinhaTipo(
    val tipo: TipoPeca,
    val quantidade: Int,
    val ozFinas: Double,
)

data class EstadoDashboard(
    val carregando: Boolean = true,
    val resumo: ResumoCarteira = ResumoCarteira(),
    val porTipo: List<LinhaTipo> = emptyList(),
    val atualizandoCotacao: Boolean = false,
    val mensagem: String? = null,
)

class DashboardViewModel(private val container: ContainerApp) : ViewModel() {

    private val atualizando = MutableStateFlow(false)
    private val mensagem = MutableStateFlow<String?>(null)

    val estado: StateFlow<EstadoDashboard> = combine(
        container.pecaRepository.observarTodas(),
        container.cotacaoRepository.observarHistorico(),
        atualizando,
        mensagem,
    ) { pecas, historico, ocupado, aviso ->
        val resolvedor = ResolvedorSpot(historico)
        val atual = historico.firstOrNull()
        val calculadas = pecas.map { it.calcular(atual, resolvedor) }

        EstadoDashboard(
            carregando = false,
            resumo = resumirCarteira(calculadas, atual),
            porTipo = TipoPeca.entries.mapNotNull { tipo ->
                val doTipo = calculadas.filter { it.peca.tipo == tipo }
                if (doTipo.isEmpty()) {
                    null
                } else {
                    LinhaTipo(tipo, doTipo.size, doTipo.sumOf { it.ozFinas })
                }
            },
            atualizandoCotacao = ocupado,
            mensagem = aviso,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoDashboard(),
    )

    fun atualizarCotacao() {
        if (atualizando.value) return
        viewModelScope.launch {
            atualizando.value = true
            mensagem.value = when (val resultado = container.cotacaoRepository.atualizar()) {
                is ResultadoCotacao.Sucesso -> null
                is ResultadoCotacao.Falha -> resultado.mensagem
            }
            atualizando.value = false
        }
    }

    fun limparMensagem() {
        mensagem.value = null
    }
}
