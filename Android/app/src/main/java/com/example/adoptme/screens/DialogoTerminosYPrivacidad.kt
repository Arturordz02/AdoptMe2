package com.example.adoptme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.adoptme.R
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary

/**
 * Diálogo accesible de Política de Privacidad y Términos y Condiciones.
 * Entrega #4: Transparencia en el uso de geolocalización, permisos y datos.
 */
@Composable
fun DialogoTerminosYPrivacidad(
    pestanaInicial: Int = 0, // 0 = Privacidad, 1 = Términos
    yaAceptado: Boolean = false,
    fechaAceptacion: String? = null,
    modoSoloLectura: Boolean = false,
    alAceptar: () -> Unit = {},
    alCerrar: () -> Unit = {},
    onAceptar: () -> Unit = alAceptar,
    onCerrar: () -> Unit = alCerrar
) {
    var pestanaSeleccionada by remember { mutableIntStateOf(pestanaInicial) }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Cabecera con selector de pestaña
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pestanaSeleccionada == 0) "🛡️ Política de Privacidad" else "📜 Términos y Condiciones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTextDark
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = pestanaSeleccionada,
                    containerColor = AdoptMeBg,
                    contentColor = AdoptTeal
                ) {
                    Tab(
                        selected = pestanaSeleccionada == 0,
                        onClick = { pestanaSeleccionada = 0 },
                        text = { Text("Privacidad y GPS", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = pestanaSeleccionada == 1,
                        onClick = { pestanaSeleccionada = 1 },
                        text = { Text("Términos de Uso", fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Contenido desplazable con scroll
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (pestanaSeleccionada == 0) {
                        SeccionPrivacidad()
                    } else {
                        SeccionTerminos()
                    }

                    if (yaAceptado && fechaAceptacion != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = AdoptTeal.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ Aceptaste estos términos y políticas el $fechaAceptacion",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdoptTeal,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCerrar,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar")
                    }

                    if (!yaAceptado && !modoSoloLectura) {
                        Spacer(modifier = Modifier.size(10.dp))
                        Button(
                            onClick = {
                                onAceptar()
                                onCerrar()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Aceptar y Continuar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionPrivacidad() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ParrafoInformativo(
            titulo = "1. Uso de fotografías y almacenamiento en la nube",
            cuerpo = "Las fotografías que captures con la cámara o selecciones desde tu galería privada se emplean exclusivamente para publicar fichas de mascotas. Las imágenes procesadas se almacenan de forma segura en la infraestructura de Cloudinary, y su URL pública HTTPS se asocia al registro de la mascota en nuestra base de datos Neon PostgreSQL para que otros usuarios puedan visualizarla."
        )
        ParrafoInformativo(
            titulo = "2. Acceso granular a galería mediante Photo Picker",
            cuerpo = "AdoptMe utiliza el Photo Picker oficial de Android. La aplicación no inspecciona toda tu galería ni solicita permisos amplios de almacenamiento; solo recibe acceso temporal a la fotografía específica que tú selecciones de manera explícita."
        )
        ParrafoInformativo(
            titulo = "3. Uso exclusivo y voluntario de la cámara",
            cuerpo = "El acceso a la cámara se solicita únicamente cuando pulsas 'Tomar foto' dentro del formulario. AdoptMe no captura imágenes ni graba video en segundo plano bajo ninguna circunstancia."
        )
        ParrafoInformativo(
            titulo = "4. Finalidad del permiso de ubicación",
            cuerpo = "AdoptMe solicita acceso a tu ubicación aproximada o precisa (GPS) exclusivamente para calcular la distancia a centros de adopción y mascotas cercanas, así como para posicionar el mapa interactivo. Ni la ubicación ni las fotografías se emplean para rastreo continuo."
        )
        ParrafoInformativo(
            titulo = "5. No almacenamos tu ubicación en tiempo real",
            cuerpo = "Tu ubicación actual es procesada localmente en tu dispositivo. AdoptMe NO rastrea, no almacena en segundo plano ni envía tus coordenadas personales a ningún servidor sin tu consentimiento expreso."
        )
        ParrafoInformativo(
            titulo = "6. Eliminación de datos y fotografías",
            cuerpo = "El usuario creador o un Administrador pueden eliminar su publicación en cualquier momento desde la aplicación. Dicha acción remueve el registro en la base de datos y elimina el recurso multimedia alojado en Cloudinary."
        )
    }
}

@Composable
private fun SeccionTerminos() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ParrafoInformativo(
            titulo = "1. Compromiso de Adopción Responsable (Ley N.º 30407)",
            cuerpo = "AdoptMe es una plataforma sin fines de lucro destinada a conectar rescatistas, albergues y adoptantes en el marco de la Ley de Protección y Bienestar Animal (Ley N.º 30407). Queda estrictamente prohibida la venta, comercialización o explotación de animales."
        )
        ParrafoInformativo(
            titulo = "2. Veracidad de la Información y Fotografías",
            cuerpo = "El usuario que publica una mascota se compromete a brindar datos verídicos sobre su estado de salud, temperamento, edad, ubicación aproximada y fotografías actuales y reales del animal."
        )
        ParrafoInformativo(
            titulo = "3. Evaluación de Solicitudes",
            cuerpo = "El envío de una solicitud de adopción no garantiza la entrega de la mascota. Cada centro o rescatista evalúa las postulaciones bajo sus propios criterios éticos y de seguridad."
        )
        ParrafoInformativo(
            titulo = "4. Uso Seguro del Mapa",
            cuerpo = "Se recomienda realizar las visitas y entregas en lugares públicos y seguros, o en las instalaciones oficiales de los centros de adopción verificados."
        )
    }
}

@Composable
private fun ParrafoInformativo(titulo: String, cuerpo: String) {
    Column {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AdoptTextDark
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = cuerpo,
            style = MaterialTheme.typography.bodyMedium,
            color = AdoptTextSecondary,
            lineHeight = 19.sp
        )
    }
}
