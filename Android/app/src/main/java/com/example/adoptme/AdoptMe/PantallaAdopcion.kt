package com.example.adoptme.AdoptMe

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.res.painterResource
import com.example.adoptme.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.Pet

@Composable
fun PantallaAdopcion(
    mascota: Pet,
    volver: () -> Unit,
    enviar: (AdoptionRequest) -> Unit,
    modifier: Modifier = Modifier,
    esPublicacionPropia: Boolean = false,
    correoUsuarioActual: String = ""
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    // Prellenado obligatorio con el correo del usuario que inició sesión
    var correo by remember(correoUsuarioActual) { mutableStateOf(correoUsuarioActual) }
    var direccion by remember { mutableStateOf("") }
    var tipoVivienda by remember { mutableStateOf("") }
    var disponibilidadTiempo by remember { mutableStateOf("") }
    var experienciaPrevia by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }

    var mensajeError by remember { mutableStateOf("") }

    val estaDisponible = mascota.status.equals("Disponible", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        TextButton(
            onClick = volver,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Volver",
                    modifier = Modifier.padding(end = 4.dp).size(18.dp)
                )
                Text("Cancelar", fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            text = "Formulario de Adopción",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tarjeta resumen de la mascota solicitada
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🐾 Postulación para adoptar a ${mascota.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${mascota.species} • ${mascota.breed} • ${mascota.age} • ${mascota.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Los campos marcados con (*) son obligatorios para evaluar tu postulación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (esPublicacionPropia) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "⚠️ No puedes solicitar la adopción de una mascota publicada por ti.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else if (!estaDisponible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "⚠️ Esta mascota no está disponible para nuevas solicitudes (${mascota.status}).",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // SECCIÓN 1: DATOS PERSONALES
        Text(
            text = "📋 Datos personales y de contacto",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                if (mensajeError.isNotEmpty()) mensajeError = ""
            },
            label = { Text("Nombre completo *") },
            placeholder = { Text("Ej: Juan Pérez") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = {
                telefono = it
                if (mensajeError.isNotEmpty()) mensajeError = ""
            },
            label = { Text("Teléfono de contacto *") },
            placeholder = { Text("Ej: 987654321") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Campo de correo bloqueado y prellenado con la cuenta del usuario activo
        OutlinedTextField(
            value = correo,
            onValueChange = { /* readOnly: no se permite editar el correo */ },
            readOnly = true,
            label = { Text("Correo electrónico * (Cuenta activa)") },
            placeholder = { Text("ejemplo@correo.com") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = {
                direccion = it
                if (mensajeError.isNotEmpty()) mensajeError = ""
            },
            label = { Text("Dirección o distrito de residencia *") },
            placeholder = { Text("Ej: Av. Las Palmeras 123, Miraflores") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // SECCIÓN 2: ENTORNO Y HOGAR
        Text(
            text = "🏠 Entorno y cuidados",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = tipoVivienda,
            onValueChange = {
                tipoVivienda = it
                if (mensajeError.isNotEmpty()) mensajeError = ""
            },
            label = { Text("Tipo de vivienda *") },
            placeholder = { Text("Ej: Casa con patio, Departamento...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = disponibilidadTiempo,
            onValueChange = { disponibilidadTiempo = it },
            label = { Text("Disponibilidad de tiempo") },
            placeholder = { Text("Ej: Tiempo completo, Tardes y fines de semana...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = experienciaPrevia,
            onValueChange = { experienciaPrevia = it },
            label = { Text("Experiencia previa con mascotas") },
            placeholder = { Text("Ej: Sí, he tenido perros antes / Primera vez") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // SECCIÓN 3: MOTIVACIÓN
        Text(
            text = "💬 Motivación y compromiso",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = motivo,
            onValueChange = {
                motivo = it
                if (mensajeError.isNotEmpty()) mensajeError = ""
            },
            label = { Text("¿Por qué deseas adoptar a ${mascota.name}? *") },
            placeholder = { Text("Cuéntanos el motivo y tu compromiso con su cuidado...") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        if (mensajeError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "⚠️ $mensajeError",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val correoFinal = if (correoUsuarioActual.isNotBlank()) correoUsuarioActual.trim() else correo.trim()

                if (esPublicacionPropia) {
                    mensajeError = "No puedes solicitar la adopción de una mascota publicada por ti."
                } else if (!estaDisponible) {
                    mensajeError = "Esta mascota no está disponible para nuevas solicitudes."
                } else if (
                    nombre.isBlank() ||
                    telefono.isBlank() ||
                    correoFinal.isBlank() ||
                    direccion.isBlank() ||
                    tipoVivienda.isBlank() ||
                    motivo.isBlank()
                ) {
                    mensajeError = "Por favor completa todos los campos obligatorios (*)."
                } else if (!esTelefonoPeruanoValido(telefono)) {
                    mensajeError = "El teléfono debe tener 9 dígitos y empezar con 9."
                } else if (!esFormatoCorreoValido(correoFinal)) {
                    mensajeError = "Ingresa un correo electrónico con formato válido."
                } else {
                    mensajeError = ""
                    val solicitud = AdoptionRequest(
                        petId = mascota.id,
                        petName = mascota.name,
                        petSpecies = mascota.species,
                        applicantName = nombre.trim(),
                        phone = normalizarTelefonoPeruano(telefono),
                        email = correoFinal,
                        address = direccion.trim(),
                        housingType = tipoVivienda.trim(),
                        timeAvailability = disponibilidadTiempo.trim(),
                        previousExperience = experienciaPrevia.trim(),
                        reason = motivo.trim(),
                        status = "Pendiente"
                    )
                    enviar(solicitud)
                }
            },
            enabled = estaDisponible && !esPublicacionPropia,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Enviar solicitud",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
