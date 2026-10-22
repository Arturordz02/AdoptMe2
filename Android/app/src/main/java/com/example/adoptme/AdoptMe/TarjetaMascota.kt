package com.example.adoptme.AdoptMe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.adoptme.R
import com.example.adoptme.model.Pet

@Composable
fun TarjetaMascota(
    mascota: Pet,
    esFavorito: Boolean,
    cambiarFavorito: () -> Unit,
    verDetalle: () -> Unit,
    modifier: Modifier = Modifier,
    esPublicacionPropia: Boolean = false
) {

    Card(
        modifier = modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),

        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline
        ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column {

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {

                // =========================
                // FOTO
                // =========================

                if (mascota.photoBitmap != null) {

                    Image(
                        bitmap =
                            mascota
                                .photoBitmap
                                .asImageBitmap(),

                        contentDescription =
                            "Foto de ${mascota.name}",

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp
                                )
                            ),

                        contentScale =
                            ContentScale.Crop
                    )

                } else if (mascota.image != 0) {

                    Image(
                        painter =
                            painterResource(
                                id = mascota.image
                            ),

                        contentDescription =
                            "Foto de ${mascota.name}",

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp
                                )
                            ),

                        contentScale =
                            ContentScale.Crop
                    )
                } else {

                    Image(
                        painter =
                            painterResource(
                                id = R.drawable.ic_pet_fallback
                            ),

                        contentDescription =
                            "Foto de ${mascota.name}",

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp
                                )
                            ),

                        contentScale =
                            ContentScale.Crop
                    )
                }

                // =========================
                // FAVORITOS
                // =========================

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .surface
                                .copy(alpha = 0.92f)
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    IconButton(
                        onClick =
                            cambiarFavorito,

                        modifier =
                            Modifier.size(38.dp)
                    ) {

                        Icon(
                            painter =
                                painterResource(
                                    id = if (esFavorito) {
                                        R.drawable.ic_favorite
                                    } else {
                                        R.drawable.ic_favorite_border
                                    }
                                ),

                            contentDescription =
                                if (esFavorito) {
                                    "Quitar de favoritos"
                                } else {
                                    "Agregar a favoritos"
                                },

                            tint =
                                if (esFavorito) {
                                    Color(0xFFE53935)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                        )
                    }
                }
            }

            // =========================
            // INFORMACIÓN
            // =========================

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

                    Column(
                        modifier =
                            Modifier.weight(
                                1f,
                                fill = false
                            )
                    ) {

                        Text(
                            text = mascota.name,

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )

                        Text(
                            text = mascota.breed,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        // ESPECIE

                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        8.dp
                                    )
                                )
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer
                                )
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                        ) {

                            Text(
                                text =
                                    mascota.species,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimaryContainer,

                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium,

                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }

                        InsigniaEstado(
                            estado =
                                mascota.status
                        )
                    }
                }

                if (esPublicacionPropia) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "📢 Tu publicación",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                // =========================
                // DATOS
                // =========================

                Text(
                    text =
                        "${mascota.species} • ${mascota.age} • ${mascota.gender} • 📍 ${mascota.location}",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                // =========================
                // DESCRIPCIÓN
                // =========================

                Text(
                    text =
                        mascota.description,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines = 2,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                // =========================
                // BOTÓN
                // =========================

                Button(
                    onClick =
                        verDetalle,

                    shape =
                        RoundedCornerShape(
                            12.dp
                        ),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary,

                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                        ),

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {

                    Text(
                        text =
                            "Ver detalle",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}