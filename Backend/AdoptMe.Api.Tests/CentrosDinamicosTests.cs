using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Claims;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using AdoptMe.Api.Controllers;
using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using AdoptMe.Api.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace AdoptMe.Api.Tests
{
    public class CentrosDinamicosTests
    {
        private static readonly byte[] JpegHeader = new byte[] { 0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46 };

        private static IFormFile CreateMockFormFile(byte[] header, string fileName, string contentType, int totalBytes = 1024)
        {
            var ms = new MemoryStream();
            ms.Write(header, 0, header.Length);
            int remaining = totalBytes - header.Length;
            if (remaining > 0)
            {
                var padding = new byte[remaining];
                new Random(42).NextBytes(padding);
                ms.Write(padding, 0, padding.Length);
            }
            ms.Position = 0;
            return new FormFile(ms, 0, ms.Length, "imagen", fileName)
            {
                Headers = new HeaderDictionary(),
                ContentType = contentType
            };
        }

        private static AdoptMeDbContext CreateInMemoryContext(string dbName)
        {
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: dbName)
                .ConfigureWarnings(w => w.Ignore(Microsoft.EntityFrameworkCore.Diagnostics.InMemoryEventId.TransactionIgnoredWarning))
                .Options;

            return new AdoptMeDbContext(options);
        }

        private static IConfiguration CreateMockConfiguration()
        {
            var myConfiguration = new Dictionary<string, string?>
            {
                { "Jwt:Key", "ClaveSuperSecretaDePruebasAdoptMe2026*ConTamanoMayorA256BitsParaHmacSha256" },
                { "Cloudinary:Folder", "adoptme/mascotas" }
            };

            return new ConfigurationBuilder()
                .AddInMemoryCollection(myConfiguration)
                .Build();
        }

        private static ControllerContext CreateUserContext(string email, string role, int userId)
        {
            var claims = new List<Claim>
            {
                new Claim(ClaimTypes.Email, email),
                new Claim(ClaimTypes.Role, role),
                new Claim(ClaimTypes.NameIdentifier, userId.ToString()),
                new Claim(ClaimTypes.Name, $"Usuario {userId}")
            };

            var identity = new ClaimsIdentity(claims, "TestAuth");
            var principal = new ClaimsPrincipal(identity);

            return new ControllerContext
            {
                HttpContext = new DefaultHttpContext { User = principal }
            };
        }

        // ====================================================================
        // GRUPO 1: REGISTRO DE CENTROS Y ADOPTANTES
        // ====================================================================

        [Fact]
        public async Task Test01_RegistroAdoptante_ConservaRolAdoptante()
        {
            using var context = CreateInMemoryContext("reg_adoptante_db");
            var hasher = new PasswordHasher<Usuario>();
            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new RegistroUsuarioDto
            {
                Nombre = "Adoptante Test",
                Correo = "adoptante.test@adoptme.pe",
                Contrasena = "Pass123*",
                Telefono = "999888111"
            };

            var result = await controller.Registrar(dto);
            var statusResult = Assert.IsType<ObjectResult>(result);
            Assert.Equal(StatusCodes.Status201Created, statusResult.StatusCode);

            var usuario = await context.Usuarios.FirstOrDefaultAsync(u => u.Correo == "adoptante.test@adoptme.pe");
            Assert.NotNull(usuario);
            Assert.Equal("Adoptante", usuario.Rol);
        }

        [Fact]
        public async Task Test02_RegistroCentro_CreaUsuarioYCentroPendiente()
        {
            using var context = CreateInMemoryContext("reg_centro_db");
            var hasher = new PasswordHasher<Usuario>();
            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new RegistroCentroDto
            {
                NombreResponsable = "Juan Perez",
                Correo = "contacto@albergueperez.pe",
                Contrasena = "Albergue2026*",
                TelefonoResponsable = "999111222",
                NombreCentro = "Albergue Hermanos Perez",
                Descripcion = "Albergue en San Miguel dedicado a rescate.",
                Direccion = "Av. La Marina 2500",
                Distrito = "San Miguel",
                TelefonoCentro = "015554433",
                CorreoContacto = "adopciones@albergueperez.pe",
                Horario = "Lunes a Sabado 9:00 a 17:00",
                Latitud = -12.0789,
                Longitud = -77.0856,
                AceptacionTerminos = true
            };

            var result = await controller.RegistrarCentro(dto);
            var statusResult = Assert.IsType<ObjectResult>(result);
            Assert.Equal(StatusCodes.Status201Created, statusResult.StatusCode);

            var usuario = await context.Usuarios.FirstOrDefaultAsync(u => u.Correo == "contacto@albergueperez.pe");
            Assert.NotNull(usuario);
            Assert.Equal("Centro", usuario.Rol);

            var centro = await context.Centros.FirstOrDefaultAsync(c => c.UsuarioId == usuario.Id);
            Assert.NotNull(centro);
            Assert.Equal("Albergue Hermanos Perez", centro.Nombre);
            Assert.Equal("Pendiente", centro.Estado);
            Assert.True(centro.Activo);
            Assert.False(centro.EsDemostrativo);
        }

        [Fact]
        public async Task Test03_RegistroCentro_TerminosNoAceptados_RetornaBadRequest()
        {
            using var context = CreateInMemoryContext("reg_centro_terminos_db");
            var hasher = new PasswordHasher<Usuario>();
            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new RegistroCentroDto
            {
                NombreResponsable = "Juan Perez",
                Correo = "terminos@albergue.pe",
                Contrasena = "Albergue2026*",
                NombreCentro = "Albergue Sin Terminos",
                Descripcion = "Descripcion valida",
                Direccion = "Av. Lima 123",
                Distrito = "Lima",
                TelefonoCentro = "999111222",
                CorreoContacto = "contacto@albergue.pe",
                Horario = "L-V 9-18",
                AceptacionTerminos = false // Invalido
            };

            var result = await controller.RegistrarCentro(dto);
            var badReq = Assert.IsType<BadRequestObjectResult>(result);
            Assert.Equal(StatusCodes.Status400BadRequest, badReq.StatusCode);
        }

        [Fact]
        public async Task Test04_RegistroCentro_CoordenadasIncompletas_RetornaBadRequest()
        {
            using var context = CreateInMemoryContext("reg_centro_coords_db");
            var hasher = new PasswordHasher<Usuario>();
            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new RegistroCentroDto
            {
                NombreResponsable = "Juan Perez",
                Correo = "coords@albergue.pe",
                Contrasena = "Albergue2026*",
                NombreCentro = "Albergue Coords",
                Descripcion = "Descripcion valida",
                Direccion = "Av. Lima 123",
                Distrito = "Lima",
                TelefonoCentro = "999111222",
                CorreoContacto = "contacto@albergue.pe",
                Horario = "L-V 9-18",
                Latitud = -12.0463,
                Longitud = null, // Incompleto: latitud sin longitud
                AceptacionTerminos = true
            };

            var result = await controller.RegistrarCentro(dto);
            var badReq = Assert.IsType<BadRequestObjectResult>(result);
            Assert.Equal(StatusCodes.Status400BadRequest, badReq.StatusCode);
        }

        [Fact]
        public async Task Test05_RegistroCentro_CorreoDuplicado_RetornaConflict()
        {
            using var context = CreateInMemoryContext("reg_centro_dup_db");
            var hasher = new PasswordHasher<Usuario>();
            context.Usuarios.Add(new Usuario { Nombre = "Ya Existe", Correo = "duplicado@adoptme.pe", Rol = "Adoptante" });
            await context.SaveChangesAsync();

            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new RegistroCentroDto
            {
                NombreResponsable = "Nuevo Responsable",
                Correo = "duplicado@adoptme.pe",
                Contrasena = "Pass123*",
                NombreCentro = "Centro Duplicado",
                Descripcion = "Descripcion",
                Direccion = "Dir",
                Distrito = "Dist",
                TelefonoCentro = "999",
                CorreoContacto = "info@dup.pe",
                Horario = "Horario",
                AceptacionTerminos = true
            };

            var result = await controller.RegistrarCentro(dto);
            var conflict = Assert.IsType<ConflictObjectResult>(result);
            Assert.Equal(StatusCodes.Status409Conflict, conflict.StatusCode);
        }

        [Fact]
        public async Task Test05b_RegistroCentro_FalloInterno_Retorna500GenericoSinFiltrarDetalles()
        {
            var context = CreateInMemoryContext("reg_centro_error_db");
            var hasher = new PasswordHasher<Usuario>();
            var controller = new UsuariosController(context, hasher, CreateMockConfiguration(), NullLogger<UsuariosController>.Instance);

            // Simular fallo disponiendo el contexto antes de la llamada
            context.Dispose();

            var dto = new RegistroCentroDto
            {
                NombreResponsable = "Juan Perez",
                Correo = "error@albergue.pe",
                Contrasena = "Albergue2026*",
                NombreCentro = "Albergue Con Error",
                Descripcion = "Descripcion valida",
                Direccion = "Av. Lima 123",
                Distrito = "Lima",
                TelefonoCentro = "999111222",
                CorreoContacto = "contacto@albergue.pe",
                Horario = "L-V 9-18",
                AceptacionTerminos = true
            };

            var result = await controller.RegistrarCentro(dto);
            var objResult = Assert.IsType<ObjectResult>(result);
            Assert.Equal(StatusCodes.Status500InternalServerError, objResult.StatusCode);

            var json = System.Text.Json.JsonSerializer.Serialize(objResult.Value, new System.Text.Json.JsonSerializerOptions
            {
                Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping
            });
            Assert.Contains("Ocurrió un error interno al registrar el centro de adopción.", json);
            Assert.DoesNotContain("ObjectDisposedException", json);
            Assert.DoesNotContain("StackTrace", json);
        }

        // ====================================================================
        // GRUPO 2: ENDPOINTS PÚBLICOS DE CENTROS
        // ====================================================================

        [Fact]
        public async Task Test06_ObtenerCentrosPublicos_SoloRetornaAprobadosYActivos()
        {
            using var context = CreateInMemoryContext("pub_centros_db");
            var authService = new CentroAuthorizationService(context);
            var fakeStorage = new FakeImageStorageService();
            var controller = new CentrosController(context, authService, fakeStorage, NullLogger<CentrosController>.Instance);

            // 1 aprobado y activo
            context.Centros.Add(new Centro { Id = 1, Nombre = "Centro Aprobado", Estado = "Aprobado", Activo = true, Direccion = "D1", Distrito = "Miraflores", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" });
            // 1 pendiente
            context.Centros.Add(new Centro { Id = 2, Nombre = "Centro Pendiente", Estado = "Pendiente", Activo = true, Direccion = "D2", Distrito = "Surco", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" });
            // 1 rechazado
            context.Centros.Add(new Centro { Id = 3, Nombre = "Centro Rechazado", Estado = "Rechazado", Activo = true, Direccion = "D3", Distrito = "SJL", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" });
            // 1 aprobado pero inactivo
            context.Centros.Add(new Centro { Id = 4, Nombre = "Centro Inactivo", Estado = "Aprobado", Activo = false, Direccion = "D4", Distrito = "Comas", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" });
            await context.SaveChangesAsync();

            var actionResult = await controller.ObtenerCentros();
            var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
            var centros = Assert.IsAssignableFrom<IEnumerable<CentroPublicoDto>>(okResult.Value).ToList();

            Assert.Single(centros);
            Assert.Equal("Centro Aprobado", centros[0].Nombre);
            Assert.Equal(1, centros[0].Id);
        }

        [Fact]
        public async Task Test07_ObtenerCentroPorId_CentroNoAprobado_RetornaNotFound()
        {
            using var context = CreateInMemoryContext("pub_centro_id_db");
            var authService = new CentroAuthorizationService(context);
            var fakeStorage = new FakeImageStorageService();
            var controller = new CentrosController(context, authService, fakeStorage, NullLogger<CentrosController>.Instance);

            context.Centros.Add(new Centro { Id = 5, Nombre = "Oculto", Estado = "Pendiente", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" });
            await context.SaveChangesAsync();

            var result = await controller.ObtenerPorId(5);
            var notFound = Assert.IsType<NotFoundObjectResult>(result.Result);
            Assert.Equal(StatusCodes.Status404NotFound, notFound.StatusCode);
        }

        // ====================================================================
        // GRUPO 3: ENDPOINTS DE PROPIETARIO DE CENTRO
        // ====================================================================

        [Fact]
        public async Task Test08_ObtenerMiCentro_RetornaPerfilDelUsuarioAutenticado()
        {
            using var context = CreateInMemoryContext("mi_centro_db");
            var usuario = new Usuario { Id = 10, Nombre = "Owner", Correo = "owner@centro.pe", Rol = "Centro" };
            var centro = new Centro { Id = 20, UsuarioId = 10, Nombre = "Mi Refugio", Estado = "Pendiente", Activo = true, Direccion = "Dir", Distrito = "Surco", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new FakeImageStorageService(), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("owner@centro.pe", "Centro", 10)
            };

            var result = await controller.ObtenerMiCentro();
            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var dto = Assert.IsType<CentroPropietarioDto>(okResult.Value);

            Assert.Equal(20, dto.Id);
            Assert.Equal("Mi Refugio", dto.Nombre);
            Assert.Equal("Pendiente", dto.Estado);
        }

        [Fact]
        public async Task Test09_ActualizarMiCentro_CambioTelefono_ConservaAprobacion()
        {
            using var context = CreateInMemoryContext("act_tel_db");
            var usuario = new Usuario { Id = 11, Nombre = "Owner", Correo = "owner11@centro.pe", Rol = "Centro" };
            var centro = new Centro
            {
                Id = 21,
                UsuarioId = 11,
                Nombre = "Albergue Seguro",
                Estado = "Aprobado",
                Activo = true,
                Direccion = "Av. Larco 100",
                Distrito = "Miraflores",
                Descripcion = "Desc",
                Horario = "9-17",
                Telefono = "999111222",
                CorreoContacto = "info@seguro.pe",
                Latitud = -12.1221,
                Longitud = -77.0305
            };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new FakeImageStorageService(), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("owner11@centro.pe", "Centro", 11)
            };

            var updateDto = new ActualizarMiCentroDto
            {
                Nombre = "Albergue Seguro", // Mismo nombre
                Descripcion = "Nueva descripcion operativa",
                Direccion = "Av. Larco 100",
                Distrito = "Miraflores",
                Telefono = "999888999", // Cambio de telefono
                CorreoContacto = "info@seguro.pe",
                Horario = "10:00 a 18:00",
                Latitud = -12.1221, // Misma coordenada
                Longitud = -77.0305
            };

            var result = await controller.ActualizarMiCentro(updateDto);
            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var resDto = Assert.IsType<CentroPropietarioDto>(okResult.Value);

            Assert.Equal("Aprobado", resDto.Estado); // CONSERVA APROBACIÓN
            Assert.Equal("999888999", resDto.Telefono);
        }

        [Fact]
        public async Task Test10_ActualizarMiCentro_CambioNombre_RegresaAPendiente()
        {
            using var context = CreateInMemoryContext("act_nombre_db");
            var usuario = new Usuario { Id = 12, Nombre = "Owner", Correo = "owner12@centro.pe", Rol = "Centro" };
            var centro = new Centro
            {
                Id = 22,
                UsuarioId = 12,
                Nombre = "Nombre Antiguo",
                Estado = "Aprobado",
                Activo = true,
                Direccion = "Av. Arequipa 500",
                Distrito = "Lima",
                Descripcion = "Desc",
                Horario = "H",
                Telefono = "T",
                CorreoContacto = "C",
                Latitud = -12.06,
                Longitud = -77.03
            };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new FakeImageStorageService(), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("owner12@centro.pe", "Centro", 12)
            };

            var updateDto = new ActualizarMiCentroDto
            {
                Nombre = "Nuevo Nombre Institucional", // CAMBIO SENSIBLE
                Descripcion = "Desc",
                Direccion = "Av. Arequipa 500",
                Distrito = "Lima",
                Telefono = "T",
                CorreoContacto = "C",
                Horario = "H",
                Latitud = -12.06,
                Longitud = -77.03
            };

            var result = await controller.ActualizarMiCentro(updateDto);
            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var resDto = Assert.IsType<CentroPropietarioDto>(okResult.Value);

            Assert.Equal("Pendiente", resDto.Estado); // REGRESA A PENDIENTE
        }

        [Fact]
        public async Task Test11_ActualizarMiCentro_MovimientoGpsMayorA500m_RegresaAPendiente()
        {
            using var context = CreateInMemoryContext("act_gps_db");
            var usuario = new Usuario { Id = 13, Nombre = "Owner", Correo = "owner13@centro.pe", Rol = "Centro" };
            var centro = new Centro
            {
                Id = 23,
                UsuarioId = 13,
                Nombre = "Albergue Fijo",
                Estado = "Aprobado",
                Activo = true,
                Direccion = "Calle 1",
                Distrito = "Miraflores",
                Descripcion = "Desc",
                Horario = "H",
                Telefono = "T",
                CorreoContacto = "C",
                Latitud = -12.1200,
                Longitud = -77.0300
            };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new FakeImageStorageService(), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("owner13@centro.pe", "Centro", 13)
            };

            // Distancia de ~1.5 km al desplazarse en latitud
            var updateDto = new ActualizarMiCentroDto
            {
                Nombre = "Albergue Fijo",
                Descripcion = "Desc",
                Direccion = "Calle Nueva",
                Distrito = "Miraflores",
                Telefono = "T",
                CorreoContacto = "C",
                Horario = "H",
                Latitud = -12.1350, // ~1.66 km de distancia
                Longitud = -77.0300
            };

            var result = await controller.ActualizarMiCentro(updateDto);
            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var resDto = Assert.IsType<CentroPropietarioDto>(okResult.Value);

            Assert.Equal("Pendiente", resDto.Estado);
        }

        [Fact]
        public async Task Test12_ReenviarSolicitud_DesdeRechazado_PasaAPendiente()
        {
            using var context = CreateInMemoryContext("reenviar_db");
            var usuario = new Usuario { Id = 14, Nombre = "Owner", Correo = "owner14@centro.pe", Rol = "Centro" };
            var centro = new Centro
            {
                Id = 24,
                UsuarioId = 14,
                Nombre = "Centro Observado",
                Estado = "Rechazado",
                MotivoRechazo = "Faltó verificar dirección",
                Activo = true,
                Direccion = "Dir",
                Distrito = "Dist",
                Descripcion = "Desc",
                Horario = "H",
                Telefono = "T",
                CorreoContacto = "C"
            };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new FakeImageStorageService(), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("owner14@centro.pe", "Centro", 14)
            };

            var result = await controller.ReenviarSolicitud();
            var okResult = Assert.IsType<OkObjectResult>(result);

            var centroBd = await context.Centros.FindAsync(24);
            Assert.NotNull(centroBd);
            Assert.Equal("Pendiente", centroBd.Estado);
            Assert.Null(centroBd.MotivoRechazo);
        }

        [Fact]
        public async Task Test13_ReenviarSolicitud_YaPendiente_RetornaConflict()
        {
            using var context = CreateInMemoryContext("reenviar_pend_db");
            var usuario = new Usuario { Id = 15, Nombre = "Owner", Correo = "owner15@centro.pe", Rol = "Centro" };
            var centro = new Centro
            {
                Id = 25,
                UsuarioId = 15,
                Nombre = "Centro En Espera",
                Estado = "Pendiente",
                Activo = true,
                Direccion = "Dir",
                Distrito = "Dist",
                Descripcion = "Desc",
                Horario = "H",
                Telefono = "T",
                CorreoContacto = "C"
            };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new FakeImageStorageService(), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("owner15@centro.pe", "Centro", 15)
            };

            var result = await controller.ReenviarSolicitud();
            var conflict = Assert.IsType<ConflictObjectResult>(result);
            Assert.Equal(StatusCodes.Status409Conflict, conflict.StatusCode);
        }

        // ====================================================================
        // GRUPO 4: ENDPOINTS ADMINISTRATIVOS (AdminCentrosController)
        // ====================================================================

        [Fact]
        public async Task Test14_Admin_AprobarCentro_CambiaEstadoAAprobado()
        {
            using var context = CreateInMemoryContext("admin_aprobar_db");
            var admin = new Usuario { Id = 1, Nombre = "Admin", Correo = "admin@adoptme.pe", Rol = "Admin" };
            var centro = new Centro { Id = 30, Nombre = "Centro Para Aprobar", Estado = "Pendiente", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(admin);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var result = await controller.AprobarCentro(30);
            var okResult = Assert.IsType<OkObjectResult>(result);

            var centroBd = await context.Centros.FindAsync(30);
            Assert.NotNull(centroBd);
            Assert.Equal("Aprobado", centroBd.Estado);
            Assert.NotNull(centroBd.FechaRevision);
            Assert.Equal(1, centroBd.RevisadoPorUsuarioId);
        }

        [Fact]
        public async Task Test15_Admin_RechazarCentro_SinMotivo_RetornaBadRequest()
        {
            using var context = CreateInMemoryContext("admin_rechazo_sinmotivo_db");
            var centro = new Centro { Id = 31, Nombre = "Centro Rechazo", Estado = "Pendiente", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new RechazarCentroDto { Motivo = "   " }; // En blanco
            var result = await controller.RechazarCentro(31, dto);
            var badReq = Assert.IsType<BadRequestObjectResult>(result);
            Assert.Equal(StatusCodes.Status400BadRequest, badReq.StatusCode);
        }

        [Fact]
        public async Task Test16_Admin_RechazarCentro_ConMotivo_GuardaMotivoYEstado()
        {
            using var context = CreateInMemoryContext("admin_rechazo_ok_db");
            var admin = new Usuario { Id = 1, Nombre = "Admin", Correo = "admin@adoptme.pe", Rol = "Admin" };
            var centro = new Centro { Id = 32, Nombre = "Centro Observado", Estado = "Pendiente", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(admin);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new RechazarCentroDto { Motivo = "La dirección indicada no corresponde a un predio autorizado." };
            var result = await controller.RechazarCentro(32, dto);
            var okResult = Assert.IsType<OkObjectResult>(result);

            var centroBd = await context.Centros.FindAsync(32);
            Assert.NotNull(centroBd);
            Assert.Equal("Rechazado", centroBd.Estado);
            Assert.Equal("La dirección indicada no corresponde a un predio autorizado.", centroBd.MotivoRechazo);
            Assert.Equal(1, centroBd.RevisadoPorUsuarioId);
        }

        [Fact]
        public async Task Test17_Admin_ActivarDesactivarCentro_CambiaFlagActivo()
        {
            using var context = CreateInMemoryContext("admin_toggle_db");
            var centro = new Centro { Id = 33, Nombre = "Centro Toggle", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            // Desactivar
            await controller.DesactivarCentro(33);
            var c1 = await context.Centros.FindAsync(33);
            Assert.False(c1!.Activo);

            // Reactivar
            await controller.ActivarCentro(33);
            var c2 = await context.Centros.FindAsync(33);
            Assert.True(c2!.Activo);
        }

        [Fact]
        public async Task Test18_Admin_AsignarPropietario_A_CentroDemostrativoHuerfano()
        {
            using var context = CreateInMemoryContext("admin_asignar_db");
            var centroDemo = new Centro { Id = 34, UsuarioId = null, Nombre = "Centro Demo Huerfano", Estado = "Aprobado", Activo = true, EsDemostrativo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            var nuevoUsuarioCentro = new Usuario { Id = 50, Nombre = "Nuevo Gestor", Correo = "gestor@adoptme.pe", Rol = "Centro" };
            context.Centros.Add(centroDemo);
            context.Usuarios.Add(nuevoUsuarioCentro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new AsignarPropietarioCentroDto { UsuarioId = 50 };
            var result = await controller.AsignarPropietario(34, dto);
            var okResult = Assert.IsType<OkObjectResult>(result);

            var cBd = await context.Centros.FindAsync(34);
            Assert.Equal(50, cBd!.UsuarioId);
        }

        [Fact]
        public async Task Test18b_Admin_AsignarPropietario_UsuarioInexistente_RetornaNotFound()
        {
            using var context = CreateInMemoryContext("admin_asignar_no_user_db");
            var centro = new Centro { Id = 35, Nombre = "Centro Sin User", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new AsignarPropietarioCentroDto { UsuarioId = 999 };
            var result = await controller.AsignarPropietario(35, dto);
            var notFound = Assert.IsType<NotFoundObjectResult>(result);
            Assert.Equal(StatusCodes.Status404NotFound, notFound.StatusCode);
        }

        [Fact]
        public async Task Test18c_Admin_AsignarPropietario_UsuarioRolIncorrecto_RetornaBadRequest()
        {
            using var context = CreateInMemoryContext("admin_asignar_bad_rol_db");
            var centro = new Centro { Id = 36, Nombre = "Centro Rol Bad", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            var adoptante = new Usuario { Id = 51, Nombre = "Adoptante Juan", Correo = "juan@adoptme.pe", Rol = "Adoptante" };
            context.Centros.Add(centro);
            context.Usuarios.Add(adoptante);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new AsignarPropietarioCentroDto { UsuarioId = 51 };
            var result = await controller.AsignarPropietario(36, dto);
            var badReq = Assert.IsType<BadRequestObjectResult>(result);
            Assert.Equal(StatusCodes.Status400BadRequest, badReq.StatusCode);
        }

        [Fact]
        public async Task Test18d_Admin_AsignarPropietario_UsuarioYaAsociadoA_OtroCentro_RetornaConflict()
        {
            using var context = CreateInMemoryContext("admin_asignar_dup_centro_db");
            var usuario = new Usuario { Id = 52, Nombre = "Centro Dueño", Correo = "dueno@adoptme.pe", Rol = "Centro" };
            var centro1 = new Centro { Id = 37, UsuarioId = 52, Nombre = "Centro Existente", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            var centro2 = new Centro { Id = 38, UsuarioId = null, Nombre = "Centro Nuevo", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.AddRange(centro1, centro2);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new AsignarPropietarioCentroDto { UsuarioId = 52 };
            var result = await controller.AsignarPropietario(38, dto);
            var conflict = Assert.IsType<ConflictObjectResult>(result);
            Assert.Equal(StatusCodes.Status409Conflict, conflict.StatusCode);
        }

        [Fact]
        public async Task Test18e_Admin_AsignarPropietario_MismoUsuario_EsIdempotenteRetornaOk()
        {
            using var context = CreateInMemoryContext("admin_asignar_idempotente_db");
            var usuario = new Usuario { Id = 53, Nombre = "Centro Mismo", Correo = "mismo@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 39, UsuarioId = 53, Nombre = "Centro Idem", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new AsignarPropietarioCentroDto { UsuarioId = 53 };
            var result = await controller.AsignarPropietario(39, dto);
            var okResult = Assert.IsType<OkObjectResult>(result);
            Assert.Equal(StatusCodes.Status200OK, okResult.StatusCode);

            var cBd = await context.Centros.FindAsync(39);
            Assert.Equal(53, cBd!.UsuarioId);
        }

        [Fact]
        public async Task Test18f_Admin_AsignarPropietario_NoSobrescribeResponsableExistente_RetornaConflict()
        {
            using var context = CreateInMemoryContext("admin_transferencia_db");
            var usuarioAntiguo = new Usuario { Id = 54, Nombre = "Centro Anterior", Correo = "ant@adoptme.pe", Rol = "Centro" };
            var usuarioNuevo = new Usuario { Id = 55, Nombre = "Centro Nuevo", Correo = "nuevo@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 40, UsuarioId = 54, Nombre = "Centro Transferible", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.AddRange(usuarioAntiguo, usuarioNuevo);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new AsignarPropietarioCentroDto { UsuarioId = 55 };
            var result = await controller.AsignarPropietario(40, dto);
            var conflictResult = Assert.IsType<ConflictObjectResult>(result);

            var cBd = await context.Centros.FindAsync(40);
            Assert.Equal(54, cBd!.UsuarioId);
        }

        [Fact]
        public async Task Test18g_AsignarPropietario_NoExponeDatosPrivados()
        {
            using var context = CreateInMemoryContext("admin_privacidad_db");
            var usuario = new Usuario { Id = 56, Nombre = "Privado Gomez", Correo = "privado@adoptme.pe", Telefono = "999888777", Rol = "Centro" };
            var centro = new Centro { Id = 41, UsuarioId = null, Nombre = "Centro Privado", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var dto = new AsignarPropietarioCentroDto { UsuarioId = 56 };
            var result = await controller.AsignarPropietario(41, dto);
            var okResult = Assert.IsType<OkObjectResult>(result);

            var json = System.Text.Json.JsonSerializer.Serialize(okResult.Value);
            Assert.DoesNotContain("privado@adoptme.pe", json);
            Assert.DoesNotContain("Privado Gomez", json);
            Assert.DoesNotContain("999888777", json);
        }

        // ====================================================================
        // GRUPO 5: PUBLICACIÓN DE MASCOTAS (MascotasController)
        // ====================================================================

        [Fact]
        public async Task Test19_AdoptantePublica_ServidorAsignaCentroIdNull()
        {
            using var context = CreateInMemoryContext("masc_adoptante_db");
            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("adoptante@adoptme.pe", "Adoptante", 70)
            };

            var nuevaMascota = new Mascota
            {
                Name = "Firulais",
                Species = "Perro",
                CentroId = 999 // Cliente intenta inyectar CentroId
            };

            var result = await controller.CrearMascota(nuevaMascota);
            var created = Assert.IsType<CreatedAtActionResult>(result.Result);
            var mascotaCreada = Assert.IsType<Mascota>(created.Value);

            Assert.Null(mascotaCreada.CentroId); // El backend sanitiza forzosamente hacia null
        }

        [Fact]
        public async Task Test20_CentroAprobadoPublica_ServidorAsignaCentroIdCorrecto()
        {
            using var context = CreateInMemoryContext("masc_centro_aprobado_db");
            var usuario = new Usuario { Id = 80, Nombre = "Centro Ok", Correo = "centro80@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 90, UsuarioId = 80, Nombre = "Refugio Oficial", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("centro80@adoptme.pe", "Centro", 80)
            };

            var nuevaMascota = new Mascota
            {
                Name = "Pelusa Institucional",
                Species = "Gato",
                CentroId = null // Cliente no lo manda
            };

            var result = await controller.CrearMascota(nuevaMascota);
            var created = Assert.IsType<CreatedAtActionResult>(result.Result);
            var mascotaCreada = Assert.IsType<Mascota>(created.Value);

            Assert.Equal(90, mascotaCreada.CentroId); // El backend infiere y asigna su centro
        }

        [Fact]
        public async Task Test21_CentroPendientePublica_RetornaForbidden403()
        {
            using var context = CreateInMemoryContext("masc_centro_pend_db");
            var usuario = new Usuario { Id = 81, Nombre = "Centro Pend", Correo = "centro81@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 91, UsuarioId = 81, Nombre = "Refugio En Espera", Estado = "Pendiente", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("centro81@adoptme.pe", "Centro", 81)
            };

            var nuevaMascota = new Mascota
            {
                Name = "Mascota Bloqueada",
                Species = "Perro"
            };

            var result = await controller.CrearMascota(nuevaMascota);
            var statusResult = Assert.IsType<ObjectResult>(result.Result);
            Assert.Equal(StatusCodes.Status403Forbidden, statusResult.StatusCode);
        }

        [Fact]
        public async Task Test22_CentroRechazadoPublica_RetornaForbidden403()
        {
            using var context = CreateInMemoryContext("masc_centro_rech_db");
            var usuario = new Usuario { Id = 82, Nombre = "Centro Rech", Correo = "centro82@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 92, UsuarioId = 82, Nombre = "Refugio Rechazado", Estado = "Rechazado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("centro82@adoptme.pe", "Centro", 82)
            };

            var nuevaMascota = new Mascota
            {
                Name = "Mascota Bloqueada 2",
                Species = "Perro"
            };

            var result = await controller.CrearMascota(nuevaMascota);
            var statusResult = Assert.IsType<ObjectResult>(result.Result);
            Assert.Equal(StatusCodes.Status403Forbidden, statusResult.StatusCode);
        }

        [Fact]
        public async Task Test23_CentroInactivoPublica_RetornaForbidden403()
        {
            using var context = CreateInMemoryContext("masc_centro_inactivo_db");
            var usuario = new Usuario { Id = 83, Nombre = "Centro Inactivo", Correo = "centro83@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 93, UsuarioId = 83, Nombre = "Refugio Inactivo", Estado = "Aprobado", Activo = false, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("centro83@adoptme.pe", "Centro", 83)
            };

            var nuevaMascota = new Mascota
            {
                Name = "Mascota Inactiva",
                Species = "Perro"
            };

            var result = await controller.CrearMascota(nuevaMascota);
            var statusResult = Assert.IsType<ObjectResult>(result.Result);
            Assert.Equal(StatusCodes.Status403Forbidden, statusResult.StatusCode);
        }

        [Fact]
        public async Task Test24_PublicacionMultipart_CentroAprobado_AsignaCentroIdCorrecto()
        {
            using var context = CreateInMemoryContext("masc_multipart_centro_db");
            var usuario = new Usuario { Id = 84, Nombre = "Centro Multipart", Correo = "centro84@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 94, UsuarioId = 84, Nombre = "Refugio Multipart", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var fakeStorage = new FakeImageStorageService();
            var controller = new MascotasController(context, fakeStorage, authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("centro84@adoptme.pe", "Centro", 84)
            };

            var file = CreateMockFormFile(JpegHeader, "foto.jpg", "image/jpeg");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "Bobby Centro",
                Species = "Perro",
                Imagen = file
            };

            var result = await controller.CrearMascotaConImagen(dto);
            var created = Assert.IsType<CreatedAtActionResult>(result.Result);
            var mascotaCreada = Assert.IsType<Mascota>(created.Value);

            Assert.Equal(94, mascotaCreada.CentroId);
            Assert.NotNull(mascotaCreada.ImageUrl);
            Assert.NotNull(mascotaCreada.ImagePublicId);
        }

        // ====================================================================
        // GRUPO 6: LOGIN Y METADATOS DE CENTRO
        // ====================================================================

        [Fact]
        public async Task Test25_LoginCentro_DevuelveEstadoYCentroId()
        {
            using var context = CreateInMemoryContext("login_centro_db");
            var hasher = new PasswordHasher<Usuario>();
            var usuario = new Usuario { Id = 100, Nombre = "User Centro", Correo = "centro100@adoptme.pe", Rol = "Centro" };
            usuario.PasswordHash = hasher.HashPassword(usuario, "Pass123*");
            var centro = new Centro { Id = 200, UsuarioId = 100, Nombre = "Centro Logueado", Estado = "Aprobado", Activo = true, EsDemostrativo = false, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new LoginUsuarioDto { Correo = "centro100@adoptme.pe", Contrasena = "Pass123*" };
            var result = await controller.Login(dto);
            var okResult = Assert.IsType<OkObjectResult>(result);

            // Inspeccionar propiedades dinámicas
            dynamic val = okResult.Value!;
            var json = System.Text.Json.JsonSerializer.Serialize(val);
            Assert.Contains("\"centroId\":200", json);
            Assert.Contains("\"estadoCentro\":\"Aprobado\"", json);
            Assert.Contains("\"centroActivo\":true", json);
        }

        [Fact]
        public async Task Test26_LoginCentro_SinPerfilCentro_DevuelveSinPerfil()
        {
            using var context = CreateInMemoryContext("login_sin_perfil_db");
            var hasher = new PasswordHasher<Usuario>();
            var usuario = new Usuario { Id = 101, Nombre = "User Sin Perfil", Correo = "sinperfil@adoptme.pe", Rol = "Centro" };
            usuario.PasswordHash = hasher.HashPassword(usuario, "Pass123*");
            context.Usuarios.Add(usuario);
            await context.SaveChangesAsync();

            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new LoginUsuarioDto { Correo = "sinperfil@adoptme.pe", Contrasena = "Pass123*" };
            var result = await controller.Login(dto);
            var okResult = Assert.IsType<OkObjectResult>(result);

            dynamic val = okResult.Value!;
            var json = System.Text.Json.JsonSerializer.Serialize(val);
            Assert.Contains("\"estadoCentro\":\"SinPerfil\"", json);
            Assert.Contains("\"centroId\":null", json);
        }

        [Fact]
        public async Task Test27_LoginAdoptante_NoRompeCompatibilidad()
        {
            using var context = CreateInMemoryContext("login_adoptante_db");
            var hasher = new PasswordHasher<Usuario>();
            var usuario = new Usuario { Id = 102, Nombre = "Adoptante Ok", Correo = "adoptante102@adoptme.pe", Rol = "Adoptante" };
            usuario.PasswordHash = hasher.HashPassword(usuario, "Pass123*");
            context.Usuarios.Add(usuario);
            await context.SaveChangesAsync();

            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new LoginUsuarioDto { Correo = "adoptante102@adoptme.pe", Contrasena = "Pass123*" };
            var result = await controller.Login(dto);
            var okResult = Assert.IsType<OkObjectResult>(result);

            dynamic val = okResult.Value!;
            var json = System.Text.Json.JsonSerializer.Serialize(val, new System.Text.Json.JsonSerializerOptions
            {
                PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase
            });
            Assert.Contains("\"rol\":\"Adoptante\"", json);
            Assert.Contains("\"centroId\":null", json);
            Assert.Contains("\"estadoCentro\":null", json);
        }

        // ====================================================================
        // GRUPO 7: SUBIDA DE LOGO INSTITUCIONAL DE CENTRO
        // ====================================================================

        [Fact]
        public async Task Test28_ActualizarLogo_CentroValido_SubeACloudinaryCentrosYActualizaLogoUrl()
        {
            using var context = CreateInMemoryContext("logo_centro_db");
            var usuario = new Usuario { Id = 110, Nombre = "Centro Logo", Correo = "logo@centro.pe", Rol = "Centro" };
            var centro = new Centro { Id = 210, UsuarioId = 110, Nombre = "Centro Con Logo", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Dist", Descripcion = "Desc", Horario = "H", Telefono = "T", CorreoContacto = "C" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var fakeStorage = new FakeImageStorageService();
            var controller = new CentrosController(context, authService, fakeStorage, NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("logo@centro.pe", "Centro", 110)
            };

            var file = CreateMockFormFile(JpegHeader, "logo.jpg", "image/jpeg");
            var result = await controller.ActualizarLogo(file);
            var okResult = Assert.IsType<OkObjectResult>(result);

            var centroBd = await context.Centros.FindAsync(210);
            Assert.NotNull(centroBd!.LogoUrl);
            Assert.NotNull(centroBd.LogoPublicId);
            Assert.Contains("adoptme/centros", centroBd.LogoPublicId);
            Assert.Single(fakeStorage.ImagenesSubidas);
            Assert.Contains("adoptme/centros", fakeStorage.ImagenesSubidas[0]);
        }

        [Fact]
        public async Task Test29_MascotasHistoricas_CentroIdNull_SiguenConsultandoseNormalmente()
        {
            using var context = CreateInMemoryContext("masc_historicas_db");
            context.Mascotas.Add(new Mascota { Id = 1, Name = "Historica 1", Species = "Perro", CentroId = null });
            context.Mascotas.Add(new Mascota { Id = 2, Name = "Historica 2", Species = "Gato", CentroId = null });
            context.Mascotas.Add(new Mascota { Id = 3, Name = "De Centro", Species = "Perro", CentroId = 55 });
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance);

            var result = await controller.ObtenerMascotas();
            var lista = result.Value!.ToList();

            Assert.Equal(3, lista.Count);
            Assert.Null(lista.First(m => m.Id == 1).CentroId);
            Assert.Null(lista.First(m => m.Id == 2).CentroId);
            Assert.Equal(55, lista.First(m => m.Id == 3).CentroId);
        }

        [Fact]
        public void Test30_Haversine_CalculoDistanciaMetros_EsExacto()
        {
            using var context = CreateInMemoryContext("haversine_db");
            var authService = new CentroAuthorizationService(context);

            // Plaza Mayor de Lima a Parque Kennedy (Miraflores) ~8.5 km
            double dist = authService.CalcularDistanciaMetros(-12.046374, -77.030583, -12.1221, -77.0305);
            Assert.InRange(dist, 8000.0, 9000.0);

            // Punto idéntico debe ser 0 metros
            double distCero = authService.CalcularDistanciaMetros(-12.046374, -77.030583, -12.046374, -77.030583);
            Assert.Equal(0.0, distCero, 2);
        }

        [Fact]
        public async Task Test31_ActualizarMascota_PutNoPermiteModificarCentroId()
        {
            using var context = CreateInMemoryContext("put_mascota_centroid_db");
            var mascota = new Mascota
            {
                Id = 1,
                Name = "Mascota Original",
                Species = "Perro",
                CreadoPorCorreo = "dueno@adoptme.pe",
                CentroId = 55
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("dueno@adoptme.pe", "Centro", 88)
            };

            var datosUpdate = new Mascota
            {
                Name = "Nombre Actualizado",
                CentroId = 999 // Intento de alterar CentroId existente
            };

            var result = await controller.ActualizarMascota(1, datosUpdate);
            Assert.IsType<NoContentResult>(result);

            var mascotaBd = await context.Mascotas.FindAsync(1);
            Assert.Equal("Nombre Actualizado", mascotaBd!.Name);
            Assert.Equal(55, mascotaBd.CentroId); // CentroId original estrictamente inmutable
        }

        [Fact]
        public async Task Test32_AdminPublicaSinCentro_CentroIdQuedaNull()
        {
            using var context = CreateInMemoryContext("admin_publica_sin_centro_db");
            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };

            var nuevaMascota = new Mascota
            {
                Name = "Mascota Admin",
                Species = "Gato",
                CentroId = null
            };

            var result = await controller.CrearMascota(nuevaMascota);
            var created = Assert.IsType<CreatedAtActionResult>(result.Result);
            var mascotaCreada = Assert.IsType<Mascota>(created.Value);

            Assert.Null(mascotaCreada.CentroId);
        }

        [Fact]
        public async Task Test33_PublicacionMultipart_CentroInyectaCentroIdFalso_ServidorIgnoraYAsignaCentroJwt()
        {
            using var context = CreateInMemoryContext("centro_inyecta_falso_centro_db");
            var user = new Usuario { Id = 10, Correo = "titular@centroreal.pe", Rol = "Centro" };
            var centro = new Centro
            {
                Id = 7,
                UsuarioId = 10,
                Nombre = "Centro Real Aprobado",
                Estado = "Aprobado",
                Activo = true
            };
            context.Usuarios.Add(user);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("titular@centroreal.pe", "Centro", 10)
            };

            var dto = new CrearMascotaMultipartDto
            {
                Name = "Cachorro Rescatado",
                Species = "Perro",
                Breed = "Mestizo",
                Age = "3 meses",
                Gender = "Macho",
                CentroId = 999 // Inyección maliciosa o errónea de CentroId distinto
            };

            var result = await controller.CrearMascotaConImagen(dto);
            var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
            var mascotaCreada = Assert.IsType<Mascota>(createdResult.Value);

            // La defensa del backend debe ignorar por completo el 999 y asignar el ID 7 resuelto desde el JWT
            Assert.Equal(7, mascotaCreada.CentroId);
            Assert.Equal("titular@centroreal.pe", mascotaCreada.CreadoPorCorreo);
        }

        [Fact]
        public async Task Test34_RegistroCentro_RetornaExactamente201Created()
        {
            using var context = CreateInMemoryContext("reg_centro_201_db");
            var hasher = new PasswordHasher<Usuario>();
            var controller = new UsuariosController(context, hasher, CreateMockConfiguration());

            var dto = new RegistroCentroDto
            {
                NombreResponsable = "Validador Contrato",
                Correo = "contrato201@adoptme.pe",
                Contrasena = "Password123!",
                TelefonoResponsable = "999888777",
                NombreCentro = "Albergue Contrato 201",
                Descripcion = "Verificación estricta del código HTTP de creación.",
                Direccion = "Av. Arequipa 1234",
                Distrito = "Lince",
                TelefonoCentro = "014443322",
                CorreoContacto = "contacto@contrato201.pe",
                Horario = "Lunes a Viernes 8:00 a 16:00",
                Latitud = -12.0800,
                Longitud = -77.0300,
                AceptacionTerminos = true
            };

            var result = await controller.RegistrarCentro(dto);
            var objectResult = Assert.IsType<ObjectResult>(result);
            Assert.Equal(StatusCodes.Status201Created, objectResult.StatusCode);

            var resultadoDto = Assert.IsType<ResultadoRegistroCentroDto>(objectResult.Value);
            Assert.True(resultadoDto.CentroId > 0);
            Assert.Equal("Pendiente", resultadoDto.Estado);
            Assert.Equal("Centro", resultadoDto.Rol);
        }

        [Fact]
        public async Task Test35_EdicionSensible_RegresaAPendiente_BloqueaOperacionPublicacionHastaNuevaAprobacion()
        {
            using var context = CreateInMemoryContext("ciclo_sensible_publicacion_db");
            var user = new Usuario { Id = 20, Correo = "titular@ciclosensible.pe", Rol = "Centro" };
            var centro = new Centro
            {
                Id = 15,
                UsuarioId = 20,
                Nombre = "Refugio Original",
                Descripcion = "Albergue inicial",
                Direccion = "Calle 1",
                Distrito = "Surco",
                Telefono = "999111222",
                CorreoContacto = "info@refugiooriginal.pe",
                Horario = "9 a 17",
                Latitud = -12.1300,
                Longitud = -76.9900,
                Estado = "Aprobado",
                Activo = true
            };
            context.Usuarios.Add(user);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var mascotasCtrl = new MascotasController(context, new FakeImageStorageService(), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("titular@ciclosensible.pe", "Centro", 20)
            };

            // 1. Centro Aprobado publica mascota -> Exitoso (201)
            var dtoPub1 = new CrearMascotaMultipartDto { Name = "Mascota 1", Species = "Perro" };
            var resPub1 = await mascotasCtrl.CrearMascotaConImagen(dtoPub1);
            Assert.IsType<CreatedAtActionResult>(resPub1.Result);

            // 2. Centro edita nombre sensiblemente
            var centrosCtrl = new CentrosController(context, authService, new FakeImageStorageService(), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("titular@ciclosensible.pe", "Centro", 20)
            };
            var dtoUpdate = new ActualizarMiCentroDto
            {
                Nombre = "Refugio Nombre Cambiado Sensible",
                Descripcion = centro.Descripcion,
                Direccion = centro.Direccion,
                Distrito = centro.Distrito,
                Telefono = centro.Telefono,
                CorreoContacto = centro.CorreoContacto,
                Horario = centro.Horario,
                Latitud = centro.Latitud,
                Longitud = centro.Longitud
            };
            var resUpdate = await centrosCtrl.ActualizarMiCentro(dtoUpdate);
            Assert.IsType<OkObjectResult>(resUpdate.Result);

            // Verificar que el estado regresó a Pendiente
            var centroActualizado = await context.Centros.FindAsync(15);
            Assert.Equal("Pendiente", centroActualizado!.Estado);

            // 3. Intento de publicar mascota en estado Pendiente -> Rechazado con 403 Forbidden
            var dtoPub2 = new CrearMascotaMultipartDto { Name = "Mascota 2", Species = "Gato" };
            var resPub2 = await mascotasCtrl.CrearMascotaConImagen(dtoPub2);
            var forbiddenRes = Assert.IsType<ObjectResult>(resPub2.Result);
            Assert.Equal(StatusCodes.Status403Forbidden, forbiddenRes.StatusCode);

            // 4. Administrador aprueba el centro tras evaluar el cambio
            var adminCtrl = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 1)
            };
            var resAprobar = await adminCtrl.AprobarCentro(15);
            Assert.IsType<OkObjectResult>(resAprobar);

            // 5. Tras la aprobación, el centro puede volver a publicar mascotas
            var resPub3 = await mascotasCtrl.CrearMascotaConImagen(dtoPub2);
            Assert.IsType<CreatedAtActionResult>(resPub3.Result);
        }
    }
}
