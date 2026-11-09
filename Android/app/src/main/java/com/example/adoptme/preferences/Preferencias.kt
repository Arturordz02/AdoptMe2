package com.example.adoptme.preferences

import android.content.Context
import com.example.adoptme.repository.normalizarCorreoUsuario

class Preferencias(
    private val preferencias: android.content.SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(
            "preferencias_adoptme",
            Context.MODE_PRIVATE
        )
    )

    fun obtenerSharedPreferencesInterno(): android.content.SharedPreferences = preferencias

    companion object {
        const val CUENTA_DEFECTO = "usuario@adoptme.com"

        // APARIENCIA
        const val TEMA_SISTEMA = "Sistema"
        const val TEMA_CLARO = "Claro"
        const val TEMA_OSCURO = "Oscuro"

        const val TEXTO_PEQUENO = "Pequeño"
        const val TEXTO_NORMAL = "Normal"
        const val TEXTO_GRANDE = "Grande"

        const val COLOR_VERDE = "Verde"
        const val COLOR_AZUL = "Azul"
        const val COLOR_MORADO = "Morado"

        // MASCOTA PREFERIDA
        const val MASCOTA_TODAS = "Todas"
        const val MASCOTA_PERROS = "Perros"
        const val MASCOTA_GATOS = "Gatos"
        const val MASCOTA_CONEJOS = "Conejos"

        // UBICACIÓN
        const val UBICACION_TODAS = "Todas"

        // ORDEN
        const val ORDEN_RECIENTES = "Más recientes"
        const val ORDEN_NOMBRE = "Nombre A-Z"
    }

    private fun clave(correo: String, nombre: String): String {
        return "${nombre}_${normalizarCorreoUsuario(correo)}"
    }

    // =========================================================
    // APARIENCIA
    // =========================================================

    fun guardarTema(correo: String, tema: String) {
        preferencias.edit()
            .putString(clave(correo, "tema"), tema)
            .apply()
    }

    fun obtenerTema(correo: String = CUENTA_DEFECTO): String {
        return preferencias.getString(
            clave(correo, "tema"),
            TEMA_SISTEMA
        ) ?: TEMA_SISTEMA
    }

    fun guardarTamanoTexto(correo: String, tamano: String) {
        preferencias.edit()
            .putString(clave(correo, "tamano_texto"), tamano)
            .apply()
    }

    fun obtenerTamanoTexto(correo: String = CUENTA_DEFECTO): String {
        return preferencias.getString(
            clave(correo, "tamano_texto"),
            TEXTO_NORMAL
        ) ?: TEXTO_NORMAL
    }

    fun guardarColorPrincipal(correo: String, color: String) {
        preferencias.edit()
            .putString(clave(correo, "color_principal"), color)
            .apply()
    }

    fun obtenerColorPrincipal(correo: String = CUENTA_DEFECTO): String {
        return preferencias.getString(
            clave(correo, "color_principal"),
            COLOR_VERDE
        ) ?: COLOR_VERDE
    }

    fun guardarColor(correo: String, color: String) = guardarColorPrincipal(correo, color)
    fun obtenerColor(correo: String = CUENTA_DEFECTO) = obtenerColorPrincipal(correo)

    // =========================================================
    // 1. TIPO DE MASCOTA PREFERIDA
    // =========================================================

    fun guardarMascotaPreferida(correo: String, tipo: String) {
        preferencias.edit()
            .putString(clave(correo, "mascota_preferida"), tipo)
            .apply()
    }

    fun obtenerMascotaPreferida(correo: String = CUENTA_DEFECTO): String {
        return preferencias.getString(
            clave(correo, "mascota_preferida"),
            MASCOTA_TODAS
        ) ?: MASCOTA_TODAS
    }

    // =========================================================
    // 2. UBICACIÓN PREFERIDA
    // =========================================================

    fun guardarUbicacionPreferida(correo: String, ubicacion: String) {
        preferencias.edit()
            .putString(clave(correo, "ubicacion_preferida"), ubicacion)
            .apply()
    }

    fun obtenerUbicacionPreferida(correo: String = CUENTA_DEFECTO): String {
        return preferencias.getString(
            clave(correo, "ubicacion_preferida"),
            UBICACION_TODAS
        ) ?: UBICACION_TODAS
    }

    // =========================================================
    // 3. NOTIFICACIONES
    // =========================================================

    fun guardarNotificacionesNuevasMascotas(correo: String, activado: Boolean) {
        preferencias.edit()
            .putBoolean(clave(correo, "notificaciones_nuevas_mascotas"), activado)
            .apply()
    }

    fun obtenerNotificacionesNuevasMascotas(correo: String = CUENTA_DEFECTO): Boolean {
        return preferencias.getBoolean(
            clave(correo, "notificaciones_nuevas_mascotas"),
            true
        )
    }

    fun guardarNotificacionesAdopciones(correo: String, activado: Boolean) {
        preferencias.edit()
            .putBoolean(clave(correo, "notificaciones_adopciones"), activado)
            .apply()
    }

    fun obtenerNotificacionesAdopciones(correo: String = CUENTA_DEFECTO): Boolean {
        return preferencias.getBoolean(
            clave(correo, "notificaciones_adopciones"),
            true
        )
    }

    fun guardarNotificacionesMensajes(correo: String, activado: Boolean) {
        preferencias.edit()
            .putBoolean(clave(correo, "notificaciones_mensajes"), activado)
            .apply()
    }

    fun obtenerNotificacionesMensajes(correo: String = CUENTA_DEFECTO): Boolean {
        return preferencias.getBoolean(
            clave(correo, "notificaciones_mensajes"),
            true
        )
    }

    // =========================================================
    // 4. ORDEN PREDETERMINADO
    // =========================================================

    fun guardarOrdenPredeterminado(correo: String, orden: String) {
        preferencias.edit()
            .putString(clave(correo, "orden_predeterminado"), orden)
            .apply()
    }

    fun obtenerOrdenPredeterminado(correo: String = CUENTA_DEFECTO): String {
        return preferencias.getString(
            clave(correo, "orden_predeterminado"),
            ORDEN_RECIENTES
        ) ?: ORDEN_RECIENTES
    }

    fun guardarOrden(correo: String, orden: String) = guardarOrdenPredeterminado(correo, orden)
    fun obtenerOrden(correo: String = CUENTA_DEFECTO) = obtenerOrdenPredeterminado(correo)

    // =========================================================
    // 5. MOSTRAR SOLO DISPONIBLES
    // =========================================================

    fun guardarSoloDisponibles(correo: String, activado: Boolean) {
        preferencias.edit()
            .putBoolean(clave(correo, "solo_disponibles"), activado)
            .apply()
    }

    fun obtenerSoloDisponibles(correo: String = CUENTA_DEFECTO): Boolean {
        return preferencias.getBoolean(
            clave(correo, "solo_disponibles"),
            false
        )
    }

    fun restablecerPreferencias(correo: String) {
        val sufijo = "_${normalizarCorreoUsuario(correo)}"
        val editor = preferencias.edit()
        preferencias.all.keys.forEach { clave ->
            if (clave.endsWith(sufijo)) {
                editor.remove(clave)
            }
        }
        editor.apply()
    }

    fun migrarPreferenciasAntiguasSiExisten(correoDestino: String = CUENTA_DEFECTO) {
        val editor = preferencias.edit()
        var huboCambios = false

        if (preferencias.contains("tema")) {
            guardarTema(correoDestino, preferencias.getString("tema", TEMA_SISTEMA) ?: TEMA_SISTEMA)
            editor.remove("tema")
            huboCambios = true
        }
        if (preferencias.contains("tamano_texto")) {
            guardarTamanoTexto(correoDestino, preferencias.getString("tamano_texto", TEXTO_NORMAL) ?: TEXTO_NORMAL)
            editor.remove("tamano_texto")
            huboCambios = true
        }
        if (preferencias.contains("color_principal")) {
            guardarColorPrincipal(correoDestino, preferencias.getString("color_principal", COLOR_VERDE) ?: COLOR_VERDE)
            editor.remove("color_principal")
            huboCambios = true
        }
        if (preferencias.contains("mascota_preferida")) {
            guardarMascotaPreferida(correoDestino, preferencias.getString("mascota_preferida", MASCOTA_TODAS) ?: MASCOTA_TODAS)
            editor.remove("mascota_preferida")
            huboCambios = true
        }
        if (preferencias.contains("ubicacion_preferida")) {
            guardarUbicacionPreferida(correoDestino, preferencias.getString("ubicacion_preferida", UBICACION_TODAS) ?: UBICACION_TODAS)
            editor.remove("ubicacion_preferida")
            huboCambios = true
        }
        if (preferencias.contains("orden_predeterminado")) {
            guardarOrdenPredeterminado(correoDestino, preferencias.getString("orden_predeterminado", ORDEN_RECIENTES) ?: ORDEN_RECIENTES)
            editor.remove("orden_predeterminado")
            huboCambios = true
        }
        if (preferencias.contains("solo_disponibles")) {
            guardarSoloDisponibles(correoDestino, preferencias.getBoolean("solo_disponibles", false))
            editor.remove("solo_disponibles")
            huboCambios = true
        }
        if (preferencias.contains("notificaciones_nuevas_mascotas")) {
            guardarNotificacionesNuevasMascotas(correoDestino, preferencias.getBoolean("notificaciones_nuevas_mascotas", true))
            editor.remove("notificaciones_nuevas_mascotas")
            huboCambios = true
        }
        if (preferencias.contains("notificaciones_adopciones")) {
            guardarNotificacionesAdopciones(correoDestino, preferencias.getBoolean("notificaciones_adopciones", true))
            editor.remove("notificaciones_adopciones")
            huboCambios = true
        }
        if (preferencias.contains("notificaciones_mensajes")) {
            guardarNotificacionesMensajes(correoDestino, preferencias.getBoolean("notificaciones_mensajes", true))
            editor.remove("notificaciones_mensajes")
            huboCambios = true
        }

        if (huboCambios) {
            editor.apply()
        }
    }

    // =========================================================
    // 6. TOKEN JWT (SESIÓN ACTIVA PARA RETROFIT)
    // =========================================================

    fun guardarTokenSesionActiva(token: String) {
        preferencias.edit().putString("token_jwt_activo", token).apply()
    }

    fun obtenerTokenSesionActiva(): String? {
        return preferencias.getString("token_jwt_activo", null)
    }

    fun borrarTokenSesionActiva() {
        preferencias.edit().remove("token_jwt_activo").apply()
    }

    // =========================================================
    // 7. TÉRMINOS Y CONDICIONES / PRIVACIDAD (Fase 7 - Entrega #4)
    // =========================================================

    data class RegistroAceptacionTerminos(
        val correo: String,
        val version: String,
        val fechaHora: String,
        val aceptado: Boolean
    )

    fun guardarAceptacionTerminos(
        correo: String,
        version: String = "1.0_2026_E4",
        fechaHoraIso: String? = null
    ) {
        val fechaIso = fechaHoraIso ?: java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.US
        ).format(java.util.Date())
        preferencias.edit()
            .putBoolean(clave(correo, "terminos_aceptados"), true)
            .putString(clave(correo, "terminos_version"), version)
            .putString(clave(correo, "terminos_fecha"), fechaIso)
            .apply()
    }

    fun haAceptadoTerminos(correo: String): Boolean {
        return preferencias.getBoolean(clave(correo, "terminos_aceptados"), false)
    }

    fun obtenerFechaAceptacionTerminos(correo: String): String? {
        return preferencias.getString(clave(correo, "terminos_fecha"), null)
    }

    fun obtenerVersionTerminosAceptada(correo: String): String? {
        return preferencias.getString(clave(correo, "terminos_version"), null)
    }

    fun obtenerRegistroAceptacionTerminos(correo: String): RegistroAceptacionTerminos? {
        if (!haAceptadoTerminos(correo)) return null
        return RegistroAceptacionTerminos(
            correo = normalizarCorreoUsuario(correo),
            version = obtenerVersionTerminosAceptada(correo) ?: "1.0_2026_E4",
            fechaHora = obtenerFechaAceptacionTerminos(correo) ?: "",
            aceptado = true
        )
    }
}