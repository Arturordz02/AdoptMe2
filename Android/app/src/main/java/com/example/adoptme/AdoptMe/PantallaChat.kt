package com.example.adoptme.AdoptMe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.model.MensajeChat
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptOrangeLight
import com.example.adoptme.ui.theme.AdoptSurfaceVariant
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealLight
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary
import com.example.adoptme.ui.theme.BorderWarm

@Composable
fun PantallaChat(
    idCentro: Int = 1,
    nombreCentro: String = "Albergue Huellitas del Norte",
    correoCentro: String = "centro@adoptme.com",
    correoUsuarioActual: String = "usuario@adoptme.com",
    rolUsuario: String = "Adoptante",
    correoAdoptante: String = "usuario@adoptme.com",
    mensajes: List<MensajeChat> = emptyList(),
    enviarMensaje: (texto: String) -> Unit = {},
    volver: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var textoMensaje by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje al cambiar la lista
    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdoptMeBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // =====================================================================
        // ENCABEZADO DE CONVERSACIÓN
        // =====================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (volver != null) {
                    IconButton(
                        onClick = volver,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AdoptSurfaceVariant)
                    ) {
                        Text(
                            text = "←",
                            color = AdoptTextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (rolUsuario == "Centro") AdoptOrangeLight else AdoptTealLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (rolUsuario == "Centro") "👤" else "🏡",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (rolUsuario == "Centro") {
                            "Conversación con $correoAdoptante"
                        } else {
                            "Chat con $nombreCentro"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTextDark,
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (rolUsuario == "Centro") AdoptTealLight else AdoptOrangeLight
                        ) {
                            Text(
                                text = if (rolUsuario == "Centro") "Centro (id #$idCentro)" else "Centro #$idCentro",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rolUsuario == "Centro") AdoptTeal else AdoptOrange,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = if (rolUsuario == "Centro") nombreCentro else "En línea",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdoptTextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // =====================================================================
        // LISTA DE MENSAJES
        // =====================================================================
        if (mensajes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AdoptCardWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💬", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aún no hay mensajes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AdoptTextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (rolUsuario == "Centro") {
                                "El adoptante no ha enviado consultas a este centro todavía."
                            } else {
                                "Escribe un mensaje para consultar sobre la adopción responsable con este centro."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdoptTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mensajes, key = { it.id }) { mensaje ->
                    val esPropio = if (rolUsuario == "Centro") {
                        mensaje.esDelCentro
                    } else {
                        !mensaje.esDelCentro
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (esPropio) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (esPropio) Alignment.End else Alignment.Start,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            // Etiqueta del remitente
                            Text(
                                text = when {
                                    esPropio -> "Tú"
                                    mensaje.esDelCentro -> "🏢 ${mensaje.nombreCentro}"
                                    else -> "👤 Adoptante (${mensaje.remitente})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = AdoptTextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            // Burbuja del mensaje
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (esPropio) 16.dp else 2.dp,
                                    bottomEnd = if (esPropio) 2.dp else 16.dp
                                ),
                                color = when {
                                    esPropio -> AdoptTeal
                                    mensaje.esDelCentro -> AdoptOrangeLight
                                    else -> AdoptCardWhite
                                },
                                shadowElevation = 1.dp,
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = if (esPropio) AdoptTeal else BorderWarm,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (esPropio) 16.dp else 2.dp,
                                        bottomEnd = if (esPropio) 2.dp else 16.dp
                                    )
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = mensaje.texto,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (esPropio) Color.White else AdoptTextDark
                                    )

                                    if (mensaje.fecha.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = mensaje.fecha,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (esPropio) Color.White.copy(alpha = 0.75f) else AdoptTextSecondary,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // =====================================================================
        // CAMPO DE ENTRADA Y BOTÓN ENVIAR
        // =====================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textoMensaje,
                    onValueChange = { textoMensaje = it },
                    placeholder = {
                        Text(
                            text = if (rolUsuario == "Centro") "Escribe una respuesta..." else "Escribe una consulta...",
                            color = AdoptTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AdoptTeal,
                        unfocusedBorderColor = BorderWarm
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val textoLimpio = textoMensaje.trim()
                        if (textoLimpio.isNotBlank()) {
                            enviarMensaje(textoLimpio)
                            textoMensaje = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdoptOrange,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        text = "Enviar",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
