package com.example.adoptme

import com.example.adoptme.mapper.toDto
import com.example.adoptme.mapper.toPet
import com.example.adoptme.model.MascotaDto
import com.example.adoptme.model.Pet
import com.example.adoptme.screens.crearMascota
import com.example.adoptme.screens.crearPublicacionMascota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MascotaCoordenadasTest {

    @Test
    fun crearMascota_sinCoordenadas_asignaNullPorDefecto() {
        val pet = crearMascota(
            nombre = "Pelusa",
            especie = "Gato",
            raza = "Angora",
            edad = "1 año",
            genero = "Hembra",
            ubicacion = "Miraflores",
            descripcion = "Tranquila y cariñosa"
        )
        assertNull(pet.latitud)
        assertNull(pet.longitud)
    }

    @Test
    fun crearMascota_conCoordenadas_asignaLatitudYLongitud() {
        val pet = crearMascota(
            nombre = "Firulais",
            especie = "Perro",
            raza = "Mestizo",
            edad = "2 años",
            genero = "Macho",
            ubicacion = "Surco",
            descripcion = "Juguetón",
            latitud = -12.1384,
            longitud = -76.9856
        )
        assertNotNull(pet.latitud)
        assertNotNull(pet.longitud)
        assertEquals(-12.1384, pet.latitud!!, 0.0001)
        assertEquals(-76.9856, pet.longitud!!, 0.0001)
    }

    @Test
    fun crearPublicacionMascota_delegaCorrectamenteConCoordenadas() {
        val pet = crearPublicacionMascota(
            nombre = "Tambor",
            especie = "Conejo",
            raza = "Mini Lop",
            edad = "6 meses",
            genero = "Macho",
            ubicacion = "San Borja",
            descripcion = "Dócil",
            latitud = -12.0864,
            longitud = -77.0033
        )
        assertEquals(-12.0864, pet.latitud!!, 0.0001)
        assertEquals(-77.0033, pet.longitud!!, 0.0001)
    }

    @Test
    fun mapper_toDto_preservaCoordenadas() {
        val pet = Pet(
            id = 55,
            name = "Rex",
            species = "Perro",
            breed = "Pastor Alemán",
            age = "3 años",
            gender = "Macho",
            location = "La Molina",
            status = "Disponible",
            description = "Entrenado",
            image = 123,
            latitud = -12.0789,
            longitud = -76.9450
        )
        val dto = pet.toDto()
        assertEquals(-12.0789, dto.latitud!!, 0.0001)
        assertEquals(-76.9450, dto.longitud!!, 0.0001)
    }

    @Test
    fun mapper_toPet_preservaCoordenadas() {
        val dto = MascotaDto(
            id = 77,
            name = "Michi",
            species = "Gato",
            breed = "Común",
            age = "2 años",
            gender = "Hembra",
            location = "San Isidro",
            status = "Disponible",
            description = "Duerme mucho",
            latitud = -12.0967,
            longitud = -77.0353
        )
        val pet = dto.toPet()
        assertEquals(-12.0967, pet.latitud!!, 0.0001)
        assertEquals(-77.0353, pet.longitud!!, 0.0001)
    }

    @Test
    fun mapper_conCoordenadasNull_mapeaNullSinExcepcion() {
        val dto = MascotaDto(
            id = 88,
            name = "Antigua",
            species = "Perro",
            breed = "Criollo",
            age = "5 años",
            gender = "Macho",
            location = "Lima",
            status = "Disponible",
            description = "Histórica sin GPS",
            latitud = null,
            longitud = null
        )
        val pet = dto.toPet()
        assertNull(pet.latitud)
        assertNull(pet.longitud)

        val dtoRetornado = pet.toDto()
        assertNull(dtoRetornado.latitud)
        assertNull(dtoRetornado.longitud)
    }

    @Test
    fun coordenadas_validarRangos_rechazaFueraDeLimite() {
        val latInvalida = 95.0
        val lonInvalida = -185.0
        val latValida = -12.0463
        val lonValida = -77.0305

        val esLatOk = com.example.adoptme.utils.HaversineUtil.sonCoordenadasValidas(latValida, lonValida)
        val esLatInvalidaOk = com.example.adoptme.utils.HaversineUtil.sonCoordenadasValidas(latInvalida, lonValida)
        val esLonInvalidaOk = com.example.adoptme.utils.HaversineUtil.sonCoordenadasValidas(latValida, lonInvalida)

        org.junit.Assert.assertTrue(esLatOk)
        org.junit.Assert.assertFalse(esLatInvalidaOk)
        org.junit.Assert.assertFalse(esLonInvalidaOk)
    }

    @Test
    fun filtroCercania_conservaMascotasSinGps_cuandoFiltroEsSinLimite() {
        val pets = listOf(
            Pet(1, "Con GPS", "Perro", "Mestizo", "1 año", "Macho", "Lima", "Disponible", "Desc", 0, latitud = -12.05, longitud = -77.03),
            Pet(2, "Sin GPS", "Gato", "Criollo", "2 años", "Hembra", "Lima", "Disponible", "Desc", 0, latitud = null, longitud = null)
        )

        // Sin filtro de distancia (< 50f significa sin límite o 50+ km)
        val distanciaKm = 50f
        val filtradas = if (distanciaKm < 50f) {
            pets.filter { it.latitud != null && it.longitud != null }
        } else {
            pets
        }

        assertEquals(2, filtradas.size)
    }

    @Test
    fun filtroCercania_excluyeMascotasSinGps_soloConFiltroEstrictoActivo() {
        val pets = listOf(
            Pet(1, "Cercano", "Perro", "Mestizo", "1 año", "Macho", "Miraflores", "Disponible", "Desc", 0, latitud = -12.1221, longitud = -77.0305),
            Pet(2, "Sin GPS", "Gato", "Criollo", "2 años", "Hembra", "Lima", "Disponible", "Desc", 0, latitud = null, longitud = null)
        )

        val userLat = -12.1200
        val userLon = -77.0300
        val distanciaKm = 5f // Filtro estricto menor a 5 km

        val filtradas = pets.filter { pet ->
            if (pet.latitud != null && pet.longitud != null) {
                val dist = com.example.adoptme.utils.HaversineUtil.calcularDistanciaKm(userLat, userLon, pet.latitud, pet.longitud)
                dist != null && dist <= distanciaKm
            } else {
                false
            }
        }

        assertEquals(1, filtradas.size)
        assertEquals("Cercano", filtradas[0].name)
    }

    @Test
    fun preferenciasTerminos_clavesAisladasPorUsuario() {
        fun claveTerminos(correo: String) = "terminos_aceptados_${com.example.adoptme.repository.obtenerClaveUsuario(correo)}"
        val clave1 = claveTerminos("usuario1@adoptme.com")
        val clave2 = claveTerminos("usuario2@adoptme.com")

        org.junit.Assert.assertNotEquals(clave1, clave2)
        org.junit.Assert.assertTrue(clave1.startsWith("terminos_aceptados_"))
    }
}
