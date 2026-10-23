package com.example.adoptme.AdoptMe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.adoptme.SharedPreferences.Preferencias
import com.example.adoptme.data.AppStorage
import com.example.adoptme.data.MascotaStorage
import com.example.adoptme.data.normalizarCorreoUsuario
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.MensajeChat
import com.example.adoptme.model.NotificationItem
import com.example.adoptme.model.Pet
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.adoptme.data.remote.RetrofitClient
import com.example.adoptme.data.remote.toDto
import com.example.adoptme.data.remote.toCrearSolicitudDto
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal y contenedor de navegación para el rol Adoptante.
 * Gestiona el Scaffold con su barra inferior, el flujo de adopción/éxito
 * y las pestañas: inicio, favoritos, centros, publicar, chat, avisos y perfil.
 */
@Composable
fun PantallaPrincipalAdoptante(
    correoUsuarioActivo: String,
    usuarioActualSesion: UsuarioRegistrado?,
    usuariosRegistrados: MutableList<UsuarioRegistrado>,
    actualizarUsuarioSesion: (UsuarioRegistrado) -> Unit,
    mascotas: MutableList<Pet>,
    mascotasPublicadas: MutableList<Pet>,
    mascotasFavoritas: MutableList<Pet>,
    notificaciones: MutableList<NotificationItem>,
    solicitudesAdopcion: MutableList<AdoptionRequest>,
    mensajesChatActuales: MutableList<MensajeChat>,
    almacenamientoApp: AppStorage,
    almacenamientoMascotas: MascotaStorage,
    preferencias: Preferencias,
    temaSeleccionado: String,
    cambiarTema: (String) -> Unit,
    colorSeleccionado: String,
    cambiarColor: (String) -> Unit,
    tamanoTextoSeleccionado: String,
    cambiarTamanoTexto: (String) -> Unit,
    mascotaPreferida: String,
    cambiarMascotaPreferida: (String) -> Unit,
    ubicacionPreferida: String,
    cambiarUbicacionPreferida: (String) -> Unit,
    ordenPredeterminado: String,
    cambiarOrdenPredeterminado: (String) -> Unit,
    soloDisponibles: Boolean,
    cambiarSoloDisponibles: (Boolean) -> Unit,
    notificarNuevasMascotas: Boolean,
    cambiarNotificarNuevasMascotas: (Boolean) -> Unit,
    notificarAdopciones: Boolean,
    cambiarNotificarAdopciones: (Boolean) -> Unit,
    notificarMensajes: Boolean,
    cambiarNotificarMensajes: (Boolean) -> Unit,
    restablecerPreferencias: () -> Unit,
    idCentroSeleccionado: Int,
    cambiarIdCentro: (Int) -> Unit,
    nombreCentroSeleccionado: String,
    cambiarNombreCentro: (String) -> Unit,
    correoCentroSeleccionado: String,
    cambiarCorreoCentro: (String) -> Unit,
    correoAdoptanteSeleccionado: String,
    cambiarCorreoAdoptante: (String) -> Unit,
    enviarMensajeChat: (String) -> Unit,
    mascotaSeleccionada: Pet?,
    cambiarMascotaSeleccionada: (Pet?) -> Unit,
    mostrarPantallaAdopcion: Boolean,
    cambiarMostrarPantallaAdopcion: (Boolean) -> Unit,
    abrirDetalleActivity: (Pet) -> Unit,
    cerrarSesion: () -> Unit,
    cargandoCatalogo: Boolean = false,
    errorCatalogo: String? = null,
    reintentarCargaCatalogo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pantallaSeleccionada by remember { mutableStateOf("home") }
    var mostrarPantallaExito by remember { mutableStateOf(false) }
    var mostrarMisPublicaciones by remember { mutableStateOf(false) }
    var mostrarPreferencias by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (!mostrarPantallaAdopcion && !mostrarPantallaExito) {
                BarraNavegacionInferior(
                    pantalla = pantallaSeleccionada,
                    cambiarPantalla = { pantalla ->
                        pantallaSeleccionada = pantalla
                    },
                    mostrarAdmin = false
                )
            }
        }
    ) { rellenoInterior ->
        Box(
            modifier = Modifier
                .padding(rellenoInterior)
                .consumeWindowInsets(rellenoInterior)
        ) {
            when {
                // 1. Pantalla de Éxito al completar una solicitud
                mostrarPantallaExito && mascotaSeleccionada != null -> {
                    PantallaExito(
                        nombreMascota = mascotaSeleccionada.name,
                        finalizar = {
                            mostrarPantallaExito = false
                            cambiarMostrarPantallaAdopcion(false)
                            cambiarMascotaSeleccionada(null)
                            pantallaSeleccionada = "home"
                        }
                    )
                }

                // 2. Formulario de Solicitud de Adopción
                mostrarPantallaAdopcion && mascotaSeleccionada != null -> {
                    val esMascotaPropia = mascotaSeleccionada.creadoPorCorreo.isNotBlank() &&
                            mascotaSeleccionada.creadoPorCorreo.equals(correoUsuarioActivo, ignoreCase = true)

                    PantallaAdopcion(
                        mascota = mascotaSeleccionada,
                        volver = {
                            cambiarMostrarPantallaAdopcion(false)
                            cambiarMascotaSeleccionada(null)
                        },
                        esPublicacionPropia = esMascotaPropia,
                        correoUsuarioActual = correoUsuarioActivo,
                        enviar = { solicitud, onCompleto ->
                            if (esMascotaPropia) {
                                onCompleto(false, "No puedes solicitar la adopción de una mascota publicada por ti.")
                                return@PantallaAdopcion
                            }

                            // Forzar siempre el correo normalizado de la sesión activa
                            val correoSolicitante = normalizarCorreoUsuario(correoUsuarioActivo)

                            // REGLA FASE 2C: Solo una mascota confirmada como remota (esRemota == true e id > 0)
                            // se envía a Neon vía Retrofit. Las mascotas puramente locales o de demo se mantienen localmente.
                            val esConfirmadaRemota = mascotaSeleccionada.esRemota && mascotaSeleccionada.id > 0

                            if (esConfirmadaRemota) {
                                coroutineScope.launch {
                                    try {
                                        val reqDto = solicitud.toCrearSolicitudDto(mascotaSeleccionada.id)
                                        val respuesta = RetrofitClient.solicitudesApi.crearSolicitud(reqDto)

                                        if (respuesta.isSuccessful && respuesta.body() != null) {
                                            val body = respuesta.body()!!
                                            // Registramos la copia local identificada con el ID real devuelto por Neon
                                            val solicitudConfirmada = solicitud.copy(
                                                id = body.id,
                                                petId = body.mascotaId,
                                                email = correoSolicitante,
                                                status = body.status
                                            )

                                            solicitudesAdopcion.add(solicitudConfirmada)
                                            almacenamientoApp.actualizarEstadoSolicitud(solicitudConfirmada)

                                            if (notificarAdopciones) {
                                                val notifSol = NotificationItem(
                                                    title = "Solicitud enviada",
                                                    message = "Tu solicitud para adoptar a ${solicitudConfirmada.petName} fue registrada en el servidor."
                                                )
                                                notificaciones.add(0, notifSol)
                                                almacenamientoApp.guardarNotificaciones(correoSolicitante, notificaciones)
                                            }

                                            Toast.makeText(contexto, "Solicitud enviada con éxito.", Toast.LENGTH_SHORT).show()
                                            onCompleto(true, null)
                                            mostrarPantallaExito = true
                                        } else {
                                            val errorMsg = when (respuesta.code()) {
                                                400 -> {
                                                    val detalle = try { respuesta.errorBody()?.string() } catch (_: Exception) { null }
                                                    if (detalle?.contains("publicada por ti", ignoreCase = true) == true) {
                                                        "No puedes solicitar la adopción de una mascota publicada por ti."
                                                    } else if (detalle?.contains("disponible", ignoreCase = true) == true) {
                                                        "La mascota ya no está disponible para adopción."
                                                    } else {
                                                        "No se pudo registrar la solicitud. Verifica los datos enviados."
                                                    }
                                                }
                                                401 -> "Sesión expirada o no autorizada. Inicia sesión nuevamente."
                                                403 -> "No tienes autorización para realizar esta solicitud."
                                                404 -> "La mascota ya no existe en el servidor."
                                                409 -> "Ya cuentas con una solicitud activa o aprobada para esta mascota."
                                                else -> "No se pudo registrar la solicitud en el servidor (Código: ${respuesta.code()})."
                                            }
                                            Toast.makeText(contexto, errorMsg, Toast.LENGTH_LONG).show()
                                            onCompleto(false, errorMsg)
                                        }
                                    } catch (e: Exception) {
                                        val errorMsg = "No se pudo conectar con el servidor. Revisa tu conexión a Internet."
                                        Toast.makeText(contexto, errorMsg, Toast.LENGTH_LONG).show()
                                        onCompleto(false, errorMsg)
                                    }
                                }
                            } else {
                                // Flujo local para mascotas puramente demostrativas
                                val yaExisteSolicitudActiva = almacenamientoApp.cargarSolicitudesGenerales().any { req ->
                                    val coincideMascota = (req.petId != 0 && req.petId == mascotaSeleccionada.id) ||
                                            req.petName.equals(mascotaSeleccionada.name, ignoreCase = true)
                                    val coincidePostulante = normalizarCorreoUsuario(req.email) == correoSolicitante
                                    val esActiva = req.status.equals("Pendiente", ignoreCase = true) ||
                                            req.status.equals("En revisión", ignoreCase = true)
                                    coincideMascota && coincidePostulante && esActiva
                                }

                                if (yaExisteSolicitudActiva) {
                                    val errorMsg = "Ya cuentas con una solicitud activa para esta mascota."
                                    Toast.makeText(contexto, errorMsg, Toast.LENGTH_LONG).show()
                                    onCompleto(false, errorMsg)
                                    return@PantallaAdopcion
                                }

                                if (mascotaSeleccionada.status.equals("Disponible", ignoreCase = true)) {
                                    val solicitudFinal = solicitud.copy(
                                        petId = if (solicitud.petId == 0) mascotaSeleccionada.id else solicitud.petId,
                                        email = correoSolicitante
                                    )

                                    solicitudesAdopcion.add(solicitudFinal)
                                    almacenamientoApp.actualizarEstadoSolicitud(solicitudFinal)

                                    if (notificarAdopciones) {
                                        val notifSol = NotificationItem(
                                            title = "Solicitud enviada",
                                            message = "Tu solicitud para adoptar a ${solicitudFinal.petName} fue registrada correctamente."
                                        )
                                        notificaciones.add(0, notifSol)
                                        almacenamientoApp.guardarNotificaciones(correoSolicitante, notificaciones)
                                    }

                                    onCompleto(true, null)
                                    mostrarPantallaExito = true
                                } else {
                                    val errorMsg = "Esta mascota no está disponible para nuevas solicitudes."
                                    Toast.makeText(contexto, errorMsg, Toast.LENGTH_LONG).show()
                                    onCompleto(false, errorMsg)
                                }
                            }
                        }
                    )
                }

                // 3. Pestañas de la barra de navegación
                else -> {
                    when (pantallaSeleccionada) {
                        "home" -> {
                            PantallaInicio(
                                mascotas = mascotas,
                                favoritos = mascotasFavoritas,
                                mascotaPreferida = mascotaPreferida,
                                ubicacionPreferida = ubicacionPreferida,
                                ordenPredeterminado = ordenPredeterminado,
                                soloDisponibles = soloDisponibles,
                                seleccionarMascota = { mascota ->
                                    abrirDetalleActivity(mascota)
                                },
                                cambiarFavorito = { mascota ->
                                    val indice = mascotasFavoritas.indexOfFirst { it.claveCompose == mascota.claveCompose }
                                    if (indice >= 0) {
                                        mascotasFavoritas.removeAt(indice)
                                    } else {
                                        mascotasFavoritas.add(mascota)
                                    }
                                    almacenamientoApp.guardarFavoritosClaves(
                                        correoUsuarioActivo,
                                        mascotasFavoritas.map { it.claveCompose }
                                    )
                                    almacenamientoApp.guardarFavoritos(
                                        correoUsuarioActivo,
                                        mascotasFavoritas.map { it.id }
                                    )
                                },
                                abrirFavoritos = {
                                    pantallaSeleccionada = "favorites"
                                },
                                correoActual = correoUsuarioActivo,
                                solicitarAdopcionDesdeTest = { mascota ->
                                    val esPropia = mascota.creadoPorCorreo.isNotBlank() &&
                                            mascota.creadoPorCorreo.equals(correoUsuarioActivo, ignoreCase = true)
                                    if (!esPropia && mascota.status.equals("Disponible", ignoreCase = true)) {
                                        cambiarMascotaSeleccionada(mascota)
                                        cambiarMostrarPantallaAdopcion(true)
                                    }
                                },
                                cargando = cargandoCatalogo,
                                errorConexion = errorCatalogo,
                                reintentarCarga = reintentarCargaCatalogo
                            )
                        }

                        "favorites" -> {
                            PantallaFavoritos(
                                favoritos = mascotasFavoritas,
                                volver = {
                                    pantallaSeleccionada = "home"
                                },
                                seleccionarMascota = { mascota ->
                                    abrirDetalleActivity(mascota)
                                },
                                cambiarFavorito = { mascota ->
                                    if (mascotasFavoritas.contains(mascota)) {
                                        mascotasFavoritas.remove(mascota)
                                    } else {
                                        mascotasFavoritas.add(mascota)
                                    }
                                    almacenamientoApp.guardarFavoritos(
                                        correoUsuarioActivo,
                                        mascotasFavoritas.map { it.id }
                                    )
                                },
                                correoActual = correoUsuarioActivo
                            )
                        }

                        "centros", "centers" -> {
                            CentrosScreen(
                                volver = {
                                    pantallaSeleccionada = "home"
                                },
                                abrirChat = { idCentro, nombreCentro, correoCentro, _ ->
                                    cambiarIdCentro(idCentro)
                                    cambiarNombreCentro(nombreCentro)
                                    cambiarCorreoCentro(correoCentro)
                                    cambiarCorreoAdoptante(correoUsuarioActivo)
                                    mensajesChatActuales.clear()
                                    val guardados = almacenamientoApp.cargarMensajesChat(correoUsuarioActivo, idCentro)
                                    mensajesChatActuales.addAll(guardados)
                                    if (mensajesChatActuales.isEmpty()) {
                                        val fechaActual = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                                        val mensajeInicial = MensajeChat(
                                            id = System.currentTimeMillis(),
                                            idCentro = idCentro,
                                            remitente = correoUsuarioActivo,
                                            receptor = correoCentro,
                                            texto = "Hola, quisiera recibir información sobre las mascotas disponibles para adopción.",
                                            nombreCentro = nombreCentro,
                                            fecha = fechaActual,
                                            esDelCentro = false
                                        )
                                        mensajesChatActuales.add(mensajeInicial)
                                        almacenamientoApp.guardarMensajesChat(correoUsuarioActivo, idCentro, mensajesChatActuales)
                                        almacenamientoApp.registrarConversacionCentro(
                                            correoCentro = correoCentro,
                                            idCentro = idCentro,
                                            correoAdoptante = correoUsuarioActivo,
                                            nombreCentro = nombreCentro,
                                            ultimoTexto = mensajeInicial.texto,
                                            fecha = fechaActual
                                        )
                                    }
                                    pantallaSeleccionada = "chat"
                                }
                            )
                        }

                        "publish" -> {
                            PantallaPublicarMascota(
                                correoActual = correoUsuarioActivo,
                                publicar = { nuevaMascota ->
                                    mascotasPublicadas.add(0, nuevaMascota)
                                    mascotas.add(0, nuevaMascota)
                                    almacenamientoMascotas.guardarMascotas(mascotasPublicadas)
                                    val notifPub = NotificationItem(
                                        title = "Mascota publicada 🐾",
                                        message = "${nuevaMascota.name} fue publicada correctamente y ya está disponible para adopción."
                                    )
                                    notificaciones.add(0, notifPub)
                                    almacenamientoApp.guardarNotificaciones(correoUsuarioActivo, notificaciones)
                                    pantallaSeleccionada = "home"
                                },
                                volver = {
                                    pantallaSeleccionada = "profile"
                                }
                            )
                        }

                        "chat" -> {
                            if (correoAdoptanteSeleccionado != correoUsuarioActivo || mensajesChatActuales.isEmpty()) {
                                cambiarCorreoAdoptante(correoUsuarioActivo)
                                if (idCentroSeleccionado <= 0) {
                                    cambiarIdCentro(1)
                                    cambiarNombreCentro("Albergue Huellitas del Norte - Lima (Demo)")
                                    cambiarCorreoCentro("centro@adoptme.com")
                                }
                                val guardados = almacenamientoApp.cargarMensajesChat(correoUsuarioActivo, idCentroSeleccionado)
                                mensajesChatActuales.clear()
                                mensajesChatActuales.addAll(guardados)
                                if (mensajesChatActuales.isEmpty()) {
                                    val fechaActual = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                                    val mensajeInicial = MensajeChat(
                                        id = System.currentTimeMillis(),
                                        idCentro = idCentroSeleccionado,
                                        remitente = correoUsuarioActivo,
                                        receptor = correoCentroSeleccionado,
                                        texto = "Hola, quisiera recibir información sobre las mascotas disponibles para adopción.",
                                        nombreCentro = nombreCentroSeleccionado,
                                        fecha = fechaActual,
                                        esDelCentro = false
                                    )
                                    mensajesChatActuales.add(mensajeInicial)
                                    almacenamientoApp.guardarMensajesChat(correoUsuarioActivo, idCentroSeleccionado, mensajesChatActuales)
                                    almacenamientoApp.registrarConversacionCentro(
                                        correoCentro = correoCentroSeleccionado,
                                        idCentro = idCentroSeleccionado,
                                        correoAdoptante = correoUsuarioActivo,
                                        nombreCentro = nombreCentroSeleccionado,
                                        ultimoTexto = mensajeInicial.texto,
                                        fecha = fechaActual
                                    )
                                }
                            }
                            PantallaChat(
                                idCentro = idCentroSeleccionado,
                                nombreCentro = nombreCentroSeleccionado,
                                correoCentro = correoCentroSeleccionado,
                                correoUsuarioActual = correoUsuarioActivo,
                                rolUsuario = "Adoptante",
                                correoAdoptante = correoUsuarioActivo,
                                mensajes = mensajesChatActuales,
                                enviarMensaje = enviarMensajeChat,
                                volver = {
                                    pantallaSeleccionada = "centros"
                                }
                            )
                        }

                        "notifications" -> {
                            PantallaNotificaciones(notificaciones = notificaciones)
                        }

                        "profile" -> {
                            when {
                                mostrarPreferencias -> {
                                    PantallaPreferencias(
                                        temaActual = temaSeleccionado,
                                        tamanoTextoActual = tamanoTextoSeleccionado,
                                        colorActual = colorSeleccionado,
                                        mascotaPreferida = mascotaPreferida,
                                        ubicacionPreferida = ubicacionPreferida,
                                        ordenPredeterminado = ordenPredeterminado,
                                        soloDisponibles = soloDisponibles,
                                        notificarNuevasMascotas = notificarNuevasMascotas,
                                        notificarAdopciones = notificarAdopciones,
                                        notificarMensajes = notificarMensajes,
                                        cambiarTema = { nuevoTema ->
                                            cambiarTema(nuevoTema)
                                            preferencias.guardarTema(correoUsuarioActivo, nuevoTema)
                                        },
                                        cambiarTamanoTexto = { nuevoTamano ->
                                            cambiarTamanoTexto(nuevoTamano)
                                            preferencias.guardarTamanoTexto(correoUsuarioActivo, nuevoTamano)
                                        },
                                        cambiarColor = { nuevoColor ->
                                            cambiarColor(nuevoColor)
                                            preferencias.guardarColorPrincipal(correoUsuarioActivo, nuevoColor)
                                        },
                                        cambiarMascotaPreferida = { nuevaMascota ->
                                            cambiarMascotaPreferida(nuevaMascota)
                                            preferencias.guardarMascotaPreferida(correoUsuarioActivo, nuevaMascota)
                                        },
                                        cambiarUbicacionPreferida = { nuevaUbicacion ->
                                            cambiarUbicacionPreferida(nuevaUbicacion)
                                            preferencias.guardarUbicacionPreferida(correoUsuarioActivo, nuevaUbicacion)
                                        },
                                        cambiarOrden = { nuevoOrden ->
                                            cambiarOrdenPredeterminado(nuevoOrden)
                                            preferencias.guardarOrdenPredeterminado(correoUsuarioActivo, nuevoOrden)
                                        },
                                        cambiarSoloDisponibles = { activado ->
                                            cambiarSoloDisponibles(activado)
                                            preferencias.guardarSoloDisponibles(correoUsuarioActivo, activado)
                                        },
                                        cambiarNotificacionMascotas = { activado ->
                                            cambiarNotificarNuevasMascotas(activado)
                                            preferencias.guardarNotificacionesNuevasMascotas(correoUsuarioActivo, activado)
                                        },
                                        cambiarNotificacionAdopciones = { activado ->
                                            cambiarNotificarAdopciones(activado)
                                            preferencias.guardarNotificacionesAdopciones(correoUsuarioActivo, activado)
                                        },
                                        cambiarNotificacionMensajes = { activado ->
                                            cambiarNotificarMensajes(activado)
                                            preferencias.guardarNotificacionesMensajes(correoUsuarioActivo, activado)
                                        },
                                        restablecer = {
                                            preferencias.restablecerPreferencias(correoUsuarioActivo)
                                            restablecerPreferencias()
                                        },
                                        volver = {
                                            mostrarPreferencias = false
                                        }
                                    )
                                }

                                mostrarMisPublicaciones -> {
                                    MisPublicacionesScreen(
                                        publicaciones = mascotasPublicadas,
                                        correoActual = correoUsuarioActivo,
                                        volver = {
                                            mostrarMisPublicaciones = false
                                        },
                                        eliminar = { mascota ->
                                            fun aplicarEliminacionLocal() {
                                                mascotasPublicadas.removeAll { it.id == mascota.id && it.esRemota == mascota.esRemota }
                                                mascotas.removeAll { it.id == mascota.id && it.esRemota == mascota.esRemota }
                                                mascotasFavoritas.removeAll { it.id == mascota.id && it.esRemota == mascota.esRemota }
                                                if (mascotaSeleccionada?.id == mascota.id && mascotaSeleccionada?.esRemota == mascota.esRemota) {
                                                    cambiarMascotaSeleccionada(null)
                                                }
                                                almacenamientoMascotas.eliminarMascota(
                                                    mascota = mascota,
                                                    mascotasRestantes = mascotasPublicadas
                                                )
                                                almacenamientoApp.guardarFavoritos(
                                                    correoUsuarioActivo,
                                                    mascotasFavoritas.map { it.id }
                                                )
                                                val notifElim = NotificationItem(
                                                    title = "Publicación eliminada",
                                                    message = "La publicación de ${mascota.name} fue eliminada correctamente."
                                                )
                                                notificaciones.add(0, notifElim)
                                                almacenamientoApp.guardarNotificaciones(correoUsuarioActivo, notificaciones)
                                            }

                                            if (mascota.esRemota && mascota.id > 0) {
                                                coroutineScope.launch {
                                                    try {
                                                        val respuesta = RetrofitClient.mascotasApi.eliminarMascota(mascota.id)
                                                        if (respuesta.isSuccessful) {
                                                            aplicarEliminacionLocal()
                                                            Toast.makeText(contexto, "Publicación eliminada del servidor.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val errorMsg = when (respuesta.code()) {
                                                                401 -> "Sesión expirada o no autorizada."
                                                                403 -> "No tienes autorización para eliminar esta publicación."
                                                                404 -> "La mascota ya no existe en el servidor."
                                                                else -> "No se pudo eliminar en el servidor (Código: ${respuesta.code()})."
                                                            }
                                                            Toast.makeText(contexto, errorMsg, Toast.LENGTH_LONG).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(contexto, "No se pudo conectar con el servidor. Revisa tu conexión a Internet.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } else {
                                                aplicarEliminacionLocal()
                                                Toast.makeText(contexto, "Publicación local eliminada.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        editar = { mascotaEditada, onResultado ->
                                            fun aplicarEdicionLocal() {
                                                val indicePub = mascotasPublicadas.indexOfFirst {
                                                    it.id == mascotaEditada.id && it.esRemota == mascotaEditada.esRemota
                                                }
                                                if (indicePub != -1) {
                                                    mascotasPublicadas[indicePub] = mascotaEditada
                                                }
                                                val indiceCat = mascotas.indexOfFirst {
                                                    it.id == mascotaEditada.id && it.esRemota == mascotaEditada.esRemota
                                                }
                                                if (indiceCat != -1) {
                                                    mascotas[indiceCat] = mascotaEditada
                                                }
                                                val indiceFav = mascotasFavoritas.indexOfFirst {
                                                    it.id == mascotaEditada.id && it.esRemota == mascotaEditada.esRemota
                                                }
                                                if (indiceFav != -1) {
                                                    mascotasFavoritas[indiceFav] = mascotaEditada
                                                }
                                                if (mascotaSeleccionada?.id == mascotaEditada.id && mascotaSeleccionada?.esRemota == mascotaEditada.esRemota) {
                                                    cambiarMascotaSeleccionada(mascotaEditada)
                                                }
                                                almacenamientoMascotas.guardarMascotas(mascotasPublicadas)

                                                val notifEdit = NotificationItem(
                                                    title = "Publicación actualizada 🐾",
                                                    message = "La información de ${mascotaEditada.name} se actualizó correctamente."
                                                )
                                                notificaciones.add(0, notifEdit)
                                                almacenamientoApp.guardarNotificaciones(correoUsuarioActivo, notificaciones)
                                            }

                                            // REGLA FASE 1C: Solo una mascota confirmada como remota (esRemota == true e id > 0)
                                            // se envía al backend. Las mascotas locales o de demostración (esRemota == false)
                                            // NUNCA tocan Neon, incluso si su ID coincide con un registro remoto.
                                            val esConfirmadaRemota = mascotaEditada.esRemota && mascotaEditada.id > 0

                                            if (esConfirmadaRemota) {
                                                coroutineScope.launch {
                                                    try {
                                                        val dto = mascotaEditada.toDto()
                                                        val respuesta = RetrofitClient.mascotasApi.actualizarMascota(mascotaEditada.id, dto)
                                                        if (respuesta.isSuccessful) {
                                                            aplicarEdicionLocal()
                                                            Toast.makeText(contexto, "Publicación actualizada en el servidor.", Toast.LENGTH_SHORT).show()
                                                            onResultado(true)
                                                        } else {
                                                            val errorMsg = when (respuesta.code()) {
                                                                401 -> "Sesión expirada o no autorizada."
                                                                403 -> "No tienes autorización para editar esta publicación."
                                                                404 -> "La mascota ya no existe en el servidor."
                                                                else -> "No se pudo actualizar en el servidor (Código: ${respuesta.code()})."
                                                            }
                                                            Toast.makeText(contexto, errorMsg, Toast.LENGTH_LONG).show()
                                                            onResultado(false)
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(contexto, "No se pudo conectar con el servidor. Revisa tu conexión a Internet.", Toast.LENGTH_LONG).show()
                                                        onResultado(false)
                                                    }
                                                }
                                            } else {
                                                aplicarEdicionLocal()
                                                Toast.makeText(contexto, "Publicación local actualizada.", Toast.LENGTH_SHORT).show()
                                                onResultado(true)
                                            }
                                        }
                                    )
                                }

                                else -> {
                                    PantallaPerfil(
                                        solicitudes = solicitudesAdopcion.filter {
                                            normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario(correoUsuarioActivo)
                                        },
                                        usuarioActual = usuarioActualSesion,
                                        abrirPublicaciones = {
                                            mostrarMisPublicaciones = true
                                        },
                                        publicarMascota = {
                                            pantallaSeleccionada = "publish"
                                        },
                                        abrirPreferencias = {
                                            mostrarPreferencias = true
                                        },
                                        cerrarSesion = cerrarSesion,
                                        guardarPerfil = { nuevoNombre, _, nuevoTelefono ->
                                            val actual = usuarioActualSesion
                                            val correoEfectivo = actual?.correo ?: correoUsuarioActivo
                                            val actualizado = (actual ?: UsuarioRegistrado(
                                                nombre = nuevoNombre,
                                                correo = correoEfectivo,
                                                contrasena = "",
                                                telefono = nuevoTelefono,
                                                rol = "Adoptante"
                                            )).copy(
                                                nombre = nuevoNombre,
                                                telefono = nuevoTelefono
                                            )
                                            actualizarUsuarioSesion(actualizado)
                                            val indice = usuariosRegistrados.indexOfFirst {
                                                normalizarCorreoUsuario(it.correo) == normalizarCorreoUsuario(correoEfectivo)
                                            }
                                            if (indice != -1) {
                                                usuariosRegistrados[indice] = actualizado
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

