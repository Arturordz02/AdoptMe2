package com.example.adoptme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.database.AdoptMeDatabase
import com.example.adoptme.datastore.CatalogoDataStore
import com.example.adoptme.entity.MascotaVisitadaEntity
import com.example.adoptme.mapper.toDomain
import com.example.adoptme.mapper.toDto
import com.example.adoptme.mapper.toPet
import com.example.adoptme.model.ActualizarEstadoSolicitudRequestDto
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.MensajeChat
import com.example.adoptme.model.NotificationItem
import com.example.adoptme.model.Pet
import com.example.adoptme.rules.localizarMascotaParaAprobacion
import com.example.adoptme.rules.puedeAprobarSolicitud
import com.example.adoptme.preferences.Preferencias
import com.example.adoptme.repository.AppStorage
import com.example.adoptme.repository.MascotaStorage
import com.example.adoptme.repository.normalizarCorreoUsuario
import com.example.adoptme.repository.sincronizarPublicacionesUsuario
import com.example.adoptme.screens.PantallaAdministrador
import com.example.adoptme.screens.PantallaCentro
import com.example.adoptme.screens.PantallaChat
import com.example.adoptme.screens.PantallaEstadoCentro
import com.example.adoptme.screens.PantallaInicioSesion
import com.example.adoptme.screens.PantallaPrincipalAdoptante
import com.example.adoptme.screens.PantallaPublicarMascota
import com.example.adoptme.screens.PantallaRegistro
import com.example.adoptme.viewmodel.MiCentroViewModel
import com.example.adoptme.viewmodel.AdminCentrosViewModel
import com.example.adoptme.model.UsuarioRegistrado
import com.example.adoptme.ui.theme.AdoptMeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicialización de persistencia local
        val almacenamientoMascotas = MascotaStorage(applicationContext)
        val almacenamientoApp = AppStorage(applicationContext)
        val preferencias = Preferencias(applicationContext)
        val catalogoDataStore = CatalogoDataStore.obtenerInstancia(applicationContext)

        // Conectar Retrofit con SharedPreferences para usar el Token JWT
        RetrofitClient.preferencias = preferencias
        // Configuración determinista: en debug conecta a URL local si está habilitado
        if (com.example.adoptme.BuildConfig.DEBUG && com.example.adoptme.BuildConfig.PERMITIR_URL_DEV && RetrofitClient.URL_LOCAL_DEV.isNotBlank()) {
            RetrofitClient.configurarBaseUrl(RetrofitClient.URL_LOCAL_DEV)
        } else {
            RetrofitClient.restaurarBaseUrlProduccion()
        }

        // Migración idempotente de claves globales antiguas
        almacenamientoApp.migrarDatosAntiguosSiExisten()
        preferencias.migrarPreferenciasAntiguasSiExisten()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            // 2. Estados globales de sesión (con persistencia segura)
            var tokenSesion by remember { mutableStateOf<String?>(preferencias.obtenerTokenSesionActiva()) }
            var correoSesionActiva by remember { mutableStateOf(preferencias.obtenerCorreoSesionActiva() ?: "") }
            var rolUsuario by remember { mutableStateOf<String?>(preferencias.obtenerRolSesionActiva()) }
            var idCentroActivo by remember { mutableStateOf<Int?>(preferencias.obtenerCentroIdSesionActiva()) }
            var estadoCentroActivo by remember { mutableStateOf<String?>(preferencias.obtenerEstadoCentroSesionActiva()) }
            var centroActivoFlag by remember { mutableStateOf(preferencias.esCentroActivoSesionActiva()) }
            var centroDemostrativoFlag by remember { mutableStateOf(preferencias.esCentroDemostrativoSesionActiva()) }
            var mostrarPublicarMascotaCentro by remember { mutableStateOf(false) }

            val usuariosRegistrados = remember { mutableStateListOf<UsuarioRegistrado>() }
            var usuarioActualSesion by remember { mutableStateOf<UsuarioRegistrado?>(null) }
            var mostrarRegistro by remember { mutableStateOf(false) }
            var mensajeExitoRegistro by remember { mutableStateOf<String?>(null) }
            var correoRegistrado by remember { mutableStateOf("") }

            // 3. Preferencias de apariencia y tema
            var temaSeleccionado by remember { mutableStateOf(Preferencias.TEMA_SISTEMA) }
            var colorSeleccionado by remember { mutableStateOf(Preferencias.COLOR_VERDE) }
            var tamanoTextoSeleccionado by remember { mutableStateOf(Preferencias.TEXTO_NORMAL) }

            // 4. Preferencias de filtros y notificaciones (aisladas por usuario)
            var mascotaPreferida by remember { mutableStateOf(Preferencias.MASCOTA_TODAS) }
            var ubicacionPreferida by remember { mutableStateOf(Preferencias.UBICACION_TODAS) }
            var ordenPredeterminado by remember { mutableStateOf(Preferencias.ORDEN_RECIENTES) }
            var soloDisponibles by remember { mutableStateOf(false) }
            var notificarNuevasMascotas by remember { mutableStateOf(true) }
            var notificarAdopciones by remember { mutableStateOf(true) }
            var notificarMensajes by remember { mutableStateOf(true) }


            AdoptMeTheme(
                tema = temaSeleccionado,
                colorPrincipal = colorSeleccionado,
                tamanoTexto = tamanoTextoSeleccionado
            ) {
                Surface {
                    // 5. Listas reactivas principales (fuente única de verdad)
                    val mascotasPublicadas = remember {
                        mutableStateListOf<Pet>().apply {
                            addAll(almacenamientoMascotas.cargarMascotas().filter { it.esRemota && it.creadoPorCorreo.isNotBlank() })
                        }
                    }

                    // Catálogo de producción: únicamente mascotas remotas reales de Neon
                    val mascotas = remember { mutableStateListOf<Pet>() }
                    val mascotasFavoritas = remember { mutableStateListOf<Pet>() }
                    val mascotasVisitadas = remember { mutableStateListOf<MascotaVisitadaEntity>() }
                    val notificaciones = remember { mutableStateListOf<NotificationItem>() }
                    val solicitudesAdopcion = remember { mutableStateListOf<AdoptionRequest>() }

                    // Estados de carga y error del catálogo remoto
                    var cargandoCatalogo by remember { mutableStateOf(true) }
                    var errorCatalogo by remember { mutableStateOf<String?>(null) }
                    var recargarCatalogoTrigger by remember { mutableStateOf(0) }
                    var recargarSolicitudesTrigger by remember { mutableStateOf(0) }
                    val procesandoSolicitudIds = remember { mutableStateListOf<Int>() }

                    LaunchedEffect(recargarCatalogoTrigger) {
                        try {
                            cargandoCatalogo = true
                            errorCatalogo = null
                            val mascotasAzure = RetrofitClient.mascotasApi.obtenerMascotas()
                            val mascotasRemotas = mascotasAzure.map { it.toPet() }

                            Log.d(
                                "ADOPTME_RETROFIT",
                                "Consulta exitosa. Mascotas recibidas: ${mascotasAzure.size}"
                            )

                            mascotas.clear()
                            mascotas.addAll(mascotasRemotas)
                            cargandoCatalogo = false

                            if (correoSesionActiva.isNotBlank()) {
                                val sincronizadas = sincronizarPublicacionesUsuario(
                                    correoUsuario = correoSesionActiva,
                                    mascotasRemotas = mascotasRemotas,
                                    publicacionesLocales = mascotasPublicadas
                                )
                                mascotasPublicadas.clear()
                                mascotasPublicadas.addAll(sincronizadas)
                                almacenamientoMascotas.guardarMascotas(mascotasPublicadas)

                                val clavesFavoritas = almacenamientoApp.cargarFavoritosClaves(correoSesionActiva)
                                mascotasFavoritas.clear()
                                mascotasFavoritas.addAll(
                                    mascotas.filter { pet ->
                                        pet.esRemota && clavesFavoritas.contains(pet.claveCompose)
                                    }
                                )
                            }

                        } catch (e: Exception) {
                            cargandoCatalogo = false
                            errorCatalogo = e.localizedMessage ?: "Error al conectar con el servidor"
                            Log.e(
                                "ADOPTME_RETROFIT",
                                "Error consultando Neon",
                                e
                            )
                        }
                    }

                    // Sincronización real de solicitudes de adopción con el backend ASP.NET Core y Neon
                    LaunchedEffect(correoSesionActiva, rolUsuario, recargarSolicitudesTrigger) {
                        if (correoSesionActiva.isNotBlank()) {
                            if (rolUsuario == "Admin") {
                                try {
                                    val respuesta = RetrofitClient.solicitudesApi.obtenerSolicitudesAdmin()
                                    if (respuesta.isSuccessful && respuesta.body() != null) {
                                        val remotas = respuesta.body()!!.map { it.toDomain() }
                                        solicitudesAdopcion.clear()
                                        solicitudesAdopcion.addAll(remotas.distinctBy { it.id })
                                        Log.d(
                                            "ADOPTME_SOLICITUDES",
                                            "Consulta exitosa como Admin. Solicitudes reales recibidas: ${remotas.size}"
                                        )
                                    } else {
                                        val errorMsg = when (respuesta.code()) {
                                            401 -> "Sesión expirada o no autorizada."
                                            403 -> "Acceso denegado: se requieren permisos de Administrador."
                                            else -> "Error en el servidor al obtener solicitudes (${respuesta.code()})."
                                        }
                                        Log.w("ADOPTME_SOLICITUDES", errorMsg)
                                        Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("ADOPTME_SOLICITUDES", "Error de red consultando solicitudes admin", e)
                                    Toast.makeText(applicationContext, "Error de conexión al cargar solicitudes del servidor.", Toast.LENGTH_LONG).show()
                                }
                            } else if (rolUsuario == "Adoptante") {
                                try {
                                    val respuesta = RetrofitClient.solicitudesApi.obtenerMisSolicitudes()
                                    if (respuesta.isSuccessful && respuesta.body() != null) {
                                        val remotas = respuesta.body()!!.map { it.toDomain() }
                                        solicitudesAdopcion.clear()
                                        solicitudesAdopcion.addAll(remotas.distinctBy { it.id })
                                        Log.d(
                                            "ADOPTME_SOLICITUDES",
                                            "Mis solicitudes reales recibidas: ${remotas.size}"
                                        )
                                    } else {
                                        Log.w("ADOPTME_SOLICITUDES", "Código al consultar mis solicitudes: ${respuesta.code()}")
                                    }
                                } catch (e: Exception) {
                                    Log.e("ADOPTME_SOLICITUDES", "Error al consultar mis solicitudes", e)
                                }
                            }
                        }
                    }

                    // Sincronizar publicaciones y favoritos del usuario autenticado en cuanto inicia sesión
                    LaunchedEffect(correoSesionActiva) {
                        if (correoSesionActiva.isNotBlank()) {
                            // 1. Migración idempotente de catálogo desde SharedPreferences hacia Preferences DataStore
                            try {
                                catalogoDataStore.migrarDesdeSharedPreferencesSiEsNecesario(
                                    correo = correoSesionActiva,
                                    sharedPreferences = preferencias.obtenerSharedPreferencesInterno()
                                )
                            } catch (e: Exception) {
                                Log.e("DATASTORE_CATALOGO", "Error durante migración de catálogo a DataStore", e)
                            }

                            // 2. Colectar Preferences DataStore reactivamente sin bloquear el hilo principal
                            launch {
                                catalogoDataStore.obtenerPreferencias(correoSesionActiva).collect { prefs ->
                                    mascotaPreferida = prefs.mascotaPreferida
                                    ubicacionPreferida = prefs.ubicacionPreferida
                                    ordenPredeterminado = prefs.ordenPredeterminado
                                    soloDisponibles = prefs.soloDisponibles
                                    Log.d("DATASTORE_CATALOGO", "Preferencias DataStore actualizadas: mascota=${prefs.mascotaPreferida}, ubicacion=${prefs.ubicacionPreferida}, orden=${prefs.ordenPredeterminado}, disponibles=${prefs.soloDisponibles}")
                                }
                            }

                            // Cargar perfil desde cache si aún es nulo
                            if (usuarioActualSesion == null) {
                                usuarioActualSesion = almacenamientoApp.cargarPerfilCache(correoSesionActiva)
                            }

                            // Consultar perfil remoto desde Neon Web API con JWT
                            try {
                                val respPerfil = RetrofitClient.usuariosApi.obtenerPerfil()
                                if (respPerfil.isSuccessful) {
                                    val perfilRemoto = respPerfil.body()
                                    if (perfilRemoto != null) {
                                        val usuarioActualizado = UsuarioRegistrado(
                                            nombre = perfilRemoto.nombre,
                                            correo = normalizarCorreoUsuario(perfilRemoto.correo),
                                            contrasena = "",
                                            telefono = perfilRemoto.telefono,
                                            rol = perfilRemoto.rol
                                        )
                                        usuarioActualSesion = usuarioActualizado
                                        almacenamientoApp.guardarPerfilCache(
                                            correo = correoSesionActiva,
                                            nombre = perfilRemoto.nombre,
                                            telefono = perfilRemoto.telefono,
                                            rol = perfilRemoto.rol,
                                            id = perfilRemoto.id
                                        )
                                        Log.d("ADOPTME_PERFIL", "Perfil de Neon sincronizado: ${perfilRemoto.nombre} (${perfilRemoto.rol})")
                                    }
                                } else {
                                    Log.w("ADOPTME_PERFIL", "No se pudo obtener perfil de Neon. Código: ${respPerfil.code()}")
                                }
                            } catch (e: Exception) {
                                Log.e("ADOPTME_PERFIL", "Error consultando perfil en Neon", e)
                            }

                            try {
                                val mascotasAzure = RetrofitClient.mascotasApi.obtenerMascotas()
                                val mascotasRemotas = mascotasAzure.map { it.toPet() }

                                mascotas.clear()
                                mascotas.addAll(mascotasRemotas)

                                val sincronizadas = sincronizarPublicacionesUsuario(
                                    correoUsuario = correoSesionActiva,
                                    mascotasRemotas = mascotasRemotas,
                                    publicacionesLocales = mascotasPublicadas
                                )
                                mascotasPublicadas.clear()
                                mascotasPublicadas.addAll(sincronizadas)
                                almacenamientoMascotas.guardarMascotas(mascotasPublicadas)

                                val clavesFavoritas = almacenamientoApp.cargarFavoritosClaves(correoSesionActiva)
                                mascotasFavoritas.clear()
                                mascotasFavoritas.addAll(
                                    mascotas.filter { pet ->
                                        pet.esRemota && clavesFavoritas.contains(pet.claveCompose)
                                    }
                                )

                                Log.d("ADOPTME_SYNC", "Sincronización completada para publicaciones del usuario: ${sincronizadas.size}")
                            } catch (e: Exception) {
                                Log.e("ADOPTME_SYNC", "Error al sincronizar publicaciones con Neon", e)
                            }
                        }
                    }

                    // Observar historial de mascotas visitadas con Room (SQLite) aislado por usuario
                    LaunchedEffect(correoSesionActiva) {
                        if (correoSesionActiva.isNotBlank()) {
                            val correoNorm = normalizarCorreoUsuario(correoSesionActiva)
                            try {
                                AdoptMeDatabase.obtenerInstancia(applicationContext)
                                    .mascotaVisitadaDao()
                                    .observarMascotasVisitadas(correoNorm, 10)
                                    .collect { lista ->
                                        mascotasVisitadas.clear()
                                        mascotasVisitadas.addAll(lista)
                                    }
                            } catch (e: Exception) {
                                Log.e("ROOM_SQLITE", "Error observando mascotas visitadas en SQLite", e)
                            }
                        } else {
                            mascotasVisitadas.clear()
                        }
                    }

                    // Estados de navegación para detalle y adopción
                    var mascotaSeleccionada by remember { mutableStateOf<Pet?>(null) }
                    var mostrarPantallaAdopcion by remember { mutableStateOf(false) }

                    // Estados para el chat local entre Adoptante y Centro
                    var idCentroSeleccionado by remember { mutableStateOf(preferencias.obtenerCentroIdSesionActiva() ?: 0) }
                    var nombreCentroSeleccionado by remember { mutableStateOf("") }
                    var correoCentroSeleccionado by remember { mutableStateOf("") }
                    var correoAdoptanteSeleccionado by remember { mutableStateOf("") }
                    var chatAbiertoCentro by remember { mutableStateOf(false) }
                    val mensajesChatActuales = remember { mutableStateListOf<MensajeChat>() }

                    // 6. Comunicación entre Activities mediante Intent y ActivityResultLauncher
                    val launcherDetalle = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { resultado ->
                        if (resultado.resultCode == Activity.RESULT_OK) {
                            val petId = resultado.data?.getIntExtra("petId", resultado.data?.getIntExtra("id", 0) ?: 0) ?: 0
                            val mascotaEncontrada = if (petId > 0) {
                                mascotas.find { it.id == petId }
                            } else {
                                val nombre = resultado.data?.getStringExtra("nombre") ?: ""
                                mascotas.find { it.name.equals(nombre, ignoreCase = true) }
                            }
                            if (mascotaEncontrada != null) {
                                mascotaSeleccionada = mascotaEncontrada
                                mostrarPantallaAdopcion = true
                            }
                        }
                    }

                    val abrirDetalleActivity: (Pet) -> Unit = { pet ->
                        if (correoSesionActiva.isNotBlank() && pet.id > 0) {
                            coroutineScope.launch {
                                try {
                                    AdoptMeDatabase.obtenerInstancia(applicationContext)
                                        .mascotaVisitadaDao()
                                        .registrarVisita(
                                            MascotaVisitadaEntity(
                                                correoUsuario = normalizarCorreoUsuario(correoSesionActiva),
                                                mascotaId = pet.id,
                                                nombre = pet.name,
                                                especie = pet.species,
                                                raza = pet.breed,
                                                edad = pet.age,
                                                genero = pet.gender,
                                                ubicacion = pet.location,
                                                estado = pet.status,
                                                descripcion = pet.description,
                                                imagenResId = pet.image,
                                                creadoPorCorreo = pet.creadoPorCorreo,
                                                fechaUltimaVisita = System.currentTimeMillis()
                                            )
                                        )
                                } catch (e: Exception) {
                                    Log.e("ROOM_SQLITE", "Error registrando visita en SQLite desde MainActivity", e)
                                }
                            }
                        }
                        val intent = Intent(this@MainActivity, DetalleMascotaActivity::class.java).apply {
                            putExtra("petId", pet.id)
                            putExtra("id", pet.id)
                            putExtra("correoUsuarioActual", correoSesionActiva)
                            putExtra("name", pet.name)
                            putExtra("nombre", pet.name)
                            putExtra("species", pet.species)
                            putExtra("breed", pet.breed)
                            putExtra("age", pet.age)
                            putExtra("gender", pet.gender)
                            putExtra("location", pet.location)
                            putExtra("status", pet.status)
                            putExtra("description", pet.description)
                            putExtra("image", pet.image)
                            putExtra("creadoPorCorreo", pet.creadoPorCorreo)
                            putExtra("esRemota", pet.esRemota)
                        }
                        launcherDetalle.launch(intent)
                    }

                    // 7. Handlers de administración sincronizados con ASP.NET Core y Neon
                    val manejarAprobar: (AdoptionRequest) -> Pair<Boolean, String> = { solicitud ->
                        if (solicitud.id > 0) {
                            if (procesandoSolicitudIds.contains(solicitud.id)) {
                                Pair(false, "Operación en curso, por favor espere.")
                            } else {
                                procesandoSolicitudIds.add(solicitud.id)
                                coroutineScope.launch {
                                    try {
                                        val reqDto = ActualizarEstadoSolicitudRequestDto(
                                            status = "Aprobada",
                                            observacion = "Solicitud aprobada por el administrador."
                                        )
                                        val respuesta = RetrofitClient.solicitudesApi.actualizarEstadoSolicitud(solicitud.id, reqDto)
                                        if (respuesta.isSuccessful && respuesta.body() != null) {
                                            val solicitudActualizada = respuesta.body()!!.toDomain()
                                            val indice = solicitudesAdopcion.indexOfFirst { it.id == solicitud.id }
                                            if (indice != -1) {
                                                solicitudesAdopcion[indice] = solicitudActualizada
                                            }
                                            // Actualizar también en caché local por congruencia
                                            almacenamientoApp.actualizarEstadoSolicitud(solicitudActualizada)

                                            // Refrescar lista administrativa completa desde Neon
                                            recargarSolicitudesTrigger++
                                            // Refrescar catálogo de mascotas (pasa a "En proceso")
                                            recargarCatalogoTrigger++

                                            Toast.makeText(applicationContext, "Solicitud aprobada en el servidor. Mascota en proceso.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val errorMsg = when (respuesta.code()) {
                                                400 -> "Solicitud no válida o la mascota no está disponible."
                                                401 -> "Sesión expirada o no autorizada."
                                                403 -> "Acceso denegado: se requieren permisos de Administrador."
                                                404 -> "La solicitud o mascota no fue encontrada en el servidor."
                                                409 -> "Conflicto: La mascota ya se encuentra en otro proceso de adopción."
                                                else -> "Error en el servidor al aprobar solicitud (${respuesta.code()})."
                                            }
                                            Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(applicationContext, "Error de red al conectar con el servidor.", Toast.LENGTH_LONG).show()
                                    } finally {
                                        procesandoSolicitudIds.remove(solicitud.id)
                                    }
                                }
                                Pair(true, "Enviando aprobación al servidor...")
                            }
                        } else {
                            val mascotaAsociada = localizarMascotaParaAprobacion(
                                mascotas = mascotas,
                                petId = solicitud.petId,
                                nombreMascota = solicitud.petName
                            )
                            val (puedeAprobar, mensajeValidacion) = puedeAprobarSolicitud(mascotaAsociada)

                            if (!puedeAprobar || mascotaAsociada == null) {
                                Pair(false, mensajeValidacion)
                            } else {
                                val indice = solicitudesAdopcion.indexOf(solicitud)
                                val solicitudAprobada = solicitud.copy(status = "Aprobada")
                                if (indice != -1) {
                                    solicitudesAdopcion[indice] = solicitudAprobada
                                }
                                almacenamientoApp.actualizarEstadoSolicitud(solicitudAprobada)

                                val mascotaActualizada = mascotaAsociada.copy(status = "En proceso")
                                val indiceMascota = mascotas.indexOfFirst { it.id == mascotaActualizada.id }
                                if (indiceMascota != -1) {
                                    mascotas[indiceMascota] = mascotaActualizada
                                }

                                val indicePub = mascotasPublicadas.indexOfFirst { it.id == mascotaActualizada.id }
                                if (indicePub != -1) {
                                    mascotasPublicadas[indicePub] = mascotaActualizada
                                    almacenamientoMascotas.guardarMascotas(mascotasPublicadas)
                                }

                                if (mascotaSeleccionada?.id == mascotaActualizada.id) {
                                    mascotaSeleccionada = mascotaActualizada
                                }

                                val indiceFavorito = mascotasFavoritas.indexOfFirst { it.id == mascotaActualizada.id }
                                if (indiceFavorito != -1) {
                                    mascotasFavoritas[indiceFavorito] = mascotaActualizada
                                }

                                if (solicitud.email.isNotBlank()) {
                                    almacenamientoApp.agregarNotificacion(
                                        solicitud.email,
                                        NotificationItem(
                                            title = "Solicitud aprobada 🎉",
                                            message = "Tu solicitud para adoptar a ${solicitud.petName} fue aprobada."
                                        )
                                    )
                                }

                                val notifAdmin = NotificationItem(
                                    title = "Solicitud aprobada",
                                    message = "Aprobaste la adopción de ${solicitud.petName} para ${solicitud.applicantName.ifBlank { solicitud.email }}."
                                )
                                notificaciones.add(0, notifAdmin)
                                almacenamientoApp.guardarNotificaciones("admin@adoptme.com", notificaciones)

                                Pair(true, "Solicitud aprobada correctamente.")
                            }
                        }
                    }

                    val manejarPonerEnRevision: (AdoptionRequest) -> Unit = { solicitud ->
                        if (solicitud.id > 0) {
                            if (procesandoSolicitudIds.contains(solicitud.id)) {
                                Toast.makeText(applicationContext, "Operación en curso, por favor espere.", Toast.LENGTH_SHORT).show()
                            } else {
                                procesandoSolicitudIds.add(solicitud.id)
                                coroutineScope.launch {
                                    try {
                                        val reqDto = ActualizarEstadoSolicitudRequestDto(
                                            status = "En revisión",
                                            observacion = "Solicitud en evaluación por el administrador."
                                        )
                                        val respuesta = RetrofitClient.solicitudesApi.actualizarEstadoSolicitud(solicitud.id, reqDto)
                                        if (respuesta.isSuccessful && respuesta.body() != null) {
                                            val solicitudActualizada = respuesta.body()!!.toDomain()
                                            val indice = solicitudesAdopcion.indexOfFirst { it.id == solicitud.id }
                                            if (indice != -1) {
                                                solicitudesAdopcion[indice] = solicitudActualizada
                                            }
                                            almacenamientoApp.actualizarEstadoSolicitud(solicitudActualizada)
                                            recargarSolicitudesTrigger++
                                            Toast.makeText(applicationContext, "Solicitud puesta en revisión en el servidor.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val errorMsg = when (respuesta.code()) {
                                                400 -> "Solicitud no válida."
                                                401 -> "Sesión expirada o no autorizada."
                                                403 -> "Acceso denegado: se requieren permisos de Administrador."
                                                404 -> "La solicitud no fue encontrada en el servidor."
                                                409 -> "Conflicto con el estado actual en el servidor."
                                                else -> "Error en el servidor al poner en revisión (${respuesta.code()})."
                                            }
                                            Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(applicationContext, "Error de red al conectar con el servidor.", Toast.LENGTH_LONG).show()
                                    } finally {
                                        procesandoSolicitudIds.remove(solicitud.id)
                                    }
                                }
                            }
                        } else {
                            val indice = solicitudesAdopcion.indexOf(solicitud)
                            val solicitudEnRevision = solicitud.copy(status = "En revisión")
                            if (indice != -1) {
                                solicitudesAdopcion[indice] = solicitudEnRevision
                            }
                            almacenamientoApp.actualizarEstadoSolicitud(solicitudEnRevision)

                            if (solicitud.email.isNotBlank()) {
                                almacenamientoApp.agregarNotificacion(
                                    solicitud.email,
                                    NotificationItem(
                                        title = "Solicitud en revisión 🔍",
                                        message = "Tu solicitud para adoptar a ${solicitud.petName} está siendo evaluada."
                                    )
                                )
                            }

                            val notifAdmin = NotificationItem(
                                title = "Solicitud en revisión",
                                message = "Pusiste en revisión la solicitud para ${solicitud.petName}."
                            )
                            notificaciones.add(0, notifAdmin)
                            almacenamientoApp.guardarNotificaciones("admin@adoptme.com", notificaciones)
                        }
                    }

                    val manejarCambiarEstadoMascota: (Pet, String) -> Unit = { mascota, nuevoEstado ->
                        fun aplicarCambioEstadoLocal() {
                            val indice = mascotas.indexOfFirst { it.id == mascota.id && it.esRemota == mascota.esRemota }
                            if (indice != -1) {
                                val mascotaActualizada = mascotas[indice].copy(status = nuevoEstado)
                                mascotas[indice] = mascotaActualizada

                                val indicePub = mascotasPublicadas.indexOfFirst { it.id == mascota.id && it.esRemota == mascota.esRemota }
                                if (indicePub != -1) {
                                    mascotasPublicadas[indicePub] = mascotaActualizada
                                    almacenamientoMascotas.guardarMascotas(mascotasPublicadas)
                                }

                                val indiceFav = mascotasFavoritas.indexOfFirst { it.id == mascota.id && it.esRemota == mascota.esRemota }
                                if (indiceFav != -1) {
                                    mascotasFavoritas[indiceFav] = mascotaActualizada
                                }

                                if (mascotaSeleccionada?.id == mascota.id && mascotaSeleccionada?.esRemota == mascota.esRemota) {
                                    mascotaSeleccionada = mascotaActualizada
                                }
                            }
                        }

                        if (mascota.esRemota && mascota.id > 0) {
                            coroutineScope.launch {
                                try {
                                    val dto = mascota.toDto().copy(status = nuevoEstado)
                                    val respuesta = RetrofitClient.mascotasApi.actualizarMascota(mascota.id, dto)
                                    if (respuesta.isSuccessful) {
                                        aplicarCambioEstadoLocal()
                                        Toast.makeText(applicationContext, "Estado actualizado en el servidor.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val errorMsg = when (respuesta.code()) {
                                            401 -> "Sesión expirada o no autorizada."
                                            403 -> "No tienes autorización para modificar esta mascota."
                                            404 -> "La mascota no fue encontrada en el servidor."
                                            else -> "Error en el servidor al actualizar estado: ${respuesta.code()}"
                                        }
                                        Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(applicationContext, "Error de conexión al actualizar estado.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            aplicarCambioEstadoLocal()
                        }
                    }

                    val manejarEliminarMascotaAdmin: (Pet) -> Unit = { mascota ->
                        fun aplicarEliminarMascotaLocal() {
                            mascotasPublicadas.removeAll { it.id == mascota.id && it.esRemota == mascota.esRemota }
                            mascotas.removeAll { it.id == mascota.id && it.esRemota == mascota.esRemota }
                            mascotasFavoritas.removeAll { it.id == mascota.id && it.esRemota == mascota.esRemota }
                            if (mascotaSeleccionada?.id == mascota.id && mascotaSeleccionada?.esRemota == mascota.esRemota) {
                                mascotaSeleccionada = null
                            }
                            almacenamientoMascotas.eliminarMascota(
                                mascota = mascota,
                                mascotasRestantes = mascotasPublicadas
                            )
                            almacenamientoApp.eliminarFavorito("admin@adoptme.com", mascota.id)

                            val notifAdmin = NotificationItem(
                                title = "Publicación eliminada",
                                message = "La publicación de ${mascota.name} fue eliminada correctamente."
                            )
                            notificaciones.add(0, notifAdmin)
                            almacenamientoApp.guardarNotificaciones("admin@adoptme.com", notificaciones)
                        }

                        if (mascota.esRemota && mascota.id > 0) {
                            coroutineScope.launch {
                                try {
                                    val respuesta = RetrofitClient.mascotasApi.eliminarMascota(mascota.id)
                                    if (respuesta.isSuccessful) {
                                        aplicarEliminarMascotaLocal()
                                        Toast.makeText(applicationContext, "Publicación eliminada del servidor.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val errorMsg = when (respuesta.code()) {
                                            401 -> "Sesión expirada o no autorizada."
                                            403 -> "No tienes autorización para eliminar esta publicación."
                                            404 -> "La mascota no fue encontrada en el servidor."
                                            else -> "No se pudo eliminar en el servidor (Código: ${respuesta.code()})."
                                        }
                                        Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(applicationContext, "Error de conexión al eliminar publicación.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            aplicarEliminarMascotaLocal()
                        }
                    }

                    val manejarRechazar: (AdoptionRequest) -> Unit = { solicitud ->
                        val indice = solicitudesAdopcion.indexOf(solicitud)
                        val solicitudRechazada = solicitud.copy(status = "Rechazada")
                        if (indice != -1) {
                            solicitudesAdopcion[indice] = solicitudRechazada
                        }
                        almacenamientoApp.actualizarEstadoSolicitud(solicitudRechazada)

                        if (solicitud.email.isNotBlank()) {
                            almacenamientoApp.agregarNotificacion(
                                solicitud.email,
                                NotificationItem(
                                    title = "Solicitud actualizada",
                                    message = "Tu solicitud para adoptar a ${solicitud.petName} fue rechazada."
                                )
                            )
                        }

                        val notifAdmin = NotificationItem(
                            title = "Solicitud rechazada",
                            message = "Rechazaste la solicitud para adoptar a ${solicitud.petName}."
                        )
                        notificaciones.add(0, notifAdmin)
                        almacenamientoApp.guardarNotificaciones("admin@adoptme.com", notificaciones)
                    }

                    // 8. Handler de envío de mensajes de chat
                    val manejarEnviarMensaje: (String) -> Unit = { textoLimpio ->
                        val fechaActual = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                        val esCentro = rolUsuario == "Centro"
                        val correoEmisor = if (esCentro) correoCentroSeleccionado else correoSesionActiva
                        val correoReceptor = if (esCentro) correoAdoptanteSeleccionado else correoCentroSeleccionado
                        val correoAdoptante = if (esCentro) correoAdoptanteSeleccionado else correoSesionActiva

                        val nuevoMensaje = MensajeChat(
                            id = System.currentTimeMillis(),
                            idCentro = idCentroSeleccionado,
                            remitente = correoEmisor,
                            receptor = correoReceptor,
                            texto = textoLimpio,
                            nombreCentro = nombreCentroSeleccionado,
                            fecha = fechaActual,
                            esDelCentro = esCentro
                        )

                        mensajesChatActuales.add(nuevoMensaje)
                        almacenamientoApp.guardarMensajesChat(
                            correoAdoptante = correoAdoptante,
                            idCentro = idCentroSeleccionado,
                            mensajes = mensajesChatActuales
                        )
                        almacenamientoApp.registrarConversacionCentro(
                            correoCentro = correoCentroSeleccionado,
                            idCentro = idCentroSeleccionado,
                            correoAdoptante = correoAdoptante,
                            nombreCentro = nombreCentroSeleccionado,
                            ultimoTexto = textoLimpio,
                            fecha = fechaActual
                        )

                        // Respuesta simulada automática del centro: solo si emisor es adoptante y no existe aún respuesta del centro
                        if (!esCentro && mensajesChatActuales.none { it.esDelCentro }) {
                            val respuestaCentro = MensajeChat(
                                id = System.currentTimeMillis() + 1,
                                idCentro = idCentroSeleccionado,
                                remitente = correoCentroSeleccionado,
                                receptor = correoAdoptante,
                                texto = "Hola, gracias por escribirnos. Revisaremos tu consulta y te responderemos pronto.",
                                nombreCentro = nombreCentroSeleccionado,
                                fecha = fechaActual,
                                esDelCentro = true
                            )
                            mensajesChatActuales.add(respuestaCentro)
                            almacenamientoApp.guardarMensajesChat(
                                correoAdoptante = correoAdoptante,
                                idCentro = idCentroSeleccionado,
                                mensajes = mensajesChatActuales
                            )
                            almacenamientoApp.registrarConversacionCentro(
                                correoCentro = correoCentroSeleccionado,
                                idCentro = idCentroSeleccionado,
                                correoAdoptante = correoAdoptante,
                                nombreCentro = nombreCentroSeleccionado,
                                ultimoTexto = respuestaCentro.texto,
                                fecha = fechaActual
                            )
                        }
                    }

                    // 9. Cierre de sesión seguro: limpia listas en memoria sin borrar persistencia
                    val manejarCerrarSesion: () -> Unit = {
                        preferencias.limpiarSesionActiva()

                        mascotasFavoritas.clear()
                        mascotasVisitadas.clear()
                        notificaciones.clear()
                        solicitudesAdopcion.clear()
                        mensajesChatActuales.clear()
                        tokenSesion = null
                        correoSesionActiva = ""
                        rolUsuario = null
                        idCentroActivo = null
                        estadoCentroActivo = null
                        centroActivoFlag = false
                        centroDemostrativoFlag = false
                        mostrarPublicarMascotaCentro = false
                        usuarioActualSesion = null
                        mascotaSeleccionada = null
                        mostrarPantallaAdopcion = false
                        idCentroSeleccionado = 0
                        nombreCentroSeleccionado = ""
                        correoCentroSeleccionado = ""
                        correoAdoptanteSeleccionado = ""
                        chatAbiertoCentro = false

                        temaSeleccionado = Preferencias.TEMA_SISTEMA
                        colorSeleccionado = Preferencias.COLOR_VERDE
                        tamanoTextoSeleccionado = Preferencias.TEXTO_NORMAL
                        mascotaPreferida = Preferencias.MASCOTA_TODAS
                        ubicacionPreferida = Preferencias.UBICACION_TODAS
                        ordenPredeterminado = Preferencias.ORDEN_RECIENTES
                        soloDisponibles = false
                        notificarNuevasMascotas = true
                        notificarAdopciones = true
                        notificarMensajes = true
                    }

                    // 10. Navegación principal por roles
                    when (rolUsuario) {
                        null -> {
                            if (mostrarRegistro) {
                                PantallaRegistro(
                                    registrarUsuario = { nuevoUsuario ->
                                        usuariosRegistrados.add(nuevoUsuario)
                                        mensajeExitoRegistro = "¡Cuenta creada con éxito! Ya puedes iniciar sesión."
                                        correoRegistrado = nuevoUsuario.correo
                                        mostrarRegistro = false
                                    },
                                    volverAlLogin = {
                                        mostrarRegistro = false
                                    },
                                    usuariosExistentes = usuariosRegistrados
                                )
                            } else {
                                PantallaInicioSesion(
                                    iniciarSesion = { rol, correoLogueado, token, nombreLogueado, telefonoLogueado, centroId, estadoCentro, centroActivo, esDemostrativo ->
                                        tokenSesion = token
                                        preferencias.guardarSesionActiva(
                                            token = token,
                                            correo = correoLogueado,
                                            rol = rol,
                                            centroId = centroId,
                                            estadoCentro = estadoCentro,
                                            centroActivo = centroActivo,
                                            esDemostrativo = esDemostrativo
                                        )
                                        idCentroActivo = centroId
                                        estadoCentroActivo = estadoCentro
                                        centroActivoFlag = centroActivo
                                        centroDemostrativoFlag = esDemostrativo
                                        if (centroId != null && centroId > 0) {
                                            idCentroSeleccionado = centroId
                                        }

                                        mascotasFavoritas.clear()
                                        notificaciones.clear()
                                        solicitudesAdopcion.clear()

                                        val correoNorm = normalizarCorreoUsuario(correoLogueado)
                                        correoSesionActiva = correoNorm
                                        rolUsuario = rol
                                        val usuarioAutenticado = UsuarioRegistrado(
                                            nombre = nombreLogueado,
                                            correo = correoNorm,
                                            contrasena = "",
                                            telefono = telefonoLogueado,
                                            rol = rol
                                        )
                                        usuarioActualSesion = usuarioAutenticado
                                        almacenamientoApp.guardarPerfilCache(
                                            correo = correoNorm,
                                            nombre = nombreLogueado,
                                            telefono = telefonoLogueado,
                                            rol = rol
                                        )

                                        // Cargar preferencias del usuario (apariencia y notificaciones en SharedPreferences; catálogo en DataStore)
                                        temaSeleccionado = preferencias.obtenerTema(correoNorm)
                                        colorSeleccionado = preferencias.obtenerColorPrincipal(correoNorm)
                                        tamanoTextoSeleccionado = preferencias.obtenerTamanoTexto(correoNorm)
                                        notificarNuevasMascotas = preferencias.obtenerNotificacionesNuevasMascotas(correoNorm)
                                        notificarAdopciones = preferencias.obtenerNotificacionesAdopciones(correoNorm)
                                        notificarMensajes = preferencias.obtenerNotificacionesMensajes(correoNorm)

                                        // Cargar datos aislados (favoritos con identificadores unívocos para mascotas remotas)
                                        val clavesFavoritas = almacenamientoApp.cargarFavoritosClaves(correoNorm)
                                        mascotasFavoritas.clear()
                                        mascotasFavoritas.addAll(
                                            mascotas.filter { pet ->
                                                pet.esRemota && clavesFavoritas.contains(pet.claveCompose)
                                            }
                                        )
                                        notificaciones.addAll(almacenamientoApp.cargarNotificaciones(correoNorm))

                                        // Sincronización autoritativa remota: la lista visible se alimenta exclusivamente de Neon
                                        solicitudesAdopcion.clear()
                                        recargarSolicitudesTrigger++
                                    },
                                    irARegistro = {
                                        mostrarRegistro = true
                                        mensajeExitoRegistro = null
                                    },
                                    usuariosRegistrados = usuariosRegistrados,
                                    mensajeExitoInicial = mensajeExitoRegistro,
                                    correoInicial = correoRegistrado
                                )
                            }
                        }

                        "Admin", "Administrador" -> {
                            PantallaAdministrador(
                                solicitudes = solicitudesAdopcion,
                                aprobar = manejarAprobar,
                                rechazar = manejarRechazar,
                                cerrarSesion = manejarCerrarSesion,
                                mascotas = mascotas,
                                publicaciones = mascotasPublicadas,
                                ponerEnRevision = manejarPonerEnRevision,
                                cambiarEstado = manejarCambiarEstadoMascota,
                                eliminarMascota = manejarEliminarMascotaAdmin
                            )
                        }

                        "Centro" -> {
                            val miCentroViewModel = remember { MiCentroViewModel() }

                            LaunchedEffect(tokenSesion) {
                                if (!tokenSesion.isNullOrBlank()) {
                                    try {
                                        val resp = RetrofitClient.centrosApi.obtenerMiCentro()
                                        if (resp.isSuccessful && resp.body() != null) {
                                            val dto = resp.body()!!
                                            estadoCentroActivo = dto.estado
                                            centroActivoFlag = dto.activo
                                            idCentroActivo = dto.id
                                            preferencias.actualizarEstadoCentro(dto.estado, dto.activo)
                                            preferencias.actualizarCentroId(dto.id)
                                        } else if (resp.code() == 401) {
                                            manejarCerrarSesion()
                                        }
                                    } catch (e: Exception) {
                                        Log.w("CENTRO_SESSION", "No se pudo sincronizar estado del centro", e)
                                    }
                                }
                            }

                            if (mostrarPublicarMascotaCentro) {
                                PantallaPublicarMascota(
                                    correoActual = correoSesionActiva,
                                    publicar = { nuevaMascota ->
                                        mascotasPublicadas.add(0, nuevaMascota)
                                        mascotas.add(0, nuevaMascota)
                                        almacenamientoMascotas.guardarMascotas(mascotasPublicadas)
                                        recargarCatalogoTrigger++
                                        mostrarPublicarMascotaCentro = false
                                    },
                                    volver = {
                                        mostrarPublicarMascotaCentro = false
                                        estadoCentroActivo = preferencias.obtenerEstadoCentroSesionActiva()
                                        centroActivoFlag = preferencias.esCentroActivoSesionActiva()
                                    }
                                )
                            } else if (chatAbiertoCentro) {
                                PantallaChat(
                                    idCentro = idCentroSeleccionado,
                                    nombreCentro = nombreCentroSeleccionado,
                                    correoCentro = correoCentroSeleccionado,
                                    correoUsuarioActual = correoSesionActiva,
                                    rolUsuario = "Centro",
                                    correoAdoptante = correoAdoptanteSeleccionado,
                                    mensajes = mensajesChatActuales,
                                    enviarMensaje = manejarEnviarMensaje,
                                    volver = {
                                        chatAbiertoCentro = false
                                    }
                                )
                            } else if (estadoCentroActivo.equals("Aprobado", ignoreCase = true) && centroActivoFlag) {
                                val conversacionesCentro = almacenamientoApp.cargarConversacionesCentro(correoSesionActiva)
                                PantallaCentro(
                                    conversaciones = conversacionesCentro,
                                    idCentroActualInicial = idCentroActivo ?: idCentroSeleccionado,
                                    abrirConversacion = { idCentro, correoAdoptante, nombreCentro ->
                                        idCentroSeleccionado = idCentro
                                        correoAdoptanteSeleccionado = correoAdoptante
                                        nombreCentroSeleccionado = nombreCentro
                                        correoCentroSeleccionado = correoSesionActiva
                                        mensajesChatActuales.clear()
                                        mensajesChatActuales.addAll(
                                            almacenamientoApp.cargarMensajesChat(correoAdoptante, idCentro)
                                        )
                                        chatAbiertoCentro = true
                                    },
                                    cerrarSesion = manejarCerrarSesion,
                                    onPublicarMascota = {
                                        mostrarPublicarMascotaCentro = true
                                    },
                                    onVerEstadoPendiente = {
                                        estadoCentroActivo = "Pendiente"
                                    },
                                    mascotasTotales = mascotas,
                                    viewModel = miCentroViewModel
                                )
                            } else {
                                PantallaEstadoCentro(
                                    viewModel = miCentroViewModel,
                                    onNavegarAPanelCentro = {
                                        estadoCentroActivo = "Aprobado"
                                        centroActivoFlag = true
                                        preferencias.actualizarEstadoCentro("Aprobado", true)
                                    },
                                    onCerrarSesion = manejarCerrarSesion
                                )
                            }
                        }

                        else -> {
                            val correoUsuarioActivo = normalizarCorreoUsuario(
                                correoSesionActiva.ifBlank {
                                    usuarioActualSesion?.correo ?: ""
                                }
                            )

                            PantallaPrincipalAdoptante(
                                correoUsuarioActivo = correoUsuarioActivo,
                                usuarioActualSesion = usuarioActualSesion,
                                usuariosRegistrados = usuariosRegistrados,
                                actualizarUsuarioSesion = {
                                    usuarioActualSesion = it
                                    almacenamientoApp.guardarPerfilCache(
                                        correo = it.correo,
                                        nombre = it.nombre,
                                        telefono = it.telefono,
                                        rol = it.rol
                                    )
                                },
                                mascotas = mascotas,
                                mascotasPublicadas = mascotasPublicadas,
                                mascotasFavoritas = mascotasFavoritas,
                                notificaciones = notificaciones,
                                solicitudesAdopcion = solicitudesAdopcion,
                                mensajesChatActuales = mensajesChatActuales,
                                almacenamientoApp = almacenamientoApp,
                                almacenamientoMascotas = almacenamientoMascotas,
                                preferencias = preferencias,
                                temaSeleccionado = temaSeleccionado,
                                cambiarTema = { temaSeleccionado = it },
                                colorSeleccionado = colorSeleccionado,
                                cambiarColor = { colorSeleccionado = it },
                                tamanoTextoSeleccionado = tamanoTextoSeleccionado,
                                cambiarTamanoTexto = { tamanoTextoSeleccionado = it },
                                mascotaPreferida = mascotaPreferida,
                                cambiarMascotaPreferida = { mascotaPreferida = it },
                                ubicacionPreferida = ubicacionPreferida,
                                cambiarUbicacionPreferida = { ubicacionPreferida = it },
                                ordenPredeterminado = ordenPredeterminado,
                                cambiarOrdenPredeterminado = { ordenPredeterminado = it },
                                soloDisponibles = soloDisponibles,
                                cambiarSoloDisponibles = { soloDisponibles = it },
                                notificarNuevasMascotas = notificarNuevasMascotas,
                                cambiarNotificarNuevasMascotas = { notificarNuevasMascotas = it },
                                notificarAdopciones = notificarAdopciones,
                                cambiarNotificarAdopciones = { notificarAdopciones = it },
                                notificarMensajes = notificarMensajes,
                                cambiarNotificarMensajes = { notificarMensajes = it },
                                restablecerPreferencias = {
                                    temaSeleccionado = Preferencias.TEMA_SISTEMA
                                    tamanoTextoSeleccionado = Preferencias.TEXTO_NORMAL
                                    colorSeleccionado = Preferencias.COLOR_VERDE
                                    mascotaPreferida = Preferencias.MASCOTA_TODAS
                                    ubicacionPreferida = Preferencias.UBICACION_TODAS
                                    ordenPredeterminado = Preferencias.ORDEN_RECIENTES
                                    soloDisponibles = false
                                    notificarNuevasMascotas = true
                                    notificarAdopciones = true
                                    notificarMensajes = true
                                },
                                idCentroSeleccionado = idCentroSeleccionado,
                                cambiarIdCentro = { idCentroSeleccionado = it },
                                nombreCentroSeleccionado = nombreCentroSeleccionado,
                                cambiarNombreCentro = { nombreCentroSeleccionado = it },
                                correoCentroSeleccionado = correoCentroSeleccionado,
                                cambiarCorreoCentro = { correoCentroSeleccionado = it },
                                correoAdoptanteSeleccionado = correoAdoptanteSeleccionado,
                                cambiarCorreoAdoptante = { correoAdoptanteSeleccionado = it },
                                enviarMensajeChat = manejarEnviarMensaje,
                                mascotaSeleccionada = mascotaSeleccionada,
                                cambiarMascotaSeleccionada = { mascotaSeleccionada = it },
                                mostrarPantallaAdopcion = mostrarPantallaAdopcion,
                                cambiarMostrarPantallaAdopcion = { mostrarPantallaAdopcion = it },
                                abrirDetalleActivity = abrirDetalleActivity,
                                cerrarSesion = manejarCerrarSesion,
                                cargandoCatalogo = cargandoCatalogo,
                                errorCatalogo = errorCatalogo,
                                reintentarCargaCatalogo = { recargarCatalogoTrigger++ },
                                mascotasVisitadas = mascotasVisitadas,
                                catalogoDataStore = catalogoDataStore
                            )
                        }
                    }
                }
            }
        }
    }
}