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
import com.example.adoptme.model.CentroApelacionDto
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
    rechazar: (solicitud: AdoptionRequest, motivo: String) -> Unit = { _, _ -> },
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
    val apelacionesPendientes by adminCentrosViewModel.apelacionesPendientes.collectAsState()
    var subPestanaCentros by remember { mutableStateOf("solicitudes") } // "solicitudes", "apelaciones"
    var apelacionParaAprobar by remember { mutableStateOf<CentroApelacionDto?>(null) }
    var apelacionParaRechazar by remember { mutableStateOf<CentroApelacionDto?>(null) }
    var respuestaRechazoApelacionTexto by remember { mutableStateOf("") }
    var errorRespuestaRechazoApelacion by remember { mutableStateOf<String?>(null) }

    // Diálogos de control para mascotas y solicitudes
    var mascotaParaDetalle by remember { mutableStateOf<Pet?>(null) }
    var mascotaParaCambiarEstado by remember { mutableStateOf<Pet?>(null) }
    var estadoSeleccionadoDialogo by remember { mutableStateOf("Disponible") }
    var mascotaParaEliminar by remember { mutableStateOf<Pet?>(null) }
    var mensajeValidacion by remember { mutableStateOf<String?>(null) }
    var mensajeConfirmacion by remember { mutableStateOf<String?>(null) }

    // Diálogos de control para centros
    var centroParaAprobar by remember { mutableStateOf<CentroAdminDto?>(null) }
    var centroParaSuspender by remember { mutableStateOf<CentroAdminDto?>(null) }
    var motivoSuspensionTexto by remember { mutableStateOf("") }
    var errorMotivoSuspension by remember { mutableStateOf<String?>(null) }
    var centroParaReactivar by remember { mutableStateOf<CentroAdminDto?>(null) }
    var centroParaRechazar by remember { mutableStateOf<CentroAdminDto?>(null) }
    var motivoRechazoTexto by remember { mutableStateOf("") }
    var errorMotivoRechazo by remember { mutableStateOf<String?>(null) }
    var centroParaVincular by remember { mutableStateOf<CentroAdminDto?>(null) }
    var usuarioIdVincularTexto by remember { mutableStateOf("") }
    var errorUsuarioIdVincular by remember { mutableStateOf<String?>(null) }

    // Cargar centros y apelaciones al inicio
    LaunchedEffect(Unit) {
        adminCentrosViewModel.cargarCentros()
        adminCentrosViewModel.cargarApelaciones()
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
                        text = "Centros de Adopción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTextDark
                    )
                    TextButton(onClick = { adminCentrosViewModel.refrescar() }) {
                        Text("Actualizar", color = AdoptTeal, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Selector de subpestañas: Directorio vs Apelaciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subSol = subPestanaCentros == "solicitudes"
                    val subApel = subPestanaCentros == "apelaciones"

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (subSol) AdoptTeal else AdoptCardWhite,
                        border = BorderStroke(1.dp, if (subSol) AdoptTeal else BorderWarm),
                        modifier = Modifier.weight(1f).clickable { subPestanaCentros = "solicitudes" }
                    ) {
                        Text(
                            text = "Directorio y Estados",
                            color = if (subSol) Color.White else AdoptTextDark,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (subApel) AdoptOrange else AdoptCardWhite,
                        border = BorderStroke(1.dp, if (subApel) AdoptOrange else BorderWarm),
                        modifier = Modifier.weight(1f).clickable {
                            subPestanaCentros = "apelaciones"
                            adminCentrosViewModel.cargarApelaciones()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Apelaciones",
                                color = if (subApel) Color.White else AdoptTextDark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (apelacionesPendientes.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (subApel) Color.White else MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = "${apelacionesPendientes.size}",
                                        color = if (subApel) AdoptOrange else Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (subPestanaCentros == "apelaciones") {
                    // VISTA DE APELACIONES PENDIENTES
                    if (apelacionesPendientes.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                            border = BorderStroke(1.dp, BorderWarm)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚖️", fontSize = 34.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No hay apelaciones pendientes de revisión.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AdoptTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            apelacionesPendientes.forEach { apelacion ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                                    border = BorderStroke(1.dp, BorderWarm),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🏡 ${apelacion.centroNombre}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AdoptTextDark
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Apelación #${apelacion.id}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (!apelacion.motivoSuspensionOriginal.isNullOrBlank()) {
                                            Text(
                                                text = "Motivo de suspensión previa: ${apelacion.motivoSuspensionOriginal}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AdoptMeBg,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "Fundamento presentado por el Centro:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AdoptTealDark
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = apelacion.motivoApelacion,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = AdoptTextDark
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { apelacionParaAprobar = apelacion },
                                                enabled = !centrosOperando,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Aprobar y Reactivar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    apelacionParaRechazar = apelacion
                                                    respuestaRechazoApelacionTexto = ""
                                                    errorRespuestaRechazoApelacion = null
                                                },
                                                enabled = !centrosOperando,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Rechazar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
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
                                            alSuspender = {
                                                centroParaSuspender = centro
                                                motivoSuspensionTexto = ""
                                                errorMotivoSuspension = null
                                            },
                                            alReactivar = {
                                                centroParaReactivar = centro
                                            },
                                            alVincularResponsable = {
                                                centroParaVincular = centro
                                                usuarioIdVincularTexto = ""
                                                errorUsuarioIdVincular = null
                                            }
                                        )
                                    }
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

    // 1b. Suspender Centro con Justificación Obligatoria
    if (centroParaSuspender != null) {
        val c = centroParaSuspender!!
        AlertDialog(
            onDismissRequest = { centroParaSuspender = null },
            title = { Text("Suspender Centro de Adopción", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa el motivo obligatorio de la suspensión operativa para '${c.nombre}'. El centro dejará de ser visible en el catálogo y no podrá publicar nuevas mascotas:")
                    OutlinedTextField(
                        value = motivoSuspensionTexto,
                        onValueChange = {
                            motivoSuspensionTexto = it
                            if (errorMotivoSuspension != null) errorMotivoSuspension = null
                        },
                        label = { Text("Motivo de suspensión (mínimo 5 caracteres) *") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMotivoSuspension != null
                    )
                    errorMotivoSuspension?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motivoSuspensionTexto.trim().length < 5) {
                            errorMotivoSuspension = "El motivo debe contener al menos 5 caracteres explicativos."
                            return@Button
                        }
                        val mot = motivoSuspensionTexto.trim()
                        val cid = c.id
                        centroParaSuspender = null
                        adminCentrosViewModel.suspenderCentro(cid, mot) { ok, msg ->
                            if (ok) mensajeConfirmacion = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Suspender centro")
                }
            },
            dismissButton = {
                TextButton(onClick = { centroParaSuspender = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 1c. Reactivar Centro Suspendido / Inactivo
    if (centroParaReactivar != null) {
        val c = centroParaReactivar!!
        AlertDialog(
            onDismissRequest = { centroParaReactivar = null },
            title = { Text("Reactivar Centro de Adopción", fontWeight = FontWeight.Bold, color = AdoptTeal) },
            text = {
                Text("¿Confirmas la reactivación de '${c.nombre}' (ID #${c.id})? El centro volverá a estar activo en el catálogo público y sus publicaciones pausadas se reanudarán.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cid = c.id
                        centroParaReactivar = null
                        adminCentrosViewModel.reactivarCentro(cid) { ok, msg ->
                            if (ok) mensajeConfirmacion = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Reactivar centro")
                }
            },
            dismissButton = {
                TextButton(onClick = { centroParaReactivar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 2. Rechazar Solicitud de Centro con Justificación Obligatoria
    if (centroParaRechazar != null) {
        val c = centroParaRechazar!!
        AlertDialog(
            onDismissRequest = { centroParaRechazar = null },
            title = { Text("Rechazar Solicitud de Centro", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa el motivo obligatorio del rechazo de solicitud para '${c.nombre}'. El responsable del centro verá esta justificación en su pantalla para corregir su solicitud:")
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
                    Text("Rechazar solicitud")
                }
            },
            dismissButton = {
                TextButton(onClick = { centroParaRechazar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 3. Vincular Responsable al Centro (1:1)
    if (centroParaVincular != null) {
        val c = centroParaVincular!!
        AlertDialog(
            onDismissRequest = { centroParaVincular = null },
            title = { Text("Vincular Usuario Responsable", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vincula un usuario con rol 'Centro' que no tenga otro centro asignado a '${c.nombre}':")
                    OutlinedTextField(
                        value = usuarioIdVincularTexto,
                        onValueChange = {
                            usuarioIdVincularTexto = it
                            if (errorUsuarioIdVincular != null) errorUsuarioIdVincular = null
                        },
                        label = { Text("ID numérico de Usuario *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorUsuarioIdVincular != null
                    )
                    errorUsuarioIdVincular?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = usuarioIdVincularTexto.trim().toIntOrNull()
                        if (uid == null || uid <= 0) {
                            errorUsuarioIdVincular = "Ingresa un ID numérico de usuario válido."
                            return@Button
                        }
                        val cid = c.id
                        centroParaVincular = null
                        adminCentrosViewModel.vincularResponsable(cid, uid) { ok, msg ->
                            if (ok) {
                                mensajeConfirmacion = msg
                            } else {
                                mensajeValidacion = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Vincular")
                }
            },
            dismissButton = {
                TextButton(onClick = { centroParaVincular = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogos de control para Apelaciones
    if (apelacionParaAprobar != null) {
        val a = apelacionParaAprobar!!
        AlertDialog(
            onDismissRequest = { apelacionParaAprobar = null },
            title = { Text("Aprobar Apelación y Reactivar", fontWeight = FontWeight.Bold, color = AdoptTealDark) },
            text = {
                Text("¿Confirmas la aprobación de la apelación #${a.id} del centro '${a.centroNombre}'? El centro será reactivado operativamente de inmediato y volverá a ser visible en el catálogo público.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val aid = a.id
                        apelacionParaAprobar = null
                        adminCentrosViewModel.aprobarApelacion(aid) { ok, msg ->
                            if (ok) mensajeConfirmacion = msg else mensajeValidacion = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Aprobar y Reactivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { apelacionParaAprobar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (apelacionParaRechazar != null) {
        val a = apelacionParaRechazar!!
        AlertDialog(
            onDismissRequest = {
                apelacionParaRechazar = null
                respuestaRechazoApelacionTexto = ""
                errorRespuestaRechazoApelacion = null
            },
            title = { Text("Rechazar Apelación", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa la fundamentación administrativa del rechazo para la apelación de '${a.centroNombre}'. El centro permanecerá suspendido:")
                    OutlinedTextField(
                        value = respuestaRechazoApelacionTexto,
                        onValueChange = {
                            respuestaRechazoApelacionTexto = it
                            if (errorRespuestaRechazoApelacion != null) errorRespuestaRechazoApelacion = null
                        },
                        label = { Text("Fundamento del rechazo (mínimo 5 caracteres) *") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorRespuestaRechazoApelacion != null
                    )
                    errorRespuestaRechazoApelacion?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val resp = respuestaRechazoApelacionTexto.trim()
                        if (resp.length < 5) {
                            errorRespuestaRechazoApelacion = "La fundamentación debe contener al menos 5 caracteres."
                            return@Button
                        }
                        val aid = a.id
                        apelacionParaRechazar = null
                        adminCentrosViewModel.rechazarApelacion(aid, resp) { ok, msg ->
                            if (ok) mensajeConfirmacion = msg else mensajeValidacion = msg
                        }
                        respuestaRechazoApelacionTexto = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar Rechazo")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    apelacionParaRechazar = null
                    respuestaRechazoApelacionTexto = ""
                    errorRespuestaRechazoApelacion = null
                }) {
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
    alSuspender: () -> Unit,
    alReactivar: () -> Unit,
    alVincularResponsable: () -> Unit
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
                val (colorBg, colorTxt, textoEstado) = when {
                    centro.estado.equals("Pendiente", ignoreCase = true) -> Triple(AdoptOrangeLight, StatusOrangeText, "Pendiente")
                    centro.estado.equals("Rechazado", ignoreCase = true) -> Triple(StatusRedBg, StatusRedText, "Rechazado")
                    centro.estado.equals("Aprobado", ignoreCase = true) && centro.activo -> Triple(StatusGreenBg, StatusGreenText, "Aprobado")
                    centro.estado.equals("Aprobado", ignoreCase = true) && !centro.activo && !centro.motivoSuspension.isNullOrBlank() -> Triple(StatusRedBg, StatusRedText, "Suspendido")
                    else -> Triple(Color(0xFFE2E8F0), Color(0xFF475569), "Inactivo")
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colorBg
                ) {
                    Text(
                        text = textoEstado,
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
                        text = "Responsable: ${centro.nombreResponsable ?: "Sin vincular"} · Correo: ${centro.correoResponsable ?: "N/A"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdoptTextDark
                    )
                    Text(
                        text = "Activo: ${if (centro.activo) "Sí (Visible)" else "No (Pausado)"} · Demostrativo: ${if (centro.esDemostrativo) "Sí" else "No"} · Mascotas: ${centro.totalMascotasPublicadas}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdoptTextSecondary
                    )
                    if (!centro.motivoSuspension.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Motivo de suspensión: ${centro.motivoSuspension}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusRedText,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (!centro.motivoRechazo.isNullOrBlank() && centro.estado.equals("Rechazado", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Motivo de rechazo: ${centro.motivoRechazo}",
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
                if (centro.estado.equals("Pendiente", ignoreCase = true)) {
                    Button(
                        onClick = alAprobar,
                        enabled = !estaOperando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                    ) {
                        Text("Aprobar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

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

                if (centro.estado.equals("Aprobado", ignoreCase = true)) {
                    if (centro.activo) {
                        OutlinedButton(
                            onClick = alSuspender,
                            enabled = !estaOperando,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Suspender", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = alReactivar,
                            enabled = !estaOperando,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                        ) {
                            Text("Reactivar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (centro.usuarioId == null) {
                    OutlinedButton(
                        onClick = alVincularResponsable,
                        enabled = !estaOperando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Vincular", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
