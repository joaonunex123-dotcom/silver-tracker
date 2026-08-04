package com.stacking.tracker.ui.detalhe

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.stacking.tracker.core.PecaCalculada
import com.stacking.tracker.core.formatarBrl
import com.stacking.tracker.core.formatarBrlAssinado
import com.stacking.tracker.core.formatarData
import com.stacking.tracker.core.formatarDataHora
import com.stacking.tracker.core.formatarGramas
import com.stacking.tracker.core.formatarOz
import com.stacking.tracker.core.formatarPercentAssinado
import com.stacking.tracker.core.formatarPureza
import com.stacking.tracker.core.formatarUsd
import com.stacking.tracker.ui.FabricaViewModel
import com.stacking.tracker.ui.componentes.EstadoVazio
import com.stacking.tracker.ui.componentes.Etiqueta
import com.stacking.tracker.ui.componentes.LinhaDado
import com.stacking.tracker.ui.componentes.Painel
import com.stacking.tracker.ui.componentes.Rotulo
import com.stacking.tracker.ui.componentes.Separador
import com.stacking.tracker.ui.theme.EstiloNumeroHero
import com.stacking.tracker.ui.theme.LocalCoresValor
import com.stacking.tracker.ui.theme.para
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheScreen(
    onVoltar: () -> Unit,
    onEditar: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetalheViewModel = viewModel(factory = FabricaViewModel),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmarExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoDetalhe.Excluido -> onVoltar()
                is EventoDetalhe.Erro -> snackbar.showSnackbar(evento.mensagem)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = estado.calculada?.peca?.nome ?: "Peca",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    estado.calculada?.let { calculada ->
                        IconButton(onClick = { onEditar(calculada.peca.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { confirmarExclusao = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddings ->
        val calculada = estado.calculada

        if (!estado.carregando && calculada == null) {
            EstadoVazio(
                titulo = "Peca nao encontrada",
                detalhe = "Ela pode ter sido excluida.",
                modifier = Modifier.padding(paddings),
            )
            return@Scaffold
        }

        if (calculada == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            calculada.peca.fotoPath?.let { caminho ->
                Image(
                    painter = rememberAsyncImagePainter(File(caminho)),
                    contentDescription = "Foto da peca",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }

            BlocoValor(calculada)
            BlocoFicha(calculada)
            BlocoPremio(calculada, estado)
            calculada.peca.observacoes?.let { BlocoObservacoes(it) }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir peca") },
            text = { Text("Esta acao nao pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    viewModel.excluir()
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun BlocoValor(calculada: PecaCalculada) {
    val cores = LocalCoresValor.current
    val temCotacao = calculada.precoOzBrlAtual > 0.0

    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(
            modifier = interno.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Rotulo("Valor atual")
            Text(
                text = if (temCotacao) formatarBrl(calculada.valorAtual) else "--",
                style = EstiloNumeroHero,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (temCotacao) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    calculada.lucro?.let {
                        Text(
                            text = formatarBrlAssinado(it),
                            style = MaterialTheme.typography.titleSmall,
                            color = cores.para(it),
                        )
                    }
                    calculada.lucroPercent?.let {
                        Text(
                            text = formatarPercentAssinado(it),
                            style = MaterialTheme.typography.titleSmall,
                            color = cores.para(it),
                        )
                    }
                }
            } else {
                Text(
                    text = "Sem cotacao registrada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BlocoFicha(calculada: PecaCalculada) {
    val peca = calculada.peca

    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(modifier = interno.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Rotulo("Ficha")
                Etiqueta(peca.tipo.rotulo)
            }
            LinhaDado("Marca", peca.marca.ifBlank { "--" })
            Separador()
            LinhaDado("Peso bruto", formatarGramas(peca.pesoGramas))
            Separador()
            LinhaDado("Oncas troy", formatarOz(calculada.pesoTroyOz))
            Separador()
            LinhaDado("Pureza", formatarPureza(peca.pureza))
            Separador()
            LinhaDado("Oncas finas (ASW)", formatarOz(calculada.ozFinas))
            Separador()
            LinhaDado("Preco pago", formatarBrl(peca.precoPago))
            Separador()
            LinhaDado("Data da compra", formatarData(peca.dataCompra))
            Separador()
            LinhaDado("Vendedor", peca.vendedor?.ifBlank { null } ?: "--")
        }
    }
}

@Composable
private fun BlocoPremio(calculada: PecaCalculada, estado: EstadoDetalhe) {
    val cores = LocalCoresValor.current

    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(modifier = interno.fillMaxWidth()) {
            Rotulo("Premio sobre o spot")

            if (calculada.valorSpotNaCompra <= 0.0) {
                Text(
                    text = "Nao ha cotacao registrada ate a data da compra, entao nao da para calcular o premio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                estado.cotacaoNaCompra?.let {
                    LinhaDado("Spot de referencia", "${formatarBrl(it.precoOzBrl)} / oz")
                    Separador()
                    LinhaDado("Data da referencia", formatarDataHora(it.data))
                    Separador()
                }
                LinhaDado("Valor do metal", formatarBrl(calculada.valorSpotNaCompra))
                Separador()
                calculada.premioReais?.let {
                    LinhaDado("Premio em R$", formatarBrl(it), corValor = cores.para(-it))
                    Separador()
                }
                LinhaDado(
                    rotulo = "Premio",
                    valor = calculada.premioPercent?.let { formatarPercentAssinado(it) } ?: "--",
                    corValor = calculada.premioPercent?.let { cores.para(-it) }
                        ?: MaterialTheme.colorScheme.onSurface,
                )
            }

            estado.cotacaoAtual?.let {
                Separador()
                LinhaDado("Spot atual", "${formatarBrl(it.precoOzBrl)} / oz")
                Separador()
                LinhaDado("Spot atual (USD)", "${formatarUsd(it.precoOzUsd)} / oz")
            }
        }
    }
}

@Composable
private fun BlocoObservacoes(texto: String) {
    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(
            modifier = interno.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Rotulo("Observacoes")
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
