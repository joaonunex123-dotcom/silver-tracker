package com.stacking.tracker.ui.navegacao

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stacking.tracker.ui.cotacao.CotacaoScreen
import com.stacking.tracker.ui.dashboard.DashboardScreen
import com.stacking.tracker.ui.detalhe.DetalheScreen
import com.stacking.tracker.ui.editor.EditorScreen
import com.stacking.tracker.ui.inventario.InventarioScreen
import com.stacking.tracker.ui.theme.EstiloRotulo
import com.stacking.tracker.ui.theme.ModoTema

object Rotas {
    const val DASHBOARD = "dashboard"
    const val INVENTARIO = "inventario"
    const val COTACAO = "cotacao"
    const val DETALHE = "peca/{pecaId}"
    const val EDITOR = "editor?pecaId={pecaId}"

    fun detalhe(id: Long) = "peca/$id"
    fun editor(id: Long = 0L) = "editor?pecaId=$id"
}

private data class ItemAba(
    val rota: String,
    val rotulo: String,
    val icone: ImageVector,
)

/**
 * Troca de aba preservando o estado de cada uma e sem empilhar back stack.
 * Usado tanto pela barra inferior quanto pelos atalhos do Painel: navegar cru
 * para uma aba faria o botao voltar percorrer um rastro de abas visitadas.
 */
private fun NavHostController.irParaAba(rota: String) {
    if (currentDestination?.route == rota) return
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private val ABAS = listOf(
    ItemAba(Rotas.DASHBOARD, "Painel", Icons.Outlined.PieChart),
    ItemAba(Rotas.INVENTARIO, "Inventario", Icons.Outlined.Inventory2),
    ItemAba(Rotas.COTACAO, "Cotacao", Icons.Outlined.TrendingUp),
)

/** Altura da faixa clicavel, sem contar o inset do sistema. */
private val ALTURA_ABAS = 52.dp

@Composable
fun AppNavegacao(
    modoTema: ModoTema,
    onAlternarTema: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route
    val mostrarAbas = ABAS.any { it.rota == rotaAtual }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        // As telas internas tem o proprio Scaffold e cuidam do inset de topo.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (mostrarAbas) {
                BarraAbas(
                    rotaAtual = rotaAtual,
                    onAba = { rota -> navController.irParaAba(rota) },
                )
            }
        },
    ) { paddings ->
        NavHost(
            navController = navController,
            startDestination = Rotas.DASHBOARD,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
        ) {
            composable(Rotas.DASHBOARD) {
                DashboardScreen(
                    modoTema = modoTema,
                    onAlternarTema = onAlternarTema,
                    onVerCotacao = { navController.irParaAba(Rotas.COTACAO) },
                    onVerInventario = { navController.irParaAba(Rotas.INVENTARIO) },
                )
            }

            composable(Rotas.INVENTARIO) {
                InventarioScreen(
                    onAbrirPeca = { id -> navController.navigate(Rotas.detalhe(id)) },
                    onNovaPeca = { navController.navigate(Rotas.editor()) },
                )
            }

            composable(Rotas.COTACAO) {
                CotacaoScreen()
            }

            composable(
                route = Rotas.DETALHE,
                arguments = listOf(navArgument("pecaId") { type = NavType.LongType }),
            ) {
                DetalheScreen(
                    onVoltar = { navController.popBackStack() },
                    onEditar = { id -> navController.navigate(Rotas.editor(id)) },
                )
            }

            composable(
                route = Rotas.EDITOR,
                arguments = listOf(
                    navArgument("pecaId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
            ) {
                EditorScreen(
                    onVoltar = { navController.popBackStack() },
                    onSalvo = { navController.popBackStack() },
                )
            }
        }
    }
}

/**
 * Barra de abas propria em vez do NavigationBar do Material 3, que tem 80dp fixos
 * por especificacao e ainda soma o inset do sistema — no aparelho isso passava de
 * 100dp para tres icones. Aqui a faixa clicavel tem [ALTURA_ABAS] e o inset entra
 * como padding, entao o app ganha de volta quase metade dessa altura.
 */
@Composable
private fun BarraAbas(
    rotaAtual: String?,
    onAba: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(ALTURA_ABAS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ABAS.forEach { aba ->
                val ativo = rotaAtual == aba.rota
                val cor = if (ativo) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onAba(aba.rota) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = aba.icone,
                        contentDescription = aba.rotulo,
                        tint = cor,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = aba.rotulo,
                        style = EstiloRotulo,
                        color = cor,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}
