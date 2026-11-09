package com.example.adoptme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adoptme.R
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptGreenButton
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealDark
import com.example.adoptme.ui.theme.AdoptTealLight
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary
import com.example.adoptme.ui.theme.BorderWarm
import com.example.adoptme.ui.theme.StatusGreenBg
import com.example.adoptme.ui.theme.StatusGreenText
import com.example.adoptme.ui.theme.StatusRedBg
import com.example.adoptme.ui.theme.StatusRedText

/**
 * Componente visual para mostrar el expediente de una solicitud de adopción en el panel Admin.
 * Contiene datos del postulante y botones según el estado de la solicitud:
 * - Pendiente: Poner en revisión.
 * - En revisión: Aprobar o Rechazar.
 * - Aprobada/Rechazada: Estado concluido.
 */
@Composable
fun TarjetaSolicitudAdmin(
    solicitud: AdoptionRequest,
    ponerEnRevision: (AdoptionRequest) -> Unit,
    aprobar: (AdoptionRequest) -> Pair<Boolean, String>,
    rechazar: (AdoptionRequest) -> Unit,
    onConfirmacion: (String) -> Unit,
    onErrorValidacion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
        border = BorderStroke(1.dp, BorderWarm),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: Mascota y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tituloMascota = if (solicitud.petSpecies.isNotBlank()) {
                    "🐾 ${solicitud.petName} (${solicitud.petSpecies})"
                } else {
                    "🐾 ${solicitud.petName}"
                }

                Text(
                    text = tituloMascota,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                )

                InsigniaEstado(estado = solicitud.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta de datos completos del postulante
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AdoptMeBg,
                border = BorderStroke(1.dp, BorderWarm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📋 Datos del Postulante",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTealDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    if (solicitud.applicantName.isNotBlank()) {
                        Text("👤 Postulante: ${solicitud.applicantName}", style = MaterialTheme.typography.bodySmall, color = AdoptTextDark)
                    }
                    if (solicitud.phone.isNotBlank()) {
                        Text("📞 Teléfono: ${solicitud.phone}", style = MaterialTheme.typography.bodySmall, color = AdoptTextDark)
                    }
                    if (solicitud.email.isNotBlank()) {
                        Text("✉️ Correo: ${solicitud.email}", style = MaterialTheme.typography.bodySmall, color = AdoptTextDark)
                    }
                    if (solicitud.address.isNotBlank()) {
                        Text("📍 Residencia / Distrito: ${solicitud.address}", style = MaterialTheme.typography.bodySmall, color = AdoptTextDark)
                    }
                    if (solicitud.housingType.isNotBlank()) {
                        Text("🏠 Tipo de vivienda: ${solicitud.housingType}", style = MaterialTheme.typography.bodySmall, color = AdoptTextDark)
                    }
                    if (solicitud.timeAvailability.isNotBlank()) {
                        Text("⏰ Disponibilidad de tiempo: ${solicitud.timeAvailability}", style = MaterialTheme.typography.bodySmall, color = AdoptTextDark)
                    }
                    if (solicitud.previousExperience.isNotBlank()) {
                        Text("🐕 Experiencia previa: ${solicitud.previousExperience}", style = MaterialTheme.typography.bodySmall, color = AdoptTextDark)
                    }
                    if (solicitud.reason.isNotBlank()) {
                        Text("💬 Motivo de adopción: ${solicitud.reason}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = AdoptTextDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones según estado de la solicitud
            val esPendiente = solicitud.status.equals("Pendiente", ignoreCase = true)
            val esEnRevision = solicitud.status.equals("En revisión", ignoreCase = true) ||
                    solicitud.status.equals("En revision", ignoreCase = true)

            when {
                esPendiente -> {
                    Button(
                        onClick = {
                            ponerEnRevision(solicitud)
                            onConfirmacion("La solicitud para ${solicitud.petName} fue puesta 'En revisión'.")
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp).size(18.dp)
                            )
                            Text("Poner en revisión", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                esEnRevision -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Botón Aprobar
                        Button(
                            onClick = {
                                val (exito, mensaje) = aprobar(solicitud)
                                if (exito) {
                                    onConfirmacion("¡Solicitud aprobada! ${solicitud.petName} ahora está 'En proceso'.")
                                } else {
                                    onErrorValidacion(mensaje)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdoptGreenButton),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp).size(18.dp)
                                )
                                Text("Aprobar", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Botón Rechazar
                        OutlinedButton(
                            onClick = {
                                rechazar(solicitud)
                                onConfirmacion("La solicitud para ${solicitud.petName} fue rechazada.")
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StatusRedText),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedText),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp).size(18.dp)
                                )
                                Text("Rechazar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                else -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (solicitud.status.equals("Aprobada", ignoreCase = true)) StatusGreenBg else StatusRedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (solicitud.status.equals("Aprobada", ignoreCase = true))
                                "✅ Solicitud concluida: Aprobada (Mascota en proceso)"
                            else
                                "❌ Solicitud concluida: Rechazada",
                            color = if (solicitud.status.equals("Aprobada", ignoreCase = true)) StatusGreenText else StatusRedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente visual para mostrar una mascota en la sección de inventario del panel Admin.
 * Permite ver detalles, cambiar estado y eliminar si es una publicación creada por un adoptante.
 */
@Composable
fun TarjetaMascotaAdmin(
    mascota: Pet,
    esDeUsuario: Boolean,
    verDetalle: () -> Unit,
    cambiarEstado: () -> Unit,
    eliminar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
        border = BorderStroke(1.dp, BorderWarm),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Fotografía de la mascota con soporte de Coil para URLs remotas (Fase A)
            ImagenMascotaRemota(
                imageUrl = mascota.imageUrl,
                photoBitmap = mascota.photoBitmap,
                fallbackDrawableRes = if (mascota.image != 0) mascota.image else R.drawable.ic_pet_fallback,
                contentDescription = "Foto de ${mascota.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Nombre y Estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                        Text(
                            text = mascota.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AdoptTextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${mascota.species} • ${mascota.breed}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdoptTextSecondary
                        )
                    }

                    InsigniaEstado(estado = mascota.status)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Distintivo de procedencia
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (esDeUsuario) AdoptTealLight else AdoptMeBg
                    ) {
                        Text(
                            text = if (esDeUsuario) "📢 Publicación de usuario" else "🐾 Catálogo base",
                            color = if (esDeUsuario) AdoptTealDark else AdoptTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "📍 ${mascota.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdoptTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Edad: ${mascota.age} • Sexo: ${mascota.gender}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdoptTextDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = verDetalle,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AdoptTeal),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AdoptTeal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ver datos", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = cambiarEstado,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Estado", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                if (esDeUsuario && eliminar != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = eliminar,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StatusRedText.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StatusRedBg,
                            contentColor = StatusRedText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp).size(16.dp),
                                tint = StatusRedText
                            )
                            Text("Eliminar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
