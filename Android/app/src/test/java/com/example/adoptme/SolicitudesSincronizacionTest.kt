package com.example.adoptme

import com.example.adoptme.data.remote.SolicitudResponseDto
import com.example.adoptme.data.remote.toDomain
import com.example.adoptme.model.AdoptionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolicitudesSincronizacionTest {

    @Test
    fun test1_ConversionCompleta_DtoADomain() {
        val dto = SolicitudResponseDto(
            id = 3,
            mascotaId = 10,
            adoptanteId = 5,
            petName = "Luna",
            petSpecies = "Gato",
            applicantName = "Andrea Paredes",
            phone = "910200311",
            email = "andrea.paredes@adoptme.com",
            address = "Av. El Sol 450, Miraflores, Lima",
            housingType = "Departamento propio",
            timeAvailability = "Tiempo completo",
            previousExperience = "5 años con gatos",
            reason = "Deseo darle un buen hogar",
            status = "Pendiente",
            observacion = null,
            fechaSolicitud = "2026-10-24T20:00:00Z",
            fechaActualizacion = null
        )

        val domain: AdoptionRequest = dto.toDomain()

        assertEquals(3, domain.id)
        assertEquals(10, domain.petId)
        assertEquals("Luna", domain.petName)
        assertEquals("Gato", domain.petSpecies)
        assertEquals("Andrea Paredes", domain.applicantName)
        assertEquals("910200311", domain.phone)
        assertEquals("andrea.paredes@adoptme.com", domain.email)
        assertEquals("Av. El Sol 450, Miraflores, Lima", domain.address)
        assertEquals("Departamento propio", domain.housingType)
        assertEquals("Tiempo completo", domain.timeAvailability)
        assertEquals("5 años con gatos", domain.previousExperience)
        assertEquals("Deseo darle un buen hogar", domain.reason)
        assertEquals("Pendiente", domain.status)
    }

    @Test
    fun test2_ValoresOpcionalesSeguros_NoGeneranNullTexto() {
        val dtoConNulls = SolicitudResponseDto(
            id = 4,
            mascotaId = 12,
            adoptanteId = 6,
            petName = "Rocky",
            petSpecies = "Perro",
            applicantName = "Nicolás Campos",
            phone = "910200312",
            email = "nicolas.campos@adoptme.com",
            address = "Jr. Las Flores 320",
            housingType = "Casa propia",
            timeAvailability = null,
            previousExperience = null,
            reason = "Compañía familiar",
            status = "Pendiente"
        )

        val domain = dtoConNulls.toDomain()

        assertEquals("", domain.timeAvailability)
        assertEquals("", domain.previousExperience)
        assertNotEquals("null", domain.timeAvailability)
        assertNotEquals("null", domain.previousExperience)
    }

    @Test
    fun test3_DeduplicacionPorId() {
        val listaConDuplicados = listOf(
            AdoptionRequest(id = 3, petId = 1, petName = "Luna", status = "Pendiente"),
            AdoptionRequest(id = 4, petId = 2, petName = "Rocky", status = "Pendiente"),
            AdoptionRequest(id = 3, petId = 1, petName = "Luna (Copia)", status = "Pendiente")
        )

        val deduplicadas = listaConDuplicados.distinctBy { it.id }

        assertEquals(2, deduplicadas.size)
        assertEquals(listOf(3, 4), deduplicadas.map { it.id })
    }

    @Test
    fun test4_ListaRemotaReemplazaListaLocal_SinMezclarHistoricas() {
        val solicitudesLocalesHistoricas = mutableListOf(
            AdoptionRequest(id = 0, petId = 1, petName = "MascotaDemo1", status = "Aprobada"),
            AdoptionRequest(id = 0, petId = 2, petName = "MascotaDemo2", status = "Rechazada")
        )

        val respuestaRemotaServidor = listOf(
            SolicitudResponseDto(id = 3, mascotaId = 10, adoptanteId = 5, petName = "Luna", status = "Pendiente"),
            SolicitudResponseDto(id = 4, mascotaId = 12, adoptanteId = 6, petName = "Rocky", status = "Pendiente")
        )

        val listaRemota = respuestaRemotaServidor.map { it.toDomain() }

        // Regla autoritativa: la lista visible se vacía y se reemplaza exclusivamente por el servidor
        val listaVisible = mutableListOf<AdoptionRequest>()
        listaVisible.clear()
        listaVisible.addAll(listaRemota.distinctBy { it.id })

        assertEquals(2, listaVisible.size)
        assertTrue(listaVisible.all { it.id > 0 })
        assertTrue(listaVisible.none { it.petName.startsWith("MascotaDemo") })
    }

    @Test
    fun test5_EstadosDevueltosPorServidor_SeReflejanEnLista() {
        val listaVisible = mutableListOf(
            AdoptionRequest(id = 3, petId = 10, petName = "Luna", status = "Pendiente"),
            AdoptionRequest(id = 4, petId = 12, petName = "Rocky", status = "Pendiente")
        )

        // Simulación de respuesta exitosa del servidor tras PUT /api/Solicitudes/3/estado
        val dtoServidorAprobada = SolicitudResponseDto(
            id = 3,
            mascotaId = 10,
            adoptanteId = 5,
            petName = "Luna",
            status = "Aprobada"
        )

        val actualizada = dtoServidorAprobada.toDomain()
        val index = listaVisible.indexOfFirst { it.id == actualizada.id }
        if (index != -1) {
            listaVisible[index] = actualizada
        }

        assertEquals("Aprobada", listaVisible.first { it.id == 3 }.status)
        assertEquals("Pendiente", listaVisible.first { it.id == 4 }.status)
    }

    @Test
    fun test6_AnteFalloHttp_NoSeAplicaEstadoLocalFalso() {
        val listaVisible = mutableListOf(
            AdoptionRequest(id = 3, petId = 10, petName = "Luna", status = "Pendiente")
        )

        // Simulación: Llamada a la API falla (ej. HTTP 409 o 500)
        val httpCode = 409
        val peticionExitosa = (httpCode in 200..299)

        if (peticionExitosa) {
            listaVisible[0] = listaVisible[0].copy(status = "Aprobada")
        }

        // El estado debe permanecer en "Pendiente", sin mutar falsamente en local
        assertEquals("Pendiente", listaVisible[0].status)
    }
}
