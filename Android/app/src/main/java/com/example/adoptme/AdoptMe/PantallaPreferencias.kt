package com.example.adoptme.AdoptMe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import com.example.adoptme.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PantallaPreferencias(
    temaActual: String,
    tamanoTextoActual: String,
    colorActual: String,

    mascotaPreferida: String,
    ubicacionPreferida: String,
    ordenPredeterminado: String,
    soloDisponibles: Boolean,

    notificarNuevasMascotas: Boolean,
    notificarAdopciones: Boolean,
    notificarMensajes: Boolean,

    cambiarTema: (String) -> Unit,
    cambiarTamanoTexto: (String) -> Unit,
    cambiarColor: (String) -> Unit,

    cambiarMascotaPreferida: (String) -> Unit,
    cambiarUbicacionPreferida: (String) -> Unit,
    cambiarOrden: (String) -> Unit,
    cambiarSoloDisponibles: (Boolean) -> Unit,

    cambiarNotificacionMascotas: (Boolean) -> Unit,
    cambiarNotificacionAdopciones: (Boolean) -> Unit,
    cambiarNotificacionMensajes: (Boolean) -> Unit,

    restablecer: () -> Unit,
    volver: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        TextButton(
            onClick = volver
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Volver",
                    modifier = Modifier.padding(end = 4.dp).size(18.dp)
                )
                Text("Volver")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Preferencias",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "Personaliza AdoptMe según tus preferencias.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =====================================================
        // APARIENCIA
        // =====================================================

        SeccionPreferencia(
            titulo = "🎨 Apariencia"
        ) {

            Text(
                text = "Tema",
                fontWeight = FontWeight.Bold
            )

            FilaOpciones(
                opciones = listOf(
                    "Sistema",
                    "Claro",
                    "Oscuro"
                ),
                seleccionada = temaActual,
                seleccionar = cambiarTema
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Tamaño del texto",
                fontWeight = FontWeight.Bold
            )

            FilaOpciones(
                opciones = listOf(
                    "Pequeño",
                    "Normal",
                    "Grande"
                ),
                seleccionada = tamanoTextoActual,
                seleccionar = cambiarTamanoTexto
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Color principal",
                fontWeight = FontWeight.Bold
            )

            FilaOpciones(
                opciones = listOf(
                    "Verde",
                    "Azul",
                    "Morado"
                ),
                seleccionada = colorActual,
                seleccionar = cambiarColor
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =====================================================
        // MASCOTA PREFERIDA
        // =====================================================

        SeccionPreferencia(
            titulo = "🐾 Mascota preferida"
        ) {

            Text(
                text = "¿Qué tipo de mascota prefieres ver?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            FilaOpciones(
                opciones = listOf(
                    "Todas",
                    "Perros",
                    "Gatos",
                    "Conejos"
                ),
                seleccionada = mascotaPreferida,
                seleccionar = cambiarMascotaPreferida
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =====================================================
        // UBICACIÓN
        // =====================================================

        SeccionPreferencia(
            titulo = "📍 Ubicación preferida"
        ) {

            Text(
                text = "Selecciona la zona que deseas usar por defecto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            FilaOpciones(
                opciones = listOf(
                    "Todas",
                    "Lima",
                    "San Juan de Lurigancho",
                    "Miraflores",
                    "Santiago de Surco",
                    "Ate",
                    "Comas",
                    "San Martín de Porres",
                    "La Molina",
                    "Callao"
                ),
                seleccionada = ubicacionPreferida,
                seleccionar = cambiarUbicacionPreferida
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =====================================================
        // SOLO DISPONIBLES
        // =====================================================

        SeccionPreferencia(
            titulo = "🏠 Estado de adopción"
        ) {

            OpcionInterruptor(
                titulo = "Mostrar solo disponibles",
                descripcion =
                    "Oculta mascotas adoptadas o que se encuentren en proceso.",
                activado = soloDisponibles,
                cambiar = cambiarSoloDisponibles
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =====================================================
        // ORDEN
        // =====================================================

        SeccionPreferencia(
            titulo = "📋 Orden predeterminado"
        ) {

            Text(
                text = "Selecciona cómo quieres ver las mascotas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            FilaOpciones(
                opciones = listOf(
                    "Más recientes",
                    "Nombre A-Z"
                ),
                seleccionada = ordenPredeterminado,
                seleccionar = cambiarOrden
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =====================================================
        // NOTIFICACIONES
        // =====================================================

        SeccionPreferencia(
            titulo = "🔔 Notificaciones"
        ) {

            OpcionInterruptor(
                titulo = "Nuevas mascotas",
                descripcion =
                    "Recibir avisos cuando se publiquen nuevas mascotas.",
                activado = notificarNuevasMascotas,
                cambiar = cambiarNotificacionMascotas
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OpcionInterruptor(
                titulo = "Estado de adopciones",
                descripcion =
                    "Recibir cambios sobre tus solicitudes de adopción.",
                activado = notificarAdopciones,
                cambiar = cambiarNotificacionAdopciones
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OpcionInterruptor(
                titulo = "Mensajes",
                descripcion =
                    "Recibir avisos relacionados con nuevos mensajes.",
                activado = notificarMensajes,
                cambiar = cambiarNotificacionMensajes
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        OutlinedButton(
            onClick = restablecer,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {

            Text(
                text = "Restablecer preferencias",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )
    }
}


// =============================================================
// TARJETA DE SECCIÓN
// =============================================================

@Composable
private fun SeccionPreferencia(
    titulo: String,
    contenido: @Composable () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            contenido()
        }
    }
}


// =============================================================
// BOTONES DE OPCIONES
// =============================================================

@Composable
private fun FilaOpciones(
    opciones: List<String>,
    seleccionada: String,
    seleccionar: (String) -> Unit
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        opciones.forEach { opcion ->

            if (opcion == seleccionada) {

                Button(
                    onClick = {
                        seleccionar(opcion)
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(10.dp)
                ) {

                    Text(
                        text = "✓ $opcion",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

            } else {

                OutlinedButton(
                    onClick = {
                        seleccionar(opcion)
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(10.dp)
                ) {

                    Text(
                        text = opcion
                    )
                }
            }
        }
    }
}


// =============================================================
// SWITCH
// =============================================================

@Composable
private fun OpcionInterruptor(
    titulo: String,
    descripcion: String,
    activado: Boolean,
    cambiar: (Boolean) -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = titulo,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text = descripcion,
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }

        Switch(
            checked = activado,
            onCheckedChange = cambiar
        )
    }
}