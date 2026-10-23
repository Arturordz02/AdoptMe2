package com.example.adoptme.data

import android.content.Context
import com.example.adoptme.AdoptMe.UsuarioRegistrado
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.ConversacionCentro
import com.example.adoptme.model.MensajeChat
import com.example.adoptme.model.NotificationItem
import org.json.JSONArray
import org.json.JSONObject

// =============================================================================
// FUNCIONES PURAS DE NORMALIZACIÓN Y GENERACIÓN DE CLAVES
// =============================================================================

/**
 * Normaliza un correo para uso consistente: minúsculas y sin espacios laterales.
 */
fun normalizarCorreoUsuario(correo: String): String {
    return correo.trim().lowercase()
}

/**
 * Genera una clave segura para SharedPreferences a partir del correo del usuario.
 * Ejemplo: "usuario@adoptme.com" -> "usuario_adoptme_com"
 */
fun obtenerClaveUsuario(correo: String): String {
    val normalizado = normalizarCorreoUsuario(correo)
    if (normalizado.isBlank()) return "anonimo"
    return normalizado.replace(Regex("[^a-zA-Z0-9]"), "_")
}

/**
 * Genera la clave de almacenamiento para el chat entre un adoptante y un centro por su ID.
 * Ejemplo: "chat_usuario_adoptme_com_centro_1"
 */
fun claveChat(
    correoAdoptante: String,
    idCentro: Int
): String {
    return "chat_${obtenerClaveUsuario(correoAdoptante)}_centro_$idCentro"
}

/**
 * Genera la clave de almacenamiento para el listado de conversaciones de un centro.
 */
fun claveConversacionesCentro(correoCentro: String): String {
    return "conversaciones_centro_${obtenerClaveUsuario(correoCentro)}"
}

class AppStorage(
    context: Context
) {

    private val preferencias =
        context.getSharedPreferences(
            "adoptme_datos",
            Context.MODE_PRIVATE
        )

    companion object {
        const val CUENTA_DEFECTO = "usuario@adoptme.com"
        const val CORREO_ADMIN = "admin@adoptme.com"
        const val CLAVE_SOLICITUDES_ADMIN = "solicitudes_admin"
    }

    // =========================================================================
    // ESTRATEGIA DE CLAVES POR USUARIO
    // =========================================================================

    fun claveFavoritos(correo: String): String =
        "favoritos_${obtenerClaveUsuario(correo)}"

    fun claveNotificaciones(correo: String): String =
        "notificaciones_${obtenerClaveUsuario(correo)}"

    fun claveSolicitudes(correo: String): String =
        "solicitudes_${obtenerClaveUsuario(correo)}"

    fun claveChat(correoAdoptante: String, idCentro: Int): String =
        com.example.adoptme.data.claveChat(correoAdoptante, idCentro)

    fun claveConversacionesCentro(correoCentro: String): String =
        com.example.adoptme.data.claveConversacionesCentro(correoCentro)

    // =========================================================================
    // PERFIL DE USUARIO EN CACHE (Presentación offline / sincronización de sesión)
    // =========================================================================

    fun clavePerfilUsuario(correo: String): String =
        "perfil_cache_${obtenerClaveUsuario(correo)}"

    fun guardarPerfilCache(
        correo: String,
        nombre: String,
        telefono: String,
        rol: String,
        id: Int = 0
    ) {
        val clave = clavePerfilUsuario(correo)
        val obj = JSONObject().apply {
            put("id", id)
            put("correo", normalizarCorreoUsuario(correo))
            put("nombre", nombre)
            put("telefono", telefono)
            put("rol", rol)
        }
        preferencias.edit()
            .putString(clave, obj.toString())
            .apply()
    }

    fun cargarPerfilCache(correo: String): UsuarioRegistrado? {
        val clave = clavePerfilUsuario(correo)
        val contenido = preferencias.getString(clave, null) ?: return null
        return try {
            val obj = JSONObject(contenido)
            UsuarioRegistrado(
                nombre = obj.optString("nombre", ""),
                correo = obj.optString("correo", normalizarCorreoUsuario(correo)),
                contrasena = "",
                telefono = obj.optString("telefono", ""),
                rol = obj.optString("rol", "Adoptante")
            )
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // FAVORITOS POR USUARIO (Soporta identificadores unívocos 'remota-id')
    // =========================================================================

    fun claveFavoritosClaves(correo: String): String =
        "favoritos_claves_${obtenerClaveUsuario(correo)}"

    fun guardarFavoritosClaves(correo: String, claves: List<String>) {
        val clave = claveFavoritosClaves(correo)
        val arreglo = JSONArray()
        claves.distinct().forEach { claveItem ->
            arreglo.put(claveItem)
        }

        preferencias.edit()
            .putString(clave, arreglo.toString())
            .apply()
    }

    fun cargarFavoritosClaves(correo: String): List<String> {
        val clave = claveFavoritosClaves(correo)
        val contenido = preferencias.getString(clave, null) ?: return emptyList()
        val resultado = mutableListOf<String>()

        try {
            val arreglo = JSONArray(contenido)
            for (i in 0 until arreglo.length()) {
                val item = arreglo.optString(i, "")
                if (item.isNotBlank()) {
                    resultado.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultado.distinct()
    }

    fun eliminarFavoritoPorClave(correo: String, claveMascota: String) {
        val favoritosActuales = cargarFavoritosClaves(correo).toMutableList()
        if (favoritosActuales.remove(claveMascota)) {
            guardarFavoritosClaves(correo, favoritosActuales)
        }
    }

    fun guardarFavoritos(correo: String, ids: List<Int>) {
        val clave = claveFavoritos(correo)
        val arreglo = JSONArray()
        ids.distinct().forEach { id ->
            arreglo.put(id)
        }

        preferencias.edit()
            .putString(clave, arreglo.toString())
            .apply()
    }

    fun cargarFavoritos(correo: String): List<Int> {
        val clave = claveFavoritos(correo)
        val contenido = preferencias.getString(clave, null) ?: return emptyList()
        val resultado = mutableListOf<Int>()

        try {
            val arreglo = JSONArray(contenido)
            for (i in 0 until arreglo.length()) {
                resultado.add(arreglo.getInt(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultado.distinct()
    }

    fun eliminarFavorito(correo: String, id: Int) {
        val favoritosActuales = cargarFavoritos(correo).toMutableList()
        if (favoritosActuales.remove(id)) {
            guardarFavoritos(correo, favoritosActuales)
        }
    }

    // =========================================================================
    // NOTIFICACIONES POR USUARIO
    // =========================================================================

    fun guardarNotificaciones(
        correo: String,
        notificaciones: List<NotificationItem>
    ) {
        val clave = claveNotificaciones(correo)
        val arreglo = JSONArray()

        notificaciones.forEach { notificacion ->
            val objeto = JSONObject()
            objeto.put("title", notificacion.title)
            objeto.put("message", notificacion.message)
            arreglo.put(objeto)
        }

        preferencias.edit()
            .putString(clave, arreglo.toString())
            .apply()
    }

    fun cargarNotificaciones(correo: String): List<NotificationItem> {
        val clave = claveNotificaciones(correo)
        val contenido = preferencias.getString(clave, null) ?: return emptyList()
        val resultado = mutableListOf<NotificationItem>()

        try {
            val arreglo = JSONArray(contenido)
            for (i in 0 until arreglo.length()) {
                val objeto = arreglo.getJSONObject(i)
                resultado.add(
                    NotificationItem(
                        title = objeto.getString("title"),
                        message = objeto.getString("message")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultado
    }

    fun agregarNotificacion(correo: String, item: NotificationItem) {
        val existentes = cargarNotificaciones(correo).toMutableList()
        existentes.add(0, item)
        guardarNotificaciones(correo, existentes)
    }

    // =========================================================================
    // SOLICITUDES DE ADOPCIÓN (ADMIN Y PARTICIÓN POR USUARIO)
    // =========================================================================

    private fun serializarSolicitudes(solicitudes: List<AdoptionRequest>): JSONArray {
        val arreglo = JSONArray()
        solicitudes.forEach { solicitud ->
            val objeto = JSONObject()
            objeto.put("petId", solicitud.petId)
            objeto.put("petName", solicitud.petName)
            objeto.put("petSpecies", solicitud.petSpecies)
            objeto.put("applicantName", solicitud.applicantName)
            objeto.put("phone", solicitud.phone)
            objeto.put("email", solicitud.email)
            objeto.put("address", solicitud.address)
            objeto.put("housingType", solicitud.housingType)
            objeto.put("timeAvailability", solicitud.timeAvailability)
            objeto.put("previousExperience", solicitud.previousExperience)
            objeto.put("reason", solicitud.reason)
            objeto.put("status", solicitud.status)
            objeto.put("id", solicitud.id)
            arreglo.put(objeto)
        }
        return arreglo
    }

    private fun deserializarSolicitudes(contenido: String?): List<AdoptionRequest> {
        if (contenido.isNullOrBlank()) return emptyList()
        val resultado = mutableListOf<AdoptionRequest>()
        try {
            val arreglo = JSONArray(contenido)
            for (i in 0 until arreglo.length()) {
                val objeto = arreglo.getJSONObject(i)
                resultado.add(
                    AdoptionRequest(
                        petId = objeto.optInt("petId", 0),
                        petName = objeto.getString("petName"),
                        petSpecies = objeto.optString("petSpecies", ""),
                        applicantName = objeto.optString("applicantName", ""),
                        phone = objeto.optString("phone", ""),
                        email = objeto.optString("email", ""),
                        address = objeto.optString("address", ""),
                        housingType = objeto.optString("housingType", ""),
                        timeAvailability = objeto.optString("timeAvailability", ""),
                        previousExperience = objeto.optString("previousExperience", ""),
                        reason = objeto.optString("reason", ""),
                        status = objeto.optString("status", "Pendiente"),
                        id = objeto.optInt("id", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultado
    }

    /**
     * Guarda la lista completa de solicitudes:
     * - Actualiza el maestro general de administración (`solicitudes_admin`).
     * - Actualiza las particiones individuales por usuario según el correo del solicitante.
     */
    fun guardarSolicitudes(solicitudes: List<AdoptionRequest>) {
        val editor = preferencias.edit()

        // 1. Maestro general de solicitudes para el Administrador
        val arregloAdmin = serializarSolicitudes(solicitudes)
        editor.putString(CLAVE_SOLICITUDES_ADMIN, arregloAdmin.toString())

        // 2. Particionar por cada solicitante para aislamiento garantizado
        val porUsuario = solicitudes.groupBy { normalizarCorreoUsuario(it.email) }
        porUsuario.forEach { (correoNorm, listaUsuario) ->
            if (correoNorm.isNotBlank()) {
                val claveUsuario = claveSolicitudes(correoNorm)
                editor.putString(claveUsuario, serializarSolicitudes(listaUsuario).toString())
            }
        }

        editor.apply()
    }

    /**
     * Carga todas las solicitudes registradas en la aplicación (usado por Admin).
     */
    fun cargarSolicitudesGenerales(): List<AdoptionRequest> {
        val contenidoAdmin = preferencias.getString(CLAVE_SOLICITUDES_ADMIN, null)
        if (!contenidoAdmin.isNullOrBlank()) {
            return deserializarSolicitudes(contenidoAdmin)
        }
        // Compatibilidad previa con clave antigua si aún no se ha migrado
        val contenidoAntiguo = preferencias.getString("solicitudes", null)
        return deserializarSolicitudes(contenidoAntiguo)
    }

    /**
     * Carga únicamente las solicitudes pertenecientes al correo del adoptante especificado.
     */
    fun cargarSolicitudesUsuario(correo: String): List<AdoptionRequest> {
        val clave = claveSolicitudes(correo)
        val contenido = preferencias.getString(clave, null)
        if (!contenido.isNullOrBlank()) {
            return deserializarSolicitudes(contenido)
        }
        // Respaldo desde maestro filtrando por correo normalizado
        val correoNorm = normalizarCorreoUsuario(correo)
        return cargarSolicitudesGenerales().filter {
            normalizarCorreoUsuario(it.email) == correoNorm
        }
    }

    /**
     * Carga solicitudes conmutando automáticamente entre el maestro general (Admin)
     * y la partición aislada del adoptante.
     */
    fun cargarSolicitudes(correo: String = ""): List<AdoptionRequest> {
        val correoNorm = normalizarCorreoUsuario(correo)
        return if (correoNorm.isBlank() || correoNorm == CORREO_ADMIN) {
            cargarSolicitudesGenerales()
        } else {
            cargarSolicitudesUsuario(correoNorm)
        }
    }

    /**
     * Actualiza el estado de una solicitud existente en el maestro general y en la partición del usuario.
     */
    fun actualizarEstadoSolicitud(solicitudActualizada: AdoptionRequest) {
        val todas = cargarSolicitudesGenerales().toMutableList()
        val indice = todas.indexOfFirst {
            val coincideSolicitudId = (it.id > 0 && solicitudActualizada.id > 0 && it.id == solicitudActualizada.id)
            val coincideId = (it.petId != 0 && it.petId == solicitudActualizada.petId)
            val coincideNombre = it.petName.equals(solicitudActualizada.petName, ignoreCase = true)
            val coincidePostulante = normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario(solicitudActualizada.email)
            coincideSolicitudId || ((coincideId || coincideNombre) && coincidePostulante)
        }

        if (indice != -1) {
            todas[indice] = solicitudActualizada
        } else {
            todas.add(solicitudActualizada)
        }

        guardarSolicitudes(todas)
    }

    // =========================================================================
    // MIGRACIÓN IDEMPOTENTE DE DATOS GLOBALES ANTIGUOS
    // =========================================================================

    /**
     * Migra las claves globales antiguas de forma idempotente hacia la cuenta por defecto (usuario@adoptme.com)
     * y elimina las claves globales originales para evitar fugas de datos entre cuentas.
     */
    fun migrarDatosAntiguosSiExisten(correoDestino: String = CUENTA_DEFECTO) {
        val editor = preferencias.edit()
        var huboCambios = false

        // 1. Migración de Favoritos globales
        val favsAntiguos = preferencias.getString("favoritos", null)
        if (!favsAntiguos.isNullOrBlank()) {
            val idsExistentes = cargarFavoritos(correoDestino)
            val idsAntiguos = mutableListOf<Int>()
            try {
                val arr = JSONArray(favsAntiguos)
                for (i in 0 until arr.length()) {
                    idsAntiguos.add(arr.getInt(i))
                }
            } catch (_: Exception) {}

            val combinados = (idsExistentes + idsAntiguos).distinct()
            guardarFavoritos(correoDestino, combinados)
            editor.remove("favoritos")
            huboCambios = true
        }

        // 2. Migración de Notificaciones globales
        val notifsAntiguas = preferencias.getString("notificaciones", null)
        if (!notifsAntiguas.isNullOrBlank()) {
            val existentes = cargarNotificaciones(correoDestino)
            val antiguas = mutableListOf<NotificationItem>()
            try {
                val arr = JSONArray(notifsAntiguas)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    antiguas.add(
                        NotificationItem(
                            title = obj.getString("title"),
                            message = obj.getString("message")
                        )
                    )
                }
            } catch (_: Exception) {}

            val combinadas = (existentes + antiguas).distinctBy { "${it.title}|${it.message}" }
            guardarNotificaciones(correoDestino, combinadas)
            editor.remove("notificaciones")
            huboCambios = true
        }

        // 3. Migración de Solicitudes globales
        val solsAntiguas = preferencias.getString("solicitudes", null)
        if (!solsAntiguas.isNullOrBlank()) {
            val existentes = cargarSolicitudesGenerales()
            val antiguas = deserializarSolicitudes(solsAntiguas)
            val combinadas = (existentes + antiguas).distinctBy {
                "${it.petId}|${normalizarCorreoUsuario(it.email)}|${it.status}"
            }
            guardarSolicitudes(combinadas)
            editor.remove("solicitudes")
            huboCambios = true
        }

        if (huboCambios) {
            editor.apply()
        }
    }

    // =========================================================================
    // CHAT LOCAL Y AISLADO ENTRE ADOPTANTE Y CENTRO
    // =========================================================================

    /**
     * Guarda los mensajes de una conversación entre un adoptante y un centro por su ID.
     */
    fun guardarMensajesChat(
        correoAdoptante: String,
        idCentro: Int,
        mensajes: List<MensajeChat>
    ) {
        val clave = claveChat(correoAdoptante, idCentro)
        val arreglo = JSONArray()
        mensajes.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("idCentro", m.idCentro)
                put("remitente", m.remitente)
                put("receptor", m.receptor)
                put("texto", m.texto)
                put("nombreCentro", m.nombreCentro)
                put("fecha", m.fecha)
                put("esDelCentro", m.esDelCentro)
            }
            arreglo.put(obj)
        }

        preferencias.edit()
            .putString(clave, arreglo.toString())
            .apply()
    }

    /**
     * Carga los mensajes de la conversación entre un adoptante y un centro por su ID.
     */
    fun cargarMensajesChat(
        correoAdoptante: String,
        idCentro: Int
    ): List<MensajeChat> {
        val clave = claveChat(correoAdoptante, idCentro)
        val contenido = preferencias.getString(clave, null) ?: return emptyList()
        val resultado = mutableListOf<MensajeChat>()

        try {
            val arreglo = JSONArray(contenido)
            for (i in 0 until arreglo.length()) {
                val obj = arreglo.getJSONObject(i)
                resultado.add(
                    MensajeChat(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        idCentro = obj.optInt("idCentro", idCentro),
                        remitente = obj.optString("remitente", ""),
                        receptor = obj.optString("receptor", ""),
                        texto = obj.optString("texto", ""),
                        nombreCentro = obj.optString("nombreCentro", ""),
                        fecha = obj.optString("fecha", ""),
                        esDelCentro = obj.optBoolean("esDelCentro", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultado
    }

    /**
     * Registra o actualiza el índice de conversaciones recibidas por un centro de adopción.
     */
    fun registrarConversacionCentro(
        correoCentro: String,
        idCentro: Int,
        correoAdoptante: String,
        nombreCentro: String,
        ultimoTexto: String,
        fecha: String
    ) {
        val clave = claveConversacionesCentro(correoCentro)
        val actuales = cargarConversacionesCentro(correoCentro).toMutableList()
        val adoptanteNorm = normalizarCorreoUsuario(correoAdoptante)

        // Evitar duplicar la conversación; actualizar su posición y último mensaje
        actuales.removeAll { it.idCentro == idCentro && normalizarCorreoUsuario(it.correoAdoptante) == adoptanteNorm }
        actuales.add(
            0,
            ConversacionCentro(
                idCentro = idCentro,
                correoAdoptante = adoptanteNorm,
                correoCentro = normalizarCorreoUsuario(correoCentro),
                nombreCentro = nombreCentro,
                ultimoTexto = ultimoTexto,
                fecha = fecha
            )
        )

        val arreglo = JSONArray()
        actuales.forEach { c ->
            val obj = JSONObject().apply {
                put("idCentro", c.idCentro)
                put("correoAdoptante", c.correoAdoptante)
                put("correoCentro", c.correoCentro)
                put("nombreCentro", c.nombreCentro)
                put("ultimoTexto", c.ultimoTexto)
                put("fecha", c.fecha)
            }
            arreglo.put(obj)
        }

        preferencias.edit()
            .putString(clave, arreglo.toString())
            .apply()
    }

    /**
     * Carga la lista de conversaciones recibidas por un centro.
     */
    fun cargarConversacionesCentro(
        correoCentro: String
    ): List<ConversacionCentro> {
        val clave = claveConversacionesCentro(correoCentro)
        val contenido = preferencias.getString(clave, null) ?: return emptyList()
        val resultado = mutableListOf<ConversacionCentro>()

        try {
            val arreglo = JSONArray(contenido)
            for (i in 0 until arreglo.length()) {
                val obj = arreglo.getJSONObject(i)
                resultado.add(
                    ConversacionCentro(
                        idCentro = obj.optInt("idCentro", 1),
                        correoAdoptante = obj.optString("correoAdoptante", ""),
                        correoCentro = obj.optString("correoCentro", correoCentro),
                        nombreCentro = obj.optString("nombreCentro", ""),
                        ultimoTexto = obj.optString("ultimoTexto", ""),
                        fecha = obj.optString("fecha", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultado
    }
}