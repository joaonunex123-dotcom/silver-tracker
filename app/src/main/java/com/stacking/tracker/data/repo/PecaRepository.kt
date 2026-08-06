package com.stacking.tracker.data.repo

import com.stacking.tracker.data.local.Peca
import com.stacking.tracker.data.local.PecaDao
import com.stacking.tracker.data.local.VendaDao
import com.stacking.tracker.util.ArmazenamentoFotos
import kotlinx.coroutines.flow.Flow

class PecaRepository(
    private val dao: PecaDao,
    private val vendaDao: VendaDao,
    private val fotos: ArmazenamentoFotos,
) {

    fun observarTodas(): Flow<List<Peca>> = dao.observarTodas()

    fun observarPorId(id: Long): Flow<Peca?> = dao.observarPorId(id)

    suspend fun porId(id: Long): Peca? = dao.porId(id)

    /** Leitura pontual para o widget, que nao tem ciclo de vida para observar Flow. */
    suspend fun todas(): List<Peca> = dao.todas()

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
        // A tabela de vendas nao tem chave estrangeira (para simplificar a
        // migracao), entao a limpeza em cascata e feita aqui.
        vendaDao.excluirDaPeca(peca.id)
        dao.excluir(peca)
    }
}
