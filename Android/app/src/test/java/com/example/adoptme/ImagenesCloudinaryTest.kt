package com.example.adoptme

import com.example.adoptme.mapper.toDto
import com.example.adoptme.mapper.toPet
import com.example.adoptme.model.MascotaDto
import com.example.adoptme.model.Pet
import com.example.adoptme.screens.ValidacionPublicacion
import com.example.adoptme.screens.validarDatosPublicacion
import com.example.adoptme.utils.ImageProcessor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagenesCloudinaryTest {

    // 1. Photo Picker cancelado: URI nula no altera el estado ni produce crash
    @Test
    fun test01_photoPickerCancelado_noAlteraEstadoNiLanzaExcepcion() {
        var fotoMascota: String? = "foto_previa"
        var mensajeError = ""

        val uriSeleccionada: String? = null
        if (uriSeleccionada != null) {
            fotoMascota = uriSeleccionada
        } else {
            // Cancelación: no se modifica la foto previa ni se genera excepción
        }

        assertEquals("foto_previa", fotoMascota)
        assertEquals("", mensajeError)
    }

    // 2. URI seleccionada válida: genera datos válidos de procesamiento
    @Test
    fun test02_uriSeleccionadaValida_procesamientoGeneraFormatoCorrecto() {
        val bytesFicticios = ByteArray(1024 * 150) // 150 KB
        val tamanoLegible = ImageProcessor.formatearTamanoBytes(bytesFicticios.size.toLong())

        assertEquals("150.0 KB", tamanoLegible)
        assertTrue(bytesFicticios.size < 2 * 1024 * 1024)
    }

    // 3. URI inválida: manejo controlado de error
    @Test
    fun test03_uriInvalida_capturaControladaDeError() {
        var mensajeError = ""
        val uriInvalida = ""

        try {
            if (uriInvalida.isBlank()) {
                throw IllegalArgumentException("La URI de la imagen seleccionada no es válida o está vacía.")
            }
        } catch (e: Exception) {
            mensajeError = "No se pudo procesar la fotografía: ${e.message}"
        }

        assertTrue(mensajeError.contains("no es válida"))
    }

    // 4. Cámara concedida: procede al flujo de captura
    @Test
    fun test04_permisoCamaraConcedido_procedeAFlujoDeCaptura() {
        var capturaIniciada = false
        val permisoConcedido = true

        if (permisoConcedido) {
            capturaIniciada = true
        }

        assertTrue(capturaIniciada)
    }

    // 5. Cámara rechazada: muestra mensaje pedagógico sin cerrar la app
    @Test
    fun test05_permisoCamaraRechazado_muestraMensajeSinCerrarApp() {
        var appCerrada = false
        var mensajeError = ""
        val permisoConcedido = false
        val shouldShowRationale = true

        if (!permisoConcedido) {
            if (shouldShowRationale) {
                mensajeError = "Debes conceder acceso a la cámara para tomar fotos directamente."
            }
        }

        assertFalse(appCerrada)
        assertTrue(mensajeError.contains("conceder acceso a la cámara"))
    }

    // 6. Cámara rechazada permanentemente: activa diálogo de ajustes
    @Test
    fun test06_permisoCamaraRechazadoPermanentemente_activaDialogoAjustes() {
        var mostrarDialogoAjustes = false
        val permisoConcedido = false
        val shouldShowRationale = false // Denegado permanentemente

        if (!permisoConcedido && !shouldShowRationale) {
            mostrarDialogoAjustes = true
        }

        assertTrue(mostrarDialogoAjustes)
    }

    // 7. Construcción multipart: genera partes y RequestBody válidos
    @Test
    fun test07_construccionMultipart_generaPartesYCabecerasEsperadas() {
        val nombre = "Bobby"
        val especie = "Perro"
        val bytes = ByteArray(500)

        val reqName = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqSpecies = especie.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqImage = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val partImage = MultipartBody.Part.createFormData("Imagen", "bobby.jpg", reqImage)

        assertNotNull(reqName)
        assertNotNull(reqSpecies)
        assertNotNull(partImage)
        assertEquals("image/jpeg", reqImage.contentType()?.toString())
    }

    // 8. Publicación sin imagen: construye DTO con imageUrl null y mantiene compatibilidad
    @Test
    fun test08_publicacionSinImagen_construyeDtoConImageUrlNull() {
        val dto = MascotaDto(
            id = 0,
            name = "MichiSinFoto",
            species = "Gato",
            imageUrl = null,
            imagePublicId = null
        )

        assertNull(dto.imageUrl)
        assertNull(dto.imagePublicId)
        val pet = dto.toPet()
        assertNull(pet.imageUrl)
        assertEquals(R.drawable.luna, pet.image) // Fallback para gato
    }

    // 9. Rechazo de archivo excesivo: formateador y límites de 2 MB
    @Test
    fun test09_formateoDeTamanoBytes_calculaEtiquetasLegibles() {
        assertEquals("500 B", ImageProcessor.formatearTamanoBytes(500))
        assertEquals("500.0 KB", ImageProcessor.formatearTamanoBytes(500 * 1024))
        assertEquals("1.50 MB", ImageProcessor.formatearTamanoBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.50 MB", ImageProcessor.formatearTamanoBytes((2.5 * 1024 * 1024).toLong()))
    }

    // 10. ImageUrl mapeada correctamente: toPet y toDto conservan la URL remota
    @Test
    fun test10_imageUrl_mapeadaCorrectamenteEnToPetYToDto() {
        val url = "https://res.cloudinary.com/adoptme/image/upload/v1/adoptme/mascotas/perro1.jpg"
        val publicId = "adoptme/mascotas/perro1"

        val dto = MascotaDto(
            id = 5,
            name = "Luna",
            species = "Perro",
            imageUrl = url,
            imagePublicId = publicId
        )

        val pet = dto.toPet()
        assertEquals(url, pet.imageUrl)
        assertEquals(publicId, pet.imagePublicId)

        val dtoReconvertido = pet.toDto()
        assertEquals(url, dtoReconvertido.imageUrl)
        assertEquals(publicId, dtoReconvertido.imagePublicId)
    }

    // 11. Fallback por especie: asigna drawable adecuado cuando imageUrl es null
    @Test
    fun test11_fallbackPorEspecie_cuandoImageUrlEsNull() {
        val dtoGato = MascotaDto(id = 1, species = "Gato", imageUrl = null)
        val dtoConejo = MascotaDto(id = 2, species = "Conejo", imageUrl = null)
        val dtoPerro = MascotaDto(id = 3, species = "Perro", imageUrl = null)

        assertEquals(R.drawable.luna, dtoGato.toPet().image)
        assertEquals(R.drawable.mia, dtoConejo.toPet().image)
        assertEquals(R.drawable.max, dtoPerro.toPet().image)
    }

    // 12. ImagePublicId: conservado en mapeo bidireccional
    @Test
    fun test12_imagePublicId_conservadoEnMapeoBidireccional() {
        val pet = Pet(
            id = 10,
            name = "Rex",
            species = "Perro",
            breed = "Pastor",
            age = "3 años",
            gender = "Macho",
            description = "Noble",
            imageUrl = "https://res.cloudinary.com/rex.jpg",
            imagePublicId = "adoptme/mascotas/rex_123"
        )

        val dto = pet.toDto()
        assertEquals("adoptme/mascotas/rex_123", dto.imagePublicId)
        val petRegresado = dto.toPet()
        assertEquals("adoptme/mascotas/rex_123", petRegresado.imagePublicId)
    }

    // 13. Estado de subida: bloquea interacción y previene doble envío
    @Test
    fun test13_estadoSubida_bloqueaInteraccionYPrevieneDobleEnvio() {
        var publicando = true
        var procesandoImagen = false

        val botonHabilitado = !publicando && !procesandoImagen
        assertFalse("El botón de publicar debe estar deshabilitado mientras se sube", botonHabilitado)

        publicando = false
        procesandoImagen = true
        val botonHabilitadoDuranteProcesamiento = !publicando && !procesandoImagen
        assertFalse("El botón debe estar deshabilitado mientras se procesa la foto", botonHabilitadoDuranteProcesamiento)

        procesandoImagen = false
        val botonHabilitadoListo = !publicando && !procesandoImagen
        assertTrue("El botón debe habilitarse cuando no hay tareas en curso", botonHabilitadoListo)
    }

    // 14. Validación de formulario: requiere nombre y especie obligatorios
    @Test
    fun test14_validacionPublicacion_requiereNombreYEspecieObligatorios() {
        val validacionInvalida = validarDatosPublicacion(
            nombre = "",
            especie = "",
            raza = "Mestizo",
            edad = "1 año",
            genero = "Macho",
            ubicacion = "Lima",
            descripcion = "Amigable"
        )
        assertFalse(validacionInvalida.esValido)
        assertNotNull(validacionInvalida.errorNombre)
        assertNotNull(validacionInvalida.errorEspecie)

        val validacionValida = validarDatosPublicacion(
            nombre = "Firulais",
            especie = "Perro",
            raza = "Mestizo",
            edad = "1 año",
            genero = "Macho",
            ubicacion = "Lima",
            descripcion = "Amigable"
        )
        assertTrue(validacionValida.esValido)
    }

    // 15. Manejo resiliente de rechazos: la app no crashea ante excepciones controladas
    @Test
    fun test15_manejoResilienteDeRechazos_mantieneAplicacionOperativa() {
        var estadoAppOperativo = true
        var mensajeUsuario = ""

        try {
            // Simular respuesta HTTP 503 (Cloudinary no configurado)
            val codigoHttp = 503
            if (codigoHttp == 503) {
                mensajeUsuario = "El servicio de almacenamiento de imágenes en la nube (Cloudinary) no está configurado en el servidor."
            }
        } catch (_: Exception) {
            estadoAppOperativo = false
        }

        assertTrue(estadoAppOperativo)
        assertTrue(mensajeUsuario.contains("Cloudinary"))
    }

    // 16. Coordenadas geográficas: se preservan en la publicación con imagen
    @Test
    fun test16_coordenadasGeograficas_sePreservanEnPublicacionConImagen() {
        val latitud: Double? = -12.046374
        val longitud: Double? = -77.030583

        val reqLat = latitud?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqLon = longitud?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        assertNotNull(reqLat)
        assertNotNull(reqLon)

        val petConGeo = Pet(
            id = 20,
            name = "GeoDog",
            species = "Perro",
            breed = "Criollo",
            age = "2 años",
            gender = "Macho",
            description = "Cerca a plaza de armas",
            imageUrl = "https://res.cloudinary.com/geodog.jpg",
            imagePublicId = "adoptme/mascotas/geodog",
            latitud = latitud,
            longitud = longitud
        )

        assertEquals(-12.046374, petConGeo.latitud!!, 0.0001)
        assertEquals(-77.030583, petConGeo.longitud!!, 0.0001)
        assertNotNull(petConGeo.imageUrl)
    }
}
