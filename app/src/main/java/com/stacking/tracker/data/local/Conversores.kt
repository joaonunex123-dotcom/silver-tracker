package com.stacking.tracker.data.local

import androidx.room.TypeConverter

class Conversores {

    @TypeConverter
    fun tipoParaTexto(tipo: TipoPeca): String = tipo.name

    @TypeConverter
    fun textoParaTipo(valor: String?): TipoPeca = TipoPeca.de(valor)

    @TypeConverter
    fun origemParaTexto(origem: OrigemCotacao): String = origem.name

    @TypeConverter
    fun textoParaOrigem(valor: String?): OrigemCotacao =
        OrigemCotacao.entries.firstOrNull { it.name == valor } ?: OrigemCotacao.API
}
