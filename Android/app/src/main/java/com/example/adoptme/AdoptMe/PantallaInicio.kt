package com.example.adoptme.AdoptMe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import com.example.adoptme.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.model.Pet
import java.text.Normalizer

fun normalizarTexto(texto: String): String {
    val textoLimpio = texto.trim().lowercase()
    if (textoLimpio.isEmpty()) return ""
    val protegido = textoLimpio.replace("ñ", "___enie___")
    val descompuesto = Normalizer.normalize(protegido, Normalizer.Form.NFD)
    val sinAcentos = descompuesto.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return sinAcentos.replace("___enie___", "ñ")
}

fun esFiltroLibre(valorFiltro: String): Boolean {
    val normalizado = normalizarTexto(valorFiltro)
    return normalizado.isEmpty() ||
            normalizado == "todos" ||
            normalizado == "todas" ||
            normalizado == "todas las mascotas" ||
            normalizado == "cualquier sexo" ||
            normalizado == "macho o hembra" ||
            normalizado == "todas las zonas" ||
            normalizado == "todas las ubicaciones / ciudades" ||
            normalizado == "cualquier estado"
}

fun esFiltroComodin(valorFiltro: String): Boolean = esFiltroLibre(valorFiltro)

fun coincideBusqueda(mascota: Pet, consulta: String): Boolean {
    val consultaNormalizada = normalizarTexto(consulta)
    if (consultaNormalizada.isEmpty()) return true
    return normalizarTexto(mascota.name).contains(consultaNormalizada) ||
            normalizarTexto(mascota.species).contains(consultaNormalizada) ||
            normalizarTexto(mascota.breed).contains(consultaNormalizada) ||
            normalizarTexto(mascota.location).contains(consultaNormalizada)
}

fun filtrarEspecie(mascota: Pet, especie: String): Boolean {
    if (esFiltroLibre(especie)) return true
    return normalizarTexto(mascota.species) == normalizarTexto(especie)
}

fun coincideEspecie(mascota: Pet, especie: String): Boolean = filtrarEspecie(mascota, especie)

fun filtrarSexo(mascota: Pet, sexo: String): Boolean {
    if (esFiltroLibre(sexo)) return true
    return normalizarTexto(mascota.gender) == normalizarTexto(sexo)
}

fun coincideSexo(mascota: Pet, sexo: String): Boolean = filtrarSexo(mascota, sexo)
fun filtrarGenero(mascota: Pet, genero: String): Boolean = filtrarSexo(mascota, genero)
fun coincideGenero(mascota: Pet, genero: String): Boolean = filtrarSexo(mascota, genero)

fun filtrarUbicacion(mascota: Pet, ubicacion: String): Boolean {
    if (esFiltroLibre(ubicacion)) return true
    return normalizarTexto(mascota.location) == normalizarTexto(ubicacion)
}

fun coincideUbicacion(mascota: Pet, ubicacion: String): Boolean = filtrarUbicacion(mascota, ubicacion)

fun filtrarEstado(mascota: Pet, estado: String): Boolean {
    if (esFiltroLibre(estado)) return true
    return normalizarTexto(mascota.status) == normalizarTexto(estado)
}

fun coincideEstado(mascota: Pet, estado: String): Boolean = filtrarEstado(mascota, estado)

fun filtrarMascotas(
    mascotas: List<Pet>,
    textoBusqueda: String,
    especieSeleccionada: String,
    sexoSeleccionado: String,
    ubicacionSeleccionada: String,
    estadoSeleccionado: String
): List<Pet> {
    return mascotas.filter { mascota ->
        coincideBusqueda(mascota, textoBusqueda) &&
                filtrarEspecie(mascota, especieSeleccionada) &&
                filtrarSexo(mascota, sexoSeleccionado) &&
                filtrarUbicacion(mascota, ubicacionSeleccionada) &&
                filtrarEstado(mascota, estadoSeleccionado)
    }
}

/**
 * Campo desplegable estilizado para los filtros de catálogo.
 */
@Composable
private fun CampoDesplegableFiltro(
    titulo: String,
    valorSeleccionado: String,
    opciones: List<Pair<String, String>>,
    seleccionarOpcion: (String) -> Unit,
    esTituloMayuscula: Boolean = true
) {
    var expandido by remember { mutableStateOf(false) }

    val etiquetaMostrar = opciones.find {
        normalizarTexto(it.first) == normalizarTexto(valorSeleccionado)
    }?.second ?: valorSeleccionado

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (esTituloMayuscula) titulo.uppercase() else titulo,
            style = if (esTituloMayuscula) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = if (esTituloMayuscula) 0.8.sp else 0.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { expandido = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = etiquetaMostrar,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            DropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false }
            ) {
                opciones.forEach { (clave, etiqueta) ->
                    val estaSeleccionado = normalizarTexto(valorSeleccionado) == normalizarTexto(clave)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = etiqueta,
                                fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                color = if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            seleccionarOpcion(clave)
                            expandido = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Chip para la sección de Compatibilidad y Condiciones Especiales.
 */
@Composable
private fun PildoraCompatibilidad(
    etiqueta: String,
    estaSeleccionado: Boolean,
    alternar: () -> Unit,
    esEspecial: Boolean = false
) {
    val fondo = when {
        esEspecial -> MaterialTheme.colorScheme.errorContainer
        estaSeleccionado -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borde = when {
        esEspecial -> MaterialTheme.colorScheme.error
        estaSeleccionado -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    val colorTexto = when {
        esEspecial -> MaterialTheme.colorScheme.onErrorContainer
        estaSeleccionado -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = alternar,
        shape = RoundedCornerShape(20.dp),
        color = fondo,
        border = BorderStroke(1.dp, borde),
        modifier = Modifier.height(34.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = etiqueta,
                color = colorTexto,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (esEspecial || estaSeleccionado) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun PantallaInicio(
    mascotas: List<Pet>,
    favoritos: List<Pet>,
    mascotaPreferida: String = "Todas",
    ubicacionPreferida: String = "Todas",
    ordenPredeterminado: String = "Más recientes",
    soloDisponibles: Boolean = false,
    seleccionarMascota: (Pet) -> Unit,
    cambiarFavorito: (Pet) -> Unit,
    abrirFavoritos: () -> Unit,
    correoActual: String = "",
    solicitarAdopcionDesdeTest: ((Pet) -> Unit)? = null,
    cargando: Boolean = false,
    errorConexion: String? = null,
    reintentarCarga: (() -> Unit)? = null
) {
    val especieInicial = when (normalizarTexto(mascotaPreferida)) {
        "perros", "perro" -> "Perro"
        "gatos", "gato" -> "Gato"
        "conejos", "conejo" -> "Conejo"
        else -> "Todos"
    }

    val ubicacionInicial = if (ubicacionPreferida.equals("Todas", ignoreCase = true)) "Todas" else ubicacionPreferida
    val estadoInicial = if (soloDisponibles) "Disponible" else "Todos"
    val ordenInicial = if (ordenPredeterminado.equals("Nombre A-Z", ignoreCase = true)) "az" else "recientes"

    var textoBusqueda by rememberSaveable { mutableStateOf("") }
    var especieSeleccionada by rememberSaveable(mascotaPreferida) { mutableStateOf(especieInicial) }
    var sexoSeleccionado by rememberSaveable { mutableStateOf("Todos") }
    var ubicacionSeleccionada by rememberSaveable(ubicacionPreferida) { mutableStateOf(ubicacionInicial) }
    var estadoSeleccionado by rememberSaveable(soloDisponibles) { mutableStateOf(estadoInicial) }
    var panelFiltrosVisible by rememberSaveable { mutableStateOf(false) }
    var ubicacionSuperiorExpandida by remember { mutableStateOf(false) }
    var mostrarTestCompatibilidad by rememberSaveable { mutableStateOf(false) }

    var ordenSeleccionado by remember(ordenPredeterminado) { mutableStateOf(ordenInicial) }
    var tamanoSeleccionado by remember { mutableStateOf("todos") }
    var distanciaKm by remember { mutableFloatStateOf(50f) }
    var compatibilidadNinos by remember { mutableStateOf(false) }
    var compatibilidadPerros by remember { mutableStateOf(false) }
    var compatibilidadGatos by remember { mutableStateOf(false) }
    var condicionEsterilizado by remember { mutableStateOf(false) }
    var condicionUrgente by remember { mutableStateOf(true) }

    val ubicaciones = remember(mascotas) {
        listOf("Todas") + mascotas.map { it.location }.distinct()
    }

    fun reiniciarFiltros() {
        textoBusqueda = ""
        especieSeleccionada = especieInicial
        sexoSeleccionado = "Todos"
        ubicacionSeleccionada = ubicacionInicial
        estadoSeleccionado = estadoInicial
        ordenSeleccionado = ordenInicial
        tamanoSeleccionado = "todos"
        distanciaKm = 50f
        compatibilidadNinos = false
        compatibilidadPerros = false
        compatibilidadGatos = false
        condicionEsterilizado = false
        condicionUrgente = true
    }

    val tieneFiltrosActivos = textoBusqueda.isNotBlank() ||
            !esFiltroComodin(especieSeleccionada) ||
            !esFiltroComodin(sexoSeleccionado) ||
            !esFiltroComodin(ubicacionSeleccionada) ||
            !esFiltroComodin(estadoSeleccionado)

    val resumenFiltrosActivos = remember(textoBusqueda, especieSeleccionada, sexoSeleccionado, ubicacionSeleccionada, estadoSeleccionado) {
        val lista = mutableListOf<String>()
        if (textoBusqueda.isNotBlank()) lista.add("\"$textoBusqueda\"")
        if (!esFiltroComodin(especieSeleccionada)) {
            lista.add(when (especieSeleccionada.lowercase()) {
                "perro" -> "Perros"
                "gato" -> "Gatos"
                "conejo" -> "Conejos"
                else -> especieSeleccionada
            })
        }
        if (!esFiltroComodin(sexoSeleccionado)) lista.add(sexoSeleccionado)
        if (!esFiltroComodin(ubicacionSeleccionada)) lista.add(ubicacionSeleccionada)
        if (!esFiltroComodin(estadoSeleccionado)) lista.add(estadoSeleccionado)
        lista.joinToString(" · ")
    }

    val mascotasFiltradas = remember(
        mascotas,
        textoBusqueda,
        especieSeleccionada,
        sexoSeleccionado,
        ubicacionSeleccionada,
        estadoSeleccionado,
        ordenSeleccionado
    ) {
        val resultado = filtrarMascotas(
            mascotas = mascotas,
            textoBusqueda = textoBusqueda,
            especieSeleccionada = especieSeleccionada,
            sexoSeleccionado = sexoSeleccionado,
            ubicacionSeleccionada = ubicacionSeleccionada,
            estadoSeleccionado = estadoSeleccionado
        ).distinctBy { it.claveCompose }

        when (ordenSeleccionado) {
            "az" -> resultado.sortedBy { normalizarTexto(it.name) }
            else -> resultado
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Encabezado superior fijo
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🐾 AdoptMe",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Encuentra a tu nuevo compañero",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = abrirFavoritos,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = if (favoritos.isNotEmpty()) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                        contentDescription = "Favoritos",
                        modifier = Modifier.padding(end = 4.dp).size(18.dp),
                        tint = if (favoritos.isNotEmpty()) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (favoritos.isNotEmpty()) "${favoritos.size}" else "Favoritos",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Desplazamiento vertical fluido
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Banner Promocional ODS 12 y Test de Compatibilidad
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFE85012), Color(0xFF1B6B5C))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✨  Adopciones Éticas y Verificadas (ODS 12)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Text(
                                text = "Dale una segunda oportunidad a quien más amor tiene para dar",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )

                            Text(
                                text = "Filtra por raza, edad exacta y distancia para encontrar al compañero ideal. Contacta directamente a los refugios y postula en pocos minutos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )

                            Button(
                                onClick = { mostrarTestCompatibilidad = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00D4B2),
                                    contentColor = Color(0xFF0D3830)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "✨",
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "Hacer Test de Compatibilidad",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF0D3830)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tarjeta de filtros
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Buscador
                        OutlinedTextField(
                            value = textoBusqueda,
                            onValueChange = { textoBusqueda = it },
                            placeholder = {
                                Text(
                                    text = "Buscar por nombre, raza o albergue...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_search),
                                    contentDescription = "Buscar",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (textoBusqueda.isNotEmpty()) {
                                    IconButton(onClick = { textoBusqueda = "" }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_close),
                                            contentDescription = "Borrar búsqueda",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        )

                        // 2. Selector de ubicaciones
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { ubicacionSuperiorExpandida = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_location),
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp).size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (ubicacionSeleccionada == "Todas") "Todas las Ubicaciones / Ciudades" else ubicacionSeleccionada,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(text = "▼", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            DropdownMenu(
                                expanded = ubicacionSuperiorExpandida,
                                onDismissRequest = { ubicacionSuperiorExpandida = false }
                            ) {
                                ubicaciones.forEach { loc ->
                                    val isSelected = normalizarTexto(ubicacionSeleccionada) == normalizarTexto(loc)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (loc == "Todas") "Todas las Ubicaciones / Ciudades" else "📍 $loc",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            ubicacionSeleccionada = loc
                                            ubicacionSuperiorExpandida = false
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Botón para alternar panel de filtros
                        Surface(
                            onClick = { panelFiltrosVisible = !panelFiltrosVisible },
                            shape = RoundedCornerShape(14.dp),
                            color = if (panelFiltrosVisible) Color(0xFFFFF0E8) else Color(0xFFFFF8F5),
                            border = BorderStroke(1.dp, if (panelFiltrosVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_filter),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp).size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (panelFiltrosVisible) "Filtros ▲" else "Filtros ▼",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Resumen de filtros activos
                        if (tieneFiltrosActivos && resumenFiltrosActivos.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFF3EC))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = resumenFiltrosActivos,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                TextButton(
                                    onClick = { reiniciarFiltros() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_close),
                                            contentDescription = "Limpiar",
                                            modifier = Modifier.padding(end = 4.dp).size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Limpiar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Panel de filtros detallados
                        if (panelFiltrosVisible) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

                            CampoDesplegableFiltro(
                                titulo = "Especie de Mascota",
                                valorSeleccionado = especieSeleccionada,
                                opciones = listOf(
                                    "Todos" to "Todas las Mascotas",
                                    "Perro" to "🐶 Perros",
                                    "Gato" to "🐱 Gatos",
                                    "Conejo" to "🐰 Conejos"
                                ),
                                seleccionarOpcion = { especieSeleccionada = it }
                            )

                            CampoDesplegableFiltro(
                                titulo = "Estado de la Mascota",
                                valorSeleccionado = estadoSeleccionado,
                                opciones = listOf(
                                    "Todos" to "Cualquier estado",
                                    "Disponible" to "🟢 Disponible",
                                    "En proceso" to "⏳ En proceso",
                                    "Adoptada" to "🏠 Adoptada"
                                ),
                                seleccionarOpcion = { estadoSeleccionado = it }
                            )

                            CampoDesplegableFiltro(
                                titulo = "Ordenar Resultados",
                                valorSeleccionado = ordenSeleccionado,
                                opciones = listOf(
                                    "recientes" to "Más Recientes primero",
                                    "az" to "Nombre (A-Z)"
                                ),
                                seleccionarOpcion = { ordenSeleccionado = it }
                            )

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

                            CampoDesplegableFiltro(
                                titulo = "Tamaño del animal",
                                valorSeleccionado = tamanoSeleccionado,
                                opciones = listOf(
                                    "todos" to "Todos los tamaños",
                                    "pequeño" to "Pequeño",
                                    "mediano" to "Mediano",
                                    "grande" to "Grande"
                                ),
                                seleccionarOpcion = { tamanoSeleccionado = it },
                                esTituloMayuscula = false
                            )

                            CampoDesplegableFiltro(
                                titulo = "Sexo",
                                valorSeleccionado = sexoSeleccionado,
                                opciones = listOf(
                                    "Todos" to "Macho o Hembra",
                                    "Macho" to "Macho",
                                    "Hembra" to "Hembra"
                                ),
                                seleccionarOpcion = { sexoSeleccionado = it },
                                esTituloMayuscula = false
                            )

                            // Control de distancia
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Radio de distancia",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (distanciaKm >= 50f) "Sin límite (50+ km)" else "${distanciaKm.toInt()} km",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = distanciaKm,
                                    onValueChange = { distanciaKm = it },
                                    valueRange = 5f..50f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

                            // Compatibilidad y Condiciones Especiales
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Compatibilidad y Condiciones Especiales",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PildoraCompatibilidad(
                                        etiqueta = "Bueno con Niños",
                                        estaSeleccionado = compatibilidadNinos,
                                        alternar = { compatibilidadNinos = !compatibilidadNinos }
                                    )
                                    PildoraCompatibilidad(
                                        etiqueta = "Sociable con Perros",
                                        estaSeleccionado = compatibilidadPerros,
                                        alternar = { compatibilidadPerros = !compatibilidadPerros }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PildoraCompatibilidad(
                                        etiqueta = "Sociable con Gatos",
                                        estaSeleccionado = compatibilidadGatos,
                                        alternar = { compatibilidadGatos = !compatibilidadGatos }
                                    )
                                    PildoraCompatibilidad(
                                        etiqueta = "Esterilizado",
                                        estaSeleccionado = condicionEsterilizado,
                                        alternar = { condicionEsterilizado = !condicionEsterilizado }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                PildoraCompatibilidad(
                                    etiqueta = "🎉 Urgente / En Acogida",
                                    estaSeleccionado = condicionUrgente,
                                    alternar = { condicionUrgente = !condicionUrgente },
                                    esEspecial = true
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

                            // Pie del panel
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${mascotasFiltradas.size} mascotas encontradas",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (tieneFiltrosActivos) {
                                    TextButton(
                                        onClick = { reiniciarFiltros() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Restablecer filtros",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lista de resultados, estados de carga, error y catálogo vacío
            if (cargando && mascotas.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando catálogo de mascotas...",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else if (errorConexion != null && mascotas.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Error de conexión con el servidor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorConexion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            if (reintentarCarga != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = reintentarCarga,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text(text = "Reintentar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (mascotas.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No hay mascotas disponibles en el catálogo en este momento.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (mascotasFiltradas.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No se encontraron mascotas con estos criterios",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Button(
                                onClick = { reiniciarFiltros() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = "Restablecer filtros", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(mascotasFiltradas, key = { it.claveCompose }) { mascota ->
                    val esPropia = mascota.creadoPorCorreo.isNotBlank() &&
                            mascota.creadoPorCorreo.equals(correoActual, ignoreCase = true)
                    TarjetaMascota(
                        mascota = mascota,
                        esFavorito = favoritos.any { it.claveCompose == mascota.claveCompose },
                        cambiarFavorito = { cambiarFavorito(mascota) },
                        verDetalle = { seleccionarMascota(mascota) },
                        esPublicacionPropia = esPropia
                    )
                }
            }
        }

        if (mostrarTestCompatibilidad) {
            DialogoTestCompatibilidad(
                mascotas = mascotas,
                cerrar = { mostrarTestCompatibilidad = false },
                seleccionarMascota = seleccionarMascota,
                correoActual = correoActual,
                solicitarAdopcion = solicitarAdopcionDesdeTest
            )
        }
    }
}
