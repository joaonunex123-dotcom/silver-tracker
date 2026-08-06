package com.stacking.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Peca::class, Cotacao::class, Venda::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class StackingDatabase : RoomDatabase() {

    abstract fun pecaDao(): PecaDao

    abstract fun cotacaoDao(): CotacaoDao

    abstract fun vendaDao(): VendaDao

    companion object {
        private const val NOME = "stacking.db"

        @Volatile
        private var instancia: StackingDatabase? = null

        fun obter(context: Context): StackingDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room
                    .databaseBuilder(context.applicationContext, StackingDatabase::class.java, NOME)
                    .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3)
                    // Sem fallbackToDestructiveMigration de proposito: preferimos
                    // falhar alto a apagar a colecao de alguem em silencio.
                    .build()
                    .also { instancia = it }
            }
    }
}
