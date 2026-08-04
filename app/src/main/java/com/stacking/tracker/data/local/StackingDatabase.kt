package com.stacking.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Peca::class, Cotacao::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class StackingDatabase : RoomDatabase() {

    abstract fun pecaDao(): PecaDao

    abstract fun cotacaoDao(): CotacaoDao

    companion object {
        private const val NOME = "stacking.db"

        @Volatile
        private var instancia: StackingDatabase? = null

        fun obter(context: Context): StackingDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room
                    .databaseBuilder(context.applicationContext, StackingDatabase::class.java, NOME)
                    .build()
                    .also { instancia = it }
            }
    }
}
