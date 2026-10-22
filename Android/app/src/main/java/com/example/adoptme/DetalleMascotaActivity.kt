package com.example.adoptme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adoptme.AdoptMe.PantallaDetalleMascota
import com.example.adoptme.data.MascotaStorage
import com.example.adoptme.data.PetRepository
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptMeTheme

class DetalleMascotaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val petId = intent.getIntExtra("petId", intent.getIntExtra("id", 0))
        val correoUsuarioActual = intent.getStringExtra("correoUsuarioActual") ?: ""

        val almacenamientoMascotas = MascotaStorage(this)
        val repositorio = PetRepository()

        // 1. Buscar primero entre las mascotas publicadas guardadas en MascotaStorage
        // y luego entre las mascotas base de PetRepository
        val mascotaPublicada = almacenamientoMascotas.cargarMascotas().find { it.id == petId }
        val mascotaEncontrada = mascotaPublicada ?: repositorio.getPets().find { it.id == petId }

        // Asegurar que si no tiene fotoBitmap ni imagen válida, se asigne el fallback seguro
        val mascota: Pet? = mascotaEncontrada?.let { p ->
            if (p.photoBitmap == null && p.image == 0) {
                p.copy(image = R.drawable.ic_pet_fallback)
            } else {
                p
            }
        }

        setContent {
            AdoptMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (mascota == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Mascota no encontrada",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        setResult(Activity.RESULT_CANCELED)
                                        finish()
                                    }
                                ) {
                                    Text("Volver al catálogo")
                                }
                            }
                        }
                    } else {
                        val esPropia = mascota.creadoPorCorreo.isNotBlank() &&
                                mascota.creadoPorCorreo.equals(correoUsuarioActual, ignoreCase = true)

                        PantallaDetalleMascota(
                            mascota = mascota,
                            volver = {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            },
                            adoptar = {
                                // Verificar nuevamente en almacenamiento que la mascota siga disponible y no sea propia
                                val mascotaActualizada = almacenamientoMascotas.cargarMascotas().find { it.id == petId }
                                    ?: repositorio.getPets().find { it.id == petId }

                                val esMascotaPropiaActual = mascotaActualizada?.creadoPorCorreo?.isNotBlank() == true &&
                                        mascotaActualizada.creadoPorCorreo.equals(correoUsuarioActual, ignoreCase = true)

                                if (mascotaActualizada == null) {
                                    Toast.makeText(
                                        this@DetalleMascotaActivity,
                                        "La mascota ya no se encuentra disponible en el catálogo.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else if (esMascotaPropiaActual) {
                                    Toast.makeText(
                                        this@DetalleMascotaActivity,
                                        "No puedes solicitar la adopción de una mascota publicada por ti.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else if (!mascotaActualizada.status.equals("Disponible", ignoreCase = true)) {
                                    Toast.makeText(
                                        this@DetalleMascotaActivity,
                                        "La mascota ya no está disponible para nuevas solicitudes (Estado: ${mascotaActualizada.status}).",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    // Todo es válido: devolver RESULT_OK con petId y nombre
                                    val resultIntent = Intent().apply {
                                        putExtra("petId", mascotaActualizada.id)
                                        putExtra("id", mascotaActualizada.id)
                                        putExtra("nombre", mascotaActualizada.name)
                                    }
                                    setResult(Activity.RESULT_OK, resultIntent)
                                    finish()
                                }
                            },
                            esPublicacionPropia = esPropia
                        )
                    }
                }
            }
        }
    }
}

