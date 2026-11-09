package com.example.adoptme.components

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.adoptme.R
import com.example.adoptme.location.LocationHelper
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptMeBg
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealDark
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Punto por defecto: Plaza Mayor de Lima (-12.046374, -77.030583)
private val PUNTO_INICIAL_LIMA = LatLng(-12.046374, -77.030583)

@Composable
fun DialogoSelectorMapa(
    latitudInicial: Double?,
    longitudInicial: Double?,
    alConfirmar: (Double, Double, String?) -> Unit,
    alDescartar: () -> Unit
) {
    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var puntoSeleccionado by remember {
        mutableStateOf(
            if (latitudInicial != null && longitudInicial != null) {
                LatLng(latitudInicial, longitudInicial)
            } else {
                PUNTO_INICIAL_LIMA
            }
        )
    }

    var tieneUbicacionMarcada by remember {
        mutableStateOf(latitudInicial != null && longitudInicial != null)
    }

    var obteniendoGps by remember { mutableStateOf(false) }
    var mensajeGps by remember { mutableStateOf<String?>(null) }
    var modoManual by remember { mutableStateOf(false) }
    var latitudTexto by remember { mutableStateOf(puntoSeleccionado.latitude.toString()) }
    var longitudTexto by remember { mutableStateOf(puntoSeleccionado.longitude.toString()) }
    var distritoDetectado by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(puntoSeleccionado, 14f)
    }

    // Efecto para geocodificar distrito en segundo plano cuando cambia el punto
    LaunchedEffect(puntoSeleccionado) {
        withContext(Dispatchers.IO) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(contexto, Locale.getDefault())
                    val resultados = geocoder.getFromLocation(
                        puntoSeleccionado.latitude,
                        puntoSeleccionado.longitude,
                        1
                    )
                    if (!resultados.isNullOrEmpty()) {
                        val direccion = resultados[0]
                        val subLoc = direccion.subLocality
                        val loc = direccion.locality
                        val texto = listOfNotNull(subLoc, loc).filter { it.isNotBlank() }.distinct().joinToString(", ")
                        if (texto.isNotBlank()) {
                            distritoDetectado = texto
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback silencioso si no hay conexión de geocodificación
            }
        }
    }

    Dialog(
        onDismissRequest = alDescartar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = AdoptCardWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📍 Ubicación de la mascota",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AdoptTextDark
                        )
                        Text(
                            text = "Toca el mapa para fijar el punto exacto de referencia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdoptTextSecondary
                        )
                    }
                    TextButton(onClick = { modoManual = !modoManual }) {
                        Text(
                            text = if (modoManual) "Ver mapa" else "Manual",
                            color = AdoptTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (modoManual) {
                    // Entrada manual de coordenadas
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AdoptMeBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Ingreso manual de coordenadas (GPS opcional)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AdoptTextDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = latitudTexto,
                                onValueChange = {
                                    latitudTexto = it
                                    val parsed = it.toDoubleOrNull()
                                    if (parsed != null && parsed in -90.0..90.0) {
                                        puntoSeleccionado = LatLng(parsed, puntoSeleccionado.longitude)
                                        tieneUbicacionMarcada = true
                                    }
                                },
                                label = { Text("Latitud (-90 a 90)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = longitudTexto,
                                onValueChange = {
                                    longitudTexto = it
                                    val parsed = it.toDoubleOrNull()
                                    if (parsed != null && parsed in -180.0..180.0) {
                                        puntoSeleccionado = LatLng(puntoSeleccionado.latitude, parsed)
                                        tieneUbicacionMarcada = true
                                    }
                                },
                                label = { Text("Longitud (-180 a 180)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Contenedor del Mapa
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.matchParentSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = LocationHelper.tienePermisoUbicacion(contexto)),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = true,
                                myLocationButtonEnabled = false,
                                compassEnabled = true
                            ),
                            onMapClick = { latLng ->
                                puntoSeleccionado = latLng
                                tieneUbicacionMarcada = true
                                latitudTexto = String.format(Locale.US, "%.6f", latLng.latitude)
                                longitudTexto = String.format(Locale.US, "%.6f", latLng.longitude)
                            }
                        ) {
                            if (tieneUbicacionMarcada) {
                                Marker(
                                    state = MarkerState(position = puntoSeleccionado),
                                    title = "Ubicación seleccionada",
                                    snippet = distritoDetectado ?: "Punto de entrega/adopción"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fila con botón GPS y estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!LocationHelper.tienePermisoUbicacion(contexto)) {
                                mensajeGps = "Se requiere permiso de ubicación para usar el GPS."
                                return@OutlinedButton
                            }
                            if (!LocationHelper.esGpsHabilitado(contexto)) {
                                mensajeGps = "El GPS está apagado. Actívalo en los ajustes del sistema."
                                return@OutlinedButton
                            }
                            obteniendoGps = true
                            mensajeGps = null
                            coroutineScope.launch {
                                val ubicacion = LocationHelper.obtenerUbicacionActual(contexto)
                                obteniendoGps = false
                                if (ubicacion != null) {
                                    val latLng = LatLng(ubicacion.latitude, ubicacion.longitude)
                                    puntoSeleccionado = latLng
                                    tieneUbicacionMarcada = true
                                    latitudTexto = String.format(Locale.US, "%.6f", latLng.latitude)
                                    longitudTexto = String.format(Locale.US, "%.6f", latLng.longitude)
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                    mensajeGps = "Ubicación GPS obtenida con éxito."
                                } else {
                                    mensajeGps = "No se pudo obtener la señal GPS. Selecciona un punto en el mapa."
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (obteniendoGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AdoptTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detectando...", fontSize = 12.sp)
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AdoptTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mi ubicación", fontSize = 12.sp, color = AdoptTealDark)
                        }
                    }

                    // Información de coordenadas
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "Lat: %.4f", puntoSeleccionado.latitude),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = AdoptTextDark
                        )
                        Text(
                            text = String.format(Locale.US, "Lon: %.4f", puntoSeleccionado.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = AdoptTextDark
                        )
                    }
                }

                if (distritoDetectado != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Distrito sugerido: $distritoDetectado",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdoptTealDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (mensajeGps != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mensajeGps!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mensajeGps!!.contains("éxito")) AdoptTealDark else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = alDescartar) {
                        Text("Cancelar", color = AdoptTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            alConfirmar(
                                puntoSeleccionado.latitude,
                                puntoSeleccionado.longitude,
                                distritoDetectado
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirmar ubicación", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
