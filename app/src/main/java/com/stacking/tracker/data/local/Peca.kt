package com.stacking.tracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pecas",
    indices = [Index("dataCompra"), Index("tipo")],
)
data class Peca(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tipo: TipoPeca = TipoPeca.MOEDA,
    val nome: String = "",
    val marca: String = "",
    /**
     * Quantas pecas identicas esta linha representa.
     *
     * Peso e preco abaixo sao **por unidade**; os totais saem multiplicando por
     * aqui. Assim vinte moedas iguais viram uma linha, e migrar do schema antigo
     * (onde cada linha era uma peca) e so assumir quantidade 1.
     */
    val quantidade: Int = 1,
    /** Peso bruto de UMA peca, em gramas. */
    val pesoGramas: Double = 0.0,
    /** Teor de prata, de 0 a 1. Ex.: 0.999 */
    val pureza: Double = 0.999,
    /** Valor pago por UMA peca, em BRL. */
    val precoPago: Double = 0.0,
    /** Timestamp epoch em millis (UTC). */
    val dataCompra: Long = 0L,
    val vendedor: String? = null,
    val observacoes: String? = null,
    @ColumnInfo(name = "fotoPath")
    val fotoPath: String? = null,
)
