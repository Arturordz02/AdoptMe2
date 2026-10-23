package com.example.adoptme.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MascotaVisitadaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AdoptMeDatabase : RoomDatabase() {

    abstract fun mascotaVisitadaDao(): MascotaVisitadaDao

    companion object {
        @Volatile
        private var INSTANCE: AdoptMeDatabase? = null

        fun obtenerInstancia(contexto: Context): AdoptMeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    contexto.applicationContext,
                    AdoptMeDatabase::class.java,
                    "adoptme_local.db"
                ).build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}

