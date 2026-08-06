package com.stacking.tracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Uma saida de peças de uma linha do inventario.
 *
 * A [Peca] nao e alterada nem apagada ao vender: o que ela guarda e **quanto foi
 * comprado**, e isso nunca muda. O estoque atual sai da subtracao. Assim da para
 * vender parte de um lote, manter o custo original para calcular lucro, e separar
 * o que ja foi realizado do que ainda esta em carteira.
 *
 * Sem chave estrangeira de proposito: manter o CREATE TABLE simples reduz a
 * chance de divergir do que o Room espera na migracao. A limpeza das vendas
 * quando uma peca e excluida fica no PecaRepository.
 */
@Entity(
    tableName = "vendas",
    indices = [Index("pecaId"), Index("data")],
)
data class Venda(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pecaId: Long = 0L,
    /** Quantas unidades sairam nesta venda. */
    val quantidade: Int = 1,
    /** Valor recebido por UMA peca, em BRL. */
    val precoUnitario: Double = 0.0,
    /** Timestamp epoch em millis (UTC), meia-noite local do dia da venda. */
    val data: Long = 0L,
    val comprador: String? = null,
    val observacoes: String? = null,
) {
    val valorRecebido: Double get() = precoUnitario * quantidade
}
