package com.example.adoptme.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.adoptme.model.Pet
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

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

            var rutaFoto = obtenerRutaFoto(mascota.id)

            if (mascota.photoBitmap != null) {

                rutaFoto = guardarFoto(
                    mascota.id,
                    mascota.photoBitmap
                )
            }

            objeto.put(
                "photoPath",
                rutaFoto
            )

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

                mascotas.add(
                    Pet(
                        id = objeto.getInt("id"),

                        name =
                            objeto.getString("name"),

                        species =
                            objeto.getString("species"),

                        breed =
                            objeto.getString("breed"),

                        age =
                            objeto.getString("age"),

                        gender =
                            objeto.getString("gender"),

                        location =
                            objeto.optString(
                                "location",
                                "Lima"
                            ),

                        status =
                            objeto.optString(
                                "status",
                                "Disponible"
                            ),

                        description =
                            objeto.getString(
                                "description"
                            ),

                        image = 0,

                        photoBitmap = bitmap,

                        creadoPorCorreo =
                            objeto.optString(
                                "creadoPorCorreo",
                                ""
                            )
                    )
                )
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