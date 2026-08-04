package com.stacking.tracker.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Copia a foto escolhida para o diretorio interno do app. Guardar so a Uri do
 * seletor nao serve: a permissao de leitura morre quando o processo reinicia.
 */
class ArmazenamentoFotos(private val context: Context) {

    private val diretorio: File
        get() = File(context.filesDir, "fotos").apply { if (!exists()) mkdirs() }

    suspend fun copiarParaInterno(origem: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val destino = File(diretorio, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(origem)?.use { entrada ->
                destino.outputStream().use { saida -> entrada.copyTo(saida) }
            } ?: return@runCatching null
            destino.absolutePath
        }.getOrNull()
    }

    suspend fun excluir(caminho: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                val arquivo = File(caminho)
                if (arquivo.exists() && arquivo.parentFile == diretorio) arquivo.delete()
            }
        }
    }
}
