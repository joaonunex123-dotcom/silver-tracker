package com.stacking.tracker.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacking.tracker.ContainerApp
import com.stacking.tracker.core.GRAMAS_POR_OZ_TROY
import com.stacking.tracker.core.ResolvedorSpot
import com.stacking.tracker.core.calcularPremioPercent
import com.stacking.tracker.core.dataDoSeletor
import com.stacking.tracker.core.hojeEmMillis
import com.stacking.tracker.core.paraCampo
import com.stacking.tracker.core.paraDoubleOuNulo
import com.stacking.tracker.data.local.Peca
import com.stacking.tracker.data.local.TipoPeca
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Campos crus do formulario, do jeito que o usuario digita. */
data class FormPeca(
    val id: Long = 0L,
    val tipo: TipoPeca = TipoPeca.MOEDA,
    val nome: String = "",
    val marca: String = "",
    val peso: String = "",
    val pureza: String = "0,999",
    val preco: String = "",
    val dataCompra: Long = hojeEmMillis(),
    val vendedor: String = "",
    val observacoes: String = "",
    val fotoPath: String? = null,
)

data class EstadoEditor(
    val carregando: Boolean = true,
    val edicao: Boolean = false,
    val form: FormPeca = FormPeca(),
    /** Spot BRL/oz valido na data de compra escolhida (0 se nao houver historico). */
    val precoOzBrlNaData: Double = 0.0,
    val precoOzUsdNaData: Double = 0.0,
    val salvando: Boolean = false,
    /** Erros so aparecem depois da primeira tentativa de salvar. */
    val validar: Boolean = false,
) {
    val pesoGramas: Double? = paraDoubleOuNulo(form.peso)
    val pureza: Double? = paraDoubleOuNulo(form.pureza)
    val precoPago: Double? = paraDoubleOuNulo(form.preco)

    val ozTroy: Double = (pesoGramas ?: 0.0) / GRAMAS_POR_OZ_TROY
    val ozFinas: Double = ozTroy * (pureza ?: 0.0)
    val valorSpotNaData: Double = ozFinas * precoOzBrlNaData
    val premioPercent: Double? = calcularPremioPercent(precoPago ?: 0.0, valorSpotNaData)
    val premioReais: Double? =
        if (valorSpotNaData > 0.0) (precoPago ?: 0.0) - valorSpotNaData else null

    val erroNome: String? = "Informe o nome".takeIf { form.nome.isBlank() }
    val erroPeso: String? = "Peso deve ser maior que zero".takeIf { (pesoGramas ?: 0.0) <= 0.0 }
    val erroPureza: String? = "Pureza deve ficar entre 0 e 1".takeIf {
        val p = pureza
        p == null || p <= 0.0 || p > 1.0
    }
    val erroPreco: String? = "Informe o preco pago".takeIf { precoPago == null || precoPago < 0.0 }

    val podeSalvar: Boolean =
        erroNome == null && erroPeso == null && erroPureza == null && erroPreco == null
}

class EditorViewModel(
    private val container: ContainerApp,
    handle: SavedStateHandle,
) : ViewModel() {

    private val pecaId: Long = handle.get<Long>(ARG_PECA_ID) ?: 0L

    private val form = MutableStateFlow(FormPeca())
    private val carregando = MutableStateFlow(pecaId != 0L)
    private val salvando = MutableStateFlow(false)
    private val validar = MutableStateFlow(false)

    private val _eventos = MutableSharedFlow<EventoEditor>()
    val eventos: SharedFlow<EventoEditor> = _eventos.asSharedFlow()

    val estado: StateFlow<EstadoEditor> = combine(
        form,
        container.cotacaoRepository.observarHistorico(),
        carregando,
        salvando,
        validar,
    ) { f, historico, carregandoAgora, salvandoAgora, validarAgora ->
        val cotacaoNaData = ResolvedorSpot(historico).em(f.dataCompra)
        EstadoEditor(
            carregando = carregandoAgora,
            edicao = pecaId != 0L,
            form = f,
            precoOzBrlNaData = cotacaoNaData?.precoOzBrl ?: 0.0,
            precoOzUsdNaData = cotacaoNaData?.precoOzUsd ?: 0.0,
            salvando = salvandoAgora,
            validar = validarAgora,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoEditor(carregando = pecaId != 0L, edicao = pecaId != 0L),
    )

    init {
        if (pecaId != 0L) {
            viewModelScope.launch {
                container.pecaRepository.porId(pecaId)?.let { peca ->
                    form.value = FormPeca(
                        id = peca.id,
                        tipo = peca.tipo,
                        nome = peca.nome,
                        marca = peca.marca,
                        peso = paraCampo(peca.pesoGramas, casas = 3),
                        pureza = paraCampo(peca.pureza, casas = 4),
                        preco = paraCampo(peca.precoPago, casas = 2),
                        dataCompra = peca.dataCompra,
                        vendedor = peca.vendedor.orEmpty(),
                        observacoes = peca.observacoes.orEmpty(),
                        fotoPath = peca.fotoPath,
                    )
                }
                carregando.value = false
            }
        }
    }

    fun definirTipo(tipo: TipoPeca) = atualizar { it.copy(tipo = tipo) }
    fun definirNome(valor: String) = atualizar { it.copy(nome = valor) }
    fun definirMarca(valor: String) = atualizar { it.copy(marca = valor) }
    fun definirPeso(valor: String) = atualizar { it.copy(peso = valor) }
    fun definirPureza(valor: String) = atualizar { it.copy(pureza = valor) }
    fun definirPreco(valor: String) = atualizar { it.copy(preco = valor) }
    /** [utcMillis] vem do DatePicker, que trabalha em UTC. */
    fun definirData(utcMillis: Long) = atualizar { it.copy(dataCompra = dataDoSeletor(utcMillis)) }
    fun definirVendedor(valor: String) = atualizar { it.copy(vendedor = valor) }
    fun definirObservacoes(valor: String) = atualizar { it.copy(observacoes = valor) }

    fun definirFoto(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val caminho = container.armazenamentoFotos.copiarParaInterno(uri)
            if (caminho == null) {
                _eventos.emit(EventoEditor.Erro("Nao foi possivel copiar a foto."))
            } else {
                atualizar { it.copy(fotoPath = caminho) }
            }
        }
    }

    fun removerFoto() = atualizar { it.copy(fotoPath = null) }

    fun salvar() {
        val atual = estado.value
        if (!atual.podeSalvar) {
            validar.value = true
            return
        }
        if (salvando.value) return

        viewModelScope.launch {
            salvando.value = true
            val f = atual.form
            val peca = Peca(
                id = f.id,
                tipo = f.tipo,
                nome = f.nome.trim(),
                marca = f.marca.trim(),
                pesoGramas = atual.pesoGramas ?: 0.0,
                pureza = atual.pureza ?: 0.0,
                precoPago = atual.precoPago ?: 0.0,
                dataCompra = f.dataCompra,
                vendedor = f.vendedor.trim().ifBlank { null },
                observacoes = f.observacoes.trim().ifBlank { null },
                fotoPath = f.fotoPath,
            )
            val resultado = runCatching { container.pecaRepository.salvar(peca) }
            salvando.value = false
            resultado
                .onSuccess { id -> _eventos.emit(EventoEditor.Salvo(id)) }
                .onFailure { _eventos.emit(EventoEditor.Erro(it.message ?: "Falha ao salvar.")) }
        }
    }

    private fun atualizar(bloco: (FormPeca) -> FormPeca) {
        form.value = bloco(form.value)
    }

    companion object {
        const val ARG_PECA_ID = "pecaId"
    }
}

sealed interface EventoEditor {
    data class Salvo(val id: Long) : EventoEditor
    data class Erro(val mensagem: String) : EventoEditor
}
