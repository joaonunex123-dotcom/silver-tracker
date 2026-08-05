package com.stacking.tracker.ui.cotacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stacking.tracker.core.formatarBrl
import com.stacking.tracker.core.formatarDataHora
import com.stacking.tracker.core.formatarNumero
import com.stacking.tracker.core.formatarPrecoUnitario
import com.stacking.tracker.core.formatarUsd
import com.stacking.tracker.core.paraDoubleOuNulo
import com.stacking.tracker.core.precoPorUnidade
import com.stacking.tracker.core.rotuloPreco
import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.ui.FabricaViewModel
import com.stacking.tracker.ui.componentes.CampoTexto
import com.stacking.tracker.ui.componentes.EstadoVazio
import com.stacking.tracker.ui.componentes.Etiqueta
import com.stacking.tracker.ui.componentes.LinhaDado
import com.stacking.tracker.ui.componentes.Painel
import com.stacking.tracker.ui.componentes.Rotulo
import com.stacking.tracker.ui.componentes.Separador
import com.stacking.tracker.ui.theme.EstiloNumeroHero
import com.stacking.tracker.ui.theme.EstiloNumeroPequeno
import com.stacking.tracker.ui.theme.LocalUnidadePeso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotacaoScreen(
    modifier: Modifier = Modifier,
    viewModel: CotacaoViewModel = viewModel(factory = FabricaViewModel),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var mostrarManual by remember { mutableStateOf(false) }

    LaunchedEffect(estado.mensagem) {
        estado.mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.limparMensagem()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Cotacao", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddings ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SpotAtual(
                    cotacao = estado.atual,
                    atualizando = estado.atualizando,
                    onAtualizar = viewModel::atualizar,
                    onManual = { mostrarManual = true },
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Rotulo("Historico")
                    Text(
                        text = "${estado.historico.size} registros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (estado.historico.isEmpty() && !estado.carregando) {
                item {
                    EstadoVazio(
                        titulo = "Nenhuma cotacao registrada",
                        detalhe = "Atualize pela API ou lance o spot manualmente.",
                    )
                }
            }

            items(estado.historico, key = { it.id }) { cotacao ->
                LinhaHistorico(cotacao = cotacao, onExcluir = { viewModel.excluir(cotacao.id) })
            }
        }
    }

    if (mostrarManual) {
        DialogoCotacaoManual(
            atual = estado.atual,
            onFechar = { mostrarManual = false },
            onConfirmar = { usd, cambio ->
                viewModel.registrarManual(usd, cambio)
                mostrarManual = false
            },
        )
    }
}

@Composable
private fun SpotAtual(
    cotacao: Cotacao?,
    atualizando: Boolean,
    onAtualizar: () -> Unit,
    onManual: () -> Unit,
) {
    val unidade = LocalUnidadePeso.current
    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(
            modifier = interno.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Rotulo("Prata spot  ${rotuloPreco("BRL", unidade)}")
            Text(
                text = cotacao?.let { formatarPrecoUnitario(it.precoOzBrl, unidade) } ?: "--",
                style = EstiloNumeroHero,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (cotacao != null) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    LinhaDado(
                        rotulo = rotuloPreco("USD", unidade),
                        valor = formatarUsd(precoPorUnidade(cotacao.precoOzUsd, unidade)),
                    )
                    Separador()
                    LinhaDado("USD / BRL", formatarNumero(cotacao.usdBrl, casas = 4))
                    Separador()
                    LinhaDado("Atualizado", formatarDataHora(cotacao.data))
                    Separador()
                    LinhaDado("Origem", cotacao.origem.rotulo)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onAtualizar,
                    enabled = !atualizando,
                    modifier = Modifier.weight(1f),
                ) {
                    if (atualizando) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(2.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Atualizar")
                    }
                }
                OutlinedButton(onClick = onManual, modifier = Modifier.weight(1f)) {
                    Text("Lancar manual")
                }
            }
        }
    }
}

@Composable
private fun LinhaHistorico(cotacao: Cotacao, onExcluir: () -> Unit) {
    val unidade = LocalUnidadePeso.current
    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Row(
            modifier = interno.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatarDataHora(cotacao.data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Etiqueta(cotacao.origem.rotulo)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatarPrecoUnitario(cotacao.precoOzBrl, unidade),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatarUsd(precoPorUnidade(cotacao.precoOzUsd, unidade)),
                    style = EstiloNumeroPequeno,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onExcluir) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Excluir cotacao",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DialogoCotacaoManual(
    atual: Cotacao?,
    onFechar: () -> Unit,
    onConfirmar: (Double, Double) -> Unit,
) {
    var usd by remember { mutableStateOf(atual?.precoOzUsd?.let { formatarNumero(it, 2) } ?: "") }
    var cambio by remember { mutableStateOf(atual?.usdBrl?.let { formatarNumero(it, 4) } ?: "") }

    val valorUsd = paraDoubleOuNulo(usd)
    val valorCambio = paraDoubleOuNulo(cambio)
    val valido = (valorUsd ?: 0.0) > 0.0 && (valorCambio ?: 0.0) > 0.0

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Lancar cotacao") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    rotulo = "Prata USD / oz troy",
                    valor = usd,
                    onValor = { usd = it },
                    teclado = KeyboardType.Decimal,
                )
                CampoTexto(
                    rotulo = "Dolar em reais",
                    valor = cambio,
                    onValor = { cambio = it },
                    teclado = KeyboardType.Decimal,
                )
                if (valido) {
                    Text(
                        text = "Resulta em ${formatarBrl(valorUsd!! * valorCambio!!)} por onca troy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(valorUsd ?: 0.0, valorCambio ?: 0.0) },
                enabled = valido,
            ) { Text("Registrar") }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        },
    )
}
