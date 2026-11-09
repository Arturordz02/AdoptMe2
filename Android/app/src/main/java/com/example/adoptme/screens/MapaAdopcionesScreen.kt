package com.example.adoptme.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.adoptme.components.DialogoExplicacionUbicacion
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adoptme.R
import com.example.adoptme.components.InsigniaEstado
import com.example.adoptme.location.LocationHelper
import com.example.adoptme.model.CentroAdopcion
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptCardWhite
import com.example.adoptme.ui.theme.AdoptOrange
import com.example.adoptme.ui.theme.AdoptTeal
import com.example.adoptme.ui.theme.AdoptTealDark
import com.example.adoptme.ui.theme.AdoptTextDark
import com.example.adoptme.ui.theme.AdoptTextSecondary
import com.example.adoptme.utils.HaversineUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

sealed class ElementoMapa {
    data class Mascota(val pet: Pet) : ElementoMapa()
    data class Centro(val centro: CentroAdopcion) : ElementoMapa()
}

// Punto por defecto: Lima Metropolitana (-12.046374, -77.030583)
private val CENTRO_LIMA = LatLng(-12.046374, -77.030583)

@Composable
fun MapaAdopcionesScreen(
    mascotas: List<Pet>,
    centros: List<CentroAdopcion> = CentrosDemostrativos,
    volver: () -> Unit = {},
    onVerDetalleMascota: (Pet) -> Unit = {},
    onContactarCentro: (idCentro: Int, nombreCentro: String, correoCentro: String, ubicacion: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var filtroTipo by remember { mutableStateOf("todos") } // "todos", "mascotas", "centros"
    var elementoSeleccionado by remember { mutableStateOf<ElementoMapa?>(null) }
    var ubicacionUsuario by remember { mutableStateOf<LatLng?>(null) }
    var centrandoGps by remember { mutableStateOf(false) }
    var mostrarExplicacionPermiso by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CENTRO_LIMA, 12f)
    }

    val lanzadorPermisosUbicacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            centrandoGps = true
            coroutineScope.launch {
                val loc = LocationHelper.obtenerUbicacionActual(contexto)
                centrandoGps = false
                if (loc != null) {
                    val userLatLng = LatLng(loc.latitude, loc.longitude)
                    ubicacionUsuario = userLatLng
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }
        }
    }

    // Filtrar mascotas con coordenadas válidas
    val mascotasConUbicacion = remember(mascotas) {
        mascotas.filter { it.latitud != null && it.longitud != null }
    }

    // Centros con coordenadas válidas
    val centrosConUbicacion = remember(centros) {
        centros.filter { it.latitud != null && it.longitud != null }
    }

    // Intentar obtener la ubicación del usuario si ya tiene permisos concedidos
    LaunchedEffect(Unit) {
        if (LocationHelper.tienePermisoUbicacion(contexto)) {
            val loc = LocationHelper.obtenerUbicacionActual(contexto)
            if (loc != null) {
                val userLatLng = LatLng(loc.latitude, loc.longitude)
                ubicacionUsuario = userLatLng
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. GoogleMap interactivo
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = LocationHelper.tienePermisoUbicacion(contexto)
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            ),
            onMapClick = {
                // Al tocar el mapa se deselecciona cualquier tarjeta abierta
                elementoSeleccionado = null
            }
        ) {
            // Marcadores de Mascotas
            if (filtroTipo == "todos" || filtroTipo == "mascotas") {
                mascotasConUbicacion.forEach { pet ->
                    val hue = when (pet.species.trim().lowercase()) {
                        "perro" -> BitmapDescriptorFactory.HUE_AZURE
                        "gato" -> BitmapDescriptorFactory.HUE_CYAN
                        "conejo" -> BitmapDescriptorFactory.HUE_GREEN
                        else -> BitmapDescriptorFactory.HUE_VIOLET
                    }

                    Marker(
                        state = MarkerState(position = LatLng(pet.latitud!!, pet.longitud!!)),
                        title = pet.name,
                        snippet = "${pet.species} · ${pet.status}",
                        icon = BitmapDescriptorFactory.defaultMarker(hue),
                        onClick = {
                            elementoSeleccionado = ElementoMapa.Mascota(pet)
                            true
                        }
                    )
                }
            }

            // Marcadores de Centros
            if (filtroTipo == "todos" || filtroTipo == "centros") {
                centrosConUbicacion.forEach { centro ->
                    Marker(
                        state = MarkerState(position = LatLng(centro.latitud!!, centro.longitud!!)),
                        title = centro.nombre,
                        snippet = "🏡 Centro de adopción",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        onClick = {
                            elementoSeleccionado = ElementoMapa.Centro(centro)
                            true
                        }
                    )
                }
            }
        }

        // 2. Barra Superior Flotante
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = AdoptCardWhite.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = volver,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Volver",
                            tint = AdoptTealDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Image(
                        painter = painterResource(id = R.drawable.adoptme_logo_original),
                        contentDescription = "Logo AdoptMe",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🗺️ Mapa de Adopciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AdoptTextDark
                        )
                        Text(
                            text = "${mascotasConUbicacion.size} mascotas y ${centrosConUbicacion.size} centros geolocalizados",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdoptTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filtros rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filtroTipo == "todos",
                        onClick = { filtroTipo = "todos" },
                        label = { Text("Todos", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdoptTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filtroTipo == "mascotas",
                        onClick = { filtroTipo = "mascotas" },
                        label = { Text("🐾 Mascotas (${mascotasConUbicacion.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdoptOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filtroTipo == "centros",
                        onClick = { filtroTipo = "centros" },
                        label = { Text("🏡 Centros (${centrosConUbicacion.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 3. Botón flotante para centrar en mi ubicación
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 110.dp, end = 16.dp)
        ) {
            Surface(
                onClick = {
                    if (LocationHelper.tienePermisoUbicacion(contexto)) {
                        centrandoGps = true
                        coroutineScope.launch {
                            val loc = LocationHelper.obtenerUbicacionActual(contexto)
                            centrandoGps = false
                            if (loc != null) {
                                val userLatLng = LatLng(loc.latitude, loc.longitude)
                                ubicacionUsuario = userLatLng
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                            }
                        }
                    } else {
                        mostrarExplicacionPermiso = true
                    }
                },
                shape = CircleShape,
                color = AdoptCardWhite,
                border = BorderStroke(1.dp, AdoptTeal),
                tonalElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (centrandoGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AdoptTeal
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_location),
                            contentDescription = "Mi ubicación",
                            tint = AdoptTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 4. Tarjeta Flotante Inferior de Detalle (al pulsar marcador)
        if (elementoSeleccionado != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .heightIn(max = 280.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AdoptCardWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    when (val elem = elementoSeleccionado!!) {
                        is ElementoMapa.Mascota -> {
                            val pet = elem.pet
                            val distanciaKm = if (ubicacionUsuario != null && pet.latitud != null && pet.longitud != null) {
                                HaversineUtil.calcularDistanciaKm(
                                    ubicacionUsuario!!.latitude,
                                    ubicacionUsuario!!.longitude,
                                    pet.latitud,
                                    pet.longitud
                                )
                            } else null

                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Imagen pequeña
                                    if (pet.photoBitmap != null) {
                                        Image(
                                            bitmap = pet.photoBitmap.asImageBitmap(),
                                            contentDescription = pet.name,
                                            modifier = Modifier
                                                .size(70.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = pet.image),
                                            contentDescription = pet.name,
                                            modifier = Modifier
                                                .size(70.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = pet.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AdoptTextDark
                                            )
                                            InsigniaEstado(estado = pet.status)
                                        }

                                        Text(
                                            text = "${pet.species} · ${pet.breed} · ${pet.age}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdoptTextSecondary
                                        )

                                        Text(
                                            text = "📍 ${pet.location}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdoptTealDark,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (distanciaKm != null) {
                                            Text(
                                                text = "📏 Aprox. ${HaversineUtil.formatearDistancia(distanciaKm)} de ti",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AdoptOrange,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { elementoSeleccionado = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_close),
                                            contentDescription = "Cerrar",
                                            tint = AdoptTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { onVerDetalleMascota(pet) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AdoptOrange),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Ver ficha de adopción", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        is ElementoMapa.Centro -> {
                            val centro = elem.centro
                            val distanciaKm = if (ubicacionUsuario != null && centro.latitud != null && centro.longitud != null) {
                                HaversineUtil.calcularDistanciaKm(
                                    ubicacionUsuario!!.latitude,
                                    ubicacionUsuario!!.longitude,
                                    centro.latitud,
                                    centro.longitud
                                )
                            } else null

                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_center),
                                            contentDescription = null,
                                            tint = AdoptTealDark,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = centro.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AdoptTextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "📍 ${centro.ubicacion}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdoptTealDark
                                        )
                                        Text(
                                            text = "🕒 ${centro.horarioAtencion}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AdoptTextSecondary
                                        )
                                        if (distanciaKm != null) {
                                            Text(
                                                text = "📏 Aprox. ${HaversineUtil.formatearDistancia(distanciaKm)} de ti",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AdoptOrange,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { elementoSeleccionado = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_close),
                                            contentDescription = "Cerrar",
                                            tint = AdoptTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        onContactarCentro(
                                            centro.id,
                                            centro.nombre,
                                            centro.contactoEjemplo,
                                            centro.ubicacion
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AdoptTeal),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Contactar centro", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mostrarExplicacionPermiso) {
            DialogoExplicacionUbicacion(
                alAceptar = {
                    mostrarExplicacionPermiso = false
                    lanzadorPermisosUbicacion.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                alRechazar = {
                    mostrarExplicacionPermiso = false
                }
            )
        }
    }
}
