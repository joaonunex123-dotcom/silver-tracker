package com.stacking.tracker.data.repo

import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.data.local.CotacaoDao
import com.stacking.tracker.data.local.OrigemCotacao
import com.stacking.tracker.data.remote.MetaisApi
import kotlinx.coroutines.flow.Flow
import java.io.IOException

sealed interface ResultadoCotacao {
    data class Sucesso(val cotacao: Cotacao) : ResultadoCotacao
    data class Falha(val mensagem: String) : ResultadoCotacao
}

class CotacaoRepository(
    private val dao: CotacaoDao,
    private val api: MetaisApi,
) {

    fun observarUltima(): Flow<Cotacao?> = dao.observarUltima()

    fun observarHistorico(): Flow<List<Cotacao>> = dao.observarHistorico()

    suspend fun ultima(): Cotacao? = dao.ultima()

    suspend fun spotEm(data: Long): Cotacao? = dao.maisProximaAntesDe(data)

    /**
     * Puxa a cotacao da API e grava no historico. O app segue funcionando sem isso:
     * o valor de mercado usa a ultima cotacao salva no banco.
     */
    suspend fun atualizar(): ResultadoCotacao {
        return try {
            val resposta = api.ultimaCotacao()
            val ozBrl = resposta.prata?.valor
            val usdBrl = resposta.dolar?.valor

            when {
                ozBrl == null || ozBrl <= 0.0 ->
                    ResultadoCotacao.Falha("A API nao retornou o preco da prata.")

                usdBrl == null || usdBrl <= 0.0 ->
                    ResultadoCotacao.Falha("A API nao retornou a cotacao do dolar.")

                else -> {
                    val cotacao = Cotacao(
                        // Horario do mercado, quando vem; o do aparelho e so reserva.
                        data = resposta.prata.instante ?: System.currentTimeMillis(),
                        precoOzUsd = ozBrl / usdBrl,
                        precoOzBrl = ozBrl,
                        origem = OrigemCotacao.API,
                    )
                    val id = dao.inserir(cotacao)
                    ResultadoCotacao.Sucesso(cotacao.copy(id = id))
                }
            }
        } catch (e: IOException) {
            ResultadoCotacao.Falha("Sem conexao com a API de cotacao.")
        } catch (e: Exception) {
            ResultadoCotacao.Falha(e.message ?: "Falha ao consultar a cotacao.")
        }
    }

    /** Entrada manual, para corrigir o spot ou lancar a cotacao de uma compra antiga. */
    suspend fun registrarManual(precoOzUsd: Double, usdBrl: Double): Cotacao {
        val cotacao = Cotacao(
            data = System.currentTimeMillis(),
            precoOzUsd = precoOzUsd,
            precoOzBrl = precoOzUsd * usdBrl,
            origem = OrigemCotacao.MANUAL,
        )
        val id = dao.inserir(cotacao)
        return cotacao.copy(id = id)
    }

    suspend fun excluir(id: Long) = dao.excluirPorId(id)
}
