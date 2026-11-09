package com.example.adoptme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.R
import com.example.adoptme.model.CentroAdopcion

typealias CentroAdopcionDemo = CentroAdopcion

val CentrosDemostrativos = listOf(
    CentroAdopcionDemo(
        id = 1,
        nombre = "Albergue Huellitas del Norte - Lima (Demo)",
        ubicacion = "San Juan de Lurigancho, Lima",
        descripcion = "Espacio comunitario en SJL dedicado al cuidado y bienestar de perros y gatos rescatados en espera de una familia responsable.",
        horarioAtencion = "Lunes a Viernes de 9:00 a 17:00",
        contactoEjemplo = "huellitas.norte.demo@ejemplo.pe",
        direccionEjemplo = "Av. Próceres de la Independencia 1402, SJL (Dirección ficticia)",
        latitud = -12.0089,
        longitud = -77.0042
    ),
    CentroAdopcionDemo(
        id = 2,
        nombre = "Refugio Patitas Felices Surco (Demo)",
        ubicacion = "Santiago de Surco, Lima",
        descripcion = "Albergue demostrativo en Surco con programa de socialización y seguimiento post-adopción para mascotas rescatadas.",
        horarioAtencion = "Martes a Sábado de 10:00 a 18:00",
        contactoEjemplo = "patitas.surco.demo@ejemplo.pe",
        direccionEjemplo = "Calle Monte Rosa 180, Santiago de Surco (Dirección ficticia)",
        latitud = -12.1384,
        longitud = -76.9856
    ),
    CentroAdopcionDemo(
        id = 3,
        nombre = "Centro de Adopción Miraflores (Demo)",
        ubicacion = "Miraflores, Lima",
        descripcion = "Iniciativa ficticia en Miraflores enfocada en la sensibilización vecinal y adopción responsable de mascotas en buen estado de salud.",
        horarioAtencion = "Lunes a Domingo de 10:00 a 16:00",
        contactoEjemplo = "adopcion.miraflores.demo@ejemplo.pe",
        direccionEjemplo = "Av. Larco 345, Miraflores (Dirección ficticia)",
        latitud = -12.1221,
        longitud = -77.0305
    ),
    CentroAdopcionDemo(
        id = 4,
        nombre = "Hogar Temporal Ate Vitarte (Demo)",
        ubicacion = "Ate Vitarte, Lima",
        descripcion = "Red de hogares de tránsito en Ate que acoge animales rescatados de la calle mientras encuentran familia definitiva.",
        horarioAtencion = "Miércoles a Domingo de 11:00 a 17:00",
        contactoEjemplo = "hogar.ate.demo@ejemplo.pe",
        direccionEjemplo = "Av. Nicolás Ayllón 2890, Ate (Dirección ficticia)",
        latitud = -12.0461,
        longitud = -76.9214
    ),
    CentroAdopcionDemo(
        id = 5,
        nombre = "Módulo Animal Comas (Demo)",
        ubicacion = "Comas, Lima",
        descripcion = "Módulo demostrativo en Comas para atención preventiva, resguardo temporal y coordinación de adopciones en la zona norte de Lima.",
        horarioAtencion = "Lunes a Viernes de 8:30 a 16:30",
        contactoEjemplo = "modulo.comas.demo@ejemplo.pe",
        direccionEjemplo = "Av. Universitaria Norte 4500, Comas (Dirección ficticia)",
        latitud = -11.9325,
        longitud = -77.0542
    )
)

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
    modifier: Modifier = Modifier
) {
    var centroParaDetalle by remember { mutableStateOf<CentroAdopcionDemo?>(null) }
    var centroParaContactar by remember { mutableStateOf<CentroAdopcionDemo?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Botón superior de regreso a Inicio
        TextButton(
            onClick = volver,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = "← Volver a Inicio",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Título de la pantalla
        Text(
            text = "🏡 Centros de Adopción",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Indicador obligatorio de prototipo
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(
                text = "ℹ️ Información demostrativa del prototipo",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Botón para ver en el mapa interactivo
        Button(
            onClick = abrirMapa,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_center),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("🗺️ Ver centros y mascotas en el mapa", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de centros demostrativos
        LazyColumn(
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = CentrosDemostrativos,
                key = { it.id }
            ) { centro ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Encabezado del centro
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = centro.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Ubicación
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 ${centro.ubicacion}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Dirección de ejemplo
                        Text(
                            text = "🏠 Dirección: ${centro.direccionEjemplo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Descripción breve
                        Text(
                            text = centro.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Horario de atención
                        Text(
                            text = "🕒 Horario: ${centro.horarioAtencion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Contacto de ejemplo
                        Text(
                            text = "✉️ Contacto: ${centro.contactoEjemplo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Botones de acción
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { centroParaDetalle = centro },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Ver centro",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    abrirChat(
                                        centro.id,
                                        centro.nombre,
                                        "centro@adoptme.com",
                                        centro.ubicacion
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chat),
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp).size(18.dp)
                                    )
                                    Text(
                                        text = "Chatear",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo "Ver centro"
    if (centroParaDetalle != null) {
        val centro = centroParaDetalle!!
        AlertDialog(
            onDismissRequest = { centroParaDetalle = null },
            title = {
                Text(
                    text = centro.nombre,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Información demostrativa del prototipo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "📍 Ubicación: ${centro.ubicacion}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "🏠 Dirección: ${centro.direccionEjemplo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🕒 Horario de atención: ${centro.horarioAtencion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "✉️ Contacto: ${centro.contactoEjemplo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = centro.descripcion,
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

    // Diálogo "Contactar"
    if (centroParaContactar != null) {
        val centro = centroParaContactar!!
        AlertDialog(
            onDismissRequest = { centroParaContactar = null },
            title = {
                Text(
                    text = "Contactar al centro",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Información demostrativa del prototipo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Centro: ${centro.nombre}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Para coordinar visitas o consultas sobre mascotas en adopción, utiliza este canal de contacto de ejemplo:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "✉️ Canal: ${centro.contactoEjemplo}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "🕒 Horario sugerido: ${centro.horarioAtencion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { centroParaContactar = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Entendido")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
