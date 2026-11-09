package com.example.adoptme

import android.content.SharedPreferences
import com.example.adoptme.preferences.Preferencias
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pruebas unitarias para la persistencia y auditoría de Aceptación de Términos y Privacidad (Entrega #4)
 * Valida:
 * 1. Correo del usuario normalizado
 * 2. Versión de los términos
 * 3. Fecha y hora de aceptación (formato ISO 8601)
 * 4. Persistencia completamente separada por usuario
 */
class PreferenciasTerminosTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var preferencias: Preferencias

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        preferencias = Preferencias(fakePrefs)
    }

    @Test
    fun usuarioNoAceptado_retornaValoresPorDefecto() {
        val correo = "usuario.nuevo@ejemplo.com"
        assertFalse("El usuario no debe tener términos aceptados por defecto", preferencias.haAceptadoTerminos(correo))
        assertNull("La versión de términos debe ser nula", preferencias.obtenerVersionTerminosAceptada(correo))
        assertNull("La fecha de términos debe ser nula", preferencias.obtenerFechaAceptacionTerminos(correo))
        assertNull("El registro completo debe ser nulo", preferencias.obtenerRegistroAceptacionTerminos(correo))
    }

    @Test
    fun guardarAceptacionTerminos_almacenaCorreoVersionYFechaIso() {
        val correo = "adoptante@adoptme.pe"
        val version = "1.0_2026_E4"
        val fechaFijada = "2026-11-09T16:30:00Z"

        preferencias.guardarAceptacionTerminos(correo, version, fechaFijada)

        assertTrue(preferencias.haAceptadoTerminos(correo))
        assertEquals(version, preferencias.obtenerVersionTerminosAceptada(correo))
        assertEquals(fechaFijada, preferencias.obtenerFechaAceptacionTerminos(correo))

        val registro = preferencias.obtenerRegistroAceptacionTerminos(correo)
        assertNotNull(registro)
        assertEquals(correo, registro?.correo)
        assertEquals(version, registro?.version)
        assertEquals(fechaFijada, registro?.fechaHora)
        assertTrue(registro?.aceptado == true)
    }

    @Test
    fun guardarAceptacionTerminos_sinFecha_generaFechaIsoValida() {
        val correo = "maria@test.com"
        preferencias.guardarAceptacionTerminos(correo, "1.0_2026_E4")

        val fecha = preferencias.obtenerFechaAceptacionTerminos(correo)
        assertNotNull(fecha)

        // Validar formato ISO 8601 (yyyy-MM-dd'T'HH:mm:ss'Z')
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        formato.isLenient = false
        val fechaParseada = formato.parse(fecha!!)
        assertNotNull("La fecha y hora generada debe ser un ISO 8601 válido", fechaParseada)
    }

    @Test
    fun persistencia_separadaPorUsuario_noColisiona() {
        val usuarioA = "adoptante.uno@gmail.com"
        val usuarioB = "adoptante.dos@hotmail.com"

        preferencias.guardarAceptacionTerminos(usuarioA, "1.0_2026_E4", "2026-11-01T10:00:00Z")
        preferencias.guardarAceptacionTerminos(usuarioB, "2.0_2026_E5", "2026-11-05T15:30:00Z")

        // Usuario A mantiene su propia información
        assertTrue(preferencias.haAceptadoTerminos(usuarioA))
        assertEquals("1.0_2026_E4", preferencias.obtenerVersionTerminosAceptada(usuarioA))
        assertEquals("2026-11-01T10:00:00Z", preferencias.obtenerFechaAceptacionTerminos(usuarioA))

        // Usuario B mantiene su propia información sin afectar al Usuario A
        assertTrue(preferencias.haAceptadoTerminos(usuarioB))
        assertEquals("2.0_2026_E5", preferencias.obtenerVersionTerminosAceptada(usuarioB))
        assertEquals("2026-11-05T15:30:00Z", preferencias.obtenerFechaAceptacionTerminos(usuarioB))

        // Usuario C no registrado permanece intacto
        assertFalse(preferencias.haAceptadoTerminos("usuario.tercero@gmail.com"))
    }

    @Test
    fun normalizacionCorreo_reconoceMayusculasYEspacios() {
        val correoOriginal = "Carlos.Perez@AdoptMe.Org"
        val correoConsulta = "  carlos.perez@adoptme.org  "

        preferencias.guardarAceptacionTerminos(correoOriginal, "1.0_2026_E4")

        assertTrue("Debe encontrar la aceptación a pesar de diferencias de mayúsculas/espacios",
            preferencias.haAceptadoTerminos(correoConsulta))
        assertEquals("1.0_2026_E4", preferencias.obtenerVersionTerminosAceptada(correoConsulta))
    }
}

/**
 * Implementación simulada en memoria de SharedPreferences para pruebas unitarias rápidas
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        (data[key] as? String) ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        (data[key] as? Int) ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        (data[key] as? Long) ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        (data[key] as? Float) ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        (data[key] as? Boolean) ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(data)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val sharedData: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val toRemove = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) toRemove.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) sharedData.clear()
            toRemove.forEach { sharedData.remove(it) }
            sharedData.putAll(temp)
        }
    }
}
