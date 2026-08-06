package com.stacking.tracker.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.stacking.tracker.MainActivity
import com.stacking.tracker.R
import com.stacking.tracker.StackingApp
import com.stacking.tracker.core.formatarBrl
import com.stacking.tracker.core.formatarBrlAssinado
import com.stacking.tracker.core.formatarPercentAssinado
import com.stacking.tracker.core.hojeEmMillis
import com.stacking.tracker.core.ozFinasUnidade
import com.stacking.tracker.core.precoPorGrama
import com.stacking.tracker.core.variacaoDoEstoque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Widget de tela inicial: preco do grama, valor do estoque e variacao do dia.
 *
 * Feito com RemoteViews, nao com Glance: sao tres numeros, e o Glance traria mais
 * uma dependencia para alinhar com uma matriz de versoes que ja e apertada.
 *
 * A variacao "de hoje" compara o estoque ao spot atual contra o mesmo estoque ao
 * spot da ultima cotacao **anterior a hoje**. Mede movimento de preco, nao
 * compras: cadastrar uma moeda hoje nao aparece como valorizacao.
 */
class WidgetStack : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendente = goAsync()
        escopo.launch {
            try {
                desenhar(context, appWidgetManager, appWidgetIds)
            } finally {
                pendente.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACAO_ATUALIZAR) return

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, WidgetStack::class.java))
        if (ids.isEmpty()) return

        val pendente = goAsync()
        escopo.launch {
            try {
                // goAsync da uma janela curta (~10s). Se a rede demorar, o catch do
                // repositorio devolve Falha e ainda assim redesenhamos com o que
                // estiver no banco.
                runCatching {
                    (context.applicationContext as StackingApp)
                        .container.cotacaoRepository.atualizar()
                }
                desenhar(context, manager, ids)
            } finally {
                pendente.finish()
            }
        }
    }

    private suspend fun desenhar(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        val container = (context.applicationContext as StackingApp).container
        val pecas = runCatching { container.pecaRepository.todas() }.getOrDefault(emptyList())
        val vendas = runCatching { container.vendaRepository.todas() }.getOrDefault(emptyList())
        val atual = runCatching { container.cotacaoRepository.ultima() }.getOrNull()
        // Ultima cotacao ate a meia-noite de hoje: a referencia do dia.
        val referencia = runCatching {
            container.cotacaoRepository.spotEm(hojeEmMillis())
        }.getOrNull()

        // So o que ainda esta em maos: prata vendida nao pode contar no widget.
        val vendidoPorPeca = vendas.groupBy { it.pecaId }
            .mapValues { (_, lista) -> lista.sumOf { it.quantidade } }
        val ozFinasTotal = pecas.sumOf { peca ->
            val emEstoque = (peca.quantidade - (vendidoPorPeca[peca.id] ?: 0)).coerceAtLeast(0)
            peca.ozFinasUnidade * emEstoque
        }
        val precoOz = atual?.precoOzBrl ?: 0.0

        val textoGrama = if (precoOz > 0.0) formatarBrl(precoPorGrama(precoOz)) else VAZIO
        val textoStack = if (precoOz > 0.0) formatarBrl(ozFinasTotal * precoOz) else VAZIO

        val variacao = variacaoDoEstoque(
            ozFinas = ozFinasTotal,
            precoOzBrlAgora = precoOz,
            precoOzBrlAntes = referencia?.precoOzBrl ?: 0.0,
        )
        val percentual = variacao?.percentual
        val textoHoje = when {
            variacao == null -> VAZIO
            percentual == null -> formatarBrlAssinado(variacao.diferenca)
            else -> formatarBrlAssinado(variacao.diferenca) + "   " + formatarPercentAssinado(percentual)
        }
        val corHoje = ContextCompat.getColor(
            context,
            when {
                variacao == null || variacao.diferenca == 0.0 -> R.color.widget_rotulo
                variacao.diferenca > 0.0 -> R.color.widget_positivo
                else -> R.color.widget_negativo
            },
        )

        val views = RemoteViews(context.packageName, R.layout.widget_stack).apply {
            setTextViewText(R.id.widget_grama, textoGrama)
            setTextViewText(R.id.widget_stack, textoStack)
            setTextViewText(R.id.widget_hoje, textoHoje)
            setTextColor(R.id.widget_hoje, corHoje)
            setOnClickPendingIntent(R.id.widget_raiz, abrirApp(context))
            setOnClickPendingIntent(R.id.widget_atualizar, pedirAtualizacao(context))
        }

        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun abrirApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pedirAtualizacao(context: Context): PendingIntent {
        val intent = Intent(context, WidgetStack::class.java).setAction(ACAO_ATUALIZAR)
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val ACAO_ATUALIZAR = "com.stacking.tracker.ATUALIZAR_WIDGET"
        private const val VAZIO = "--"

        // O provider e recriado a cada broadcast, entao o escopo vive no companion.
        private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Redesenha o widget depois que o app mexeu em pecas ou cotacao. */
        fun redesenhar(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, WidgetStack::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(app, WidgetStack::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            app.sendBroadcast(intent)
        }
    }
}
