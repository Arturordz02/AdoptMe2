package com.example.adoptme.AdoptMe

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adoptme.R
import com.example.adoptme.model.Pet

@Composable
fun PantallaDetalleMascota(
    mascota: Pet,
    volver: () -> Unit,
    adoptar: () -> Unit,
    esPublicacionPropia: Boolean = false
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // BOTÓN VOLVER
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

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // FOTO DE LA MASCOTA
        if (mascota.photoBitmap != null) {

            Image(
                bitmap = mascota.photoBitmap.asImageBitmap(),
                contentDescription = "Foto de ${mascota.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(
                        RoundedCornerShape(20.dp)
                    ),
                contentScale = ContentScale.Crop
            )

        } else if (mascota.image != 0) {

            Image(
                painter = painterResource(
                    id = mascota.image
                ),
                contentDescription = "Foto de ${mascota.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(
                        RoundedCornerShape(20.dp)
                    ),
                contentScale = ContentScale.Crop
            )
        } else {

            Image(
                painter = painterResource(
                    id = R.drawable.ic_pet_fallback
                ),
                contentDescription = "Foto de ${mascota.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(
                        RoundedCornerShape(20.dp)
                    ),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // NOMBRE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mascota.name,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (esPublicacionPropia) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "📢 Tu publicación",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // RAZA
        Text(
            text = mascota.breed,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // INFORMACIÓN GENERAL
        Text(
            text = "${mascota.species} • ${mascota.age} • ${mascota.gender}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // UBICACIÓN
        Text(
            text = "📍 ${mascota.location}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // ESTADO
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = when (mascota.status) {

                        "Disponible" ->
                            "🟢 Disponible"

                        "En proceso" ->
                            "🟡 En proceso"

                        "Adoptado" ->
                            "🔵 Adoptado"

                        else ->
                            mascota.status
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // DESCRIPCIÓN
        Text(
            text = "Sobre ${mascota.name}",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = mascota.description,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // ACCIÓN DE ADOPCIÓN
        if (esPublicacionPropia) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📢 Tu publicación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No puedes solicitar la adopción de una mascota publicada por ti.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else if (
            mascota.status.equals(
                "Disponible",
                ignoreCase = true
            )
        ) {
            Button(
                onClick = adoptar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_paw),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                )
                Text("Solicitar adopción", fontWeight = FontWeight.Bold)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Esta mascota actualmente no está disponible para nuevas solicitudes de adopción.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // BOTÓN COMPARTIR CON INTENT NATIVO (Criterio de comunicación de la T1)
        OutlinedButton(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Adopción de ${mascota.name} en AdoptMe"
                    )
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "¡Conoce a ${mascota.name}! Es un ${mascota.species} de ${mascota.age} ubicado en ${mascota.location}. Encuéntralo en AdoptMe y dale un hogar responsable."
                    )
                }
                val chooser = Intent.createChooser(shareIntent, "Compartir ficha de ${mascota.name}")
                context.startActivity(chooser)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_share),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(20.dp)
            )
            Text("Compartir ficha", fontWeight = FontWeight.SemiBold)
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )
    }
}