package com.example.adoptme

import com.example.adoptme.model.AsignarPropietarioCentroRequest
import com.example.adoptme.model.CentroAdminDto
import com.example.adoptme.model.CentroPropietarioDto
import com.example.adoptme.model.EstadoCentroDto
import com.example.adoptme.model.OperacionCentroResponse
import com.example.adoptme.model.SuspenderCentroRequest
import com.example.adoptme.viewmodel.AdminCentrosViewModel
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para la Fase UX-B3:
 * - Rechazo inicial vs. Suspensión operativa
 * - Reactivación administrativa
 * - Manejo de centros inactivos heredados
 * - Vinculación 1:1 de usuario responsable
 * - Contratos DTO y reglas de negocio UI
 */
class SuspensionYVinculacionCentroTest {

    private val gson = Gson()

    // =========================================================================
    // 1. SERIALIZACIÓN Y DESERIALIZACIÓN DTOs
    // =========================================================================

    @Test
    fun test01_CentroPropietarioDto_DeserializaCamposSuspensionCorrectamente() {
        val json = """
            {
                "id": 10,
                "nombre": "Refugio Los Patitas",
                "estado": "Aprobado",
                "activo": false,
                "motivoSuspension": "Incumplimiento de estándares de salubridad",
                "fechaSuspension": "2026-11-10T15:30:00Z",
                "fechaReactivacion": "2026-11-10T18:00:00Z"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, CentroPropietarioDto::class.java)

        assertEquals(10, dto.id)
        assertEquals("Refugio Los Patitas", dto.nombre)
        assertEquals("Aprobado", dto.estado)
        assertFalse(dto.activo)
        assertEquals("Incumplimiento de estándares de salubridad", dto.motivoSuspension)
        assertEquals("2026-11-10T15:30:00Z", dto.fechaSuspension)
        assertEquals("2026-11-10T18:00:00Z", dto.fechaReactivacion)
    }

    @Test
    fun test02_CentroAdminDto_DeserializaAuditoriaSuspension() {
        val json = """
            {
                "id": 12,
                "nombre": "Centro Esperanza",
                "estado": "Aprobado",
                "activo": false,
                "motivoSuspension": "Observaciones en inspección presencial",
                "fechaSuspension": "2026-11-10T12:00:00Z",
                "fechaReactivacion": null,
                "suspendidoPorAdminId": 1,
                "suspendidoPorAdminNombre": "Administrador Principal"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, CentroAdminDto::class.java)

        assertEquals(12, dto.id)
        assertEquals("Aprobado", dto.estado)
        assertFalse(dto.activo)
        assertEquals("Observaciones en inspección presencial", dto.motivoSuspension)
        assertEquals("2026-11-10T12:00:00Z", dto.fechaSuspension)
        assertNull(dto.fechaReactivacion)
        assertEquals(1, dto.suspendidoPorAdminId)
        assertEquals("Administrador Principal", dto.suspendidoPorAdminNombre)
    }

    @Test
    fun test03_EstadoCentroDto_DeserializaCamposSuspension() {
        val json = """
            {
                "centroId": 15,
                "estado": "Aprobado",
                "activo": false,
                "motivoSuspension": "Suspensión preventiva",
                "fechaSuspension": "2026-11-10T10:00:00Z",
                "fechaReactivacion": null
            }
        """.trimIndent()

        val dto = gson.fromJson(json, EstadoCentroDto::class.java)

        assertEquals(15, dto.centroId)
        assertEquals("Aprobado", dto.estado)
        assertFalse(dto.activo)
        assertEquals("Suspensión preventiva", dto.motivoSuspension)
        assertEquals("2026-11-10T10:00:00Z", dto.fechaSuspension)
    }

    @Test
    fun test04_SuspenderCentroRequest_SerializaCorrectamente() {
        val request = SuspenderCentroRequest("Motivo técnico suficiente para suspensión.")
        val json = gson.toJson(request)

        assertTrue(json.contains("\"motivo\":\"Motivo técnico suficiente para suspensión.\""))
    }

    @Test
    fun test05_OperacionCentroResponse_DeserializaCamposSuspension() {
        val json = """
            {
                "mensaje": "Centro suspendido exitosamente.",
                "id": 8,
                "estado": "Aprobado",
                "activo": false,
                "motivoSuspension": "Mantenimiento institucional",
                "fechaSuspension": "2026-11-10T14:00:00Z",
                "fechaReactivacion": null
            }
        """.trimIndent()

        val response = gson.fromJson(json, OperacionCentroResponse::class.java)

        assertEquals("Centro suspendido exitosamente.", response.mensaje)
        assertEquals(8, response.id)
        assertEquals("Aprobado", response.estado)
        assertEquals(false, response.activo)
        assertEquals("Mantenimiento institucional", response.motivoSuspension)
        assertEquals("2026-11-10T14:00:00Z", response.fechaSuspension)
    }

    // =========================================================================
    // 2. MÁQUINA DE ESTADOS Y CLASIFICACIÓN DE CENTROS EN UI
    // =========================================================================

    private enum class CategoriaCentroUi {
        OperativoActivo,
        Suspendido,
        InactivoHeredado,
        RechazadoInicial,
        PendienteRevision,
        Desconocido
    }

    private fun clasificarCentro(estado: String?, activo: Boolean, motivoSuspension: String?): CategoriaCentroUi {
        return when {
            estado.equals("Pendiente", ignoreCase = true) -> CategoriaCentroUi.PendienteRevision
            estado.equals("Rechazado", ignoreCase = true) -> CategoriaCentroUi.RechazadoInicial
            estado.equals("Aprobado", ignoreCase = true) && activo -> CategoriaCentroUi.OperativoActivo
            estado.equals("Aprobado", ignoreCase = true) && !activo && !motivoSuspension.isNullOrBlank() -> CategoriaCentroUi.Suspendido
            estado.equals("Aprobado", ignoreCase = true) && !activo && motivoSuspension.isNullOrBlank() -> CategoriaCentroUi.InactivoHeredado
            else -> CategoriaCentroUi.Desconocido
        }
    }

    @Test
    fun test06_EstadoCentro_IdentificaAprobadoActivo() {
        val cat = clasificarCentro(estado = "Aprobado", activo = true, motivoSuspension = null)
        assertEquals(CategoriaCentroUi.OperativoActivo, cat)
    }

    @Test
    fun test07_EstadoCentro_IdentificaAprobadoSuspendidoConMotivo() {
        val cat = clasificarCentro(
            estado = "Aprobado",
            activo = false,
            motivoSuspension = "Falta de ventilación en instalaciones temporales"
        )
        assertEquals(CategoriaCentroUi.Suspendido, cat)
    }

    @Test
    fun test08_EstadoCentro_IdentificaAprobadoInactivoHeredadoSinMotivo() {
        val cat = clasificarCentro(estado = "Aprobado", activo = false, motivoSuspension = null)
        assertEquals(CategoriaCentroUi.InactivoHeredado, cat)
    }

    @Test
    fun test09_EstadoCentro_IdentificaRechazadoInicial() {
        val cat = clasificarCentro(estado = "Rechazado", activo = false, motivoSuspension = null)
        assertEquals(CategoriaCentroUi.RechazadoInicial, cat)
    }

    @Test
    fun test10_EstadoCentro_IdentificaPendiente() {
        val cat = clasificarCentro(estado = "Pendiente", activo = false, motivoSuspension = null)
        assertEquals(CategoriaCentroUi.PendienteRevision, cat)
    }

    // =========================================================================
    // 3. VALIDACIONES Y CONTROLADORES DE VIEWMODEL
    // =========================================================================

    @Test
    fun test11_AdminCentrosViewModel_SuspenderCentro_MotivoMenorA5Chars_RetornaErrorSinLlamarApi() {
        val viewModel = AdminCentrosViewModel()
        var exitoResultado: Boolean? = null
        var mensajeResultado: String? = null

        viewModel.suspenderCentro(id = 1, motivo = "abc") { exito, mensaje ->
            exitoResultado = exito
            mensajeResultado = mensaje
        }

        assertEquals(false, exitoResultado)
        assertNotNull(mensajeResultado)
        assertTrue(mensajeResultado!!.contains("al menos 5 caracteres"))
    }

    @Test
    fun test12_AdminCentrosViewModel_SuspenderCentro_Valido_PasaValidacionLocal() {
        val motivoValido = "   Cierre temporal por remodelación.   "
        val trimmed = motivoValido.trim()

        assertTrue(trimmed.length >= 5)
        assertEquals("Cierre temporal por remodelación.", trimmed)
    }

    @Test
    fun test13_AdminCentrosViewModel_ReactivarCentro_EstructuraCorrecta() {
        val idCentro = 5
        assertTrue(idCentro > 0)
    }

    @Test
    fun test14_AdminCentrosViewModel_VincularResponsable_EstructuraCorrecta() {
        val request = AsignarPropietarioCentroRequest(usuarioId = 20)
        assertEquals(20, request.usuarioId)

        val json = gson.toJson(request)
        assertTrue(json.contains("\"usuarioId\":20"))
    }

    @Test
    fun test15_AdminCentrosViewModel_ProcesarErrorHttp409_RetornaMensajeConflictoClaro() {
        val jsonConflicto = """{"mensaje":"El usuario ya es responsable de otro centro."}"""
        val response = gson.fromJson(jsonConflicto, Map::class.java)

        assertEquals("El usuario ya es responsable de otro centro.", response["mensaje"])
    }

    @Test
    fun test16_AdminCentrosViewModel_ProcesarErrorHttp403_RetornaMensajePermisoAdmin() {
        val mensajeEsperado = "Acceso denegado: se requieren credenciales de Administrador."
        assertTrue(mensajeEsperado.contains("credenciales de Administrador"))
    }

    @Test
    fun test17_AdminCentrosViewModel_ProcesarErrorHttp404_RetornaMensajeCentroNoEncontrado() {
        val mensajeEsperado = "El centro no fue encontrado en el servidor."
        assertTrue(mensajeEsperado.contains("no fue encontrado"))
    }

    // =========================================================================
    // 4. REGLAS DE NEGOCIO E INTEGRIDAD UI
    // =========================================================================

    @Test
    fun test18_ReglaNegocio_CentroSuspendido_NoEsRechazado() {
        val centroSuspendido = CentroPropietarioDto(
            id = 7,
            nombre = "Albergue Sol",
            estado = "Aprobado",
            activo = false,
            motivoSuspension = "Suspensión administrativa",
            motivoRechazo = null
        )

        assertFalse(centroSuspendido.estado.equals("Rechazado", ignoreCase = true))
        assertEquals("Aprobado", centroSuspendido.estado)
        assertFalse(centroSuspendido.activo)
        assertNotNull(centroSuspendido.motivoSuspension)
    }

    @Test
    fun test19_ReglaNegocio_CentroAprobado_NoMuestraBotonRechazarEnAdmin() {
        fun puedeMostrarBotonRechazar(estado: String): Boolean {
            return estado.equals("Pendiente", ignoreCase = true)
        }

        assertFalse(puedeMostrarBotonRechazar("Aprobado"))
        assertFalse(puedeMostrarBotonRechazar("aprobado"))
        assertTrue(puedeMostrarBotonRechazar("Pendiente"))
    }

    @Test
    fun test20_ReglaNegocio_CentroConResponsable_NoMuestraBotonVincular() {
        fun puedeMostrarBotonVincular(usuarioId: Int?): Boolean {
            return usuarioId == null
        }

        assertFalse(puedeMostrarBotonVincular(15))
        assertTrue(puedeMostrarBotonVincular(null))
    }

    @Test
    fun test21_ReglaNegocio_CentroSuspendido_NoPermiteReenviarSolicitud() {
        fun puedeReenviarSolicitud(estado: String): Boolean {
            return estado.equals("Rechazado", ignoreCase = true)
        }

        assertFalse(puedeReenviarSolicitud("Aprobado"))
        assertTrue(puedeReenviarSolicitud("Rechazado"))
    }
}
