package com.stacking.tracker.ui.navegacao

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
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

private val ABAS = listOf(
    ItemAba(Rotas.DASHBOARD, "Painel", Icons.Outlined.PieChart),
    ItemAba(Rotas.INVENTARIO, "Inventario", Icons.Outlined.Inventory2),
    ItemAba(Rotas.COTACAO, "Cotacao", Icons.Outlined.TrendingUp),
)

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
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 0.dp,
                ) {
                    ABAS.forEach { aba ->
                        NavigationBarItem(
                            selected = rotaAtual == aba.rota,
                            onClick = {
                                if (rotaAtual != aba.rota) {
                                    navController.navigate(aba.rota) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(aba.icone, contentDescription = null) },
                            label = { Text(aba.rotulo, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
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
                    onVerCotacao = { navController.navigate(Rotas.COTACAO) },
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
