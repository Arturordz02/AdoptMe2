package com.example.adoptme.AdoptMe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.adoptme.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptOrangeDark
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealDark
import com.example.adoptme.ui.theme.AdoptTealLight
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary
import com.example.adoptme.ui.theme.BorderWarm
import com.example.adoptme.ui.theme.StatusOrangeBg
import com.example.adoptme.ui.theme.StatusOrangeText

// =============================================================================
// MODELOS Y LÓGICA PURA DE COMPATIBILIDAD
// =============================================================================

enum class PasoTest {
    PREGUNTAS,
    RESULTADOS
}

data class RespuestasTestCompatibilidad(
    val vivienda: String = "",
    val tiempoDisponible: String = "",
    val experiencia: String = "",
    val especiePreferida: String = ""
)

data class CoincidenciaMascota(
    val mascota: Pet,
    val porcentajeCoincidencia: Int,
    val explicacion: String = ""
)

/**
 * Normaliza textos para evitar inconsistencias de mayúsculas, minúsculas y tildes.
 */
fun limpiarTexto(texto: String): String {
    return texto.lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .trim()
}

/**
 * Valida si se puede avanzar de pregunta en el cuestionario.
 */
fun respuestaValida(respuestaSeleccionada: String): Boolean {
    return respuestaSeleccionada.isNotBlank()
}

/**
 * Determina si una mascota permite iniciar una nueva solicitud de adopción (RN-03).
 */
fun puedeAdoptar(mascota: Pet, esPublicacionPropia: Boolean = false): Boolean {
    if (esPublicacionPropia) return false
    return limpiarTexto(mascota.status) == "disponible"
}

/**
 * Reinicia las respuestas del cuestionario a sus valores en blanco.
 */
fun reiniciarTest(): RespuestasTestCompatibilidad {
    return RespuestasTestCompatibilidad()
}

/**
 * Evalúa y calcula la compatibilidad de una mascota usando exclusivamente los campos
 * disponibles en Pet.kt (species, status, description, location, etc.).
 */
fun puntuarMascota(
    mascota: Pet,
    respuestas: RespuestasTestCompatibilidad
): CoincidenciaMascota {
    val especieNorm = limpiarTexto(mascota.species)
    val descNorm = limpiarTexto(mascota.description)
    val prefEspecie = limpiarTexto(respuestas.especiePreferida)
    val vivNorm = limpiarTexto(respuestas.vivienda)
    val tiempoNorm = limpiarTexto(respuestas.tiempoDisponible)
    val expNorm = limpiarTexto(respuestas.experiencia)

    var puntos = 10
    val motivos = mutableListOf<String>()

    // 1. Preferencia de especie (peso principal)
    if (prefEspecie.isNotEmpty() && !prefEspecie.contains("preferencia") && !prefEspecie.contains("cualquiera")) {
        if (especieNorm.contains(prefEspecie) || prefEspecie.contains(especieNorm)) {
            puntos += 45
            motivos.add("Coincide con tu preferencia de especie (${mascota.species}).")
        } else {
            puntos += 5
        }
    } else {
        puntos += 35
        motivos.add("Buena alternativa para adoptantes con apertura a distintas especies.")
    }

    // 2. Vivienda
    if (vivNorm.contains("departamento") || vivNorm.contains("piso")) {
        if (especieNorm.contains("gato") || especieNorm.contains("conejo")) {
            puntos += 20
            motivos.add("Es una buena opción para el tipo de vivienda seleccionado.")
        } else {
            puntos += 10
        }
    } else if (vivNorm.contains("patio") || vivNorm.contains("campo") || vivNorm.contains("finca")) {
        if (especieNorm.contains("perro")) {
            puntos += 20
            motivos.add("Es una buena opción para el tipo de vivienda seleccionado.")
        } else {
            puntos += 15
        }
    } else {
        puntos += 10
    }

    // 3. Tiempo disponible diario
    if (tiempoNorm.contains("menos de 1")) {
        if (especieNorm.contains("gato") || especieNorm.contains("conejo")) {
            puntos += 15
            motivos.add("Su nivel de actividad puede adaptarse a tu disponibilidad.")
        } else {
            puntos += 5
        }
    } else {
        puntos += 15
        motivos.add("Su nivel de actividad puede adaptarse a tu disponibilidad.")
    }

    // 4. Experiencia
    if (expNorm.contains("primera") || expNorm.contains("principiante")) {
        if (descNorm.contains("tranquil") || descNorm.contains("docil") || descNorm.contains("carinos") || descNorm.contains("amigable") || descNorm.contains("sociable")) {
            puntos += 10
            motivos.add("Carácter sociable y dócil, adecuado para primeros adoptantes.")
        } else {
            puntos += 5
        }
    } else {
        puntos += 10
    }

    val porcentaje = puntos.coerceIn(20, 98)
    val explicacionFinal = if (motivos.isNotEmpty()) {
        motivos.first()
    } else {
        "La compatibilidad es aproximada basada en la especie y vivienda seleccionada."
    }

    return CoincidenciaMascota(
        mascota = mascota,
        porcentajeCoincidencia = porcentaje,
        explicacion = explicacionFinal
    )
}

/**
 * Evalúa las mascotas candidatas del catálogo:
 * - Filtra por especies permitidas (Perro, Gato, Conejo).
 * - Excluye mascotas Adoptadas de las recomendaciones activas de adopción.
 * - Ordena de mayor a menor porcentaje.
 */
fun buscarCoincidencias(
    mascotas: List<Pet>,
    respuestas: RespuestasTestCompatibilidad
): List<CoincidenciaMascota> {
    val candidatos = mascotas.filter { mascota ->
        val esp = limpiarTexto(mascota.species)
        val est = limpiarTexto(mascota.status)
        val esEspecieValida = esp.contains("perro") || esp.contains("gato") || esp.contains("conejo")
        val noEstaAdoptada = !est.contains("adoptad")
        esEspecieValida && noEstaAdoptada
    }

    return candidatos.map { puntuarMascota(it, respuestas) }
        .sortedByDescending { it.porcentajeCoincidencia }
}

// =============================================================================
// ALIASES PARA COMPATIBILIDAD CON PRUEBAS UNITARIAS
// =============================================================================
fun puedeAvanzarPregunta(respuesta: String): Boolean = respuestaValida(respuesta)
fun normalizarTextoCompatibilidad(texto: String): String = limpiarTexto(texto)
fun reiniciarRespuestasTest(): RespuestasTestCompatibilidad = reiniciarTest()
fun evaluarTestCompatibilidad(mascotas: List<Pet>, respuestas: RespuestasTestCompatibilidad): List<CoincidenciaMascota> = buscarCoincidencias(mascotas, respuestas)
fun permiteSolicitarAdopcion(mascota: Pet, esPublicacionPropia: Boolean = false): Boolean = puedeAdoptar(mascota, esPublicacionPropia)

// =============================================================================
// COMPOSABLE DEL DIÁLOGO
// =============================================================================

@Composable
fun DialogoTestCompatibilidad(
    mascotas: List<Pet>,
    cerrar: () -> Unit,
    seleccionarMascota: (Pet) -> Unit,
    correoActual: String = "",
    solicitarAdopcion: ((Pet) -> Unit)? = null
) {
    var paso by remember { mutableStateOf(PasoTest.PREGUNTAS) }
    var indicePregunta by remember { mutableStateOf(0) }
    var mensajeError by remember { mutableStateOf("") }

    // Respuestas seleccionadas
    var vivienda by remember { mutableStateOf("") }
    var tiempo by remember { mutableStateOf("") }
    var experiencia by remember { mutableStateOf("") }
    var especiePreferida by remember { mutableStateOf("") }

    val totalPreguntas = 4

    val respuestasActuales = remember(vivienda, tiempo, experiencia, especiePreferida) {
        RespuestasTestCompatibilidad(
            vivienda = vivienda,
            tiempoDisponible = tiempo,
            experiencia = experiencia,
            especiePreferida = especiePreferida
        )
    }

    val mascotasCoincidentes = remember(respuestasActuales, mascotas, paso) {
        if (paso == PasoTest.RESULTADOS) {
            buscarCoincidencias(mascotas, respuestasActuales)
        } else {
            emptyList()
        }
    }

    Dialog(
        onDismissRequest = cerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptMeBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // =============================================================
                // CABECERA NARANJA VIBRANTE
                // =============================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(AdoptOrangeDark, AdoptOrange)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✨", fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Test de compatibilidad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (paso == PasoTest.PREGUNTAS) "Descubre qué mascota se adapta mejor a tu hogar"
                                else "Resultados calculados según tus respuestas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = cerrar,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // =============================================================
                // CUERPO: PREGUNTAS O RESULTADOS
                // =============================================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (paso == PasoTest.PREGUNTAS) {
                        // -----------------------------------------------------
                        // INDICADOR DE PROGRESO
                        // -----------------------------------------------------
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pregunta ${indicePregunta + 1} de $totalPreguntas",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = AdoptTealDark
                            )
                            Text(
                                text = "${((indicePregunta + 1) * 100) / totalPreguntas}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AdoptTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (indicePregunta + 1) / totalPreguntas.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AdoptOrange,
                            trackColor = BorderWarm
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // -----------------------------------------------------
                        // PREGUNTAS PASO A PASO
                        // -----------------------------------------------------
                        when (indicePregunta) {
                            0 -> {
                                TituloPregunta(icono = "🏠", titulo = "1. ¿Cómo es tu vivienda?")
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        "Departamento / Piso" to "Ideal para mascotas medianas o tranquilas.",
                                        "Casa con patio / jardín" to "Espacio exterior para juegos diarios.",
                                        "Casa de campo / Finca" to "Amplio terreno para libre esparcimiento."
                                    ).forEach { (opcion, desc) ->
                                        TarjetaOpcionVertical(
                                            titulo = opcion,
                                            subtitulo = desc,
                                            estaSeleccionado = vivienda == opcion,
                                            seleccionar = {
                                                vivienda = opcion
                                                mensajeError = ""
                                            }
                                        )
                                    }
                                }
                            }

                            1 -> {
                                TituloPregunta(icono = "⏰", titulo = "2. ¿Cuánto tiempo diario tienes para la mascota?")
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        "Menos de 1 hora" to "Atención esencial y cuidados básicos.",
                                        "1 a 3 horas" to "Tiempo suficiente para paseos y convivencia.",
                                        "Más de 3 horas" to "Gran dedicación para juegos y compañía constante."
                                    ).forEach { (opcion, desc) ->
                                        TarjetaOpcionVertical(
                                            titulo = opcion,
                                            subtitulo = desc,
                                            estaSeleccionado = tiempo == opcion,
                                            seleccionar = {
                                                tiempo = opcion
                                                mensajeError = ""
                                            }
                                        )
                                    }
                                }
                            }

                            2 -> {
                                TituloPregunta(icono = "🐾", titulo = "3. ¿Cuál es tu experiencia con animales?")
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        "Primera vez (Principiante)" to "Buscando aprender junto a una mascota dócil.",
                                        "Alguna experiencia" to "He convivido previamente con mascotas.",
                                        "Mucha experiencia" to "Familiarizado con cuidados avanzados y adiestramiento."
                                    ).forEach { (opcion, desc) ->
                                        TarjetaOpcionVertical(
                                            titulo = opcion,
                                            subtitulo = desc,
                                            estaSeleccionado = experiencia == opcion,
                                            seleccionar = {
                                                experiencia = opcion
                                                mensajeError = ""
                                            }
                                        )
                                    }
                                }
                            }

                            3 -> {
                                TituloPregunta(icono = "🐕🐈🐇", titulo = "4. ¿Qué especie prefieres adoptar?")
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        "Perro" to "Leal, activo y compañero para paseos.",
                                        "Gato" to "Independiente, cariñoso y de fácil adaptación.",
                                        "Conejo" to "Tranquilo, dócil y excelente para espacios silenciosos.",
                                        "Sin preferencia (Cualquiera)" to "Abierto a conocer cualquier especie del catálogo."
                                    ).forEach { (opcion, desc) ->
                                        TarjetaOpcionVertical(
                                            titulo = opcion,
                                            subtitulo = desc,
                                            estaSeleccionado = especiePreferida == opcion,
                                            seleccionar = {
                                                especiePreferida = opcion
                                                mensajeError = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Mensaje de validación si no ha seleccionado respuesta
                        if (mensajeError.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "⚠️ $mensajeError",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botones de navegación (Anterior / Siguiente o Ver resultados)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (indicePregunta > 0) {
                                OutlinedButton(
                                    onClick = {
                                        indicePregunta--
                                        mensajeError = ""
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BorderWarm),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("← Anterior", color = AdoptTextDark)
                                }
                            }

                            Button(
                                onClick = {
                                    val respuestaActual = when (indicePregunta) {
                                        0 -> vivienda
                                        1 -> tiempo
                                        2 -> experiencia
                                        3 -> especiePreferida
                                        else -> ""
                                    }

                                    if (!respuestaValida(respuestaActual)) {
                                        mensajeError = "Por favor selecciona una opción para continuar."
                                    } else {
                                        mensajeError = ""
                                        if (indicePregunta < totalPreguntas - 1) {
                                            indicePregunta++
                                        } else {
                                            paso = PasoTest.RESULTADOS
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (indicePregunta < totalPreguntas - 1) "Siguiente →" else "Ver resultados ✨",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                    } else {
                        // =====================================================
                        // PASO 2: RESULTADOS DE COMPATIBILIDAD
                        // =====================================================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Mascotas compatibles contigo",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AdoptTextDark
                            )
                            Text(
                                text = "${mascotasCoincidentes.size} recomendaciones ordenadas según tu perfil",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdoptTextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (mascotasCoincidentes.isEmpty()) {
                                // ---------------------------------------------
                                // ESTADO SIN RESULTADOS
                                // ---------------------------------------------
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                                    border = BorderStroke(1.dp, BorderWarm)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🔍", fontSize = 36.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No encontramos mascotas compatibles con tus respuestas.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AdoptTextDark,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = {
                                                vivienda = ""
                                                tiempo = ""
                                                experiencia = ""
                                                especiePreferida = ""
                                                indicePregunta = 0
                                                paso = PasoTest.PREGUNTAS
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Repetir test", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // ---------------------------------------------
                                // LISTA DE RESULTADOS ORDENADA
                                // ---------------------------------------------
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                     mascotasCoincidentes.forEach { coincidencia ->
                                         val mascota = coincidencia.mascota
                                         val esPropia = mascota.creadoPorCorreo.isNotBlank() &&
                                                 mascota.creadoPorCorreo.equals(correoActual, ignoreCase = true)
                                         val permiteAdopcion = puedeAdoptar(mascota, esPropia)
                                         val esEnProceso = limpiarTexto(mascota.status) == "en proceso"

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                                            border = BorderStroke(1.dp, BorderWarm),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Foto
                                                    if (mascota.photoBitmap != null) {
                                                        Image(
                                                            bitmap = mascota.photoBitmap.asImageBitmap(),
                                                            contentDescription = mascota.name,
                                                            modifier = Modifier
                                                                .size(72.dp)
                                                                .clip(RoundedCornerShape(12.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else if (mascota.image != 0) {
                                                        Image(
                                                            painter = painterResource(id = mascota.image),
                                                            contentDescription = mascota.name,
                                                            modifier = Modifier
                                                                .size(72.dp)
                                                                .clip(RoundedCornerShape(12.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_pet_fallback),
                                                            contentDescription = mascota.name,
                                                            modifier = Modifier
                                                                .size(72.dp)
                                                                .clip(RoundedCornerShape(12.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = mascota.name,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = AdoptTextDark
                                                            )

                                                            // Insignia de compatibilidad
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = AdoptTealLight
                                                            ) {
                                                                Text(
                                                                    text = "${coincidencia.porcentajeCoincidencia}% Match",
                                                                    color = AdoptTealDark,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(2.dp))

                                                        Text(
                                                            text = "${mascota.species} • ${mascota.breed} • ${mascota.age}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = AdoptTextSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        Text(
                                                            text = "📍 ${mascota.location}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = AdoptTextSecondary
                                                        )

                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        InsigniaEstado(estado = mascota.status)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Explicación de la compatibilidad
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = AdoptMeBg,
                                                    border = BorderStroke(1.dp, BorderWarm),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("💡 ", fontSize = 12.sp)
                                                        Text(
                                                            text = coincidencia.explicacion,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = AdoptTextDark
                                                        )
                                                    }
                                                }

                                                // Advertencia si la mascota es publicación propia
                                                if (esPropia) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = AdoptTealLight,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "📢 Tu publicación (No puedes solicitar tu propia mascota)",
                                                            color = AdoptTealDark,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }

                                                // Advertencia si la mascota está en proceso
                                                if (esEnProceso) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = StatusOrangeBg,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "⏳ En proceso (No admite nuevas solicitudes)",
                                                            color = StatusOrangeText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Botones de acción coherentes con el estado
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            cerrar()
                                                            seleccionarMascota(mascota)
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, AdoptTeal),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AdoptTeal),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Ver detalle", fontWeight = FontWeight.SemiBold)
                                                    }

                                                    if (permiteAdopcion) {
                                                        Button(
                                                            onClick = {
                                                                cerrar()
                                                                if (solicitarAdopcion != null) {
                                                                    solicitarAdopcion(mascota)
                                                                } else {
                                                                    seleccionarMascota(mascota)
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text("Adoptar", fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Botón repetir test
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(onClick = {
                                            vivienda = ""
                                            tiempo = ""
                                            experiencia = ""
                                            especiePreferida = ""
                                            indicePregunta = 0
                                            paso = PasoTest.PREGUNTAS
                                        }) {
                                            Text(
                                                text = "🔄 Repetir test",
                                                color = AdoptOrangeDark,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TituloPregunta(icono: String, titulo: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icono, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AdoptTextDark
        )
    }
}

@Composable
private fun TarjetaOpcionVertical(
    titulo: String,
    subtitulo: String,
    estaSeleccionado: Boolean,
    seleccionar: () -> Unit
) {
    val colorFondo = if (estaSeleccionado) AdoptTealLight else AdoptCardWhite
    val borde = if (estaSeleccionado) AdoptTeal else BorderWarm
    val colorTexto = if (estaSeleccionado) AdoptTealDark else AdoptTextDark

    Surface(
        onClick = seleccionar,
        shape = RoundedCornerShape(14.dp),
        color = colorFondo,
        border = BorderStroke(if (estaSeleccionado) 1.5.dp else 1.dp, borde),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Medium,
                    color = colorTexto
                )
                if (subtitulo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = AdoptTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (estaSeleccionado) {
                Text("✓", color = AdoptTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
