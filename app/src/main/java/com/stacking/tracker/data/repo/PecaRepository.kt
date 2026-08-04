package com.stacking.tracker.data.repo

import com.stacking.tracker.data.local.Peca
import com.stacking.tracker.data.local.PecaDao
import com.stacking.tracker.util.ArmazenamentoFotos
import kotlinx.coroutines.flow.Flow

class PecaRepository(
    private val dao: PecaDao,
    private val fotos: ArmazenamentoFotos,
) {

    fun observarTodas(): Flow<List<Peca>> = dao.observarTodas()

    fun observarPorId(id: Long): Flow<Peca?> = dao.observarPorId(id)

    suspend fun porId(id: Long): Peca? = dao.porId(id)

    suspend fun salvar(peca: Peca): Long =
        if (peca.id == 0L) {
            dao.inserir(peca)
        } else {
            // Se a foto trocou, a antiga vira lixo no diretorio interno.
            val anterior = dao.porId(peca.id)
            if (anterior?.fotoPath != null && anterior.fotoPath != peca.fotoPath) {
                fotos.excluir(anterior.fotoPath)
            }
            dao.atualizar(peca)
            peca.id
        }

    suspend fun excluir(peca: Peca) {
        peca.fotoPath?.let { fotos.excluir(it) }
        dao.excluir(peca)
    }
}
