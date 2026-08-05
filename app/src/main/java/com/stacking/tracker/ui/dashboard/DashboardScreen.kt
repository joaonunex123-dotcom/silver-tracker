package com.stacking.tracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stacking.tracker.core.ResumoCarteira
import com.stacking.tracker.core.formatarBrl
import com.stacking.tracker.core.formatarBrlAssinado
import com.stacking.tracker.core.formatarDataHora
import com.stacking.tracker.core.formatarGramas
import com.stacking.tracker.core.formatarOz
import com.stacking.tracker.core.formatarPercentAssinado
import com.stacking.tracker.core.formatarUsd
import com.stacking.tracker.core.precoPorGrama
import com.stacking.tracker.ui.FabricaViewModel
import com.stacking.tracker.ui.componentes.MetricaCompacta
import com.stacking.tracker.ui.componentes.Painel
import com.stacking.tracker.ui.componentes.Rotulo
import com.stacking.tracker.ui.theme.EstiloNumeroHero
import com.stacking.tracker.ui.theme.EstiloNumeroMedio
import com.stacking.tracker.ui.theme.LocalCoresValor
import com.stacking.tracker.ui.theme.ModoTema
import com.stacking.tracker.ui.theme.para

/**
 * O Painel foi desenhado para caber **sem rolagem** num telefone comum: blocos
 * densos em vez de listas longas. O verticalScroll fica so como rede de seguranca
 * para telas pequenas ou fonte do sistema muito grande — em uso normal nao engata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modoTema: ModoTema,
    onAlternarTema: () -> Unit,
    onVerCotacao: () -> Unit,
    onVerInventario: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = FabricaViewModel),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensagem) {
        estado.mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.limparMensagem()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        // O inset de baixo ja e consumido pela barra de abas; a TopAppBar cuida do
        // de cima sozinha. Sem isso, sobra uma faixa morta acima das abas.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Painel", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = onAlternarTema) {
                        Icon(
                            imageVector = when (modoTema) {
                                ModoTema.ESCURO -> Icons.Outlined.DarkMode
                                ModoTema.CLARO -> Icons.Outlined.LightMode
                                ModoTema.SISTEMA -> Icons.Outlined.SettingsBrightness
                            },
                            contentDescription = "Tema: ${modoTema.rotulo}",
                        )
                    }
                    IconButton(
                        onClick = viewModel::atualizarCotacao,
                        enabled = !estado.atualizandoCotacao,
                    ) {
                        if (estado.atualizandoCotacao) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(4.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar cotacao")
                        }
                    }
                },
            )
        },
    ) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!estado.carregando && estado.resumo.quantidadePecas == 0) {
                PrimeirosPassos(
                    temCotacao = estado.resumo.temCotacao,
                    onVerCotacao = onVerCotacao,
                    onVerInventario = onVerInventario,
                )
            }
            ValorDeMercado(estado.resumo, onVerCotacao)
            LinhaInvestimento(estado.resumo)
            LinhaEstoque(estado.resumo)
            BlocoSpot(estado.resumo, onVerCotacao)
        }
    }
}

@Composable
private fun ValorDeMercado(resumo: ResumoCarteira, onVerCotacao: () -> Unit) {
    val cores = LocalCoresValor.current

    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(
            modifier = interno.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Rotulo("Valor de mercado")
            Text(
                text = if (resumo.temCotacao) formatarBrl(resumo.valorMercado) else "--",
                style = EstiloNumeroHero,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            if (resumo.temCotacao) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatarBrlAssinado(resumo.lucro),
                        style = MaterialTheme.typography.titleSmall,
                        color = cores.para(resumo.lucro),
                    )
                    resumo.lucroPercent?.let {
                        Text(
                            text = formatarPercentAssinado(it),
                            style = MaterialTheme.typography.titleSmall,
                            color = cores.para(it),
                        )
                    }
                }
            } else {
                TextButton(onClick = onVerCotacao, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        text = "Sem cotacao. Registrar agora",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaInvestimento(resumo: ResumoCarteira) {
    val cores = LocalCoresValor.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricaCompacta(
            rotulo = "Investido",
            valor = formatarBrl(resumo.totalInvestido),
            modifier = Modifier.weight(1f),
        )
        MetricaCompacta(
            rotulo = "Premio medio",
            valor = resumo.premioMedioPercent?.let { formatarPercentAssinado(it) } ?: "--",
            // Premio alto e ruim para quem compra: inverte o sinal da cor.
            corValor = resumo.premioMedioPercent?.let { cores.para(-it) }
                ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LinhaEstoque(resumo: ResumoCarteira) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricaCompacta(
            rotulo = "Pecas",
            valor = resumo.quantidadePecas.toString(),
            modifier = Modifier.weight(1f),
        )
        MetricaCompacta(
            rotulo = "Oz finas",
            valor = formatarOz(resumo.totalOzFinas),
            modifier = Modifier.weight(1.3f),
        )
        MetricaCompacta(
            rotulo = "Peso",
            valor = formatarGramas(resumo.totalGramas),
            modifier = Modifier.weight(1.3f),
        )
    }
}

@Composable
private fun BlocoSpot(resumo: ResumoCarteira, onVerCotacao: () -> Unit) {
    val cotacao = resumo.cotacao

    Painel(modifier = Modifier.fillMaxWidth(), paddingV = 12.dp) { interno ->
        Column(
            modifier = interno.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Rotulo("Spot da prata")
                if (cotacao != null) {
                    Text(
                        text = formatarDataHora(cotacao.data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (cotacao == null) {
                TextButton(onClick = onVerCotacao, contentPadding = PaddingValues(0.dp)) {
                    Text("Nenhuma cotacao registrada", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ValorSpot("por grama", formatarBrl(precoPorGrama(cotacao.precoOzBrl)))
                    ValorSpot("BRL / oz", formatarBrl(cotacao.precoOzBrl))
                    ValorSpot("USD / oz", formatarUsd(cotacao.precoOzUsd))
                }
            }
        }
    }
}

@Composable
private fun ValorSpot(rotulo: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = valor,
            style = EstiloNumeroMedio,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * Carteira vazia: a ordem importa. Sem cotacao no historico o premio de qualquer
 * peca cadastrada sai vazio, entao a cotacao vem primeiro.
 */
@Composable
private fun PrimeirosPassos(
    temCotacao: Boolean,
    onVerCotacao: () -> Unit,
    onVerInventario: () -> Unit,
) {
    Painel(modifier = Modifier.fillMaxWidth(), paddingV = 12.dp) { interno ->
        Column(
            modifier = interno.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Rotulo("Primeiros passos")
            Text(
                text = if (temCotacao) {
                    "Cotacao registrada. Agora cadastre suas pecas no Inventario."
                } else {
                    "Registre a cotacao antes das pecas: o premio de cada compra e " +
                        "calculado contra o spot do dia dela."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!temCotacao) {
                    TextButton(
                        onClick = onVerCotacao,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("Lancar cotacao", style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(
                    onClick = onVerInventario,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("Adicionar peca", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
