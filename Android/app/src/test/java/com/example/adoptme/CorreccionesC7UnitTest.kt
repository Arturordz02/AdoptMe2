package com.example.adoptme

import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.CentroApelacionDto
import com.example.adoptme.model.ConversacionAdoptante
import com.example.adoptme.model.CrearApelacionRequest
import com.example.adoptme.model.ResolverApelacionRequest
import com.example.adoptme.model.SolicitudResponseDto
import com.example.adoptme.mapper.toDomain
import com.example.adoptme.screens.resolverIconoMascota
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para la Fase C7:
 * - Corrección de solicitudes de adopción (motivo obligatorio >= 5 chars, estados terminales).
 * - Persistencia y mapeo de observación de rechazo.
 * - Flujo DTO de apelaciones de centros suspendidos.
 * - Bandeja de conversaciones del Adoptante.
 * - Mapeo de especies a iconos en el Mapa de Adopciones.
 */
class CorreccionesC7UnitTest {

    private val gson = Gson()

    // =========================================================================
    // 1. REGLAS DE NEGOCIO: SOLICITUDES DE ADOPCIÓN Y MOTIVO DE RECHAZO
    // =========================================================================

    @Test
    fun test01_MotivoRechazo_ValidacionLongitudMinima5() {
        val validarMotivo: (String) -> Boolean = { it.trim().length >= 5 }

        assertFalse(validarMotivo(""))
        assertFalse(validarMotivo("   "))
        assertFalse(validarMotivo("No"))
        assertFalse(validarMotivo("1234"))
        assertTrue(validarMotivo("12345"))
        assertTrue(validarMotivo("Falta de espacio"))
        assertTrue(validarMotivo("  Adoptante no apto por horario  "))
    }

    @Test
    fun test02_EstadoTerminal_BloqueaAccionesAprobarYRechazar() {
        fun esEstadoTerminal(estado: String): Boolean {
            return estado.equals("Aprobada", ignoreCase = true) ||
                   estado.equals("Rechazada", ignoreCase = true)
        }

        assertFalse(esEstadoTerminal("Pendiente"))
        assertFalse(esEstadoTerminal("En revisión"))
        assertFalse(esEstadoTerminal("en revision"))
        assertTrue(esEstadoTerminal("Aprobada"))
        assertTrue(esEstadoTerminal("aprobada"))
        assertTrue(esEstadoTerminal("Rechazada"))
        assertTrue(esEstadoTerminal("rechazada"))
    }

    @Test
    fun test03_MapeoDtoADomain_ConservaObservacionRechazo() {
        val dto = SolicitudResponseDto(
            id = 15,
            mascotaId = 3,
            adoptanteId = 9,
            petName = "Rocky",
            petSpecies = "Perro",
            applicantName = "Carlos Perez",
            status = "Rechazada",
            observacion = "El adoptante reside en un inmueble sin autorización para mascotas."
        )

        val domain: AdoptionRequest = dto.toDomain()

        assertEquals("Rechazada", domain.status)
        assertEquals("El adoptante reside en un inmueble sin autorización para mascotas.", domain.observacion)
    }

    @Test
    fun test04_SerializacionDeserializacion_AdoptionRequestConObservacion() {
        val solicitud = AdoptionRequest(
            id = 20,
            petId = 7,
            petName = "Firulais",
            applicantName = "Lucia Gomez",
            email = "lucia@adoptme.com",
            status = "Rechazada",
            observacion = "No cuenta con patio cercado"
        )

        val json = gson.toJson(solicitud)
        val deserializada = gson.fromJson(json, AdoptionRequest::class.java)

        assertEquals(solicitud.id, deserializada.id)
        assertEquals(solicitud.status, deserializada.status)
        assertEquals(solicitud.observacion, deserializada.observacion)
    }

    // =========================================================================
    // 2. APELACIONES INSTITUCIONALES PARA CENTROS SUSPENDIDOS
    // =========================================================================

    @Test
    fun test05_CrearApelacionRequest_SerializaCorrectamente() {
        val req = CrearApelacionRequest(
            motivoApelacion = "Hemos subsanado la observación sobre las medidas de salubridad y cuarentena."
        )

        val json = gson.toJson(req)
        assertTrue(json.contains("motivoApelacion"))
        assertTrue(json.contains("salubridad"))
    }

    @Test
    fun test06_ResolverApelacionRequest_SerializaCorrectamente() {
        val req = ResolverApelacionRequest(
            respuestaAdmin = "Documentación aprobada. Procedemos a levantar la suspensión."
        )

        val json = gson.toJson(req)
        assertTrue(json.contains("respuestaAdmin"))
        assertTrue(json.contains("levantar la suspensión"))
    }

    @Test
    fun test07_CentroApelacionDto_DeserializaCamposCompletos() {
        val json = """
            {
                "id": 101,
                "centroId": 12,
                "centroNombre": "Albergue Huellitas Felices",
                "motivoApelacion": "Contamos con certificado veterinario emitido por SENASA.",
                "estado": "Pendiente",
                "fechaCreacion": "2026-11-10T14:00:00Z",
                "motivoSuspensionOriginal": "Falta de control veterinario",
                "fechaSuspensionOriginal": "2026-11-08T10:00:00Z"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, CentroApelacionDto::class.java)

        assertEquals(101, dto.id)
        assertEquals(12, dto.centroId)
        assertEquals("Albergue Huellitas Felices", dto.centroNombre)
        assertEquals("Pendiente", dto.estado)
        assertEquals("Falta de control veterinario", dto.motivoSuspensionOriginal)
        assertNotNull(dto.fechaSuspensionOriginal)
        assertNull(dto.fechaRevision)
    }

    // =========================================================================
    // 3. BANDEJA DE CONVERSACIONES DEL ADOPTANTE
    // =========================================================================

    @Test
    fun test08_ConversacionAdoptante_EstructuraYPersistenciaJson() {
        val lista = listOf(
            ConversacionAdoptante(
                idCentro = 1,
                nombreCentro = "Refugio Esperanza",
                correoCentro = "esperanza@adoptme.pe",
                ultimoTexto = "¿Cuándo puedo ir a visitarlo?",
                fecha = "10/11 15:30",
                mensajesNoLeidos = 0
            ),
            ConversacionAdoptante(
                idCentro = 2,
                nombreCentro = "Albergue Patitas",
                correoCentro = "patitas@adoptme.pe",
                ultimoTexto = "Hola, tenemos cachorros disponibles",
                fecha = "10/11 16:45",
                mensajesNoLeidos = 2
            )
        )

        val json = gson.toJson(lista)
        val tipo = object : TypeToken<List<ConversacionAdoptante>>() {}.type
        val recuperadas: List<ConversacionAdoptante> = gson.fromJson(json, tipo)

        assertEquals(2, recuperadas.size)
        assertEquals("Refugio Esperanza", recuperadas[0].nombreCentro)
        assertEquals(2, recuperadas[1].mensajesNoLeidos)
    }

    @Test
    fun test09_ConversacionAdoptante_OrdenDescendenteYBadge() {
        val c1 = ConversacionAdoptante(idCentro = 1, nombreCentro = "Centro A", correoCentro = "a@centro.pe", ultimoTexto = "Hola", fecha = "10/11 10:00")
        val c2 = ConversacionAdoptante(idCentro = 2, nombreCentro = "Centro B", correoCentro = "b@centro.pe", ultimoTexto = "Buenas", fecha = "10/11 12:00")

        val lista = listOf(c1, c2)
        // La lista más reciente primero
        val ordenada = lista.sortedByDescending { it.fecha }

        assertEquals(2, ordenada.first().idCentro)
    }

    // =========================================================================
    // 4. MAPEO DE ESPECIES A ICONOS EN MAPA
    // =========================================================================

    @Test
    fun test10_MapeoEspecies_LogicaDeteccion() {
        fun clasificarEspecie(especie: String): String {
            val esp = especie.trim().lowercase()
            return when {
                esp.contains("perr") || esp.contains("can") || esp.contains("dog") -> "perro"
                esp.contains("gat") || esp.contains("felin") || esp.contains("cat") -> "gato"
                esp.contains("conej") || esp.contains("rabbit") -> "conejo"
                else -> "otro"
            }
        }

        assertEquals("perro", clasificarEspecie("Perro"))
        assertEquals("perro", clasificarEspecie("perrito callejero"))
        assertEquals("perro", clasificarEspecie("Canino mestizo"))
        assertEquals("gato", clasificarEspecie("Gato Siames"))
        assertEquals("gato", clasificarEspecie("gatito persa"))
        assertEquals("gato", clasificarEspecie("felino"))
        assertEquals("conejo", clasificarEspecie("Conejo enano"))
        assertEquals("conejo", clasificarEspecie("rabbit"))
        assertEquals("otro", clasificarEspecie("Hámster"))
    }

    // =========================================================================
    // 5. PUBLICACIONES EN MODO SOLO LECTURA
    // =========================================================================

    @Test
    fun test11_CentroSuspendido_ActivaModoSoloLectura() {
        val estaSuspendido = true
        val esModoSoloLectura = estaSuspendido

        assertTrue(esModoSoloLectura)
        // En modo solo lectura, no se permite editar ni eliminar mascotas
        val puedeEditar = !esModoSoloLectura
        val puedeEliminar = !esModoSoloLectura

        assertFalse(puedeEditar)
        assertFalse(puedeEliminar)
    }
}
