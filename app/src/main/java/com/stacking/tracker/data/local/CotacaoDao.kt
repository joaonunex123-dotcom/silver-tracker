package com.stacking.tracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CotacaoDao {

    @Query("SELECT * FROM cotacoes ORDER BY data DESC LIMIT 1")
    fun observarUltima(): Flow<Cotacao?>

    @Query("SELECT * FROM cotacoes ORDER BY data DESC")
    fun observarHistorico(): Flow<List<Cotacao>>

    @Query("SELECT * FROM cotacoes ORDER BY data DESC LIMIT :limite")
    fun observarHistorico(limite: Int): Flow<List<Cotacao>>

    @Query("SELECT * FROM cotacoes ORDER BY data DESC LIMIT 1")
    suspend fun ultima(): Cotacao?

    /** Cotacao valida na data informada: a mais recente que nao seja posterior a ela. */
    @Query("SELECT * FROM cotacoes WHERE data <= :data ORDER BY data DESC LIMIT 1")
    suspend fun maisProximaAntesDe(data: Long): Cotacao?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(cotacao: Cotacao): Long

    @Query("DELETE FROM cotacoes WHERE id = :id")
    suspend fun excluirPorId(id: Long)
}
