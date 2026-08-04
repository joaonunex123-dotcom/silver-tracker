package com.stacking.tracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Origem da cotacao: puxada da API ou digitada pelo usuario. */
enum class OrigemCotacao(val rotulo: String) {
    API("API"),
    MANUAL("Manual"),
}

@Entity(
    tableName = "cotacoes",
    indices = [Index("data")],
)
data class Cotacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** Timestamp epoch em millis (UTC) do momento em que a cotacao vale. */
    val data: Long = 0L,
    /** Preco spot da prata em USD por onca troy. */
    val precoOzUsd: Double = 0.0,
    /** Preco spot da prata em BRL por onca troy. */
    val precoOzBrl: Double = 0.0,
    val origem: OrigemCotacao = OrigemCotacao.API,
) {
    val usdBrl: Double
        get() = if (precoOzUsd > 0.0) precoOzBrl / precoOzUsd else 0.0
}
