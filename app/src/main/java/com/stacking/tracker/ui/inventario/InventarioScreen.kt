package com.stacking.tracker.ui.inventario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stacking.tracker.core.PecaCalculada
import com.stacking.tracker.core.formatarBrl
import com.stacking.tracker.core.formatarData
import com.stacking.tracker.core.formatarPercentAssinado
import com.stacking.tracker.core.formatarQuantidade
import com.stacking.tracker.data.local.TipoPeca
import com.stacking.tracker.ui.FabricaViewModel
import com.stacking.tracker.ui.componentes.EstadoVazio
import com.stacking.tracker.ui.componentes.Etiqueta
import com.stacking.tracker.ui.componentes.Painel
import com.stacking.tracker.ui.componentes.Rotulo
import com.stacking.tracker.ui.theme.EstiloNumeroMedio
import com.stacking.tracker.ui.theme.EstiloNumeroPequeno
import com.stacking.tracker.ui.theme.LocalCoresValor
import com.stacking.tracker.ui.theme.LocalUnidadePeso
import com.stacking.tracker.ui.theme.para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onAbrirPeca: (Long) -> Unit,
    onNovaPeca: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventarioViewModel = viewModel(factory = FabricaViewModel),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Inventario", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNovaPeca,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Adicionar peca")
            }
        },
    ) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
        ) {
            CampoBusca(
                texto = estado.filtro.busca,
                onTexto = viewModel::definirBusca,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            FiltroTipos(
                selecionado = estado.filtro.tipo,
                onTipo = viewModel::definirTipo,
                modifier = Modifier.padding(top = 8.dp),
            )

            BarraResumoEOrdem(
                estado = estado,
                onCriterio = viewModel::definirCriterio,
                onDirecao = viewModel::alternarDirecao,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (!estado.carregando && estado.pecas.isEmpty()) {
                EstadoVazio(
                    titulo = if (estado.totalNoBanco == 0) {
                        "Nenhuma peca cadastrada"
                    } else {
                        "Nenhuma peca corresponde ao filtro"
                    },
                    detalhe = if (estado.totalNoBanco == 0) {
                        "Use o botao + para adicionar a primeira."
                    } else {
                        null
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(estado.pecas, key = { it.peca.id }) { item ->
                        CartaoPeca(
                            item = item,
                            temCotacao = estado.temCotacao,
                            onClick = { onAbrirPeca(item.peca.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoBusca(
    texto: String,
    onTexto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = texto,
        onValueChange = onTexto,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Buscar por nome ou marca", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onTexto("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Limpar busca")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun FiltroTipos(
    selecionado: TipoPeca?,
    onTipo: (TipoPeca?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChipTipo("Todos", selecionado == null) { onTipo(null) }
        TipoPeca.entries.forEach { tipo ->
            ChipTipo(tipo.rotulo, selecionado == tipo) { onTipo(tipo) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipTipo(rotulo: String, selecionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selecionado,
        onClick = onClick,
        label = { Text(rotulo, style = MaterialTheme.typography.bodySmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/** Contagem e total a esquerda, controle de ordenacao a direita. */
@Composable
private fun BarraResumoEOrdem(
    estado: EstadoInventario,
    onCriterio: (CriterioOrdem) -> Unit,
    onDirecao: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuAberto by remember { mutableStateOf(false) }
    val unidade = LocalUnidadePeso.current
    val quantidade = estado.pecas.size
    val total = estado.totalNoBanco

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (quantidade == total) {
                    "$quantidade pecas  ${formatarQuantidade(estado.ozFiltradas, unidade)} de prata pura"
                } else {
                    "$quantidade de $total  ${formatarQuantidade(estado.ozFiltradas, unidade)} de prata pura"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = if (estado.temCotacao) formatarBrl(estado.valorFiltrado) else "--",
                style = EstiloNumeroPequeno,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                TextButton(onClick = { menuAberto = true }) {
                    Text(estado.filtro.criterio.rotulo, style = MaterialTheme.typography.bodySmall)
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    CriterioOrdem.entries.forEach { criterio ->
                        DropdownMenuItem(
                            text = { Text(criterio.rotulo) },
                            onClick = {
                                onCriterio(criterio)
                                menuAberto = false
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onDirecao) {
                Icon(
                    imageVector = if (estado.filtro.decrescente) {
                        Icons.Outlined.ArrowDownward
                    } else {
                        Icons.Outlined.ArrowUpward
                    },
                    contentDescription = if (estado.filtro.decrescente) "Decrescente" else "Crescente",
                )
            }
        }
    }
}

@Composable
private fun CartaoPeca(
    item: PecaCalculada,
    temCotacao: Boolean,
    onClick: () -> Unit,
) {
    val cores = LocalCoresValor.current
    val unidade = LocalUnidadePeso.current
    val peca = item.peca

    Painel(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) { interno ->
        Row(
            modifier = interno.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = peca.nome,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(
                        peca.marca.ifBlank { null },
                        formatarQuantidade(item.pesoTroyOz, unidade),
                        formatarData(peca.dataCompra),
                    ).joinToString("  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Etiqueta(peca.tipo.rotulo)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (temCotacao) formatarBrl(item.valorAtual) else formatarBrl(peca.precoPago),
                    style = EstiloNumeroMedio,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (temCotacao) {
                    item.lucroPercent?.let {
                        Text(
                            text = formatarPercentAssinado(it),
                            style = EstiloNumeroPequeno,
                            color = cores.para(it),
                        )
                    }
                } else {
                    Rotulo("pago")
                }
                Text(
                    text = formatarQuantidade(item.ozFinas, unidade),
                    style = EstiloNumeroPequeno,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
