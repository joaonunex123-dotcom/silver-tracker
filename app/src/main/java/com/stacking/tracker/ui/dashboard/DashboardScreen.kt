package com.stacking.tracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.stacking.tracker.ui.FabricaViewModel
import com.stacking.tracker.ui.componentes.CartaoMetrica
import com.stacking.tracker.ui.componentes.LinhaDado
import com.stacking.tracker.ui.componentes.Painel
import com.stacking.tracker.ui.componentes.Rotulo
import com.stacking.tracker.ui.componentes.Separador
import com.stacking.tracker.ui.theme.EstiloNumeroHero
import com.stacking.tracker.ui.theme.LocalCoresValor
import com.stacking.tracker.ui.theme.ModoTema
import com.stacking.tracker.ui.theme.para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modoTema: ModoTema,
    onAlternarTema: () -> Unit,
    onVerCotacao: () -> Unit,
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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ValorDeMercado(estado.resumo, onVerCotacao)
            LinhaDeMetricas(estado.resumo)
            BlocoEstoque(estado.resumo)
            if (estado.porTipo.isNotEmpty()) BlocoPorTipo(estado.porTipo)
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Rotulo("Valor de mercado")
            Text(
                text = if (resumo.temCotacao) formatarBrl(resumo.valorMercado) else "--",
                style = EstiloNumeroHero,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${formatarOz(resumo.totalOzFinas)} finas  ${formatarGramas(resumo.totalGramas)} brutos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (resumo.temCotacao) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
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
                TextButton(
                    onClick = onVerCotacao,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("Sem cotacao registrada. Atualizar agora", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun LinhaDeMetricas(resumo: ResumoCarteira) {
    val cores = LocalCoresValor.current

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CartaoMetrica(
            rotulo = "Investido",
            valor = formatarBrl(resumo.totalInvestido),
            apoio = resumo.custoMedioPorOzFina?.let { "${formatarBrl(it)} / oz fina" },
            modifier = Modifier.weight(1f),
        )
        CartaoMetrica(
            rotulo = "Lucro / prejuizo",
            valor = if (resumo.temCotacao) formatarBrlAssinado(resumo.lucro) else "--",
            apoio = resumo.lucroPercent?.let { formatarPercentAssinado(it) },
            corValor = if (resumo.temCotacao) cores.para(resumo.lucro) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BlocoEstoque(resumo: ResumoCarteira) {
    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(modifier = interno.fillMaxWidth()) {
            Rotulo("Estoque")
            LinhaDado("Pecas", resumo.quantidadePecas.toString())
            Separador()
            LinhaDado("Peso bruto", formatarGramas(resumo.totalGramas))
            Separador()
            LinhaDado("Oncas troy (bruto)", formatarOz(resumo.totalOzTroy))
            Separador()
            LinhaDado("Oncas finas (ASW)", formatarOz(resumo.totalOzFinas))
            Separador()
            LinhaDado(
                rotulo = "Premio medio pago",
                valor = resumo.premioMedioPercent?.let { formatarPercentAssinado(it) } ?: "--",
                corValor = resumo.premioMedioPercent?.let {
                    // Premio alto e ruim para quem compra: inverte o sinal da cor.
                    LocalCoresValor.current.para(-it)
                } ?: MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BlocoPorTipo(linhas: List<LinhaTipo>) {
    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(modifier = interno.fillMaxWidth()) {
            Rotulo("Por tipo")
            linhas.forEachIndexed { indice, linha ->
                if (indice > 0) Separador()
                LinhaDado(
                    rotulo = "${linha.tipo.rotulo}  ${linha.quantidade}",
                    valor = formatarOz(linha.ozFinas),
                )
            }
        }
    }
}

@Composable
private fun BlocoSpot(resumo: ResumoCarteira, onVerCotacao: () -> Unit) {
    val cotacao = resumo.cotacao

    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(modifier = interno.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Rotulo("Spot da prata")
                TextButton(onClick = onVerCotacao) {
                    Text("Detalhes", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (cotacao == null) {
                Text(
                    text = "Nenhuma cotacao registrada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinhaDado("USD / oz", formatarUsd(cotacao.precoOzUsd))
                Separador()
                LinhaDado("BRL / oz", formatarBrl(cotacao.precoOzBrl))
                Separador()
                LinhaDado("Atualizado", formatarDataHora(cotacao.data))
            }
        }
    }
}
