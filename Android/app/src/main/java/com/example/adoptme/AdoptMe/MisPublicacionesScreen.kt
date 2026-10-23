package com.example.adoptme.AdoptMe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.adoptme.R
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    correoActual: String = "",
    modifier: Modifier = Modifier
) {
    val publicacionesPropias = remember(publicaciones, correoActual) {
        if (correoActual.isBlank()) {
            publicaciones
        } else {
            publicaciones.filter {
                it.creadoPorCorreo.isBlank() || it.creadoPorCorreo.equals(correoActual, ignoreCase = true)
            }
        }
    }

    var mascotaAEliminar by remember {
        mutableStateOf<Pet?>(null)
    }

    var mensajeExito by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdoptMeBg)
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
            LazyColumn {
                items(
                    items = publicacionesPropias,
                    key = { mascota -> mascota.id }
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
                            // Fotografía o imagen drawable existente
                            if (mascota.photoBitmap != null) {
                                Image(
                                    bitmap = mascota.photoBitmap.asImageBitmap(),
                                    contentDescription = "Foto de ${mascota.name}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (mascota.image != 0) {
                                Image(
                                    painter = painterResource(id = mascota.image),
                                    contentDescription = "Foto de ${mascota.name}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_pet_fallback),
                                    contentDescription = "Foto de ${mascota.name}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

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

                                OutlinedButton(
                                    onClick = { mascotaAEliminar = mascota },
                                    modifier = Modifier.fillMaxWidth(),
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