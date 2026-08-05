package com.stacking.tracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PecaDao {

    /**
     * A colecao de um stacker cabe folgada em memoria, e a ordenacao por valor
     * depende da cotacao (que nao esta no banco da peca). Por isso o DAO entrega
     * tudo e o filtro/ordenacao acontece no ViewModel.
     */
    @Query("SELECT * FROM pecas ORDER BY dataCompra DESC, id DESC")
    fun observarTodas(): Flow<List<Peca>>

    @Query("SELECT * FROM pecas WHERE id = :id")
    fun observarPorId(id: Long): Flow<Peca?>

    @Query("SELECT * FROM pecas WHERE id = :id")
    suspend fun porId(id: Long): Peca?

    /** Leitura pontual, usada pelo widget, que roda fora de qualquer tela. */
    @Query("SELECT * FROM pecas")
    suspend fun todas(): List<Peca>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(peca: Peca): Long

    @Update
    suspend fun atualizar(peca: Peca)

    @Delete
    suspend fun excluir(peca: Peca)

    @Query("DELETE FROM pecas WHERE id = :id")
    suspend fun excluirPorId(id: Long)
}
