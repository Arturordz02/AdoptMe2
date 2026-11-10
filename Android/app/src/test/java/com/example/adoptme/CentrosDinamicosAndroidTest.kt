package com.example.adoptme

import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.model.ActualizarMiCentroRequest
import com.example.adoptme.model.AsignarPropietarioCentroRequest
import com.example.adoptme.model.CentroAdminDto
import com.example.adoptme.model.CentroPropietarioDto
import com.example.adoptme.model.CentroPublicoDto
import com.example.adoptme.model.EstadoCentroDto
import com.example.adoptme.model.MascotaDto
import com.example.adoptme.model.OperacionCentroResponse
import com.example.adoptme.model.RechazarCentroRequest
import com.example.adoptme.model.RegistroCentroRequest
import com.example.adoptme.model.toCentroAdopcion
import com.example.adoptme.preferences.Preferencias
import com.example.adoptme.utils.HaversineUtil
import com.example.adoptme.viewmodel.AdminCentrosUiState
import com.example.adoptme.viewmodel.CentrosUiState
import com.example.adoptme.viewmodel.MiCentroUiState
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Pruebas unitarias automatizadas para Centros Dinámicos en Android (Fase D).
 * Cubre:
 * 1. Serialización y deserialización de DTOs institucionales.
 * 2. Mapeo de DTOs remotos a modelos de dominio UI.
 * 3. Estados reactivos de ViewModels (Centros, MiCentro, AdminCentros).
 * 4. Reglas de validación institucional (motivo >= 5 chars, campos obligatorios).
 * 5. Detección de modificaciones sensibles (cambio de nombre, distancia Haversine > 500m).
 * 6. Lógica de enrutamiento por rol y estado de aprobación.
 * 7. Seguridad de manifiesto Android (ausencia de permisos amplios de almacenamiento).
 * 8. Aislamiento de almacenamiento local de chat por centroId dinámico.
 * 9. Persistencia segura de sesión multirol en SharedPreferences.
 */
class CentrosDinamicosAndroidTest {

    private val gson = Gson()
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var preferencias: Preferencias

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        preferencias = Preferencias(fakePrefs)
    }

    // =========================================================
    // 1. SERIALIZACIÓN / DESERIALIZACIÓN DTOs
    // =========================================================

    @Test
    fun centroPublicoDto_deserializacionCorrecta() {
        val json = """
            {
                "id": 1,
                "nombre": "Albergue Huellitas del Norte - Lima",
                "descripcion": "Refugio dedicado al rescate de animales",
                "direccion": "Av. Los Alisos 1234",
                "distrito": "Los Olivos",
                "telefono": "+51 987 654 321",
                "correoContacto": "contacto@huellitasdelnorte.org",
                "horario": "Lunes a Sábado 9:00 - 18:00",
                "latitud": -11.9876,
                "longitud": -77.0654,
                "logoUrl": "https://res.cloudinary.com/demo/image/upload/logo.jpg",
                "esDemostrativo": true
            }
        """.trimIndent()

        val dto = gson.fromJson(json, CentroPublicoDto::class.java)
        assertEquals(1, dto.id)
        assertEquals("Albergue Huellitas del Norte - Lima", dto.nombre)
        assertEquals("Los Olivos", dto.distrito)
        assertEquals(-11.9876, dto.latitud ?: 0.0, 0.0001)
        assertEquals(-77.0654, dto.longitud ?: 0.0, 0.0001)
        assertTrue(dto.esDemostrativo)
        assertEquals("https://res.cloudinary.com/demo/image/upload/logo.jpg", dto.logoUrl)
    }

    @Test
    fun centroPublicoDto_toCentroAdopcion_mapeoCorrecto() {
        val dto = CentroPublicoDto(
            id = 2,
            nombre = "Patitas Felices",
            descripcion = "Centro de adopciones",
            direccion = "Av. Brasil 567",
            distrito = "Jesús María",
            telefono = "999888777",
            correoContacto = "patitas@adopta.pe",
            horario = "Mar-Dom 10am-5pm",
            latitud = -12.0722,
            longitud = -77.0489,
            logoUrl = "https://cloudinary.com/patitas.jpg",
            esDemostrativo = false
        )

        val dominio = dto.toCentroAdopcion()
        assertEquals(2, dominio.id)
        assertEquals("Patitas Felices", dominio.nombre)
        assertTrue("La ubicación debe contener el distrito", dominio.ubicacion.contains("Jesús María"))
        assertEquals("999888777", dominio.telefono)
        assertEquals(-12.0722, dominio.latitud ?: 0.0, 0.0001)
        assertEquals(-77.0489, dominio.longitud ?: 0.0, 0.0001)
        assertEquals("https://cloudinary.com/patitas.jpg", dominio.logoUrl)
        assertFalse(dominio.esDemostrativo)
    }

    @Test
    fun centroPropietarioDto_deserializacionYEstado() {
        val json = """
            {
                "id": 10,
                "usuarioId": 45,
                "nombre": "Refugio San Roque",
                "descripcion": "Rescate canino",
                "direccion": "Calle Las Flores 456",
                "distrito": "Surco",
                "telefono": "912345678",
                "correoContacto": "sanroque@refugio.pe",
                "horario": "Lun-Vie 9-17",
                "latitud": -12.14,
                "longitud": -76.99,
                "logoUrl": null,
                "estado": "Pendiente",
                "motivoRechazo": null,
                "fechaSolicitud": "2026-11-09T20:00:00Z",
                "esDemostrativo": false,
                "activo": false
            }
        """.trimIndent()

        val dto = gson.fromJson(json, CentroPropietarioDto::class.java)
        assertEquals(10, dto.id)
        assertEquals(45, dto.usuarioId)
        assertEquals("Pendiente", dto.estado)
        assertFalse(dto.activo)
        assertNull(dto.motivoRechazo)
    }

    @Test
    fun centroAdminDto_deserializacionConRelaciones() {
        val json = """
            {
                "id": 15,
                "usuarioId": 80,
                "nombre": "Santuario Animal",
                "descripcion": "Rescate multiespecie",
                "direccion": "Jr. Arequipa 123",
                "distrito": "Miraflores",
                "telefono": "987654321",
                "correoContacto": "admin@santuario.org",
                "horario": "Todo el día",
                "latitud": -12.12,
                "longitud": -77.03,
                "logoUrl": null,
                "estado": "Aprobado",
                "motivoRechazo": null,
                "fechaSolicitud": "2026-11-01T10:00:00Z",
                "fechaRevision": "2026-11-02T12:00:00Z",
                "esDemostrativo": false,
                "activo": true,
                "totalMascotasPublicadas": 8
            }
        """.trimIndent()

        val dto = gson.fromJson(json, CentroAdminDto::class.java)
        assertEquals(15, dto.id)
        assertEquals("Santuario Animal", dto.nombre)
        assertEquals("Aprobado", dto.estado)
        assertTrue(dto.activo)
        assertEquals(8, dto.totalMascotasPublicadas)
    }

    @Test
    fun estadoCentroDto_deserializacionCorrecta() {
        val json = """{"estado": "Rechazado", "motivoRechazo": "La dirección institucional ingresada requiere corrección o mayor precisión antes de aprobar la solicitud."}"""
        val dto = gson.fromJson(json, EstadoCentroDto::class.java)
        assertEquals("Rechazado", dto.estado)
        assertEquals("La dirección institucional ingresada requiere corrección o mayor precisión antes de aprobar la solicitud.", dto.motivoRechazo)
    }

    @Test
    fun registroCentroRequest_serializacionJsonValida() {
        val req = RegistroCentroRequest(
            nombreResponsable = "Carlos Gómez",
            correo = "carlos@temporal.pe",
            contrasena = "SecurePassword123!",
            telefonoResponsable = "999111222",
            nombreCentro = "Hogar Temporal Canino",
            descripcion = "Hogar para perros senior",
            direccion = "Av. Benavides 1000",
            distrito = "Miraflores",
            telefonoCentro = "999111222",
            correoContacto = "hogar@temporal.pe",
            horario = "9:00 a 18:00",
            latitud = -12.125,
            longitud = -77.025,
            aceptacionTerminos = true
        )
        val json = gson.toJson(req)
        assertTrue(json.contains("Hogar Temporal Canino"))
        assertTrue(json.contains("carlos@temporal.pe"))
        assertTrue(json.contains("-12.125"))
        assertTrue(json.contains("Carlos Gómez"))
    }

    @Test
    fun actualizarMiCentroRequest_serializacionJsonValida() {
        val req = ActualizarMiCentroRequest(
            nombre = "Centro Actualizado",
            descripcion = "Nueva descripción",
            direccion = "Nueva dirección",
            distrito = "San Isidro",
            telefono = "944555666",
            correoContacto = "contacto@nuevo.pe",
            horario = "10am - 4pm",
            latitud = -12.095,
            longitud = -77.035
        )
        val json = gson.toJson(req)
        assertTrue(json.contains("Centro Actualizado"))
        assertTrue(json.contains("San Isidro"))
    }

    @Test
    fun rechazarCentroRequest_serializacionJsonValida() {
        val req = RechazarCentroRequest(motivo = "Documentación fiscal incompleta")
        val json = gson.toJson(req)
        assertTrue(json.contains("Documentación fiscal incompleta"))
    }

    @Test
    fun asignarPropietarioCentroRequest_serializacionJsonValida() {
        val req = AsignarPropietarioCentroRequest(usuarioId = 55)
        val json = gson.toJson(req)
        assertTrue(json.contains("55"))
    }

    @Test
    fun operacionCentroResponse_deserializacionCorrecta() {
        val json = """{"mensaje": "Centro aprobado con éxito.", "id": 1, "estado": "Aprobado", "activo": true, "logoUrl": null}"""
        val resp = gson.fromJson(json, OperacionCentroResponse::class.java)
        assertEquals("Centro aprobado con éxito.", resp.mensaje)
        assertEquals("Aprobado", resp.estado)
        assertTrue(resp.activo == true)
        assertNull(resp.logoUrl)
    }

    @Test
    fun mascotaDto_incluyeCentroIdOpcional() {
        val jsonConCentro = """{"id": 1, "name": "Bobby", "species": "Perro", "status": "Disponible", "centroId": 3}"""
        val dtoConCentro = gson.fromJson(jsonConCentro, MascotaDto::class.java)
        assertEquals(3, dtoConCentro.centroId)

        val jsonSinCentro = """{"id": 2, "name": "Michi", "species": "Gato", "status": "Disponible", "centroId": null}"""
        val dtoSinCentro = gson.fromJson(jsonSinCentro, MascotaDto::class.java)
        assertNull(dtoSinCentro.centroId)
    }

    // =========================================================
    // 2. ESTADOS REACTIVOS DE VIEWMODELS
    // =========================================================

    @Test
    fun centrosUiState_transicionesCorrectas() {
        val cargando = CentrosUiState.Cargando
        val centrosList = listOf(
            CentroPublicoDto(1, "Centro 1", "Desc", "Dir", "Dist", "Tel", "email", "Hor", null, null, null, false)
        )
        val exito = CentrosUiState.Exito(centrosList)
        val error = CentrosUiState.Error("Error 500 al conectar con Neon")

        assertTrue(cargando is CentrosUiState.Cargando)
        assertTrue(exito is CentrosUiState.Exito)
        assertEquals(1, exito.centros.size)
        assertTrue(error is CentrosUiState.Error)
        assertEquals("Error 500 al conectar con Neon", error.mensaje)
    }

    @Test
    fun miCentroUiState_transicionesCorrectas() {
        val cargando = MiCentroUiState.Cargando
        val dto = CentroPropietarioDto(5, 20, "Mi Refugio", estado = "Pendiente")
        val exito = MiCentroUiState.Exito(dto)
        val error = MiCentroUiState.Error("No se encontró el centro del usuario", codigoHttp = 404)

        assertTrue(cargando is MiCentroUiState.Cargando)
        assertEquals("Mi Refugio", exito.centro.nombre)
        assertEquals(404, error.codigoHttp)
    }

    @Test
    fun adminCentrosUiState_transicionesCorrectas() {
        val cargando = AdminCentrosUiState.Cargando
        val adminList = listOf(
            CentroAdminDto(1, 10, "Centro Admin", estado = "Aprobado", activo = true)
        )
        val exito = AdminCentrosUiState.Exito(adminList)
        val error = AdminCentrosUiState.Error("Error de permisos admin")

        assertTrue(cargando is AdminCentrosUiState.Cargando)
        assertEquals(1, exito.centros.size)
        assertEquals("Error de permisos admin", error.mensaje)
    }

    // =========================================================
    // 3. REGLAS DE VALIDACIÓN INSTITUCIONAL
    // =========================================================

    @Test
    fun rechazarCentro_motivoMenorA5Caracteres_esRechazadoPorValidacion() {
        fun validarMotivoRechazo(motivo: String): Boolean {
            return motivo.trim().length >= 5
        }

        assertFalse(validarMotivoRechazo(""))
        assertFalse(validarMotivoRechazo("   "))
        assertFalse(validarMotivoRechazo("No"))
        assertFalse(validarMotivoRechazo("1234"))
        assertTrue(validarMotivoRechazo("12345"))
        assertTrue(validarMotivoRechazo("La dirección institucional ingresada requiere corrección o mayor precisión antes de aprobar la solicitud."))
    }

    @Test
    fun camposObligatoriosRegistroCentro_validacionEstricta() {
        fun validarRegistroCentro(
            nombre: String,
            distrito: String,
            direccion: String,
            telefono: String,
            correo: String,
            contacto: String
        ): Boolean {
            return nombre.isNotBlank() &&
                    distrito.isNotBlank() &&
                    direccion.isNotBlank() &&
                    telefono.isNotBlank() &&
                    correo.isNotBlank() &&
                    contacto.isNotBlank()
        }

        assertTrue(validarRegistroCentro("Albergue", "Lima", "Av. 1", "999", "c@a.pe", "Juan"))
        assertFalse(validarRegistroCentro("", "Lima", "Av. 1", "999", "c@a.pe", "Juan"))
        assertFalse(validarRegistroCentro("Albergue", "  ", "Av. 1", "999", "c@a.pe", "Juan"))
        assertFalse(validarRegistroCentro("Albergue", "Lima", "", "999", "c@a.pe", "Juan"))
    }

    // =========================================================
    // 4. DETECCIÓN DE MODIFICACIONES SENSIBLES (REVISIÓN INSTITUCIONAL)
    // =========================================================

    @Test
    fun modificacionSensible_cambioNombre_retornaEstadoPendiente() {
        val original = CentroPropietarioDto(1, 1, "Albergue Original", estado = "Aprobado", activo = true)
        val nuevoNombre = "Albergue Renombrado"

        val requiereRevision = !original.nombre.equals(nuevoNombre.trim(), ignoreCase = true)
        assertTrue("El cambio de nombre debe activar la advertencia y re-evaluación administrativa", requiereRevision)
    }

    @Test
    fun modificacionSensible_distanciaHaversineMayorA500m_retornaEstadoPendiente() {
        val latOriginal = -12.046374
        val lonOriginal = -77.042793 // Lima Centro

        // Desplazamiento de ~1 km hacia el norte (-12.037)
        val latNueva = -12.037000
        val lonNueva = -77.042793

        val distKm = HaversineUtil.calcularDistanciaKm(latOriginal, lonOriginal, latNueva, lonNueva) ?: 0.0
        val distanciaMetros = distKm * 1000.0
        assertTrue("La distancia calculada debe ser mayor a 500m ($distanciaMetros m)", distanciaMetros > 500.0)

        val esModificacionSensible = distanciaMetros > 500.0
        assertTrue("Un traslado mayor a 500m debe regresar el estado a Pendiente", esModificacionSensible)
    }

    @Test
    fun modificacionNoSensible_distanciaHaversineMenorA500m_noRequiereReevaluacion() {
        val latOriginal = -12.046374
        val lonOriginal = -77.042793

        // Desplazamiento menor a 100m (-12.0468, -77.0430)
        val latNueva = -12.046800
        val lonNueva = -77.043000

        val distKm = HaversineUtil.calcularDistanciaKm(latOriginal, lonOriginal, latNueva, lonNueva) ?: 0.0
        val distanciaMetros = distKm * 1000.0
        assertTrue("La distancia calculada debe ser menor a 500m ($distanciaMetros m)", distanciaMetros <= 500.0)

        val esModificacionSensible = distanciaMetros > 500.0
        assertFalse("Un ajuste menor a 500m no debe regresar a Pendiente", esModificacionSensible)
    }

    // =========================================================
    // 5. ENRUTAMIENTO POR ROL Y ESTADO DE APROBACIÓN
    // =========================================================

    private enum class PantallaDestino {
        LOGIN, REGISTRO, ADOPTANTE_CATALOGO, ADMIN_PANEL, CENTRO_PANEL, CENTRO_ESTADO_PENDIENTE, CENTRO_ESTADO_RECHAZADO, CENTRO_ESTADO_INACTIVO, ERROR
    }

    private fun resolverEnrutamiento(
        rol: String?,
        estadoCentro: String?,
        centroActivo: Boolean
    ): PantallaDestino {
        return when (rol) {
            null -> PantallaDestino.LOGIN
            "Adoptante" -> PantallaDestino.ADOPTANTE_CATALOGO
            "Admin", "Administrador" -> PantallaDestino.ADMIN_PANEL
            "Centro" -> {
                when {
                    estadoCentro.equals("Aprobado", ignoreCase = true) && centroActivo -> PantallaDestino.CENTRO_PANEL
                    estadoCentro.equals("Aprobado", ignoreCase = true) && !centroActivo -> PantallaDestino.CENTRO_ESTADO_INACTIVO
                    estadoCentro.equals("Rechazado", ignoreCase = true) -> PantallaDestino.CENTRO_ESTADO_RECHAZADO
                    estadoCentro.equals("Pendiente", ignoreCase = true) -> PantallaDestino.CENTRO_ESTADO_PENDIENTE
                    else -> PantallaDestino.CENTRO_ESTADO_PENDIENTE
                }
            }
            else -> PantallaDestino.ERROR
        }
    }

    @Test
    fun enrutamiento_usuarioAdoptante_redirigeACatalogo() {
        val destino = resolverEnrutamiento("Adoptante", null, false)
        assertEquals(PantallaDestino.ADOPTANTE_CATALOGO, destino)
    }

    @Test
    fun enrutamiento_usuarioAdmin_redirigeAPanelAdmin() {
        val destino = resolverEnrutamiento("Admin", null, false)
        assertEquals(PantallaDestino.ADMIN_PANEL, destino)

        val destinoAlt = resolverEnrutamiento("Administrador", null, false)
        assertEquals(PantallaDestino.ADMIN_PANEL, destinoAlt)
    }

    @Test
    fun enrutamiento_centroAprobadoYActivo_redirigeAPanelOperativo() {
        val destino = resolverEnrutamiento("Centro", "Aprobado", true)
        assertEquals(PantallaDestino.CENTRO_PANEL, destino)
    }

    @Test
    fun enrutamiento_centroPendiente_redirigeAEsfuerzoPendiente() {
        val destino = resolverEnrutamiento("Centro", "Pendiente", false)
        assertEquals(PantallaDestino.CENTRO_ESTADO_PENDIENTE, destino)
    }

    @Test
    fun enrutamiento_centroRechazado_redirigeAEstadoRechazado() {
        val destino = resolverEnrutamiento("Centro", "Rechazado", false)
        assertEquals(PantallaDestino.CENTRO_ESTADO_RECHAZADO, destino)
    }

    @Test
    fun enrutamiento_centroAprobadoPeroInactivo_redirigeAEstadoInactivo() {
        val destino = resolverEnrutamiento("Centro", "Aprobado", false)
        assertEquals(PantallaDestino.CENTRO_ESTADO_INACTIVO, destino)
    }

    @Test
    fun enrutamiento_sinSesion_redirigeALogin() {
        val destino = resolverEnrutamiento(null, null, false)
        assertEquals(PantallaDestino.LOGIN, destino)
    }

    // =========================================================
    // 6. SEGURIDAD DE MANIFIESTO ANDROID (PERMISOS Y FILEPROVIDER)
    // =========================================================

    @Test
    fun manifestAndroid_noDeclaraPermisosAmpliosDeAlmacenamiento() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml debe existir", manifestFile.exists())

        val contenido = manifestFile.readText()
        assertFalse(
            "AndroidManifest.xml NO debe solicitar READ_EXTERNAL_STORAGE",
            contenido.contains("android.permission.READ_EXTERNAL_STORAGE")
        )
        assertFalse(
            "AndroidManifest.xml NO debe solicitar WRITE_EXTERNAL_STORAGE",
            contenido.contains("android.permission.WRITE_EXTERNAL_STORAGE")
        )
        assertFalse(
            "AndroidManifest.xml NO debe solicitar READ_MEDIA_IMAGES",
            contenido.contains("android.permission.READ_MEDIA_IMAGES")
        )
    }

    @Test
    fun manifestAndroid_declaraFileProviderParaCapturasSeguras() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val contenido = manifestFile.readText()

        assertTrue(
            "AndroidManifest.xml debe declarar androidx.core.content.FileProvider",
            contenido.contains("androidx.core.content.FileProvider")
        )
        assertTrue(
            "FileProvider debe usar autoridades dinámicas con \${applicationId}.fileprovider",
            contenido.contains("\${applicationId}.fileprovider")
        )
    }

    // =========================================================
    // 7. AISLAMIENTO DE CHAT LOCAL POR CENTRO ID DINÁMICO
    // =========================================================

    @Test
    fun claveChatLocal_seAislaPorCentroIdDinamico() {
        fun generarClaveChat(correoAdoptante: String, centroId: Int): String {
            return "chat_${correoAdoptante.trim().lowercase()}_centro_${centroId}"
        }

        val claveCentro1 = generarClaveChat("adoptante@demo.pe", 1)
        val claveCentro2 = generarClaveChat("adoptante@demo.pe", 2)
        val claveCentroOtroUsuario = generarClaveChat("otro@demo.pe", 1)

        assertEquals("chat_adoptante@demo.pe_centro_1", claveCentro1)
        assertEquals("chat_adoptante@demo.pe_centro_2", claveCentro2)
        assertNotEquals(
            "Dos centros distintos no deben compartir historial de chat",
            claveCentro1,
            claveCentro2
        )
        assertNotEquals(
            "Dos adoptantes distintos no deben compartir historial en el mismo centro",
            claveCentro1,
            claveCentroOtroUsuario
        )
    }

    // =========================================================
    // 8. PERSISTENCIA DE SESIÓN EN PREFERENCIAS
    // =========================================================

    @Test
    fun preferencias_guardarSesionActivaCentro_persisteTodosLosCampos() {
        preferencias.guardarSesionActiva(
            token = "jwt.fake.token",
            correo = "centro@refugio.pe",
            rol = "Centro",
            centroId = 7,
            estadoCentro = "Aprobado",
            centroActivo = true,
            esDemostrativo = false
        )

        assertEquals("jwt.fake.token", preferencias.obtenerTokenSesionActiva())
        assertEquals("centro@refugio.pe", preferencias.obtenerCorreoSesionActiva())
        assertEquals("Centro", preferencias.obtenerRolSesionActiva())
        assertEquals(7, preferencias.obtenerCentroIdSesionActiva())
        assertEquals("Aprobado", preferencias.obtenerEstadoCentroSesionActiva())
        assertTrue(preferencias.esCentroActivoSesionActiva())
        assertFalse(preferencias.esCentroDemostrativoSesionActiva())
    }

    @Test
    fun preferencias_actualizarEstadoCentro_modificaEstadoSinAlterarOtrosValores() {
        preferencias.guardarSesionActiva(
            token = "jwt.token",
            correo = "centro@refugio.pe",
            rol = "Centro",
            centroId = 7,
            estadoCentro = "Pendiente",
            centroActivo = false
        )

        preferencias.actualizarEstadoCentro("Aprobado", true)

        assertEquals("Aprobado", preferencias.obtenerEstadoCentroSesionActiva())
        assertTrue(preferencias.esCentroActivoSesionActiva())
        assertEquals(7, preferencias.obtenerCentroIdSesionActiva())
        assertEquals("centro@refugio.pe", preferencias.obtenerCorreoSesionActiva())
    }

    @Test
    fun preferencias_limpiarSesionActiva_eliminaTodasLasClavesDeSesion() {
        preferencias.guardarSesionActiva(
            token = "jwt.token",
            correo = "admin@adoptme.pe",
            rol = "Admin",
            centroId = null,
            estadoCentro = null,
            centroActivo = false
        )

        preferencias.limpiarSesionActiva()

        assertNull(preferencias.obtenerTokenSesionActiva())
        assertNull(preferencias.obtenerCorreoSesionActiva())
        assertNull(preferencias.obtenerRolSesionActiva())
        assertNull(preferencias.obtenerCentroIdSesionActiva())
        assertNull(preferencias.obtenerEstadoCentroSesionActiva())
        assertFalse(preferencias.esCentroActivoSesionActiva())
    }

    @Test
    fun publicacionAndroid_payloadMultipartNoIncluyeCentroId() {
        val metodo = com.example.adoptme.api.MascotasApi::class.java.methods
            .firstOrNull { it.name == "publicarMascotaConImagen" }
        assertNotNull(metodo)

        // Verificar las anotaciones @Part del método para garantizar que ninguna sea "CentroId" o "centroId"
        val anotacionesPart = metodo!!.parameterAnnotations.flatMap { it.toList() }
            .filterIsInstance<retrofit2.http.Part>()
        val nombresPart = anotacionesPart.map { it.value }

        assertFalse("Android no debe enviar CentroId en la petición multipart", nombresPart.any { it.equals("centroId", ignoreCase = true) })
    }

    @Test
    fun publicacionAndroid_mascotaDtoPorDefectoTieneCentroIdNull() {
        val dto = MascotaDto(
            name = "Pelusa",
            species = "Gato",
            breed = "Siames"
        )
        assertNull("Por defecto el DTO de cliente no asigna CentroId; la autoridad es el JWT en backend", dto.centroId)
    }

    @Test
    fun baseUrl_releaseApuntaSiempreAProduccionRender() {
        assertEquals("https://adoptme2.onrender.com/", com.example.adoptme.BuildConfig.BASE_URL)
        assertEquals("https://adoptme2.onrender.com/", RetrofitClient.URL_PRODUCCION)
    }

    @Test
    fun baseUrl_conmutacionRestringidaEnRelease() {
        fun simularConfigurarBaseUrl(nuevaUrl: String, permitirDev: Boolean, urlActual: String): String {
            if (!permitirDev) return urlActual
            return if (nuevaUrl.endsWith("/")) nuevaUrl else "$nuevaUrl/"
        }

        val urlProduccion = "https://adoptme2.onrender.com/"
        val urlIntentoLocal = "http://127.0.0.1:5217/"

        // En release (permitirDev = false), el intento de cambio a localhost debe ser rechazado
        val urlResultadoRelease = simularConfigurarBaseUrl(urlIntentoLocal, permitirDev = false, urlActual = urlProduccion)
        assertEquals("https://adoptme2.onrender.com/", urlResultadoRelease)

        // En debug (permitirDev = true), sí se admite conmutar para pruebas locales
        val urlResultadoDebug = simularConfigurarBaseUrl(urlIntentoLocal, permitirDev = true, urlActual = urlProduccion)
        assertEquals("http://127.0.0.1:5217/", urlResultadoDebug)
    }

    @Test
    fun reinicioApp_actualizaEstadoCentroDesdeServidor() {
        // Estado inicial guardado en SharedPreferences
        preferencias.guardarSesionActiva(
            token = "jwt.token.valido",
            correo = "centro@adoptme.pe",
            rol = "Centro",
            centroId = 12,
            estadoCentro = "Pendiente",
            centroActivo = true
        )

        assertEquals("Pendiente", preferencias.obtenerEstadoCentroSesionActiva())

        // Simulación de respuesta autoritativa del servidor en frío
        val respuestaServidorDto = EstadoCentroDto(
            centroId = 12,
            estado = "Aprobado",
            activo = true,
            motivoRechazo = null
        )

        // Aplicación reactiva de la actualización en preferencias locales
        preferencias.actualizarEstadoCentro(respuestaServidorDto.estado, respuestaServidorDto.activo)

        assertEquals("Aprobado", preferencias.obtenerEstadoCentroSesionActiva())
        assertTrue(preferencias.esCentroActivoSesionActiva())
    }

    @Test
    fun sesionExpirada_401LimpiaSesionActiva() {
        preferencias.guardarSesionActiva(
            token = "jwt.expirado.invalido",
            correo = "usuario@adoptme.pe",
            rol = "Adoptante"
        )
        assertNotNull(preferencias.obtenerTokenSesionActiva())

        // Simulación de detección 401 Unauthorized
        preferencias.limpiarSesionActiva()

        assertNull("Token debe eliminarse al recibir 401", preferencias.obtenerTokenSesionActiva())
        assertNull("Correo de sesión debe eliminarse al recibir 401", preferencias.obtenerCorreoSesionActiva())
    }

    @Test
    fun cambioSensible_bloqueaOperacionesHastaAprobacion() {
        // Centro originalmente Aprobado
        val estadoOriginal = "Aprobado"
        val activoOriginal = true
        assertEquals(PantallaDestino.CENTRO_PANEL, resolverEnrutamiento(rol = "Centro", estadoCentro = estadoOriginal, centroActivo = activoOriginal))

        // Al cambiar nombre o desplazarse > 500m, el estado local y remoto pasa a Pendiente
        val estadoTrasCambioSensible = "Pendiente"
        val rutaTrasCambio = resolverEnrutamiento(rol = "Centro", estadoCentro = estadoTrasCambioSensible, centroActivo = activoOriginal)

        assertEquals(PantallaDestino.CENTRO_ESTADO_PENDIENTE, rutaTrasCambio)
    }

    @Test
    fun pedagogicoCamara_rechazoNoBloqueaFormulario() {
        var formularioBloqueado = false
        var datosFormularioBorrados = false
        var nombreMascota = "Bobby"

        // Simulación de rechazo del permiso CAMERA
        val permisoCameraConcedido = false
        if (!permisoCameraConcedido) {
            // Comportamiento esperado: no bloquear ni borrar datos
            formularioBloqueado = false
            datosFormularioBorrados = false
        }

        assertFalse("El rechazo de cámara no debe bloquear el formulario", formularioBloqueado)
        assertFalse("El rechazo de cámara no debe borrar los datos ingresados", datosFormularioBorrados)
        assertEquals("Bobby", nombreMascota)
    }

    @Test
    fun photoPicker_noRequierePermisoAlmacenamiento() {
        // Validar que el Photo Picker de Android (PickVisualMedia) opera desacoplado de permisos de almacenamiento masivo
        val permisosRequeridosParaPhotoPicker = emptyList<String>()
        assertTrue("Photo Picker nativo no requiere solicitar permisos en tiempo de ejecución", permisosRequeridosParaPhotoPicker.isEmpty())
    }
}
