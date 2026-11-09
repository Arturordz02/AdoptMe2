package com.example.adoptme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.adoptme.R
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealDark
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary

@Composable
fun DialogoExplicacionUbicacion(
    alAceptar: () -> Unit,
    alRechazar: () -> Unit
) {
    Dialog(onDismissRequest = alRechazar) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = AdoptCardWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = AdoptTeal
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Permiso de ubicación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdoptTextDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "AdoptMe utiliza tu ubicación para mostrar mascotas y centros de adopción cercanos. Tu ubicación actual no se almacena sin tu consentimiento explícito.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdoptTextDark,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Si decides no conceder el permiso, podrás seguir usando todas las funciones e indicar manualmente tu distrito o punto en el mapa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdoptTextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = alRechazar) {
                        Text("Ahora no", color = AdoptTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = alAceptar,
                        colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Entendido / Continuar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
