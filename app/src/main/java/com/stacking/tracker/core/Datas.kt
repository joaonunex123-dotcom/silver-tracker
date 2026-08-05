package com.stacking.tracker.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Datas de compra sao guardadas como **meia-noite no fuso local**, que e como
 * [formatarData] as le de volta.
 *
 * O DatePicker do Material 3, porem, trabalha em UTC: devolve a meia-noite UTC do
 * dia escolhido. Passar esse valor direto para o banco faz a data voltar um dia
 * para quem esta a oeste de Greenwich — escolher 05/08 e ver 04/08 na tela.
 * Por isso as duas conversoes abaixo, uma em cada sentido.
 */
fun dataDoSeletor(utcMillis: Long, zona: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atStartOfDay(zona)
        .toInstant()
        .toEpochMilli()

fun dataParaSeletor(millisLocal: Long, zona: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(millisLocal)
        .atZone(zona)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

/** Ultimo milissegundo do dia local ao qual [millis] pertence. */
fun fimDoDia(millis: Long, zona: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(millis)
        .atZone(zona)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zona)
        .toInstant()
        .toEpochMilli() - 1

fun hojeEmMillis(zona: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zona).atStartOfDay(zona).toInstant().toEpochMilli()
