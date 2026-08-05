package com.stacking.tracker.core

/**
 * Unidade em que o app mostra peso e preco unitario.
 *
 * O banco continua guardando gramas na peca e preco por **onca troy** na cotacao,
 * que e como o mercado cota. Isto aqui e so apresentacao: trocar a unidade nao
 * altera nenhum dado nem nenhum calculo.
 */
enum class UnidadePeso(val rotulo: String, val sufixo: String) {
    GRAMAS("Gramas", "g"),
    ONCAS("Oncas troy", "oz"),
    ;

    companion object {
        fun de(valor: String?): UnidadePeso =
            entries.firstOrNull { it.name == valor } ?: GRAMAS
    }
}

/** Quantidade de metal na unidade escolhida. A entrada e sempre em oncas troy. */
fun formatarQuantidade(ozTroy: Double, unidade: UnidadePeso): String = when (unidade) {
    UnidadePeso.GRAMAS -> formatarGramas(ozTroyParaGramas(ozTroy))
    UnidadePeso.ONCAS -> formatarOz(ozTroy)
}

/** Preco de uma unidade de peso, derivado do spot por onca troy. */
fun precoPorUnidade(precoOzBrl: Double, unidade: UnidadePeso): Double = when (unidade) {
    UnidadePeso.GRAMAS -> precoPorGrama(precoOzBrl)
    UnidadePeso.ONCAS -> precoOzBrl
}

fun formatarPrecoUnitario(precoOzBrl: Double, unidade: UnidadePeso): String =
    formatarBrl(precoPorUnidade(precoOzBrl, unidade))

/** Rotulo curto do tipo "BRL / g" ou "BRL / oz". */
fun rotuloPreco(moeda: String, unidade: UnidadePeso): String = "$moeda / ${unidade.sufixo}"
