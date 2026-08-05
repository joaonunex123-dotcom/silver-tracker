package com.stacking.tracker.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stacking.tracker.core.UnidadePeso
import com.stacking.tracker.ui.componentes.Painel
import com.stacking.tracker.ui.componentes.Rotulo
import com.stacking.tracker.ui.theme.ModoTema

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    modoTema: ModoTema,
    unidade: UnidadePeso,
    onModoTema: (ModoTema) -> Unit,
    onUnidade: (UnidadePeso) -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Painel(modifier = Modifier.fillMaxWidth()) { interno ->
                Column(
                    modifier = interno.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Rotulo("Unidade de peso")
                    Text(
                        text = "Muda so a exibicao. Os dados continuam guardados em gramas, " +
                            "e a cotacao em oncas troy, que e como o mercado cota.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnidadePeso.entries.forEach { opcao ->
                            BotaoOpcao(
                                rotulo = opcao.rotulo,
                                ativo = opcao == unidade,
                                onClick = { onUnidade(opcao) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Painel(modifier = Modifier.fillMaxWidth()) { interno ->
                Column(
                    modifier = interno.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Rotulo("Tema")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModoTema.entries.forEach { opcao ->
                            BotaoOpcao(
                                rotulo = opcao.rotulo,
                                ativo = opcao == modoTema,
                                onClick = { onModoTema(opcao) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotaoOpcao(
    rotulo: String,
    ativo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (ativo) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                Color.Transparent
            },
            contentColor = if (ativo) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
