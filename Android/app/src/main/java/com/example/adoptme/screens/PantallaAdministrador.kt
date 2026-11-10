package com.example.adoptme.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.adoptme.R
import com.example.adoptme.components.TarjetaMascotaAdmin
import com.example.adoptme.components.TarjetaSolicitudAdmin
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.CentroAdminDto
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptOrangeLight
import com.example.adoptme.ui.theme.AdoptSurfaceVariant
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
import com.example.adoptme.viewmodel.AdminCentrosUiState
import com.example.adoptme.viewmodel.AdminCentrosViewModel

/**
 * Panel de Administración de AdoptMe.
 * Permite gestionar:
 * 1. Solicitudes de adopción de mascotas.
 * 2. Catálogo de publicaciones y moderación.
 * 3. Solicitudes de centros de adopción (Aprobar, Rechazar con motivo, Activar/Desactivar, Asignar).
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
    adminCentrosViewModel: AdminCentrosViewModel = remember { AdminCentrosViewModel() },
    modifier: Modifier = Modifier
) {
    var seccionActiva by remember { mutableStateOf("solicitudes") } // "solicitudes", "publicaciones", "centros"
    var filtroSolicitudes by remember { mutableStateOf("Todas") }
    var filtroPublicaciones by remember { mutableStateOf("Todas") }
    var filtroCentros by remember { mutableStateOf("Todos") }

    val centrosUiState by adminCentrosViewModel.uiState.collectAsState()
    val centrosOperando by adminCentrosViewModel.estaOperando.collectAsState()

    // Diálogos de control para mascotas y solicitudes
    var mascotaParaDetalle by remember { mutableStateOf<Pet?>(null) }
    var mascotaParaCambiarEstado by remember { mutableStateOf<Pet?>(null) }
    var estadoSeleccionadoDialogo by remember { mutableStateOf("Disponible") }
    var mascotaParaEliminar by remember { mutableStateOf<Pet?>(null) }
    var mensajeValidacion by remember { mutableStateOf<String?>(null) }
    var mensajeConfirmacion by remember { mutableStateOf<String?>(null) }

    // Diálogos de control para centros
    var centroParaAprobar by remember { mutableStateOf<CentroAdminDto?>(null) }
    var centroParaDesactivar by remember { mutableStateOf<CentroAdminDto?>(null) }
    var centroParaRechazar by remember { mutableStateOf<CentroAdminDto?>(null) }
    var motivoRechazoTexto by remember { mutableStateOf("") }
    var errorMotivoRechazo by remember { mutableStateOf<String?>(null) }
    var centroParaAsignar by remember { mutableStateOf<CentroAdminDto?>(null) }
    var usuarioIdAsignarTexto by remember { mutableStateOf("") }
    var errorUsuarioIdAsignar by remember { mutableStateOf<String?>(null) }

    // Cargar centros al inicio
    LaunchedEffect(Unit) {
        adminCentrosViewModel.cargarCentros()
    }

    // Cálculo de métricas
    val pendientesCount = solicitudes.count { it.status.equals("Pendiente", ignoreCase = true) }
    val enRevisionCount = solicitudes.count {
        it.status.equals("En revisión", ignoreCase = true) || it.status.equals("En revision", ignoreCase = true)
    }
    val disponiblesCount = mascotas.count { it.status.equals("Disponible", ignoreCase = true) }
    val centrosTotalCount = (centrosUiState as? AdminCentrosUiState.Exito)?.centros?.size ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdoptMeBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        // 1. ENCABEZADO
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.adoptme_logo_original),
                            contentDescription = "Logo de AdoptMe",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(44.dp)
                        )
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Panel de Administración",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AdoptTextDark
                            )
                            Text(
                                text = "Auditoría de solicitudes, centros y publicaciones",
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

        // 2. RESUMEN EN TARJETAS
        Text(
            text = "📊 Resumen Global",
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
                    Text("🏡 Centros", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$centrosTotalCount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTeal
                    )
                    Text("En plataforma", style = MaterialTheme.typography.labelSmall, color = AdoptTextSecondary)
                }
            }
        }

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

        // 3. SELECTOR DE PESTAÑAS (3 SECCIONES: SOLICITUDES, CENTROS, PUBLICACIONES)
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
                    text = "📋 Solicitudes",
                    color = if (activaSol) Color.White else AdoptTextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (activaSol) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }

            val activaCentros = seccionActiva == "centros"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (activaCentros) AdoptOrange else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        seccionActiva = "centros"
                        adminCentrosViewModel.refrescar()
                    }
            ) {
                Text(
                    text = "🏡 Centros",
                    color = if (activaCentros) Color.White else AdoptTextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (activaCentros) FontWeight.Bold else FontWeight.Medium,
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
                    text = "🐾 Mascotas",
                    color = if (activaPub) Color.White else AdoptTextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (activaPub) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. CONTENIDO SEGÚN SECCIÓN
        when (seccionActiva) {
            "solicitudes" -> {
                // SECCIÓN SOLICITUDES DE ADOPCIÓN
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
                            modifier = Modifier.fillMaxWidth().padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📥", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (filtroSolicitudes == "Todas") "No hay solicitudes registradas." else "No hay solicitudes con estado '$filtroSolicitudes'.",
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
                            onConfirmacion = { msg -> mensajeConfirmacion = msg; mensajeValidacion = null },
                            onErrorValidacion = { err -> mensajeValidacion = err; mensajeConfirmacion = null }
                        )
                    }
                }
            }

            "centros" -> {
                // SECCIÓN SOLICITUDES Y AUDITORÍA DE CENTROS DE ADOPCIÓN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Solicitudes de Centros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTextDark
                    )
                    TextButton(onClick = { adminCentrosViewModel.refrescar() }) {
                        Text("Actualizar", color = AdoptTeal, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Filtros de estado de centros
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Todos", "Pendiente", "Aprobado", "Rechazado").forEach { filtro ->
                        val seleccionado = filtroCentros == filtro
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (seleccionado) AdoptTeal else AdoptCardWhite,
                            border = BorderStroke(1.dp, if (seleccionado) AdoptTeal else BorderWarm),
                            modifier = Modifier.clickable {
                                filtroCentros = filtro
                                adminCentrosViewModel.cargarCentros(
                                    estado = if (filtro == "Todos") null else filtro
                                )
                            }
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

                when (val state = centrosUiState) {
                    is AdminCentrosUiState.Cargando -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AdoptTeal)
                        }
                    }
                    is AdminCentrosUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { adminCentrosViewModel.refrescar() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                    is AdminCentrosUiState.Exito -> {
                        val listaCentros = state.centros
                        if (listaCentros.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                                border = BorderStroke(1.dp, BorderWarm)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🏡", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No se encontraron centros con estado '$filtroCentros'.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AdoptTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                listaCentros.forEach { centro ->
                                    TarjetaAdminCentroItem(
                                        centro = centro,
                                        estaOperando = centrosOperando,
                                        alAprobar = { centroParaAprobar = centro },
                                        alRechazar = {
                                            centroParaRechazar = centro
                                            motivoRechazoTexto = ""
                                            errorMotivoRechazo = null
                                        },
                                        alActivar = {
                                            adminCentrosViewModel.activarCentro(centro.id) { ok, msg ->
                                                if (ok) mensajeConfirmacion = msg
                                            }
                                        },
                                        alDesactivar = {
                                            centroParaDesactivar = centro
                                        },
                                        alAsignarPropietario = {
                                            centroParaAsignar = centro
                                            usuarioIdAsignarTexto = centro.usuarioId?.toString() ?: ""
                                            errorUsuarioIdAsignar = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "publicaciones" -> {
                // SECCIÓN PUBLICACIONES DE MASCOTAS
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
                            modifier = Modifier.fillMaxWidth().padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🐾", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (filtroPublicaciones == "Todas") "No hay mascotas en el sistema." else "No se encontraron mascotas con filtro '$filtroPublicaciones'.",
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
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // DIÁLOGOS MODALES ADMINISTRATIVOS PARA CENTROS

    // 1. Confirmar Aprobación de Centro
    if (centroParaAprobar != null) {
        val c = centroParaAprobar!!
        AlertDialog(
            onDismissRequest = { centroParaAprobar = null },
            title = { Text("Aprobar Centro de Adopción", fontWeight = FontWeight.Bold) },
            text = {
                Text("¿Confirmas la aprobación de '${c.nombre}' (ID #${c.id})? El centro pasará a estado APROBADO, aparecerá en el catálogo público y podrá publicar mascotas.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        centroParaAprobar = null
                        adminCentrosViewModel.aprobarCentro(c.id) { ok, msg ->
                            if (ok) mensajeConfirmacion = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Aprobar centro")
                }
            },
            dismissButton = {
                TextButton(onClick = { centroParaAprobar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 1b. Confirmar Desactivación de Centro
    if (centroParaDesactivar != null) {
        val c = centroParaDesactivar!!
        AlertDialog(
            onDismissRequest = { centroParaDesactivar = null },
            title = { Text("Desactivar Centro de Adopción", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text("¿Confirmas la desactivación de '${c.nombre}' (ID #${c.id})? El centro dejará de ser visible en el catálogo público y en el mapa de adopciones.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cid = c.id
                        centroParaDesactivar = null
                        adminCentrosViewModel.desactivarCentro(cid) { ok, msg ->
                            if (ok) mensajeConfirmacion = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Desactivar centro")
                }
            },
            dismissButton = {
                TextButton(onClick = { centroParaDesactivar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 2. Rechazar Centro con Justificación Obligatoria
    if (centroParaRechazar != null) {
        val c = centroParaRechazar!!
        AlertDialog(
            onDismissRequest = { centroParaRechazar = null },
            title = { Text("Rechazar Centro de Adopción", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa el motivo obligatorio del rechazo para '${c.nombre}'. El responsable del centro verá esta justificación en su pantalla para corregir la solicitud:")
                    OutlinedTextField(
                        value = motivoRechazoTexto,
                        onValueChange = {
                            motivoRechazoTexto = it
                            if (errorMotivoRechazo != null) errorMotivoRechazo = null
                        },
                        label = { Text("Motivo de rechazo (mínimo 5 caracteres) *") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMotivoRechazo != null
                    )
                    errorMotivoRechazo?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motivoRechazoTexto.trim().length < 5) {
                            errorMotivoRechazo = "El motivo debe contener al menos 5 caracteres explicativos."
                            return@Button
                        }
                        val mot = motivoRechazoTexto.trim()
                        val cid = c.id
                        centroParaRechazar = null
                        adminCentrosViewModel.rechazarCentro(cid, mot) { ok, msg ->
                            if (ok) mensajeConfirmacion = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Rechazar con motivo")
                }
            },
            dismissButton = {
                TextButton(onClick = { centroParaRechazar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 3. Asignar Propietario al Centro
    if (centroParaAsignar != null) {
        val c = centroParaAsignar!!
        AlertDialog(
            onDismissRequest = { centroParaAsignar = null },
            title = { Text("Asignar Propietario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Asigna o transfiere la titularidad de '${c.nombre}' a un usuario con rol 'Centro':")
                    OutlinedTextField(
                        value = usuarioIdAsignarTexto,
                        onValueChange = {
                            usuarioIdAsignarTexto = it
                            if (errorUsuarioIdAsignar != null) errorUsuarioIdAsignar = null
                        },
                        label = { Text("ID numérico de Usuario *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorUsuarioIdAsignar != null
                    )
                    errorUsuarioIdAsignar?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = usuarioIdAsignarTexto.trim().toIntOrNull()
                        if (uid == null || uid <= 0) {
                            errorUsuarioIdAsignar = "Ingresa un ID numérico de usuario válido."
                            return@Button
                        }
                        val cid = c.id
                        centroParaAsignar = null
                        adminCentrosViewModel.asignarPropietario(cid, uid) { ok, msg ->
                            if (ok) mensajeConfirmacion = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Asignar titular")
                }
            },
            dismissButton = {
                TextButton(onClick = { centroParaAsignar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogos de control previos para mascotas
    if (mascotaParaDetalle != null) {
        val pet = mascotaParaDetalle!!
        AlertDialog(
            onDismissRequest = { mascotaParaDetalle = null },
            title = { Text("🐾 ${pet.name} (${pet.species})", fontWeight = FontWeight.Bold, color = AdoptTextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Raza: ${pet.breed}", style = MaterialTheme.typography.bodyMedium)
                    Text("Edad: ${pet.age}", style = MaterialTheme.typography.bodyMedium)
                    Text("Sexo: ${pet.gender}", style = MaterialTheme.typography.bodyMedium)
                    Text("Ubicación: ${pet.location}", style = MaterialTheme.typography.bodyMedium)
                    Text("Estado actual: ${pet.status}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    if (pet.centroId != null) {
                        Text("Centro ID: #${pet.centroId}", style = MaterialTheme.typography.bodyMedium, color = AdoptOrange)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Descripción: ${pet.description}", style = MaterialTheme.typography.bodySmall, color = AdoptTextSecondary)
                }
            },
            confirmButton = {
                Button(onClick = { mascotaParaDetalle = null }, colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (mascotaParaCambiarEstado != null) {
        val pet = mascotaParaCambiarEstado!!
        AlertDialog(
            onDismissRequest = { mascotaParaCambiarEstado = null },
            title = { Text("Cambiar estado: ${pet.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Disponible", "En proceso", "Adoptada").forEach { estado ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { estadoSeleccionadoDialogo = estado }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = estadoSeleccionadoDialogo == estado,
                                onClick = { estadoSeleccionadoDialogo = estado }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(estado)
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
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mascotaParaCambiarEstado = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mascotaParaEliminar != null) {
        val pet = mascotaParaEliminar!!
        AlertDialog(
            onDismissRequest = { mascotaParaEliminar = null },
            title = { Text("Eliminar publicación", fontWeight = FontWeight.Bold, color = StatusRedText) },
            text = { Text("¿Seguro que deseas eliminar la publicación de ${pet.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        eliminarMascota(pet)
                        mensajeConfirmacion = "La publicación de ${pet.name} fue eliminada correctamente."
                        mascotaParaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRedText)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mascotaParaEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mensajeValidacion != null) {
        AlertDialog(
            onDismissRequest = { mensajeValidacion = null },
            title = { Text("⚠️ Atención", fontWeight = FontWeight.Bold, color = AdoptOrange) },
            text = { Text(mensajeValidacion!!) },
            confirmButton = {
                Button(onClick = { mensajeValidacion = null }, colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
fun TarjetaAdminCentroItem(
    centro: CentroAdminDto,
    estaOperando: Boolean,
    alAprobar: () -> Unit,
    alRechazar: () -> Unit,
    alActivar: () -> Unit,
    alDesactivar: () -> Unit,
    alAsignarPropietario: () -> Unit
) {
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
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AdoptOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (!centro.logoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = centro.logoUrl,
                            contentDescription = centro.nombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = { Text("🏡", fontSize = 22.sp) }
                        )
                    } else {
                        Text("🏡", fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = centro.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTextDark
                    )
                    Text(
                        text = "ID #${centro.id} · ${centro.distrito} · ${centro.telefono}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdoptTextSecondary
                    )
                }

                // Insignia de Estado
                val (colorBg, colorTxt) = when (centro.estado.lowercase()) {
                    "aprobado" -> StatusGreenBg to StatusGreenText
                    "rechazado" -> StatusRedBg to StatusRedText
                    else -> AdoptOrangeLight to StatusOrangeText
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colorBg
                ) {
                    Text(
                        text = centro.estado,
                        color = colorTxt,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Datos del usuario responsable
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AdoptSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Responsable: ${centro.nombreResponsable ?: "Sin asignar"} · Correo: ${centro.correoResponsable ?: "N/A"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdoptTextDark
                    )
                    Text(
                        text = "Activo: ${if (centro.activo) "Sí (Visible)" else "No (Oculto)"} · Demostrativo: ${if (centro.esDemostrativo) "Sí" else "No"} · Mascotas: ${centro.totalMascotasPublicadas}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdoptTextSecondary
                    )
                    if (!centro.motivoRechazo.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Motivo de rechazo previo: ${centro.motivoRechazo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusRedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fila de acciones administrativas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!centro.estado.equals("Aprobado", ignoreCase = true)) {
                    Button(
                        onClick = alAprobar,
                        enabled = !estaOperando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                    ) {
                        Text("Aprobar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!centro.estado.equals("Rechazado", ignoreCase = true)) {
                    OutlinedButton(
                        onClick = alRechazar,
                        enabled = !estaOperando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Rechazar", fontSize = 11.sp)
                    }
                }

                if (centro.activo) {
                    OutlinedButton(
                        onClick = alDesactivar,
                        enabled = !estaOperando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Desactivar", fontSize = 11.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = alActivar,
                        enabled = !estaOperando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Activar", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = alAsignarPropietario,
                    enabled = !estaOperando,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Asignar", fontSize = 11.sp)
                }
            }
        }
    }
}
