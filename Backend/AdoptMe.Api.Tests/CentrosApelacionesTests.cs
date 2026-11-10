using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Claims;
using System.Threading;
using System.Threading.Tasks;
using AdoptMe.Api.Controllers;
using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using AdoptMe.Api.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace AdoptMe.Api.Tests
{
    public class CentrosApelacionesTests
    {
        private class MockImageStorageService : IImageStorageService
        {
            public bool IsConfigured => true;
            public Task<ImageUploadResult> SubirImagenAsync(Stream fileStream, string fileName, CancellationToken cancellationToken = default)
            {
                return Task.FromResult(new ImageUploadResult { Exitoso = true, SecureUrl = "https://example.com/logo.jpg", PublicId = "logo1" });
            }
            public Task<ImageUploadResult> SubirImagenAsync(Stream fileStream, string fileName, string? folder = null, CancellationToken cancellationToken = default)
            {
                return Task.FromResult(new ImageUploadResult { Exitoso = true, SecureUrl = "https://example.com/logo.jpg", PublicId = "logo1" });
            }
            public Task<bool> EliminarImagenAsync(string publicId, CancellationToken cancellationToken = default)
            {
                return Task.FromResult(true);
            }
        }

        private static AdoptMeDbContext CreateInMemoryContext()
        {
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
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

        private static CentrosController CreateCentrosController(AdoptMeDbContext context, string email, string role, int userId)
        {
            var authService = new CentroAuthorizationService(context);
            var mockImageService = new MockImageStorageService();
            var controller = new CentrosController(context, authService, mockImageService, NullLogger<CentrosController>.Instance)
            {
                ControllerContext = CreateUserContext(email, role, userId)
            };
            return controller;
        }

        private static AdminCentrosController CreateAdminController(AdoptMeDbContext context, string email = "admin@adoptme.pe", string role = "Admin", int userId = 99)
        {
            var controller = new AdminCentrosController(context, NullLogger<AdminCentrosController>.Instance)
            {
                ControllerContext = CreateUserContext(email, role, userId)
            };
            return controller;
        }

        [Fact]
        public async Task Test1_CentroSuspendido_CreaApelacion_Exito()
        {
            using var context = CreateInMemoryContext();
            context.Usuarios.Add(new Usuario { Id = 10, Nombre = "Titular", Correo = "esperanza@adoptme.pe", Rol = "Centro" });
            var centro = new Centro
            {
                Id = 1,
                Nombre = "Albergue Esperanza",
                Estado = "Aprobado",
                Activo = false,
                MotivoSuspension = "Falta de actualización de datos de contacto",
                FechaSuspension = DateTime.UtcNow.AddDays(-2),
                UsuarioId = 10,
                Direccion = "Av Lima 123",
                Distrito = "Miraflores",
                Horario = "9am - 6pm",
                Telefono = "999888777",
                CorreoContacto = "esperanza@adoptme.pe",
                Descripcion = "Albergue de mascotas"
            };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = CreateCentrosController(context, "esperanza@adoptme.pe", "Centro", 10);
            var req = new CrearApelacionDto
            {
                MotivoApelacion = "Ya hemos actualizado todos los datos de contacto y teléfonos de emergencia."
            };

            var result = await controller.CrearApelacion(req);

            var createdResult = Assert.IsType<OkObjectResult>(result.Result);
            var apelacionDto = Assert.IsType<CentroApelacionDto>(createdResult.Value);
            Assert.Equal("Pendiente", apelacionDto.Estado);
            Assert.Equal(1, apelacionDto.CentroId);
            Assert.Equal(req.MotivoApelacion, apelacionDto.MotivoApelacion);
        }

        [Theory]
        [InlineData(null)]
        [InlineData("")]
        [InlineData("    ")]
        [InlineData("Muy corto")]
        public async Task Test2_CentroSuspendido_CreaApelacion_MotivoInvalido_Devuelve400(string? motivoInvalido)
        {
            using var context = CreateInMemoryContext();
            context.Usuarios.Add(new Usuario { Id = 20, Nombre = "Titular 2", Correo = "esperanza2@adoptme.pe", Rol = "Centro" });
            var centro = new Centro
            {
                Id = 2,
                Nombre = "Albergue Esperanza",
                Estado = "Aprobado",
                Activo = false,
                MotivoSuspension = "Revisión preventiva",
                UsuarioId = 20,
                Direccion = "Av Lima 123",
                Distrito = "Miraflores",
                Horario = "9am - 6pm",
                Telefono = "999888777",
                CorreoContacto = "esperanza2@adoptme.pe",
                Descripcion = "Albergue"
            };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = CreateCentrosController(context, "esperanza2@adoptme.pe", "Centro", 20);
            var req = new CrearApelacionDto { MotivoApelacion = motivoInvalido! };

            var result = await controller.CrearApelacion(req);

            var badRequest = Assert.IsType<BadRequestObjectResult>(result.Result);
            Assert.Contains("10 caracteres", badRequest.Value?.ToString() ?? "");
        }

        [Fact]
        public async Task Test3_CentroNoSuspendido_IntentaApelar_Devuelve400()
        {
            using var context = CreateInMemoryContext();
            context.Usuarios.Add(new Usuario { Id = 30, Nombre = "Titular 3", Correo = "activo@adoptme.pe", Rol = "Centro" });
            var centro = new Centro
            {
                Id = 3,
                Nombre = "Albergue Activo",
                Estado = "Aprobado",
                Activo = true,
                UsuarioId = 30,
                Direccion = "Av Lima 123",
                Distrito = "Miraflores",
                Horario = "9am - 6pm",
                Telefono = "999888777",
                CorreoContacto = "activo@adoptme.pe",
                Descripcion = "Albergue"
            };
            context.Centros.Add(centro);
            await context.SaveChangesAsync();

            var controller = CreateCentrosController(context, "activo@adoptme.pe", "Centro", 30);
            var req = new CrearApelacionDto
            {
                MotivoApelacion = "Queremos apelar aunque no estamos suspendidos en este momento."
            };

            var result = await controller.CrearApelacion(req);

            var badRequest = Assert.IsType<BadRequestObjectResult>(result.Result);
            Assert.Contains("suspendido", badRequest.Value?.ToString() ?? "", StringComparison.OrdinalIgnoreCase);
        }

        [Fact]
        public async Task Test4_CentroSuspendido_YaTieneApelacionPendiente_Devuelve409Conflict()
        {
            using var context = CreateInMemoryContext();
            context.Usuarios.Add(new Usuario { Id = 40, Nombre = "Titular 4", Correo = "albergue4@adoptme.pe", Rol = "Centro" });
            var centro = new Centro
            {
                Id = 4,
                Nombre = "Albergue 4",
                Estado = "Aprobado",
                Activo = false,
                MotivoSuspension = "Medida cautelar",
                UsuarioId = 40,
                Direccion = "Av Lima 123",
                Distrito = "Miraflores",
                Horario = "9am - 6pm",
                Telefono = "999888777",
                CorreoContacto = "albergue4@adoptme.pe",
                Descripcion = "Albergue"
            };
            context.Centros.Add(centro);
            context.CentrosApelaciones.Add(new CentroApelacion
            {
                Id = 1,
                CentroId = 4,
                MotivoApelacion = "Apelación existente previamente registrada",
                Estado = "Pendiente",
                FechaCreacion = DateTime.UtcNow
            });
            await context.SaveChangesAsync();

            var controller = CreateCentrosController(context, "albergue4@adoptme.pe", "Centro", 40);
            var req = new CrearApelacionDto
            {
                MotivoApelacion = "Segunda apelación que no debe proceder porque ya hay una en curso."
            };

            var result = await controller.CrearApelacion(req);

            var conflict = Assert.IsType<ConflictObjectResult>(result.Result);
            Assert.Contains("pendiente", conflict.Value?.ToString() ?? "", StringComparison.OrdinalIgnoreCase);
        }

        [Fact]
        public async Task Test5_CentroSuspendido_ConsultaApelacionPendiente_Exito()
        {
            using var context = CreateInMemoryContext();
            context.Usuarios.Add(new Usuario { Id = 50, Nombre = "Titular 5", Correo = "albergue5@adoptme.pe", Rol = "Centro" });
            var centro = new Centro
            {
                Id = 5,
                Nombre = "Albergue 5",
                Estado = "Aprobado",
                Activo = false,
                MotivoSuspension = "Revisión técnica",
                UsuarioId = 50,
                Direccion = "Av Lima 123",
                Distrito = "Miraflores",
                Horario = "9am - 6pm",
                Telefono = "999888777",
                CorreoContacto = "albergue5@adoptme.pe",
                Descripcion = "Albergue"
            };
            context.Centros.Add(centro);
            context.CentrosApelaciones.Add(new CentroApelacion
            {
                Id = 2,
                CentroId = 5,
                MotivoApelacion = "Apelación pendiente de revisión administrativa.",
                Estado = "Pendiente",
                FechaCreacion = DateTime.UtcNow
            });
            await context.SaveChangesAsync();

            var controller = CreateCentrosController(context, "albergue5@adoptme.pe", "Centro", 50);

            var result = await controller.ObtenerApelacionPendiente();

            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var dto = Assert.IsType<CentroApelacionDto>(okResult.Value);
            Assert.Equal(2, dto.Id);
            Assert.Equal("Pendiente", dto.Estado);
        }

        [Fact]
        public async Task Test6_Admin_ApruebaApelacion_ReactivaCentroYActualizaApelacion()
        {
            using var context = CreateInMemoryContext();
            var centro = new Centro
            {
                Id = 6,
                Nombre = "Albergue 6",
                Estado = "Aprobado",
                Activo = false,
                MotivoSuspension = "Documentación vencida",
                FechaSuspension = DateTime.UtcNow.AddDays(-5),
                UsuarioId = 60,
                Direccion = "Av Lima 123",
                Distrito = "Miraflores",
                Horario = "9am - 6pm",
                Telefono = "999888777",
                CorreoContacto = "albergue6@adoptme.pe",
                Descripcion = "Albergue"
            };
            context.Centros.Add(centro);

            var apelacion = new CentroApelacion
            {
                Id = 10,
                CentroId = 6,
                MotivoApelacion = "Se adjuntó la documentación renovada y aprobada por la municipalidad.",
                Estado = "Pendiente",
                FechaCreacion = DateTime.UtcNow.AddDays(-1)
            };
            context.CentrosApelaciones.Add(apelacion);
            await context.SaveChangesAsync();

            var adminController = CreateAdminController(context, "admin@adoptme.pe", "Admin", 99);

            var result = await adminController.AprobarApelacion(10);

            var okResult = Assert.IsType<OkObjectResult>(result);
            var centroActualizado = await context.Centros.FindAsync(6);
            Assert.NotNull(centroActualizado);
            Assert.True(centroActualizado.Activo);
            Assert.Null(centroActualizado.MotivoSuspension);
            Assert.NotNull(centroActualizado.FechaReactivacion);
        }

        [Fact]
        public async Task Test7_Admin_RechazaApelacion_MantieneSuspensionYActualizaApelacion()
        {
            using var context = CreateInMemoryContext();
            var centro = new Centro
            {
                Id = 7,
                Nombre = "Albergue 7",
                Estado = "Aprobado",
                Activo = false,
                MotivoSuspension = "Denuncias sanitarias",
                FechaSuspension = DateTime.UtcNow.AddDays(-10),
                UsuarioId = 70,
                Direccion = "Av Lima 123",
                Distrito = "Miraflores",
                Horario = "9am - 6pm",
                Telefono = "999888777",
                CorreoContacto = "albergue7@adoptme.pe",
                Descripcion = "Albergue"
            };
            context.Centros.Add(centro);

            var apelacion = new CentroApelacion
            {
                Id = 20,
                CentroId = 7,
                MotivoApelacion = "Solicitamos reconsideración inmediata de la medida preventiva.",
                Estado = "Pendiente",
                FechaCreacion = DateTime.UtcNow.AddDays(-1)
            };
            context.CentrosApelaciones.Add(apelacion);
            await context.SaveChangesAsync();

            var adminController = CreateAdminController(context, "admin@adoptme.pe", "Admin", 99);
            var req = new ResolverApelacionDto
            {
                RespuestaAdmin = "La inspección no ha sido subsanada en su totalidad. Se mantiene la suspensión."
            };

            var result = await adminController.RechazarApelacion(20, req);

            var okResult = Assert.IsType<OkObjectResult>(result);

            var centroActualizado = await context.Centros.FindAsync(7);
            Assert.NotNull(centroActualizado);
            Assert.False(centroActualizado.Activo);
            Assert.Equal("Denuncias sanitarias", centroActualizado.MotivoSuspension);
        }
    }
}
