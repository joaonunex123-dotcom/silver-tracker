package com.stacking.tracker.util

import android.content.Context
import com.stacking.tracker.core.UnidadePeso
import com.stacking.tracker.ui.theme.ModoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferencias locais do app. Poucas chaves, sincronas: SharedPreferences basta. */
class PreferenciasApp(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("stacking_prefs", Context.MODE_PRIVATE)

    private val _modoTema = MutableStateFlow(lerModoTema())
    val modoTema: StateFlow<ModoTema> = _modoTema.asStateFlow()

    private val _unidade = MutableStateFlow(lerUnidade())
    val unidade: StateFlow<UnidadePeso> = _unidade.asStateFlow()

    private fun lerModoTema(): ModoTema {
        val salvo = prefs.getString(CHAVE_TEMA, null)
        return ModoTema.entries.firstOrNull { it.name == salvo } ?: ModoTema.ESCURO
    }

    private fun lerUnidade(): UnidadePeso = UnidadePeso.de(prefs.getString(CHAVE_UNIDADE, null))

    fun definirModoTema(modo: ModoTema) {
        prefs.edit().putString(CHAVE_TEMA, modo.name).apply()
        _modoTema.value = modo
    }

    fun definirUnidade(unidade: UnidadePeso) {
        prefs.edit().putString(CHAVE_UNIDADE, unidade.name).apply()
        _unidade.value = unidade
    }

    private companion object {
        const val CHAVE_TEMA = "modo_tema"
        const val CHAVE_UNIDADE = "unidade_peso"
    }
}
