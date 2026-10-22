package com.example.adoptme.AdoptMe

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.example.adoptme.data.remote.MascotaDto
import com.example.adoptme.data.remote.RetrofitClient
import com.example.adoptme.data.remote.toPet
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
import com.example.adoptme.model.Pet
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
    val regex = Regex("""^([1-9]\d?)\s*(ano|anos|anios|año|años|mes|meses)$""")
    return regex.matches(limpia)
}

/**
 * Valida el sexo de la mascota ("Macho" o "Hembra").
 */
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
    creadoPorCorreo: String = ""
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
        age = edad.trim(),
        gender = generoCap,
        location = ubicacion.trim(),
        status = "Disponible",
        description = descripcion.trim(),
        image = recursoDrawable,
        photoBitmap = fotoBitmap,
        creadoPorCorreo = creadoPorCorreo.trim()
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
    creadoPorCorreo: String = ""
): Pet = crearMascota(nombre, especie, raza, edad, genero, ubicacion, descripcion, fotoBitmap, creadoPorCorreo)

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
    var errores by remember { mutableStateOf(ValidacionPublicacion(true)) }
    var mensajeGeneralError by remember { mutableStateOf("") }

    // Opciones rápidas de especies permitidas (RN-02)
    val especiesDisponibles = listOf("Perro", "Gato", "Conejo")
    val generosDisponibles = listOf("Macho", "Hembra")

    /*
     * CÁMARA
     */
    val lanzadorCamara =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                fotoMascota = bitmap
                mensajeGeneralError = ""
            } else {
                mensajeGeneralError = "No se pudo obtener la fotografía."
            }
        }

    /*
     * PERMISO DE CÁMARA
     */
    val lanzadorPermisoCamara =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { permisoConcedido ->
            if (permisoConcedido) {
                lanzadorCamara.launch(null)
            } else {
                mensajeGeneralError = "Debes permitir el acceso a la cámara para tomar una foto."
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdoptMeBg)
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

        // Tarjeta de Fotografía
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
                    text = "Toma una foto con la cámara o se asignará una ilustración de apoyo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdoptTextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

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
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        val tienePermiso = ContextCompat.checkSelfPermission(
                            contexto,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (tienePermiso) {
                            lanzadorCamara.launch(null)
                        } else {
                            lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp).size(20.dp)
                        )
                        Text(
                            if (fotoMascota == null) "ABRIR CÁMARA" else "TOMAR OTRA FOTO",
                            fontWeight = FontWeight.Bold
                        )
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
                    publicando = true
                    mensajeGeneralError = ""

                    coroutineScope.launch {
                        try {
                            val dto = MascotaDto(
                                id = 0,
                                name = nombre.trim(),
                                species = especie.trim(),
                                breed = raza.trim(),
                                age = edad.trim(),
                                gender = genero.trim(),
                                location = ubicacion.trim(),
                                status = "Disponible",
                                description = descripcion.trim(),
                                imageUrl = null,
                                creadoPorCorreo = correoActual
                            )

                            val respuesta = RetrofitClient.mascotasApi.publicarMascota(dto)

                            if (respuesta.isSuccessful) {
                                val dtoGuardado = respuesta.body()
                                if (dtoGuardado != null && dtoGuardado.id > 0) {
                                    val nuevaMascota = dtoGuardado.toPet().let {
                                        if (fotoMascota != null) it.copy(photoBitmap = fotoMascota) else it
                                    }
                                    publicar(nuevaMascota)
                                } else {
                                    mensajeGeneralError = "El servidor confirmó la operación pero no devolvió datos válidos de la mascota."
                                }
                            } else if (respuesta.code() == 401) {
                                mensajeGeneralError = "Sesión no autorizada o expirada. Por favor vuelve a iniciar sesión."
                            } else {
                                mensajeGeneralError = "No se pudo registrar la mascota en el servidor. Código: ${respuesta.code()}"
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
            enabled = !publicando,
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
                        text = "PUBLICANDO EN SERVIDOR...",
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
                        text = "PUBLICAR MASCOTA",
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
                text = "CANCELAR",
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}