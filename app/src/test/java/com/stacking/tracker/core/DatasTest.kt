package com.stacking.tracker.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class DatasTest {

    // Um fuso a oeste e um a leste de Greenwich: o bug so aparece fora de UTC,
    // e aparece com sinais opostos de cada lado.
    private val saoPaulo: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val toquio: ZoneId = ZoneId.of("Asia/Tokyo")

    private fun diaLocal(millis: Long, zona: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    /** O que o DatePicker do Material 3 devolve: meia-noite UTC do dia escolhido. */
    private fun doSeletor(dia: LocalDate): Long =
        dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `dia escolhido no seletor nao volta a oeste de Greenwich`() {
        val escolhido = LocalDate.of(2026, 8, 5)
        val guardado = dataDoSeletor(doSeletor(escolhido), saoPaulo)
        assertEquals(escolhido, diaLocal(guardado, saoPaulo))
    }

    @Test
    fun `dia escolhido no seletor nao avanca a leste de Greenwich`() {
        val escolhido = LocalDate.of(2026, 8, 5)
        val guardado = dataDoSeletor(doSeletor(escolhido), toquio)
        assertEquals(escolhido, diaLocal(guardado, toquio))
    }

    @Test
    fun `o que e guardado vira meia-noite local`() {
        val guardado = dataDoSeletor(doSeletor(LocalDate.of(2026, 8, 5)), saoPaulo)
        val hora = Instant.ofEpochMilli(guardado).atZone(saoPaulo)
        assertEquals(0, hora.hour)
        assertEquals(0, hora.minute)
    }

    @Test
    fun `ida e volta entre banco e seletor preserva o dia`() {
        listOf(saoPaulo, toquio).forEach { zona ->
            listOf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 12, 31),
            ).forEach { dia ->
                val guardado = dia.atStartOfDay(zona).toInstant().toEpochMilli()
                val voltou = dataDoSeletor(dataParaSeletor(guardado, zona), zona)
                assertEquals("zona=$zona dia=$dia", guardado, voltou)
            }
        }
    }

    @Test
    fun `fim do dia cobre o ultimo milissegundo local`() {
        val dia = LocalDate.of(2026, 8, 5)
        val meiaNoite = dia.atStartOfDay(saoPaulo).toInstant().toEpochMilli()
        val fim = fimDoDia(meiaNoite, saoPaulo)

        assertEquals(dia, diaLocal(fim, saoPaulo))
        assertEquals(dia.plusDays(1), diaLocal(fim + 1, saoPaulo))
        assertEquals(86_400_000L - 1, fim - meiaNoite)
    }

    @Test
    fun `hoje em millis cai a meia-noite da zona pedida`() {
        val hoje = hojeEmMillis(saoPaulo)
        val zoned = Instant.ofEpochMilli(hoje).atZone(saoPaulo)
        assertEquals(0, zoned.hour)
        assertEquals(LocalDate.now(saoPaulo), zoned.toLocalDate())
    }
}
