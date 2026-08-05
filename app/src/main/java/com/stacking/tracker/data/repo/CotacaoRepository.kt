package com.stacking.tracker.data.repo

import com.stacking.tracker.data.local.Cotacao
import com.stacking.tracker.data.local.CotacaoDao
import com.stacking.tracker.data.local.OrigemCotacao
import com.stacking.tracker.data.remote.AwesomeApi
import com.stacking.tracker.data.remote.CambioApi
import com.stacking.tracker.data.remote.OuroApi
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

sealed interface ResultadoCotacao {
    data class Sucesso(val cotacao: Cotacao) : ResultadoCotacao
    data class Falha(val mensagem: String) : ResultadoCotacao
}

/** Preco spot ja normalizado, venha de onde vier. */
private data class SpotBruto(
    val precoOzBrl: Double,
    val precoOzUsd: Double,
    val instante: Long,
)

class CotacaoRepository(
    private val dao: CotacaoDao,
    private val awesome: AwesomeApi,
    private val ouro: OuroApi,
    private val cambio: CambioApi,
) {

    fun observarUltima(): Flow<Cotacao?> = dao.observarUltima()

    fun observarHistorico(): Flow<List<Cotacao>> = dao.observarHistorico()

    suspend fun ultima(): Cotacao? = dao.ultima()

    suspend fun spotEm(data: Long): Cotacao? = dao.maisProximaAntesDe(data)

    /**
     * Tenta a AwesomeAPI e, se ela falhar, cai para gold-api + frankfurter.
     *
     * A reserva existe porque o limite anonimo da AwesomeAPI e por IP, e operadora
     * de celular compartilha IP entre milhares de aparelhos: da 429 sem o usuario
     * ter feito nada. Nenhuma das tres fontes exige chave.
     */
    suspend fun atualizar(): ResultadoCotacao {
        val falhas = mutableListOf<String>()

        buscar(falhas) { viaAwesome() }?.let { return gravar(it) }
        buscar(falhas) { viaReserva() }?.let { return gravar(it) }

        return ResultadoCotacao.Falha(
            falhas.firstOrNull() ?: "Nao foi possivel obter a cotacao.",
        )
    }

    private suspend fun buscar(
        falhas: MutableList<String>,
        bloco: suspend () -> SpotBruto?,
    ): SpotBruto? = try {
        val spot = bloco()
        if (spot == null) falhas += "A fonte nao retornou os precos."
        spot
    } catch (e: Exception) {
        falhas += descrever(e)
        null
    }

    private suspend fun viaAwesome(): SpotBruto? {
        val r = awesome.ultimaCotacao()
        val ozBrl = r.prata?.valor ?: return null
        val usdBrl = r.dolar?.valor ?: return null
        if (ozBrl <= 0.0 || usdBrl <= 0.0) return null
        return SpotBruto(
            precoOzBrl = ozBrl,
            precoOzUsd = ozBrl / usdBrl,
            // Horario do mercado, quando vem; o do aparelho e so reserva.
            instante = r.prata.instante ?: System.currentTimeMillis(),
        )
    }

    private suspend fun viaReserva(): SpotBruto? {
        val ozUsd = ouro.prata().price ?: return null
        val usdBrl = cambio.usdParaBrl().brl ?: return null
        if (ozUsd <= 0.0 || usdBrl <= 0.0) return null
        return SpotBruto(
            precoOzBrl = ozUsd * usdBrl,
            precoOzUsd = ozUsd,
            instante = System.currentTimeMillis(),
        )
    }

    private suspend fun gravar(spot: SpotBruto): ResultadoCotacao {
        val cotacao = Cotacao(
            data = spot.instante,
            precoOzUsd = spot.precoOzUsd,
            precoOzBrl = spot.precoOzBrl,
            origem = OrigemCotacao.API,
        )
        val id = dao.inserir(cotacao)
        return ResultadoCotacao.Sucesso(cotacao.copy(id = id))
    }

    private fun descrever(e: Throwable): String = when {
        e is HttpException && e.code() == 429 ->
            "Fonte de cotacao ocupada (limite de consultas)."
        e is HttpException -> "A fonte respondeu HTTP ${e.code()}."
        e is IOException -> "Sem conexao com a internet."
        else -> e.message ?: "Falha ao consultar a cotacao."
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
