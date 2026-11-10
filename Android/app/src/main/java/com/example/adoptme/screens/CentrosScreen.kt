package com.example.adoptme.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.adoptme.R
import com.example.adoptme.model.CentroAdopcion
import com.example.adoptme.model.CentroPublicoDto
import com.example.adoptme.model.toCentroAdopcion
import com.example.adoptme.viewmodel.CentrosUiState
import com.example.adoptme.viewmodel.CentrosViewModel

@Composable
fun CentrosScreen(
    volver: () -> Unit = {},
    abrirChat: (
        idCentro: Int,
        nombreCentro: String,
        correoCentro: String,
        ubicacion: String
    ) -> Unit = { _, _, _, _ -> },
    abrirMapa: () -> Unit = {},
    viewModel: CentrosViewModel = remember { CentrosViewModel() },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var centroParaDetalle by remember { mutableStateOf<CentroPublicoDto?>(null) }
    var centroParaContactar by remember { mutableStateOf<CentroPublicoDto?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Botón superior de regreso
        TextButton(
            onClick = volver,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = "← Volver al Inicio",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Título de la pantalla
        Text(
            text = "Centros de Adopción",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Centros y albergues verificados por el equipo administrativo de AdoptMe.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        when (val state = uiState) {
            is CentrosUiState.Cargando -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cargando centros de adopción...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is CentrosUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("⚠️", fontSize = 32.sp)
                            Text(
                                text = "No se pudieron cargar los centros",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.mensaje,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.reintentar() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
            is CentrosUiState.Exito -> {
                if (state.centros.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🏡", fontSize = 36.sp)
                                Text(
                                    text = "No hay centros disponibles",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Actualmente no existen centros de adopción aprobados y activos en esta sección.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.reintentar() }
                                ) {
                                    Text("Actualizar")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(
                            items = state.centros,
                            key = { it.id }
                        ) { centro ->
                            TarjetaCentroPublico(
                                centro = centro,
                                alTocarDetalle = { centroParaDetalle = centro },
                                alTocarContactar = {
                                    abrirChat(
                                        centro.id,
                                        centro.nombre,
                                        centro.correoContacto.ifBlank { "contacto@centro${centro.id}.pe" },
                                        centro.distrito.ifBlank { centro.direccion }
                                    )
                                },
                                alTocarVerEnMapa = abrirMapa
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo "Detalle del centro"
    if (centroParaDetalle != null) {
        val c = centroParaDetalle!!
        AlertDialog(
            onDismissRequest = { centroParaDetalle = null },
            title = {
                Text(
                    text = c.nombre,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c.esDemostrativo) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Albergue Demostrativo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "📍 Distrito: ${c.distrito.ifBlank { "No especificado" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "🏠 Dirección: ${c.direccion.ifBlank { "No especificada" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "🕒 Horario: ${c.horario.ifBlank { "No especificado" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "📞 Teléfono: ${c.telefono.ifBlank { "No especificado" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "✉️ Correo: ${c.correoContacto.ifBlank { "No especificado" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = c.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { centroParaDetalle = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cerrar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun TarjetaCentroPublico(
    centro: CentroPublicoDto,
    alTocarDetalle: () -> Unit,
    alTocarContactar: () -> Unit,
    alTocarVerEnMapa: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logotipo remoto con Coil o placeholder institucional
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!centro.logoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(contexto)
                                .data(centro.logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo de ${centro.nombre}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            error = {
                                Text("🏡", fontSize = 24.sp)
                            }
                        )
                    } else {
                        Text("🏡", fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = centro.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (centro.distrito.isNotBlank()) "📍 ${centro.distrito}" else "📍 Lima",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (centro.esDemostrativo) {
                        Text(
                            text = "Albergue Demostrativo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = centro.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (centro.horario.isNotBlank()) {
                Text(
                    text = "🕒 ${centro.horario}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (centro.telefono.isNotBlank()) {
                Text(
                    text = "📞 ${centro.telefono}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = alTocarDetalle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Detalle", fontSize = 12.sp)
                }

                if (centro.latitud != null && centro.longitud != null) {
                    OutlinedButton(
                        onClick = alTocarVerEnMapa,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ver en mapa", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = alTocarContactar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Chat", fontSize = 12.sp)
                }
            }
        }
    }
}
