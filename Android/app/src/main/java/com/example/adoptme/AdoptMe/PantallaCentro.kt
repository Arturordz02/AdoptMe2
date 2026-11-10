package com.example.adoptme.AdoptMe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.example.adoptme.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.model.ConversacionCentro
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptOrangeLight
import com.example.adoptme.ui.theme.AdoptSurfaceVariant
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealLight
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary

data class InfoCentroDemo(
    val id: Int,
    val nombre: String,
    val nombreCorto: String,
    val icono: String
)

val CentrosDisponiblesDemo = listOf(
    InfoCentroDemo(1, "Albergue Huellitas del Norte", "Huellitas del Norte", "🏡"),
    InfoCentroDemo(2, "Refugio Patitas Felices Surco", "Patitas Felices", "🏠"),
    InfoCentroDemo(3, "Centro de Adopción Miraflores", "Miraflores", "🏢"),
    InfoCentroDemo(4, "Hogar Temporal Ate Vitarte", "Hogar Ate", "🐾"),
    InfoCentroDemo(5, "Módulo Animal Comas", "Módulo Comas", "🏥")
)

@Composable
fun PantallaCentro(
    conversaciones: List<ConversacionCentro>,
    abrirConversacion: (idCentro: Int, correoAdoptante: String, nombreCentro: String) -> Unit,
    cerrarSesion: () -> Unit,
    idCentroActualInicial: Int = 1,
    modifier: Modifier = Modifier
) {
    val centroActual = CentrosDisponiblesDemo.firstOrNull { it.id == idCentroActualInicial }
        ?: InfoCentroDemo(idCentroActualInicial, "Centro de Adopción", "Centro", "🏡")
    val conversacionesFiltradas = conversaciones.filter { it.idCentro == idCentroActualInicial }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdoptMeBg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // =====================================================================
        // ENCABEZADO DEL CENTRO
        // =====================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // 1. Icono, título y nombre del centro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AdoptOrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = centroActual.icono, fontSize = 24.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Panel del Centro de Adopción",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AdoptTextDark
                        )
                        Text(
                            text = centroActual.nombre,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AdoptOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Insignias descriptivas sin comprimir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Rol: Centro (ID #${centroActual.id})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier.weight(1f, fill = false),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Mensajería demostrativa",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Botón Cerrar sesión en fila independiente con ancho completo
                OutlinedButton(
                    onClick = cerrarSesion,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logout),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Cerrar sesión",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================================
        // LISTADO DE CONVERSACIONES AISLADAS DEL CENTRO SELECCIONADO
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Conversaciones recibidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdoptTextDark
                )
                Text(
                    text = "Almacenamiento local del dispositivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdoptTextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (conversacionesFiltradas.isNotEmpty()) AdoptTealLight else AdoptSurfaceVariant
            ) {
                Text(
                    text = "${conversacionesFiltradas.size} para este centro",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (conversacionesFiltradas.isNotEmpty()) AdoptTeal else AdoptTextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (conversacionesFiltradas.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AdoptCardWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📬", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sin mensajes para ${centroActual.nombreCorto}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AdoptTextDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Solo se muestran los mensajes dirigidos a este centro (ID #${centroActual.id}). Los chats de otras casas de adopción no aparecerán aquí.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdoptTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(conversacionesFiltradas, key = { "${it.idCentro}_${it.correoAdoptante}" }) { conv ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(AdoptTealLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "👤", fontSize = 18.sp)
                                    }

                                    Column {
                                        Text(
                                            text = conv.correoAdoptante,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AdoptTextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AdoptOrangeLight
                                            ) {
                                                Text(
                                                    text = "Centro #${conv.idCentro}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AdoptOrange,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }

                                            Text(
                                                text = conv.nombreCentro,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AdoptTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (conv.fecha.isNotBlank()) {
                                    Text(
                                        text = conv.fecha,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdoptTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Vista previa del último mensaje
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AdoptSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = conv.ultimoTexto.ifBlank { "Sin mensajes" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdoptTextDark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    abrirConversacion(conv.idCentro, conv.correoAdoptante, conv.nombreCentro)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AdoptTeal,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Abrir conversación",
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
