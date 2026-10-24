package com.example.adoptme.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.adoptme.SharedPreferences.Preferencias
import com.example.adoptme.data.normalizarCorreoUsuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extensión para asegurar una única instancia de DataStore por Context
private val Context.catalogoDataStore: DataStore<Preferences> by preferencesDataStore(name = "adoptme_catalogo_preferences")

/**
 * Modelo de datos inmutable para las preferencias del catálogo expuestas por DataStore.
 */
data class PreferenciasCatalogo(
    val mascotaPreferida: String = Preferencias.MASCOTA_TODAS,
    val ubicacionPreferida: String = Preferencias.UBICACION_TODAS,
    val ordenPredeterminado: String = Preferencias.ORDEN_RECIENTES,
    val soloDisponibles: Boolean = false
)

/**
 * Claves de Preferences DataStore y de SharedPreferences aisladas por correo normalizado.
 */
object ClavesCatalogo {
    fun claveMascota(correo: String): Preferences.Key<String> =
        stringPreferencesKey("mascota_preferida_${normalizarCorreoUsuario(correo)}")

    fun claveUbicacion(correo: String): Preferences.Key<String> =
        stringPreferencesKey("ubicacion_preferida_${normalizarCorreoUsuario(correo)}")

    fun claveOrden(correo: String): Preferences.Key<String> =
        stringPreferencesKey("orden_predeterminado_${normalizarCorreoUsuario(correo)}")

    fun claveSoloDisponibles(correo: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("solo_disponibles_${normalizarCorreoUsuario(correo)}")

    // Claves equivalentes para consultar o limpiar en SharedPreferences durante la migración
    fun spKeyMascota(correo: String): String = "mascota_preferida_${normalizarCorreoUsuario(correo)}"
    fun spKeyUbicacion(correo: String): String = "ubicacion_preferida_${normalizarCorreoUsuario(correo)}"
    fun spKeyOrden(correo: String): String = "orden_predeterminado_${normalizarCorreoUsuario(correo)}"
    fun spKeySoloDisponibles(correo: String): String = "solo_disponibles_${normalizarCorreoUsuario(correo)}"
}

/**
 * Función pura que modela la lógica de migración única e idempotente de catálogo.
 * Permite verificar rigurosamente las reglas en pruebas unitarias sin dependencias de Context.
 */
fun ejecutarLogicaMigracionCatalogo(
    correo: String,
    spValores: Map<String, Any>,
    dataStoreValores: MutableMap<String, Any>
): Pair<Boolean, Set<String>> {
    val correoNorm = normalizarCorreoUsuario(correo)
    val spKeyMascota = ClavesCatalogo.spKeyMascota(correoNorm)
    val spKeyUbicacion = ClavesCatalogo.spKeyUbicacion(correoNorm)
    val spKeyOrden = ClavesCatalogo.spKeyOrden(correoNorm)
    val spKeyDisponibles = ClavesCatalogo.spKeySoloDisponibles(correoNorm)

    val clavesCandidatasSP = setOf(spKeyMascota, spKeyUbicacion, spKeyOrden, spKeyDisponibles)
    val tieneEnSp = clavesCandidatasSP.any { spValores.containsKey(it) }

    val kMascota = ClavesCatalogo.claveMascota(correoNorm).name
    val kUbicacion = ClavesCatalogo.claveUbicacion(correoNorm).name
    val kOrden = ClavesCatalogo.claveOrden(correoNorm).name
    val kDisponibles = ClavesCatalogo.claveSoloDisponibles(correoNorm).name
    val clavesCandidatasDS = setOf(kMascota, kUbicacion, kOrden, kDisponibles)

    val yaTieneEnDS = clavesCandidatasDS.any { dataStoreValores.containsKey(it) }

    if (yaTieneEnDS) {
        // Idempotente: DataStore ya cuenta con valores; se retiran las claves viejas de SP
        val clavesALimpiar = clavesCandidatasSP.filter { spValores.containsKey(it) }.toSet()
        return Pair(false, clavesALimpiar)
    }

    if (!tieneEnSp) {
        return Pair(false, emptySet())
    }

    // Copiar a DataStore
    spValores[spKeyMascota]?.let { dataStoreValores[kMascota] = it }
    spValores[spKeyUbicacion]?.let { dataStoreValores[kUbicacion] = it }
    spValores[spKeyOrden]?.let { dataStoreValores[kOrden] = it }
    spValores[spKeyDisponibles]?.let { dataStoreValores[kDisponibles] = it }

    val clavesRemovidas = clavesCandidatasSP.filter { spValores.containsKey(it) }.toSet()
    return Pair(true, clavesRemovidas)
}

/**
 * Repositorio singleton de Preferences DataStore para el catálogo de AdoptMe.
 */
class CatalogoDataStore private constructor(private val context: Context) {

    /**
     * Expone el flujo reactivo con las cuatro preferencias de catálogo para el usuario indicado.
     */
    fun obtenerPreferencias(correo: String): Flow<PreferenciasCatalogo> {
        val correoNorm = normalizarCorreoUsuario(correo)
        val kMascota = ClavesCatalogo.claveMascota(correoNorm)
        val kUbicacion = ClavesCatalogo.claveUbicacion(correoNorm)
        val kOrden = ClavesCatalogo.claveOrden(correoNorm)
        val kDisponibles = ClavesCatalogo.claveSoloDisponibles(correoNorm)

        return context.catalogoDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                PreferenciasCatalogo(
                    mascotaPreferida = prefs[kMascota] ?: Preferencias.MASCOTA_TODAS,
                    ubicacionPreferida = prefs[kUbicacion] ?: Preferencias.UBICACION_TODAS,
                    ordenPredeterminado = prefs[kOrden] ?: Preferencias.ORDEN_RECIENTES,
                    soloDisponibles = prefs[kDisponibles] ?: false
                )
            }
    }

    suspend fun guardarMascotaPreferida(correo: String, mascota: String) {
        val correoNorm = normalizarCorreoUsuario(correo)
        context.catalogoDataStore.edit { prefs ->
            prefs[ClavesCatalogo.claveMascota(correoNorm)] = mascota
        }
    }

    suspend fun guardarUbicacionPreferida(correo: String, ubicacion: String) {
        val correoNorm = normalizarCorreoUsuario(correo)
        context.catalogoDataStore.edit { prefs ->
            prefs[ClavesCatalogo.claveUbicacion(correoNorm)] = ubicacion
        }
    }

    suspend fun guardarOrdenPredeterminado(correo: String, orden: String) {
        val correoNorm = normalizarCorreoUsuario(correo)
        context.catalogoDataStore.edit { prefs ->
            prefs[ClavesCatalogo.claveOrden(correoNorm)] = orden
        }
    }

    suspend fun guardarSoloDisponibles(correo: String, soloDisponibles: Boolean) {
        val correoNorm = normalizarCorreoUsuario(correo)
        context.catalogoDataStore.edit { prefs ->
            prefs[ClavesCatalogo.claveSoloDisponibles(correoNorm)] = soloDisponibles
        }
    }

    suspend fun restablecerPreferencias(correo: String) {
        val correoNorm = normalizarCorreoUsuario(correo)
        context.catalogoDataStore.edit { prefs ->
            prefs.remove(ClavesCatalogo.claveMascota(correoNorm))
            prefs.remove(ClavesCatalogo.claveUbicacion(correoNorm))
            prefs.remove(ClavesCatalogo.claveOrden(correoNorm))
            prefs.remove(ClavesCatalogo.claveSoloDisponibles(correoNorm))
        }
    }

    /**
     * Migración única e idempotente desde SharedPreferences hacia Preferences DataStore.
     * Retira únicamente las 4 claves del catálogo de SharedPreferences conservando tema, color,
     * tamaño de texto, JWT y notificaciones intactos.
     */
    suspend fun migrarDesdeSharedPreferencesSiEsNecesario(
        correo: String,
        sharedPreferences: SharedPreferences
    ): Boolean {
        val correoNorm = normalizarCorreoUsuario(correo)
        if (correoNorm.isBlank()) return false

        val spKeyMascota = ClavesCatalogo.spKeyMascota(correoNorm)
        val spKeyUbicacion = ClavesCatalogo.spKeyUbicacion(correoNorm)
        val spKeyOrden = ClavesCatalogo.spKeyOrden(correoNorm)
        val spKeyDisponibles = ClavesCatalogo.spKeySoloDisponibles(correoNorm)

        val tieneEnSp = sharedPreferences.contains(spKeyMascota) ||
                sharedPreferences.contains(spKeyUbicacion) ||
                sharedPreferences.contains(spKeyOrden) ||
                sharedPreferences.contains(spKeyDisponibles) ||
                sharedPreferences.contains("mascota_preferida") ||
                sharedPreferences.contains("ubicacion_preferida") ||
                sharedPreferences.contains("orden_predeterminado") ||
                sharedPreferences.contains("solo_disponibles")

        if (!tieneEnSp) {
            return false
        }

        val prefsActuales = context.catalogoDataStore.data.first()
        val kMascota = ClavesCatalogo.claveMascota(correoNorm)
        val kUbicacion = ClavesCatalogo.claveUbicacion(correoNorm)
        val kOrden = ClavesCatalogo.claveOrden(correoNorm)
        val kDisponibles = ClavesCatalogo.claveSoloDisponibles(correoNorm)

        val yaTieneEnDataStore = prefsActuales.contains(kMascota) ||
                prefsActuales.contains(kUbicacion) ||
                prefsActuales.contains(kOrden) ||
                prefsActuales.contains(kDisponibles)

        if (yaTieneEnDataStore) {
            // Idempotente: DataStore ya tiene valores. Limpiar claves viejas de SP
            sharedPreferences.edit()
                .remove(spKeyMascota)
                .remove(spKeyUbicacion)
                .remove(spKeyOrden)
                .remove(spKeyDisponibles)
                .remove("mascota_preferida")
                .remove("ubicacion_preferida")
                .remove("orden_predeterminado")
                .remove("solo_disponibles")
                .apply()
            return false
        }

        // Leer valores de SharedPreferences
        val valorMascota = sharedPreferences.getString(spKeyMascota, null)
            ?: sharedPreferences.getString("mascota_preferida", null)
        val valorUbicacion = sharedPreferences.getString(spKeyUbicacion, null)
            ?: sharedPreferences.getString("ubicacion_preferida", null)
        val valorOrden = sharedPreferences.getString(spKeyOrden, null)
            ?: sharedPreferences.getString("orden_predeterminado", null)
        val valorDisponibles = when {
            sharedPreferences.contains(spKeyDisponibles) -> sharedPreferences.getBoolean(spKeyDisponibles, false)
            sharedPreferences.contains("solo_disponibles") -> sharedPreferences.getBoolean("solo_disponibles", false)
            else -> null
        }

        // Escribir en DataStore de forma confirmada
        context.catalogoDataStore.edit { dsPrefs ->
            if (valorMascota != null) dsPrefs[kMascota] = valorMascota
            if (valorUbicacion != null) dsPrefs[kUbicacion] = valorUbicacion
            if (valorOrden != null) dsPrefs[kOrden] = valorOrden
            if (valorDisponibles != null) dsPrefs[kDisponibles] = valorDisponibles
        }

        // Retirar estrictamente las cuatro claves de catálogo de SharedPreferences
        sharedPreferences.edit()
            .remove(spKeyMascota)
            .remove(spKeyUbicacion)
            .remove(spKeyOrden)
            .remove(spKeyDisponibles)
            .remove("mascota_preferida")
            .remove("ubicacion_preferida")
            .remove("orden_predeterminado")
            .remove("solo_disponibles")
            .apply()

        return true
    }

    companion object {
        @Volatile
        private var INSTANCE: CatalogoDataStore? = null

        fun obtenerInstancia(context: Context): CatalogoDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CatalogoDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

