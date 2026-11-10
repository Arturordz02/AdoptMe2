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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.R
import com.example.adoptme.api.RegistroUsuarioRequest
import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.components.DialogoExplicacionUbicacion
import com.example.adoptme.components.DialogoSelectorMapa
import com.example.adoptme.model.RegistroCentroRequest
import com.example.adoptme.model.RegistroCentroResponse
import com.example.adoptme.model.UsuarioRegistrado
import com.example.adoptme.preferences.Preferencias
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealDark
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary
import com.example.adoptme.ui.theme.BorderWarm
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class TipoRegistro {
    ADOPTANTE,
    CENTRO
}

fun normalizarTelefonoPeruano(telefono: String): String {
    return telefono.replace("\\s+".toRegex(), "").trim()
}

fun esTelefonoPeruanoValido(telefono: String): Boolean {
    val normalizado = normalizarTelefonoPeruano(telefono)
    return Regex("^9\\d{8}$").matches(normalizado)
}

fun esFormatoCorreoValido(correo: String): Boolean {
    val regex = Regex("^[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$")
    return regex.matches(correo.trim())
}

@Composable
fun PantallaRegistro(
    registrarUsuario: (UsuarioRegistrado) -> Unit,
    volverAlLogin: () -> Unit,
    usuariosExistentes: List<UsuarioRegistrado> = emptyList(),
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val preferencias = remember { Preferencias(contexto) }
    val coroutineScope = rememberCoroutineScope()

    var tipoSeleccionado by remember { mutableStateOf(TipoRegistro.ADOPTANTE) }

    // --- Campos de Adoptante ---
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }
    var confirmarContrasenaVisible by remember { mutableStateOf(false) }

    var errorNombre by remember { mutableStateOf("") }
    var errorCorreo by remember { mutableStateOf("") }
    var errorTelefono by remember { mutableStateOf("") }
    var errorContrasena by remember { mutableStateOf("") }
    var errorConfirmarContrasena by remember { mutableStateOf("") }
    var errorTerminos by remember { mutableStateOf("") }
    var terminosAceptados by remember { mutableStateOf(false) }

    // --- Campos de Centro de Adopción ---
    var nombreCentro by remember { mutableStateOf("") }
    var descripcionCentro by remember { mutableStateOf("") }
    var direccionCentro by remember { mutableStateOf("") }
    var distritoCentro by remember { mutableStateOf("") }
    var telefonoCentro by remember { mutableStateOf("") }
    var correoContactoCentro by remember { mutableStateOf("") }
    var horarioCentro by remember { mutableStateOf("") }
    var latitudCentro by remember { mutableStateOf<Double?>(null) }
    var longitudCentro by remember { mutableStateOf<Double?>(null) }

    var nombreResponsable by remember { mutableStateOf("") }
    var correoResponsable by remember { mutableStateOf("") }
    var telefonoResponsable by remember { mutableStateOf("") }
    var contrasenaCentro by remember { mutableStateOf("") }
    var confirmarContrasenaCentro by remember { mutableStateOf("") }
    var contrasenaCentroVisible by remember { mutableStateOf(false) }
    var confirmarContrasenaCentroVisible by remember { mutableStateOf(false) }

    var errorNombreCentro by remember { mutableStateOf("") }
    var errorDescripcionCentro by remember { mutableStateOf("") }
    var errorDireccionCentro by remember { mutableStateOf("") }
    var errorDistritoCentro by remember { mutableStateOf("") }
    var errorTelefonoCentro by remember { mutableStateOf("") }
    var errorCorreoContactoCentro by remember { mutableStateOf("") }
    var errorHorarioCentro by remember { mutableStateOf("") }
    var errorNombreResp by remember { mutableStateOf("") }
    var errorCorreoResp by remember { mutableStateOf("") }
    var errorContrasenaCentro by remember { mutableStateOf("") }
    var errorConfirmarContrasenaCentro by remember { mutableStateOf("") }
    var errorTerminosCentro by remember { mutableStateOf("") }
    var terminosAceptadosCentro by remember { mutableStateOf(false) }

    // Diálogos pedagógicos y auxiliares
    var mostrarDialogoTerminos by remember { mutableStateOf(false) }
    var mostrarDialogoExplicacionUbicacion by remember { mutableStateOf(false) }
    var mostrarSelectorMapa by remember { mutableStateOf(false) }

    var registrando by remember { mutableStateOf(false) }
    var errorServidor by remember { mutableStateOf("") }

    var mostrarDialogoExitoAdoptante by remember { mutableStateOf(false) }
    var usuarioCreadoParaRetorno by remember { mutableStateOf<UsuarioRegistrado?>(null) }

    var mostrarDialogoExitoCentro by remember { mutableStateOf(false) }
    var resultadoRegistroCentro by remember { mutableStateOf<RegistroCentroResponse?>(null) }

    fun validarYRegistrarAdoptante() {
        var esValido = true
        val nombreLimpio = nombre.trim()
        val correoLimpio = correo.trim()
        val telefonoLimpio = telefono.trim()
        val telefonoNormalizado = normalizarTelefonoPeruano(telefono)

        if (nombreLimpio.isEmpty()) {
            errorNombre = "El nombre no puede estar vacío."
            esValido = false
        } else {
            errorNombre = ""
        }

        when {
            correoLimpio.isEmpty() -> {
                errorCorreo = "El correo no puede estar vacío."
                esValido = false
            }
            !esFormatoCorreoValido(correoLimpio) -> {
                errorCorreo = "El correo debe tener un formato válido (ej. usuario@correo.com)."
                esValido = false
            }
            correoLimpio.equals("usuario@adoptme.com", ignoreCase = true) ||
                    correoLimpio.equals("admin@adoptme.com", ignoreCase = true) -> {
                errorCorreo = "No se puede utilizar este correo predeterminado."
                esValido = false
            }
            usuariosExistentes.any { it.correo.equals(correoLimpio, ignoreCase = true) } -> {
                errorCorreo = "Este correo ya está registrado."
                esValido = false
            }
            else -> {
                errorCorreo = ""
            }
        }

        when {
            telefonoLimpio.isEmpty() -> {
                errorTelefono = "El teléfono celular no puede estar vacío."
                esValido = false
            }
            !telefono.all { it.isDigit() || it.isWhitespace() } -> {
                errorTelefono = "El teléfono solo puede contener números."
                esValido = false
            }
            !telefonoNormalizado.startsWith("9") -> {
                errorTelefono = "El teléfono debe comenzar con 9."
                esValido = false
            }
            telefonoNormalizado.length != 9 -> {
                errorTelefono = "El teléfono debe tener exactamente 9 dígitos."
                esValido = false
            }
            else -> {
                errorTelefono = ""
            }
        }

        when {
            contrasena.isEmpty() -> {
                errorContrasena = "La contraseña no puede estar vacía."
                esValido = false
            }
            contrasena.length < 6 -> {
                errorContrasena = "La contraseña debe tener mínimo 6 caracteres."
                esValido = false
            }
            else -> {
                errorContrasena = ""
            }
        }

        when {
            confirmarContrasena.isEmpty() -> {
                errorConfirmarContrasena = "La confirmación no puede estar vacía."
                esValido = false
            }
            contrasena != confirmarContrasena -> {
                errorConfirmarContrasena = "La contraseña y la confirmación deben coincidir."
                esValido = false
            }
            else -> {
                errorConfirmarContrasena = ""
            }
        }

        if (!terminosAceptados) {
            errorTerminos = "Debes aceptar los Términos y la Política de Privacidad."
            esValido = false
        } else {
            errorTerminos = ""
        }

        if (esValido && !registrando) {
            registrando = true
            errorServidor = ""

            coroutineScope.launch {
                try {
                    val respuesta = RetrofitClient.usuariosApi.registrarUsuario(
                        RegistroUsuarioRequest(
                            nombre = nombreLimpio,
                            correo = correoLimpio,
                            contrasena = contrasena,
                            telefono = telefonoNormalizado
                        )
                    )

                    if (respuesta.isSuccessful) {
                        val nuevoUsuario = UsuarioRegistrado(
                            nombre = nombreLimpio,
                            correo = correoLimpio,
                            contrasena = contrasena,
                            telefono = telefonoNormalizado,
                            rol = "Adoptante"
                        )
                        usuarioCreadoParaRetorno = nuevoUsuario
                        preferencias.guardarAceptacionTerminos(correoLimpio)
                        mostrarDialogoExitoAdoptante = true
                    } else if (respuesta.code() == 409) {
                        errorServidor = "Este correo ya está registrado en el sistema."
                    } else {
                        errorServidor = "No se pudo completar el registro (HTTP ${respuesta.code()})."
                    }
                } catch (e: Exception) {
                    errorServidor = RetrofitClient.resolverMensajeErrorServidor(e)
                } finally {
                    registrando = false
                }
            }
        }
    }

    fun validarYRegistrarCentro() {
        var esValido = true
        val ncLimpio = nombreCentro.trim()
        val descLimpio = descripcionCentro.trim()
        val dirLimpio = direccionCentro.trim()
        val distLimpio = distritoCentro.trim()
        val telCentroLimpio = telefonoCentro.trim()
        val correoContLimpio = correoContactoCentro.trim()
        val horarioLimpio = horarioCentro.trim()

        val respLimpio = nombreResponsable.trim()
        val correoRespLimpio = correoResponsable.trim()
        val telRespLimpio = telefonoResponsable.trim()

        // 1. Institucionales
        if (ncLimpio.isEmpty()) {
            errorNombreCentro = "El nombre del centro es obligatorio."
            esValido = false
        } else errorNombreCentro = ""

        if (descLimpio.isEmpty()) {
            errorDescripcionCentro = "La descripción institucional es obligatoria."
            esValido = false
        } else errorDescripcionCentro = ""

        if (dirLimpio.isEmpty()) {
            errorDireccionCentro = "La dirección física es obligatoria."
            esValido = false
        } else errorDireccionCentro = ""

        if (distLimpio.isEmpty()) {
            errorDistritoCentro = "El distrito es obligatorio."
            esValido = false
        } else errorDistritoCentro = ""

        if (telCentroLimpio.isEmpty()) {
            errorTelefonoCentro = "El teléfono del centro es obligatorio."
            esValido = false
        } else errorTelefonoCentro = ""

        if (correoContLimpio.isEmpty() || !esFormatoCorreoValido(correoContLimpio)) {
            errorCorreoContactoCentro = "Ingresa un correo de contacto público válido."
            esValido = false
        } else errorCorreoContactoCentro = ""

        if (horarioLimpio.isEmpty()) {
            errorHorarioCentro = "El horario de atención es obligatorio."
            esValido = false
        } else errorHorarioCentro = ""

        // 2. Responsable
        if (respLimpio.isEmpty()) {
            errorNombreResp = "El nombre del responsable es obligatorio."
            esValido = false
        } else errorNombreResp = ""

        if (correoRespLimpio.isEmpty() || !esFormatoCorreoValido(correoRespLimpio)) {
            errorCorreoResp = "Ingresa un correo de acceso válido para el responsable."
            esValido = false
        } else errorCorreoResp = ""

        // 3. Contraseñas
        if (contrasenaCentro.length < 6) {
            errorContrasenaCentro = "La contraseña debe tener mínimo 6 caracteres."
            esValido = false
        } else errorContrasenaCentro = ""

        if (contrasenaCentro != confirmarContrasenaCentro) {
            errorConfirmarContrasenaCentro = "Las contraseñas no coinciden."
            esValido = false
        } else errorConfirmarContrasenaCentro = ""

        // 4. Términos
        if (!terminosAceptadosCentro) {
            errorTerminosCentro = "Debes aceptar los Términos y Condiciones para solicitar el registro."
            esValido = false
        } else errorTerminosCentro = ""

        if (esValido && !registrando) {
            registrando = true
            errorServidor = ""

            coroutineScope.launch {
                try {
                    val request = RegistroCentroRequest(
                        nombreResponsable = respLimpio,
                        correo = correoRespLimpio,
                        contrasena = contrasenaCentro,
                        telefonoResponsable = telRespLimpio.ifBlank { null },
                        nombreCentro = ncLimpio,
                        descripcion = descLimpio,
                        direccion = dirLimpio,
                        distrito = distLimpio,
                        telefonoCentro = telCentroLimpio,
                        correoContacto = correoContLimpio,
                        horario = horarioLimpio,
                        latitud = latitudCentro,
                        longitud = longitudCentro,
                        aceptacionTerminos = true
                    )

                    val respuesta = RetrofitClient.usuariosApi.registrarCentro(request)

                    if (respuesta.isSuccessful && respuesta.body() != null) {
                        resultadoRegistroCentro = respuesta.body()
                        preferencias.guardarAceptacionTerminos(correoRespLimpio)
                        mostrarDialogoExitoCentro = true
                    } else {
                        val cuerpoError = respuesta.errorBody()?.string()
                        var mensaje = "No se pudo completar la solicitud de centro (${respuesta.code()})."
                        if (!cuerpoError.isNullOrBlank()) {
                            try {
                                val json = JSONObject(cuerpoError)
                                if (json.has("mensaje")) mensaje = json.getString("mensaje")
                            } catch (_: Exception) { }
                        }
                        errorServidor = mensaje
                    }
                } catch (e: Exception) {
                    errorServidor = RetrofitClient.resolverMensajeErrorServidor(e)
                } finally {
                    registrando = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdoptMeBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HEADER CON DEGRADADO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(AdoptOrange, AdoptTeal),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .padding(top = 28.dp, bottom = 40.dp, start = 20.dp, end = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.adoptme_logo_original),
                    contentDescription = "Logo de AdoptMe",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Únete a AdoptMe",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (tipoSeleccionado == TipoRegistro.ADOPTANTE)
                        "Crea tu cuenta de Adoptante para encontrar a tu nuevo compañero"
                    else
                        "Registra tu Centro de Adopción para publicar mascotas y recibir solicitudes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // TARJETA PRINCIPAL BLANCA
        Card(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .offset(y = (-20).dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tipo de Cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // SELECTOR EXCLUSIVO: ADOPTANTE O CENTRO (NUNCA ADMIN)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tipoSeleccionado = TipoRegistro.ADOPTANTE },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (tipoSeleccionado == TipoRegistro.ADOPTANTE)
                                AdoptTeal.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (tipoSeleccionado == TipoRegistro.ADOPTANTE)
                            BorderStroke(2.dp, AdoptTeal)
                        else null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👤", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Adoptante",
                                fontWeight = FontWeight.Bold,
                                color = if (tipoSeleccionado == TipoRegistro.ADOPTANTE) AdoptTealDark else AdoptTextDark
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tipoSeleccionado = TipoRegistro.CENTRO },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (tipoSeleccionado == TipoRegistro.CENTRO)
                                AdoptOrange.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (tipoSeleccionado == TipoRegistro.CENTRO)
                            BorderStroke(2.dp, AdoptOrange)
                        else null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏡", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Centro de Adopción",
                                fontWeight = FontWeight.Bold,
                                color = if (tipoSeleccionado == TipoRegistro.CENTRO) AdoptOrange else AdoptTextDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // FORMULARIO CONDICIONAL SEGÚN TIPO
                if (tipoSeleccionado == TipoRegistro.ADOPTANTE) {
                    // --- FORMULARIO DE ADOPTANTE ---
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it; if (errorNombre.isNotEmpty()) errorNombre = "" },
                        label = { Text("Nombre completo *") },
                        placeholder = { Text("Ej. Lucía Quispe") },
                        isError = errorNombre.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorNombre.isNotEmpty()) {
                        Text(errorNombre, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it; if (errorCorreo.isNotEmpty()) errorCorreo = "" },
                        label = { Text("Correo electrónico *") },
                        placeholder = { Text("ejemplo@correo.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = errorCorreo.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorCorreo.isNotEmpty()) {
                        Text(errorCorreo, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it; if (errorTelefono.isNotEmpty()) errorTelefono = "" },
                        label = { Text("Teléfono celular (9 dígitos) *") },
                        placeholder = { Text("912345678") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = errorTelefono.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorTelefono.isNotEmpty()) {
                        Text(errorTelefono, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = contrasena,
                        onValueChange = { contrasena = it; if (errorContrasena.isNotEmpty()) errorContrasena = "" },
                        label = { Text("Contraseña *") },
                        visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                                Icon(
                                    painter = painterResource(id = if (contrasenaVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
                                    contentDescription = null
                                )
                            }
                        },
                        isError = errorContrasena.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorContrasena.isNotEmpty()) {
                        Text(errorContrasena, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmarContrasena,
                        onValueChange = { confirmarContrasena = it; if (errorConfirmarContrasena.isNotEmpty()) errorConfirmarContrasena = "" },
                        label = { Text("Confirmar contraseña *") },
                        visualTransformation = if (confirmarContrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmarContrasenaVisible = !confirmarContrasenaVisible }) {
                                Icon(
                                    painter = painterResource(id = if (confirmarContrasenaVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
                                    contentDescription = null
                                )
                            }
                        },
                        isError = errorConfirmarContrasena.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorConfirmarContrasena.isNotEmpty()) {
                        Text(errorConfirmarContrasena, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Términos y privacidad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = terminosAceptados,
                            onCheckedChange = { terminosAceptados = it; if (it) errorTerminos = "" }
                        )
                        Text(
                            text = "Acepto los Términos y la Política de Privacidad",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mostrarDialogoTerminos = true }
                        )
                    }
                    if (errorTerminos.isNotEmpty()) {
                        Text(errorTerminos, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { validarYRegistrarAdoptante() },
                        enabled = !registrando,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (registrando) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Crear Cuenta de Adoptante", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // --- FORMULARIO DE CENTRO DE ADOPCIÓN ---
                    Surface(
                        color = AdoptOrange.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Las solicitudes de centros se registran en estado Pendiente. Un Administrador revisará los datos institucionales antes de su aprobación.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdoptTextDark,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("1. Datos Institucionales del Centro", fontWeight = FontWeight.Bold, color = AdoptTextDark, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nombreCentro,
                        onValueChange = { nombreCentro = it; if (errorNombreCentro.isNotEmpty()) errorNombreCentro = "" },
                        label = { Text("Nombre del centro o albergue *") },
                        placeholder = { Text("Ej. Albergue San Francisco") },
                        isError = errorNombreCentro.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorNombreCentro.isNotEmpty()) Text(errorNombreCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descripcionCentro,
                        onValueChange = { descripcionCentro = it; if (errorDescripcionCentro.isNotEmpty()) errorDescripcionCentro = "" },
                        label = { Text("Descripción del centro y misión *") },
                        isError = errorDescripcionCentro.isNotEmpty(),
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorDescripcionCentro.isNotEmpty()) Text(errorDescripcionCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = direccionCentro,
                        onValueChange = { direccionCentro = it; if (errorDireccionCentro.isNotEmpty()) errorDireccionCentro = "" },
                        label = { Text("Dirección física *") },
                        isError = errorDireccionCentro.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorDireccionCentro.isNotEmpty()) Text(errorDireccionCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = distritoCentro,
                        onValueChange = { distritoCentro = it; if (errorDistritoCentro.isNotEmpty()) errorDistritoCentro = "" },
                        label = { Text("Distrito *") },
                        placeholder = { Text("Ej. Santiago de Surco") },
                        isError = errorDistritoCentro.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorDistritoCentro.isNotEmpty()) Text(errorDistritoCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = telefonoCentro,
                        onValueChange = { telefonoCentro = it; if (errorTelefonoCentro.isNotEmpty()) errorTelefonoCentro = "" },
                        label = { Text("Teléfono de contacto del centro *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = errorTelefonoCentro.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorTelefonoCentro.isNotEmpty()) Text(errorTelefonoCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = correoContactoCentro,
                        onValueChange = { correoContactoCentro = it; if (errorCorreoContactoCentro.isNotEmpty()) errorCorreoContactoCentro = "" },
                        label = { Text("Correo público de contacto *") },
                        placeholder = { Text("contacto@albergue.org") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = errorCorreoContactoCentro.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorCorreoContactoCentro.isNotEmpty()) Text(errorCorreoContactoCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = horarioCentro,
                        onValueChange = { horarioCentro = it; if (errorHorarioCentro.isNotEmpty()) errorHorarioCentro = "" },
                        label = { Text("Horario de atención *") },
                        placeholder = { Text("Lunes a Sábado 9:00 - 18:00") },
                        isError = errorHorarioCentro.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorHorarioCentro.isNotEmpty()) Text(errorHorarioCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    // Selector de Coordenadas en Mapa con Jerarquía Visual
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "📍 Ubicación geográfica",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (latitudCentro != null && longitudCentro != null)
                                            String.format(java.util.Locale.US, "Coordenadas fijadas: %.4f, %.4f", latitudCentro, longitudCentro)
                                        else
                                            "Selecciona en el mapa para autocompletar dirección y distrito.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { mostrarDialogoExplicacionUbicacion = true }
                                ) {
                                    Text(
                                        text = if (latitudCentro != null) "Cambiar" else "Abrir mapa",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("2. Datos de Acceso del Responsable", fontWeight = FontWeight.Bold, color = AdoptTextDark, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nombreResponsable,
                        onValueChange = { nombreResponsable = it; if (errorNombreResp.isNotEmpty()) errorNombreResp = "" },
                        label = { Text("Nombre del responsable *") },
                        isError = errorNombreResp.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorNombreResp.isNotEmpty()) Text(errorNombreResp, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = correoResponsable,
                        onValueChange = { correoResponsable = it; if (errorCorreoResp.isNotEmpty()) errorCorreoResp = "" },
                        label = { Text("Correo de acceso a la cuenta *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = errorCorreoResp.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorCorreoResp.isNotEmpty()) Text(errorCorreoResp, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = telefonoResponsable,
                        onValueChange = { telefonoResponsable = it },
                        label = { Text("Teléfono personal del responsable (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contrasenaCentro,
                        onValueChange = { contrasenaCentro = it; if (errorContrasenaCentro.isNotEmpty()) errorContrasenaCentro = "" },
                        label = { Text("Contraseña de acceso *") },
                        visualTransformation = if (contrasenaCentroVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { contrasenaCentroVisible = !contrasenaCentroVisible }) {
                                Icon(painter = painterResource(id = if (contrasenaCentroVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off), contentDescription = null)
                            }
                        },
                        isError = errorContrasenaCentro.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorContrasenaCentro.isNotEmpty()) Text(errorContrasenaCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmarContrasenaCentro,
                        onValueChange = { confirmarContrasenaCentro = it; if (errorConfirmarContrasenaCentro.isNotEmpty()) errorConfirmarContrasenaCentro = "" },
                        label = { Text("Confirmar contraseña *") },
                        visualTransformation = if (confirmarContrasenaCentroVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmarContrasenaCentroVisible = !confirmarContrasenaCentroVisible }) {
                                Icon(painter = painterResource(id = if (confirmarContrasenaCentroVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off), contentDescription = null)
                            }
                        },
                        isError = errorConfirmarContrasenaCentro.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorConfirmarContrasenaCentro.isNotEmpty()) Text(errorConfirmarContrasenaCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = terminosAceptadosCentro,
                            onCheckedChange = { terminosAceptadosCentro = it; if (it) errorTerminosCentro = "" }
                        )
                        Text(
                            text = "Acepto los Términos de la Plataforma y la Política de Centros",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mostrarDialogoTerminos = true }
                        )
                    }
                    if (errorTerminosCentro.isNotEmpty()) {
                        Text(errorTerminosCentro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { validarYRegistrarCentro() },
                        enabled = !registrando,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (registrando) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Enviar Solicitud de Centro", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (errorServidor.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorServidor,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = volverAlLogin,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, AdoptTeal),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = AdoptTeal),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("¿Ya tienes cuenta? Iniciar sesión", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // DIÁLOGOS MODALES

    // 1. Éxito Adoptante
    if (mostrarDialogoExitoAdoptante && usuarioCreadoParaRetorno != null) {
        val u = usuarioCreadoParaRetorno!!
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoExitoAdoptante = false
                registrarUsuario(u)
            },
            title = { Text("¡Cuenta de Adoptante creada! 🎉", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("La cuenta para ${u.nombre} fue registrada correctamente.")
                    Text("Ya puedes iniciar sesión con tu correo ${u.correo}.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoExitoAdoptante = false
                        registrarUsuario(u)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
                ) {
                    Text("Iniciar sesión")
                }
            }
        )
    }

    // 2. Éxito Centro (Muestra estado Pendiente sin simular aprobación local)
    if (mostrarDialogoExitoCentro && resultadoRegistroCentro != null) {
        val res = resultadoRegistroCentro!!
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoExitoCentro = false
                volverAlLogin()
            },
            title = { Text("Solicitud de Centro Registrada 🎉", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "La solicitud para registrar '${res.nombreCentro}' ha sido enviada exitosamente.",
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = AdoptOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Estado actual: PENDIENTE DE REVISIÓN",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Text(
                        text = "El equipo administrativo evaluará los datos antes de activar las funciones del centro. Podrás consultar el avance iniciando sesión con tus credenciales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdoptTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoExitoCentro = false
                        volverAlLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange)
                ) {
                    Text("Entendido / Ir al inicio")
                }
            }
        )
    }

    // 3. Explicación pedagógica de permiso de ubicación antes del mapa
    if (mostrarDialogoExplicacionUbicacion) {
        DialogoExplicacionUbicacion(
            alAceptar = {
                mostrarDialogoExplicacionUbicacion = false
                mostrarSelectorMapa = true
            },
            alRechazar = {
                mostrarDialogoExplicacionUbicacion = false
            }
        )
    }

    // 4. Selector de mapa interactivo
    if (mostrarSelectorMapa) {
        DialogoSelectorMapa(
            latitudInicial = latitudCentro,
            longitudInicial = longitudCentro,
            alConfirmarDetallado = { lat, lon, res ->
                latitudCentro = lat
                longitudCentro = lon
                if (res.direccionCompleta.isNotBlank() && !res.direccionCompleta.startsWith("Coordenadas:")) {
                    direccionCentro = res.direccionCompleta
                    if (errorDireccionCentro.isNotEmpty()) errorDireccionCentro = ""
                }
                if (res.distrito.isNotBlank()) {
                    distritoCentro = res.distrito
                    if (errorDistritoCentro.isNotEmpty()) errorDistritoCentro = ""
                }
                mostrarSelectorMapa = false
            },
            alConfirmar = { lat, lon, dist ->
                latitudCentro = lat
                longitudCentro = lon
                if (!dist.isNullOrBlank() && distritoCentro.isBlank()) {
                    distritoCentro = dist
                }
                mostrarSelectorMapa = false
            },
            alDescartar = { mostrarSelectorMapa = false }
        )
    }

    // 5. Términos y privacidad
    if (mostrarDialogoTerminos) {
        DialogoTerminosYPrivacidad(
            alAceptar = {
                terminosAceptados = true
                terminosAceptadosCentro = true
                errorTerminos = ""
                errorTerminosCentro = ""
                mostrarDialogoTerminos = false
            },
            alCerrar = { mostrarDialogoTerminos = false }
        )
    }
}
