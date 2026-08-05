package com.stacking.tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import com.stacking.tracker.core.UnidadePeso
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ModoTema(val rotulo: String) {
    ESCURO("Escuro"),
    CLARO("Claro"),
    SISTEMA("Sistema"),
}

// Cantos discretos: nada de pilulas nem cartoes muito arredondados.
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

/**
 * Unidade de exibicao de peso e preco unitario. Fica num CompositionLocal porque
 * praticamente toda tela mostra peso, e passar isso por parametro em cascata
 * poluiria cada assinatura no caminho.
 */
val LocalUnidadePeso = staticCompositionLocalOf { UnidadePeso.GRAMAS }

@Composable
fun StackingTheme(
    modo: ModoTema = ModoTema.ESCURO,
    unidade: UnidadePeso = UnidadePeso.GRAMAS,
    conteudo: @Composable () -> Unit,
) {
    val escuro = when (modo) {
        ModoTema.ESCURO -> true
        ModoTema.CLARO -> false
        ModoTema.SISTEMA -> isSystemInDarkTheme()
    }

    val esquema = if (escuro) EsquemaEscuro else EsquemaClaro
    val coresValor = if (escuro) CoresValorEscuro else CoresValorClaro

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val janela = (view.context as Activity).window
            WindowCompat.getInsetsController(janela, view).apply {
                isAppearanceLightStatusBars = !escuro
                isAppearanceLightNavigationBars = !escuro
            }
        }
    }

    CompositionLocalProvider(
        LocalCoresValor provides coresValor,
        LocalUnidadePeso provides unidade,
    ) {
        MaterialTheme(
            colorScheme = esquema,
            typography = Tipografia,
            shapes = Formas,
            content = conteudo,
        )
    }
}
