package com.example.adoptme.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.R
import com.example.adoptme.model.ActualizarMiCentroRequest
import com.example.adoptme.model.CentroPropietarioDto
import com.example.adoptme.viewmodel.MiCentroUiState
import com.example.adoptme.viewmodel.MiCentroViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEstadoCentro(
    viewModel: MiCentroViewModel,
    onNavegarAPanelCentro: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val estaOperando by viewModel.estaOperando.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var mostrarDialogoReenvio by remember { mutableStateOf(false) }
    var mostrarDialogoEdicion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.cargarMiCentro()
    }

    // Redirección reactiva si el centro pasa a estar Aprobado y Activo
    LaunchedEffect(uiState) {
        if (uiState is MiCentroUiState.Exito) {
            val centro = (uiState as MiCentroUiState.Exito).centro
            if (centro.estado.equals("Aprobado", ignoreCase = true) && centro.activo) {
                onNavegarAPanelCentro()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Estado de tu Centro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    TextButton(onClick = onCerrarSesion) {
                        Text(
                            text = "Cerrar sesión",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Consultando el estado de tu centro...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MiCentroUiState.Error -> {
                    ContenidoError(
                        mensaje = state.mensaje,
                        codigoHttp = state.codigoHttp,
                        estado = state.estado,
                        onReintentar = { viewModel.cargarMiCentro() },
                        onCerrarSesion = onCerrarSesion
                    )
                }
                is MiCentroUiState.Exito -> {
                    ContenidoCentroPorEstado(
                        centro = state.centro,
                        estaOperando = estaOperando,
                        onVerificarEstado = { viewModel.cargarMiCentro() },
                        onSolicitarReenvio = { mostrarDialogoReenvio = true },
                        onEditarPerfil = { mostrarDialogoEdicion = true },
                        onCerrarSesion = onCerrarSesion
                    )

                    if (mostrarDialogoReenvio) {
                        AlertDialog(
                            onDismissRequest = { mostrarDialogoReenvio = false },
                            title = { Text("Reenviar solicitud a revisión") },
                            text = {
                                Text(
                                    "¿Confirmas que has corregido las observaciones señaladas por el Administrador y deseas someter tu perfil a una nueva revisión?"
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        mostrarDialogoReenvio = false
                                        viewModel.reenviarSolicitud { exito, mensaje ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(mensaje)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Reenviar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { mostrarDialogoReenvio = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    if (mostrarDialogoEdicion) {
                        DialogoEditarMiCentro(
                            centro = state.centro,
                            onDescartar = { mostrarDialogoEdicion = false },
                            onGuardar = { request ->
                                mostrarDialogoEdicion = false
                                viewModel.actualizarPerfil(request) { exito, mensaje, _ ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(mensaje)
                                    }
                                }
                            }
                        )
                    }
                }
                is MiCentroUiState.Inactivo -> {
                    // Estado inicial transitorio mientras carga
                }
            }
        }
    }
}

@Composable
private fun ContenidoCentroPorEstado(
    centro: CentroPropietarioDto,
    estaOperando: Boolean,
    onVerificarEstado: () -> Unit,
    onSolicitarReenvio: () -> Unit,
    onEditarPerfil: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado con nombre del centro
        Text(
            text = centro.nombre,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Distrito: ${centro.distrito.ifBlank { "Sin especificar" }}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        when {
            centro.estado.equals("Pendiente", ignoreCase = true) -> {
                TarjetaEstadoPendiente(
                    centro = centro,
                    onEditarPerfil = onEditarPerfil
                )
            }
            centro.estado.equals("Rechazado", ignoreCase = true) -> {
                TarjetaEstadoRechazado(
                    centro = centro,
                    estaOperando = estaOperando,
                    onEditarPerfil = onEditarPerfil,
                    onReenviar = onSolicitarReenvio
                )
            }
            centro.estado.equals("Aprobado", ignoreCase = true) && !centro.activo -> {
                TarjetaEstadoInactivo(
                    centro = centro
                )
            }
            else -> {
                TarjetaEstadoGeneral(centro = centro)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de comprobación manual del estado en el backend
        OutlinedButton(
            onClick = onVerificarEstado,
            enabled = !estaOperando,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_notifications),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Actualizar estado desde el servidor")
        }

        TextButton(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cerrar sesión",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TarjetaEstadoPendiente(
    centro: CentroPropietarioDto,
    onEditarPerfil: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⏳", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Solicitud Pendiente de Revisión",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFB45309)
                    )
                    Text(
                        text = "Estado: En evaluación administrativa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Tu solicitud institucional ha sido recibida con éxito y se encuentra en proceso de revisión por el equipo administrativo de AdoptMe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Durante este período de verificación, las funciones operativas de publicación de mascotas y visibilidad pública permanecerán temporalmente bloqueadas para salvaguardar la confianza de la comunidad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!centro.fechaSolicitud.isNullOrBlank()) {
                Text(
                    text = "Fecha de registro: ${centro.fechaSolicitud}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onEditarPerfil,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Corregir o completar información institucional")
            }
        }
    }
}

@Composable
private fun TarjetaEstadoRechazado(
    centro: CentroPropietarioDto,
    estaOperando: Boolean,
    onEditarPerfil: () -> Unit,
    onReenviar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("❌", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Solicitud Rechazada",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Se requieren correcciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text(
                text = "El Administrador revisó tu solicitud y requiere que subsanes las siguientes observaciones antes de autorizar la apertura operativa del centro:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Motivo del Administrador obligatorio
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Motivo de la observación:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = centro.motivoRechazo ?: "Sin observaciones específicas detalladas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditarPerfil,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Editar perfil")
                }

                Button(
                    onClick = onReenviar,
                    enabled = !estaOperando,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Reenviar")
                }
            }
        }
    }
}

@Composable
private fun TarjetaEstadoInactivo(
    centro: CentroPropietarioDto
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF64748B).copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Centro Temporalmente Desactivado",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF334155)
            )
            Text(
                text = "Tu centro se encuentra en estado Aprobado pero ha sido desactivado temporalmente por la administración de la plataforma.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Durante la suspensión, tus publicaciones se encuentran ocultas del catálogo público y no es posible registrar nuevas mascotas. Por favor, comunícate con soporte o el administrador institucional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaEstadoGeneral(
    centro: CentroPropietarioDto
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Estado: ${centro.estado}",
                fontWeight = FontWeight.Bold
            )
            Text(text = "Activo: ${if (centro.activo) "Sí" else "No"}")
            Text(text = "Dirección: ${centro.direccion}")
            Text(text = "Teléfono: ${centro.telefono}")
        }
    }
}

@Composable
private fun ContenidoError(
    mensaje: String,
    codigoHttp: Int?,
    estado: String?,
    onReintentar: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (estado == "SinPerfil") "Cuenta sin centro asignado" else "Error al verificar centro",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (estado != "SinPerfil") {
            Button(
                onClick = onReintentar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reintentar")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
fun DialogoEditarMiCentro(
    centro: CentroPropietarioDto,
    onDescartar: () -> Unit,
    onGuardar: (ActualizarMiCentroRequest) -> Unit
) {
    var nombre by remember { mutableStateOf(centro.nombre) }
    var descripcion by remember { mutableStateOf(centro.descripcion) }
    var direccion by remember { mutableStateOf(centro.direccion) }
    var distrito by remember { mutableStateOf(centro.distrito) }
    var telefono by remember { mutableStateOf(centro.telefono) }
    var correoContacto by remember { mutableStateOf(centro.correoContacto) }
    var horario by remember { mutableStateOf(centro.horario) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    val nombreOriginal = centro.nombre.trim()
    val nombreCambiado = !nombre.trim().equals(nombreOriginal, ignoreCase = true)
    val esAprobado = centro.estado.equals("Aprobado", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDescartar,
        title = {
            Text(
                text = "Editar Perfil Institucional",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (esAprobado && nombreCambiado) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠️ Advertencia: Modificar el nombre institucional de un centro aprobado regresará tu solicitud a estado Pendiente para nueva revisión del Administrador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                androidx.compose.material3.OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del centro *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción institucional *") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
                    value = distrito,
                    onValueChange = { distrito = it },
                    label = { Text("Distrito *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono de contacto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
                    value = correoContacto,
                    onValueChange = { correoContacto = it },
                    label = { Text("Correo de contacto público *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
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
                    onGuardar(
                        ActualizarMiCentroRequest(
                            nombre = nombre.trim(),
                            descripcion = descripcion.trim(),
                            direccion = direccion.trim(),
                            distrito = distrito.trim(),
                            telefono = telefono.trim(),
                            correoContacto = correoContacto.trim(),
                            horario = horario.trim(),
                            latitud = centro.latitud,
                            longitud = centro.longitud
                        )
                    )
                }
            ) {
                Text("Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDescartar) {
                Text("Cancelar")
            }
        }
    )
}
