package com.example.adoptme.AdoptMe

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import com.example.adoptme.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.adoptme.AdoptMe.UsuarioRegistrado
import com.example.adoptme.data.remote.ActualizarPerfilRequest
import com.example.adoptme.data.remote.RetrofitClient
import com.example.adoptme.model.AdoptionRequest
import kotlinx.coroutines.launch

@Composable
fun PantallaPerfil(
    solicitudes: List<AdoptionRequest>,
    usuarioActual: UsuarioRegistrado? = null,
    abrirPublicaciones: () -> Unit = {},
    publicarMascota: () -> Unit = {},
    abrirPreferencias: () -> Unit = {},
    cerrarSesion: () -> Unit = {},
    guardarPerfil: (nombre: String, correo: String, telefono: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {

    val nombreInicial = usuarioActual?.nombre?.ifBlank { "Usuario AdoptMe" } ?: "Usuario AdoptMe"
    val correoInicial = usuarioActual?.correo.orEmpty()
    val telefonoInicial = usuarioActual?.telefono?.trim().orEmpty()

    var nombre by remember(usuarioActual) {
        mutableStateOf(nombreInicial)
    }

    var correo by remember(usuarioActual) {
        mutableStateOf(correoInicial)
    }

    var telefono by remember(usuarioActual) {
        mutableStateOf(telefonoInicial)
    }

    var estaEditando by remember {
        mutableStateOf(false)
    }

    var mostrarMensaje by remember {
        mutableStateOf(false)
    }

    var mensajeErrorPerfil by remember {
        mutableStateOf("")
    }

    val coroutineScope = rememberCoroutineScope()
    var guardando by remember {
        mutableStateOf(false)
    }

    var errorNombre by remember {
        mutableStateOf<String?>(null)
    }

    var errorTelefono by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {

        Text(
            text = "👤 Mi Perfil",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (!estaEditando) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface
                ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                painter = painterResource(id = R.drawable.ic_person),
                                contentDescription = "Avatar de usuario",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(
                            modifier =
                                Modifier.padding(start = 16.dp)
                        ) {

                            Text(
                                text = nombre,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Rol: ${usuarioActual?.rol?.ifBlank { "Adoptante" } ?: "Adoptante"}",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "Correo electrónico",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )

                    Text(
                        text = correo.ifBlank { "Sin correo registrado" },
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = "Teléfono de contacto",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )

                    Text(
                        text = if (telefono.isBlank()) "Sin teléfono registrado" else telefono,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = {
                    estaEditando = true
                    mostrarMensaje = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )
                    Text(
                        text = "Editar perfil",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Button(
                onClick = publicarMascota,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_paw),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )
                    Text(
                        text = "Publicar mascota",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // NUEVO BOTÓN
            OutlinedButton(
                onClick = abrirPublicaciones,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_paw),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )
                    Text(
                        text = "Mis publicaciones",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )
            OutlinedButton(
                onClick = abrirPreferencias,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )
                    Text(
                        text = "Preferencias",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )
            OutlinedButton(
                onClick = cerrarSesion,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logout),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )
                    Text(
                        text = "Cerrar sesión",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Text(
                text = "🐾 Mis solicitudes de adopción",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (solicitudes.isEmpty()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                        )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "📋",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Todavía no tienes solicitudes de adopción registradas.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }

            } else {

                solicitudes.forEach { solicitud ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape =
                            RoundedCornerShape(14.dp),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 1.dp
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(16.dp)
                        ) {

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                val titulo =
                                    if (
                                        solicitud
                                            .petSpecies
                                            .isNotBlank()
                                    ) {

                                        "🐾 ${solicitud.petName} (${solicitud.petSpecies})"

                                    } else {

                                        "🐾 ${solicitud.petName}"
                                    }

                                Text(
                                    text = titulo,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                InsigniaEstado(
                                    estado =
                                        solicitud.status
                                )
                            }

                            if (
                                solicitud
                                    .applicantName
                                    .isNotBlank()
                            ) {

                                Text(
                                    text =
                                        "Postulante: ${solicitud.applicantName}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    modifier =
                                        Modifier.padding(
                                            top = 6.dp
                                        )
                                )
                            }
                        }
                    }
                }
            }

        } else {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Editar Datos del Perfil",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.padding(
                                bottom = 14.dp
                            )
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = {
                            nombre = it
                            errorNombre = null
                        },
                        label = {
                            Text("Nombre completo")
                        },
                        isError = errorNombre != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorNombre != null) {
                        Text(
                            text = errorNombre!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = correoInicial,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = {
                            Text("Correo electrónico")
                        },
                        supportingText = {
                            Text("El correo identifica tu cuenta y no puede modificarse.")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = {
                            telefono = it
                            errorTelefono = null
                        },
                        label = {
                            Text("Teléfono celular")
                        },
                        isError = errorTelefono != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorTelefono != null) {
                        Text(
                            text = errorTelefono!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    if (mensajeErrorPerfil.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "⚠️ $mensajeErrorPerfil",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )

                    Button(
                        onClick = {
                            val nombreLimpio = nombre.trim()
                            val telefonoLimpio = telefono.trim()
                            var hayError = false

                            if (nombreLimpio.isBlank()) {
                                errorNombre = "El nombre completo no puede estar vacío."
                                hayError = true
                            } else if (nombreLimpio.length < 2) {
                                errorNombre = "El nombre debe tener al menos 2 caracteres."
                                hayError = true
                            }

                            if (telefonoLimpio.isNotBlank() && !esTelefonoPeruanoValido(telefonoLimpio)) {
                                errorTelefono = "El teléfono debe tener 9 dígitos y empezar con 9."
                                hayError = true
                            }

                            if (!hayError && !guardando) {
                                guardando = true
                                mensajeErrorPerfil = ""
                                errorNombre = null
                                errorTelefono = null

                                coroutineScope.launch {
                                    try {
                                        val telParaEnviar = if (telefonoLimpio.isNotBlank()) normalizarTelefonoPeruano(telefonoLimpio) else ""
                                        val respuesta = RetrofitClient.usuariosApi.actualizarPerfil(
                                            ActualizarPerfilRequest(
                                                nombre = nombreLimpio,
                                                telefono = telParaEnviar
                                            )
                                        )

                                        if (respuesta.isSuccessful) {
                                            val perfilActualizado = respuesta.body()
                                            if (perfilActualizado != null) {
                                                nombre = perfilActualizado.nombre
                                                telefono = perfilActualizado.telefono
                                                guardarPerfil(
                                                    perfilActualizado.nombre,
                                                    perfilActualizado.correo,
                                                    perfilActualizado.telefono
                                                )
                                                estaEditando = false
                                                mostrarMensaje = true
                                                mensajeErrorPerfil = ""
                                            } else {
                                                mensajeErrorPerfil = "El servidor no devolvió los datos actualizados."
                                            }
                                        } else {
                                            mensajeErrorPerfil = "No se pudo actualizar el perfil. Código de error: ${respuesta.code()}"
                                        }
                                    } catch (e: Exception) {
                                        mensajeErrorPerfil = "Error de conexión con el servidor. Revisa tu conexión e inténtalo nuevamente."
                                    } finally {
                                        guardando = false
                                    }
                                }
                            }
                        },
                        enabled = !guardando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (guardando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Guardando...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Guardar cambios",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            nombre = nombreInicial
                            telefono = telefonoInicial
                            errorNombre = null
                            errorTelefono = null
                            mensajeErrorPerfil = ""
                            estaEditando = false
                        },
                        enabled = !guardando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }

        if (mostrarMensaje) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    )
            ) {

                Text(
                    text =
                        "✅ Perfil actualizado correctamente",
                    modifier =
                        Modifier.padding(12.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}