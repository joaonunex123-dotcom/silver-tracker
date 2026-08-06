package com.stacking.tracker.ui.detalhe

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.stacking.tracker.core.PecaCalculada
import com.stacking.tracker.core.ResultadoVenda
import com.stacking.tracker.core.dataDoSeletor
import com.stacking.tracker.core.dataParaSeletor
import com.stacking.tracker.core.formatarBrl
import com.stacking.tracker.core.formatarBrlAssinado
import com.stacking.tracker.core.formatarData
import com.stacking.tracker.core.formatarDataHora
import com.stacking.tracker.core.formatarPercentAssinado
import com.stacking.tracker.core.formatarPrecoUnitario
import com.stacking.tracker.core.formatarPureza
import com.stacking.tracker.core.formatarQuantidade
import com.stacking.tracker.core.formatarUsd
import com.stacking.tracker.core.hojeEmMillis
import com.stacking.tracker.core.paraDoubleOuNulo
import com.stacking.tracker.core.precoPorUnidade
import com.stacking.tracker.core.rotuloPreco
import com.stacking.tracker.ui.FabricaViewModel
import com.stacking.tracker.ui.componentes.CampoTexto
import com.stacking.tracker.ui.componentes.EstadoVazio
import com.stacking.tracker.ui.componentes.Etiqueta
import com.stacking.tracker.ui.componentes.LinhaDado
import com.stacking.tracker.ui.componentes.Painel
import com.stacking.tracker.ui.componentes.Rotulo
import com.stacking.tracker.ui.componentes.Separador
import com.stacking.tracker.ui.theme.EstiloNumeroHero
import com.stacking.tracker.ui.theme.EstiloNumeroMedio
import com.stacking.tracker.ui.theme.LocalCoresValor
import com.stacking.tracker.ui.theme.LocalUnidadePeso
import com.stacking.tracker.ui.theme.para
import java.io.File
import kotlin.math.abs

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
    var mostrarVenda by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoDetalhe.Excluido -> onVoltar()
                is EventoDetalhe.Erro -> snackbar.showSnackbar(evento.mensagem)
                is EventoDetalhe.Vendido -> {
                    mostrarVenda = false
                    val r = evento.resultado
                    val verbo = if (r.ganhou) "Lucro" else "Prejuizo"
                    snackbar.showSnackbar(
                        "Venda registrada. $verbo de ${formatarBrl(abs(r.lucro))}" +
                            (r.lucroPercent?.let { " (${formatarPercentAssinado(it)})" } ?: ""),
                    )
                }
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
            if (calculada.quantidadeEmEstoque > 0) {
                OutlinedButton(
                    onClick = { mostrarVenda = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = if (calculada.quantidade > 1) {
                            "Vender  (${calculada.quantidadeEmEstoque} em estoque)"
                        } else {
                            "Vender"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            BlocoFicha(calculada)
            if (estado.vendas.isNotEmpty()) {
                BlocoVendas(
                    vendas = estado.vendas,
                    totalRealizado = calculada.lucroRealizado,
                    onDesfazer = viewModel::desfazerVenda,
                )
            }
            BlocoPremio(calculada, estado)
            calculada.peca.observacoes?.let { BlocoObservacoes(it) }
        }
    }

    val emEstoque = estado.calculada?.quantidadeEmEstoque ?: 0
    if (mostrarVenda && emEstoque > 0) {
        DialogoVenda(
            emEstoque = emEstoque,
            custoUnitario = estado.calculada?.peca?.precoPago ?: 0.0,
            onFechar = { mostrarVenda = false },
            onConfirmar = { qtd, preco, data -> viewModel.vender(qtd, preco, data) },
        )
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
    val unidade = LocalUnidadePeso.current

    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(modifier = interno.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Rotulo("Ficha")
                Etiqueta(peca.tipo.rotulo)
            }
            val lote = calculada.quantidade > 1

            LinhaDado("Marca", peca.marca.ifBlank { "--" })
            Separador()
            if (lote) {
                LinhaDado("Quantidade", "${calculada.quantidade} pecas")
                Separador()
                LinhaDado("Peso por peca", formatarQuantidade(calculada.pesoTroyOzUnidade, unidade))
                Separador()
            }
            LinhaDado(
                rotulo = if (lote) "Peso bruto do lote" else "Peso bruto",
                valor = formatarQuantidade(calculada.pesoTroyOz, unidade),
            )
            Separador()
            LinhaDado("Pureza", formatarPureza(peca.pureza))
            Separador()
            LinhaDado(
                rotulo = if (lote) "Prata pura do lote" else "Prata pura",
                valor = formatarQuantidade(calculada.ozFinas, unidade),
            )
            Separador()
            if (lote) {
                LinhaDado("Preco por peca", formatarBrl(peca.precoPago))
                Separador()
            }
            LinhaDado(
                rotulo = if (lote) "Total pago" else "Preco pago",
                valor = formatarBrl(calculada.precoPagoTotal),
            )
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
    val unidade = LocalUnidadePeso.current

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
                    LinhaDado(
                        rotulo = "Spot de referencia",
                        valor = "${formatarPrecoUnitario(it.precoOzBrl, unidade)} / ${unidade.sufixo}",
                    )
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
                LinhaDado(
                    rotulo = rotuloPreco("Spot BRL", unidade),
                    valor = formatarPrecoUnitario(it.precoOzBrl, unidade),
                )
                Separador()
                LinhaDado(
                    rotulo = rotuloPreco("Spot USD", unidade),
                    valor = formatarUsd(precoPorUnidade(it.precoOzUsd, unidade)),
                )
            }
        }
    }
}

/**
 * Registro de venda. Mostra o resultado antes de confirmar, para nao ser preciso
 * gravar so para descobrir se o negocio foi bom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoVenda(
    emEstoque: Int,
    custoUnitario: Double,
    onFechar: () -> Unit,
    onConfirmar: (Int, Double, Long) -> Unit,
) {
    val cores = LocalCoresValor.current
    var qtdTexto by remember { mutableStateOf("1") }
    var precoTexto by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(hojeEmMillis()) }
    var mostrarData by remember { mutableStateOf(false) }

    val qtd = qtdTexto.trim().toIntOrNull()
    val preco = paraDoubleOuNulo(precoTexto)
    val valido = qtd != null && qtd in 1..emEstoque && preco != null && preco >= 0.0

    val recebido = (preco ?: 0.0) * (qtd ?: 0)
    val custo = custoUnitario * (qtd ?: 0)
    val lucro = recebido - custo
    val lucroPercent = if (custo > 0.0) lucro / custo * 100.0 else null

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Registrar venda") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoTexto(
                        rotulo = "Quantidade",
                        valor = qtdTexto,
                        onValor = { qtdTexto = it.filter { c -> c.isDigit() } },
                        teclado = KeyboardType.Number,
                        apoio = "Ate $emEstoque",
                        erro = if (qtd != null && qtd !in 1..emEstoque) "Max $emEstoque" else null,
                        modifier = Modifier.weight(1f),
                    )
                    CampoTexto(
                        rotulo = "Preco por peca",
                        valor = precoTexto,
                        onValor = { precoTexto = it },
                        sufixo = "R$",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1.3f),
                    )
                }

                Painel(modifier = Modifier.fillMaxWidth(), paddingV = 10.dp) { interno ->
                    Column(modifier = interno.fillMaxWidth()) {
                        Rotulo("Data da venda")
                        TextButton(
                            onClick = { mostrarData = true },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(formatarData(data), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                if (valido && qtd != null && qtd > 0) {
                    Painel(modifier = Modifier.fillMaxWidth(), paddingV = 10.dp) { interno ->
                        Column(modifier = interno.fillMaxWidth()) {
                            LinhaDado("Vai receber", formatarBrl(recebido))
                            Separador()
                            LinhaDado("Custou", formatarBrl(custo))
                            Separador()
                            LinhaDado(
                                rotulo = if (lucro >= 0.0) "Lucro" else "Prejuizo",
                                valor = formatarBrlAssinado(lucro) +
                                    (lucroPercent?.let { "   ${formatarPercentAssinado(it)}" } ?: ""),
                                corValor = cores.para(lucro),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(qtd ?: 0, preco ?: 0.0, data) },
                enabled = valido,
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )

    if (mostrarData) {
        val estadoData = rememberDatePickerState(initialSelectedDateMillis = dataParaSeletor(data))
        DatePickerDialog(
            onDismissRequest = { mostrarData = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoData.selectedDateMillis?.let { data = dataDoSeletor(it) }
                    mostrarData = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarData = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoData)
        }
    }
}

/** Historico de saidas desta peca, cada uma com seu proprio resultado. */
@Composable
private fun BlocoVendas(
    vendas: List<ResultadoVenda>,
    totalRealizado: Double,
    onDesfazer: (Long) -> Unit,
) {
    val cores = LocalCoresValor.current

    Painel(modifier = Modifier.fillMaxWidth()) { interno ->
        Column(modifier = interno.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Rotulo("Vendas")
                Text(
                    text = formatarBrlAssinado(totalRealizado),
                    style = MaterialTheme.typography.titleSmall,
                    color = cores.para(totalRealizado),
                )
            }

            vendas.forEachIndexed { indice, r ->
                if (indice > 0) Separador()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${r.venda.quantidade}x por ${formatarBrl(r.venda.precoUnitario)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${formatarData(r.venda.data)}  recebeu ${formatarBrl(r.recebido)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatarBrlAssinado(r.lucro),
                            style = EstiloNumeroMedio,
                            color = cores.para(r.lucro),
                        )
                        r.lucroPercent?.let {
                            Text(
                                text = formatarPercentAssinado(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = cores.para(it),
                            )
                        }
                    }
                    IconButton(onClick = { onDesfazer(r.venda.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Desfazer venda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
