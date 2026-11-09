package com.example.adoptme.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.R

/**
 * Modelo de datos para las tarjetas informativas de consumo responsable (HU-11).
 */
private data class ItemConsumoResponsable(
    val id: String,
    val icono: String,
    val titulo: String,
    val bajada: String,
    val detalles: List<String>,
    val notaSeguridad: String? = null
)

/**
 * Pantalla informativa de Consumo Responsable para mascotas (HU-11 / PC4).
 * Enmarcada en el ODS 12: Producción y consumo responsables.
 */
@Composable
fun PantallaConsumoResponsable(
    volver: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { volver() }

    val scrollState = rememberScrollState()

    // Lista de recomendaciones éticas y prácticas
    val recomendaciones = remember {
        listOf(
            ItemConsumoResponsable(
                id = "cuidados",
                icono = "🌱",
                titulo = "Consejos para cuidar responsablemente",
                bajada = "Aprende hábitos simples que marcan la diferencia.",
                detalles = listOf(
                    "Evalúa las necesidades reales de tu mascota antes de adquirir nuevos productos o juguetes.",
                    "Prioriza artículos duraderos y seguros según la especie, tamaño y etapa de vida del animal.",
                    "Fomenta una rutina de cuidados preventivos y chequeos veterinarios oportunos para garantizar su bienestar integral."
                )
            ),
            ItemConsumoResponsable(
                id = "reutiliza",
                icono = "♻️",
                titulo = "Reutiliza accesorios en buen estado",
                bajada = "Dale una segunda vida a lo que ya tienes.",
                detalles = listOf(
                    "Limpia y desinfecta collares, correas, transportadoras y platos que conserven su estructura intacta.",
                    "Revisa periódicamente los cierres, mosquetones y costuras para confirmar que sigan ofreciendo máxima seguridad.",
                    "Adapta mantas y toallas usadas para fabricar camas o zonas de descanso confortables."
                ),
                notaSeguridad = "Criterio de seguridad: Nunca reutilices accesorios con piezas sueltas, alambres expuestos o deterioros que puedan ser ingeridos o lastimar al animal."
            ),
            ItemConsumoResponsable(
                id = "dona",
                icono = "💚",
                titulo = "Dona lo que ya no utilizas",
                bajada = "Tu generosidad también ayuda a los refugios.",
                detalles = listOf(
                    "Los albergues y centros de rescate suelen requerir mantas limpias, toallas, transportadoras y comederos en buen estado.",
                    "Coordina siempre de forma previa con el refugio para confirmar sus necesidades actuales y protocolos de entrega vigentes.",
                    "Asegúrate de entregar los artículos debidamente higienizados y listos para su uso."
                ),
                notaSeguridad = "Aviso importante: Por motivos sanitarios, no dones medicamentos sobrantes, tratamientos abiertos ni alimentos caducados o a granel."
            ),
            ItemConsumoResponsable(
                id = "intercambia",
                icono = "📦",
                titulo = "Intercambia productos en buen estado",
                bajada = "Comparte lo que ya no usas y dale un nuevo hogar.",
                detalles = listOf(
                    "Cuando un cachorro crece con rapidez, muchos de sus accesorios iniciales quedan prácticamente nuevos.",
                    "Intercambia transportadoras, vallas o abrigos limpios con amigos, familiares u otros adoptantes responsables.",
                    "Verifica siempre las condiciones higiénicas del producto antes de incorporarlo al entorno de tu mascota."
                )
            ),
            ItemConsumoResponsable(
                id = "reduce",
                icono = "🛒",
                titulo = "Evita compras innecesarias",
                bajada = "Consume con conciencia: menos es más.",
                detalles = listOf(
                    "Mide con precisión las raciones diarias de alimento para evitar el desperdicio de comida y prevenir sobrepeso.",
                    "Elige utensilios resistentes de acero inoxidable o cerámica frente a plásticos descartables de corta vida útil.",
                    "Evita modas pasajeras o artículos prescindibles que terminen convirtiéndose en residuos contaminantes."
                )
            )
        )
    }

    // Registro del elemento expandido (por defecto el primero expandido)
    var idExpandido by remember { mutableStateOf<String?>("cuidados") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Barra superior con botón volver
        Surface(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column {
                    Text(
                        text = "Consumo responsable",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ODS 12 · Prácticas sostenibles para mascotas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Contenido con desplazamiento vertical
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Banner Hero principal (estilo PC4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1B6B5C),
                                    Color(0xFFC04E1D)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.20f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🌱",
                                        fontSize = 22.sp
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Consumo responsable",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "ODS 12: Producción y consumo responsables",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                            }
                        }

                        Text(
                            text = "Pequeñas decisiones, un gran impacto para el planeta y los animales.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }
            }

            // Encabezado de la lista
            Text(
                text = "Guía de prácticas responsables",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            // Lista de tarjetas interactivas
            recomendaciones.forEach { item ->
                val estaExpandido = idExpandido == item.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(250))
                        .clickable {
                            idExpandido = if (estaExpandido) null else item.id
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (estaExpandido) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (estaExpandido) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (estaExpandido) 2.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Fila principal: icono, texto y chevron
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.icono,
                                        fontSize = 20.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.titulo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.bajada,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = if (estaExpandido) "▲" else "▶",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // Detalle desplegable
                        AnimatedVisibility(
                            visible = estaExpandido,
                            enter = fadeIn(animationSpec = tween(200)),
                            exit = fadeOut(animationSpec = tween(150))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                )

                                item.detalles.forEach { detalle ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = detalle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (item.notaSeguridad != null) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "⚠️",
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = item.notaSeguridad,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Banner inferior de cierre ético
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 12.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.adoptme_logo_original),
                        contentDescription = "Logo de AdoptMe",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(64.dp)
                    )

                    Text(
                        text = "¡Juntos por un mundo mejor!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Adoptar éticamente y cuidar con responsabilidad reduce el impacto ambiental y brinda una vida digna a los animales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}