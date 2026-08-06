package com.stacking.tracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

private const val ADICIONA_QUANTIDADE =
    "ALTER TABLE pecas ADD COLUMN quantidade INTEGER NOT NULL DEFAULT 1"

/**
 * v1 -> v2: cada linha passa a poder representar N pecas identicas.
 *
 * `DEFAULT 1` faz as linhas existentes continuarem valendo exatamente o que
 * valiam, entao ninguem perde nada nem ve numero mudar.
 *
 * As duas sobrecargas de `migrate` sao sobrescritas de proposito: o Room 2.8 tem
 * a API antiga (SupportSQLiteDatabase) e a nova (SQLiteConnection), e qual delas
 * e chamada depende do caminho interno escolhido. Implementar so uma arriscaria a
 * migracao nao rodar — e migracao que nao roda derruba o app do usuario.
 */
val MIGRACAO_1_2 = object : Migration(1, 2) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(ADICIONA_QUANTIDADE)
    }

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADICIONA_QUANTIDADE)
    }
}

/**
 * v2 -> v3: tabela de vendas.
 *
 * O SQL abaixo precisa bater **caractere a caractere** com o que o Room gera para
 * a entity [Venda]; ele compara os dois ao abrir o banco e derruba o app se
 * divergirem. O CI publica o `createSql` esperado como anotacao para conferencia.
 */
private val CRIA_VENDAS = listOf(
    "CREATE TABLE IF NOT EXISTS `vendas` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`pecaId` INTEGER NOT NULL, " +
        "`quantidade` INTEGER NOT NULL, " +
        "`precoUnitario` REAL NOT NULL, " +
        "`data` INTEGER NOT NULL, " +
        "`comprador` TEXT, " +
        "`observacoes` TEXT)",
    "CREATE INDEX IF NOT EXISTS `index_vendas_pecaId` ON `vendas` (`pecaId`)",
    "CREATE INDEX IF NOT EXISTS `index_vendas_data` ON `vendas` (`data`)",
)

val MIGRACAO_2_3 = object : Migration(2, 3) {

    override fun migrate(connection: SQLiteConnection) {
        CRIA_VENDAS.forEach { connection.execSQL(it) }
    }

    override fun migrate(db: SupportSQLiteDatabase) {
        CRIA_VENDAS.forEach { db.execSQL(it) }
    }
}
