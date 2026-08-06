package com.stacking.tracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VendaDao {

    @Query("SELECT * FROM vendas ORDER BY data DESC, id DESC")
    fun observarTodas(): Flow<List<Venda>>

    @Query("SELECT * FROM vendas WHERE pecaId = :pecaId ORDER BY data DESC, id DESC")
    fun observarDaPeca(pecaId: Long): Flow<List<Venda>>

    /** Leitura pontual, usada pelo widget. */
    @Query("SELECT * FROM vendas")
    suspend fun todas(): List<Venda>

    @Query("SELECT COALESCE(SUM(quantidade), 0) FROM vendas WHERE pecaId = :pecaId")
    suspend fun quantidadeVendida(pecaId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(venda: Venda): Long

    @Delete
    suspend fun excluir(venda: Venda)

    @Query("DELETE FROM vendas WHERE id = :id")
    suspend fun excluirPorId(id: Long)

    @Query("DELETE FROM vendas WHERE pecaId = :pecaId")
    suspend fun excluirDaPeca(pecaId: Long)
}
