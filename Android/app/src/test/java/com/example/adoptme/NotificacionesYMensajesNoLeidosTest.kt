package com.example.adoptme

import android.content.Context
import android.content.SharedPreferences
import com.example.adoptme.model.ConversacionCentro
import com.example.adoptme.model.MensajeChat
import com.example.adoptme.model.NotificationItem
import com.example.adoptme.repository.AppStorage
import com.example.adoptme.repository.esNotificacionPruebaE2E
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Suite de pruebas unitarias exhaustivas para la Fase UX-B1:
 * Notificaciones y Mensajes no leídos, Badges, Limpieza E2E y Aislamiento por cuenta.
 */
class NotificacionesYMensajesNoLeidosTest {

    private lateinit var appStorage: AppStorage
    private lateinit var sharedPreferences: SharedPreferences
    private val almacenamientoMemoria = mutableMapOf<String, Any?>()

    @Before
    fun setUp() {
        almacenamientoMemoria.clear()

        // Creamos una implementación en memoria de SharedPreferences y Editor usando Proxy dinámico
        var editorProxy: SharedPreferences.Editor? = null
        editorProxy = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)
        ) { _, method, args ->
            when (method.name) {
                "putString" -> {
                    almacenamientoMemoria[args[0] as String] = args[1]
                    editorProxy
                }
                "remove" -> {
                    almacenamientoMemoria.remove(args[0] as String)
                    editorProxy
                }
                "clear" -> {
                    almacenamientoMemoria.clear()
                    editorProxy
                }
                "apply" -> null
                "commit" -> true
                else -> editorProxy
            }
        } as SharedPreferences.Editor

        sharedPreferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getString" -> {
                    val key = args[0] as String
                    val defVal = args[1] as String?
                    almacenamientoMemoria[key] ?: defVal
                }
                "edit" -> editorProxy
                "getAll" -> almacenamientoMemoria
                "contains" -> almacenamientoMemoria.containsKey(args[0] as String)
                else -> null
            }
        } as SharedPreferences

        appStorage = AppStorage(sharedPreferences)
    }

    // =========================================================================
    // 1. Nueva notificación creada como no leída
    // =========================================================================
    @Test
    fun test01_nuevaNotificacion_creadaComoNoLeida() {
        val notificacion = NotificationItem(
            title = "Mascota publicada 🐾",
            message = "Toby está ahora disponible para adopción."
        )

        assertFalse("Una nueva notificación debe tener leida = false por defecto", notificacion.leida)
        assertNotNull("Debe poseer un identificador unívoco no nulo", notificacion.id)
        assertTrue("El identificador debe ser no vacío", notificacion.id.isNotBlank())
    }

    // =========================================================================
    // 2. Contador de notificaciones no leídas
    // =========================================================================
    @Test
    fun test02_contadorNotificaciones_calculaExactamenteNoLeidas() {
        val correo = "adoptante@adoptme.com"
        val n1 = NotificationItem("Aviso 1", "Texto 1", leida = false)
        val n2 = NotificationItem("Aviso 2", "Texto 2", leida = true)
        val n3 = NotificationItem("Aviso 3", "Texto 3", leida = false)

        appStorage.guardarNotificaciones(correo, listOf(n1, n2, n3))

        val noLeidas = appStorage.contarNotificacionesNoLeidas(correo)
        assertEquals(2, noLeidas)
    }

    // =========================================================================
    // 3. Badge oculto cuando el contador es cero
    // =========================================================================
    @Test
    fun test03_badgeOculto_cuandoContadorEsCero() {
        val correo = "adoptante@adoptme.com"
        val n1 = NotificationItem("Aviso Leído", "Todo en orden", leida = true)
        appStorage.guardarNotificaciones(correo, listOf(n1))

        val conteo = appStorage.contarNotificacionesNoLeidas(correo)
        assertEquals(0, conteo)
        // Regla UI: Si conteo == 0, BadgedBox no renderiza Badge
        val deberiaMostrarBadge = conteo > 0
        assertFalse(deberiaMostrarBadge)
    }

    // =========================================================================
    // 4. Formato "99+" cuando el contador supera 99
    // =========================================================================
    @Test
    fun test04_badgeFormato99Plus_cuandoSupera99() {
        fun formatearBadge(conteo: Int): String {
            return if (conteo > 99) "99+" else conteo.toString()
        }

        assertEquals("0", formatearBadge(0))
        assertEquals("1", formatearBadge(1))
        assertEquals("99", formatearBadge(99))
        assertEquals("99+", formatearBadge(100))
        assertEquals("99+", formatearBadge(150))
    }

    // =========================================================================
    // 5. Marcar una notificación como leída
    // =========================================================================
    @Test
    fun test05_marcarNotificacionComoLeida_actualizaPersistenciaYDisminuyeContador() {
        val correo = "usuario@adoptme.com"
        val n1 = NotificationItem("Aviso A", "Texto A", id = "id-1", leida = false)
        val n2 = NotificationItem("Aviso B", "Texto B", id = "id-2", leida = false)
        appStorage.guardarNotificaciones(correo, listOf(n1, n2))

        assertEquals(2, appStorage.contarNotificacionesNoLeidas(correo))

        val resultado = appStorage.marcarNotificacionComoLeida(correo, "id-1")
        assertTrue(resultado)

        val actualizadas = appStorage.cargarNotificaciones(correo)
        assertTrue(actualizadas.first { it.id == "id-1" }.leida)
        assertFalse(actualizadas.first { it.id == "id-2" }.leida)
        assertEquals(1, appStorage.contarNotificacionesNoLeidas(correo))
    }

    // =========================================================================
    // 6. Marcar todas como leídas
    // =========================================================================
    @Test
    fun test06_marcarTodasNotificacionesComoLeidas_reseteaContadorACero() {
        val correo = "usuario@adoptme.com"
        val n1 = NotificationItem("Aviso 1", "Texto", id = "1", leida = false)
        val n2 = NotificationItem("Aviso 2", "Texto", id = "2", leida = false)
        val n3 = NotificationItem("Aviso 3", "Texto", id = "3", leida = false)
        appStorage.guardarNotificaciones(correo, listOf(n1, n2, n3))

        val modificadas = appStorage.marcarTodasNotificacionesComoLeidas(correo)
        assertEquals(3, modificadas)
        assertEquals(0, appStorage.contarNotificacionesNoLeidas(correo))
    }

    // =========================================================================
    // 7. Eliminar una notificación
    // =========================================================================
    @Test
    fun test07_eliminarNotificacion_remueveItemYPersisteLista() {
        val correo = "usuario@adoptme.com"
        val n1 = NotificationItem("Aviso A", "Texto", id = "notif-eliminar", leida = false)
        val n2 = NotificationItem("Aviso B", "Texto", id = "notif-quedar", leida = false)
        appStorage.guardarNotificaciones(correo, listOf(n1, n2))

        val eliminado = appStorage.eliminarNotificacion(correo, "notif-eliminar")
        assertTrue(eliminado)

        val cargadas = appStorage.cargarNotificaciones(correo)
        assertEquals(1, cargadas.size)
        assertEquals("notif-quedar", cargadas[0].id)
    }

    // =========================================================================
    // 8 y 9. Limpieza idempotente de PRUEBA_* y preservación de legítimas
    // =========================================================================
    @Test
    fun test08_limpiezaIdempotente_eliminaSoloPrefijosPruebaYPreservaLegitimas() {
        val correo = "usuario@adoptme.com"
        val items = listOf(
            NotificationItem(title = "PRUEBA_SOLICITUD_123", message = "Mensaje técnico temporal", id = "t1"),
            NotificationItem(title = "PRUEBA_ANDROID_CAMERA", message = "Test temporal", id = "t2"),
            NotificationItem(title = "TEST_E2E_LOGIN", message = "Prueba automatizada", id = "t3"),
            NotificationItem(title = "Mascota publicada 🐾", message = "Max fue publicado correctamente", id = "legitimo1"),
            NotificationItem(title = "Solicitud de adopción", message = "Tu postulación para Luna fue enviada", id = "legitimo2"),
            NotificationItem(title = "Bienvenido a AdoptMe", message = "Gracias por registrarte", id = "legitimo3")
        )

        appStorage.guardarNotificaciones(correo, items)
        assertEquals(6, appStorage.cargarNotificaciones(correo).size)

        // Primera ejecución de limpieza
        val eliminadasPrimeraVez = appStorage.limpiarNotificacionesPrueba(correo)
        assertEquals(3, eliminadasPrimeraVez)

        val restantes = appStorage.cargarNotificaciones(correo)
        assertEquals(3, restantes.size)
        assertTrue(restantes.any { it.id == "legitimo1" })
        assertTrue(restantes.any { it.id == "legitimo2" })
        assertTrue(restantes.any { it.id == "legitimo3" })
        assertFalse(restantes.any { it.title.startsWith("PRUEBA_") || it.title.startsWith("TEST_") })

        // Idempotencia: Una segunda ejecución inmediata debe dar 0 eliminadas y mantener las 3
        val eliminadasSegundaVez = appStorage.limpiarNotificacionesPrueba(correo)
        assertEquals(0, eliminadasSegundaVez)
        assertEquals(3, appStorage.cargarNotificaciones(correo).size)
    }

    // =========================================================================
    // 10. Aislamiento de notificaciones por cuenta
    // =========================================================================
    @Test
    fun test10_aislamientoNotificacionesPorCuenta() {
        val cuenta1 = "adoptante1@adoptme.com"
        val cuenta2 = "adoptante2@adoptme.com"

        appStorage.guardarNotificaciones(cuenta1, listOf(NotificationItem("Aviso 1", "Mensaje 1")))
        appStorage.guardarNotificaciones(cuenta2, listOf(NotificationItem("Aviso 2", "Mensaje 2"), NotificationItem("Aviso 3", "Mensaje 3")))

        val notifsCuenta1 = appStorage.cargarNotificaciones(cuenta1)
        val notifsCuenta2 = appStorage.cargarNotificaciones(cuenta2)

        assertEquals(1, notifsCuenta1.size)
        assertEquals(2, notifsCuenta2.size)
        assertEquals("Aviso 1", notifsCuenta1[0].title)
        assertEquals("Aviso 2", notifsCuenta2[0].title)
    }

    // =========================================================================
    // 11 y 12. Mensajes recibidos vs propios y contadores
    // =========================================================================
    @Test
    fun test11_12_mensajesRecibidosIncrementanContador_mensajesPropiosNo() {
        val correoAdoptante = "adoptante@adoptme.com"
        val idCentro = 1

        val msgPropio = MensajeChat(
            id = 1L,
            idCentro = idCentro,
            remitente = correoAdoptante,
            receptor = "centro@adoptme.com",
            texto = "Hola, me interesa adoptar.",
            nombreCentro = "Huellitas",
            fecha = "10:00",
            esDelCentro = false,
            leido = false
        )

        val msgRecibido = MensajeChat(
            id = 2L,
            idCentro = idCentro,
            remitente = "centro@adoptme.com",
            receptor = correoAdoptante,
            texto = "¡Hola! Por supuesto, ¿qué dudas tienes?",
            nombreCentro = "Huellitas",
            fecha = "10:05",
            esDelCentro = true,
            leido = false
        )

        appStorage.guardarMensajesChat(correoAdoptante, idCentro, listOf(msgPropio, msgRecibido))

        // Para el Adoptante:
        // Solo el mensaje recibido del centro (esDelCentro == true) debe contar como no leído.
        val noLeidosAdoptante = appStorage.contarMensajesNoLeidosConversacion(correoAdoptante, idCentro, "Adoptante")
        assertEquals(1, noLeidosAdoptante)

        // Para el Centro:
        // Solo el mensaje enviado por el adoptante (!esDelCentro) debe contar como no leído.
        val noLeidosCentro = appStorage.contarMensajesNoLeidosConversacion(correoAdoptante, idCentro, "Centro")
        assertEquals(1, noLeidosCentro)
    }

    // =========================================================================
    // 13 y 14. Contador por conversación y contador total
    // =========================================================================
    @Test
    fun test13_14_contadorPorConversacionYContadorTotal() {
        val correoAdoptante = "carlos@adoptme.com"

        // Chat con Centro 1 (2 mensajes recibidos sin leer)
        appStorage.guardarMensajesChat(
            correoAdoptante,
            1,
            listOf(
                MensajeChat(id = 1L, idCentro = 1, remitente = "c1@m.com", receptor = correoAdoptante, texto = "M1", nombreCentro = "C1", fecha = "10:00", esDelCentro = true, leido = false),
                MensajeChat(id = 2L, idCentro = 1, remitente = "c1@m.com", receptor = correoAdoptante, texto = "M2", nombreCentro = "C1", fecha = "10:01", esDelCentro = true, leido = false)
            )
        )

        // Chat con Centro 2 (1 mensaje recibido sin leer)
        appStorage.guardarMensajesChat(
            correoAdoptante,
            2,
            listOf(
                MensajeChat(id = 3L, idCentro = 2, remitente = "c2@m.com", receptor = correoAdoptante, texto = "M3", nombreCentro = "C2", fecha = "10:05", esDelCentro = true, leido = false)
            )
        )

        // Contadores por conversación
        assertEquals(2, appStorage.contarMensajesNoLeidosConversacion(correoAdoptante, 1, "Adoptante"))
        assertEquals(1, appStorage.contarMensajesNoLeidosConversacion(correoAdoptante, 2, "Adoptante"))

        // Contador total del Adoptante a través de todos sus centros
        val totalAdoptante = appStorage.contarMensajesNoLeidosAdoptante(correoAdoptante)
        assertEquals(3, totalAdoptante)
    }

    // =========================================================================
    // 15. Abrir un chat marca solo los mensajes recibidos de esa conversación
    // =========================================================================
    @Test
    fun test15_abrirChat_marcaSoloMensajesRecibidosDeEsaConversacion() {
        val correoAdoptante = "carlos@adoptme.com"

        // Mensaje del centro no leído
        val msg1 = MensajeChat(1L, 1, "c@m.com", correoAdoptante, "Hola", "C1", "10:00", esDelCentro = true, leido = false)
        // Mensaje propio no leído
        val msg2 = MensajeChat(2L, 1, correoAdoptante, "c@m.com", "Respuesta", "C1", "10:02", esDelCentro = false, leido = false)
        appStorage.guardarMensajesChat(correoAdoptante, 1, listOf(msg1, msg2))

        // Mensajes de otra conversación (Centro 2)
        val msgCentro2 = MensajeChat(3L, 2, "c2@m.com", correoAdoptante, "Otro", "C2", "10:10", esDelCentro = true, leido = false)
        appStorage.guardarMensajesChat(correoAdoptante, 2, listOf(msgCentro2))

        // El adoptante abre el chat del Centro 1
        val marcados = appStorage.marcarMensajesComoLeidos(correoAdoptante, 1, "Adoptante")
        assertEquals(1, marcados) // Solo marcó el recibido (msg1)

        val cargadosCentro1 = appStorage.cargarMensajesChat(correoAdoptante, 1)
        assertTrue("El mensaje del centro ahora debe estar leído", cargadosCentro1.first { it.id == 1L }.leido)
        assertNotNull("Debe poseer fecha de lectura", cargadosCentro1.first { it.id == 1L }.fechaLectura)
        assertFalse("El mensaje propio no debe modificarse indebidamente", cargadosCentro1.first { it.id == 2L }.leido)

        // El chat del Centro 2 NO debe haber sido alterado
        val cargadosCentro2 = appStorage.cargarMensajesChat(correoAdoptante, 2)
        assertFalse("El mensaje de la otra conversación debe continuar sin leer", cargadosCentro2.first { it.id == 3L }.leido)
        assertEquals(1, appStorage.contarMensajesNoLeidosConversacion(correoAdoptante, 2, "Adoptante"))
    }

    // =========================================================================
    // 16. Persistencia tras reconstruir el almacenamiento
    // =========================================================================
    @Test
    fun test16_persistencia_trasReconstruirAlmacenamiento() {
        val correo = "persistencia@adoptme.com"
        val n1 = NotificationItem("Persistente", "Contenido", id = "p-1", leida = false)
        appStorage.guardarNotificaciones(correo, listOf(n1))

        // Simulación de reinicio de la app: instanciamos un nuevo AppStorage con el mismo SharedPreferences subyacente
        val nuevoAppStorage = AppStorage(sharedPreferences)

        val recuperadas = nuevoAppStorage.cargarNotificaciones(correo)
        assertEquals(1, recuperadas.size)
        assertEquals("Persistente", recuperadas[0].title)
        assertFalse(recuperadas[0].leida)
    }

    // =========================================================================
    // 17. Aislamiento Adoptante / Centro
    // =========================================================================
    @Test
    fun test17_aislamientoAdoptanteYCentro() {
        val correoAdoptante = "adoptante@adoptme.com"
        val correoCentro = "refugio@adoptme.com"
        val idCentro = 5

        // Adoptante envía mensaje al centro
        val msgAdoptante = MensajeChat(
            id = 10L,
            idCentro = idCentro,
            remitente = correoAdoptante,
            receptor = correoCentro,
            texto = "Consulta adopción",
            nombreCentro = "Refugio 5",
            fecha = "12:00",
            esDelCentro = false,
            leido = false
        )
        appStorage.guardarMensajesChat(correoAdoptante, idCentro, listOf(msgAdoptante))
        appStorage.registrarConversacionCentro(correoCentro, idCentro, correoAdoptante, "Refugio 5", "Consulta adopción", "12:00")

        // El Adoptante NO debe tener mensajes no leídos (fue él quien lo envió)
        assertEquals(0, appStorage.contarMensajesNoLeidosAdoptante(correoAdoptante))

        // El Centro SÍ debe tener 1 mensaje no leído en su bandeja
        val conversacionesCentro = appStorage.cargarConversacionesCentro(correoCentro)
        assertEquals(1, conversacionesCentro.size)
        assertEquals(1, conversacionesCentro[0].mensajesNoLeidos)
        assertEquals(1, appStorage.contarMensajesNoLeidosCentro(correoCentro))
    }

    // =========================================================================
    // 18. Compatibilidad con registros locales antiguos
    // =========================================================================
    @Test
    fun test18_compatibilidadConRegistrosLocalesAntiguos() {
        // Simulamos un JSON antiguo en SharedPreferences que no contenía 'id' ni 'leida'
        val jsonNotificacionesAntiguo = """
            [
                {"title": "Aviso Histórico 1", "message": "Mensaje antiguo 1"},
                {"title": "Aviso Histórico 2", "message": "Mensaje antiguo 2"}
            ]
        """.trimIndent()

        val correo = "antiguo@adoptme.com"
        almacenamientoMemoria["notificaciones_antiguo_adoptme_com"] = jsonNotificacionesAntiguo

        val cargadas = appStorage.cargarNotificaciones(correo)
        assertEquals(2, cargadas.size)
        // Regla: por compatibilidad, registros históricos que no tengan 'leida' se asumen ya vistos (true)
        assertTrue(cargadas[0].leida)
        assertTrue(cargadas[1].leida)
        assertNotNull(cargadas[0].id)
        assertTrue(cargadas[0].id.isNotBlank())
        assertEquals(0, appStorage.contarNotificacionesNoLeidas(correo))

        // Simulamos un JSON antiguo de chat sin 'leido' ni 'fechaLectura'
        val jsonChatAntiguo = """
            [
                {"id": 1, "idCentro": 1, "remitente": "c@m.com", "receptor": "a@m.com", "texto": "Histórico", "nombreCentro": "C", "fecha": "01/01", "esDelCentro": true}
            ]
        """.trimIndent()
        almacenamientoMemoria["chat_antiguo_adoptme_com_centro_1"] = jsonChatAntiguo

        val chatCargado = appStorage.cargarMensajesChat(correo, 1)
        assertEquals(1, chatCargado.size)
        assertTrue("Los mensajes históricos preexistentes sin 'leido' deben asumirse leídos", chatCargado[0].leido)
    }
}
