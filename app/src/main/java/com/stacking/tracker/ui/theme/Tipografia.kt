package com.stacking.tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// "tnum" = algarismos de largura fixa. Mantem colunas de numeros alinhadas e
// evita o texto "pular" quando um valor muda.
private const val TABULAR = "tnum"

val Tipografia = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TABULAR,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.7.sp,
    ),
)

/** Numero principal de uma tela (valor de mercado, spot). */
val EstiloNumeroHero = TextStyle(
    fontSize = 40.sp,
    lineHeight = 46.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = (-0.8).sp,
    fontFeatureSettings = TABULAR,
)

/** Numero de destaque dentro de um cartao. */
val EstiloNumeroGrande = TextStyle(
    fontSize = 24.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = (-0.3).sp,
    fontFeatureSettings = TABULAR,
)

/** Numero em linha de lista ou coluna secundaria. */
val EstiloNumeroMedio = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.sp,
    fontFeatureSettings = TABULAR,
)

val EstiloNumeroPequeno = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
    fontFeatureSettings = TABULAR,
)

/** Rotulo de campo: caixa alta curta, sempre acima ou ao lado do numero. */
val EstiloRotulo = TextStyle(
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.8.sp,
)
