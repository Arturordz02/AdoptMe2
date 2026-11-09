package com.example.adoptme

import com.example.adoptme.utils.HaversineUtil
import org.junit.Assert.*
import org.junit.Test

class HaversineUtilTest {

    @Test
    fun calcularDistanciaKm_mismoPunto_retornaCero() {
        val lat = -12.046374
        val lon = -77.030583
        val distancia = HaversineUtil.calcularDistanciaKm(lat, lon, lat, lon)

        assertNotNull(distancia)
        assertEquals(0.0, distancia!!, 0.001)
    }

    @Test
    fun calcularDistanciaKm_puntosConocidosLima_retornaDistanciaEsperada() {
        // Plaza Mayor de Lima (-12.046374, -77.030583)
        // Parque Kennedy, Miraflores (-12.1217, -77.0298)
        val distancia = HaversineUtil.calcularDistanciaKm(-12.046374, -77.030583, -12.1217, -77.0298)

        assertNotNull(distancia)
        // La distancia real en línea recta es aprox. 8.38 km
        assertTrue("La distancia debe rondar entre 8.0 y 8.8 km, fue: $distancia", distancia!! in 8.0..8.8)
    }

    @Test
    fun calcularDistanciaKm_coordenadaNula_retornaNull() {
        assertNull(HaversineUtil.calcularDistanciaKm(null, -77.0, -12.0, -77.0))
        assertNull(HaversineUtil.calcularDistanciaKm(-12.0, null, -12.0, -77.0))
        assertNull(HaversineUtil.calcularDistanciaKm(-12.0, -77.0, null, -77.0))
        assertNull(HaversineUtil.calcularDistanciaKm(-12.0, -77.0, -12.0, null))
    }

    @Test
    fun calcularDistanciaKm_latitudInvalida_retornaNull() {
        assertNull(HaversineUtil.calcularDistanciaKm(95.0, -77.0, -12.0, -77.0))
        assertNull(HaversineUtil.calcularDistanciaKm(-95.0, -77.0, -12.0, -77.0))
        assertNull(HaversineUtil.calcularDistanciaKm(-12.0, -77.0, 91.0, -77.0))
    }

    @Test
    fun calcularDistanciaKm_longitudInvalida_retornaNull() {
        assertNull(HaversineUtil.calcularDistanciaKm(-12.0, 185.0, -12.0, -77.0))
        assertNull(HaversineUtil.calcularDistanciaKm(-12.0, -185.0, -12.0, -77.0))
        assertNull(HaversineUtil.calcularDistanciaKm(-12.0, -77.0, -12.0, 190.0))
    }

    @Test
    fun sonCoordenadasValidas_limitesValidos() {
        assertTrue(HaversineUtil.sonCoordenadasValidas(0.0, 0.0))
        assertTrue(HaversineUtil.sonCoordenadasValidas(90.0, 180.0))
        assertTrue(HaversineUtil.sonCoordenadasValidas(-90.0, -180.0))

        assertFalse(HaversineUtil.sonCoordenadasValidas(90.1, 0.0))
        assertFalse(HaversineUtil.sonCoordenadasValidas(-90.1, 0.0))
        assertFalse(HaversineUtil.sonCoordenadasValidas(0.0, 180.1))
        assertFalse(HaversineUtil.sonCoordenadasValidas(0.0, -180.1))
        assertFalse(HaversineUtil.sonCoordenadasValidas(Double.NaN, 0.0))
    }

    @Test
    fun formatearDistancia_metrosYKilometros() {
        assertEquals("a 500 m", HaversineUtil.formatearDistancia(0.5))
        assertEquals("a 850 m", HaversineUtil.formatearDistancia(0.85))
        assertEquals("a 2.5 km", HaversineUtil.formatearDistancia(2.5))
        assertEquals("Distancia no disponible", HaversineUtil.formatearDistancia(null))
        assertEquals("Distancia no disponible", HaversineUtil.formatearDistancia(-1.0))
    }
}
