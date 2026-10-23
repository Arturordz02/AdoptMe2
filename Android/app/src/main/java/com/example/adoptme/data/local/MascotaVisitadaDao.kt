package com.example.adoptme.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MascotaVisitadaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarVisita(visita: MascotaVisitadaEntity): Long

    @Query("SELECT * FROM mascotas_visitadas WHERE correoUsuario = :correoUsuario ORDER BY fechaUltimaVisita DESC LIMIT :limite")
    suspend fun obtenerMascotasVisitadas(correoUsuario: String, limite: Int = 10): List<MascotaVisitadaEntity>

    @Query("SELECT * FROM mascotas_visitadas WHERE correoUsuario = :correoUsuario ORDER BY fechaUltimaVisita DESC LIMIT :limite")
    fun observarMascotasVisitadas(correoUsuario: String, limite: Int = 10): Flow<List<MascotaVisitadaEntity>>

    @Query("SELECT COUNT(*) FROM mascotas_visitadas WHERE correoUsuario = :correoUsuario")
    suspend fun contarVisitasUsuario(correoUsuario: String): Int

    @Query("DELETE FROM mascotas_visitadas WHERE correoUsuario = :correoUsuario")
    suspend fun limpiarHistorialUsuario(correoUsuario: String): Int
}

