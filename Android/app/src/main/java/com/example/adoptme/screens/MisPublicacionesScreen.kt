package com.example.adoptme.screens

import com.example.adoptme.utils.formatearEdadCanonica

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.R
import com.example.adoptme.components.InsigniaEstado
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealDark
import com.example.adoptme.ui.theme.AdoptTealLight
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary
import com.example.adoptme.ui.theme.BorderWarm

@Composable
fun MisPublicacionesScreen(
    publicaciones: List<Pet>,
    volver: () -> Unit,
    eliminar: (Pet) -> Unit,
    editar: ((Pet, (Boolean) -> Unit) -> Unit)? = null,
    correoActual: String = "",
    modifier: Modifier = Modifier
) {
    val publicacionesPropias = remember(publicaciones, correoActual) {
        val lista = if (correoActual.isBlank()) {
            publicaciones
        } else {
            publicaciones.filter {
                it.creadoPorCorreo.isBlank() || it.creadoPorCorreo.equals(correoActual, ignoreCase = true)
            }
        }
        lista.distinctBy { it.claveCompose }
    }

    var mascotaAEliminar by remember {
        mutableStateOf<Pet?>(null)
    }

    var mascotaAEditar by remember {
        mutableStateOf<Pet?>(null)
    }

    var mensajeExito by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdoptMeBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Botón Volver
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = volver) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Volver",
                        modifier = Modifier.padding(end = 4.dp).size(18.dp),
                        tint = AdoptTeal
                    )
                    Text(
                        text = "Volver",
                        color = AdoptTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Encabezado
        Text(
            text = "🐾 Mis publicaciones",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AdoptTextDark
        )

        Text(
            text = "Mascotas que has puesto en adopción responsable.",
            style = MaterialTheme.typography.bodyMedium,
            color = AdoptTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Banner informativo tras eliminar
        if (mensajeExito != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mensajeExito!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { mensajeExito = null }) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (publicacionesPropias.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                border = BorderStroke(1.dp, BorderWarm)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🐾", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Todavía no has publicado ninguna mascota.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Puedes publicar mascotas desde la opción 'Publicar una mascota' en tu perfil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdoptTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
                items(
                    items = publicacionesPropias,
                    key = { mascota -> mascota.claveCompose }
                ) { mascota ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                        border = BorderStroke(1.dp, BorderWarm),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            // Fotografía o imagen con soporte de Coil para URLs remotas (Fase A)
                            com.example.adoptme.components.ImagenMascotaRemota(
                                imageUrl = mascota.imageUrl,
                                photoBitmap = mascota.photoBitmap,
                                fallbackDrawableRes = if (mascota.image != 0) mascota.image else R.drawable.ic_pet_fallback,
                                contentDescription = "Foto de ${mascota.name}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mascota.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AdoptTextDark
                                        )
                                        Text(
                                            text = "${mascota.species} • ${mascota.breed}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdoptTextSecondary
                                        )
                                    }

                                    InsigniaEstado(estado = mascota.status)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AdoptTealLight
                                ) {
                                    Text(
                                        text = "📢 Tu publicación",
                                        color = AdoptTealDark,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Consulta de datos completa
                                Text(
                                    text = "Edad: ${mascota.age} • Sexo: ${mascota.gender}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdoptTextDark,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "📍 ${mascota.location}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdoptTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                                )

                                if (mascota.description.isNotBlank()) {
                                    Text(
                                        text = "Descripción: ${mascota.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AdoptTextDark,
                                        maxLines = 2,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { mascotaAEditar = mascota },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = AdoptTeal
                                        ),
                                        border = BorderStroke(1.dp, AdoptTeal)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_edit),
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp).size(18.dp),
                                                tint = AdoptTeal
                                            )
                                            Text(
                                                text = "Editar",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { mascotaAEliminar = mascota },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_delete),
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp).size(18.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "Eliminar",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mascotaAEditar != null) {
        DialogoEditarMascota(
            mascota = mascotaAEditar!!,
            onDismiss = {
                mascotaAEditar = null
            },
            onGuardar = { mascotaActualizada, onResultado ->
                if (editar != null) {
                    editar(mascotaActualizada) { exito ->
                        onResultado(exito)
                        if (exito) {
                            mensajeExito = if (mascotaActualizada.esRemota) {
                                "La publicación de ${mascotaActualizada.name} fue actualizada en el servidor correctamente."
                            } else {
                                "La publicación de ${mascotaActualizada.name} fue actualizada localmente."
                            }
                            mascotaAEditar = null
                        }
                    }
                } else {
                    onResultado(true)
                    mensajeExito = "La publicación de ${mascotaActualizada.name} fue actualizada localmente."
                    mascotaAEditar = null
                }
            }
        )
    }

    if (mascotaAEliminar != null) {
        AlertDialog(
            onDismissRequest = {
                mascotaAEliminar = null
            },
            title = {
                Text(
                    text = "Eliminar publicación",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar la publicación de ${mascotaAEliminar!!.name}? Se retirará del catálogo y de la lista de adopción."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mascotaEliminada = mascotaAEliminar!!
                        eliminar(mascotaEliminada)
                        mensajeExito = "La publicación de ${mascotaEliminada.name} fue eliminada correctamente."
                        mascotaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mascotaAEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Diálogo modal para editar una publicación de mascota.
 * Precarga los datos existentes y no permite alterar el ID ni CreadoPorCorreo.
 */
@Composable
fun DialogoEditarMascota(
    mascota: Pet,
    onDismiss: () -> Unit,
    onGuardar: (Pet, (Boolean) -> Unit) -> Unit
) {
    var nombre by remember { mutableStateOf(mascota.name) }
    var especie by remember { mutableStateOf(mascota.species) }
    var raza by remember { mutableStateOf(mascota.breed) }
    var edad by remember { mutableStateOf(mascota.age) }
    var genero by remember { mutableStateOf(mascota.gender) }
    var estado by remember { mutableStateOf(mascota.status) }
    var ubicacion by remember { mutableStateOf(mascota.location) }
    var descripcion by remember { mutableStateOf(mascota.description) }

    var guardando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = Modifier.widthIn(max = 520.dp),
        onDismissRequest = {
            if (!guardando) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp).size(22.dp),
                    tint = AdoptTeal
                )
                Text(
                    text = "Editar publicación",
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMensaje != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMensaje!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // 1. Nombre
                Text("Nombre *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        errorMensaje = null
                    },
                    placeholder = { Text("Ej: Bobby, Michi") },
                    singleLine = true,
                    enabled = !guardando,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Especie (RN-02)
                Text("Especie * (Perro, Gato o Conejo)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Perro", "Gato", "Conejo").forEach { opcion ->
                        val seleccionada = especie.equals(opcion, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (seleccionada) AdoptOrange else AdoptMeBg,
                            border = BorderStroke(1.dp, if (seleccionada) AdoptOrange else BorderWarm),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !guardando) {
                                    especie = opcion
                                    errorMensaje = null
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = opcion,
                                    color = if (seleccionada) Color.White else AdoptTextDark,
                                    fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Raza
                Text("Raza *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = raza,
                    onValueChange = {
                        raza = it
                        errorMensaje = null
                    },
                    placeholder = { Text("Ej: Mestizo, Siamés") },
                    singleLine = true,
                    enabled = !guardando,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Edad
                Text("Edad * (Ej: 2 años y 6 meses, 1 año o Menos de 1 mes)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = edad,
                    onValueChange = {
                        edad = it
                        errorMensaje = null
                    },
                    placeholder = { Text("Ej: 2 años y 6 meses") },
                    singleLine = true,
                    enabled = !guardando,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Sexo
                Text("Sexo *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Macho", "Hembra").forEach { opcion ->
                        val seleccionada = genero.equals(opcion, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (seleccionada) AdoptTeal else AdoptMeBg,
                            border = BorderStroke(1.dp, if (seleccionada) AdoptTeal else BorderWarm),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !guardando) {
                                    genero = opcion
                                    errorMensaje = null
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (opcion == "Macho") "♂ Macho" else "♀ Hembra",
                                    color = if (seleccionada) Color.White else AdoptTextDark,
                                    fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Estado de publicación
                Text("Estado *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Disponible", "En proceso", "Adoptado").forEach { opcion ->
                        val seleccionada = estado.equals(opcion, ignoreCase = true) ||
                                (opcion == "Adoptado" && estado.equals("Adoptada", ignoreCase = true))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (seleccionada) AdoptTealDark else AdoptMeBg,
                            border = BorderStroke(1.dp, if (seleccionada) AdoptTealDark else BorderWarm),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !guardando) {
                                    estado = opcion
                                    errorMensaje = null
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = opcion,
                                    color = if (seleccionada) Color.White else AdoptTextDark,
                                    fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 7. Ubicación
                Text("Ubicación o distrito *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = {
                        ubicacion = it
                        errorMensaje = null
                    },
                    placeholder = { Text("Ej: Surco, Lima") },
                    singleLine = true,
                    enabled = !guardando,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 8. Descripción
                Text("Descripción *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AdoptTextDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        errorMensaje = null
                    },
                    placeholder = { Text("Describe el comportamiento y cuidados de la mascota...") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !guardando,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rev = revisarPublicacion(
                        nombre = nombre,
                        especie = especie,
                        raza = raza,
                        edad = edad,
                        genero = genero,
                        ubicacion = ubicacion,
                        descripcion = descripcion
                    )
                    if (!rev.esValido) {
                        errorMensaje = rev.errorNombre
                            ?: rev.errorEspecie
                            ?: rev.errorRaza
                            ?: rev.errorEdad
                            ?: rev.errorGenero
                            ?: rev.errorUbicacion
                            ?: rev.errorDescripcion
                            ?: "Por favor verifica los campos obligatorios."
                        return@Button
                    }

                    guardando = true
                    val mascotaActualizada = mascota.copy(
                        name = nombre.trim(),
                        species = especie.trim(),
                        breed = raza.trim(),
                        age = formatearEdadCanonica(edad),
                        gender = genero.trim(),
                        status = estado.trim(),
                        location = ubicacion.trim(),
                        description = descripcion.trim()
                    )

                    onGuardar(mascotaActualizada) { exito ->
                        guardando = false
                        if (!exito) {
                            errorMensaje = "No se pudieron guardar los cambios en el servidor. Intenta nuevamente."
                        }
                    }
                },
                enabled = !guardando,
                colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal)
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardando...")
                } else {
                    Text("Guardar cambios", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !guardando
            ) {
                Text("Cancelar")
            }
        }
    )
}