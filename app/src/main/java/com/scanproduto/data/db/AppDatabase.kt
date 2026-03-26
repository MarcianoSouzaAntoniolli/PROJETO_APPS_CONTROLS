package com.scanproduto.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.scanproduto.model.Produto

/**
 * Classe principal do banco de dados Room.
 * Singleton para garantir uma única instância no app.
 */
@Database(
    entities = [Produto::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produtoDao(): ProdutoDao

    companion object {
        private const val DATABASE_NAME = "scanproduto.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Retorna a instância singleton do banco de dados.
         * Thread-safe com double-checked locking.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // Em produção, implementar Migrations
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
