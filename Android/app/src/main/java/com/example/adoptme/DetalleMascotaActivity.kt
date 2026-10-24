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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adoptme.AdoptMe.PantallaDetalleMascota
import com.example.adoptme.SharedPreferences.Preferencias
import com.example.adoptme.data.MascotaStorage
import com.example.adoptme.data.local.AdoptMeDatabase
import com.example.adoptme.data.local.MascotaVisitadaEntity
import com.example.adoptme.data.normalizarCorreoUsuario
import com.example.adoptme.data.remote.RetrofitClient
import com.example.adoptme.data.remote.toPet
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptMeTheme
import android.util.Log

class DetalleMascotaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val petId = intent.getIntExtra("petId", intent.getIntExtra("id", 0))
        val correoUsuarioActual = intent.getStringExtra("correoUsuarioActual") ?: ""

        val preferencias = Preferencias(this)
        val correoPref = if (correoUsuarioActual.isNotBlank()) correoUsuarioActual else Preferencias.CUENTA_DEFECTO
        val temaSeleccionado = preferencias.obtenerTema(correoPref)
        val colorSeleccionado = preferencias.obtenerColorPrincipal(correoPref)
        val tamanoTextoSeleccionado = preferencias.obtenerTamanoTexto(correoPref)

        val almacenamientoMascotas = MascotaStorage(this)

        // 1. Intentar resolver la mascota desde los extras del Intent (si MainActivity pasó los datos completos)
        val intentName = intent.getStringExtra("name") ?: intent.getStringExtra("nombre")
        val mascotaDesdeIntent = if (!intentName.isNullOrBlank()) {
            val especie = intent.getStringExtra("species") ?: ""
            val imgResource = intent.getIntExtra("image", 0).let { resId ->
                if (resId != 0) resId else when (especie.trim().lowercase()) {
                    "gato" -> R.drawable.luna
                    "conejo" -> R.drawable.mia
                    else -> R.drawable.max
                }
            }
            Pet(
                id = petId,
                name = intentName,
                species = especie,
                breed = intent.getStringExtra("breed") ?: "",
                age = intent.getStringExtra("age") ?: "",
                gender = intent.getStringExtra("gender") ?: "",
                location = intent.getStringExtra("location") ?: "Lima",
                status = intent.getStringExtra("status") ?: "Disponible",
                description = intent.getStringExtra("description") ?: "",
                image = imgResource,
                photoBitmap = almacenamientoMascotas.cargarMascotas().find {
                    it.id == petId || it.name.equals(intentName, ignoreCase = true)
                }?.photoBitmap,
                creadoPorCorreo = intent.getStringExtra("creadoPorCorreo") ?: "",
                esRemota = intent.getBooleanExtra("esRemota", petId > 0)
            )
        } else null

        // 2. Si no vino en el Intent, buscar en almacenamiento local confirmado
        val mascotaLocal = almacenamientoMascotas.cargarMascotas().find {
            it.id == petId || (intentName != null && it.name.equals(intentName, ignoreCase = true))
        }

        val mascotaInicial = mascotaDesdeIntent ?: mascotaLocal?.let { p ->
            if (p.photoBitmap == null && p.image == 0) {
                p.copy(image = R.drawable.ic_pet_fallback)
            } else {
                p
            }
        }

        setContent {
            AdoptMeTheme(
                tema = temaSeleccionado,
                colorPrincipal = colorSeleccionado,
                tamanoTexto = tamanoTextoSeleccionado
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var mascotaActual by remember { mutableStateOf(mascotaInicial) }
                    var cargando by remember { mutableStateOf(mascotaInicial == null && petId > 0) }

                    // Registro automático en SQLite (Room) para historial de visitas de la mascota real
                    LaunchedEffect(mascotaActual?.id, correoUsuarioActual) {
                        val m = mascotaActual
                        if (m != null && m.id > 0 && correoUsuarioActual.isNotBlank()) {
                            try {
                                AdoptMeDatabase.obtenerInstancia(applicationContext).mascotaVisitadaDao().registrarVisita(
                                    MascotaVisitadaEntity(
                                        correoUsuario = normalizarCorreoUsuario(correoUsuarioActual),
                                        mascotaId = m.id,
                                        nombre = m.name,
                                        especie = m.species,
                                        raza = m.breed,
                                        edad = m.age,
                                        genero = m.gender,
                                        ubicacion = m.location,
                                        estado = m.status,
                                        descripcion = m.description,
                                        imagenResId = m.image,
                                        creadoPorCorreo = m.creadoPorCorreo,
                                        fechaUltimaVisita = System.currentTimeMillis()
                                    )
                                )
                                Log.d("ROOM_SQLITE", "Visita registrada en SQLite: mascota ${m.id} (${m.name}) para $correoUsuarioActual")
                            } catch (e: Exception) {
                                Log.e("ROOM_SQLITE", "Error registrando visita en SQLite", e)
                            }
                        }
                    }

                    LaunchedEffect(petId) {
                        if (mascotaActual == null && petId > 0) {
                            try {
                                cargando = true
                                val respuesta = RetrofitClient.mascotasApi.obtenerMascotaPorId(petId)
                                if (respuesta.isSuccessful && respuesta.body() != null) {
                                    val dto = respuesta.body()!!
                                    val petMapeada = dto.toPet()
                                    val localConFoto = almacenamientoMascotas.cargarMascotas().find {
                                        it.id == petId || it.name.equals(dto.name, ignoreCase = true)
                                    }
                                    mascotaActual = if (localConFoto?.photoBitmap != null) {
                                        petMapeada.copy(photoBitmap = localConFoto.photoBitmap)
                                    } else {
                                        petMapeada
                                    }
                                }
                            } catch (e: Exception) {
                                // Error de red o servidor al consultar por ID
                            } finally {
                                cargando = false
                            }
                        }
                    }

                    if (cargando) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (mascotaActual == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Mascota no encontrada",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
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
                        val m = mascotaActual!!
                        val esPropia = m.creadoPorCorreo.isNotBlank() &&
                                m.creadoPorCorreo.equals(correoUsuarioActual, ignoreCase = true)

                        PantallaDetalleMascota(
                            mascota = m,
                            volver = {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            },
                            adoptar = {
                                val esMascotaPropiaActual = m.creadoPorCorreo.isNotBlank() &&
                                        m.creadoPorCorreo.equals(correoUsuarioActual, ignoreCase = true)

                                if (esMascotaPropiaActual) {
                                    Toast.makeText(
                                        this@DetalleMascotaActivity,
                                        "No puedes solicitar la adopción de una mascota publicada por ti.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else if (!m.status.equals("Disponible", ignoreCase = true)) {
                                    Toast.makeText(
                                        this@DetalleMascotaActivity,
                                        "La mascota ya no está disponible para nuevas solicitudes (Estado: ${m.status}).",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    // Todo es válido: devolver RESULT_OK con petId y nombre
                                    val resultIntent = Intent().apply {
                                        putExtra("petId", m.id)
                                        putExtra("id", m.id)
                                        putExtra("nombre", m.name)
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

