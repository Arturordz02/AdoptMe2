package com.example.adoptme.screens

import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.components.DialogoExplicacionUbicacion
import com.example.adoptme.components.DialogoSelectorMapa
import com.example.adoptme.components.InsigniaEstado
import com.example.adoptme.location.LocationHelper
import com.example.adoptme.mapper.toPet
import com.example.adoptme.model.MascotaDto
import com.example.adoptme.model.Pet
import com.example.adoptme.utils.formatearEdadCanonica
import com.example.adoptme.utils.ImageProcessor
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.app.ActivityCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext




import android.Manifest

import android.content.pm.PackageManager

import android.graphics.Bitmap

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Image

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.setValue




import kotlinx.coroutines.launch

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import com.example.adoptme.R


import com.example.adoptme.ui.theme.AdoptCardWhite

import com.example.adoptme.ui.theme.AdoptMeBg

import com.example.adoptme.ui.theme.AdoptOrange

import com.example.adoptme.ui.theme.AdoptTeal

import com.example.adoptme.ui.theme.AdoptTealDark

import com.example.adoptme.ui.theme.AdoptTealLight

import com.example.adoptme.ui.theme.AdoptTextDark

import com.example.adoptme.ui.theme.AdoptTextSecondary

import com.example.adoptme.ui.theme.BorderWarm



// =============================================================================

// FUNCIONES Y MODELOS DE VALIDACIÓN PURA (ACCESIBLES PARA TESTS UNITARIOS)

// =============================================================================



/**

 * Valida si una especie es admitida por las reglas de la aplicación (RN-02).

 * Únicamente se aceptan: Perro, Gato y Conejo.

 */

fun esEspecieValida(especie: String): Boolean {

    val limpia = especie.trim().lowercase()

        .replace('á', 'a')

        .replace('é', 'e')

        .replace('í', 'i')

        .replace('ó', 'o')

        .replace('ú', 'u')

    return limpia == "perro" || limpia == "gato" || limpia == "conejo"

}



/**

 * Valida el formato de la edad de la mascota.

 * Acepta formatos como: "1 año", "2 años", "6 meses", "10 meses".

 * Rechaza vacío, texto sin número, números negativos o formatos desconocidos.

 */

fun esFormatoEdadValido(edad: String): Boolean {

    val limpia = edad.trim().lowercase()

        .replace('á', 'a')

        .replace('é', 'e')

        .replace('í', 'i')

        .replace('ó', 'o')

        .replace('ú', 'u')

    if (limpia.isBlank()) return false

    val regex = Regex("""^([1-9]\d?)\s*(ano|anos|anio|anios|año|años|mes|meses)$""")

    return regex.matches(limpia)

}



fun esSexoValido(sexo: String): Boolean {

    val limpia = sexo.trim().lowercase()

    return limpia == "macho" || limpia == "hembra"

}



data class ValidacionPublicacion(

    val esValido: Boolean,

    val errorNombre: String? = null,

    val errorEspecie: String? = null,

    val errorRaza: String? = null,

    val errorEdad: String? = null,

    val errorGenero: String? = null,

    val errorUbicacion: String? = null,

    val errorDescripcion: String? = null

)



/**

 * Valida integralmente los datos del formulario de publicación de mascota.

 */

fun revisarPublicacion(

    nombre: String,

    especie: String,

    raza: String,

    edad: String,

    genero: String,

    ubicacion: String,

    descripcion: String

): ValidacionPublicacion {

    val errorNombre = if (nombre.isBlank()) "El nombre de la mascota es obligatorio." else null

    val errorEspecie = when {

        especie.isBlank() -> "La especie es obligatoria."

        !esEspecieValida(especie) -> "Solo se permiten especies: Perro, Gato o Conejo (RN-02)."

        else -> null

    }

    val errorRaza = if (raza.isBlank()) "La raza es obligatoria." else null

    val errorEdad = when {

        edad.isBlank() -> "La edad es obligatoria."

        !esFormatoEdadValido(edad) -> "Formato inválido. Ejemplo: '2 años' o '6 meses'."

        else -> null

    }

    val errorGenero = when {

        genero.isBlank() -> "El sexo es obligatorio."

        !esSexoValido(genero) -> "Solo se permite 'Macho' o 'Hembra'."

        else -> null

    }

    val errorUbicacion = if (ubicacion.isBlank()) "La ubicación o distrito es obligatorio." else null

    val errorDescripcion = if (descripcion.isBlank()) "La descripción de la mascota es obligatoria." else null



    val esValido = errorNombre == null &&

            errorEspecie == null &&

            errorRaza == null &&

            errorEdad == null &&

            errorGenero == null &&

            errorUbicacion == null &&

            errorDescripcion == null



    return ValidacionPublicacion(

        esValido = esValido,

        errorNombre = errorNombre,

        errorEspecie = errorEspecie,

        errorRaza = errorRaza,

        errorEdad = errorEdad,

        errorGenero = errorGenero,

        errorUbicacion = errorUbicacion,

        errorDescripcion = errorDescripcion

    )

}



fun validarDatosPublicacion(

    nombre: String,

    especie: String,

    raza: String,

    edad: String,

    genero: String,

    ubicacion: String,

    descripcion: String

): ValidacionPublicacion = revisarPublicacion(nombre, especie, raza, edad, genero, ubicacion, descripcion)



/**

 * Construye un objeto Pet respetando estrictamente las propiedades existentes del modelo.

 * Genera un identificador único positivo y asigna el estado inicial "Disponible".

 * Si no existe fotoBitmap, asigna el recurso drawable de apoyo existente.

 */

fun crearMascota(

    nombre: String,

    especie: String,

    raza: String,

    edad: String,

    genero: String,

    ubicacion: String,

    descripcion: String,

    fotoBitmap: Bitmap? = null,

    creadoPorCorreo: String = "",

    latitud: Double? = null,

    longitud: Double? = null

): Pet {

    val idUnico = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()

    val especieNorm = especie.trim().lowercase()

    val especieCap = when {

        especieNorm.startsWith("perr") -> "Perro"

        especieNorm.startsWith("gat") -> "Gato"

        especieNorm.startsWith("conej") -> "Conejo"

        else -> especie.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    }

    val generoCap = if (genero.trim().equals("hembra", ignoreCase = true)) "Hembra" else "Macho"



    val recursoDrawable = when (especieCap) {

        "Gato" -> R.drawable.luna

        "Conejo" -> R.drawable.mia

        else -> R.drawable.max

    }



    return Pet(

        id = idUnico,

        name = nombre.trim(),

        species = especieCap,

        breed = raza.trim(),

        age = formatearEdadCanonica(edad),

        gender = generoCap,

        location = ubicacion.trim(),

        status = "Disponible",

        description = descripcion.trim(),

        image = recursoDrawable,

        photoBitmap = fotoBitmap,

        creadoPorCorreo = creadoPorCorreo.trim(),

        latitud = latitud,

        longitud = longitud

    )

}



fun crearPublicacionMascota(

    nombre: String,

    especie: String,

    raza: String,

    edad: String,

    genero: String,

    ubicacion: String,

    descripcion: String,

    fotoBitmap: Bitmap? = null,

    creadoPorCorreo: String = "",

    latitud: Double? = null,

    longitud: Double? = null

): Pet = crearMascota(nombre, especie, raza, edad, genero, ubicacion, descripcion, fotoBitmap, creadoPorCorreo, latitud, longitud)



// =============================================================================

// COMPOSABLE DEL FORMULARIO DE PUBLICACIÓN

// =============================================================================



@Composable

fun PantallaPublicarMascota(

    publicar: (Pet) -> Unit,

    volver: () -> Unit = {},

    correoActual: String = ""

) {

    val contexto = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var publicando by remember { mutableStateOf(false) }



    var nombre by remember { mutableStateOf("") }

    var especie by remember { mutableStateOf("") }

    var raza by remember { mutableStateOf("") }

    var edad by remember { mutableStateOf("") }

    var genero by remember { mutableStateOf("") }

    var ubicacion by remember { mutableStateOf("Miraflores, Lima") }

    var descripcion by remember { mutableStateOf("") }



    var fotoMascota by remember { mutableStateOf<Bitmap?>(null) }
    var bytesFotoMascota by remember { mutableStateOf<ByteArray?>(null) }
    var infoTamanoFoto by remember { mutableStateOf<String?>(null) }
    var procesandoImagen by remember { mutableStateOf(false) }

    var rutaArchivoTemporalCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var uriTemporalCamaraStr by rememberSaveable { mutableStateOf<String?>(null) }

    var mostrarDialogoPedagogicoGaleria by remember { mutableStateOf(false) }
    var mostrarDialogoPedagogicoCamara by remember { mutableStateOf(false) }
    var mostrarDialogoAjustesCamara by remember { mutableStateOf(false) }

    var errores by remember { mutableStateOf(ValidacionPublicacion(true)) }

    var mensajeGeneralError by remember { mutableStateOf("") }

    // Opciones rápidas de especies permitidas (RN-02)
    val especiesDisponibles = listOf("Perro", "Gato", "Conejo")
    val generosDisponibles = listOf("Macho", "Hembra")

    /*
     * PHOTO PICKER (SELECCIÓN PRIVADA DE GALERÍA)
     */
    val lanzadorGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        Log.d("AdoptMe", "lanzadorGaleria callback uri=$uri")
        if (uri != null) {
            procesandoImagen = true
            mensajeGeneralError = ""
            coroutineScope.launch {
                try {
                    val resultado = ImageProcessor.procesarImagenUri(contexto, uri)
                    fotoMascota = resultado.bitmap
                    bytesFotoMascota = resultado.bytes
                    infoTamanoFoto = resultado.tamanoLegible
                    Log.d("AdoptMe", "lanzadorGaleria éxito: ${resultado.tamanoLegible}")
                } catch (e: Exception) {
                    Log.e("AdoptMe", "lanzadorGaleria error", e)
                    mensajeGeneralError = "No se pudo procesar la fotografía seleccionada: ${e.message}"
                } finally {
                    procesandoImagen = false
                }
            }
        }
    }

    /*
     * CAPTURA DE FOTOGRAFÍA CON CÁMARA (ALTA CALIDAD VÍA FILEPROVIDER)
     */
    val lanzadorCapturaFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { capturaExitosa ->
        Log.d("AdoptMe", "lanzadorCapturaFoto callback capturaExitosa=$capturaExitosa uriStr=$uriTemporalCamaraStr ruta=$rutaArchivoTemporalCamara")
        val uriStr = uriTemporalCamaraStr
        val ruta = rutaArchivoTemporalCamara
        val uri = if (uriStr != null) Uri.parse(uriStr) else null
        val archivo = if (ruta != null) java.io.File(ruta) else null
        if (capturaExitosa && uri != null) {
            procesandoImagen = true
            mensajeGeneralError = ""
            coroutineScope.launch {
                try {
                    val resultado = ImageProcessor.procesarImagenUri(contexto, uri, archivoLocal = archivo)
                    fotoMascota = resultado.bitmap
                    bytesFotoMascota = resultado.bytes
                    infoTamanoFoto = resultado.tamanoLegible
                    Log.d("AdoptMe", "lanzadorCapturaFoto éxito: ${resultado.tamanoLegible}")
                } catch (e: Exception) {
                    Log.e("AdoptMe", "lanzadorCapturaFoto error", e)
                    mensajeGeneralError = "No se pudo procesar la fotografía capturada: ${e.message}"
                } finally {
                    procesandoImagen = false
                    archivo?.delete()
                    rutaArchivoTemporalCamara = null
                    uriTemporalCamaraStr = null
                }
            }
        } else {
            archivo?.delete()
            rutaArchivoTemporalCamara = null
            uriTemporalCamaraStr = null
        }
    }

    fun iniciarCapturaCamara() {
        try {
            val (archivo, uri) = ImageProcessor.crearArchivoTemporalCamara(contexto)
            rutaArchivoTemporalCamara = archivo.absolutePath
            uriTemporalCamaraStr = uri.toString()
            Log.d("AdoptMe", "iniciarCapturaCamara lanzando uri=$uri ruta=${archivo.absolutePath}")
            lanzadorCapturaFoto.launch(uri)
        } catch (e: Exception) {
            mensajeGeneralError = "No se pudo preparar el archivo temporal para la cámara: ${e.message}"
        }
    }

    /*
     * PERMISO DE CÁMARA (GESTIÓN PEDAGÓGICA Y RECHAZO)
     */
    val lanzadorPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { permisoConcedido ->
        if (permisoConcedido) {
            iniciarCapturaCamara()
        } else {
            val actividad = contexto as? android.app.Activity
            val deberiaMostrarRationale = actividad?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false

            if (!deberiaMostrarRationale) {
                mostrarDialogoAjustesCamara = true
            } else {
                mensajeGeneralError = "Debes conceder acceso a la cámara para tomar fotos directamente. También puedes seleccionar una fotografía de tu galería privada."
            }
        }
    }

    /*
     * UBICACIÓN Y GEOLOCALIZACIÓN
     */
    var latitudSeleccionada by remember { mutableStateOf<Double?>(null) }
    var longitudSeleccionada by remember { mutableStateOf<Double?>(null) }
    var mostrarSelectorMapa by remember { mutableStateOf(false) }
    var mostrarExplicacionPermiso by remember { mutableStateOf(false) }
    var accionPendientePorPermiso by remember { mutableStateOf<(() -> Unit)?>(null) }
    var detectandoGpsFormulario by remember { mutableStateOf(false) }

    val lanzadorPermisosUbicacion =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->
            val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (concedido) {
                accionPendientePorPermiso?.invoke()
            } else {
                mensajeGeneralError = "Permiso de ubicación no concedido. Puedes ingresar el distrito o marcar en el mapa manualmente."
            }
            accionPendientePorPermiso = null
        }

    fun ejecutarConPermisoUbicacion(accion: () -> Unit) {
        if (LocationHelper.tienePermisoUbicacion(contexto)) {
            accion()
        } else {
            accionPendientePorPermiso = accion
            mostrarExplicacionPermiso = true
        }
    }



    Column(

        modifier = Modifier

            .fillMaxSize()

            .background(AdoptMeBg)

            .statusBarsPadding()

            .navigationBarsPadding()

            .imePadding()

            .verticalScroll(rememberScrollState())

            .padding(horizontal = 20.dp, vertical = 16.dp)

    ) {

        // Botón superior Volver

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.Start,

            verticalAlignment = Alignment.CenterVertically

        ) {

            TextButton(onClick = volver) {

                Text(

                    text = "← Volver",

                    color = AdoptTeal,

                    fontWeight = FontWeight.Bold,

                    fontSize = 15.sp

                )

            }

        }



        Spacer(modifier = Modifier.height(4.dp))



        // Encabezado

        Text(

            text = "🐾 Publicar mascota",

            style = MaterialTheme.typography.headlineMedium,

            fontWeight = FontWeight.Bold,

            color = AdoptTextDark

        )



        Text(

            text = "Registra una mascota que necesita un nuevo hogar en el Perú.",

            style = MaterialTheme.typography.bodyMedium,

            color = AdoptTextSecondary,

            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)

        )



        // Estado inicial de la publicación

        Card(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(14.dp),

            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),

            border = BorderStroke(1.dp, BorderWarm)

        ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(14.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically

            ) {

                Column {

                    Text(

                        text = "Estado inicial de publicación",

                        style = MaterialTheme.typography.labelMedium,

                        color = AdoptTextSecondary

                    )

                    Text(

                        text = "Aparecerá en el catálogo general",

                        style = MaterialTheme.typography.bodySmall,

                        color = AdoptTealDark,

                        fontWeight = FontWeight.Medium

                    )

                }

                InsigniaEstado(estado = "Disponible")

            }

        }



        Spacer(modifier = Modifier.height(16.dp))



        // Tarjeta de Fotografía (Fase A: Cloudinary + Photo Picker + Cámara)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
            border = BorderStroke(1.dp, BorderWarm),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📷 Foto de la mascota",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark
                )

                Text(
                    text = "Toma una foto con la cámara o selecciona una imagen desde tu galería privada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdoptTextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                if (procesandoImagen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AdoptTeal,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Optimizando fotografía...",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdoptTealDark
                        )
                    }
                }

                if (fotoMascota != null) {
                    Image(
                        bitmap = fotoMascota!!.asImageBitmap(),
                        contentDescription = "Foto de la mascota",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    if (infoTamanoFoto != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tamaño optimizado: $infoTamanoFoto (JPEG máx. 1024×1024)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdoptTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val tienePermiso = ContextCompat.checkSelfPermission(
                                    contexto,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (tienePermiso) {
                                    iniciarCapturaCamara()
                                } else {
                                    mostrarDialogoPedagogicoCamara = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cámara", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { mostrarDialogoPedagogicoGaleria = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Galería", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                fotoMascota = null
                                bytesFotoMascota = null
                                infoTamanoFoto = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                        ) {
                            Text("Quitar", fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val tienePermiso = ContextCompat.checkSelfPermission(
                                    contexto,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (tienePermiso) {
                                    iniciarCapturaCamara()
                                } else {
                                    mostrarDialogoPedagogicoCamara = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_camera),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp).size(18.dp)
                                )
                                Text("Tomar foto", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { mostrarDialogoPedagogicoGaleria = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AdoptTeal)
                        ) {
                            Text("Elegir de galería", color = AdoptTealDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }



        Spacer(modifier = Modifier.height(18.dp))



        // Formulario de datos

        Card(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(16.dp),

            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),

            border = BorderStroke(1.dp, BorderWarm),

            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

        ) {

            Column(modifier = Modifier.padding(18.dp)) {



                // 1. Nombre

                Text("Nombre de la mascota *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(

                    value = nombre,

                    onValueChange = {

                        nombre = it

                        if (errores.errorNombre != null) errores = errores.copy(errorNombre = null)

                    },

                    placeholder = { Text("Ejemplo: Bobby, Michi, Tambor") },

                    singleLine = true,

                    isError = errores.errorNombre != null,

                    shape = RoundedCornerShape(10.dp),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errores.errorNombre != null) {

                    Text(text = errores.errorNombre!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                }



                Spacer(modifier = Modifier.height(14.dp))



                // 2. Especie (RN-02)

                Text("Especie * (RN-02: Perro, Gato o Conejo)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)

                Spacer(modifier = Modifier.height(6.dp))

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    especiesDisponibles.forEach { opcion ->

                        val seleccionada = especie.equals(opcion, ignoreCase = true)

                        Surface(

                            shape = RoundedCornerShape(10.dp),

                            color = if (seleccionada) AdoptOrange else AdoptMeBg,

                            border = BorderStroke(1.dp, if (seleccionada) AdoptOrange else BorderWarm),

                            modifier = Modifier

                                .weight(1f)

                                .clickable {

                                    especie = opcion

                                    if (errores.errorEspecie != null) errores = errores.copy(errorEspecie = null)

                                }

                        ) {

                            Box(

                                contentAlignment = Alignment.Center,

                                modifier = Modifier.padding(vertical = 10.dp)

                            ) {

                                Text(

                                    text = opcion,

                                    color = if (seleccionada) Color.White else AdoptTextDark,

                                    fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,

                                    fontSize = 14.sp

                                )

                            }

                        }

                    }

                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(

                    value = especie,

                    onValueChange = {

                        especie = it

                        if (errores.errorEspecie != null) errores = errores.copy(errorEspecie = null)

                    },

                    placeholder = { Text("O escribe la especie (Perro, Gato o Conejo)") },

                    singleLine = true,

                    isError = errores.errorEspecie != null,

                    shape = RoundedCornerShape(10.dp),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errores.errorEspecie != null) {

                    Text(text = errores.errorEspecie!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                }



                Spacer(modifier = Modifier.height(14.dp))



                // 3. Raza

                Text("Raza *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(

                    value = raza,

                    onValueChange = {

                        raza = it

                        if (errores.errorRaza != null) errores = errores.copy(errorRaza = null)

                    },

                    placeholder = { Text("Ejemplo: Mestizo, Criollo, Siamés") },

                    singleLine = true,

                    isError = errores.errorRaza != null,

                    shape = RoundedCornerShape(10.dp),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errores.errorRaza != null) {

                    Text(text = errores.errorRaza!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                }



                Spacer(modifier = Modifier.height(14.dp))



                // 4. Edad

                Text("Edad *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(

                    value = edad,

                    onValueChange = {

                        edad = it

                        if (errores.errorEdad != null) errores = errores.copy(errorEdad = null)

                    },

                    placeholder = { Text("Ejemplo: 2 años o 6 meses") },

                    singleLine = true,

                    isError = errores.errorEdad != null,

                    shape = RoundedCornerShape(10.dp),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errores.errorEdad != null) {

                    Text(text = errores.errorEdad!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                }



                Spacer(modifier = Modifier.height(14.dp))



                // 5. Sexo / Género

                Text("Sexo *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)

                Spacer(modifier = Modifier.height(6.dp))

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    generosDisponibles.forEach { opcion ->

                        val seleccionado = genero.equals(opcion, ignoreCase = true)

                        Surface(

                            shape = RoundedCornerShape(10.dp),

                            color = if (seleccionado) AdoptTeal else AdoptMeBg,

                            border = BorderStroke(1.dp, if (seleccionado) AdoptTeal else BorderWarm),

                            modifier = Modifier

                                .weight(1f)

                                .clickable {

                                    genero = opcion

                                    if (errores.errorGenero != null) errores = errores.copy(errorGenero = null)

                                }

                        ) {

                            Box(

                                contentAlignment = Alignment.Center,

                                modifier = Modifier.padding(vertical = 10.dp)

                            ) {

                                Text(

                                    text = if (opcion == "Macho") "♂ Macho" else "♀ Hembra",

                                    color = if (seleccionado) Color.White else AdoptTextDark,

                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,

                                    fontSize = 14.sp

                                )

                            }

                        }

                    }

                }

                if (errores.errorGenero != null) {

                    Text(text = errores.errorGenero!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                }



                Spacer(modifier = Modifier.height(14.dp))



                // 6. Ubicación / Distrito

                Text("Ubicación o distrito en Lima/Perú *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(

                    value = ubicacion,

                    onValueChange = {

                        ubicacion = it

                        if (errores.errorUbicacion != null) errores = errores.copy(errorUbicacion = null)

                    },

                    placeholder = { Text("Ejemplo: Miraflores, Lima") },

                    singleLine = true,

                    isError = errores.errorUbicacion != null,

                    shape = RoundedCornerShape(10.dp),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errores.errorUbicacion != null) {

                    Text(text = errores.errorUbicacion!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selector de Ubicación en el Mapa y GPS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AdoptMeBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = AdoptTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Geolocalización en mapa (Opcional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AdoptTextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Permite que los adoptantes encuentren a la mascota según cercanía.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdoptTextSecondary
                        )

                        if (latitudSeleccionada != null && longitudSeleccionada != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AdoptCardWhite,
                                border = BorderStroke(1.dp, AdoptTeal)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "📍 Lat: %.4f, Lon: %.4f", latitudSeleccionada, longitudSeleccionada),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AdoptTealDark
                                    )
                                    TextButton(
                                        onClick = {
                                            latitudSeleccionada = null
                                            longitudSeleccionada = null
                                        }
                                    ) {
                                        Text("Quitar", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    ejecutarConPermisoUbicacion {
                                        if (!LocationHelper.esGpsHabilitado(contexto)) {
                                            mensajeGeneralError = "El GPS está apagado. Puedes activarlo o seleccionar el punto en el mapa."
                                            return@ejecutarConPermisoUbicacion
                                        }
                                        detectandoGpsFormulario = true
                                        mensajeGeneralError = ""
                                        coroutineScope.launch {
                                            val loc = LocationHelper.obtenerUbicacionActual(contexto)
                                            detectandoGpsFormulario = false
                                            if (loc != null) {
                                                latitudSeleccionada = loc.latitude
                                                longitudSeleccionada = loc.longitude
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        if (Geocoder.isPresent()) {
                                                            val geocoder = Geocoder(contexto, Locale.getDefault())
                                                            val lista = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                                            if (!lista.isNullOrEmpty()) {
                                                                val subLoc = lista[0].subLocality
                                                                val ciudad = lista[0].locality
                                                                val dist = listOfNotNull(subLoc, ciudad).filter { it.isNotBlank() }.distinct().joinToString(", ")
                                                                if (dist.isNotBlank()) {
                                                                    ubicacion = dist
                                                                }
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                            } else {
                                                mensajeGeneralError = "No se pudo obtener la posición GPS. Prueba con 'Ver en mapa'."
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !detectandoGpsFormulario
                            ) {
                                if (detectandoGpsFormulario) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AdoptTeal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GPS...", fontSize = 12.sp)
                                } else {
                                    Text("Usar mi GPS", fontSize = 12.sp, color = AdoptTealDark, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = { mostrarSelectorMapa = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                            ) {
                                Text("Ver en mapa", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))



                // 7. Descripción

                Text("Descripción de la mascota *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(

                    value = descripcion,

                    onValueChange = {

                        descripcion = it

                        if (errores.errorDescripcion != null) errores = errores.copy(errorDescripcion = null)

                    },

                    placeholder = { Text("Cuéntanos sobre su personalidad, temperamento y cuidados necesarios...") },

                    minLines = 3,

                    isError = errores.errorDescripcion != null,

                    shape = RoundedCornerShape(10.dp),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errores.errorDescripcion != null) {

                    Text(text = errores.errorDescripcion!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                }

            }

        }



        if (mensajeGeneralError.isNotEmpty()) {

            Spacer(modifier = Modifier.height(12.dp))

            Card(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(10.dp),

                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)

            ) {

                Text(

                    text = "⚠️ $mensajeGeneralError",

                    color = MaterialTheme.colorScheme.onErrorContainer,

                    style = MaterialTheme.typography.bodyMedium,

                    modifier = Modifier.padding(12.dp)

                )

            }

        }



        Spacer(modifier = Modifier.height(20.dp))



        // Botón para publicar con estados de carga y llamada a Retrofit

        Button(

            onClick = {

                if (publicando) return@Button



                val resultadoValidacion = revisarPublicacion(

                    nombre = nombre,

                    especie = especie,

                    raza = raza,

                    edad = edad,

                    genero = genero,

                    ubicacion = ubicacion,

                    descripcion = descripcion

                )



                errores = resultadoValidacion



                if (resultadoValidacion.esValido) {
                    val edadCanonica = formatearEdadCanonica(edad)
                    edad = edadCanonica
                    publicando = true
                    mensajeGeneralError = ""

                    coroutineScope.launch {
                        try {
                            val respuesta = if (bytesFotoMascota != null && bytesFotoMascota!!.isNotEmpty()) {
                                // Publicación autenticada mediante multipart/form-data con imagen (Fase A)
                                val reqName = nombre.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqSpecies = especie.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqBreed = raza.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqAge = edadCanonica.toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqGender = genero.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqLocation = ubicacion.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqDesc = descripcion.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqStatus = "Disponible".toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqLat = latitudSeleccionada?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqLon = longitudSeleccionada?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                                val reqImg = bytesFotoMascota!!.toRequestBody("image/jpeg".toMediaTypeOrNull())
                                val partImg = MultipartBody.Part.createFormData("Imagen", "mascota_${System.currentTimeMillis()}.jpg", reqImg)

                                RetrofitClient.mascotasApi.publicarMascotaConImagen(
                                    name = reqName,
                                    species = reqSpecies,
                                    breed = reqBreed,
                                    age = reqAge,
                                    gender = reqGender,
                                    location = reqLocation,
                                    description = reqDesc,
                                    status = reqStatus,
                                    latitud = reqLat,
                                    longitud = reqLon,
                                    imagen = partImg
                                )
                            } else {
                                // Publicación tradicional JSON sin imagen
                                val dto = MascotaDto(
                                    id = 0,
                                    name = nombre.trim(),
                                    species = especie.trim(),
                                    breed = raza.trim(),
                                    age = edadCanonica,
                                    gender = genero.trim(),
                                    location = ubicacion.trim(),
                                    status = "Disponible",
                                    description = descripcion.trim(),
                                    imageUrl = null,
                                    creadoPorCorreo = correoActual,
                                    latitud = latitudSeleccionada,
                                    longitud = longitudSeleccionada
                                )
                                RetrofitClient.mascotasApi.publicarMascota(dto)
                            }

                            if (respuesta.isSuccessful) {
                                val dtoGuardado = respuesta.body()
                                if (dtoGuardado != null && dtoGuardado.id > 0) {
                                    val nuevaMascota = dtoGuardado.toPet().let {
                                        if (fotoMascota != null && it.photoBitmap == null) it.copy(photoBitmap = fotoMascota) else it
                                    }
                                    publicar(nuevaMascota)
                                } else {
                                    mensajeGeneralError = "El servidor confirmó la operación pero no devolvió datos válidos de la mascota."
                                }
                            } else when (respuesta.code()) {
                                400 -> {
                                    val errorBody = respuesta.errorBody()?.string() ?: ""
                                    mensajeGeneralError = if (errorBody.contains("mensaje")) {
                                        try { org.json.JSONObject(errorBody).optString("mensaje", "Datos o imagen inválidos.") } catch (_: Exception) { "Datos o fotografía inválidos." }
                                    } else {
                                        "Datos o fotografía inválidos."
                                    }
                                }
                                401 -> mensajeGeneralError = "Sesión no autorizada o expirada. Por favor vuelve a iniciar sesión."
                                413 -> mensajeGeneralError = "La imagen supera el tamaño máximo permitido por el servidor (2 MB)."
                                503 -> mensajeGeneralError = "El servicio de almacenamiento de imágenes en la nube (Cloudinary) no está configurado en el servidor. Puedes publicar sin fotografía."
                                else -> mensajeGeneralError = "No se pudo registrar la mascota en el servidor. Código: ${respuesta.code()}"
                            }
                        } catch (e: Exception) {
                            mensajeGeneralError = "No se pudo conectar con el servidor. Revisa tu conexión a Internet e inténtalo nuevamente."
                        } finally {
                            publicando = false
                        }
                    }
                } else {
                    mensajeGeneralError = "Por favor corrige los campos indicados antes de publicar."
                }
            },
            enabled = !publicando && !procesandoImagen,

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(12.dp),

            colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange)

        ) {

            Row(

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.Center

            ) {

                if (publicando) {

                    CircularProgressIndicator(

                        color = Color.White,

                        modifier = Modifier.size(20.dp),

                        strokeWidth = 2.dp

                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(

                        text = "Publicando...",

                        fontWeight = FontWeight.Bold,

                        fontSize = 15.sp,

                        color = Color.White,

                        modifier = Modifier.padding(vertical = 4.dp)

                    )

                } else {

                    Icon(

                        painter = painterResource(id = R.drawable.ic_paw),

                        contentDescription = null,

                        modifier = Modifier.padding(end = 8.dp).size(20.dp)

                    )

                    Text(

                        text = "Publicar mascota",

                        fontWeight = FontWeight.Bold,

                        fontSize = 16.sp,

                        modifier = Modifier.padding(vertical = 4.dp)

                    )

                }

            }

        }



        Spacer(modifier = Modifier.height(10.dp))



        OutlinedButton(

            onClick = volver,

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(12.dp)

        ) {

            Text(

                text = "Cancelar",

                fontWeight = FontWeight.SemiBold

            )

        }



        Spacer(modifier = Modifier.height(30.dp))

    }

    if (mostrarExplicacionPermiso) {
        DialogoExplicacionUbicacion(
            alAceptar = {
                mostrarExplicacionPermiso = false
                lanzadorPermisosUbicacion.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            alRechazar = {
                mostrarExplicacionPermiso = false
                accionPendientePorPermiso = null
            }
        )
    }

    if (mostrarSelectorMapa) {
        DialogoSelectorMapa(
            latitudInicial = latitudSeleccionada,
            longitudInicial = longitudSeleccionada,
            alConfirmar = { lat, lon, distritoSugerido ->
                latitudSeleccionada = lat
                longitudSeleccionada = lon
                if (!distritoSugerido.isNullOrBlank() && (ubicacion.isBlank() || ubicacion == "Miraflores, Lima")) {
                    ubicacion = distritoSugerido
                }
                mostrarSelectorMapa = false
            },
            alDescartar = { mostrarSelectorMapa = false }
        )
    }

    if (mostrarDialogoPedagogicoGaleria) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPedagogicoGaleria = false },
            title = {
                Text("Seleccionar una fotografía", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Podrás elegir una sola imagen mediante el selector privado de Android. AdoptMe solo tendrá acceso a la fotografía que selecciones y no a toda tu galería.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoPedagogicoGaleria = false
                        lanzadorGaleria.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Elegir fotografía")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPedagogicoGaleria = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoPedagogicoCamara) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPedagogicoCamara = false },
            title = {
                Text("Permiso para tomar fotografías", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("AdoptMe necesita acceso a la cámara únicamente cuando decidas fotografiar una mascota. No se capturan imágenes ni video en segundo plano.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoPedagogicoCamara = false
                        val tienePermiso = ContextCompat.checkSelfPermission(
                            contexto,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (tienePermiso) {
                            iniciarCapturaCamara()
                        } else {
                            lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPedagogicoCamara = false }) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (mostrarDialogoAjustesCamara) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAjustesCamara = false },
            title = {
                Text("Acceso a la cámara bloqueado", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("El acceso a la cámara fue bloqueado permanentemente en el sistema. Puedes activarlo manualmente en los Ajustes de la aplicación, o elegir una imagen desde tu galería.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoAjustesCamara = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${contexto.packageName}")
                        )
                        contexto.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Abrir ajustes")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoAjustesCamara = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}