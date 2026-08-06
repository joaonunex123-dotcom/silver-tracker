package com.stacking.tracker.data.repo

import com.stacking.tracker.data.local.Venda
import com.stacking.tracker.data.local.VendaDao
import kotlinx.coroutines.flow.Flow

class VendaRepository(private val dao: VendaDao) {

    fun observarTodas(): Flow<List<Venda>> = dao.observarTodas()

    fun observarDaPeca(pecaId: Long): Flow<List<Venda>> = dao.observarDaPeca(pecaId)

    /** Leitura pontual para o widget, que nao observa Flow. */
    suspend fun todas(): List<Venda> = dao.todas()

    suspend fun quantidadeVendida(pecaId: Long): Int = dao.quantidadeVendida(pecaId)

    suspend fun registrar(venda: Venda): Venda {
        val id = dao.inserir(venda)
        return venda.copy(id = id)
    }

    suspend fun excluir(id: Long) = dao.excluirPorId(id)
}
