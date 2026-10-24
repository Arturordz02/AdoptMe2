package com.example.adoptme.AdoptMe

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.R
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptOrangeLight
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealDark
import com.example.adoptme.ui.theme.AdoptTealLight
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary
import com.example.adoptme.ui.theme.BorderWarm
import com.example.adoptme.ui.theme.StatusGreenBg
import com.example.adoptme.ui.theme.StatusGreenText
import com.example.adoptme.ui.theme.StatusOrangeText
import com.example.adoptme.ui.theme.StatusRedBg
import com.example.adoptme.ui.theme.StatusRedText

/**
 * Panel de Administración de AdoptMe.
 * Permite gestionar solicitudes de adopción (aprobar, rechazar, revisar)
 * e inspeccionar el catálogo de mascotas (cambiar estado, ver datos, eliminar publicaciones de usuarios).
 */
@Composable
fun PantallaAdministrador(
    solicitudes: List<AdoptionRequest>,
    aprobar: (AdoptionRequest) -> Pair<Boolean, String>,
    rechazar: (AdoptionRequest) -> Unit,
    cerrarSesion: () -> Unit = {},
    mascotas: List<Pet> = emptyList(),
    publicaciones: List<Pet> = emptyList(),
    ponerEnRevision: (AdoptionRequest) -> Unit = {},
    cambiarEstado: (Pet, String) -> Unit = { _, _ -> },
    eliminarMascota: (Pet) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var seccionActiva by remember { mutableStateOf("solicitudes") }
    var filtroSolicitudes by remember { mutableStateOf("Todas") }
    var filtroPublicaciones by remember { mutableStateOf("Todas") }

    // Diálogos de control
    var mascotaParaDetalle by remember { mutableStateOf<Pet?>(null) }
    var mascotaParaCambiarEstado by remember { mutableStateOf<Pet?>(null) }
    var estadoSeleccionadoDialogo by remember { mutableStateOf("Disponible") }
    var mascotaParaEliminar by remember { mutableStateOf<Pet?>(null) }
    var mensajeValidacion by remember { mutableStateOf<String?>(null) }
    var mensajeConfirmacion by remember { mutableStateOf<String?>(null) }

    // Cálculo de métricas en tiempo real
    val pendientesCount = solicitudes.count { it.status.equals("Pendiente", ignoreCase = true) }
    val enRevisionCount = solicitudes.count {
        it.status.equals("En revisión", ignoreCase = true) || it.status.equals("En revision", ignoreCase = true)
    }
    val disponiblesCount = mascotas.count { it.status.equals("Disponible", ignoreCase = true) }
    val enProcesoCount = mascotas.count { it.status.equals("En proceso", ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdoptMeBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        // =====================================================================
        // 1. ENCABEZADO DEL PANEL DE ADMINISTRACIÓN
        // =====================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
            border = BorderStroke(1.dp, BorderWarm),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AdoptTealLight,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🛡️", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Panel de Administración",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AdoptTextDark
                            )
                            Text(
                                text = "Gestiona solicitudes y publicaciones de AdoptMe",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdoptTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👑 ", fontSize = 12.sp)
                            Text(
                                text = "Rol: Administrador",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = cerrarSesion,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StatusRedText.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StatusRedBg,
                            contentColor = StatusRedText
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logout),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp).size(16.dp),
                                tint = StatusRedText
                            )
                            Text(
                                text = "Cerrar sesión",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================================
        // 2. RESUMEN DEL PANEL (Tarjetas indicadoras)
        // =====================================================================
        Text(
            text = "📊 Resumen del Panel",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AdoptTextDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                border = BorderStroke(1.dp, BorderWarm)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⏳ Pendientes", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$pendientesCount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusOrangeText
                    )
                    Text("Solicitudes", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                border = BorderStroke(1.dp, BorderWarm)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🔍 En revisión", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$enRevisionCount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTeal
                    )
                    Text("Solicitudes", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                border = BorderStroke(1.dp, BorderWarm)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🟢 Disponibles", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$disponiblesCount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreenText
                    )
                    Text("En catálogo", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                border = BorderStroke(1.dp, BorderWarm)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🐾 En proceso", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$enProcesoCount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AdoptOrange
                    )
                    Text("Adopciones", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                }
            }
        }

        // Mensaje temporal de confirmación si existe
        if (mensajeConfirmacion != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StatusGreenBg,
                border = BorderStroke(1.dp, StatusGreenText.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = mensajeConfirmacion!!,
                        color = StatusGreenText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { mensajeConfirmacion = null }) {
                        Text("OK", color = StatusGreenText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // =====================================================================
        // 3. SELECTOR DE SECCIONES (Pestañas)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AdoptCardWhite, shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val activaSol = seccionActiva == "solicitudes"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (activaSol) AdoptTeal else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { seccionActiva = "solicitudes" }
            ) {
                Text(
                    text = "📋 Solicitudes (${solicitudes.size})",
                    color = if (activaSol) Color.White else AdoptTextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (activaSol) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }

            val activaPub = seccionActiva == "publicaciones"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (activaPub) AdoptTeal else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { seccionActiva = "publicaciones" }
            ) {
                Text(
                    text = "🐾 Publicaciones (${mascotas.size})",
                    color = if (activaPub) Color.White else AdoptTextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (activaPub) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================================
        // 4. CONTENIDO SEGÚN SECCIÓN SELECCIONADA
        // =====================================================================
        if (seccionActiva == "solicitudes") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Expedientes de Solicitudes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Gestión demostrativa almacenada localmente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filtros rápidos para solicitudes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Todas", "Pendiente", "En revisión", "Aprobada", "Rechazada").forEach { filtro ->
                    val seleccionado = filtroSolicitudes == filtro
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (seleccionado) AdoptOrange else AdoptCardWhite,
                        border = BorderStroke(1.dp, if (seleccionado) AdoptOrange else BorderWarm),
                        modifier = Modifier.clickable { filtroSolicitudes = filtro }
                    ) {
                        Text(
                            text = filtro,
                            color = if (seleccionado) Color.White else AdoptTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val solicitudesFiltradas = solicitudes.filter { solicitud ->
                when (filtroSolicitudes) {
                    "Todas" -> true
                    "En revisión" -> solicitud.status.equals("En revisión", ignoreCase = true) || solicitud.status.equals("En revision", ignoreCase = true)
                    else -> solicitud.status.equals(filtroSolicitudes, ignoreCase = true)
                }
            }

            if (solicitudesFiltradas.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                    border = BorderStroke(1.dp, BorderWarm)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📥", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (filtroSolicitudes == "Todas") "No hay solicitudes registradas actualmente."
                            else "No hay solicitudes con estado '$filtroSolicitudes'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdoptTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                solicitudesFiltradas.forEach { solicitud ->
                    TarjetaSolicitudAdmin(
                        solicitud = solicitud,
                        ponerEnRevision = ponerEnRevision,
                        aprobar = aprobar,
                        rechazar = rechazar,
                        onConfirmacion = { msg ->
                            mensajeConfirmacion = msg
                            mensajeValidacion = null
                        },
                        onErrorValidacion = { err ->
                            mensajeValidacion = err
                            mensajeConfirmacion = null
                        }
                    )
                }
            }

        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Publicaciones de mascotas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtros rápidos para publicaciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Todas", "Disponible", "En proceso", "Adoptada", "Publicadas").forEach { filtro ->
                    val seleccionado = filtroPublicaciones == filtro
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (seleccionado) AdoptTeal else AdoptCardWhite,
                        border = BorderStroke(1.dp, if (seleccionado) AdoptTeal else BorderWarm),
                        modifier = Modifier.clickable { filtroPublicaciones = filtro }
                    ) {
                        Text(
                            text = filtro,
                            color = if (seleccionado) Color.White else AdoptTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val mascotasFiltradas = mascotas.filter { mascota ->
                when (filtroPublicaciones) {
                    "Todas" -> true
                    "Publicadas" -> publicaciones.any { it.id == mascota.id }
                    else -> mascota.status.equals(filtroPublicaciones, ignoreCase = true)
                }
            }

            if (mascotasFiltradas.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                    border = BorderStroke(1.dp, BorderWarm)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🐾", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (filtroPublicaciones == "Todas") "No hay mascotas en el sistema."
                            else "No se encontraron mascotas con filtro '$filtroPublicaciones'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdoptTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                mascotasFiltradas.forEach { mascota ->
                    val esDeUsuario = publicaciones.any { it.id == mascota.id }
                    TarjetaMascotaAdmin(
                        mascota = mascota,
                        esDeUsuario = esDeUsuario,
                        verDetalle = { mascotaParaDetalle = mascota },
                        cambiarEstado = {
                            mascotaParaCambiarEstado = mascota
                            estadoSeleccionadoDialogo = mascota.status
                        },
                        eliminar = if (esDeUsuario) { { mascotaParaEliminar = mascota } } else null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // =========================================================================
    // DIÁLOGOS MODALES DE CONTROL
    // =========================================================================

    // 1. Diálogo "Ver datos de la publicación"
    if (mascotaParaDetalle != null) {
        val pet = mascotaParaDetalle!!
        AlertDialog(
            onDismissRequest = { mascotaParaDetalle = null },
            title = {
                Text(
                    text = "🐾 ${pet.name} (${pet.species})",
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Raza: ${pet.breed}", style = MaterialTheme.typography.bodyMedium)
                    Text("Edad: ${pet.age}", style = MaterialTheme.typography.bodyMedium)
                    Text("Sexo: ${pet.gender}", style = MaterialTheme.typography.bodyMedium)
                    Text("Ubicación: ${pet.location}", style = MaterialTheme.typography.bodyMedium)
                    Text("Estado actual: ${pet.status}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Descripción: ${pet.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdoptTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { mascotaParaDetalle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cerrar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = AdoptCardWhite
        )
    }

    // 2. Diálogo "Cambiar estado de mascota"
    if (mascotaParaCambiarEstado != null) {
        val pet = mascotaParaCambiarEstado!!
        AlertDialog(
            onDismissRequest = { mascotaParaCambiarEstado = null },
            title = {
                Text(
                    text = "Cambiar estado: ${pet.name}",
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Selecciona el nuevo estado de la mascota:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdoptTextSecondary
                    )

                    listOf("Disponible", "En proceso", "Adoptada").forEach { estado ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { estadoSeleccionadoDialogo = estado }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = estadoSeleccionadoDialogo == estado,
                                onClick = { estadoSeleccionadoDialogo = estado },
                                colors = RadioButtonDefaults.colors(selectedColor = AdoptTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (estadoSeleccionadoDialogo == estado) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        cambiarEstado(pet, estadoSeleccionadoDialogo)
                        mensajeConfirmacion = "Estado de ${pet.name} actualizado a '$estadoSeleccionadoDialogo'."
                        mascotaParaCambiarEstado = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mascotaParaCambiarEstado = null }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = AdoptCardWhite
        )
    }

    // 3. Diálogo "Confirmar eliminación de publicación"
    if (mascotaParaEliminar != null) {
        val pet = mascotaParaEliminar!!
        AlertDialog(
            onDismissRequest = { mascotaParaEliminar = null },
            title = {
                Text(
                    text = "Eliminar publicación",
                    fontWeight = FontWeight.Bold,
                    color = StatusRedText
                )
            },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar la publicación de ${pet.name}? Esta mascota se quitará permanentemente del catálogo y de la lista de publicaciones.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        eliminarMascota(pet)
                        mensajeConfirmacion = "La publicación de ${pet.name} fue eliminada correctamente."
                        mascotaParaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRedText),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mascotaParaEliminar = null }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = AdoptCardWhite
        )
    }

    // 4. Diálogo de validación de disponibilidad
    if (mensajeValidacion != null) {
        AlertDialog(
            onDismissRequest = { mensajeValidacion = null },
            title = {
                Text(
                    text = "⚠️ Mascota no disponible",
                    fontWeight = FontWeight.Bold,
                    color = AdoptOrange
                )
            },
            text = {
                Text(
                    text = mensajeValidacion!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { mensajeValidacion = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Entendido")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = AdoptCardWhite
        )
    }
}
