package com.stacking.tracker

import android.content.Context
import com.stacking.tracker.data.local.StackingDatabase
import com.stacking.tracker.data.remote.Rede
import com.stacking.tracker.data.repo.CotacaoRepository
import com.stacking.tracker.data.repo.PecaRepository
import com.stacking.tracker.util.ArmazenamentoFotos
import com.stacking.tracker.util.PreferenciasApp

/**
 * Service locator simples. O app tem poucas dependencias e nenhum grafo de
 * escopo; Hilt aqui seria mais cerimonia do que ajuda.
 */
class ContainerApp(context: Context) {

    private val appContext = context.applicationContext

    private val banco by lazy { StackingDatabase.obter(appContext) }

    val armazenamentoFotos by lazy { ArmazenamentoFotos(appContext) }

    val preferencias by lazy { PreferenciasApp(appContext) }

    val pecaRepository by lazy { PecaRepository(banco.pecaDao(), armazenamentoFotos) }

    val cotacaoRepository by lazy {
        CotacaoRepository(
            dao = banco.cotacaoDao(),
            api = Rede.criarApi(),
        )
    }
}
