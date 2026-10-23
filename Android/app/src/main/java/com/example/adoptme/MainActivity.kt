package com.example.adoptme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.adoptme.AdoptMe.PantallaAdministrador
import com.example.adoptme.AdoptMe.PantallaCentro
import com.example.adoptme.AdoptMe.PantallaChat
import com.example.adoptme.AdoptMe.PantallaInicioSesion
import com.example.adoptme.AdoptMe.PantallaPrincipalAdoptante
import com.example.adoptme.AdoptMe.PantallaRegistro
import com.example.adoptme.AdoptMe.UsuarioRegistrado
import com.example.adoptme.SharedPreferences.Preferencias
import com.example.adoptme.data.AppStorage
import com.example.adoptme.data.MascotaStorage
import com.example.adoptme.data.PetRepository
import com.example.adoptme.data.normalizarCorreoUsuario
import com.example.adoptme.data.sincronizarPublicacionesUsuario
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.MensajeChat
import com.example.adoptme.model.NotificationItem
import com.example.adoptme.model.Pet
import com.example.adoptme.ui.theme.AdoptMeTheme
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import com.example.adoptme.data.remote.RetrofitClient
import com.example.adoptme.data.remote.toPet
import com.example.adoptme.data.remote.toDto
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicialización de repositorios y persistencia local
        val repositorio = PetRepository()
        val almacenamientoMascotas = MascotaStorage(applicationContext)
        val almacenamientoApp = AppStorage(applicationContext)
        val preferencias = Preferencias(applicationContext)

        // NUEVO: Conectar Retrofit con SharedPreferences para usar el Token JWT
        RetrofitClient.preferencias = preferencias

        // Migración idempotente de claves globales antiguas
        almacenamientoApp.migrarDatosAntiguosSiExisten()
        preferencias.migrarPreferenciasAntiguasSiExisten()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            // 2. Estados globales de sesión
            var correoSesionActiva by remember { mutableStateOf("") }
            var rolUsuario by remember { mutableStateOf<String?>(null) }
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

            var tokenSesion by remember { mutableStateOf<String?>(null) }

            AdoptMeTheme(
                tema = temaSeleccionado,
                colorPrincipal = colorSeleccionado,
                tamanoTexto = tamanoTextoSeleccionado
            ) {
                Surface {
                    // 5. Listas reactivas principales (fuente única de verdad)
                    val mascotasPublicadas = remember {
                        mutableStateListOf<Pet>().apply {
                            addAll(almacenamientoMascotas.cargarMascotas())
                        }
                    }

                    val mascotas = remember {
                        mutableStateListOf<Pet>().apply {
                            addAll(mascotasPublicadas)
                            addAll(repositorio.getPets())
                        }
                    }

                    LaunchedEffect(Unit) {
                        try {
                            val mascotasAzure = RetrofitClient.mascotasApi.obtenerMascotas()
                            val mascotasRemotas = mascotasAzure.map { it.toPet() }

                            Log.d(
                                "ADOPTME_RETROFIT",
                                "Consulta exitosa. Mascotas recibidas: ${mascotasAzure.size}"
                            )

                            mascotasRemotas.forEach { mascotaNueva ->
                                val indiceExistente = mascotas.indexOfFirst {
                                    it.esRemota && it.id == mascotaNueva.id
                                }

                                if (indiceExistente >= 0) {
                                    mascotas[indiceExistente] = mascotaNueva
                                } else {
                                    mascotas.add(mascotaNueva)
                                }
                            }

                            if (correoSesionActiva.isNotBlank()) {
                                val sincronizadas = sincronizarPublicacionesUsuario(
                                    correoUsuario = correoSesionActiva,
                                    mascotasRemotas = mascotasRemotas,
                                    publicacionesLocales = mascotasPublicadas
                                )
                                mascotasPublicadas.clear()
                                mascotasPublicadas.addAll(sincronizadas)
                                almacenamientoMascotas.guardarMascotas(mascotasPublicadas)
                            }

                        } catch (e: Exception) {
                            Log.e(
                                "ADOPTME_RETROFIT",
                                "Error consultando Azure/Neon",
                                e
                            )
                        }
                    }

                    // FASE 1C: Sincronizar publicaciones del usuario autenticado en cuanto inicia sesión
                    LaunchedEffect(correoSesionActiva) {
                        if (correoSesionActiva.isNotBlank()) {
                            try {
                                val mascotasAzure = RetrofitClient.mascotasApi.obtenerMascotas()
                                val mascotasRemotas = mascotasAzure.map { it.toPet() }

                                mascotasRemotas.forEach { mascotaNueva ->
                                    val indiceExistente = mascotas.indexOfFirst {
                                        it.esRemota && it.id == mascotaNueva.id
                                    }
                                    if (indiceExistente >= 0) {
                                        mascotas[indiceExistente] = mascotaNueva
                                    } else {
                                        mascotas.add(mascotaNueva)
                                    }
                                }

                                val sincronizadas = sincronizarPublicacionesUsuario(
                                    correoUsuario = correoSesionActiva,
                                    mascotasRemotas = mascotasRemotas,
                                    publicacionesLocales = mascotasPublicadas
                                )
                                mascotasPublicadas.clear()
                                mascotasPublicadas.addAll(sincronizadas)
                                almacenamientoMascotas.guardarMascotas(mascotasPublicadas)

                                Log.d("ADOPTME_SYNC", "Sincronización completada para publicaciones del usuario: ${sincronizadas.size}")
                            } catch (e: Exception) {
                                Log.e("ADOPTME_SYNC", "Error al sincronizar publicaciones con Neon", e)
                            }
                        }
                    }

                    val mascotasFavoritas = remember { mutableStateListOf<Pet>() }
                    val notificaciones = remember { mutableStateListOf<NotificationItem>() }
                    val solicitudesAdopcion = remember { mutableStateListOf<AdoptionRequest>() }

                    // Estados de navegación para detalle y adopción
                    var mascotaSeleccionada by remember { mutableStateOf<Pet?>(null) }
                    var mostrarPantallaAdopcion by remember { mutableStateOf(false) }

                    // Estados para el chat local entre Adoptante y Centro
                    var idCentroSeleccionado by remember { mutableStateOf(1) }
                    var nombreCentroSeleccionado by remember { mutableStateOf("Albergue Huellitas del Norte - Lima (Demo)") }
                    var correoCentroSeleccionado by remember { mutableStateOf("centro@adoptme.com") }
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
                        }
                        launcherDetalle.launch(intent)
                    }

                    // 7. Handlers de administración
                    val manejarAprobar: (AdoptionRequest) -> Pair<Boolean, String> = { solicitud ->
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

                    val manejarPonerEnRevision: (AdoptionRequest) -> Unit = { solicitud ->
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
                        // NUEVO: Borrar el token al cerrar sesión
                        preferencias.borrarTokenSesionActiva()

                        mascotasFavoritas.clear()
                        notificaciones.clear()
                        solicitudesAdopcion.clear()
                        mensajesChatActuales.clear()
                        tokenSesion = null
                        correoSesionActiva = ""
                        rolUsuario = null
                        usuarioActualSesion = null
                        mascotaSeleccionada = null
                        mostrarPantallaAdopcion = false
                        idCentroSeleccionado = 1
                        nombreCentroSeleccionado = "Albergue Huellitas del Norte - Lima (Demo)"
                        correoCentroSeleccionado = "centro@adoptme.com"
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
                                    iniciarSesion = { rol, correoLogueado, token ->
                                        tokenSesion = token
                                        // NUEVO: Guardar el token localmente para que Retrofit lo use
                                        preferencias.guardarTokenSesionActiva(token)

                                        mascotasFavoritas.clear()
                                        notificaciones.clear()
                                        solicitudesAdopcion.clear()

                                        val correoNorm = normalizarCorreoUsuario(correoLogueado)
                                        correoSesionActiva = correoNorm
                                        rolUsuario = rol
                                        usuarioActualSesion = usuariosRegistrados.find {
                                            normalizarCorreoUsuario(it.correo) == correoNorm
                                        }

                                        // Cargar preferencias del usuario
                                        temaSeleccionado = preferencias.obtenerTema(correoNorm)
                                        colorSeleccionado = preferencias.obtenerColorPrincipal(correoNorm)
                                        tamanoTextoSeleccionado = preferencias.obtenerTamanoTexto(correoNorm)
                                        mascotaPreferida = preferencias.obtenerMascotaPreferida(correoNorm)
                                        ubicacionPreferida = preferencias.obtenerUbicacionPreferida(correoNorm)
                                        ordenPredeterminado = preferencias.obtenerOrdenPredeterminado(correoNorm)
                                        soloDisponibles = preferencias.obtenerSoloDisponibles(correoNorm)
                                        notificarNuevasMascotas = preferencias.obtenerNotificacionesNuevasMascotas(correoNorm)
                                        notificarAdopciones = preferencias.obtenerNotificacionesAdopciones(correoNorm)
                                        notificarMensajes = preferencias.obtenerNotificacionesMensajes(correoNorm)

                                        // Cargar datos aislados
                                        val idsFavoritos = almacenamientoApp.cargarFavoritos(correoNorm)
                                        mascotasFavoritas.addAll(mascotas.filter { idsFavoritos.contains(it.id) })
                                        notificaciones.addAll(almacenamientoApp.cargarNotificaciones(correoNorm))

                                        if (rol == "Admin") {
                                            solicitudesAdopcion.addAll(almacenamientoApp.cargarSolicitudesGenerales())
                                        } else {
                                            solicitudesAdopcion.addAll(almacenamientoApp.cargarSolicitudesUsuario(correoNorm))
                                        }
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

                        "Admin" -> {
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
                            if (chatAbiertoCentro) {
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
                            } else {
                                val conversacionesCentro = almacenamientoApp.cargarConversacionesCentro(correoSesionActiva)
                                PantallaCentro(
                                    conversaciones = conversacionesCentro,
                                    idCentroActualInicial = idCentroSeleccionado,
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
                                    cerrarSesion = manejarCerrarSesion
                                )
                            }
                        }

                        else -> {
                            val correoUsuarioActivo = normalizarCorreoUsuario(
                                correoSesionActiva.ifBlank {
                                    usuarioActualSesion?.correo ?: if (rolUsuario == "Adoptante") "usuario@adoptme.com" else ""
                                }
                            )

                            PantallaPrincipalAdoptante(
                                correoUsuarioActivo = correoUsuarioActivo,
                                usuarioActualSesion = usuarioActualSesion,
                                usuariosRegistrados = usuariosRegistrados,
                                actualizarUsuarioSesion = { usuarioActualSesion = it },
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
                                cerrarSesion = manejarCerrarSesion
                            )
                        }
                    }
                }
            }
        }
    }
}