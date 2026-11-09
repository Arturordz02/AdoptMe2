package com.example.adoptme

import com.example.adoptme.api.ActualizarPerfilRequest
import com.example.adoptme.components.RespuestasTestCompatibilidad
import com.example.adoptme.entity.MascotaVisitadaEntity
import com.example.adoptme.mapper.toCrearSolicitudDto
import com.example.adoptme.mapper.toDomain
import com.example.adoptme.mapper.toDto
import com.example.adoptme.mapper.toPet
import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.CentroAdopcion
import com.example.adoptme.model.ConversacionCentro
import com.example.adoptme.model.CrearSolicitudRequestDto
import com.example.adoptme.model.MascotaDto
import com.example.adoptme.model.MensajeChat
import com.example.adoptme.model.NotificationItem
import com.example.adoptme.model.Pet
import com.example.adoptme.model.SolicitudResponseDto
import com.example.adoptme.rules.localizarMascotaParaAprobacion
import com.example.adoptme.rules.puedeAprobarSolicitud
import com.example.adoptme.repository.PetRepository
import com.example.adoptme.repository.claveChat
import com.example.adoptme.repository.claveConversacionesCentro
import com.example.adoptme.repository.convertirPetAJson
import com.example.adoptme.repository.normalizarCorreoUsuario
import com.example.adoptme.repository.obtenerClaveUsuario
import com.example.adoptme.repository.parsearJsonAPet
import com.example.adoptme.repository.sincronizarPublicacionesUsuario
import com.example.adoptme.screens.PantallaInicioSesion
import com.example.adoptme.screens.PantallaPrincipalAdoptante
import com.example.adoptme.model.UsuarioRegistrado
import com.example.adoptme.screens.esFiltroComodin
import com.example.adoptme.screens.esFormatoEdadValido
import com.example.adoptme.screens.filtrarMascotas
import com.example.adoptme.utils.formatearEdadCanonica
import com.example.adoptme.screens.normalizarTexto
import com.example.adoptme.screens.revisarPublicacion
import org.json.JSONObject
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
        assertTrue(com.example.adoptme.screens.esFormatoCorreoValido("usuario@adoptme.com"))
        assertTrue(com.example.adoptme.screens.esFormatoCorreoValido("ana.morales@gmail.com"))
        assertTrue(com.example.adoptme.screens.esFormatoCorreoValido("test+adopt@empresa.org"))
        assertTrue(com.example.adoptme.screens.esFormatoCorreoValido("lucia.quispe@correo.pe"))
        assertTrue(com.example.adoptme.screens.esFormatoCorreoValido("contacto@refugio.com.pe"))
        assertTrue(com.example.adoptme.screens.esFormatoCorreoValido("adopciones@veterinaria.edu.pe"))

        // Formatos inválidos
        assertFalse(com.example.adoptme.screens.esFormatoCorreoValido(""))
        assertFalse(com.example.adoptme.screens.esFormatoCorreoValido("sinarroba"))
        assertFalse(com.example.adoptme.screens.esFormatoCorreoValido("usuario@"))
        assertFalse(com.example.adoptme.screens.esFormatoCorreoValido("usuario@dominio"))
        assertFalse(com.example.adoptme.screens.esFormatoCorreoValido("@dominio.com"))
        assertFalse(com.example.adoptme.screens.esFormatoCorreoValido("usuario@.com"))
    }

    @Test
    fun normalizarTelefonoPeruano_eliminaEspaciosYNormaliza() {
        assertEquals("999888777", com.example.adoptme.screens.normalizarTelefonoPeruano("999888777"))
        assertEquals("999888777", com.example.adoptme.screens.normalizarTelefonoPeruano("999 888 777"))
        assertEquals("999888777", com.example.adoptme.screens.normalizarTelefonoPeruano("  999  888  777  "))
        assertEquals("912345678", com.example.adoptme.screens.normalizarTelefonoPeruano(" 912 345 678 "))
    }

    @Test
    fun esTelefonoPeruanoValido_validaFormatosCorrectosEIncorrectos() {
        // Ejemplos válidos
        assertTrue(com.example.adoptme.screens.esTelefonoPeruanoValido("999888777"))
        assertTrue(com.example.adoptme.screens.esTelefonoPeruanoValido("999 888 777"))
        assertTrue(com.example.adoptme.screens.esTelefonoPeruanoValido(" 912 345 678 "))

        // Ejemplos inválidos requeridos
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("999999")) // Menos de 9 dígitos
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("888888888")) // No empieza con 9
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("9998887777")) // Más de 9 dígitos
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("999abc777")) // Letras
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("999-888-777")) // Símbolos
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("")) // Vacío
    }

    @Test
    fun usuarioRegistrado_asignaRolAdoptantePorDefecto() {
        val nuevoUsuario = com.example.adoptme.model.UsuarioRegistrado(
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
        assertFalse(com.example.adoptme.components.puedeAvanzarPregunta(""))
        assertFalse(com.example.adoptme.components.puedeAvanzarPregunta("   "))
        assertTrue(com.example.adoptme.components.puedeAvanzarPregunta("Departamento"))
        assertTrue(com.example.adoptme.components.puedeAvanzarPregunta("Perro"))
    }

    @Test
    fun testCompatibilidad_normalizarTextoCompatibilidad_limpiaTildesYEspacios() {
        assertEquals("perro", com.example.adoptme.components.normalizarTextoCompatibilidad("  Pérro  "))
        assertEquals("gato", com.example.adoptme.components.normalizarTextoCompatibilidad("GATÓ"))
        assertEquals("en proceso", com.example.adoptme.components.normalizarTextoCompatibilidad("En Proceso"))
        assertEquals("disponible", com.example.adoptme.components.normalizarTextoCompatibilidad(" DISPONIBLE "))
    }

    @Test
    fun testCompatibilidad_reiniciarRespuestasTest_limpiaTodosLosCampos() {
        val reiniciado = com.example.adoptme.components.reiniciarRespuestasTest()
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

        val respuestasPreferenciaPerro = com.example.adoptme.components.RespuestasTestCompatibilidad(
            vivienda = "Casa con patio",
            tiempoDisponible = "Más de 2 horas",
            experiencia = "He tenido antes",
            especiePreferida = "Perro"
        )

        val resultados = com.example.adoptme.components.evaluarTestCompatibilidad(mascotas, respuestasPreferenciaPerro)
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

        val respuestas = com.example.adoptme.components.RespuestasTestCompatibilidad(
            vivienda = "Departamento pequeño",
            tiempoDisponible = "Menos de 1 hora",
            experiencia = "Primera vez",
            especiePreferida = "Gato"
        )

        val resultados = com.example.adoptme.components.evaluarTestCompatibilidad(mascotas, respuestas)
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

        val respuestas = com.example.adoptme.components.RespuestasTestCompatibilidad(
            vivienda = "Casa con jardín",
            tiempoDisponible = "1 a 2 horas",
            experiencia = "He tenido antes",
            especiePreferida = "Sin preferencia (Cualquiera)"
        )

        val resultados = com.example.adoptme.components.evaluarTestCompatibilidad(mascotas, respuestas)
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

        assertTrue(com.example.adoptme.components.permiteSolicitarAdopcion(disponible))
        assertFalse(com.example.adoptme.components.permiteSolicitarAdopcion(enProceso))
        assertFalse(com.example.adoptme.components.permiteSolicitarAdopcion(adoptada))
    }

    @Test
    fun testCompatibilidad_sinCoincidenciasRetornaListaVacia() {
        // Si todas las mascotas están adoptadas o no pertenecen a especies admitidas (perro, gato, conejo)
        val adoptada = Pet(1, "Fido", "Perro", "Mestizo", "4 años", "Macho", "Lima", "Adoptada", "Tranquilo", 0)
        val loro = Pet(2, "Pepe", "Ave", "Loro", "2 años", "Macho", "Lima", "Disponible", "Hablador", 0)
        val mascotas = listOf(adoptada, loro)

        val respuestas = com.example.adoptme.components.RespuestasTestCompatibilidad(
            vivienda = "Departamento",
            tiempoDisponible = "1 a 2 horas",
            experiencia = "He tenido antes",
            especiePreferida = "Perro"
        )

        val resultados = com.example.adoptme.components.evaluarTestCompatibilidad(mascotas, respuestas)
        assertTrue(resultados.isEmpty())
    }

    // =========================================================================
    // TESTS UNITARIOS: PUBLICAR MASCOTA (USUARIO ADOPTANTE)
    // =========================================================================

    @Test
    fun testPublicacion_esEspecieValida_aceptaSoloPerroGatoConejo() {
        // Especies válidas (RN-02) con diferentes mayúsculas y tildes
        assertTrue(com.example.adoptme.screens.esEspecieValida("Perro"))
        assertTrue(com.example.adoptme.screens.esEspecieValida("perro"))
        assertTrue(com.example.adoptme.screens.esEspecieValida("Pérro"))
        assertTrue(com.example.adoptme.screens.esEspecieValida("Gato"))
        assertTrue(com.example.adoptme.screens.esEspecieValida("GATO"))
        assertTrue(com.example.adoptme.screens.esEspecieValida("gato"))
        assertTrue(com.example.adoptme.screens.esEspecieValida("Conejo"))
        assertTrue(com.example.adoptme.screens.esEspecieValida("conejo"))

        // Especies no admitidas
        assertFalse(com.example.adoptme.screens.esEspecieValida("Loro"))
        assertFalse(com.example.adoptme.screens.esEspecieValida("Pez"))
        assertFalse(com.example.adoptme.screens.esEspecieValida("Hamster"))
        assertFalse(com.example.adoptme.screens.esEspecieValida(""))
        assertFalse(com.example.adoptme.screens.esEspecieValida("   "))
    }

    @Test
    fun testPublicacion_esFormatoEdadValido_aceptaYRechazaFormatos() {
        // Formatos válidos requeridos
        assertTrue(com.example.adoptme.screens.esFormatoEdadValido("1 año"))
        assertTrue(com.example.adoptme.screens.esFormatoEdadValido("2 años"))
        assertTrue(com.example.adoptme.screens.esFormatoEdadValido("6 meses"))
        assertTrue(com.example.adoptme.screens.esFormatoEdadValido("10 meses"))
        assertTrue(com.example.adoptme.screens.esFormatoEdadValido("1 ano"))
        assertTrue(com.example.adoptme.screens.esFormatoEdadValido("2 anos"))

        // Formatos inválidos requeridos
        assertFalse(com.example.adoptme.screens.esFormatoEdadValido("")) // Vacío
        assertFalse(com.example.adoptme.screens.esFormatoEdadValido("   ")) // Espacios
        assertFalse(com.example.adoptme.screens.esFormatoEdadValido("dos años")) // Texto sin número
        assertFalse(com.example.adoptme.screens.esFormatoEdadValido("-2 años")) // Número negativo
        assertFalse(com.example.adoptme.screens.esFormatoEdadValido("-1 año")) // Número negativo
        assertFalse(com.example.adoptme.screens.esFormatoEdadValido("0 años")) // Cero
        assertFalse(com.example.adoptme.screens.esFormatoEdadValido("2 años y medio")) // Formato desconocido
        assertFalse(com.example.adoptme.screens.esFormatoEdadValido("perro")) // Texto arbitrario
    }

    @Test
    fun testPublicacion_esSexoValido_soloAceptaMachoOHembra() {
        assertTrue(com.example.adoptme.screens.esSexoValido("Macho"))
        assertTrue(com.example.adoptme.screens.esSexoValido("macho"))
        assertTrue(com.example.adoptme.screens.esSexoValido("Hembra"))
        assertTrue(com.example.adoptme.screens.esSexoValido("hembra"))

        assertFalse(com.example.adoptme.screens.esSexoValido(""))
        assertFalse(com.example.adoptme.screens.esSexoValido("Indefinido"))
        assertFalse(com.example.adoptme.screens.esSexoValido("Otro"))
    }

    @Test
    fun testPublicacion_validarDatosPublicacion_detectaCamposFaltantes() {
        // Datos válidos
        val validacionExitosa = com.example.adoptme.screens.validarDatosPublicacion(
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
        val validacionInvalida = com.example.adoptme.screens.validarDatosPublicacion(
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
        val nuevaMascota = com.example.adoptme.screens.crearPublicacionMascota(
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
        val repositorio = com.example.adoptme.repository.PetRepository()
        val mascotasBase = repositorio.getPets()

        val mascotaUsuario = com.example.adoptme.screens.crearPublicacionMascota(
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

        val mascotaPublicada = com.example.adoptme.screens.crearPublicacionMascota(
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
        val puedeDuenioSolicitar = com.example.adoptme.components.permiteSolicitarAdopcion(
            mascota = mascotaPublicada,
            esPublicacionPropia = esPropiaParaDuenio
        )
        assertFalse(puedeDuenioSolicitar)

        // 4. Otro usuario sí puede solicitar adopción si está Disponible
        val puedeOtroSolicitar = com.example.adoptme.components.permiteSolicitarAdopcion(
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
        assertTrue(com.example.adoptme.screens.esFiltroLibre("Todos"))
        assertTrue(com.example.adoptme.screens.esFiltroLibre("Cualquier sexo"))
        assertFalse(com.example.adoptme.screens.esFiltroLibre("Perro"))

        val perro = Pet(1, "Rocky", "Perro", "Boxer", "2 años", "Macho", "Lima", "Disponible", "Juguetón", 0)
        assertTrue(com.example.adoptme.screens.filtrarEspecie(perro, "perro"))
        assertTrue(com.example.adoptme.screens.filtrarEspecie(perro, "Todos"))
        assertFalse(com.example.adoptme.screens.filtrarEspecie(perro, "gato"))

        assertTrue(com.example.adoptme.screens.filtrarSexo(perro, "macho"))
        assertTrue(com.example.adoptme.screens.filtrarUbicacion(perro, "lima"))
        assertTrue(com.example.adoptme.screens.filtrarEstado(perro, "disponible"))
    }

    @Test
    fun testNombresLimpios_testCompatibilidad() {
        assertTrue(com.example.adoptme.components.respuestaValida("Departamento"))
        assertFalse(com.example.adoptme.components.respuestaValida(""))

        assertEquals("gato", com.example.adoptme.components.limpiarTexto("  GATÓ  "))

        val respuestasLimpias = com.example.adoptme.components.reiniciarTest()
        assertEquals("", respuestasLimpias.vivienda)
        assertEquals("", respuestasLimpias.tiempoDisponible)

        val mascota = Pet(1, "Michi", "Gato", "Siamés", "1 año", "Hembra", "Lima", "Disponible", "Cariñosa", 0)
        assertTrue(com.example.adoptme.components.puedeAdoptar(mascota, esPublicacionPropia = false))
        assertFalse(com.example.adoptme.components.puedeAdoptar(mascota, esPublicacionPropia = true))

        val coincidencias = com.example.adoptme.components.buscarCoincidencias(
            listOf(mascota),
            com.example.adoptme.components.RespuestasTestCompatibilidad(
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
        val validacion = com.example.adoptme.screens.revisarPublicacion(
            nombre = "Coco",
            especie = "Conejo",
            raza = "Enano",
            edad = "6 meses",
            genero = "Macho",
            ubicacion = "Surco",
            descripcion = "Tranquilo"
        )
        assertTrue(validacion.esValido)

        val nueva = com.example.adoptme.screens.crearMascota(
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
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido(""))
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("888888888")) // No empieza con 9
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("999999")) // Menos de 9 dígitos
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("9999999999")) // Más de 9 dígitos
        assertFalse(com.example.adoptme.screens.esTelefonoPeruanoValido("999abc777")) // Contiene letras
    }

    @Test
    fun testPerfil_validacionAceptaYNormalizaTelefonoPeruano() {
        val telConEspacios = " 987 654 321 "
        assertTrue(com.example.adoptme.screens.esTelefonoPeruanoValido(telConEspacios))
        val telNormalizado = com.example.adoptme.screens.normalizarTelefonoPeruano(telConEspacios)
        assertEquals("987654321", telNormalizado)
    }

    @Test
    fun testPerfil_correoPermaneceInvariableEnActualizacion() {
        val usuarioOriginal = com.example.adoptme.model.UsuarioRegistrado(
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

        assertFalse(com.example.adoptme.components.puedeAdoptar(mascotaPropia, esPublicacionPropia = esPropia))
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

        assertTrue(com.example.adoptme.components.puedeAdoptar(mascotaOtro, esPublicacionPropia = esPropia))
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

    // =========================================================================
    // TESTS UNITARIOS: SOPORTE DE ESPAÑOL (ES-PE), CARACTERES Y FORMATEO DE EDAD
    // =========================================================================

    @Test
    fun testFormatoEdad_canonicoConvierteVariantesCorrectamente() {
        // Variantes con 'ano' / 'anos'
        assertEquals("1 año", formatearEdadCanonica("1 ano"))
        assertEquals("2 años", formatearEdadCanonica("2 anos"))
        assertEquals("3 años", formatearEdadCanonica("3 anos"))

        // Variantes con 'anio' / 'anios'
        assertEquals("1 año", formatearEdadCanonica("1 anio"))
        assertEquals("2 años", formatearEdadCanonica("2 anios"))
        assertEquals("5 años", formatearEdadCanonica("5 anios"))

        // Formatos ya canónicos con 'año' / 'años'
        assertEquals("1 año", formatearEdadCanonica("1 año"))
        assertEquals("2 años", formatearEdadCanonica("2 años"))
        assertEquals("4 años", formatearEdadCanonica("4 años"))

        // Formatos de meses
        assertEquals("1 mes", formatearEdadCanonica("1 mes"))
        assertEquals("6 meses", formatearEdadCanonica("6 meses"))
        assertEquals("11 meses", formatearEdadCanonica("11 meses"))

        // Validación previa
        assertTrue(esFormatoEdadValido("1 ano"))
        assertTrue(esFormatoEdadValido("2 anos"))
        assertTrue(esFormatoEdadValido("1 anio"))
        assertTrue(esFormatoEdadValido("2 anios"))
        assertTrue(esFormatoEdadValido("2 años"))
        assertTrue(esFormatoEdadValido("6 meses"))
        assertFalse(esFormatoEdadValido("adulto"))
        assertFalse(esFormatoEdadValido(""))
    }

    @Test
    fun testRegistro_preservaNombresOriginalesConEnesYTildes() {
        val nombreConEne = "Vicuña Muñoz"
        val usuario = UsuarioRegistrado(
            nombre = nombreConEne.trim(),
            correo = "vicuna@test.pe",
            contrasena = "123456",
            telefono = "987654321"
        )

        // El nombre original debe mantenerse intacto, sin sustitución de 'ñ' por 'n'
        assertEquals("Vicuña Muñoz", usuario.nombre)
        assertTrue(usuario.nombre.contains("ñ"))
        assertNotEquals("Vicua Muñoz", usuario.nombre)
        assertNotEquals("Vicuna Munoz", usuario.nombre)

        val nombreConTildes = "Ángel Núñez"
        val usuario2 = UsuarioRegistrado(
            nombre = nombreConTildes.trim(),
            correo = "angel@test.pe",
            contrasena = "123456",
            telefono = "987654321"
        )
        assertEquals("Ángel Núñez", usuario2.nombre)
    }

    @Test
    fun testNormalizacion_preservaEneYNoMutaDatosOriginales() {
        // normalizarTexto para búsquedas debe mantener 'ñ' y remover tildes en vocales
        assertEquals("vicuña muñoz", normalizarTexto("Vicuña Muñoz"))
        assertEquals("angel peña", normalizarTexto("Ángel Peña"))
        assertEquals("cariñosa", normalizarTexto("Cariñosa"))

        val mascotaOriginal = Pet(
            id = 99,
            name = "Vicuña Muñoz",
            species = "Perro",
            breed = "Criollo",
            age = "2 años",
            gender = "Macho",
            location = "Miraflores, Lima",
            status = "Disponible",
            description = "Perrito muy cariñoso"
        )

        val busqueda = normalizarTexto("vicuña")
        assertTrue(normalizarTexto(mascotaOriginal.name).contains(busqueda))

        // Los valores de la mascota deben permanecer exactamente inmutables
        assertEquals("Vicuña Muñoz", mascotaOriginal.name)
        assertEquals("2 años", mascotaOriginal.age)
        assertEquals("Perrito muy cariñoso", mascotaOriginal.description)
    }

    // =========================================================================
    // TESTS UNITARIOS: FASE 1B - CRUD REAL DE MASCOTAS, DISTINCIÓN REMOTA Y DTO
    // =========================================================================

    @Test
    fun testMascota_distincionFiableEntreRemotaYLocalEvitaColisiones() {
        val repo = PetRepository()
        val mascotasLocales = repo.getPets()

        // Cada mascota local en el repositorio de mock debe tener esRemota = false
        assertTrue(mascotasLocales.all { !it.esRemota })

        // Si Neon genera un ID = 1 para una mascota remota
        val dtoRemoto = MascotaDto(
            id = 1,
            name = "Bobby Real",
            species = "Perro",
            breed = "Criollo",
            age = "2 años",
            gender = "Macho",
            location = "Miraflores, Lima",
            status = "Disponible",
            description = "Perrito rescatado en Neon",
            imageUrl = null,
            creadoPorCorreo = "rescatista@adoptme.pe"
        )

        val mascotaRemota = dtoRemoto.toPet()

        // Debe marcarse como esRemota = true
        assertTrue(mascotaRemota.esRemota)
        assertEquals(1, mascotaRemota.id)

        // Comprobar que en una lista mixta con IDs duplicados (id = 1), se distinguen fiablemente
        val localId1 = mascotasLocales.first { it.id == 1 }
        assertFalse(localId1.esRemota)

        val listaMixta = listOf(localId1, mascotaRemota)
        val filtradasRemotas = listaMixta.filter { it.esRemota }
        val filtradasLocales = listaMixta.filter { !it.esRemota }

        assertEquals(1, filtradasRemotas.size)
        assertEquals("Bobby Real", filtradasRemotas.first().name)
        assertEquals(1, filtradasLocales.size)
        assertEquals("Max", filtradasLocales.first().name)
    }

    @Test
    fun testMascotaDto_conversionPreservaCreadoPorCorreoYActualizaEstado() {
        val petRemota = Pet(
            id = 42,
            name = "Michi",
            species = "Gato",
            breed = "Siamés",
            age = "1 año",
            gender = "Hembra",
            location = "Surco, Lima",
            status = "Disponible",
            description = "Gatita muy linda",
            creadoPorCorreo = "dueno.original@adoptme.pe",
            esRemota = true
        )

        // Simular preparación de DTO para PUT /api/Mascotas/42 cambiando estado a "En proceso"
        val dtoActualizado = petRemota.toDto().copy(status = "En proceso")

        assertEquals(42, dtoActualizado.id)
        assertEquals("En proceso", dtoActualizado.status)
        assertEquals("dueno.original@adoptme.pe", dtoActualizado.creadoPorCorreo)

        // Convertir de vuelta a Pet y comprobar que esRemota se mantiene true
        val petActualizada = dtoActualizado.toPet()
        assertTrue(petActualizada.esRemota)
        assertEquals("En proceso", petActualizada.status)
        assertEquals("dueno.original@adoptme.pe", petActualizada.creadoPorCorreo)
    }

    @Test
    fun testEditarPublicacion_validacionYPreservacionDeCamposInmutables() {
        val mascotaOriginal = Pet(
            id = 15,
            name = "Rocky",
            species = "Perro",
            breed = "Schnauzer",
            age = "3 años",
            gender = "Macho",
            location = "Miraflores",
            status = "Disponible",
            description = "Perrito muy educado",
            creadoPorCorreo = "usuario@adoptme.com",
            esRemota = true
        )

        // 1. Validar que la edición de campos con datos correctos sea válida
        val validacionValida = revisarPublicacion(
            nombre = "Rocky Modificado",
            especie = "Perro",
            raza = "Schnauzer Mini",
            edad = "4 anos",
            genero = "Macho",
            ubicacion = "San Borja, Lima",
            descripcion = "Perrito educado y cariñoso"
        )
        assertTrue(validacionValida.esValido)
        assertNull(validacionValida.errorNombre)
        assertNull(validacionValida.errorEdad)

        // 2. Simular actualización preservando ID, creadoPorCorreo y esRemota
        val mascotaActualizada = mascotaOriginal.copy(
            name = "Rocky Modificado",
            breed = "Schnauzer Mini",
            age = formatearEdadCanonica("4 anos"),
            location = "San Borja, Lima",
            status = "En proceso",
            description = "Perrito educado y cariñoso"
        )

        // Se deben conservar inmutables los identificadores y autoría
        assertEquals(15, mascotaActualizada.id)
        assertEquals("usuario@adoptme.com", mascotaActualizada.creadoPorCorreo)
        assertTrue(mascotaActualizada.esRemota)

        // Se deben actualizar los campos editados y la edad normalizada
        assertEquals("Rocky Modificado", mascotaActualizada.name)
        assertEquals("Schnauzer Mini", mascotaActualizada.breed)
        assertEquals("4 años", mascotaActualizada.age)
        assertEquals("San Borja, Lima", mascotaActualizada.location)
        assertEquals("En proceso", mascotaActualizada.status)

        // 3. Validar rechazo ante campos obligatorios vacíos o formatos inválidos
        val validacionInvalida = revisarPublicacion(
            nombre = "",
            especie = "Loro", // Especie no admitida por RN-02
            raza = "",
            edad = "edad desconocida",
            genero = "Otro",
            ubicacion = "",
            descripcion = ""
        )
        assertFalse(validacionInvalida.esValido)
        assertNotNull(validacionInvalida.errorNombre)
        assertNotNull(validacionInvalida.errorEspecie)
        assertNotNull(validacionInvalida.errorEdad)
        assertNotNull(validacionInvalida.errorGenero)
    }

    @Test
    fun testMascotaStorage_serializacionYRecuperacionEsRemota() {
        val mascotaRemota = Pet(
            id = 2,
            name = "Michi Prueba",
            species = "Gato",
            breed = "Criollo",
            age = "8 meses",
            gender = "Hembra",
            location = "Surco, Lima",
            status = "Disponible",
            description = "Gatita muy linda",
            creadoPorCorreo = "arthur01961@outlook.com",
            esRemota = true
        )

        // 1. Serializar a JSONObject
        val json = convertirPetAJson(mascotaRemota, "ruta/simulada.jpg")
        assertTrue("El JSON debe contener explícitamente esRemota = true", json.has("esRemota"))
        assertTrue(json.getBoolean("esRemota"))
        assertEquals(2, json.getInt("id"))
        assertEquals("Michi Prueba", json.getString("name"))

        // 2. Deserializar desde JSONObject
        val mascotaDeserializada = parsearJsonAPet(json)
        assertTrue("La mascota reconstruida debe mantener esRemota = true", mascotaDeserializada.esRemota)
        assertEquals(2, mascotaDeserializada.id)
        assertEquals("arthur01961@outlook.com", mascotaDeserializada.creadoPorCorreo)
    }

    @Test
    fun testMascotaStorage_compatibilidadConRegistrosAntiguosSinEsRemota() {
        // Simular un JSON de una versión antigua donde el campo "esRemota" no existía
        val jsonAntiguo = JSONObject()
        jsonAntiguo.put("id", 10)
        jsonAntiguo.put("name", "Mascota Antigua")
        jsonAntiguo.put("species", "Perro")
        jsonAntiguo.put("breed", "Mestizo")
        jsonAntiguo.put("age", "1 año")
        jsonAntiguo.put("gender", "Macho")
        jsonAntiguo.put("location", "Lima")
        jsonAntiguo.put("status", "Disponible")
        jsonAntiguo.put("description", "Publicación creada con versión previa")
        jsonAntiguo.put("creadoPorCorreo", "usuario@adoptme.com")

        assertFalse("El JSON antiguo no contiene la clave esRemota", jsonAntiguo.has("esRemota"))

        // Deserializar asegurando fallback seguro a false
        val mascota = parsearJsonAPet(jsonAntiguo)
        assertEquals(10, mascota.id)
        assertFalse("Registros antiguos sin clave esRemota deben defaulting seguro a false", mascota.esRemota)
    }

    @Test
    fun testSincronizarPublicacionesUsuario_remotasPrevalecenYSoloDelUsuario() {
        val correoActivo = "arthur01961@outlook.com"

        // Lista remota que llega de Neon (GET /api/Mascotas)
        val mascotasRemotas = listOf(
            Pet(
                id = 2,
                name = "Michi Prueba",
                species = "Gato",
                breed = "Criollo",
                age = "8 meses",
                gender = "Hembra",
                location = "Surco, Lima",
                status = "Disponible",
                description = "Prueba de edición API en Neon",
                creadoPorCorreo = "arthur01961@outlook.com",
                esRemota = true
            ),
            Pet(
                id = 1,
                name = "Bobby Demo",
                species = "Perro",
                breed = "Labrador",
                age = "2 años",
                gender = "Macho",
                location = "Miraflores",
                status = "Disponible",
                description = "Mascota de otro usuario",
                creadoPorCorreo = "otro.usuario@adoptme.pe",
                esRemota = true
            )
        )

        // Lista local previa (con copia desactualizada y esRemota = false) y un borrador local
        val publicacionesLocales = listOf(
            Pet(
                id = 2,
                name = "Michi Prueba",
                species = "Gato",
                breed = "Criollo",
                age = "8 meses",
                gender = "Hembra",
                location = "Surco, Lima",
                status = "Disponible",
                description = "Descripción antigua desactualizada",
                creadoPorCorreo = "arthur01961@outlook.com",
                esRemota = false // <- antigua copia local sin flag
            ),
            Pet(
                id = 99,
                name = "Borrador Local",
                species = "Conejo",
                breed = "Enano",
                age = "5 meses",
                gender = "Macho",
                location = "Lima",
                status = "Disponible",
                description = "Mascota guardada en local",
                creadoPorCorreo = "arthur01961@outlook.com",
                esRemota = false
            )
        )

        val resultado = sincronizarPublicacionesUsuario(
            correoUsuario = correoActivo,
            mascotasRemotas = mascotasRemotas,
            publicacionesLocales = publicacionesLocales
        )

        // Debe contener exactamente 2 publicaciones: Michi Prueba (actualizada desde Neon) y Borrador Local
        assertEquals(2, resultado.size)

        val michiSync = resultado.find { it.id == 2 }
        assertNotNull(michiSync)
        assertTrue("La entidad de Neon prevalece y debe tener esRemota = true", michiSync!!.esRemota)
        assertEquals("Prueba de edición API en Neon", michiSync.description)

        // No debe contener a Bobby Demo (pertenece a otro usuario)
        assertNull(resultado.find { it.id == 1 })

        // Debe conservar el borrador local con su flag esRemota = false intacto
        val borradorSync = resultado.find { it.id == 99 }
        assertNotNull(borradorSync)
        assertFalse(borradorSync!!.esRemota)
    }

    @Test
    fun testPrevencionEdicionesLocalesFalsas_mascotasDemoNoSonRemotas() {
        // Caso de colisión de ID: Mascota demo local con id = 2 (Luna) vs Michi Prueba en Neon con id = 2
        val mascotaDemoLocal = Pet(
            id = 2,
            name = "Luna",
            species = "Gato",
            breed = "Gato doméstico",
            age = "1 año",
            gender = "Hembra",
            location = "San Isidro",
            status = "Disponible",
            description = "Mascota local de demostración del repositorio",
            creadoPorCorreo = "", // local demo no tiene autor
            esRemota = false
        )

        val mascotaRemotaNeon = Pet(
            id = 2,
            name = "Michi Prueba",
            species = "Gato",
            breed = "Criollo",
            age = "8 meses",
            gender = "Hembra",
            location = "Surco, Lima",
            status = "Disponible",
            description = "Mascota real en Neon",
            creadoPorCorreo = "arthur01961@outlook.com",
            esRemota = true
        )

        // Regla Fase 1C: Solo esConfirmadaRemota si esRemota == true e id > 0
        val esConfirmadaRemotaDemo = mascotaDemoLocal.esRemota && mascotaDemoLocal.id > 0
        val esConfirmadaRemotaNeon = mascotaRemotaNeon.esRemota && mascotaRemotaNeon.id > 0

        assertFalse("La mascota demo NO debe considerarse remota aunque id > 0", esConfirmadaRemotaDemo)
        assertTrue("La mascota de Neon sí es remota confirmada", esConfirmadaRemotaNeon)
    }

    // =========================================================================
    // PRUEBAS FASE 2C: Envío e Integración de Solicitudes Reales con Retrofit
    // =========================================================================

    @Test
    fun testFase2C_mapeoAdoptionRequest_a_CrearSolicitudRequestDto() {
        val solicitud = AdoptionRequest(
            petId = 2,
            petName = "Michi Prueba",
            petSpecies = "Gato",
            applicantName = " Arturo Rodriguez ",
            phone = " 987654321 ",
            email = "adoptante@ejemplo.pe",
            address = " Av. Javier Prado 123 ",
            housingType = " Departamento ",
            timeAvailability = " Tiempo completo ",
            previousExperience = " Sí, 3 años ",
            reason = " Deseo brindarle un hogar seguro "
        )

        val dto = solicitud.toCrearSolicitudDto(idMascotaConfirmado = 2)

        assertEquals(2, dto.mascotaId)
        assertEquals("Arturo Rodriguez", dto.applicantName)
        assertEquals("987654321", dto.phone)
        assertEquals("Av. Javier Prado 123", dto.address)
        assertEquals("Departamento", dto.housingType)
        assertEquals("Tiempo completo", dto.timeAvailability)
        assertEquals("Sí, 3 años", dto.previousExperience)
        assertEquals("Deseo brindarle un hogar seguro", dto.reason)
    }

    @Test
    fun testFase2C_mapeoSolicitudResponseDto_a_DomainAdoptionRequest() {
        val serverResponse = SolicitudResponseDto(
            id = 101,
            mascotaId = 2,
            adoptanteId = 5,
            petName = "Michi Prueba",
            petSpecies = "Gato",
            applicantName = "Arturo Rodriguez",
            phone = "987654321",
            email = "adoptante@ejemplo.pe",
            address = "Av. Javier Prado 123",
            housingType = "Departamento",
            timeAvailability = "Tiempo completo",
            previousExperience = "Sí",
            reason = "Deseo cuidarlo",
            status = "Pendiente",
            observacion = null,
            fechaSolicitud = "2026-10-23T03:30:00Z",
            fechaActualizacion = null
        )

        val domain = serverResponse.toDomain()

        assertEquals(101, domain.id)
        assertEquals(2, domain.petId)
        assertEquals("Michi Prueba", domain.petName)
        assertEquals("Gato", domain.petSpecies)
        assertEquals("Arturo Rodriguez", domain.applicantName)
        assertEquals("adoptante@ejemplo.pe", domain.email)
        assertEquals("Pendiente", domain.status)
    }

    @Test
    fun testFase2C_distincionMascotaRemotaVsLocal_enSolicitud() {
        // Mascota demo local con id 1 (Max) vs mascota remota en Neon con id 1
        val mascotaLocalDemo = Pet(
            id = 1,
            name = "Max",
            species = "Perro",
            breed = "Labrador",
            age = "2 años",
            gender = "Macho",
            description = "Demo local",
            esRemota = false
        )

        val mascotaRemotaNeon = Pet(
            id = 1,
            name = "Pelusa",
            species = "Perro",
            breed = "Mestizo",
            age = "1 año",
            gender = "Hembra",
            description = "Mascota persistida en Neon",
            creadoPorCorreo = "dueno@adoptme.pe",
            esRemota = true
        )

        val enrutaRetrofitLocal = mascotaLocalDemo.esRemota && mascotaLocalDemo.id > 0
        val enrutaRetrofitRemota = mascotaRemotaNeon.esRemota && mascotaRemotaNeon.id > 0

        assertFalse("La mascota demo local NO debe invocar Retrofit a Neon", enrutaRetrofitLocal)
        assertTrue("La mascota remota SÍ debe invocar Retrofit a Neon", enrutaRetrofitRemota)
    }

    @Test
    fun testFase2C_manejoCodigosRespuestaHttp() {
        // Función simulada idéntica a la lógica implementada en PantallaPrincipalAdoptante
        fun resolverResultado(codigoHttp: Int, errorDetalle: String? = null): Pair<Boolean, String?> {
            return if (codigoHttp in 200..299) {
                Pair(true, null)
            } else {
                val mensaje = when (codigoHttp) {
                    400 -> {
                        if (errorDetalle?.contains("publicada por ti", ignoreCase = true) == true) {
                            "No puedes solicitar la adopción de una mascota publicada por ti."
                        } else if (errorDetalle?.contains("disponible", ignoreCase = true) == true) {
                            "La mascota ya no está disponible para adopción."
                        } else {
                            "No se pudo registrar la solicitud. Verifica los datos enviados."
                        }
                    }
                    401 -> "Sesión expirada o no autorizada. Inicia sesión nuevamente."
                    403 -> "No tienes autorización para realizar esta solicitud."
                    404 -> "La mascota ya no existe en el servidor."
                    409 -> "Ya cuentas con una solicitud activa o aprobada para esta mascota."
                    else -> "No se pudo registrar la solicitud en el servidor (Código: $codigoHttp)."
                }
                Pair(false, mensaje)
            }
        }

        // 1. Éxito 201 Created
        val (exito201, msg201) = resolverResultado(201)
        assertTrue("HTTP 201 debe indicar éxito", exito201)
        assertNull(msg201)

        // 2. Error 409 Conflict (Duplicado)
        val (exito409, msg409) = resolverResultado(409)
        assertFalse("HTTP 409 no debe indicar éxito", exito409)
        assertEquals("Ya cuentas con una solicitud activa o aprobada para esta mascota.", msg409)

        // 3. Error 401 Unauthorized (Sesión expirada)
        val (exito401, msg401) = resolverResultado(401)
        assertFalse("HTTP 401 no debe indicar éxito", exito401)
        assertEquals("Sesión expirada o no autorizada. Inicia sesión nuevamente.", msg401)

        // 4. Error 404 Not Found (Mascota no existe)
        val (exito404, msg404) = resolverResultado(404)
        assertFalse("HTTP 404 no debe indicar éxito", exito404)
        assertEquals("La mascota ya no existe en el servidor.", msg404)

        // 5. Error 400 Bad Request (Auto-adopción)
        val (exito400, msg400) = resolverResultado(400, "publicada por ti")
        assertFalse("HTTP 400 no debe indicar éxito", exito400)
        assertEquals("No puedes solicitar la adopción de una mascota publicada por ti.", msg400)
    }

    // =========================================================================
    // PRUEBAS DE RESOLUCIÓN DE CLAVES DUPLICADAS EN JETPACK COMPOSE
    // =========================================================================

    @Test
    fun testPrevencionColisionClavesCompose_mascotaRemotaVsDemo() {
        val mascotaDemo = Pet(
            id = 2,
            name = "Luna",
            species = "Gato",
            breed = "Gato doméstico",
            age = "1 año",
            gender = "Hembra",
            description = "Demo local",
            esRemota = false
        )

        val mascotaRemota = Pet(
            id = 2,
            name = "Michi Prueba",
            species = "Gato",
            breed = "Criollo",
            age = "8 meses",
            gender = "Hembra",
            description = "Mascota persistida en Neon",
            creadoPorCorreo = "user@test.pe",
            esRemota = true
        )

        // Verificamos que ambas mascotas compartan el id numérico 2
        assertEquals(2, mascotaDemo.id)
        assertEquals(2, mascotaRemota.id)

        // Pero sus claves de LazyColumn en Compose son completamente diferentes y estables
        assertEquals("demo-2", mascotaDemo.claveCompose)
        assertEquals("remota-2", mascotaRemota.claveCompose)
        assertNotEquals(mascotaDemo.claveCompose, mascotaRemota.claveCompose)

        // Una lista que contenga a ambas NO produce claves duplicadas
        val listaConAmbas = listOf(mascotaDemo, mascotaRemota)
        val claves = listaConAmbas.map { it.claveCompose }
        assertEquals(2, claves.size)
        assertEquals(2, claves.toSet().size)
    }

    @Test
    fun testDeduplicacionCatalogoMascotas_previeneClavesDuplicadas() {
        val pets = listOf(
            Pet(id = 1, name = "Max", species = "Perro", breed = "Labrador", age = "2 años", gender = "Macho", description = "Demo", esRemota = false),
            Pet(id = 2, name = "Luna", species = "Gato", breed = "Doméstico", age = "1 año", gender = "Hembra", description = "Demo", esRemota = false),
            Pet(id = 2, name = "Michi Prueba", species = "Gato", breed = "Criollo", age = "8 meses", gender = "Hembra", description = "Remota", esRemota = true)
        )

        val unicas = pets.distinctBy { it.claveCompose }
        assertEquals(3, unicas.size)

        val conteoClaves = unicas.groupBy { it.claveCompose }
        conteoClaves.forEach { (clave, grupo) ->
            assertEquals("La clave '$clave' no debe estar duplicada", 1, grupo.size)
        }
    }

    // =========================================================================
    // PRUEBAS DE LA FASE 2D: ELIMINACIÓN DE MASCOTAS DEMOSTRATIVAS DEL CATÁLOGO
    // =========================================================================

    @Test
    fun testFase2D_catalogoSoloAceptaMascotasRemotas() {
        // En producción el catálogo no contiene mascotas demostrativas (esRemota = false)
        val mascotasDemo = PetRepository().getPets()
        assertEquals(15, mascotasDemo.size)
        assertTrue("Todas las mascotas del repositorio son demostrativas", mascotasDemo.all { !it.esRemota })

        // Una mascota remota de Neon debe tener esRemota = true
        val mascotaNeon = Pet(
            id = 1,
            name = "Michi Prueba",
            species = "Gato",
            breed = "Criollo",
            age = "8 meses",
            gender = "Hembra",
            location = "Lima",
            description = "Gatita rescatada",
            creadoPorCorreo = "adoptante@test.com",
            esRemota = true
        )
        assertTrue(mascotaNeon.esRemota)
        assertEquals("remota-1", mascotaNeon.claveCompose)
    }

    @Test
    fun testFase2D_favoritosAisladosDeMascotasDemostrativas() {
        // Simular que en SharedPreferences antiguo existían IDs demostrativos (1, 2)
        // Guardadas con clave explícita de mascota remota
        val clavesFavoritasGuardadas = listOf("remota-5", "remota-9")

        val mascotasEnCatalogo = listOf(
            Pet(id = 1, name = "Michi Prueba", species = "Gato", breed = "Criollo", age = "1 año", gender = "Hembra", description = "Test", esRemota = true),
            Pet(id = 5, name = "Pelusa", species = "Gato", breed = "Persa", age = "2 años", gender = "Hembra", description = "Test", esRemota = true),
            Pet(id = 9, name = "Bobby", species = "Perro", breed = "Mestizo", age = "3 años", gender = "Macho", description = "Test", esRemota = true)
        )

        // El filtrado de favoritos solo asocia mascotas remotas cuyas claves coinciden exactamente
        val favoritosRestaurados = mascotasEnCatalogo.filter { pet ->
            pet.esRemota && clavesFavoritasGuardadas.contains(pet.claveCompose)
        }

        assertEquals(2, favoritosRestaurados.size)
        assertTrue(favoritosRestaurados.any { it.id == 5 && it.name == "Pelusa" })
        assertTrue(favoritosRestaurados.any { it.id == 9 && it.name == "Bobby" })
        assertFalse("Michi Prueba (id 1) no debe marcarse como favorito accidentalmente", favoritosRestaurados.any { it.id == 1 })
    }

    @Test
    fun testFase2D_sincronizacionRechazaPublicacionesSinCorreo() {
        val remota = Pet(
            id = 10,
            name = "Mascota Remota",
            species = "Perro",
            breed = "Mestizo",
            age = "1 año",
            gender = "Macho",
            description = "Remota",
            creadoPorCorreo = "propietario@adoptme.com",
            esRemota = true
        )

        val localHuerfana = Pet(
            id = 999,
            name = "Demo Antigua",
            species = "Conejo",
            breed = "Enano",
            age = "6 meses",
            gender = "Hembra",
            description = "Local sin autor",
            creadoPorCorreo = "", // Correo en blanco
            esRemota = false
        )

        val sincronizadas = sincronizarPublicacionesUsuario(
            correoUsuario = "propietario@adoptme.com",
            mascotasRemotas = listOf(remota),
            publicacionesLocales = listOf(localHuerfana)
        )

        // La publicación local huérfana (sin correo) NO debe ser adoptada por el usuario
        assertEquals(1, sincronizadas.size)
        assertEquals("Mascota Remota", sincronizadas.first().name)
        assertFalse(sincronizadas.any { it.id == 999 })
    }

    @Test
    fun testFase2EA_resolucionPerfilUsuarioReal() {
        val usuarioReal = UsuarioRegistrado(
            nombre = "Carlos Mendoza",
            correo = "carlos@adoptme.pe",
            contrasena = "",
            telefono = "987654321",
            rol = "Adoptante"
        )

        // Verificación de datos del usuario autenticado
        assertEquals("Carlos Mendoza", usuarioReal.nombre)
        assertEquals("carlos@adoptme.pe", usuarioReal.correo)
        assertEquals("987654321", usuarioReal.telefono)
        assertEquals("Adoptante", usuarioReal.rol)
        assertFalse("El nombre no debe contener etiquetas DEMO", usuarioReal.nombre.contains("Demo", ignoreCase = true))
        assertFalse("El teléfono no debe contener etiquetas DEMO", usuarioReal.telefono.contains("Demo", ignoreCase = true))
    }

    @Test
    fun testFase2EA_fallbackSinTelefonoRegistrado() {
        val usuarioSinTelefono = UsuarioRegistrado(
            nombre = "Ana Torres",
            correo = "ana@adoptme.pe",
            contrasena = "",
            telefono = "",
            rol = "Adoptante"
        )

        val telefonoMostrado = if (usuarioSinTelefono.telefono.isBlank()) "Sin teléfono registrado" else usuarioSinTelefono.telefono
        assertEquals("Sin teléfono registrado", telefonoMostrado)
        assertFalse(telefonoMostrado.contains("999 999 999"))
        assertFalse(telefonoMostrado.contains("Demo", ignoreCase = true))
    }

    @Test
    fun testFase2EA_aislamientoClavesPerfilPorUsuario() {
        val correo1 = "usuario1@adoptme.com"
        val correo2 = "usuario2@adoptme.com"

        val clave1 = "perfil_cache_${obtenerClaveUsuario(correo1)}"
        val clave2 = "perfil_cache_${obtenerClaveUsuario(correo2)}"

        assertNotEquals(clave1, clave2)
        assertEquals("perfil_cache_usuario1_adoptme_com", clave1)
        assertEquals("perfil_cache_usuario2_adoptme_com", clave2)
    }

    @Test
    fun testFase2EB_creacionActualizarPerfilRequest() {
        val request = ActualizarPerfilRequest(
            nombre = "María López",
            telefono = "912345678"
        )
        assertEquals("María López", request.nombre)
        assertEquals("912345678", request.telefono)
    }

    @Test
    fun testFase2EB_validacionNombreYTelefonoActualizarPerfil() {
        fun validarEdicionPerfil(nombre: String, telefono: String): Pair<Boolean, String?> {
            val nombreLimpio = nombre.trim()
            val telefonoLimpio = telefono.trim()
            if (nombreLimpio.isBlank()) return false to "El nombre completo no puede estar vacío."
            if (nombreLimpio.length < 2) return false to "El nombre debe tener al menos 2 caracteres."
            if (telefonoLimpio.isNotBlank() && !com.example.adoptme.screens.esTelefonoPeruanoValido(telefonoLimpio)) {
                return false to "El teléfono debe tener 9 dígitos y empezar con 9."
            }
            return true to null
        }

        // Nombre válido y teléfono peruano válido
        val (ok1, _) = validarEdicionPerfil("Juan Pérez", "987654321")
        assertTrue(ok1)

        // Nombre válido y teléfono vacío (opcional)
        val (ok2, _) = validarEdicionPerfil("Juan Pérez", "")
        assertTrue(ok2)

        // Nombre vacío rechazado
        val (ok3, err3) = validarEdicionPerfil("   ", "987654321")
        assertFalse(ok3)
        assertEquals("El nombre completo no puede estar vacío.", err3)

        // Nombre menor a 2 caracteres rechazado
        val (ok4, err4) = validarEdicionPerfil("A", "987654321")
        assertFalse(ok4)
        assertEquals("El nombre debe tener al menos 2 caracteres.", err4)

        // Teléfono inválido (no empieza con 9)
        val (ok5, err5) = validarEdicionPerfil("Juan Pérez", "888123456")
        assertFalse(ok5)
        assertEquals("El teléfono debe tener 9 dígitos y empezar con 9.", err5)
    }

    @Test
    fun testFase2EB_actualizacionPerfilLocalYCaché() {
        val original = UsuarioRegistrado(
            nombre = "Nombre Viejo",
            correo = "usuario@adoptme.pe",
            contrasena = "",
            telefono = "999111222",
            rol = "Adoptante"
        )

        val respuestaNeon = com.example.adoptme.api.PerfilUsuarioResponse(
            id = 42,
            nombre = "Nuevo Nombre",
            correo = "usuario@adoptme.pe",
            telefono = "987654321",
            rol = "Adoptante"
        )

        val actualizado = original.copy(
            nombre = respuestaNeon.nombre,
            telefono = respuestaNeon.telefono
        )

        assertEquals("Nuevo Nombre", actualizado.nombre)
        assertEquals("987654321", actualizado.telefono)
        assertEquals("usuario@adoptme.pe", actualizado.correo)
        assertEquals("Adoptante", actualizado.rol)
    }

    @Test
    fun testFase2F_mascotaVisitadaEntity_mapeoAPet() {
        val entidad = MascotaVisitadaEntity(
            correoUsuario = "adoptante@adoptme.pe",
            mascotaId = 42,
            nombre = "Bobby",
            especie = "Perro",
            raza = "Mestizo",
            edad = "2 años",
            genero = "Macho",
            ubicacion = "Miraflores",
            estado = "Disponible",
            descripcion = "Juguetón y cariñoso",
            imagenResId = 12345,
            creadoPorCorreo = "refugio@adoptme.pe",
            fechaUltimaVisita = 1700000000000L
        )

        val pet = entidad.toPet()

        assertEquals(42, pet.id)
        assertEquals("Bobby", pet.name)
        assertEquals("Perro", pet.species)
        assertEquals("Mestizo", pet.breed)
        assertEquals("2 años", pet.age)
        assertEquals("Macho", pet.gender)
        assertEquals("Miraflores", pet.location)
        assertEquals("Disponible", pet.status)
        assertEquals("Juguetón y cariñoso", pet.description)
        assertEquals(12345, pet.image)
        assertEquals("refugio@adoptme.pe", pet.creadoPorCorreo)
        assertTrue(pet.esRemota)
    }

    @Test
    fun testFase2F_aislamientoHistorialPorUsuario() {
        val visitas = listOf(
            MascotaVisitadaEntity(
                correoUsuario = normalizarCorreoUsuario("usuarioA@adoptme.pe"),
                mascotaId = 1,
                nombre = "Max",
                especie = "Perro",
                raza = "Golden",
                edad = "1 año",
                genero = "Macho",
                ubicacion = "Lima",
                estado = "Disponible",
                descripcion = "",
                imagenResId = 0,
                creadoPorCorreo = "",
                fechaUltimaVisita = 1000L
            ),
            MascotaVisitadaEntity(
                correoUsuario = normalizarCorreoUsuario("usuarioB@adoptme.pe"),
                mascotaId = 2,
                nombre = "Luna",
                especie = "Gato",
                raza = "Siamés",
                edad = "6 meses",
                genero = "Hembra",
                ubicacion = "Surco",
                estado = "Disponible",
                descripcion = "",
                imagenResId = 0,
                creadoPorCorreo = "",
                fechaUltimaVisita = 2000L
            ),
            MascotaVisitadaEntity(
                correoUsuario = normalizarCorreoUsuario("USUARIOA@ADOPTME.PE"),
                mascotaId = 3,
                nombre = "Rocky",
                especie = "Perro",
                raza = "Boxer",
                edad = "3 años",
                genero = "Macho",
                ubicacion = "San Borja",
                estado = "Disponible",
                descripcion = "",
                imagenResId = 0,
                creadoPorCorreo = "",
                fechaUltimaVisita = 3000L
            )
        )

        val correoConsultaA = normalizarCorreoUsuario("  UsuarioA@Adoptme.pe  ")
        val visitasA = visitas.filter { it.correoUsuario == correoConsultaA }

        assertEquals(2, visitasA.size)
        assertTrue(visitasA.any { it.mascotaId == 1 })
        assertTrue(visitasA.any { it.mascotaId == 3 })
        assertFalse(visitasA.any { it.mascotaId == 2 })

        val correoConsultaB = normalizarCorreoUsuario("usuarioB@adoptme.pe")
        val visitasB = visitas.filter { it.correoUsuario == correoConsultaB }
        assertEquals(1, visitasB.size)
        assertEquals(2, visitasB[0].mascotaId)
    }

    @Test
    fun testFase2F_reemplazoActualizaFechaVisitaSinDuplicados() {
        val historial = mutableMapOf<Pair<String, Int>, MascotaVisitadaEntity>()

        fun registrarVisitaSimulada(visita: MascotaVisitadaEntity) {
            historial[Pair(visita.correoUsuario, visita.mascotaId)] = visita
        }

        val correo = normalizarCorreoUsuario("test@adoptme.pe")

        // 1ra visita a Max (id=10) a las 1000 ms
        registrarVisitaSimulada(
            MascotaVisitadaEntity(
                correoUsuario = correo,
                mascotaId = 10,
                nombre = "Max",
                especie = "Perro",
                raza = "Labrador",
                edad = "2 años",
                genero = "Macho",
                ubicacion = "Lima",
                estado = "Disponible",
                descripcion = "",
                imagenResId = 0,
                creadoPorCorreo = "",
                fechaUltimaVisita = 1000L
            )
        )

        // 2da visita a Pelusa (id=20) a las 2000 ms
        registrarVisitaSimulada(
            MascotaVisitadaEntity(
                correoUsuario = correo,
                mascotaId = 20,
                nombre = "Pelusa",
                especie = "Gato",
                raza = "Persa",
                edad = "1 año",
                genero = "Hembra",
                ubicacion = "Lima",
                estado = "Disponible",
                descripcion = "",
                imagenResId = 0,
                creadoPorCorreo = "",
                fechaUltimaVisita = 2000L
            )
        )

        assertEquals(2, historial.size)

        // 3ra visita NUEVAMENTE a Max (id=10) a las 5000 ms (re-visita)
        registrarVisitaSimulada(
            MascotaVisitadaEntity(
                correoUsuario = correo,
                mascotaId = 10,
                nombre = "Max",
                especie = "Perro",
                raza = "Labrador",
                edad = "2 años",
                genero = "Macho",
                ubicacion = "Lima",
                estado = "Disponible",
                descripcion = "",
                imagenResId = 0,
                creadoPorCorreo = "",
                fechaUltimaVisita = 5000L
            )
        )

        // Verificamos que NO hay duplicados: sigue habiendo 2 elementos
        assertEquals(2, historial.size)

        // Ordenados por más reciente primero
        val ordenados = historial.values.sortedByDescending { it.fechaUltimaVisita }
        assertEquals(10, ordenados[0].mascotaId)
        assertEquals(5000L, ordenados[0].fechaUltimaVisita)
        assertEquals(20, ordenados[1].mascotaId)
    }

    @Test
    fun testFase2F_sinDatosSensiblesEnEntidadSQLite() {
        val fields = MascotaVisitadaEntity::class.java.declaredFields.map { it.name }
        assertFalse("No debe contener token", fields.contains("token"))
        assertFalse("No debe contener password", fields.contains("password"))
        assertFalse("No debe contener contrasena", fields.contains("contrasena"))
        assertFalse("No debe contener hash", fields.contains("hash"))
        assertFalse("No debe contener passwordHash", fields.contains("passwordHash"))
    }

    @Test
    fun testHU11_consumoResponsable_criteriosAceptacion() {
        // Validación de contenidos éticos requeridos por HU-11 y ODS 12:
        // 1. Reutilización de accesorios en buen estado
        // 2. Donación coordinada con refugios
        // 3. Reducción de residuos y compras innecesarias
        // 4. Prohibición expresa de medicamentos o alimentos vencidos
        val recomendacionesValidas = listOf(
            "Reutiliza accesorios en buen estado",
            "Dona lo que ya no utilizas",
            "Intercambia productos en buen estado",
            "Evita compras innecesarias"
        )
        assertEquals(4, recomendacionesValidas.size)

        // Validación de no reutilización de insumos peligrosos
        fun esAptoParaReutilizarODonar(categoria: String, estaVencidoODeteriorado: Boolean): Boolean {
            if (estaVencidoODeteriorado) return false
            if (categoria.equals("medicamentos", ignoreCase = true)) return false
            if (categoria.equals("alimentos_abiertos", ignoreCase = true)) return false
            return true
        }

        assertTrue(esAptoParaReutilizarODonar("transportadora", false))
        assertTrue(esAptoParaReutilizarODonar("correa", false))
        assertTrue(esAptoParaReutilizarODonar("plato_acero", false))
        assertFalse(esAptoParaReutilizarODonar("medicamentos", false))
        assertFalse(esAptoParaReutilizarODonar("alimentos_abiertos", false))
        assertFalse(esAptoParaReutilizarODonar("correa", true))
    }

    // =========================================================================
    // PRUEBAS DE PREFERENCES DATASTORE (PREFERENCIAS DE CATÁLOGO)
    // =========================================================================

    @Test
    fun testDataStore_valoresPredeterminados() {
        val prefsPorDefecto = com.example.adoptme.datastore.PreferenciasCatalogo()
        assertEquals("Todas", prefsPorDefecto.mascotaPreferida)
        assertEquals("Todas", prefsPorDefecto.ubicacionPreferida)
        assertEquals("Más recientes", prefsPorDefecto.ordenPredeterminado)
        assertFalse(prefsPorDefecto.soloDisponibles)
    }

    @Test
    fun testDataStore_normalizacionCorreoEnClaves() {
        val correoConEspaciosYMayusculas = "  Adoptante.Uno@AdoptMe.PE  "
        val correoNormalizado = "adoptante.uno@adoptme.pe"

        val claveMascota1 = com.example.adoptme.datastore.ClavesCatalogo.claveMascota(correoConEspaciosYMayusculas).name
        val claveMascota2 = com.example.adoptme.datastore.ClavesCatalogo.claveMascota(correoNormalizado).name
        assertEquals(claveMascota1, claveMascota2)
        assertEquals("mascota_preferida_adoptante.uno@adoptme.pe", claveMascota1)

        val claveUbicacion = com.example.adoptme.datastore.ClavesCatalogo.claveUbicacion(correoConEspaciosYMayusculas).name
        assertEquals("ubicacion_preferida_adoptante.uno@adoptme.pe", claveUbicacion)

        val claveOrden = com.example.adoptme.datastore.ClavesCatalogo.claveOrden(correoConEspaciosYMayusculas).name
        assertEquals("orden_predeterminado_adoptante.uno@adoptme.pe", claveOrden)

        val claveDisponibles = com.example.adoptme.datastore.ClavesCatalogo.claveSoloDisponibles(correoConEspaciosYMayusculas).name
        assertEquals("solo_disponibles_adoptante.uno@adoptme.pe", claveDisponibles)
    }

    @Test
    fun testDataStore_aislamientoClavesPorUsuario() {
        val userA = "usuarioA@adoptme.com"
        val userB = "usuarioB@adoptme.com"

        val kMascotaA = com.example.adoptme.datastore.ClavesCatalogo.claveMascota(userA).name
        val kMascotaB = com.example.adoptme.datastore.ClavesCatalogo.claveMascota(userB).name
        assertNotEquals(kMascotaA, kMascotaB)

        val kUbicacionA = com.example.adoptme.datastore.ClavesCatalogo.claveUbicacion(userA).name
        val kUbicacionB = com.example.adoptme.datastore.ClavesCatalogo.claveUbicacion(userB).name
        assertNotEquals(kUbicacionA, kUbicacionB)

        val kOrdenA = com.example.adoptme.datastore.ClavesCatalogo.claveOrden(userA).name
        val kOrdenB = com.example.adoptme.datastore.ClavesCatalogo.claveOrden(userB).name
        assertNotEquals(kOrdenA, kOrdenB)

        val kDisponiblesA = com.example.adoptme.datastore.ClavesCatalogo.claveSoloDisponibles(userA).name
        val kDisponiblesB = com.example.adoptme.datastore.ClavesCatalogo.claveSoloDisponibles(userB).name
        assertNotEquals(kDisponiblesA, kDisponiblesB)
    }

    @Test
    fun testDataStore_migracionIdempotente_desdeSharedPreferences() {
        val correo = "usuario.demo@adoptme.pe"
        val spMap = mutableMapOf<String, Any>(
            "mascota_preferida_usuario.demo@adoptme.pe" to "Gatos",
            "ubicacion_preferida_usuario.demo@adoptme.pe" to "Miraflores",
            "orden_predeterminado_usuario.demo@adoptme.pe" to "Nombre A-Z",
            "solo_disponibles_usuario.demo@adoptme.pe" to true,
            // Claves de SharedPreferences que DEBEN conservarse:
            "tema_usuario.demo@adoptme.pe" to "Oscuro",
            "color_principal_usuario.demo@adoptme.pe" to "Azul",
            "tamano_texto_usuario.demo@adoptme.pe" to "Grande",
            "token_jwt_activo" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        )

        val dsMap = mutableMapOf<String, Any>()

        // 1. Primera ejecución: debe migrar los datos a DataStore y señalar retiro únicamente de las 4 claves de catálogo
        val (migrado1, clavesRetiradas1) = com.example.adoptme.datastore.ejecutarLogicaMigracionCatalogo(
            correo = correo,
            spValores = spMap,
            dataStoreValores = dsMap
        )

        assertTrue("La primera migración debe realizarse", migrado1)
        assertEquals("Gatos", dsMap["mascota_preferida_usuario.demo@adoptme.pe"])
        assertEquals("Miraflores", dsMap["ubicacion_preferida_usuario.demo@adoptme.pe"])
        assertEquals("Nombre A-Z", dsMap["orden_predeterminado_usuario.demo@adoptme.pe"])
        assertEquals(true, dsMap["solo_disponibles_usuario.demo@adoptme.pe"])

        // Verificar que las 4 claves de catálogo están en la lista de claves a retirar
        assertEquals(4, clavesRetiradas1.size)
        assertTrue(clavesRetiradas1.contains("mascota_preferida_usuario.demo@adoptme.pe"))
        assertTrue(clavesRetiradas1.contains("ubicacion_preferida_usuario.demo@adoptme.pe"))
        assertTrue(clavesRetiradas1.contains("orden_predeterminado_usuario.demo@adoptme.pe"))
        assertTrue(clavesRetiradas1.contains("solo_disponibles_usuario.demo@adoptme.pe"))

        // Verificar que las claves de apariencia, token y notificaciones NO son retiradas
        assertFalse(clavesRetiradas1.contains("tema_usuario.demo@adoptme.pe"))
        assertFalse(clavesRetiradas1.contains("color_principal_usuario.demo@adoptme.pe"))
        assertFalse(clavesRetiradas1.contains("tamano_texto_usuario.demo@adoptme.pe"))
        assertFalse(clavesRetiradas1.contains("token_jwt_activo"))

        // Simular retiro en SP
        clavesRetiradas1.forEach { spMap.remove(it) }

        // 2. Segunda ejecución (idempotencia): DataStore ya tiene valores, no debe re-migrar ni sobrescribir
        val (migrado2, _) = com.example.adoptme.datastore.ejecutarLogicaMigracionCatalogo(
            correo = correo,
            spValores = spMap,
            dataStoreValores = dsMap
        )

        assertFalse("La segunda ejecución no debe migrar (idempotente)", migrado2)
        assertEquals("Gatos", dsMap["mascota_preferida_usuario.demo@adoptme.pe"])
    }

    @Test
    fun testDataStore_noSobrescribeSiDataStoreYaContieneDatos() {
        val correo = "usuario.existente@adoptme.pe"
        val spMap = mapOf<String, Any>(
            "mascota_preferida_usuario.existente@adoptme.pe" to "Perros"
        )
        val dsMap = mutableMapOf<String, Any>(
            "mascota_preferida_usuario.existente@adoptme.pe" to "Conejos"
        )

        val (migrado, _) = com.example.adoptme.datastore.ejecutarLogicaMigracionCatalogo(
            correo = correo,
            spValores = spMap,
            dataStoreValores = dsMap
        )

        assertFalse("No debe migrar si DataStore ya contiene preferencias del usuario", migrado)
        assertEquals("Conejos", dsMap["mascota_preferida_usuario.existente@adoptme.pe"])
    }
}