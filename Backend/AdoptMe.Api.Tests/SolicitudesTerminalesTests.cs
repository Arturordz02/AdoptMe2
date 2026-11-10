using System;
using System.Collections.Generic;
using System.Security.Claims;
using System.Threading.Tasks;
using AdoptMe.Api.Controllers;
using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace AdoptMe.Api.Tests
{
    public class SolicitudesTerminalesTests
    {
        private AdoptMeDbContext GetInMemoryDbContext()
        {
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
                .Options;

            return new AdoptMeDbContext(options);
        }

        private SolicitudesController CreateController(AdoptMeDbContext context, string? role = "Admin", int userId = 1, string email = "admin@adoptme.com")
        {
            var controller = new SolicitudesController(context);

            var claims = new List<Claim>();
            if (role != null)
            {
                claims.Add(new Claim(ClaimTypes.Role, role));
                claims.Add(new Claim(ClaimTypes.NameIdentifier, userId.ToString()));
                claims.Add(new Claim(ClaimTypes.Email, email));
            }

            var identity = role != null ? new ClaimsIdentity(claims, "TestAuth") : new ClaimsIdentity();
            var principal = new ClaimsPrincipal(identity);

            controller.ControllerContext = new ControllerContext
            {
                HttpContext = new DefaultHttpContext { User = principal }
            };

            return controller;
        }

        [Fact]
        public async Task Test1_SolicitudYaAprobada_Devuelve409Conflict()
        {
            using var context = GetInMemoryDbContext();
            context.Mascotas.Add(new Mascota { Id = 1, Name = "Bobby", Species = "Perro", Status = "Adoptado" });
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 1,
                MascotaId = 1,
                AdoptanteId = 10,
                PetName = "Bobby",
                Email = "user1@adoptme.com",
                Status = "Aprobada"
            });
            await context.SaveChangesAsync();

            var controller = CreateController(context);
            var dto = new ActualizarEstadoSolicitudDto { Status = "Rechazada", Observacion = "Motivo de prueba posterior" };

            var result = await controller.ActualizarEstado(1, dto);

            var conflictResult = Assert.IsType<ConflictObjectResult>(result.Result);
            Assert.Contains("terminal", conflictResult.Value?.ToString() ?? "", StringComparison.OrdinalIgnoreCase);
        }

        [Fact]
        public async Task Test2_SolicitudYaRechazada_Devuelve409Conflict()
        {
            using var context = GetInMemoryDbContext();
            context.Mascotas.Add(new Mascota { Id = 1, Name = "Bobby", Species = "Perro", Status = "Disponible" });
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 1,
                MascotaId = 1,
                AdoptanteId = 10,
                PetName = "Bobby",
                Email = "user1@adoptme.com",
                Status = "Rechazada",
                Observacion = "Rechazado previamente por falta de espacio"
            });
            await context.SaveChangesAsync();

            var controller = CreateController(context);
            var dto = new ActualizarEstadoSolicitudDto { Status = "Aprobada" };

            var result = await controller.ActualizarEstado(1, dto);

            var conflictResult = Assert.IsType<ConflictObjectResult>(result.Result);
            Assert.Contains("terminal", conflictResult.Value?.ToString() ?? "", StringComparison.OrdinalIgnoreCase);
        }

        [Theory]
        [InlineData(null)]
        [InlineData("")]
        [InlineData("    ")]
        [InlineData("No")]
        [InlineData("1234")]
        public async Task Test3_RechazarSinMotivoValido_Devuelve400BadRequest(string? motivoInvalido)
        {
            using var context = GetInMemoryDbContext();
            context.Mascotas.Add(new Mascota { Id = 1, Name = "Bobby", Species = "Perro", Status = "Disponible" });
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 1,
                MascotaId = 1,
                AdoptanteId = 10,
                PetName = "Bobby",
                Email = "user1@adoptme.com",
                Status = "Pendiente"
            });
            await context.SaveChangesAsync();

            var controller = CreateController(context);
            var dto = new ActualizarEstadoSolicitudDto { Status = "Rechazada", Observacion = motivoInvalido };

            var result = await controller.ActualizarEstado(1, dto);

            var badRequest = Assert.IsType<BadRequestObjectResult>(result.Result);
            Assert.Contains("5 caracteres", badRequest.Value?.ToString() ?? "");
        }

        [Fact]
        public async Task Test4_RechazarConMotivoValido_ActualizaYPersisteObservacion()
        {
            using var context = GetInMemoryDbContext();
            context.Mascotas.Add(new Mascota { Id = 1, Name = "Bobby", Species = "Perro", Status = "Disponible" });
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 1,
                MascotaId = 1,
                AdoptanteId = 10,
                PetName = "Bobby",
                Email = "user1@adoptme.com",
                Status = "Pendiente"
            });
            await context.SaveChangesAsync();

            var controller = CreateController(context);
            var motivo = "El adoptante no cumple con el espacio mínimo requerido.";
            var dto = new ActualizarEstadoSolicitudDto { Status = "Rechazada", Observacion = motivo };

            var result = await controller.ActualizarEstado(1, dto);

            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var sol = await context.Solicitudes.FindAsync(1);
            Assert.NotNull(sol);
            Assert.Equal("Rechazada", sol.Status);
            Assert.Equal(motivo, sol.Observacion);
        }

        [Fact]
        public async Task Test5_AprobarSolicitud_AutoRechazaOtrasPendientesConObservacionAutomatica()
        {
            using var context = GetInMemoryDbContext();
            context.Mascotas.Add(new Mascota { Id = 5, Name = "Luna", Species = "Gato", Status = "Disponible" });

            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 10,
                MascotaId = 5,
                AdoptanteId = 1,
                PetName = "Luna",
                Email = "ganador@adoptme.com",
                Status = "Pendiente"
            });
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 11,
                MascotaId = 5,
                AdoptanteId = 2,
                PetName = "Luna",
                Email = "otro1@adoptme.com",
                Status = "Pendiente"
            });
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 12,
                MascotaId = 5,
                AdoptanteId = 3,
                PetName = "Luna",
                Email = "otro2@adoptme.com",
                Status = "En revisión"
            });
            await context.SaveChangesAsync();

            var controller = CreateController(context);
            var dto = new ActualizarEstadoSolicitudDto { Status = "Aprobada" };

            var result = await controller.ActualizarEstado(10, dto);
            Assert.IsType<OkObjectResult>(result.Result);

            var sol10 = await context.Solicitudes.FindAsync(10);
            var sol11 = await context.Solicitudes.FindAsync(11);
            var sol12 = await context.Solicitudes.FindAsync(12);

            Assert.Equal("Aprobada", sol10?.Status);
            Assert.Equal("Rechazada", sol11?.Status);
            Assert.Equal("Rechazada", sol12?.Status);
            Assert.Contains("otra solicitud aprobada", sol11?.Observacion ?? "");
            Assert.Contains("otra solicitud aprobada", sol12?.Observacion ?? "");
        }
    }
}
