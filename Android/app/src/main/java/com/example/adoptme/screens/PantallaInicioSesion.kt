package com.example.adoptme.screens
import com.example.adoptme.model.UsuarioRegistrado

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.adoptme.R
import com.example.adoptme.api.LoginUsuarioRequest
import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PantallaInicioSesion(
    iniciarSesion: (
        rol: String,
        correo: String,
        token: String,
        nombre: String,
        telefono: String,
        centroId: Int?,
        estadoCentro: String?,
        centroActivo: Boolean,
        esDemostrativo: Boolean
    ) -> Unit,
    irARegistro: () -> Unit = {},
    usuariosRegistrados: List<UsuarioRegistrado> = emptyList(),
    mensajeExitoInicial: String? = null,
    correoInicial: String = "",
    modifier: Modifier = Modifier
) {
    var correo by remember(correoInicial) { mutableStateOf(correoInicial) }
    var contrasena by remember { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var mensajeExito by remember(mensajeExitoInicial) { mutableStateOf(mensajeExitoInicial) }
    val coroutineScope = rememberCoroutineScope()
    var iniciandoSesion by remember { mutableStateOf(false) }
    var mostrarDialogoTerminos by remember { mutableStateOf(false) }

    fun procesarInicioSesion(correoParam: String? = null, contrasenaParam: String? = null) {
        val emailObjetivo = correoParam ?: correo
        val passObjetivo = contrasenaParam ?: contrasena
        val correoLimpio = emailObjetivo.trim().lowercase()

        if (correoLimpio.isBlank() || passObjetivo.isBlank()) {
            mensajeError = "Ingresa tu correo y contraseña."
            return
        }

        if (iniciandoSesion) return

        iniciandoSesion = true
        mensajeError = ""

        coroutineScope.launch {
            try {
                val respuesta = RetrofitClient.usuariosApi.iniciarSesion(
                    LoginUsuarioRequest(
                        correo = correoLimpio,
                        contrasena = passObjetivo
                    )
                )

                when {
                    respuesta.isSuccessful -> {
                        val usuario = respuesta.body()
                        if (usuario != null) {
                            mensajeError = ""
                            iniciarSesion(
                                usuario.rol,
                                usuario.correo,
                                usuario.token,
                                usuario.nombre,
                                usuario.telefono,
                                usuario.centroId,
                                usuario.estadoCentro,
                                usuario.centroActivo,
                                usuario.esDemostrativo
                            )
                        } else {
                            mensajeError = "Respuesta inesperada del servidor."
                        }
                    }
                    respuesta.code() == 401 -> {
                        mensajeError = "Correo o contraseña incorrectos."
                    }
                    respuesta.code() in listOf(502, 503, 504) -> {
                        mensajeError = "El servidor está iniciando. Intenta nuevamente en unos segundos."
                    }
                    else -> {
                        mensajeError = "No se pudo iniciar sesión. Código: ${respuesta.code()}"
                    }
                }
            } catch (e: Exception) {
                mensajeError = RetrofitClient.resolverMensajeErrorServidor(e)
            } finally {
                iniciandoSesion = false
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
        // =====================================================================
        // 1. HEADER SUPERIOR CON FONDO DEGRADADO
        // =====================================================================
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
                .padding(top = 36.dp, bottom = 48.dp, start = 20.dp, end = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_paw),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.18f),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 0.dp, y = 4.dp)
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_paw),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.16f),
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = 12.dp)
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_paw),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 8.dp, y = (-8).dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.adoptme_logo_original),
                    contentDescription = "Logo de AdoptMe",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(68.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Adopta con Amor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Dale una segunda oportunidad a quien más amor tiene para dar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.adoptme_dog_cat_original),
                    contentDescription = "Perro y gato de AdoptMe",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .widthIn(max = 240.dp)
                        .heightIn(min = 60.dp, max = 125.dp)
                )
            }
        }

        // =====================================================================
        // 2. CARD INFERIOR EN BLANCO
        // =====================================================================
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .offset(y = (-24).dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Accede a tu cuenta para seguir encontrando a tu compañero ideal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdoptTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!mensajeExito.isNullOrEmpty()) {
                    Surface(
                        color = AdoptTealLight,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AdoptTeal.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ $mensajeExito",
                                color = AdoptTealDark,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = correo,
                    onValueChange = {
                        correo = it
                        if (mensajeError.isNotEmpty()) mensajeError = ""
                    },
                    label = { Text("Correo") },
                    placeholder = { Text("ejemplo@adoptme.com") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_email),
                            contentDescription = "Correo",
                            tint = AdoptOrange
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = contrasena,
                    onValueChange = {
                        contrasena = it
                        if (mensajeError.isNotEmpty()) mensajeError = ""
                    },
                    label = { Text("Contraseña") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = "Contraseña",
                            tint = AdoptOrange
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                            Icon(
                                painter = painterResource(
                                    id = if (contrasenaVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                                ),
                                contentDescription = if (contrasenaVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = AdoptTextSecondary
                            )
                        }
                    },
                    visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = AdoptOrange,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { }
                    )
                }

                if (mensajeError.isNotEmpty()) {
                    Text(
                        text = "⚠️ $mensajeError",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // BOTÓN CON ESTADO DE CARGA AÑADIDO
                Button(
                    onClick = { procesarInicioSesion() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !iniciandoSesion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(AdoptGreenButton, AdoptGreenButtonDark)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (iniciandoSesion) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Conectando...",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_login_arrow),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Iniciar Sesión",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "¿No tienes cuenta?",
                    color = AdoptTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = irARegistro,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, AdoptTeal),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = AdoptTeal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person_add),
                            contentDescription = null,
                            tint = AdoptTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Registrarse",
                            color = AdoptTeal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AdoptSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "💡 Acceso rápido para demostración:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AdoptTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    correo = "usuario@adoptme.com"
                                    contrasena = "123456"
                                    mensajeError = ""
                                    procesarInicioSesion("usuario@adoptme.com", "123456")
                                },
                                enabled = !iniciandoSesion,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("👤 Adoptante", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    correo = "centro@adoptme.com"
                                    contrasena = "123456"
                                    mensajeError = ""
                                    procesarInicioSesion("centro@adoptme.com", "123456")
                                },
                                enabled = !iniciandoSesion,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🏢 Centro", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    correo = "admin@adoptme.com"
                                    contrasena = "123456"
                                    mensajeError = ""
                                    procesarInicioSesion("admin@adoptme.com", "123456")
                                },
                                enabled = !iniciandoSesion,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🛡️ Admin", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = { mostrarDialogoTerminos = true }
                ) {
                    Text(
                        text = "📜 Términos de Adopción y Política de Privacidad",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdoptTealDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (mostrarDialogoTerminos) {
        DialogoTerminosYPrivacidad(
            alCerrar = { mostrarDialogoTerminos = false },
            modoSoloLectura = true
        )
    }
}