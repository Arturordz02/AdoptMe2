package com.example.adoptme.screens

import com.example.adoptme.api.RegistroUsuarioRequest
import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.model.UsuarioRegistrado
import com.example.adoptme.preferences.Preferencias
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material3.Checkbox




import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Image

import androidx.compose.ui.layout.ContentScale

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

import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn


import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Button

import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.OutlinedTextFieldDefaults

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.input.PasswordVisualTransformation

import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import com.example.adoptme.R

import com.example.adoptme.ui.theme.AdoptCardWhite

import com.example.adoptme.ui.theme.AdoptGreenButton

import com.example.adoptme.ui.theme.AdoptGreenButtonDark

import com.example.adoptme.ui.theme.AdoptMeBg

import com.example.adoptme.ui.theme.AdoptOrange

import com.example.adoptme.ui.theme.AdoptTeal

import com.example.adoptme.ui.theme.AdoptTextDark

import com.example.adoptme.ui.theme.AdoptTextSecondary

import com.example.adoptme.ui.theme.BorderWarm

import androidx.compose.runtime.rememberCoroutineScope



import kotlinx.coroutines.launch





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
    var mostrarDialogoTerminos by remember { mutableStateOf(false) }

    val contexto = LocalContext.current
    val preferencias = remember { Preferencias(contexto) }

    var mostrarDialogoExito by remember { mutableStateOf(false) }

    var usuarioCreadoParaRetorno by remember { mutableStateOf<UsuarioRegistrado?>(null) }



    val coroutineScope = rememberCoroutineScope()

    var registrando by remember { mutableStateOf(false) }

    var errorServidor by remember { mutableStateOf("") }





    fun registrar() {

        var esValido = true



        val nombreLimpio = nombre.trim()

        val correoLimpio = correo.trim()

        val telefonoLimpio = telefono.trim()

        val telefonoNormalizado = normalizarTelefonoPeruano(telefono)



        // 1. Validación de nombre

        if (nombreLimpio.isEmpty()) {

            errorNombre = "El nombre no puede estar vacío."

            esValido = false

        } else {

            errorNombre = ""

        }



        // 2. Validación de correo

        when {

            correoLimpio.isEmpty() -> {

                errorCorreo = "El correo no puede estar vacío."

                esValido = false

            }

            !esFormatoCorreoValido(correoLimpio) -> {

                errorCorreo = "El correo debe tener un formato válido (ej. usuario@correo.pe o .com)."

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



        // 3. Validación de teléfono celular peruano (obligatorio)

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

                errorTelefono = "El teléfono debe tener exactamente 9 dígitos (ingresados: ${telefonoNormalizado.length})."

                esValido = false

            }

            else -> {

                errorTelefono = ""

            }

        }



        // 4. Validación de contraseña

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



        // 5. Validación de confirmación de contraseña

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

        // 6. Validación de Términos y Privacidad (FASE 7)
        if (!terminosAceptados) {
            errorTerminos = "Debes aceptar los Términos y la Política de Privacidad para registrarte."
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
                        mostrarDialogoExito = true

                    } else if (respuesta.code() == 409) {

                        errorServidor = "Este correo ya está registrado."

                    } else {

                        errorServidor = "No se pudo completar el registro. Código: ${respuesta.code()}"

                    }



                } catch (e: Exception) {

                    errorServidor = "No se pudo conectar con el servidor. Revisa tu conexión a Internet."

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

        // =====================================================================

        // HEADER CON DEGRADADO (AdoptOrange a AdoptTeal)

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

                .padding(top = 32.dp, bottom = 44.dp, start = 20.dp, end = 20.dp)

        ) {

            Icon(

                painter = painterResource(id = R.drawable.ic_paw),

                contentDescription = null,

                tint = Color.White.copy(alpha = 0.18f),

                modifier = Modifier

                    .size(36.dp)

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

                    .offset(x = 4.dp, y = 10.dp)

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

                    text = "Únete a AdoptMe",

                    style = MaterialTheme.typography.headlineMedium,

                    fontWeight = FontWeight.Bold,

                    color = Color.White

                )



                Spacer(modifier = Modifier.height(4.dp))



                Text(

                    text = "Crea tu cuenta de Adoptante para encontrar a tu nuevo compañero",

                    style = MaterialTheme.typography.bodyMedium,

                    color = Color.White.copy(alpha = 0.95f),

                    textAlign = TextAlign.Center,

                    modifier = Modifier.padding(horizontal = 16.dp)

                )

            }

        }



        // =====================================================================

        // TARJETA PRINCIPAL BLANCA

        // =====================================================================

        Card(

            modifier = Modifier
                .widthIn(max = 520.dp)

                .fillMaxWidth()

                .offset(y = (-20).dp),

            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),

            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),

            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)

        ) {

            Column(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 24.dp, vertical = 26.dp),

                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Text(

                    text = "Registro de Cuenta",

                    style = MaterialTheme.typography.headlineSmall,

                    fontWeight = FontWeight.Bold,

                    color = AdoptTextDark,

                    modifier = Modifier.fillMaxWidth()

                )



                Spacer(modifier = Modifier.height(4.dp))



                Text(

                    text = "Ingresa tus datos para registrarte como Adoptante.",

                    style = MaterialTheme.typography.bodyMedium,

                    color = AdoptTextSecondary,

                    modifier = Modifier.fillMaxWidth()

                )



                Spacer(modifier = Modifier.height(18.dp))



                // 1. Nombre completo

                OutlinedTextField(

                    value = nombre,

                    onValueChange = {

                        nombre = it

                        if (errorNombre.isNotEmpty()) errorNombre = ""

                    },

                    label = { Text("Nombre completo") },

                    placeholder = { Text("Ej. Lucía Quispe Mamani") },

                    leadingIcon = {

                        Icon(

                            painter = painterResource(id = R.drawable.ic_person_add),

                            contentDescription = null,

                            tint = if (errorNombre.isNotEmpty()) MaterialTheme.colorScheme.error else AdoptTeal,

                            modifier = Modifier.size(22.dp)

                        )

                    },

                    isError = errorNombre.isNotEmpty(),

                    singleLine = true,

                    shape = RoundedCornerShape(14.dp),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = AdoptTeal,

                        unfocusedBorderColor = BorderWarm,

                        focusedLabelColor = AdoptTeal

                    ),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errorNombre.isNotEmpty()) {

                    Text(

                        text = "⚠️ $errorNombre",

                        color = MaterialTheme.colorScheme.error,

                        style = MaterialTheme.typography.bodySmall,

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(start = 6.dp, top = 4.dp)

                    )

                }



                Spacer(modifier = Modifier.height(12.dp))



                // 2. Correo electrónico

                OutlinedTextField(

                    value = correo,

                    onValueChange = {

                        correo = it

                        if (errorCorreo.isNotEmpty()) errorCorreo = ""

                    },

                    label = { Text("Correo electrónico") },

                    placeholder = { Text("ejemplo@correo.pe") },

                    leadingIcon = {

                        Icon(

                            painter = painterResource(id = R.drawable.ic_email),

                            contentDescription = null,

                            tint = if (errorCorreo.isNotEmpty()) MaterialTheme.colorScheme.error else AdoptTeal,

                            modifier = Modifier.size(22.dp)

                        )

                    },

                    isError = errorCorreo.isNotEmpty(),

                    singleLine = true,

                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),

                    shape = RoundedCornerShape(14.dp),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = AdoptTeal,

                        unfocusedBorderColor = BorderWarm,

                        focusedLabelColor = AdoptTeal

                    ),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errorCorreo.isNotEmpty()) {

                    Text(

                        text = "⚠️ $errorCorreo",

                        color = MaterialTheme.colorScheme.error,

                        style = MaterialTheme.typography.bodySmall,

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(start = 6.dp, top = 4.dp)

                    )

                }



                Spacer(modifier = Modifier.height(12.dp))



                // 3. Teléfono celular (obligatorio)

                OutlinedTextField(

                    value = telefono,

                    onValueChange = {

                        telefono = it

                        if (errorTelefono.isNotEmpty()) errorTelefono = ""

                    },

                    label = { Text("Teléfono celular") },

                    placeholder = { Text("Ej. 999 888 777") },

                    leadingIcon = {

                        Icon(

                            painter = painterResource(id = R.drawable.ic_phone),

                            contentDescription = null,

                            tint = if (errorTelefono.isNotEmpty()) MaterialTheme.colorScheme.error else AdoptTeal,

                            modifier = Modifier.size(22.dp)

                        )

                    },

                    isError = errorTelefono.isNotEmpty(),

                    singleLine = true,

                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

                    shape = RoundedCornerShape(14.dp),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = AdoptTeal,

                        unfocusedBorderColor = BorderWarm,

                        focusedLabelColor = AdoptTeal

                    ),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errorTelefono.isNotEmpty()) {

                    Text(

                        text = "⚠️ $errorTelefono",

                        color = MaterialTheme.colorScheme.error,

                        style = MaterialTheme.typography.bodySmall,

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(start = 6.dp, top = 4.dp)

                    )

                }



                Spacer(modifier = Modifier.height(12.dp))



                // 4. Contraseña

                OutlinedTextField(

                    value = contrasena,

                    onValueChange = {

                        contrasena = it

                        if (errorContrasena.isNotEmpty()) errorContrasena = ""

                    },

                    label = { Text("Contraseña") },

                    placeholder = { Text("Mínimo 6 caracteres") },

                    leadingIcon = {

                        Icon(

                            painter = painterResource(id = R.drawable.ic_lock),

                            contentDescription = null,

                            tint = if (errorContrasena.isNotEmpty()) MaterialTheme.colorScheme.error else AdoptTeal,

                            modifier = Modifier.size(22.dp)

                        )

                    },

                    trailingIcon = {

                        IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {

                            Icon(

                                painter = painterResource(

                                    id = if (contrasenaVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off

                                ),

                                contentDescription = if (contrasenaVisible) "Ocultar contraseña" else "Mostrar contraseña",

                                tint = AdoptTextSecondary,

                                modifier = Modifier.size(22.dp)

                            )

                        }

                    },

                    visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),

                    isError = errorContrasena.isNotEmpty(),

                    singleLine = true,

                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),

                    shape = RoundedCornerShape(14.dp),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = AdoptTeal,

                        unfocusedBorderColor = BorderWarm,

                        focusedLabelColor = AdoptTeal

                    ),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errorContrasena.isNotEmpty()) {

                    Text(

                        text = "⚠️ $errorContrasena",

                        color = MaterialTheme.colorScheme.error,

                        style = MaterialTheme.typography.bodySmall,

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(start = 6.dp, top = 4.dp)

                    )

                }



                Spacer(modifier = Modifier.height(12.dp))



                // 4. Confirmar contraseña

                OutlinedTextField(

                    value = confirmarContrasena,

                    onValueChange = {

                        confirmarContrasena = it

                        if (errorConfirmarContrasena.isNotEmpty()) errorConfirmarContrasena = ""

                    },

                    label = { Text("Confirmar contraseña") },

                    placeholder = { Text("Repite tu contraseña") },

                    leadingIcon = {

                        Icon(

                            painter = painterResource(id = R.drawable.ic_lock),

                            contentDescription = null,

                            tint = if (errorConfirmarContrasena.isNotEmpty()) MaterialTheme.colorScheme.error else AdoptTeal,

                            modifier = Modifier.size(22.dp)

                        )

                    },

                    trailingIcon = {

                        IconButton(onClick = { confirmarContrasenaVisible = !confirmarContrasenaVisible }) {

                            Icon(

                                painter = painterResource(

                                    id = if (confirmarContrasenaVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off

                                ),

                                contentDescription = if (confirmarContrasenaVisible) "Ocultar contraseña" else "Mostrar contraseña",

                                tint = AdoptTextSecondary,

                                modifier = Modifier.size(22.dp)

                            )

                        }

                    },

                    visualTransformation = if (confirmarContrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),

                    isError = errorConfirmarContrasena.isNotEmpty(),

                    singleLine = true,

                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),

                    shape = RoundedCornerShape(14.dp),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = AdoptTeal,

                        unfocusedBorderColor = BorderWarm,

                        focusedLabelColor = AdoptTeal

                    ),

                    modifier = Modifier.fillMaxWidth()

                )

                if (errorConfirmarContrasena.isNotEmpty()) {

                    Text(

                        text = "⚠️ $errorConfirmarContrasena",

                        color = MaterialTheme.colorScheme.error,

                        style = MaterialTheme.typography.bodySmall,

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(start = 6.dp, top = 4.dp)

                    )

                }

                Spacer(modifier = Modifier.height(14.dp))

                // Aceptación obligatoria de Términos y Privacidad (FASE 7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = terminosAceptados,
                        onCheckedChange = {
                            terminosAceptados = it
                            if (it && errorTerminos.isNotEmpty()) errorTerminos = ""
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acepto los Términos de Adopción y la Política de Privacidad de Ubicación (Ley N° 30407)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdoptTextDark
                        )
                        Text(
                            text = "Leer Términos y Privacidad",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AdoptTeal,
                            modifier = Modifier
                                .clickable { mostrarDialogoTerminos = true }
                                .padding(top = 2.dp)
                        )
                    }
                }

                if (errorTerminos.isNotEmpty()) {
                    Text(
                        text = "⚠️ $errorTerminos",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))





                // Botón "Crear cuenta"

                Button(

                    onClick = { registrar() },

                    enabled = !registrando,

                    shape = RoundedCornerShape(14.dp),

                    colors = ButtonDefaults.buttonColors(

                        containerColor = Color.Transparent

                    ),

                    contentPadding = PaddingValues(0.dp),

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(52.dp)

                        .background(

                            brush = Brush.horizontalGradient(

                                colors = listOf(

                                    AdoptGreenButton,

                                    AdoptGreenButtonDark

                                )

                            ),

                            shape = RoundedCornerShape(14.dp)

                        )

                ){

                    Row(

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.Center

                    ) {

                        Icon(

                            painter = painterResource(id = R.drawable.ic_person_add),

                            contentDescription = null,

                            tint = Color.White,

                            modifier = Modifier.size(20.dp)

                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(

                            text = "Crear cuenta",

                            color = Color.White,

                            style = MaterialTheme.typography.titleMedium,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }



                Spacer(modifier = Modifier.height(14.dp))



                if (errorServidor.isNotEmpty()) {

                    Text(

                        text = "⚠️ $errorServidor",

                        color = MaterialTheme.colorScheme.error,

                        style = MaterialTheme.typography.bodySmall,

                        textAlign = TextAlign.Center,

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(bottom = 12.dp)

                    )

                }



                // Botón "Volver al inicio de sesión"

                OutlinedButton(

                    onClick = volverAlLogin,

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

                    Text(

                        text = "Iniciar sesión",

                        color = AdoptTeal,

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold

                    )

                }

            }

        }

    }



    // Diálogo modal de confirmación de registro exitoso

    if (mostrarDialogoExito && usuarioCreadoParaRetorno != null) {

        val usuario = usuarioCreadoParaRetorno!!

        AlertDialog(

            onDismissRequest = {

                mostrarDialogoExito = false

                registrarUsuario(usuario)

            },

            title = {

                Text(

                    text = "¡Cuenta creada con éxito! 🎉",

                    fontWeight = FontWeight.Bold,

                    color = AdoptTextDark

                )

            },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text(

                        text = "La cuenta para ${usuario.nombre} fue registrada correctamente con el rol de Adoptante.",

                        style = MaterialTheme.typography.bodyMedium

                    )

                    Text(

                        text = "📱 Celular registrado: ${usuario.telefono}",

                        style = MaterialTheme.typography.bodyMedium,

                        fontWeight = FontWeight.Medium,

                        color = AdoptTeal

                    )

                    Text(

                        text = "Ya puedes iniciar sesión utilizando tu correo ${usuario.correo} y tu contraseña.",

                        style = MaterialTheme.typography.bodyMedium,

                        color = AdoptTextSecondary

                    )

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        mostrarDialogoExito = false

                        registrarUsuario(usuario)

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),

                    shape = RoundedCornerShape(10.dp)

                ) {

                    Text("Iniciar sesión")

                }

            },

            shape = RoundedCornerShape(16.dp),

            containerColor = AdoptCardWhite

        )

    }

    if (mostrarDialogoTerminos) {
        DialogoTerminosYPrivacidad(
            alAceptar = {
                terminosAceptados = true
                errorTerminos = ""
                mostrarDialogoTerminos = false
            },
            alCerrar = {
                mostrarDialogoTerminos = false
            }
        )
    }

}
