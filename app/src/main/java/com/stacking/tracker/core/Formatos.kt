package com.stacking.tracker.core

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")

private val MOEDA_BRL: NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_BR)
private val MOEDA_USD: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

private fun decimal(minimo: Int, maximo: Int): NumberFormat =
    NumberFormat.getNumberInstance(LOCALE_BR).apply {
        minimumFractionDigits = minimo
        maximumFractionDigits = maximo
    }

private val DATA_CURTA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_BR)
private val DATA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE_BR)

fun formatarBrl(valor: Double): String = MOEDA_BRL.format(valor)

fun formatarUsd(valor: Double): String = MOEDA_USD.format(valor)

fun formatarNumero(valor: Double, casas: Int = 2): String = decimal(casas, casas).format(valor)

fun formatarGramas(valor: Double): String = "${decimal(0, 1).format(valor)} g"

fun formatarOz(valor: Double, casas: Int = 3): String = "${decimal(casas, casas).format(valor)} oz"

fun formatarPureza(valor: Double): String = decimal(3, 4).format(valor)

/** Percentual com sinal explicito: usado em lucro e premio. */
fun formatarPercentAssinado(valor: Double): String {
    val sinal = if (valor > 0.0) "+" else ""
    return "$sinal${decimal(2, 2).format(valor)}%"
}

fun formatarPercent(valor: Double): String = "${decimal(2, 2).format(valor)}%"

/** Valor em BRL com sinal explicito. */
fun formatarBrlAssinado(valor: Double): String {
    val sinal = if (valor > 0.0) "+" else ""
    return "$sinal${MOEDA_BRL.format(valor)}"
}

fun formatarData(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(DATA_CURTA)

fun formatarDataHora(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DATA_HORA)

/**
 * Le numero digitado pelo usuario aceitando virgula ou ponto como separador decimal.
 * "1.234,56" -> 1234.56 | "0.999" -> 0.999 | "12" -> 12.0
 */
fun paraDoubleOuNulo(texto: String): Double? {
    val limpo = texto.trim().replace(" ", "").replace("R$", "")
    if (limpo.isEmpty()) return null
    val normalizado =
        if (limpo.contains(',')) limpo.replace(".", "").replace(',', '.') else limpo
    return normalizado.toDoubleOrNull()
}

fun paraDouble(texto: String, padrao: Double = 0.0): Double = paraDoubleOuNulo(texto) ?: padrao

/** Converte Double para texto editavel no formulario (sem separador de milhar). */
fun paraCampo(valor: Double, casas: Int = 2): String =
    decimal(0, casas).format(valor).replace(".", "")
