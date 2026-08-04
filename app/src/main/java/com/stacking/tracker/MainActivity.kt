package com.stacking.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacking.tracker.ui.navegacao.AppNavegacao
import com.stacking.tracker.ui.theme.ModoTema
import com.stacking.tracker.ui.theme.StackingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val preferencias = (application as StackingApp).container.preferencias

        setContent {
            val modo by preferencias.modoTema.collectAsStateWithLifecycle()

            StackingTheme(modo = modo) {
                AppNavegacao(
                    modoTema = modo,
                    onAlternarTema = { preferencias.definirModoTema(proximoModo(modo)) },
                )
            }
        }
    }
}

/** Escuro -> Claro -> Sistema -> Escuro. */
private fun proximoModo(atual: ModoTema): ModoTema = when (atual) {
    ModoTema.ESCURO -> ModoTema.CLARO
    ModoTema.CLARO -> ModoTema.SISTEMA
    ModoTema.SISTEMA -> ModoTema.ESCURO
}
