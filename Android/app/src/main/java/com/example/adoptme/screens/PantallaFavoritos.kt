package com.example.adoptme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adoptme.R
import com.example.adoptme.components.TarjetaMascota
import com.example.adoptme.model.Pet

@Composable
fun PantallaFavoritos(
    favoritos: List<Pet>,
    volver: () -> Unit,
    seleccionarMascota: (Pet) -> Unit,
    cambiarFavorito: (Pet) -> Unit,
    modifier: Modifier = Modifier,
    correoActual: String = ""
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
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
                Text("Volver", fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            text = "Mis Favoritos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (favoritos.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite_border),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Todavía no has agregado mascotas a tus favoritos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    items = favoritos.distinctBy { it.claveCompose },
                    key = { it.claveCompose }
                ) { mascota ->
                    val esPropia = mascota.creadoPorCorreo.isNotBlank() &&
                            mascota.creadoPorCorreo.equals(correoActual, ignoreCase = true)
                    TarjetaMascota(
                        mascota = mascota,
                        esFavorito = true,
                        cambiarFavorito = { cambiarFavorito(mascota) },
                        verDetalle = { seleccionarMascota(mascota) },
                        esPublicacionPropia = esPropia
                    )
                }
            }
        }
    }
}
