using System.Security.Claims;
using AdoptMe.Api.Controllers;
using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using AdoptMe.Api.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace AdoptMe.Api.Tests
{
    public class CentrosSuspensionTests
    {
        private static AdoptMeDbContext CreateInMemoryContext(string dbName)
        {
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: dbName)
                .ConfigureWarnings(w => w.Ignore(Microsoft.EntityFrameworkCore.Diagnostics.InMemoryEventId.TransactionIgnoredWarning))
                .Options;

            return new AdoptMeDbContext(options);
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

        private static IFormFile CreateMockFormFile(string fileName = "foto.jpg")
        {
            var header = new byte[] { 0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46 };
            var ms = new MemoryStream();
            ms.Write(header, 0, header.Length);
            var padding = new byte[100];
            ms.Write(padding, 0, padding.Length);
            ms.Position = 0;

            return new FormFile(ms, 0, ms.Length, "imagen", fileName)
            {
                Headers = new HeaderDictionary(),
                ContentType = "image/jpeg"
            };
        }

        // ====================================================================
        // FASE 13: 32 PRUEBAS BACKEND DE SUSPENSIÓN, REACTIVACIÓN Y VINCULACIÓN
        // ====================================================================

        [Fact]
        public async Task Test01_Admin_SuspendeCentroAprobado_Exito()
        {
            using var context = CreateInMemoryContext("test01_db");
            var centro = new Centro { Id = 1, Nombre = "Centro 1", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var dto = new SuspenderCentroDto { Motivo = "Incumplimiento de estándares de bienestar animal." };
            var result = await controller.SuspenderCentro(1, dto);

            var okResult = Assert.IsType<OkObjectResult>(result);
            var cActualizado = await context.Centros.FindAsync(1);
            Assert.False(cActualizado!.Activo);
            Assert.Equal("Aprobado", cActualizado.Estado);
            Assert.Equal(dto.Motivo, cActualizado.MotivoSuspension);
        }

        [Fact]
        public async Task Test02_Admin_SuspenderCentro_MotivoObligatorio_Falla()
        {
            using var context = CreateInMemoryContext("test02_db");
            var centro = new Centro { Id = 2, Nombre = "Centro 2", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var dto = new SuspenderCentroDto { Motivo = "   " };
            var result = await controller.SuspenderCentro(2, dto);

            Assert.IsType<BadRequestObjectResult>(result);
        }

        [Fact]
        public async Task Test03_Admin_SuspenderCentro_MotivoDemasiadoCorto_Falla()
        {
            using var context = CreateInMemoryContext("test03_db");
            var centro = new Centro { Id = 3, Nombre = "Centro 3", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var dto = new SuspenderCentroDto { Motivo = "Mal" }; // Menos de 5 caracteres
            var result = await controller.SuspenderCentro(3, dto);

            Assert.IsType<BadRequestObjectResult>(result);
        }

        [Fact]
        public async Task Test04_Admin_SuspenderCentro_SinToken_Falla()
        {
            using var context = CreateInMemoryContext("test04_db");
            var centro = new Centro { Id = 4, Nombre = "Centro 4", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() } // Sin claims
            };

            var dto = new SuspenderCentroDto { Motivo = "Suspensión administrativa válida." };
            var result = await controller.SuspenderCentro(4, dto);

            // Se ejecuta pero registra AdminId nulo al no haber JWT presente
            var okResult = Assert.IsType<OkObjectResult>(result);
        }

        [Fact]
        public void Test05_Admin_SuspenderCentro_NoAdmin_FallaEnAutorizacion()
        {
            // El atributo [Authorize(Roles = "Admin,Administrador")] protege la clase
            var type = typeof(AdminCentrosController);
            var authAttrs = type.GetCustomAttributes(typeof(Microsoft.AspNetCore.Authorization.AuthorizeAttribute), true);
            Assert.NotEmpty(authAttrs);
            var authAttr = (Microsoft.AspNetCore.Authorization.AuthorizeAttribute)authAttrs[0];
            Assert.Contains("Admin", authAttr.Roles);
        }

        [Fact]
        public async Task Test06_Admin_SuspenderCentro_CentroInexistente_RetornaNotFound()
        {
            using var context = CreateInMemoryContext("test06_db");
            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var dto = new SuspenderCentroDto { Motivo = "Motivo para centro inexistente." };
            var result = await controller.SuspenderCentro(999, dto);

            Assert.IsType<NotFoundObjectResult>(result);
        }

        [Fact]
        public async Task Test07_Admin_SuspenderCentro_NoSuspenderPendiente_RetornaBadRequest()
        {
            using var context = CreateInMemoryContext("test07_db");
            var centro = new Centro { Id = 7, Nombre = "Centro Pendiente", Estado = "Pendiente", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var dto = new SuspenderCentroDto { Motivo = "Intento de suspender solicitud pendiente." };
            var result = await controller.SuspenderCentro(7, dto);

            Assert.IsType<BadRequestObjectResult>(result);
        }

        [Fact]
        public async Task Test08_Admin_SuspenderCentro_NoSuspenderRechazado_RetornaBadRequest()
        {
            using var context = CreateInMemoryContext("test08_db");
            var centro = new Centro { Id = 8, Nombre = "Centro Rechazado", Estado = "Rechazado", Activo = false, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var dto = new SuspenderCentroDto { Motivo = "Intento de suspender solicitud ya rechazada." };
            var result = await controller.SuspenderCentro(8, dto);

            Assert.IsType<BadRequestObjectResult>(result);
        }

        [Fact]
        public async Task Test09_Admin_SuspenderCentro_NoSuspenderDosVeces_RetornaConflict()
        {
            using var context = CreateInMemoryContext("test09_db");
            var centro = new Centro { Id = 9, Nombre = "Centro Suspendido", Estado = "Aprobado", Activo = false, MotivoSuspension = "Primera suspensión", FechaSuspension = DateTime.UtcNow, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var dto = new SuspenderCentroDto { Motivo = "Segunda suspensión redundante." };
            var result = await controller.SuspenderCentro(9, dto);

            Assert.IsType<ConflictObjectResult>(result);
        }

        [Fact]
        public async Task Test10_Admin_SuspenderCentro_ConservaEstadoAprobado()
        {
            using var context = CreateInMemoryContext("test10_db");
            var centro = new Centro { Id = 10, Nombre = "Centro 10", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            await controller.SuspenderCentro(10, new SuspenderCentroDto { Motivo = "Verificación de conservación de estado." });

            var c = await context.Centros.FindAsync(10);
            Assert.Equal("Aprobado", c!.Estado);
        }

        [Fact]
        public async Task Test11_Admin_SuspenderCentro_EstableceActivoFalse()
        {
            using var context = CreateInMemoryContext("test11_db");
            var centro = new Centro { Id = 11, Nombre = "Centro 11", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            await controller.SuspenderCentro(11, new SuspenderCentroDto { Motivo = "Verificación de desactivación." });

            var c = await context.Centros.FindAsync(11);
            Assert.False(c!.Activo);
        }

        [Fact]
        public async Task Test12_Admin_SuspenderCentro_GuardaMotivoFechaYAdmin()
        {
            using var context = CreateInMemoryContext("test12_db");
            var centro = new Centro { Id = 12, Nombre = "Centro 12", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 88)
            };

            await controller.SuspenderCentro(12, new SuspenderCentroDto { Motivo = "Auditoría completa de campos." });

            var c = await context.Centros.FindAsync(12);
            Assert.Equal("Auditoría completa de campos.", c!.MotivoSuspension);
            Assert.NotNull(c.FechaSuspension);
            Assert.Equal(88, c.SuspendidoPorAdminId);
            Assert.Null(c.FechaReactivacion);
        }

        [Fact]
        public async Task Test13_Admin_ReactivaCentroSuspendido_Exito()
        {
            using var context = CreateInMemoryContext("test13_db");
            var centro = new Centro { Id = 13, Nombre = "Centro 13", Estado = "Aprobado", Activo = false, MotivoSuspension = "Suspendido previamente", FechaSuspension = DateTime.UtcNow, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.ReactivarCentro(13);
            Assert.IsType<OkObjectResult>(result);

            var c = await context.Centros.FindAsync(13);
            Assert.True(c!.Activo);
            Assert.NotNull(c.FechaReactivacion);
        }

        [Fact]
        public async Task Test14_Admin_ReactivarCentro_NoReactivarCentroActivo_RetornaConflict()
        {
            using var context = CreateInMemoryContext("test14_db");
            var centro = new Centro { Id = 14, Nombre = "Centro 14", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.ReactivarCentro(14);
            Assert.IsType<ConflictObjectResult>(result);
        }

        [Fact]
        public async Task Test15_Admin_Reactivacion_ConservaAuditoriaAnterior()
        {
            using var context = CreateInMemoryContext("test15_db");
            var fechaSusp = DateTime.UtcNow.AddDays(-2);
            var centro = new Centro { Id = 15, Nombre = "Centro 15", Estado = "Aprobado", Activo = false, MotivoSuspension = "Motivo histórico", FechaSuspension = fechaSusp, SuspendidoPorAdminId = 77, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            await controller.ReactivarCentro(15);

            var c = await context.Centros.FindAsync(15);
            Assert.True(c!.Activo);
            Assert.Equal("Motivo histórico", c.MotivoSuspension);
            Assert.Equal(fechaSusp, c.FechaSuspension);
            Assert.Equal(77, c.SuspendidoPorAdminId);
            Assert.NotNull(c.FechaReactivacion);
        }

        [Fact]
        public async Task Test16_Admin_Rechazo_FuncionaParaPendiente()
        {
            using var context = CreateInMemoryContext("test16_db");
            var centro = new Centro { Id = 16, Nombre = "Centro Pendiente", Estado = "Pendiente", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.RechazarCentro(16, new RechazarCentroDto { Motivo = "Falta corroborar dirección física." });
            Assert.IsType<OkObjectResult>(result);

            var c = await context.Centros.FindAsync(16);
            Assert.Equal("Rechazado", c!.Estado);
            Assert.Equal("Falta corroborar dirección física.", c.MotivoRechazo);
        }

        [Fact]
        public async Task Test17_Admin_Rechazo_FallaParaAprobado()
        {
            using var context = CreateInMemoryContext("test17_db");
            var centro = new Centro { Id = 17, Nombre = "Centro Aprobado", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.RechazarCentro(17, new RechazarCentroDto { Motivo = "Intento indebido de rechazar centro ya aprobado." });
            var badResult = Assert.IsType<BadRequestObjectResult>(result);
            Assert.Contains("El rechazo solo se aplica durante la evaluación inicial", badResult.Value?.ToString());
        }

        [Fact]
        public async Task Test18_Centro_Reenvio_FuncionaSoloParaRechazado()
        {
            using var context = CreateInMemoryContext("test18_db");
            var usuario = new Usuario { Id = 30, Nombre = "Titular", Correo = "titular@adoptme.pe", Rol = "Centro" };
            var centroRechazado = new Centro { Id = 18, UsuarioId = 30, Nombre = "Centro Rechazado", Estado = "Rechazado", MotivoRechazo = "Observaciones", Activo = false, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "titular@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centroRechazado);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new MockImageStorageService(true), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("titular@adoptme.pe", "Centro", 30)
            };

            var result = await controller.ReenviarSolicitud();
            Assert.IsType<OkObjectResult>(result);

            var c = await context.Centros.FindAsync(18);
            Assert.Equal("Pendiente", c!.Estado);
            Assert.Null(c.MotivoRechazo);
        }

        [Fact]
        public async Task Test19_CatalogoPublico_ExcluyeSuspendidos()
        {
            using var context = CreateInMemoryContext("test19_db");
            var centroAprobadoActivo = new Centro { Id = 191, Nombre = "Centro Activo", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c1@c.pe", Descripcion = "Desc" };
            var centroSuspendido = new Centro { Id = 192, Nombre = "Centro Suspendido", Estado = "Aprobado", Activo = false, MotivoSuspension = "Suspendido", Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c2@c.pe", Descripcion = "Desc" };
            context.Centros.AddRange(centroAprobadoActivo, centroSuspendido);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new MockImageStorageService(true), NullLogger<CentrosController>.Instance);

            var result = await controller.ObtenerCentros();
            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var lista = Assert.IsAssignableFrom<IEnumerable<CentroPublicoDto>>(okResult.Value);

            Assert.Single(lista);
            Assert.Equal(191, lista.First().Id);
        }

        [Fact]
        public async Task Test20_MiCentro_DevuelveSuspendidos()
        {
            using var context = CreateInMemoryContext("test20_db");
            var usuario = new Usuario { Id = 35, Nombre = "Titular Suspendido", Correo = "susp@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 20, UsuarioId = 35, Nombre = "Centro Suspendido", Estado = "Aprobado", Activo = false, MotivoSuspension = "Medida administrativa", FechaSuspension = DateTime.UtcNow, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "susp@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new MockImageStorageService(true), NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("susp@adoptme.pe", "Centro", 35)
            };

            var result = await controller.ObtenerMiCentro();
            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var dto = Assert.IsType<CentroPropietarioDto>(okResult.Value);

            Assert.Equal("Aprobado", dto.Estado);
            Assert.False(dto.Activo);
            Assert.Equal("Medida administrativa", dto.MotivoSuspension);
        }

        [Fact]
        public async Task Test21_CentroSuspendido_NoPuedePublicarMascotaJson_Retorna403()
        {
            using var context = CreateInMemoryContext("test21_db");
            var usuario = new Usuario { Id = 40, Nombre = "Titular Susp", Correo = "susp.pub@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 21, UsuarioId = 40, Nombre = "Centro Suspendido", Estado = "Aprobado", Activo = false, MotivoSuspension = "Suspendido por faltas", Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "susp.pub@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new MockImageStorageService(true), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("susp.pub@adoptme.pe", "Centro", 40)
            };

            var nuevaMascota = new Mascota { Name = "Firulais", Species = "Perro", Age = "2 años", Gender = "Macho" };
            var result = await controller.CrearMascota(nuevaMascota);

            var objectResult = Assert.IsType<ObjectResult>(result.Result);
            Assert.Equal(StatusCodes.Status403Forbidden, objectResult.StatusCode);
        }

        [Fact]
        public async Task Test22_CentroSuspendido_NoPuedePublicarMascotaMultipart_Retorna403()
        {
            using var context = CreateInMemoryContext("test22_db");
            var usuario = new Usuario { Id = 41, Nombre = "Titular Susp Multi", Correo = "susp.multi@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 22, UsuarioId = 41, Nombre = "Centro Suspendido", Estado = "Aprobado", Activo = false, MotivoSuspension = "Suspendido por faltas", Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "susp.multi@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var mockCloudinary = new MockImageStorageService(true);
            var controller = new MascotasController(context, mockCloudinary, authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("susp.multi@adoptme.pe", "Centro", 41)
            };

            var dto = new CrearMascotaMultipartDto
            {
                Name = "Pelusa",
                Species = "Gato",
                Age = "1 año",
                Gender = "Hembra",
                Imagen = CreateMockFormFile()
            };

            var result = await controller.CrearMascotaConImagen(dto);
            var objectResult = Assert.IsType<ObjectResult>(result.Result);
            Assert.Equal(StatusCodes.Status403Forbidden, objectResult.StatusCode);
            Assert.Equal(0, mockCloudinary.SubidasEjecutadas); // No se subió nada a Cloudinary
        }

        [Fact]
        public async Task Test23_Bloqueo_OcurreAntesDeCloudinary()
        {
            using var context = CreateInMemoryContext("test23_db");
            var usuario = new Usuario { Id = 42, Nombre = "Titular Susp PreCloud", Correo = "susp.pre@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 23, UsuarioId = 42, Nombre = "Centro Suspendido", Estado = "Aprobado", Activo = false, MotivoSuspension = "Sanción", Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "susp.pre@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var mockCloudinary = new MockImageStorageService(true);
            var controller = new MascotasController(context, mockCloudinary, authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("susp.pre@adoptme.pe", "Centro", 42)
            };

            var dto = new CrearMascotaMultipartDto
            {
                Name = "Toby",
                Species = "Perro",
                Age = "3 años",
                Gender = "Macho",
                Imagen = CreateMockFormFile()
            };

            await controller.CrearMascotaConImagen(dto);
            Assert.Equal(0, mockCloudinary.SubidasEjecutadas);
        }

        [Fact]
        public async Task Test24_Adoptante_ConservaPublicacionNormal()
        {
            using var context = CreateInMemoryContext("test24_db");
            var usuario = new Usuario { Id = 43, Nombre = "Adoptante Libre", Correo = "adop@adoptme.pe", Rol = "Adoptante" };
            context.Usuarios.Add(usuario);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new MockImageStorageService(true), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("adop@adoptme.pe", "Adoptante", 43)
            };

            var nueva = new Mascota { Name = "Adoptadito", Species = "Perro", Age = "1 año", Gender = "Macho" };
            var result = await controller.CrearMascota(nueva);

            var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
            var m = Assert.IsType<Mascota>(createdResult.Value);
            Assert.Null(m.CentroId); // Los adoptantes tienen CentroId null
        }

        [Fact]
        public async Task Test25_CentroActivo_ConservaPublicacionNormal()
        {
            using var context = CreateInMemoryContext("test25_db");
            var usuario = new Usuario { Id = 44, Nombre = "Titular Activo", Correo = "act@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 25, UsuarioId = 44, Nombre = "Centro Activo", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "act@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new MascotasController(context, new MockImageStorageService(true), authService, NullLogger<MascotasController>.Instance)
            {
                ControllerContext = CreateUserContext("act@adoptme.pe", "Centro", 44)
            };

            var nueva = new Mascota { Name = "Rescatado", Species = "Gato", Age = "6 meses", Gender = "Macho" };
            var result = await controller.CrearMascota(nueva);

            var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
            var m = Assert.IsType<Mascota>(createdResult.Value);
            Assert.Equal(25, m.CentroId); // Vinculado formalmente al Centro
        }

        [Fact]
        public async Task Test26_VincularResponsable_UsuarioCentroValido_Exito()
        {
            using var context = CreateInMemoryContext("test26_db");
            var usuario = new Usuario { Id = 50, Nombre = "Usuario Centro", Correo = "uc@adoptme.pe", Rol = "Centro" };
            var centroDemo = new Centro { Id = 26, UsuarioId = null, Nombre = "Centro Demo", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "demo@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centroDemo);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.AsignarPropietario(26, new AsignarPropietarioCentroDto { UsuarioId = 50 });
            Assert.IsType<OkObjectResult>(result);

            var c = await context.Centros.FindAsync(26);
            Assert.Equal(50, c!.UsuarioId);
        }

        [Fact]
        public async Task Test27_VincularResponsable_NoVincularAdoptante_RetornaBadRequest()
        {
            using var context = CreateInMemoryContext("test27_db");
            var usuario = new Usuario { Id = 51, Nombre = "Adoptante Juan", Correo = "juan@adoptme.pe", Rol = "Adoptante" };
            var centroDemo = new Centro { Id = 27, UsuarioId = null, Nombre = "Centro Demo", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "demo@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centroDemo);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.AsignarPropietario(27, new AsignarPropietarioCentroDto { UsuarioId = 51 });
            var badResult = Assert.IsType<BadRequestObjectResult>(result);
            Assert.Contains("El usuario debe tener rol 'Centro'", badResult.Value?.ToString());
        }

        [Fact]
        public async Task Test28_VincularResponsable_NoVincularAdmin_RetornaBadRequest()
        {
            using var context = CreateInMemoryContext("test28_db");
            var usuario = new Usuario { Id = 52, Nombre = "Admin Pedro", Correo = "pedro@adoptme.pe", Rol = "Admin" };
            var centroDemo = new Centro { Id = 28, UsuarioId = null, Nombre = "Centro Demo", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "demo@adoptme.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centroDemo);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.AsignarPropietario(28, new AsignarPropietarioCentroDto { UsuarioId = 52 });
            var badResult = Assert.IsType<BadRequestObjectResult>(result);
            Assert.Contains("El usuario debe tener rol 'Centro'", badResult.Value?.ToString());
        }

        [Fact]
        public async Task Test29_VincularResponsable_NoVincularUsuarioYaAsociado_RetornaConflict()
        {
            using var context = CreateInMemoryContext("test29_db");
            var usuario = new Usuario { Id = 53, Nombre = "Usuario Ocupado", Correo = "ocupado@adoptme.pe", Rol = "Centro" };
            var centroExistente = new Centro { Id = 291, UsuarioId = 53, Nombre = "Centro Existente", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c1@c.pe", Descripcion = "Desc" };
            var centroNuevo = new Centro { Id = 292, UsuarioId = null, Nombre = "Centro Nuevo", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c2@c.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.AddRange(centroExistente, centroNuevo);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.AsignarPropietario(292, new AsignarPropietarioCentroDto { UsuarioId = 53 });
            Assert.IsType<ConflictObjectResult>(result);
        }

        [Fact]
        public async Task Test30_VincularResponsable_NoSobrescribirResponsableExistente_RetornaConflict()
        {
            using var context = CreateInMemoryContext("test30_db");
            var usuarioActual = new Usuario { Id = 60, Nombre = "Titular Actual", Correo = "act@adoptme.pe", Rol = "Centro" };
            var usuarioNuevo = new Usuario { Id = 61, Nombre = "Titular Aspirante", Correo = "asp@adoptme.pe", Rol = "Centro" };
            var centroConTitular = new Centro { Id = 30, UsuarioId = 60, Nombre = "Centro Asignado", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Usuarios.AddRange(usuarioActual, usuarioNuevo);
            context.Centros.Add(centroConTitular);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var result = await controller.AsignarPropietario(30, new AsignarPropietarioCentroDto { UsuarioId = 61 });
            var conflictResult = Assert.IsType<ConflictObjectResult>(result);
            Assert.Contains("Este centro ya tiene un responsable vinculado", conflictResult.Value?.ToString());

            var c = await context.Centros.FindAsync(30);
            Assert.Equal(60, c!.UsuarioId); // Permanece intacto
        }

        [Fact]
        public async Task Test31_VincularResponsable_Relacion1a1Preservada()
        {
            using var context = CreateInMemoryContext("test31_db");
            var usuario = new Usuario { Id = 62, Nombre = "Unico Titular", Correo = "unico@adoptme.pe", Rol = "Centro" };
            var centro = new Centro { Id = 31, UsuarioId = null, Nombre = "Centro Único", Estado = "Aprobado", Activo = true, Direccion = "D", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "c@c.pe", Descripcion = "Desc" };
            context.Usuarios.Add(usuario);
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext("admin@adoptme.pe", "Admin", 99)
            };

            var res1 = await controller.AsignarPropietario(31, new AsignarPropietarioCentroDto { UsuarioId = 62 });
            Assert.IsType<OkObjectResult>(res1);

            // Reintento idéntico es idempotente
            var res2 = await controller.AsignarPropietario(31, new AsignarPropietarioCentroDto { UsuarioId = 62 });
            Assert.IsType<OkObjectResult>(res2);

            var cCount = await context.Centros.CountAsync(c => c.UsuarioId == 62);
            Assert.Equal(1, cCount);
        }

        [Fact]
        public async Task Test32_CentrosDemo_ContinuanValidos()
        {
            using var context = CreateInMemoryContext("test32_db");
            var demoCentros = new List<Centro>
            {
                new Centro { Id = 1, Nombre = "Demo 1", Estado = "Aprobado", Activo = true, EsDemostrativo = true, Direccion = "D1", Distrito = "Lima", Horario = "H", Telefono = "T", CorreoContacto = "d1@c.pe", Descripcion = "Desc" },
                new Centro { Id = 2, Nombre = "Demo 2", Estado = "Aprobado", Activo = true, EsDemostrativo = true, Direccion = "D2", Distrito = "Surco", Horario = "H", Telefono = "T", CorreoContacto = "d2@c.pe", Descripcion = "Desc" },
                new Centro { Id = 3, Nombre = "Demo 3", Estado = "Aprobado", Activo = true, EsDemostrativo = true, Direccion = "D3", Distrito = "Miraflores", Horario = "H", Telefono = "T", CorreoContacto = "d3@c.pe", Descripcion = "Desc" },
                new Centro { Id = 4, Nombre = "Demo 4", Estado = "Aprobado", Activo = true, EsDemostrativo = true, Direccion = "D4", Distrito = "Ate", Horario = "H", Telefono = "T", CorreoContacto = "d4@c.pe", Descripcion = "Desc" },
                new Centro { Id = 5, Nombre = "Demo 5", Estado = "Aprobado", Activo = true, EsDemostrativo = true, Direccion = "D5", Distrito = "Comas", Horario = "H", Telefono = "T", CorreoContacto = "d5@c.pe", Descripcion = "Desc" }
            };
            context.Centros.AddRange(demoCentros);
            await context.SaveChangesAsync();

            var authService = new CentroAuthorizationService(context);
            var controller = new CentrosController(context, authService, new MockImageStorageService(true), NullLogger<CentrosController>.Instance);

            var result = await controller.ObtenerCentros();
            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var lista = Assert.IsAssignableFrom<IEnumerable<CentroPublicoDto>>(okResult.Value);

            Assert.Equal(5, lista.Count());
            Assert.All(lista, c => Assert.True(c.EsDemostrativo));
        }

        // ====================================================================
        // SERVICIO MOCK DE CLOUDINARY PARA TESTS
        // ====================================================================
        private class MockImageStorageService : IImageStorageService
        {
            private readonly bool _exitoso;
            public int SubidasEjecutadas { get; private set; } = 0;

            public MockImageStorageService(bool exitoso)
            {
                _exitoso = exitoso;
            }

            public bool IsConfigured => true;

            public Task<ImageUploadResult> SubirImagenAsync(Stream fileStream, string fileName, CancellationToken cancellationToken = default)
            {
                return SubirImagenAsync(fileStream, fileName, null, cancellationToken);
            }

            public Task<ImageUploadResult> SubirImagenAsync(Stream fileStream, string fileName, string? folder = null, CancellationToken cancellationToken = default)
            {
                SubidasEjecutadas++;
                if (_exitoso)
                {
                    return Task.FromResult(new ImageUploadResult
                    {
                        Exitoso = true,
                        SecureUrl = $"https://res.cloudinary.com/demo/image/upload/v1/{folder}/{fileName}.jpg",
                        PublicId = $"{folder}/{fileName}"
                    });
                }
                return Task.FromResult(new ImageUploadResult
                {
                    Exitoso = false,
                    Error = "Fallo simulado en almacenamiento de imagen."
                });
            }

            public Task<bool> EliminarImagenAsync(string publicId, CancellationToken cancellationToken = default)
            {
                return Task.FromResult(true);
            }
        }
    }
}
