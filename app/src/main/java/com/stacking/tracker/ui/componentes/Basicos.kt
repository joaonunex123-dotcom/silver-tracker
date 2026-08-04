package com.stacking.tracker.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stacking.tracker.ui.theme.EstiloNumeroGrande
import com.stacking.tracker.ui.theme.EstiloNumeroMedio
import com.stacking.tracker.ui.theme.EstiloRotulo

/** Rotulo curto em caixa alta acima de um numero. */
@Composable
fun Rotulo(
    texto: String,
    modifier: Modifier = Modifier,
    cor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = texto.uppercase(),
        style = EstiloRotulo,
        color = cor,
        modifier = modifier,
    )
}

/** Superficie base de cartao: borda fina, sem elevacao, sem sombra. */
@Composable
fun Painel(
    modifier: Modifier = Modifier,
    conteudo: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp),
            ),
    ) {
        conteudo(Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
    }
}

/** Cartao com rotulo em cima e numero grande embaixo. */
@Composable
fun CartaoMetrica(
    rotulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    apoio: String? = null,
    corValor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Painel(modifier = modifier) { interno ->
        Column(
            modifier = interno.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Rotulo(rotulo)
            Text(
                text = valor,
                style = EstiloNumeroGrande,
                color = corValor,
                maxLines = 1,
            )
            if (apoio != null) {
                Text(
                    text = apoio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Linha rotulo a esquerda, valor a direita. Base das listas de dados. */
@Composable
fun LinhaDado(
    rotulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    corValor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = EstiloNumeroMedio,
            color = corValor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun Separador(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline,
    )
}

/** Estado vazio: uma frase, sem ilustracao. */
@Composable
fun EstadoVazio(
    titulo: String,
    detalhe: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (detalhe != null) {
            Text(
                text = detalhe,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Etiqueta discreta usada no tipo da peca e na origem da cotacao. */
@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = texto.uppercase(),
        style = EstiloRotulo,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
