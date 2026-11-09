package com.example.adoptme.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.adoptme.model.Pet
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * Convierte un objeto Pet a un JSONObject para su serialización en almacenamiento local.
 * Serializa explícitamente el flag esRemota para garantizar la distinción entre
 * entidades locales y remotas persistidas en Neon PostgreSQL.
 */
fun convertirPetAJson(mascota: Pet, photoPath: String = ""): JSONObject {
    val objeto = JSONObject()
    objeto.put("id", mascota.id)
    objeto.put("name", mascota.name)
    objeto.put("species", mascota.species)
    objeto.put("breed", mascota.breed)
    objeto.put("age", mascota.age)
    objeto.put("gender", mascota.gender)
    objeto.put("location", mascota.location)
    objeto.put("status", mascota.status)
    objeto.put("description", mascota.description)
    objeto.put("creadoPorCorreo", mascota.creadoPorCorreo)
    objeto.put("esRemota", mascota.esRemota)
    objeto.put("photoPath", photoPath)
    if (mascota.imageUrl != null) {
        objeto.put("imageUrl", mascota.imageUrl)
    }
    if (mascota.imagePublicId != null) {
        objeto.put("imagePublicId", mascota.imagePublicId)
    }
    return objeto
}

/**
 * Reconstruye un objeto Pet desde un JSONObject guardado localmente.
 * Mantiene compatibilidad con registros antiguos donde la clave "esRemota" no existía.
 */
fun parsearJsonAPet(objeto: JSONObject, bitmap: Bitmap? = null): Pet {
    val esRemota = if (objeto.has("esRemota")) objeto.getBoolean("esRemota") else false
    return Pet(
        id = objeto.getInt("id"),
        name = objeto.getString("name"),
        species = objeto.getString("species"),
        breed = objeto.getString("breed"),
        age = objeto.getString("age"),
        gender = objeto.getString("gender"),
        location = objeto.optString("location", "Lima"),
        status = objeto.optString("status", "Disponible"),
        description = objeto.getString("description"),
        image = 0,
        photoBitmap = bitmap,
        imageUrl = if (objeto.has("imageUrl") && !objeto.isNull("imageUrl")) objeto.getString("imageUrl") else null,
        imagePublicId = if (objeto.has("imagePublicId") && !objeto.isNull("imagePublicId")) objeto.getString("imagePublicId") else null,
        creadoPorCorreo = objeto.optString("creadoPorCorreo", ""),
        esRemota = esRemota
    )
}

/**
 * Sincroniza las publicaciones del usuario autenticado entre la lista local y las entidades remotas de Neon.
 *
 * Reglas:
 * 1. Solo considera remotas del usuario aquellas cuyo `creadoPorCorreo` coincide con el correo autenticado.
 * 2. Las entidades de Neon (esRemota = true) prevalecen sobre copias locales con el mismo ID.
 * 3. Se preserva el photoBitmap local si la entidad remota aún no tiene imagen asignada.
 * 4. Las publicaciones locales puras (esRemota = false) que pertenezcan al usuario y no colisionen con las remotas se conservan.
 * 5. Nunca se mezclan publicaciones de otros usuarios.
 */
fun sincronizarPublicacionesUsuario(
    correoUsuario: String,
    mascotasRemotas: List<Pet>,
    publicacionesLocales: List<Pet>
): List<Pet> {
    val correoNorm = normalizarCorreoUsuario(correoUsuario)
    if (correoNorm.isBlank()) {
        return publicacionesLocales
    }

    // 1. Filtrar únicamente las mascotas de Neon que pertenecen al usuario autenticado
    val remotasDelUsuario = mascotasRemotas.filter {
        it.esRemota && normalizarCorreoUsuario(it.creadoPorCorreo) == correoNorm
    }

    val resultado = mutableListOf<Pet>()
    val idsRemotosProcesados = mutableSetOf<Int>()

    // 2. Incorporar las remotas de Neon (prevalecen sobre datos locales)
    remotasDelUsuario.forEach { remota ->
        // Si teníamos una copia local con foto en Bitmap, preservamos el bitmap
        val copiaLocal = publicacionesLocales.find { it.id == remota.id }
        val remotaConFoto = if (copiaLocal?.photoBitmap != null && remota.photoBitmap == null) {
            remota.copy(photoBitmap = copiaLocal.photoBitmap)
        } else {
            remota
        }
        resultado.add(remotaConFoto)
        idsRemotosProcesados.add(remota.id)
    }

    // 3. Conservar publicaciones locales puras (esRemota = false) que pertenezcan explícitamente a este usuario
    // y cuyos IDs no colisionen con las remotas recién sincronizadas
    publicacionesLocales.forEach { local ->
        val perteneceAUsuario = local.creadoPorCorreo.isNotBlank() &&
                normalizarCorreoUsuario(local.creadoPorCorreo) == correoNorm

        val esLocalPura = !local.esRemota

        if (perteneceAUsuario && esLocalPura && !idsRemotosProcesados.contains(local.id)) {
            resultado.add(local)
        }
    }

    return resultado
}

class MascotaStorage(
    private val context: Context
) {

    private val preferencias =
        context.getSharedPreferences(
            "adoptme_mascotas",
            Context.MODE_PRIVATE
        )

    fun guardarMascotas(
        mascotas: List<Pet>
    ) {

        val arreglo = JSONArray()

        mascotas.forEach { mascota ->
            var rutaFoto = obtenerRutaFoto(mascota.id)

            if (mascota.photoBitmap != null) {
                rutaFoto = guardarFoto(
                    mascota.id,
                    mascota.photoBitmap
                )
            }

            val objeto = convertirPetAJson(mascota, rutaFoto)
            arreglo.put(objeto)
        }

        preferencias
            .edit()
            .putString(
                "mascotas_publicadas",
                arreglo.toString()
            )
            .apply()
    }

    fun cargarMascotas(): List<Pet> {

        val mascotas =
            mutableListOf<Pet>()

        val contenido =
            preferencias.getString(
                "mascotas_publicadas",
                null
            ) ?: return emptyList()

        try {

            val arreglo =
                JSONArray(contenido)

            for (i in 0 until arreglo.length()) {

                val objeto =
                    arreglo.getJSONObject(i)

                val rutaFoto =
                    objeto.optString(
                        "photoPath",
                        ""
                    )

                val bitmap =
                    cargarFoto(rutaFoto)

                mascotas.add(parsearJsonAPet(objeto, bitmap))
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }

        return mascotas
    }

    fun eliminarMascota(
        mascota: Pet,
        mascotasRestantes: List<Pet>
    ) {

        // Eliminar la fotografía guardada
        val archivo =
            File(
                context.filesDir,
                "mascota_${mascota.id}.jpg"
            )

        if (archivo.exists()) {
            archivo.delete()
        }

        // Actualizar SharedPreferences
        guardarMascotas(
            mascotasRestantes
        )
    }

    private fun obtenerRutaFoto(
        id: Int
    ): String {

        val archivo =
            File(
                context.filesDir,
                "mascota_$id.jpg"
            )

        return if (archivo.exists()) {
            archivo.absolutePath
        } else {
            ""
        }
    }

    private fun guardarFoto(
        id: Int,
        bitmap: Bitmap
    ): String {

        return try {

            val archivo =
                File(
                    context.filesDir,
                    "mascota_$id.jpg"
                )

            FileOutputStream(archivo).use { salida ->

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    salida
                )
            }

            archivo.absolutePath

        } catch (e: Exception) {

            e.printStackTrace()

            ""
        }
    }

    private fun cargarFoto(
        ruta: String
    ): Bitmap? {

        if (ruta.isBlank()) {
            return null
        }

        val archivo =
            File(ruta)

        if (!archivo.exists()) {
            return null
        }

        return BitmapFactory.decodeFile(
            archivo.absolutePath
        )
    }
}