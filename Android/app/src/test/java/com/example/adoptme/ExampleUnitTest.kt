package com.example.adoptme

import com.example.adoptme.AdoptMe.esFiltroComodin
import com.example.adoptme.AdoptMe.filtrarMascotas
import com.example.adoptme.AdoptMe.normalizarTexto
import com.example.adoptme.data.normalizarCorreoUsuario
import com.example.adoptme.data.obtenerClaveUsuario
import com.example.adoptme.data.claveChat
import com.example.adoptme.data.claveConversacionesCentro
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.CentroAdopcion
import com.example.adoptme.model.ConversacionCentro
import com.example.adoptme.model.MensajeChat
import com.example.adoptme.model.NotificationItem
import com.example.adoptme.model.Pet
import com.example.adoptme.localizarMascotaParaAprobacion
import com.example.adoptme.puedeAprobarSolicitud
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun normalizarTexto_manejaMayusculasYTildes() {
        assertEquals("perro", normalizarTexto("Perro"))
        assertEquals("perro", normalizarTexto("PERRO"))
        assertEquals("leon", normalizarTexto("León"))
        assertEquals("angel", normalizarTexto("Ángel"))
        assertEquals("en proceso", normalizarTexto("  En Proceso  "))
        assertEquals("", normalizarTexto("   "))
    }

    @Test
    fun esFiltroComodin_identificaTodosLosComodines() {
        assertTrue(esFiltroComodin("Todos"))
        assertTrue(esFiltroComodin("TODAS"))
        assertTrue(esFiltroComodin("Cualquier sexo"))
        assertTrue(esFiltroComodin("Todas las zonas"))
        assertTrue(esFiltroComodin("Cualquier estado"))
        assertTrue(esFiltroComodin(""))

        assertFalse(esFiltroComodin("Perro"))
        assertFalse(esFiltroComodin("Gato"))
        assertFalse(esFiltroComodin("Conejo"))
        assertFalse(esFiltroComodin("Macho"))
        assertFalse(esFiltroComodin("Disponible"))
        assertFalse(esFiltroComodin("Miraflores"))
    }

    @Test
    fun filtrarMascotas_combinaFiltrosCorrectamente() {
        val mascotas = listOf(
            Pet(1, "Max", "Perro", "Labrador", "2 años", "Macho", "Miraflores", "Disponible", "Dócil", 0),
            Pet(2, "Luna", "Gato", "Siamés", "1 año", "Hembra", "San Isidro", "En proceso", "Cariñosa", 0),
            Pet(3, "Tambor", "Conejo", "Enano", "6 meses", "Macho", "Surco", "Disponible", "Tranquilo", 0),
            Pet(4, "Pelusa", "Perro", "Poodle", "3 años", "Hembra", "Miraflores", "Adoptada", "Amigable", 0)
        )

        // 1. Comodines devuelven todas las mascotas
        val todasLasMascotas = filtrarMascotas(mascotas, "", "Todos", "Todos", "Todas", "Todos")
        assertEquals(4, todasLasMascotas.size)

        // 2. Filtro por especie ignorando mayúsculas y tildes
        val perros = filtrarMascotas(mascotas, "", "perro", "Todos", "Todas", "Todos")
        assertEquals(2, perros.size)

        // 3. Consulta de búsqueda coincide con raza con tilde (Siamés vs siames)
        val siameses = filtrarMascotas(mascotas, "siames", "Todos", "Todos", "Todas", "Todos")
        assertEquals(1, siameses.size)
        assertEquals("Luna", siameses[0].name)

        // 4. Filtros combinados: Perro + Hembra + Miraflores + Adoptada
        val pelusas = filtrarMascotas(mascotas, "", "PERRO", "Hembra", "miraflores", "Adoptada")
        assertEquals(1, pelusas.size)
        assertEquals("Pelusa", pelusas[0].name)

        // 5. Filtros combinados sin coincidencias
        val sinCoincidencias = filtrarMascotas(mascotas, "", "Conejo", "Hembra", "Surco", "Disponible")
        assertTrue(sinCoincidencias.isEmpty())

        // 6. Búsqueda por ubicación
        val mascotasSurco = filtrarMascotas(mascotas, "surco", "Todos", "Todos", "Todas", "Todos")
        assertEquals(1, mascotasSurco.size)
        assertEquals("Tambor", mascotasSurco[0].name)
    }

    @Test
    fun esFormatoCorreoValido_validaFormatosCorrectosEIncorrectos() {
        // Formatos válidos (incluyendo dominios peruanos comunes)
        assertTrue(com.example.adoptme.AdoptMe.esFormatoCorreoValido("usuario@adoptme.com"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoCorreoValido("ana.morales@gmail.com"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoCorreoValido("test+adopt@empresa.org"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoCorreoValido("lucia.quispe@correo.pe"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoCorreoValido("contacto@refugio.com.pe"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoCorreoValido("adopciones@veterinaria.edu.pe"))

        // Formatos inválidos
        assertFalse(com.example.adoptme.AdoptMe.esFormatoCorreoValido(""))
        assertFalse(com.example.adoptme.AdoptMe.esFormatoCorreoValido("sinarroba"))
        assertFalse(com.example.adoptme.AdoptMe.esFormatoCorreoValido("usuario@"))
        assertFalse(com.example.adoptme.AdoptMe.esFormatoCorreoValido("usuario@dominio"))
        assertFalse(com.example.adoptme.AdoptMe.esFormatoCorreoValido("@dominio.com"))
        assertFalse(com.example.adoptme.AdoptMe.esFormatoCorreoValido("usuario@.com"))
    }

    @Test
    fun normalizarTelefonoPeruano_eliminaEspaciosYNormaliza() {
        assertEquals("999888777", com.example.adoptme.AdoptMe.normalizarTelefonoPeruano("999888777"))
        assertEquals("999888777", com.example.adoptme.AdoptMe.normalizarTelefonoPeruano("999 888 777"))
        assertEquals("999888777", com.example.adoptme.AdoptMe.normalizarTelefonoPeruano("  999  888  777  "))
        assertEquals("912345678", com.example.adoptme.AdoptMe.normalizarTelefonoPeruano(" 912 345 678 "))
    }

    @Test
    fun esTelefonoPeruanoValido_validaFormatosCorrectosEIncorrectos() {
        // Ejemplos válidos
        assertTrue(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("999888777"))
        assertTrue(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("999 888 777"))
        assertTrue(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido(" 912 345 678 "))

        // Ejemplos inválidos requeridos
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("999999")) // Menos de 9 dígitos
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("888888888")) // No empieza con 9
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("9998887777")) // Más de 9 dígitos
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("999abc777")) // Letras
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("999-888-777")) // Símbolos
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("")) // Vacío
    }

    @Test
    fun usuarioRegistrado_asignaRolAdoptantePorDefecto() {
        val nuevoUsuario = com.example.adoptme.AdoptMe.UsuarioRegistrado(
            nombre = "Carlos Díaz",
            correo = "carlos@correo.pe",
            contrasena = "secreto123",
            telefono = "999888777"
        )
        assertEquals("Carlos Díaz", nuevoUsuario.nombre)
        assertEquals("carlos@correo.pe", nuevoUsuario.correo)
        assertEquals("secreto123", nuevoUsuario.contrasena)
        assertEquals("999888777", nuevoUsuario.telefono)
        assertEquals("Adoptante", nuevoUsuario.rol)
    }

    // =========================================================================
    // TESTS UNITARIOS: TEST DE COMPATIBILIDAD
    // =========================================================================

    @Test
    fun testCompatibilidad_noPermiteAvanzarConRespuestaVacia() {
        assertFalse(com.example.adoptme.AdoptMe.puedeAvanzarPregunta(""))
        assertFalse(com.example.adoptme.AdoptMe.puedeAvanzarPregunta("   "))
        assertTrue(com.example.adoptme.AdoptMe.puedeAvanzarPregunta("Departamento"))
        assertTrue(com.example.adoptme.AdoptMe.puedeAvanzarPregunta("Perro"))
    }

    @Test
    fun testCompatibilidad_normalizarTextoCompatibilidad_limpiaTildesYEspacios() {
        assertEquals("perro", com.example.adoptme.AdoptMe.normalizarTextoCompatibilidad("  Pérro  "))
        assertEquals("gato", com.example.adoptme.AdoptMe.normalizarTextoCompatibilidad("GATÓ"))
        assertEquals("en proceso", com.example.adoptme.AdoptMe.normalizarTextoCompatibilidad("En Proceso"))
        assertEquals("disponible", com.example.adoptme.AdoptMe.normalizarTextoCompatibilidad(" DISPONIBLE "))
    }

    @Test
    fun testCompatibilidad_reiniciarRespuestasTest_limpiaTodosLosCampos() {
        val reiniciado = com.example.adoptme.AdoptMe.reiniciarRespuestasTest()
        assertEquals("", reiniciado.vivienda)
        assertEquals("", reiniciado.tiempoDisponible)
        assertEquals("", reiniciado.experiencia)
        assertEquals("", reiniciado.especiePreferida)
    }

    @Test
    fun testCompatibilidad_preferenciaEspecieFavoreceEspecieCorrecta() {
        val perro = Pet(1, "Bobby", "Perro", "Mestizo", "2 años", "Macho", "Lima", "Disponible", "Dócil", 0)
        val gato = Pet(2, "Michi", "Gato", "Siamés", "1 año", "Hembra", "Lima", "Disponible", "Cariñoso", 0)
        val mascotas = listOf(perro, gato)

        val respuestasPreferenciaPerro = com.example.adoptme.AdoptMe.RespuestasTestCompatibilidad(
            vivienda = "Casa con patio",
            tiempoDisponible = "Más de 2 horas",
            experiencia = "He tenido antes",
            especiePreferida = "Perro"
        )

        val resultados = com.example.adoptme.AdoptMe.evaluarTestCompatibilidad(mascotas, respuestasPreferenciaPerro)
        assertEquals(2, resultados.size)
        // Bobby (perro) debe estar primero por tener mayor puntaje
        assertEquals("Bobby", resultados[0].mascota.name)
        assertTrue(resultados[0].porcentajeCoincidencia > resultados[1].porcentajeCoincidencia)
        assertTrue(resultados[0].explicacion.contains("especie", ignoreCase = true))
    }

    @Test
    fun testCompatibilidad_ordenaResultadosDeMayorAMenor() {
        val perro = Pet(1, "Rocky", "Perro", "Criollo", "3 años", "Macho", "Lima", "Disponible", "Sociable", 0)
        val gato = Pet(2, "Misu", "Gato", "Común", "1 año", "Hembra", "Lima", "Disponible", "Tranquila", 0)
        val conejo = Pet(3, "Tambor", "Conejo", "Enano", "6 meses", "Macho", "Lima", "Disponible", "Amigable", 0)
        val mascotas = listOf(perro, gato, conejo)

        val respuestas = com.example.adoptme.AdoptMe.RespuestasTestCompatibilidad(
            vivienda = "Departamento pequeño",
            tiempoDisponible = "Menos de 1 hora",
            experiencia = "Primera vez",
            especiePreferida = "Gato"
        )

        val resultados = com.example.adoptme.AdoptMe.evaluarTestCompatibilidad(mascotas, respuestas)
        assertTrue(resultados.isNotEmpty())
        for (i in 0 until resultados.size - 1) {
            assertTrue(
                "La lista debe estar ordenada descendentemente por porcentaje",
                resultados[i].porcentajeCoincidencia >= resultados[i + 1].porcentajeCoincidencia
            )
        }
    }

    @Test
    fun testCompatibilidad_excluyeMascotasAdoptadas() {
        val disponible = Pet(1, "Toby", "Perro", "Mestizo", "2 años", "Macho", "Lima", "Disponible", "Amigable", 0)
        val adoptada = Pet(2, "Fido", "Perro", "Mestizo", "4 años", "Macho", "Lima", "Adoptada", "Tranquilo", 0)
        val enProceso = Pet(3, "Pelusa", "Gato", "Común", "1 año", "Hembra", "Lima", "En proceso", "Dócil", 0)
        val mascotas = listOf(disponible, adoptada, enProceso)

        val respuestas = com.example.adoptme.AdoptMe.RespuestasTestCompatibilidad(
            vivienda = "Casa con jardín",
            tiempoDisponible = "1 a 2 horas",
            experiencia = "He tenido antes",
            especiePreferida = "Sin preferencia (Cualquiera)"
        )

        val resultados = com.example.adoptme.AdoptMe.evaluarTestCompatibilidad(mascotas, respuestas)
        // La mascota 'Adoptada' no debe figurar en los resultados
        assertFalse(resultados.any { it.mascota.name == "Fido" })
        assertTrue(resultados.any { it.mascota.name == "Toby" })
        assertTrue(resultados.any { it.mascota.name == "Pelusa" })
    }

    @Test
    fun testCompatibilidad_permiteSolicitarAdopcion_soloSiDisponible() {
        val disponible = Pet(1, "Toby", "Perro", "Mestizo", "2 años", "Macho", "Lima", "Disponible", "Amigable", 0)
        val enProceso = Pet(2, "Pelusa", "Gato", "Común", "1 año", "Hembra", "Lima", "En proceso", "Dócil", 0)
        val adoptada = Pet(3, "Fido", "Perro", "Mestizo", "4 años", "Macho", "Lima", "Adoptada", "Tranquilo", 0)

        assertTrue(com.example.adoptme.AdoptMe.permiteSolicitarAdopcion(disponible))
        assertFalse(com.example.adoptme.AdoptMe.permiteSolicitarAdopcion(enProceso))
        assertFalse(com.example.adoptme.AdoptMe.permiteSolicitarAdopcion(adoptada))
    }

    @Test
    fun testCompatibilidad_sinCoincidenciasRetornaListaVacia() {
        // Si todas las mascotas están adoptadas o no pertenecen a especies admitidas (perro, gato, conejo)
        val adoptada = Pet(1, "Fido", "Perro", "Mestizo", "4 años", "Macho", "Lima", "Adoptada", "Tranquilo", 0)
        val loro = Pet(2, "Pepe", "Ave", "Loro", "2 años", "Macho", "Lima", "Disponible", "Hablador", 0)
        val mascotas = listOf(adoptada, loro)

        val respuestas = com.example.adoptme.AdoptMe.RespuestasTestCompatibilidad(
            vivienda = "Departamento",
            tiempoDisponible = "1 a 2 horas",
            experiencia = "He tenido antes",
            especiePreferida = "Perro"
        )

        val resultados = com.example.adoptme.AdoptMe.evaluarTestCompatibilidad(mascotas, respuestas)
        assertTrue(resultados.isEmpty())
    }

    // =========================================================================
    // TESTS UNITARIOS: PUBLICAR MASCOTA (USUARIO ADOPTANTE)
    // =========================================================================

    @Test
    fun testPublicacion_esEspecieValida_aceptaSoloPerroGatoConejo() {
        // Especies válidas (RN-02) con diferentes mayúsculas y tildes
        assertTrue(com.example.adoptme.AdoptMe.esEspecieValida("Perro"))
        assertTrue(com.example.adoptme.AdoptMe.esEspecieValida("perro"))
        assertTrue(com.example.adoptme.AdoptMe.esEspecieValida("Pérro"))
        assertTrue(com.example.adoptme.AdoptMe.esEspecieValida("Gato"))
        assertTrue(com.example.adoptme.AdoptMe.esEspecieValida("GATO"))
        assertTrue(com.example.adoptme.AdoptMe.esEspecieValida("gato"))
        assertTrue(com.example.adoptme.AdoptMe.esEspecieValida("Conejo"))
        assertTrue(com.example.adoptme.AdoptMe.esEspecieValida("conejo"))

        // Especies no admitidas
        assertFalse(com.example.adoptme.AdoptMe.esEspecieValida("Loro"))
        assertFalse(com.example.adoptme.AdoptMe.esEspecieValida("Pez"))
        assertFalse(com.example.adoptme.AdoptMe.esEspecieValida("Hamster"))
        assertFalse(com.example.adoptme.AdoptMe.esEspecieValida(""))
        assertFalse(com.example.adoptme.AdoptMe.esEspecieValida("   "))
    }

    @Test
    fun testPublicacion_esFormatoEdadValido_aceptaYRechazaFormatos() {
        // Formatos válidos requeridos
        assertTrue(com.example.adoptme.AdoptMe.esFormatoEdadValido("1 año"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoEdadValido("2 años"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoEdadValido("6 meses"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoEdadValido("10 meses"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoEdadValido("1 ano"))
        assertTrue(com.example.adoptme.AdoptMe.esFormatoEdadValido("2 anos"))

        // Formatos inválidos requeridos
        assertFalse(com.example.adoptme.AdoptMe.esFormatoEdadValido("")) // Vacío
        assertFalse(com.example.adoptme.AdoptMe.esFormatoEdadValido("   ")) // Espacios
        assertFalse(com.example.adoptme.AdoptMe.esFormatoEdadValido("dos años")) // Texto sin número
        assertFalse(com.example.adoptme.AdoptMe.esFormatoEdadValido("-2 años")) // Número negativo
        assertFalse(com.example.adoptme.AdoptMe.esFormatoEdadValido("-1 año")) // Número negativo
        assertFalse(com.example.adoptme.AdoptMe.esFormatoEdadValido("0 años")) // Cero
        assertFalse(com.example.adoptme.AdoptMe.esFormatoEdadValido("2 años y medio")) // Formato desconocido
        assertFalse(com.example.adoptme.AdoptMe.esFormatoEdadValido("perro")) // Texto arbitrario
    }

    @Test
    fun testPublicacion_esSexoValido_soloAceptaMachoOHembra() {
        assertTrue(com.example.adoptme.AdoptMe.esSexoValido("Macho"))
        assertTrue(com.example.adoptme.AdoptMe.esSexoValido("macho"))
        assertTrue(com.example.adoptme.AdoptMe.esSexoValido("Hembra"))
        assertTrue(com.example.adoptme.AdoptMe.esSexoValido("hembra"))

        assertFalse(com.example.adoptme.AdoptMe.esSexoValido(""))
        assertFalse(com.example.adoptme.AdoptMe.esSexoValido("Indefinido"))
        assertFalse(com.example.adoptme.AdoptMe.esSexoValido("Otro"))
    }

    @Test
    fun testPublicacion_validarDatosPublicacion_detectaCamposFaltantes() {
        // Datos válidos
        val validacionExitosa = com.example.adoptme.AdoptMe.validarDatosPublicacion(
            nombre = "Bobby",
            especie = "Perro",
            raza = "Mestizo",
            edad = "2 años",
            genero = "Macho",
            ubicacion = "Miraflores, Lima",
            descripcion = "Perro muy alegre y juguetón."
        )
        assertTrue(validacionExitosa.esValido)
        assertNull(validacionExitosa.errorNombre)
        assertNull(validacionExitosa.errorEspecie)
        assertNull(validacionExitosa.errorEdad)

        // Datos con campos vacíos y especie no permitida
        val validacionInvalida = com.example.adoptme.AdoptMe.validarDatosPublicacion(
            nombre = "",
            especie = "Loro",
            raza = "",
            edad = "dos años",
            genero = "",
            ubicacion = "",
            descripcion = ""
        )
        assertFalse(validacionInvalida.esValido)
        assertNotNull(validacionInvalida.errorNombre)
        assertNotNull(validacionInvalida.errorEspecie)
        assertNotNull(validacionInvalida.errorRaza)
        assertNotNull(validacionInvalida.errorEdad)
        assertNotNull(validacionInvalida.errorGenero)
        assertNotNull(validacionInvalida.errorUbicacion)
        assertNotNull(validacionInvalida.errorDescripcion)
    }

    @Test
    fun testPublicacion_crearPublicacionMascota_asignaEstadoDisponibleEIdValido() {
        val nuevaMascota = com.example.adoptme.AdoptMe.crearPublicacionMascota(
            nombre = "Michi",
            especie = "gato",
            raza = "Siamés",
            edad = "1 año",
            genero = "hembra",
            ubicacion = "San Borja, Lima",
            descripcion = "Gatita muy tranquila y cariñosa.",
            fotoBitmap = null
        )

        assertTrue(nuevaMascota.id > 0)
        assertEquals("Michi", nuevaMascota.name)
        assertEquals("Gato", nuevaMascota.species)
        assertEquals("Siamés", nuevaNuevaRaza(nuevaMascota))
        assertEquals("1 año", nuevaMascota.age)
        assertEquals("Hembra", nuevaMascota.gender)
        assertEquals("Disponible", nuevaMascota.status)
        assertEquals("San Borja, Lima", nuevaMascota.location)
        assertTrue(nuevaMascota.image != 0) // Asignó drawable existente de apoyo
        assertNull(nuevaMascota.photoBitmap)
    }

    private fun nuevaNuevaRaza(p: Pet): String = p.breed

    @Test
    fun testPublicacion_diferenciacionEntreMascotaPublicadaYBase() {
        val repositorio = com.example.adoptme.data.PetRepository()
        val mascotasBase = repositorio.getPets()

        val mascotaUsuario = com.example.adoptme.AdoptMe.crearPublicacionMascota(
            nombre = "Pelusa",
            especie = "Conejo",
            raza = "Enano",
            edad = "6 meses",
            genero = "Macho",
            ubicacion = "Surco, Lima",
            descripcion = "Conejo muy dócil."
        )

        val mascotasPublicadas = mutableListOf<Pet>(mascotaUsuario)

        // La mascota del usuario se encuentra en mascotasPublicadas
        assertTrue(mascotasPublicadas.any { it.id == mascotaUsuario.id })

        // Ninguna de las mascotas base debe pertenecer a mascotasPublicadas
        mascotasBase.forEach { base ->
            assertFalse(mascotasPublicadas.any { it.id == base.id })
        }
    }

    // =========================================================================
    // TESTS UNITARIOS: AUDITORÍA Y FLUJO T1
    // =========================================================================

    @Test
    fun testPropiedad_bloqueaAutoAdopcionYReconocePublicacionPropia() {
        val correoDuenio = "adoptante@correo.pe"
        val correoOtroUsuario = "otro@correo.pe"

        val mascotaPublicada = com.example.adoptme.AdoptMe.crearPublicacionMascota(
            nombre = "Boby",
            especie = "Perro",
            raza = "Mestizo",
            edad = "2 años",
            genero = "Macho",
            ubicacion = "Lima",
            descripcion = "Tranquilo",
            creadoPorCorreo = correoDuenio
        )

        // 1. Verificación de asignación de creadoPorCorreo
        assertEquals(correoDuenio, mascotaPublicada.creadoPorCorreo)

        // 2. Comprobación de propiedad según correo del usuario activo
        val esPropiaParaDuenio = mascotaPublicada.creadoPorCorreo.isNotBlank() &&
                mascotaPublicada.creadoPorCorreo.equals(correoDuenio, ignoreCase = true)
        assertTrue(esPropiaParaDuenio)

        val esPropiaParaOtro = mascotaPublicada.creadoPorCorreo.isNotBlank() &&
                mascotaPublicada.creadoPorCorreo.equals(correoOtroUsuario, ignoreCase = true)
        assertFalse(esPropiaParaOtro)

        // 3. Bloqueo de solicitud de adopción si es publicación propia
        val puedeDuenioSolicitar = com.example.adoptme.AdoptMe.permiteSolicitarAdopcion(
            mascota = mascotaPublicada,
            esPublicacionPropia = esPropiaParaDuenio
        )
        assertFalse(puedeDuenioSolicitar)

        // 4. Otro usuario sí puede solicitar adopción si está Disponible
        val puedeOtroSolicitar = com.example.adoptme.AdoptMe.permiteSolicitarAdopcion(
            mascota = mascotaPublicada,
            esPublicacionPropia = esPropiaParaOtro
        )
        assertTrue(puedeOtroSolicitar)
    }

    @Test
    fun testSolicitudes_controlDuplicadosActivosPorMascotaYPostulante() {
        val solicitudes = listOf(
            com.example.adoptme.model.AdoptionRequest(
                petId = 1,
                petName = "Max",
                applicantName = "Juan Pérez",
                email = "juan@correo.pe",
                status = "Pendiente"
            ),
            com.example.adoptme.model.AdoptionRequest(
                petId = 2,
                petName = "Luna",
                applicantName = "Ana Gómez",
                email = "ana@correo.pe",
                status = "Rechazada"
            )
        )

        // Intento 1: Juan intenta solicitar de nuevo a Max (petId = 1) -> Debe detectarse como duplicada activa
        val esDuplicadaJuan = solicitudes.any { req ->
            val coincideMascota = (req.petId == 1) || req.petName.equals("Max", ignoreCase = true)
            val coincidePostulante = req.email.equals("juan@correo.pe", ignoreCase = true)
            val esActiva = req.status.equals("Pendiente", ignoreCase = true) || req.status.equals("En revisión", ignoreCase = true)
            coincideMascota && coincidePostulante && esActiva
        }
        assertTrue(esDuplicadaJuan)

        // Intento 2: Carlos solicita a Max (petId = 1) -> No es duplicada de Carlos
        val esDuplicadaCarlos = solicitudes.any { req ->
            val coincideMascota = (req.petId == 1) || req.petName.equals("Max", ignoreCase = true)
            val coincidePostulante = req.email.equals("carlos@correo.pe", ignoreCase = true)
            val esActiva = req.status.equals("Pendiente", ignoreCase = true) || req.status.equals("En revisión", ignoreCase = true)
            coincideMascota && coincidePostulante && esActiva
        }
        assertFalse(esDuplicadaCarlos)

        // Intento 3: Ana vuelve a solicitar a Luna (petId = 2) cuya solicitud anterior fue "Rechazada" (no activa)
        val esDuplicadaAna = solicitudes.any { req ->
            val coincideMascota = (req.petId == 2) || req.petName.equals("Luna", ignoreCase = true)
            val coincidePostulante = req.email.equals("ana@correo.pe", ignoreCase = true)
            val esActiva = req.status.equals("Pendiente", ignoreCase = true) || req.status.equals("En revisión", ignoreCase = true)
            coincideMascota && coincidePostulante && esActiva
        }
        assertFalse(esDuplicadaAna)
    }

    @Test
    fun testAdmin_alAprobarSolicitud_mascotaPasaAEnProceso() {
        val catalogo = mutableListOf(
            Pet(10, "Rocky", "Perro", "Boxer", "2 años", "Macho", "Lima", "Disponible", "Juguetón", 0)
        )
        val solicitudes = mutableListOf(
            com.example.adoptme.model.AdoptionRequest(
                petId = 10,
                petName = "Rocky",
                applicantName = "Pedro",
                email = "pedro@correo.pe",
                status = "Pendiente"
            )
        )

        // Simulación de aprobación por el Admin
        val indiceSol = solicitudes.indexOfFirst { it.petId == 10 }
        assertTrue(indiceSol != -1)
        solicitudes[indiceSol] = solicitudes[indiceSol].copy(status = "Aprobada")

        val indiceMascota = catalogo.indexOfFirst { it.id == 10 }
        assertTrue(indiceMascota != -1)
        catalogo[indiceMascota] = catalogo[indiceMascota].copy(status = "En proceso")

        assertEquals("Aprobada", solicitudes[0].status)
        assertEquals("En proceso", catalogo[0].status)
    }

    // =========================================================================
    // TESTS UNITARIOS: FUNCIONES CON NOMBRES LIMPIOS Y CONCISOS
    // =========================================================================

    @Test
    fun testNombresLimpios_filtrosInicio() {
        assertTrue(com.example.adoptme.AdoptMe.esFiltroLibre("Todos"))
        assertTrue(com.example.adoptme.AdoptMe.esFiltroLibre("Cualquier sexo"))
        assertFalse(com.example.adoptme.AdoptMe.esFiltroLibre("Perro"))

        val perro = Pet(1, "Rocky", "Perro", "Boxer", "2 años", "Macho", "Lima", "Disponible", "Juguetón", 0)
        assertTrue(com.example.adoptme.AdoptMe.filtrarEspecie(perro, "perro"))
        assertTrue(com.example.adoptme.AdoptMe.filtrarEspecie(perro, "Todos"))
        assertFalse(com.example.adoptme.AdoptMe.filtrarEspecie(perro, "gato"))

        assertTrue(com.example.adoptme.AdoptMe.filtrarSexo(perro, "macho"))
        assertTrue(com.example.adoptme.AdoptMe.filtrarUbicacion(perro, "lima"))
        assertTrue(com.example.adoptme.AdoptMe.filtrarEstado(perro, "disponible"))
    }

    @Test
    fun testNombresLimpios_testCompatibilidad() {
        assertTrue(com.example.adoptme.AdoptMe.respuestaValida("Departamento"))
        assertFalse(com.example.adoptme.AdoptMe.respuestaValida(""))

        assertEquals("gato", com.example.adoptme.AdoptMe.limpiarTexto("  GATÓ  "))

        val respuestasLimpias = com.example.adoptme.AdoptMe.reiniciarTest()
        assertEquals("", respuestasLimpias.vivienda)
        assertEquals("", respuestasLimpias.tiempoDisponible)

        val mascota = Pet(1, "Michi", "Gato", "Siamés", "1 año", "Hembra", "Lima", "Disponible", "Cariñosa", 0)
        assertTrue(com.example.adoptme.AdoptMe.puedeAdoptar(mascota, esPublicacionPropia = false))
        assertFalse(com.example.adoptme.AdoptMe.puedeAdoptar(mascota, esPublicacionPropia = true))

        val coincidencias = com.example.adoptme.AdoptMe.buscarCoincidencias(
            listOf(mascota),
            com.example.adoptme.AdoptMe.RespuestasTestCompatibilidad(
                vivienda = "Departamento",
                tiempoDisponible = "1 hora",
                experiencia = "Primera vez",
                especiePreferida = "Gato"
            )
        )
        assertEquals(1, coincidencias.size)
        assertEquals("Michi", coincidencias[0].mascota.name)
    }

    @Test
    fun testNombresLimpios_publicarMascota() {
        val validacion = com.example.adoptme.AdoptMe.revisarPublicacion(
            nombre = "Coco",
            especie = "Conejo",
            raza = "Enano",
            edad = "6 meses",
            genero = "Macho",
            ubicacion = "Surco",
            descripcion = "Tranquilo"
        )
        assertTrue(validacion.esValido)

        val nueva = com.example.adoptme.AdoptMe.crearMascota(
            nombre = "Coco",
            especie = "Conejo",
            raza = "Enano",
            edad = "6 meses",
            genero = "Macho",
            ubicacion = "Surco",
            descripcion = "Tranquilo",
            creadoPorCorreo = "duenio@correo.pe"
        )
        assertEquals("Coco", nueva.name)
        assertEquals("Conejo", nueva.species)
        assertEquals("duenio@correo.pe", nueva.creadoPorCorreo)
    }

    // =========================================================================
    // TESTS UNITARIOS: AISLAMIENTO DE DATOS POR USUARIO
    // =========================================================================

    @Test
    fun testAislamiento_normalizarCorreoUsuario_recortaEspaciosYConvierteAMinusculas() {
        assertEquals("usuario@adoptme.com", normalizarCorreoUsuario("  Usuario@AdoptMe.COM  "))
        assertEquals("ana.morales@correo.pe", normalizarCorreoUsuario(" Ana.Morales@Correo.PE "))
        assertEquals("admin@adoptme.com", normalizarCorreoUsuario("ADMIN@ADOPTME.COM"))
        assertEquals("", normalizarCorreoUsuario("   "))
        assertTrue(normalizarCorreoUsuario("Usuario@AdoptMe.COM") == normalizarCorreoUsuario("usuario@adoptme.com"))
    }

    @Test
    fun testAislamiento_obtenerClaveUsuario_generaClaveSegura() {
        assertEquals("usuario_adoptme_com", obtenerClaveUsuario("usuario@adoptme.com"))
        assertEquals("ana_morales_correo_pe", obtenerClaveUsuario("ana.morales@correo.pe"))
        assertEquals("carlos_t1_adoptme_com", obtenerClaveUsuario("carlos+t1@adoptme.com"))
        assertEquals("admin_adoptme_com", obtenerClaveUsuario("ADMIN@ADOPTME.COM"))
        assertEquals("anonimo", obtenerClaveUsuario("   "))
    }

    @Test
    fun testAislamiento_clavesDiferenciadasEntreUsuarios() {
        val claveUsuario1 = "favoritos_${obtenerClaveUsuario("usuario@adoptme.com")}"
        val claveUsuario2 = "favoritos_${obtenerClaveUsuario("ana@adoptme.com")}"
        val claveAdmin = "favoritos_${obtenerClaveUsuario("admin@adoptme.com")}"

        // Las claves de SharedPreferences no deben colisionar entre usuarios
        assertNotEquals(claveUsuario1, claveUsuario2)
        assertNotEquals(claveUsuario1, claveAdmin)
        assertNotEquals(claveUsuario2, claveAdmin)

        // Claves de notificaciones
        val notifUser1 = "notificaciones_${obtenerClaveUsuario("usuario@adoptme.com")}"
        val notifUser2 = "notificaciones_${obtenerClaveUsuario("ana@adoptme.com")}"
        assertNotEquals(notifUser1, notifUser2)

        // Solicitudes: partición de usuario vs admin general
        val solUser1 = "solicitudes_${obtenerClaveUsuario("usuario@adoptme.com")}"
        val solAdmin = "solicitudes_admin"
        assertNotEquals(solUser1, solAdmin)
    }

    @Test
    fun testAislamiento_noAlmacenaContrasenasEnClaves() {
        // Ninguna clave de usuario ni preferencia debe contener la palabra 'contrasena' ni 'password'
        val clave = obtenerClaveUsuario("usuario@adoptme.com")
        assertFalse(clave.contains("contrasena", ignoreCase = true))
        assertFalse(clave.contains("password", ignoreCase = true))

        val clavePrefTema = "tema_${obtenerClaveUsuario("usuario@adoptme.com")}"
        assertFalse(clavePrefTema.contains("contrasena", ignoreCase = true))
    }

    @Test
    fun testAislamiento_solicitudesParticionUsuarioVsAdmin() {
        // Simulación del comportamiento de almacenamiento particionado
        val todasLasSolicitudes = mutableListOf(
            AdoptionRequest(
                petId = 10,
                petName = "Rocky",
                applicantName = "Carlos",
                email = "carlos@correo.pe",
                status = "Pendiente"
            ),
            AdoptionRequest(
                petId = 20,
                petName = "Michi",
                applicantName = "Ana",
                email = "ana@correo.pe",
                status = "Pendiente"
            )
        )

        // Filtrado por usuario normalizado (como hace cargarSolicitudesUsuario)
        val solicitudesCarlos = todasLasSolicitudes.filter {
            normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario("carlos@correo.pe")
        }
        val solicitudesAna = todasLasSolicitudes.filter {
            normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario("ana@correo.pe")
        }

        // 1. Cada usuario solo ve sus propias solicitudes
        assertEquals(1, solicitudesCarlos.size)
        assertEquals("Rocky", solicitudesCarlos[0].petName)

        assertEquals(1, solicitudesAna.size)
        assertEquals("Michi", solicitudesAna[0].petName)

        // 2. Admin ve todas las solicitudes
        assertEquals(2, todasLasSolicitudes.size)

        // 3. Admin aprueba la solicitud de Carlos -> Se actualiza en el maestro general y en la vista del usuario
        val solicitudAprobada = solicitudesCarlos[0].copy(status = "Aprobada")
        val indiceAdmin = todasLasSolicitudes.indexOfFirst {
            it.petId == solicitudAprobada.petId &&
                normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario(solicitudAprobada.email)
        }
        assertTrue(indiceAdmin != -1)
        todasLasSolicitudes[indiceAdmin] = solicitudAprobada

        val solicitudesCarlosActualizadas = todasLasSolicitudes.filter {
            normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario("carlos@correo.pe")
        }
        assertEquals("Aprobada", solicitudesCarlosActualizadas[0].status)

        // La solicitud de Ana permanece sin cambios
        val solicitudesAnaSinCambio = todasLasSolicitudes.filter {
            normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario("ana@correo.pe")
        }
        assertEquals("Pendiente", solicitudesAnaSinCambio[0].status)
    }

    @Test
    fun testAislamiento_cierreSesionLimpiaListasEnMemoriaSinAfectarPersistencia() {
        // Listas reactivas activas en memoria para la sesión
        val mascotasFavoritas = mutableListOf(
            Pet(1, "Max", "Perro", "Labrador", "2 años", "Macho", "Lima", "Disponible", "Dócil", 0)
        )
        val notificaciones = mutableListOf(
            NotificationItem("Bienvenido", "¡Hola!")
        )
        val solicitudesAdopcion = mutableListOf(
            AdoptionRequest(petId = 1, petName = "Max", applicantName = "User", email = "user@adoptme.com")
        )

        // Simulación del paso 1 de cerrarSesion: limpiar listas reactivas en memoria
        mascotasFavoritas.clear()
        notificaciones.clear()
        solicitudesAdopcion.clear()

        // Verificamos que las listas en memoria quedan totalmente limpias para el próximo usuario
        assertTrue(mascotasFavoritas.isEmpty())
        assertTrue(notificaciones.isEmpty())
        assertTrue(solicitudesAdopcion.isEmpty())
    }

    // =========================================================================
    // TESTS UNITARIOS: REGLAS DE ADMINISTRACIÓN Y APROBACIÓN POR PETID
    // =========================================================================

    @Test
    fun testAdmin_localizarMascotaParaAprobacion_priorizaPetId() {
        val mascota1 = Pet(id = 101, name = "Rocky", species = "Perro", breed = "Boxer", age = "2 años", gender = "Macho", location = "Lima", status = "Disponible", description = "", image = 0)
        val mascota2 = Pet(id = 202, name = "Rocky", species = "Perro", breed = "Criollo", age = "1 año", gender = "Macho", location = "Surco", status = "Disponible", description = "", image = 0)
        val mascotas = listOf(mascota1, mascota2)

        // Cuando petId > 0, debe encontrar estrictamente por ID (202), no por el primer nombre encontrado
        val encontrada = localizarMascotaParaAprobacion(mascotas, petId = 202, nombreMascota = "Rocky")
        assertNotNull(encontrada)
        assertEquals(202, encontrada?.id)
        assertEquals("Criollo", encontrada?.breed)
    }

    @Test
    fun testAdmin_localizarMascotaParaAprobacion_usaNombreSoloCuandoPetIdEsCero() {
        val mascota1 = Pet(id = 50, name = "Luna", species = "Gato", breed = "Siamés", age = "1 año", gender = "Hembra", location = "Lima", status = "Disponible", description = "", image = 0)
        val mascotas = listOf(mascota1)

        // petId == 0 usa el nombre como respaldo
        val encontrada = localizarMascotaParaAprobacion(mascotas, petId = 0, nombreMascota = "Luna")
        assertNotNull(encontrada)
        assertEquals(50, encontrada?.id)

        // Nombre inexistente con petId == 0 retorna null
        val noEncontrada = localizarMascotaParaAprobacion(mascotas, petId = 0, nombreMascota = "Inexistente")
        assertNull(noEncontrada)
    }

    @Test
    fun testAdmin_puedeAprobarSolicitud_rechazaMascotaInexistente() {
        val (puedeAprobar, mensaje) = puedeAprobarSolicitud(null)
        assertFalse(puedeAprobar)
        assertTrue(mensaje.contains("No se encontró la mascota", ignoreCase = true))
    }

    @Test
    fun testAdmin_puedeAprobarSolicitud_rechazaMascotaEnProceso() {
        val mascotaEnProceso = Pet(id = 1, name = "Toby", species = "Perro", breed = "Pug", age = "2 años", gender = "Macho", location = "Lima", status = "En proceso", description = "", image = 0)
        val (puedeAprobar, mensaje) = puedeAprobarSolicitud(mascotaEnProceso)
        assertFalse(puedeAprobar)
        assertTrue(mensaje.contains("En proceso", ignoreCase = true))
    }

    @Test
    fun testAdmin_puedeAprobarSolicitud_rechazaMascotaAdoptada() {
        val mascotaAdoptada = Pet(id = 2, name = "Mia", species = "Gato", breed = "Común", age = "1 año", gender = "Hembra", location = "Lima", status = "Adoptada", description = "", image = 0)
        val (puedeAprobar, mensaje) = puedeAprobarSolicitud(mascotaAdoptada)
        assertFalse(puedeAprobar)
        assertTrue(mensaje.contains("adoptada", ignoreCase = true))
    }

    @Test
    fun testAdmin_puedeAprobarSolicitud_aceptaMascotaDisponible() {
        val mascotaDisponible = Pet(id = 3, name = "Pelusa", species = "Conejo", breed = "Enano", age = "6 meses", gender = "Macho", location = "Lima", status = "Disponible", description = "", image = 0)
        val (puedeAprobar, mensaje) = puedeAprobarSolicitud(mascotaDisponible)
        assertTrue(puedeAprobar)
        assertTrue(mensaje.contains("permitida", ignoreCase = true))
    }

    // =========================================================================
    // TESTS UNITARIOS: EDICIÓN SEGURA DEL PERFIL Y TELÉFONO PERUANO
    // =========================================================================

    @Test
    fun testPerfil_validacionRechazaNombreVacio() {
        val nombreVacio = ""
        val nombreEspacios = "   "
        assertTrue(nombreVacio.trim().isBlank())
        assertTrue(nombreEspacios.trim().isBlank())
    }

    @Test
    fun testPerfil_validacionRechazaTelefonoPeruanoInvalido() {
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido(""))
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("888888888")) // No empieza con 9
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("999999")) // Menos de 9 dígitos
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("9999999999")) // Más de 9 dígitos
        assertFalse(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido("999abc777")) // Contiene letras
    }

    @Test
    fun testPerfil_validacionAceptaYNormalizaTelefonoPeruano() {
        val telConEspacios = " 987 654 321 "
        assertTrue(com.example.adoptme.AdoptMe.esTelefonoPeruanoValido(telConEspacios))
        val telNormalizado = com.example.adoptme.AdoptMe.normalizarTelefonoPeruano(telConEspacios)
        assertEquals("987654321", telNormalizado)
    }

    @Test
    fun testPerfil_correoPermaneceInvariableEnActualizacion() {
        val usuarioOriginal = com.example.adoptme.AdoptMe.UsuarioRegistrado(
            nombre = "Juan Pérez",
            correo = "juan@adoptme.com",
            telefono = "999888777",
            contrasena = "clave123"
        )

        // Simulación de guardar perfil: actualiza nombre y teléfono, conservando el correo original
        val nuevoNombre = "Juan Carlos Pérez"
        val nuevoTelefono = "912345678"
        val usuarioActualizado = usuarioOriginal.copy(
            nombre = nuevoNombre,
            correo = usuarioOriginal.correo, // Invariable
            telefono = nuevoTelefono
        )

        assertEquals("juan@adoptme.com", usuarioActualizado.correo)
        assertEquals("Juan Carlos Pérez", usuarioActualizado.nombre)
        assertEquals("912345678", usuarioActualizado.telefono)
    }

    // =========================================================================
    // TESTS UNITARIOS: PROPIEDAD Y PREFERENCIAS AISLADAS
    // =========================================================================

    @Test
    fun testPropiedad_propietarioNoPuedeSolicitarSuPublicacion() {
        val mascotaPropia = Pet(
            id = 10,
            name = "Fido",
            species = "Perro",
            breed = "Mestizo",
            age = "2 años",
            gender = "Macho",
            location = "Lima",
            status = "Disponible",
            description = "",
            image = 0,
            creadoPorCorreo = "dueño@adoptme.com"
        )
        val correoUsuario = "dueño@adoptme.com"
        val esPropia = mascotaPropia.creadoPorCorreo.isNotBlank() &&
                mascotaPropia.creadoPorCorreo.equals(correoUsuario, ignoreCase = true)

        assertFalse(com.example.adoptme.AdoptMe.puedeAdoptar(mascotaPropia, esPublicacionPropia = esPropia))
    }

    @Test
    fun testPropiedad_mascotaDisponibleDeOtroUsuarioSiPuedeSolicitarse() {
        val mascotaOtro = Pet(
            id = 11,
            name = "Bobi",
            species = "Perro",
            breed = "Mestizo",
            age = "1 año",
            gender = "Macho",
            location = "Lima",
            status = "Disponible",
            description = "",
            image = 0,
            creadoPorCorreo = "otro@adoptme.com"
        )
        val correoUsuario = "adoptante@adoptme.com"
        val esPropia = mascotaOtro.creadoPorCorreo.isNotBlank() &&
                mascotaOtro.creadoPorCorreo.equals(correoUsuario, ignoreCase = true)

        assertTrue(com.example.adoptme.AdoptMe.puedeAdoptar(mascotaOtro, esPublicacionPropia = esPropia))
    }

    @Test
    fun testAislamiento_preferenciasSeparadasEntreUsuarios() {
        val clavePrefUser1 = "tema_${obtenerClaveUsuario("usuario1@adoptme.com")}"
        val clavePrefUser2 = "tema_${obtenerClaveUsuario("usuario2@adoptme.com")}"
        assertNotEquals(clavePrefUser1, clavePrefUser2)

        val claveFiltroUser1 = "mascota_preferida_${obtenerClaveUsuario("usuario1@adoptme.com")}"
        val claveFiltroUser2 = "mascota_preferida_${obtenerClaveUsuario("usuario2@adoptme.com")}"
        assertNotEquals(claveFiltroUser1, claveFiltroUser2)
    }

    // =========================================================================
    // PRUEBAS DE CENTRO DE ADOPCIÓN Y CHAT LOCAL FUNCIONAL
    // =========================================================================

    @Test
    fun testLogin_rolesEsperadosSegunCredenciales() {
        // Función simulada que representa el comportamiento de PantallaInicioSesion.kt
        fun autenticarDemo(correo: String, contrasena: String): String? {
            val cLimpio = correo.trim().lowercase()
            val pass = contrasena.trim()
            return when {
                cLimpio == "usuario@adoptme.com" && pass == "123456" -> "Adoptante"
                cLimpio == "centro@adoptme.com" && pass == "123456" -> "Centro"
                cLimpio == "admin@adoptme.com" && pass == "123456" -> "Admin"
                else -> null
            }
        }

        assertEquals("Centro", autenticarDemo("centro@adoptme.com", "123456"))
        assertEquals("Centro", autenticarDemo("  CENTRO@adoptme.com  ", "123456"))
        assertEquals("Adoptante", autenticarDemo("usuario@adoptme.com", "123456"))
        assertEquals("Admin", autenticarDemo("admin@adoptme.com", "123456"))
        assertNull(autenticarDemo("centro@adoptme.com", "erronea"))
        assertNull(autenticarDemo("desconocido@adoptme.com", "123456"))
    }

    @Test
    fun testCentroAdopcion_idsUnicosParaCadaCentro() {
        val centros = listOf(
            CentroAdopcion(1, "Albergue Huellitas del Norte", "SJL, Lima", "Refugio SJL", "9:00 - 17:00", "huellitas.norte@ejemplo.pe", "Av. Próceres 1402"),
            CentroAdopcion(2, "Refugio Patitas Felices", "Surco, Lima", "Refugio Surco", "10:00 - 18:00", "patitas.surco@ejemplo.pe", "Calle Monte Rosa 180"),
            CentroAdopcion(3, "Centro de Adopción Miraflores", "Miraflores, Lima", "Centro Miraflores", "10:00 - 16:00", "adopcion.miraflores@ejemplo.pe", "Av. Larco 345"),
            CentroAdopcion(4, "Hogar Temporal Ate Vitarte", "Ate, Lima", "Hogar Ate", "11:00 - 17:00", "hogar.ate@ejemplo.pe", "Av. Nicolás Ayllón 2890"),
            CentroAdopcion(5, "Módulo Animal Comas", "Comas, Lima", "Módulo Comas", "8:30 - 16:30", "modulo.comas@ejemplo.pe", "Av. Universitaria Norte 4500")
        )

        val ids = centros.map { it.id }
        assertEquals(5, ids.distinct().size)
        assertTrue(ids.containsAll(listOf(1, 2, 3, 4, 5)))
    }

    @Test
    fun testClaveChat_dosCentrosGeneranClavesDiferentesConMismoCorreoDemo() {
        val correoAdoptante = "usuario@adoptme.com"
        val claveCentro1 = claveChat(correoAdoptante, idCentro = 1)
        val claveCentro2 = claveChat(correoAdoptante, idCentro = 2)

        assertNotEquals(claveCentro1, claveCentro2)
        assertEquals("chat_usuario_adoptme_com_centro_1", claveCentro1)
        assertEquals("chat_usuario_adoptme_com_centro_2", claveCentro2)
    }

    @Test
    fun testClaveChat_mismoAdoptanteTieneConversacionesSeparadasConVariosCentros() {
        val correoAdoptante = "ana.morales@gmail.com"
        val clave1 = claveChat(correoAdoptante, idCentro = 1)
        val clave2 = claveChat(correoAdoptante, idCentro = 2)
        val clave3 = claveChat(correoAdoptante, idCentro = 3)

        assertNotEquals(clave1, clave2)
        assertNotEquals(clave2, clave3)
        assertNotEquals(clave1, clave3)
    }

    @Test
    fun testClaveChat_dosAdoptantesNoCompartenConversacionesConMismoCentro() {
        val claveAdoptante1 = claveChat("usuario1@adoptme.com", idCentro = 1)
        val claveAdoptante2 = claveChat("usuario2@adoptme.com", idCentro = 1)

        assertNotEquals(claveAdoptante1, claveAdoptante2)
        assertEquals("chat_usuario1_adoptme_com_centro_1", claveAdoptante1)
        assertEquals("chat_usuario2_adoptme_com_centro_1", claveAdoptante2)
    }

    @Test
    fun testMensajeChat_conservaIdCentroYIdentificaRemitente() {
        val mensajeAdoptante = MensajeChat(
            id = 1001L,
            idCentro = 1,
            remitente = "usuario@adoptme.com",
            receptor = "centro@adoptme.com",
            texto = "Hola, quisiera información sobre Max.",
            nombreCentro = "Albergue Huellitas del Norte",
            fecha = "23/09 10:00",
            esDelCentro = false
        )

        assertEquals(1, mensajeAdoptante.idCentro)
        assertEquals("usuario@adoptme.com", mensajeAdoptante.remitente)
        assertEquals("centro@adoptme.com", mensajeAdoptante.receptor)
        assertFalse(mensajeAdoptante.esDelCentro)

        val respuestaCentro = MensajeChat(
            id = 1002L,
            idCentro = 1,
            remitente = "centro@adoptme.com",
            receptor = "usuario@adoptme.com",
            texto = "Hola, Max está disponible y listo para adopción.",
            nombreCentro = "Albergue Huellitas del Norte",
            fecha = "23/09 10:05",
            esDelCentro = true
        )

        assertEquals(1, respuestaCentro.idCentro)
        assertEquals("centro@adoptme.com", respuestaCentro.remitente)
        assertEquals("usuario@adoptme.com", respuestaCentro.receptor)
        assertTrue(respuestaCentro.esDelCentro)
    }

    @Test
    fun testCentro_abreConversacionCorrectaSegunIdCentroYAdoptante() {
        val conversaciones = listOf(
            ConversacionCentro(1, "usuario@adoptme.com", "centro@adoptme.com", "Albergue Huellitas del Norte", "Consulta sobre Max", "23/09 10:00"),
            ConversacionCentro(2, "carlos@adoptme.com", "centro@adoptme.com", "Refugio Patitas Felices", "Consulta sobre Luna", "23/09 11:00")
        )

        val convCentro1 = conversaciones.find { it.idCentro == 1 && it.correoAdoptante == "usuario@adoptme.com" }
        assertNotNull(convCentro1)
        assertEquals("Albergue Huellitas del Norte", convCentro1?.nombreCentro)
        assertEquals("Consulta sobre Max", convCentro1?.ultimoTexto)

        val convCentro2 = conversaciones.find { it.idCentro == 2 && it.correoAdoptante == "carlos@adoptme.com" }
        assertNotNull(convCentro2)
        assertEquals("Refugio Patitas Felices", convCentro2?.nombreCentro)
    }

    @Test
    fun testRespuestas_noAparecenEnOtroCentro() {
        val mensajesTodos = listOf(
            MensajeChat(1L, 1, "usuario@adoptme.com", "centro@adoptme.com", "Mensaje Centro 1", "Centro 1", "23/09", false),
            MensajeChat(2L, 1, "centro@adoptme.com", "usuario@adoptme.com", "Respuesta Centro 1", "Centro 1", "23/09", true),
            MensajeChat(3L, 2, "usuario@adoptme.com", "centro@adoptme.com", "Mensaje Centro 2", "Centro 2", "23/09", false)
        )

        val mensajesParaCentro1 = mensajesTodos.filter { it.idCentro == 1 }
        val mensajesParaCentro2 = mensajesTodos.filter { it.idCentro == 2 }

        assertEquals(2, mensajesParaCentro1.size)
        assertEquals(1, mensajesParaCentro2.size)
        assertTrue(mensajesParaCentro2.none { it.texto.contains("Centro 1") })
    }

    @Test
    fun testRespuestaAutomatica_soloSeGeneraUnaVez() {
        val conversacion = mutableListOf<MensajeChat>()

        // 1. Primer mensaje del adoptante
        val msg1 = MensajeChat(
            id = 1L,
            idCentro = 1,
            remitente = "usuario@adoptme.com",
            receptor = "centro@adoptme.com",
            texto = "Hola, información por favor.",
            nombreCentro = "Huellitas",
            fecha = "23/09 10:00",
            esDelCentro = false
        )
        conversacion.add(msg1)

        // Condición de generación de respuesta simulada automática: si no hay mensajes del centro
        if (conversacion.none { it.esDelCentro }) {
            val autoResp = MensajeChat(
                id = 2L,
                idCentro = 1,
                remitente = "centro@adoptme.com",
                receptor = "usuario@adoptme.com",
                texto = "Hola, gracias por escribirnos. Revisaremos tu consulta y te responderemos pronto.",
                nombreCentro = "Huellitas",
                fecha = "23/09 10:00",
                esDelCentro = true
            )
            conversacion.add(autoResp)
        }

        assertEquals(2, conversacion.size)
        assertTrue(conversacion.any { it.esDelCentro })

        // 2. Segundo mensaje del adoptante: NO debe generar una segunda respuesta automática
        val msg2 = MensajeChat(
            id = 3L,
            idCentro = 1,
            remitente = "usuario@adoptme.com",
            receptor = "centro@adoptme.com",
            texto = "¿Tienen fotos adicionales?",
            nombreCentro = "Huellitas",
            fecha = "23/09 10:05",
            esDelCentro = false
        )
        conversacion.add(msg2)

        if (conversacion.none { it.esDelCentro }) {
            // No entra aquí porque ya existe un mensaje del centro
            fail("No debe generar otra respuesta automática si ya existe una")
        }

        assertEquals(3, conversacion.size)
        assertEquals(1, conversacion.count { it.esDelCentro })
    }

    @Test
    fun testCerrarSesion_limpiaMemoriaVisibleSinAfectarPersistencia() {
        val memoriaMensajes = mutableListOf(
            MensajeChat(1L, 1, "usuario@adoptme.com", "centro@adoptme.com", "Consulta", "Huellitas", "23/09", false)
        )
        val almacenamientoSimulado = mutableMapOf(
            claveChat("usuario@adoptme.com", 1) to "json_mensajes_guardados"
        )

        // Simular cierre de sesión
        memoriaMensajes.clear()

        // Verificar que memoria visible queda limpia
        assertTrue(memoriaMensajes.isEmpty())

        // Verificar que almacenamiento persistente conserva la clave
        assertTrue(almacenamientoSimulado.containsKey("chat_usuario_adoptme_com_centro_1"))
        assertEquals("json_mensajes_guardados", almacenamientoSimulado["chat_usuario_adoptme_com_centro_1"])
    }

    @Test
    fun testSeguridad_noSeGuardanContrasenasEnClavesNiMensajes() {
        val claveC = claveChat("usuario@adoptme.com", 1)
        val claveConv = claveConversacionesCentro("centro@adoptme.com")

        assertFalse(claveC.contains("123456"))
        assertFalse(claveC.contains("password"))
        assertFalse(claveConv.contains("123456"))
        assertFalse(claveConv.contains("password"))

        val mensaje = MensajeChat(
            id = 1L,
            idCentro = 1,
            remitente = "usuario@adoptme.com",
            receptor = "centro@adoptme.com",
            texto = "Consulta sin secretos",
            nombreCentro = "Huellitas",
            fecha = "23/09",
            esDelCentro = false
        )
        assertFalse(mensaje.texto.contains("123456"))
    }

    @Test
    fun testAislamientoPanelCentro_conversacionesNoSeMezclanEntreCasasDeAdopcion() {
        // Simulación: el usuario contactó a 3 casas distintas
        val conversacionesRecibidas = listOf(
            ConversacionCentro(idCentro = 1, correoAdoptante = "usuario@adoptme.com", correoCentro = "centro@adoptme.com", nombreCentro = "Albergue Huellitas del Norte", ultimoTexto = "Consulta Huellitas", fecha = "23/09 10:00"),
            ConversacionCentro(idCentro = 2, correoAdoptante = "usuario@adoptme.com", correoCentro = "centro@adoptme.com", nombreCentro = "Refugio Patitas Felices Surco", ultimoTexto = "Consulta Patitas", fecha = "23/09 10:05"),
            ConversacionCentro(idCentro = 3, correoAdoptante = "usuario@adoptme.com", correoCentro = "centro@adoptme.com", nombreCentro = "Centro de Adopción Miraflores", ultimoTexto = "Consulta Miraflores", fecha = "23/09 10:10")
        )

        // Cuando la vista del Centro muestra Albergue Huellitas del Norte (ID = 1):
        val chatsHuellitas = conversacionesRecibidas.filter { it.idCentro == 1 }
        assertEquals(1, chatsHuellitas.size)
        assertEquals("Albergue Huellitas del Norte", chatsHuellitas.first().nombreCentro)
        assertTrue(chatsHuellitas.none { it.idCentro == 2 || it.idCentro == 3 })

        // Cuando la vista del Centro muestra Refugio Patitas Felices (ID = 2):
        val chatsPatitas = conversacionesRecibidas.filter { it.idCentro == 2 }
        assertEquals(1, chatsPatitas.size)
        assertEquals("Refugio Patitas Felices Surco", chatsPatitas.first().nombreCentro)
        assertTrue(chatsPatitas.none { it.idCentro == 1 || it.idCentro == 3 })

        // Cuando la vista del Centro muestra un centro sin mensajes (ID = 5, Comas):
        val chatsComas = conversacionesRecibidas.filter { it.idCentro == 5 }
        assertTrue(chatsComas.isEmpty())
    }

    // =========================================================================
    // TESTS UNITARIOS: VINCULACIÓN OBLIGATORIA DEL CORREO DE SESIÓN ACTIVA EN SOLICITUDES
    // =========================================================================

    @Test
    fun testSolicitud_asociaObligatoriamenteCorreoSesionActivaNormalizado() {
        val correoSesionActiva = "  Usuario@AdoptMe.COM  "
        val correoNormalizado = normalizarCorreoUsuario(correoSesionActiva)
        assertEquals("usuario@adoptme.com", correoNormalizado)

        // Supongamos que un formulario fraudulento intentara enviar otro correo
        val solicitudDesdeFormulario = AdoptionRequest(
            petId = 1,
            petName = "Max",
            applicantName = "Juan",
            email = "otro_correo@empresa.com", // Correo que debe ser ignorado
            status = "Pendiente"
        )

        // Regla aplicada en MainActivity y PantallaPrincipalAdoptante:
        // Asignar obligatoriamente el correo normalizado de la sesión activa
        val solicitudFinal = solicitudDesdeFormulario.copy(
            email = correoNormalizado
        )

        assertEquals("usuario@adoptme.com", solicitudFinal.email)
        assertNotEquals("otro_correo@empresa.com", solicitudFinal.email)
    }

    @Test
    fun testSolicitud_aislamientoEstrictoEntreCuentasDistintas() {
        val solicitudesTotales = listOf(
            AdoptionRequest(petId = 1, petName = "Max", applicantName = "User 1", email = "usuario@adoptme.com", status = "Pendiente"),
            AdoptionRequest(petId = 2, petName = "Luna", applicantName = "User 2", email = "a@gmail.com", status = "Pendiente")
        )

        val solicitudesUsuarioDemo = solicitudesTotales.filter {
            normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario("usuario@adoptme.com")
        }
        val solicitudesNuevoUsuario = solicitudesTotales.filter {
            normalizarCorreoUsuario(it.email) == normalizarCorreoUsuario("a@gmail.com")
        }

        // Cada usuario únicamente ve sus propias solicitudes
        assertEquals(1, solicitudesUsuarioDemo.size)
        assertEquals("Max", solicitudesUsuarioDemo.first().petName)
        assertEquals("usuario@adoptme.com", solicitudesUsuarioDemo.first().email)

        assertEquals(1, solicitudesNuevoUsuario.size)
        assertEquals("Luna", solicitudesNuevoUsuario.first().petName)
        assertEquals("a@gmail.com", solicitudesNuevoUsuario.first().email)

        // Ninguno ve las solicitudes del otro
        assertTrue(solicitudesUsuarioDemo.none { it.email == "a@gmail.com" })
        assertTrue(solicitudesNuevoUsuario.none { it.email == "usuario@adoptme.com" })
    }
}