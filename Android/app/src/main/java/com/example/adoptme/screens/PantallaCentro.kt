package com.example.adoptme.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.adoptme.R
import com.example.adoptme.components.DialogoExplicacionUbicacion
import com.example.adoptme.components.DialogoSelectorMapa
import com.example.adoptme.model.ActualizarMiCentroRequest
import com.example.adoptme.model.CentroPropietarioDto
import com.example.adoptme.model.ConversacionCentro
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
import com.example.adoptme.utils.HaversineUtil
import com.example.adoptme.utils.ImageProcessor
import com.example.adoptme.viewmodel.MiCentroUiState
import com.example.adoptme.viewmodel.MiCentroViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCentro(
    conversaciones: List<ConversacionCentro>,
    abrirConversacion: (idCentro: Int, correoAdoptante: String, nombreCentro: String) -> Unit,
    cerrarSesion: () -> Unit,
    idCentroActualInicial: Int = 0,
    onPublicarMascota: () -> Unit = {},
    onVerPublicaciones: () -> Unit = {},
    onVerEstadoPendiente: () -> Unit = {},
    mascotasTotales: List<Pet> = emptyList(),
    viewModel: MiCentroViewModel = remember { MiCentroViewModel() },
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    val estaOperando by viewModel.estaOperando.collectAsState()

    var mostrarDialogoEditar by remember { mutableStateOf(false) }
    var mostrarDialogoOpcionesLogo by remember { mutableStateOf(false) }
    var mostrarPedagogicoCamara by remember { mutableStateOf(false) }
    var mostrarPedagogicoGaleria by remember { mutableStateOf(false) }

    var uriFotoTemporalCamara by remember { mutableStateOf<Uri?>(null) }
    var archivoFotoTemporalCamara by remember { mutableStateOf<File?>(null) }

    // Lanzador nativo de Photo Picker (sin permisos generales de almacenamiento)
    val lanzadorPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val resultado = ImageProcessor.procesarImagenUri(contexto, uri)
                    val body = resultado.bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("imagen", "logo_centro.jpg", body)
                    viewModel.actualizarLogo(part) { exito, mensaje, _ ->
                        scope.launch { snackbarHostState.showSnackbar(mensaje) }
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Error al procesar la imagen.")
                }
            }
        }
    }

    // Lanzador nativo de Cámara con FileProvider
    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito && uriFotoTemporalCamara != null) {
            scope.launch {
                try {
                    val resultado = ImageProcessor.procesarImagenUri(
                        contexto,
                        uriFotoTemporalCamara!!,
                        archivoFotoTemporalCamara
                    )
                    val body = resultado.bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("imagen", "logo_centro.jpg", body)
                    viewModel.actualizarLogo(part) { ok, mensaje, _ ->
                        scope.launch { snackbarHostState.showSnackbar(mensaje) }
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Error al procesar fotografía.")
                }
            }
        }
    }

    // Lanzador de permiso de Cámara
    val lanzadorPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val par = ImageProcessor.crearArchivoTemporalCamara(contexto)
            archivoFotoTemporalCamara = par.first
            uriFotoTemporalCamara = par.second
            lanzadorCamara.launch(par.second)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Permiso de cámara rechazado. Puedes subir el logo desde la galería.")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarMiCentro()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is MiCentroUiState.Cargando -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AdoptTeal)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Cargando perfil operativo del centro...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MiCentroUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.mensaje,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.cargarMiCentro() },
                            colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                        ) {
                            Text("Reintentar")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = cerrarSesion) {
                            Text("Cerrar sesión")
                        }
                    }
                }
                is MiCentroUiState.Exito -> {
                    val centro = state.centro

                    // Si no está aprobado o está inactivo, redirigir a PantallaEstadoCentro
                    if (!centro.estado.equals("Aprobado", ignoreCase = true) || !centro.activo) {
                        LaunchedEffect(centro.estado, centro.activo) {
                            onVerEstadoPendiente()
                        }
                    }

                    // Aislamiento estricto por ID numérico real del backend
                    val idCentroReal = centro.id
                    val conversacionesFiltradas = conversaciones.filter { it.idCentro == idCentroReal }
                    val mascotasDelCentro = mascotasTotales.filter { it.centroId == idCentroReal }
                    val mascotasDisponibles = mascotasDelCentro.count { it.status.equals("Disponible", ignoreCase = true) }
                    val totalNoLeidosCentro = conversacionesFiltradas.sumOf { it.mensajesNoLeidos }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // TARJETA DE PERFIL INSTITUCIONAL REDISEÑADA
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Logotipo institucional
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(AdoptTealLight)
                                            .clickable { mostrarDialogoOpcionesLogo = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!centro.logoUrl.isNullOrBlank()) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(contexto)
                                                    .data(centro.logoUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Logotipo de ${centro.nombre}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = AdoptTeal
                                                    )
                                                },
                                                error = { Text("🏡", fontSize = 28.sp) }
                                            )
                                        } else {
                                            Text("🏡", fontSize = 28.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = centro.nombre,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        // Badges de estado institucional
                                        Row(
                                            modifier = Modifier.padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (centro.estado.equals("Aprobado", ignoreCase = true)) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "✓ Verificado",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (centro.activo) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AdoptTeal.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Activo (ID #$idCentroReal)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AdoptTealDark,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (centro.esDemostrativo) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AdoptOrangeLight
                                                ) {
                                                    Text(
                                                        text = "Demostrativo",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AdoptOrange,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Dirección física y distrito
                                        Text(
                                            text = "📍 ${centro.direccion}${if (centro.distrito.isNotBlank()) ", ${centro.distrito}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdoptTealDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Contacto y horario
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🕒 ${centro.horario}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "📞 ${centro.telefono}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Acciones operativas responsive (no 3 botones apretados)
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    if (maxWidth < 360.dp) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = onPublicarMascota,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Publicar mascota", fontWeight = FontWeight.Bold)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { mostrarDialogoEditar = true },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Editar", fontSize = 12.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { mostrarDialogoOpcionesLogo = true },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Logo", fontSize = 12.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = cerrarSesion,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text("Salir", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { mostrarDialogoEditar = true },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Editar perfil", fontSize = 12.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { mostrarDialogoOpcionesLogo = true },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Cambiar logo", fontSize = 12.sp)
                                                }
                                                Button(
                                                    onClick = onPublicarMascota,
                                                    modifier = Modifier.weight(1.3f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Publicar mascota", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = cerrarSesion,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text("Cerrar sesión", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // CUADRÍCULA 2x2 DE MÉTRICAS OPERATIVAS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TarjetaMetricaCentro(
                                titulo = "Publicaciones",
                                valor = "${mascotasDelCentro.size}",
                                icono = "🐾",
                                onClick = onVerPublicaciones,
                                modifier = Modifier.weight(1f)
                            )
                            TarjetaMetricaCentro(
                                titulo = "Disponibles",
                                valor = "$mascotasDisponibles",
                                icono = "✨",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TarjetaMetricaCentro(
                                titulo = "Conversaciones",
                                valor = "${conversacionesFiltradas.size}",
                                icono = "💬",
                                modifier = Modifier.weight(1f)
                            )
                            TarjetaMetricaCentro(
                                titulo = "No leídos",
                                valor = if (totalNoLeidosCentro > 99) "99+" else "$totalNoLeidosCentro",
                                icono = "📬",
                                resaltar = totalNoLeidosCentro > 0,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // SECCIÓN DE CONVERSACIONES AISLADAS (UX-B1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Bandeja de mensajes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Mensajería local demostrativa (aislada al ID #$idCentroReal)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (totalNoLeidosCentro > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        val textoNoLeidos = if (totalNoLeidosCentro > 99) "99+ no leídos" else "$totalNoLeidosCentro no leído(s)"
                                        Text(
                                            text = textoNoLeidos,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AdoptTealLight
                                ) {
                                    Text(
                                        text = "${conversacionesFiltradas.size} chats",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AdoptTealDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (conversacionesFiltradas.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📬", fontSize = 40.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Sin conversaciones para este centro",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Los mensajes enviados por adoptantes a ${centro.nombre} aparecerán aquí automáticamente.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(
                                    items = conversacionesFiltradas,
                                    key = { "${it.idCentro}_${it.correoAdoptante}" }
                                ) { conv ->
                                    val tienePendientes = conv.mensajesNoLeidos > 0

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (tienePendientes) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (tienePendientes) {
                                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                        } else null,
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (tienePendientes) 3.dp else 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = conv.correoAdoptante,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (tienePendientes) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.errorContainer
                                                        ) {
                                                            val txt = if (conv.mensajesNoLeidos > 99) "99+ nuevo(s)" else "${conv.mensajesNoLeidos} nuevo(s)"
                                                            Text(
                                                                text = txt,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                if (conv.fecha.isNotBlank()) {
                                                    Text(
                                                        text = conv.fecha,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = conv.ultimoTexto.ifBlank { "Sin mensajes" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Button(
                                                onClick = {
                                                    abrirConversacion(conv.idCentro, conv.correoAdoptante, conv.nombreCentro)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                                            ) {
                                                Text("Abrir chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // DIÁLOGO DE EDICIÓN CON EVALUACIÓN DE CAMBIOS SENSIBLES
                    if (mostrarDialogoEditar) {
                        DialogoEditarMiCentroConSensibilidad(
                            centro = centro,
                            onDescartar = { mostrarDialogoEditar = false },
                            onGuardar = { request, requiereReevaluacion ->
                                mostrarDialogoEditar = false
                                viewModel.actualizarPerfil(request) { exito, mensaje, pasoAPendiente ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(mensaje)
                                    }
                                    if (pasoAPendiente) {
                                        onVerEstadoPendiente()
                                    }
                                }
                            }
                        )
                    }

                    // DIÁLOGO DE SELECCIÓN DE ORIGEN PARA LOGOTIPO
                    if (mostrarDialogoOpcionesLogo) {
                        AlertDialog(
                            onDismissRequest = { mostrarDialogoOpcionesLogo = false },
                            title = { Text("Actualizar logotipo institucional", fontWeight = FontWeight.Bold) },
                            text = {
                                Text("Selecciona el medio para capturar o elegir el logo oficial de tu centro de adopción.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        mostrarDialogoOpcionesLogo = false
                                        mostrarPedagogicoGaleria = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                                ) {
                                    Text("Galería (Photo Picker)")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = {
                                        mostrarDialogoOpcionesLogo = false
                                        mostrarPedagogicoCamara = true
                                    }
                                ) {
                                    Text("Cámara")
                                }
                            }
                        )
                    }

                    // Diálogo pedagógico para Cámara (explicación visible previa al permiso nativo)
                    if (mostrarPedagogicoCamara) {
                        AlertDialog(
                            onDismissRequest = { mostrarPedagogicoCamara = false },
                            title = { Text("📷 Permiso de Cámara", fontWeight = FontWeight.Bold) },
                            text = {
                                Text("AdoptMe utilizará tu cámara exclusivamente para capturar la fotografía del logotipo institucional de tu centro. La imagen se almacena en la caché privada de la app mediante FileProvider sin exponerla públicamente.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        mostrarPedagogicoCamara = false
                                        lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                                ) {
                                    Text("Continuar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { mostrarPedagogicoCamara = false }) {
                                    Text("Ahora no")
                                }
                            }
                        )
                    }

                    // Diálogo pedagógico para Galería (Photo Picker seguro sin permisos amplios de almacenamiento)
                    if (mostrarPedagogicoGaleria) {
                        AlertDialog(
                            onDismissRequest = { mostrarPedagogicoGaleria = false },
                            title = { Text("🖼️ Selección de Imagen", fontWeight = FontWeight.Bold) },
                            text = {
                                Text("Se abrirá el selector nativo de Android (Photo Picker). Podrás elegir una sola imagen para el logo sin conceder a AdoptMe acceso a toda tu galería.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        mostrarPedagogicoGaleria = false
                                        lanzadorPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                                ) {
                                    Text("Abrir selector")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { mostrarPedagogicoGaleria = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
                is MiCentroUiState.Inactivo -> { }
            }
        }
    }
}

/**
 * Tarjeta de métrica operativa individual para el panel del centro.
 */
@Composable
private fun TarjetaMetricaCentro(
    titulo: String,
    valor: String,
    icono: String,
    resaltar: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (resaltar) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (resaltar) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(text = icono, fontSize = 20.sp)
        }
    }
}

@Composable
fun DialogoEditarMiCentroConSensibilidad(
    centro: CentroPropietarioDto,
    onDescartar: () -> Unit,
    onGuardar: (ActualizarMiCentroRequest, Boolean) -> Unit
) {
    var nombre by remember { mutableStateOf(centro.nombre) }
    var descripcion by remember { mutableStateOf(centro.descripcion) }
    var direccion by remember { mutableStateOf(centro.direccion) }
    var distrito by remember { mutableStateOf(centro.distrito) }
    var telefono by remember { mutableStateOf(centro.telefono) }
    var correoContacto by remember { mutableStateOf(centro.correoContacto) }
    var horario by remember { mutableStateOf(centro.horario) }
    var latitud by remember { mutableStateOf(centro.latitud) }
    var longitud by remember { mutableStateOf(centro.longitud) }

    var mostrarSelectorMapa by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var mostrarConfirmacionSensible by remember { mutableStateOf(false) }

    val nombreCambiado = !nombre.trim().equals(centro.nombre.trim(), ignoreCase = true)
    val distanciaMetros = if (centro.latitud != null && centro.longitud != null && latitud != null && longitud != null) {
        val distKm = HaversineUtil.calcularDistanciaKm(centro.latitud, centro.longitud, latitud, longitud)
        if (distKm != null) distKm * 1000.0 else 0.0
    } else 0.0

    val cambioUbicacionSensible = (centro.latitud != null) != (latitud != null) || distanciaMetros > 500.0
    val esSensible = (nombreCambiado || cambioUbicacionSensible) && centro.estado.equals("Aprobado", ignoreCase = true)

    if (mostrarSelectorMapa) {
        DialogoSelectorMapa(
            latitudInicial = latitud,
            longitudInicial = longitud,
            alConfirmarDetallado = { lat, lon, res ->
                latitud = lat
                longitud = lon
                if (res.direccionCompleta.isNotBlank() && !res.direccionCompleta.startsWith("Coordenadas:")) {
                    direccion = res.direccionCompleta
                }
                if (res.distrito.isNotBlank()) {
                    distrito = res.distrito
                }
                mostrarSelectorMapa = false
            },
            alConfirmar = { lat, lon, dist ->
                latitud = lat
                longitud = lon
                if (!dist.isNullOrBlank() && distrito.isBlank()) {
                    distrito = dist
                }
                mostrarSelectorMapa = false
            },
            alDescartar = { mostrarSelectorMapa = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDescartar,
        title = { Text("Editar Perfil Institucional", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (esSensible) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠️ Atención: Has modificado campos sensibles (nombre o ubicación > 500m). Al guardar, tu centro regresará a estado PENDIENTE para nueva revisión administrativa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ℹ️ Modificar teléfono u horario de atención se actualiza de inmediato sin requerir re-aprobación administrativa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del centro *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción *") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección física *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = distrito,
                    onValueChange = { distrito = it },
                    label = { Text("Distrito *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Botón selector de mapa para ubicación exacta
                OutlinedButton(
                    onClick = { mostrarSelectorMapa = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (latitud != null && longitud != null)
                            String.format(Locale.US, "📍 Coordenadas: %.4f, %.4f", latitud, longitud)
                        else
                            "📍 Ubicar en mapa interactivo"
                    )
                }

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono de contacto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = correoContacto,
                    onValueChange = { correoContacto = it },
                    label = { Text("Correo de contacto público *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = horario,
                    onValueChange = { horario = it },
                    label = { Text("Horario de atención *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMensaje != null) {
                    Text(
                        text = errorMensaje!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isBlank() || descripcion.isBlank() || direccion.isBlank() ||
                        distrito.isBlank() || telefono.isBlank() || correoContacto.isBlank() || horario.isBlank()) {
                        errorMensaje = "Todos los campos obligatorios (*) deben completarse."
                        return@Button
                    }
                    if (esSensible) {
                        mostrarConfirmacionSensible = true
                    } else {
                        onGuardar(
                            ActualizarMiCentroRequest(
                                nombre = nombre.trim(),
                                descripcion = descripcion.trim(),
                                direccion = direccion.trim(),
                                distrito = distrito.trim(),
                                telefono = telefono.trim(),
                                correoContacto = correoContacto.trim(),
                                horario = horario.trim(),
                                latitud = latitud,
                                longitud = longitud
                            ),
                            false
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (esSensible) MaterialTheme.colorScheme.error else AdoptTeal
                )
            ) {
                Text(if (esSensible) "Guardar (Pasa a Pendiente)" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDescartar) {
                Text("Cancelar")
            }
        }
    )

    if (mostrarConfirmacionSensible) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionSensible = false },
            title = { Text("¿Confirmar modificación sensible?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Al cambiar el nombre institucional o mover la ubicación más de 500 metros, tu centro perderá temporalmente su aprobación activa y volverá a estado PENDIENTE hasta que el administrador verifique los nuevos datos. ¿Deseas continuar?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionSensible = false
                        onGuardar(
                            ActualizarMiCentroRequest(
                                nombre = nombre.trim(),
                                descripcion = descripcion.trim(),
                                direccion = direccion.trim(),
                                distrito = distrito.trim(),
                                telefono = telefono.trim(),
                                correoContacto = correoContacto.trim(),
                                horario = horario.trim(),
                                latitud = latitud,
                                longitud = longitud
                            ),
                            true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, enviar a re-evaluación")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionSensible = false }) {
                    Text("Volver")
                }
            }
        )
    }
}
