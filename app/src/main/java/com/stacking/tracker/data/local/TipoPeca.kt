package com.stacking.tracker.data.local

enum class TipoPeca(val rotulo: String) {
    MOEDA("Moeda"),
    BARRA("Barra"),
    REDONDA("Redonda"),
    ;

    companion object {
        fun de(valor: String?): TipoPeca =
            entries.firstOrNull { it.name == valor } ?: MOEDA
    }
}
