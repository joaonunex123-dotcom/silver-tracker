package com.stacking.tracker.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.stacking.tracker.core.dataParaSeletor
import com.stacking.tracker.core.formatarBrl
import com.stacking.tracker.core.formatarData
import com.stacking.tracker.core.formatarPercentAssinado
import com.stacking.tracker.core.formatarPrecoUnitario
import com.stacking.tracker.core.formatarQuantidade
import com.stacking.tracker.data.local.TipoPeca
import com.stacking.tracker.ui.FabricaViewModel
import com.stacking.tracker.ui.componentes.CampoTexto
import com.stacking.tracker.ui.componentes.LinhaDado
import com.stacking.tracker.ui.componentes.Painel
import com.stacking.tracker.ui.componentes.Rotulo
import com.stacking.tracker.ui.componentes.Separador
import com.stacking.tracker.ui.theme.LocalCoresValor
import com.stacking.tracker.ui.theme.LocalUnidadePeso
import com.stacking.tracker.ui.theme.para
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onVoltar: () -> Unit,
    onSalvo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel(factory = FabricaViewModel),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var mostrarSeletorData by remember { mutableStateOf(false) }

    val seletorFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> viewModel.definirFoto(uri) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EventoEditor.Salvo -> onSalvo(evento.id)
                is EventoEditor.Erro -> snackbar.showSnackbar(evento.mensagem)
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
                        text = if (estado.edicao) "Editar peca" else "Nova peca",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::salvar, enabled = !estado.salvando) {
                        Text("Salvar")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SeletorTipo(
                selecionado = estado.form.tipo,
                onTipo = viewModel::definirTipo,
            )

            CampoTexto(
                rotulo = "Nome",
                valor = estado.form.nome,
                onValor = viewModel::definirNome,
                erro = if (estado.validar) estado.erroNome else null,
            )

            CampoTexto(
                rotulo = "Marca / casa",
                valor = estado.form.marca,
                onValor = viewModel::definirMarca,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    rotulo = "Peso",
                    valor = estado.form.peso,
                    onValor = viewModel::definirPeso,
                    sufixo = "g",
                    teclado = KeyboardType.Decimal,
                    erro = if (estado.validar) estado.erroPeso else null,
                    modifier = Modifier.weight(1f),
                )
                CampoTexto(
                    rotulo = "Pureza",
                    valor = estado.form.pureza,
                    onValor = viewModel::definirPureza,
                    teclado = KeyboardType.Decimal,
                    apoio = "0 a 1",
                    erro = if (estado.validar) estado.erroPureza else null,
                    modifier = Modifier.weight(1f),
                )
            }

            CampoTexto(
                rotulo = "Preco pago",
                valor = estado.form.preco,
                onValor = viewModel::definirPreco,
                sufixo = "R$",
                teclado = KeyboardType.Decimal,
                erro = if (estado.validar) estado.erroPreco else null,
            )

            CampoData(
                data = estado.form.dataCompra,
                onAbrir = { mostrarSeletorData = true },
            )

            Derivados(estado)

            CampoTexto(
                rotulo = "Vendedor (opcional)",
                valor = estado.form.vendedor,
                onValor = viewModel::definirVendedor,
            )

            CampoTexto(
                rotulo = "Observacoes (opcional)",
                valor = estado.form.observacoes,
                onValor = viewModel::definirObservacoes,
                linhaUnica = false,
                minLinhas = 3,
                imeAction = ImeAction.Default,
            )

            BlocoFoto(
                caminho = estado.form.fotoPath,
                onEscolher = {
                    seletorFoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemover = viewModel::removerFoto,
            )
        }
    }

    if (mostrarSeletorData) {
        // O DatePicker fala UTC; a data guardada e meia-noite local.
        val estadoData = rememberDatePickerState(
            initialSelectedDateMillis = dataParaSeletor(estado.form.dataCompra),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSeletorData = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoData.selectedDateMillis?.let(viewModel::definirData)
                    mostrarSeletorData = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSeletorData = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoData)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorTipo(selecionado: TipoPeca, onTipo: (TipoPeca) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rotulo("Tipo")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoPeca.entries.forEach { tipo ->
                val ativo = tipo == selecionado
                OutlinedButton(
                    onClick = { onTipo(tipo) },
                    modifier = Modifier.weight(1f),
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
                    Text(tipo.rotulo, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CampoData(data: Long, onAbrir: () -> Unit) {
    Painel(modifier = Modifier.fillMaxWidth().clickable(onClick = onAbrir)) { interno ->
        Row(
            modifier = interno.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Rotulo("Data da compra")
            Text(
                text = formatarData(data),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Numeros calculados enquanto o usuario digita. */
@Composable
private fun Derivados(estado: EstadoEditor) {
    val cores = LocalCoresValor.current
    val unidade = LocalUnidadePeso.current

    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(modifier = interno.fillMaxWidth()) {
            Rotulo("Calculado")
            LinhaDado("Peso bruto", formatarQuantidade(estado.ozTroy, unidade))
            Separador()
            LinhaDado("Prata pura", formatarQuantidade(estado.ozFinas, unidade))
            Separador()

            if (estado.precoOzBrlNaData > 0.0) {
                LinhaDado(
                    rotulo = "Spot na data",
                    valor = "${formatarPrecoUnitario(estado.precoOzBrlNaData, unidade)} / ${unidade.sufixo}",
                )
                Separador()
                LinhaDado("Valor do metal", formatarBrl(estado.valorSpotNaData))
                Separador()
                LinhaDado(
                    rotulo = "Premio pago",
                    valor = estado.premioPercent?.let { formatarPercentAssinado(it) } ?: "--",
                    // Premio alto e ruim para quem compra: inverte o sinal da cor.
                    corValor = estado.premioPercent?.let { cores.para(-it) }
                        ?: MaterialTheme.colorScheme.onSurface,
                )
                estado.premioReais?.let {
                    Separador()
                    LinhaDado("Premio em R$", formatarBrl(it), corValor = cores.para(-it))
                }
            } else {
                Text(
                    text = "Sem cotacao registrada ate esta data: o premio nao pode ser calculado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BlocoFoto(
    caminho: String?,
    onEscolher: () -> Unit,
    onRemover: () -> Unit,
) {
    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(
            modifier = interno.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Rotulo("Foto")
                Row {
                    TextButton(onClick = onEscolher) {
                        Text(
                            text = if (caminho == null) "Escolher" else "Trocar",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (caminho != null) {
                        TextButton(onClick = onRemover) {
                            Text("Remover", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (caminho != null) {
                Image(
                    painter = rememberAsyncImagePainter(File(caminho)),
                    contentDescription = "Foto da peca",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
