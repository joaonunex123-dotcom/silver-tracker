package com.stacking.tracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta neutra: cinza-metal com um unico acento claro. Nada de cor decorativa.
private val Grafite0 = Color(0xFF0D0D0F)
private val Grafite1 = Color(0xFF141417)
private val Grafite2 = Color(0xFF1C1C20)
private val Grafite3 = Color(0xFF26262B)
private val LinhaEscura = Color(0xFF2E2E35)
private val TextoEscuro = Color(0xFFEDEDF0)
private val TextoEscuroFraco = Color(0xFF95959F)
private val PrataClara = Color(0xFFC9CED6)

private val Papel0 = Color(0xFFFAFAFB)
private val Papel1 = Color(0xFFFFFFFF)
private val Papel2 = Color(0xFFF2F2F4)
private val Papel3 = Color(0xFFE8E8EC)
private val LinhaClara = Color(0xFFDCDCE2)
private val TextoClaro = Color(0xFF15151A)
private val TextoClaroFraco = Color(0xFF61616B)
private val GrafiteAcento = Color(0xFF33363C)

val EsquemaEscuro = darkColorScheme(
    primary = PrataClara,
    onPrimary = Grafite0,
    primaryContainer = Grafite3,
    onPrimaryContainer = TextoEscuro,
    secondary = TextoEscuroFraco,
    onSecondary = Grafite0,
    background = Grafite0,
    onBackground = TextoEscuro,
    surface = Grafite1,
    onSurface = TextoEscuro,
    surfaceVariant = Grafite2,
    onSurfaceVariant = TextoEscuroFraco,
    surfaceContainer = Grafite1,
    surfaceContainerHigh = Grafite2,
    surfaceContainerHighest = Grafite3,
    surfaceContainerLow = Grafite1,
    surfaceContainerLowest = Grafite0,
    outline = LinhaEscura,
    outlineVariant = Grafite3,
    error = Color(0xFFF0736B),
    onError = Grafite0,
)

val EsquemaClaro = lightColorScheme(
    primary = GrafiteAcento,
    onPrimary = Papel1,
    primaryContainer = Papel3,
    onPrimaryContainer = TextoClaro,
    secondary = TextoClaroFraco,
    onSecondary = Papel1,
    background = Papel0,
    onBackground = TextoClaro,
    surface = Papel1,
    onSurface = TextoClaro,
    surfaceVariant = Papel2,
    onSurfaceVariant = TextoClaroFraco,
    surfaceContainer = Papel1,
    surfaceContainerHigh = Papel2,
    surfaceContainerHighest = Papel3,
    surfaceContainerLow = Papel1,
    surfaceContainerLowest = Papel0,
    outline = LinhaClara,
    outlineVariant = Papel3,
    error = Color(0xFFC0342B),
    onError = Papel1,
)

/** Cores de significado (lucro/prejuizo) que o ColorScheme do M3 nao cobre. */
@Immutable
data class CoresValor(
    val positivo: Color,
    val negativo: Color,
    val neutro: Color,
)

val CoresValorEscuro = CoresValor(
    positivo = Color(0xFF56D68A),
    negativo = Color(0xFFF0736B),
    neutro = TextoEscuroFraco,
)

val CoresValorClaro = CoresValor(
    positivo = Color(0xFF14804A),
    negativo = Color(0xFFB3261E),
    neutro = TextoClaroFraco,
)

val LocalCoresValor = staticCompositionLocalOf { CoresValorEscuro }

/** Verde para ganho, vermelho para perda, cinza para zero/indefinido. */
fun CoresValor.para(valor: Double?): Color = when {
    valor == null || valor == 0.0 -> neutro
    valor > 0.0 -> positivo
    else -> negativo
}
