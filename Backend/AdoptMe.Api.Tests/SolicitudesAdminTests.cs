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
    public class SolicitudesAdminTests
    {
        private AdoptMeDbContext GetInMemoryDbContext()
        {
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
                .Options;

            return new AdoptMeDbContext(options);
        }

        private SolicitudesController CreateController(AdoptMeDbContext context, string? role = null, int userId = 1, string email = "admin@adoptme.com")
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
        public async Task Test1_Admin_PuedeListarSolicitudes()
        {
            using var context = GetInMemoryDbContext();
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 1,
                MascotaId = 10,
                AdoptanteId = 2,
                PetName = "Milo",
                Email = "user@adoptme.com",
                Status = "Pendiente",
                FechaSolicitud = DateTime.UtcNow
            });
            await context.SaveChangesAsync();

            var controller = CreateController(context, role: "Admin");
            var result = await controller.ObtenerTodasAdmin();

            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var list = Assert.IsAssignableFrom<IEnumerable<SolicitudResponseDto>>(okResult.Value);
            Assert.Single(list);
        }

        [Fact]
        public void Test2_EndpointAdmin_TieneAtributoAuthorizeConRolAdmin()
        {
            var method = typeof(SolicitudesController).GetMethod(nameof(SolicitudesController.ObtenerTodasAdmin));
            Assert.NotNull(method);

            var authAttr = (Microsoft.AspNetCore.Authorization.AuthorizeAttribute?)Attribute.GetCustomAttribute(method, typeof(Microsoft.AspNetCore.Authorization.AuthorizeAttribute));
            Assert.NotNull(authAttr);
            Assert.Equal("Admin", authAttr.Roles);
        }

        [Fact]
        public void Test3_EndpointActualizarEstado_TieneAtributoAuthorizeConRolAdmin()
        {
            var method = typeof(SolicitudesController).GetMethod(nameof(SolicitudesController.ActualizarEstado));
            Assert.NotNull(method);

            var authAttr = (Microsoft.AspNetCore.Authorization.AuthorizeAttribute?)Attribute.GetCustomAttribute(method, typeof(Microsoft.AspNetCore.Authorization.AuthorizeAttribute));
            Assert.NotNull(authAttr);
            Assert.Equal("Admin", authAttr.Roles);
        }

        [Fact]
        public async Task Test4_Admin_PuedePasarSolicitudAEnRevision()
        {
            using var context = GetInMemoryDbContext();
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 1,
                MascotaId = 10,
                AdoptanteId = 2,
                PetName = "Milo",
                Email = "user@adoptme.com",
                Status = "Pendiente"
            });
            await context.SaveChangesAsync();

            var controller = CreateController(context, role: "Admin");
            var dto = new ActualizarEstadoSolicitudDto { Status = "En revision", Observacion = "Evaluando documentos" };
            var result = await controller.ActualizarEstado(1, dto);

            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var response = Assert.IsType<SolicitudResponseDto>(okResult.Value);
            Assert.Equal("En revisión", response.Status);
            Assert.Equal("Evaluando documentos", response.Observacion);

            var inDb = await context.Solicitudes.FindAsync(1);
            Assert.Equal("En revisión", inDb?.Status);
            Assert.NotNull(inDb?.FechaActualizacion);
        }

        [Fact]
        public async Task Test5_Admin_PuedeRechazarSolicitud()
        {
            using var context = GetInMemoryDbContext();
            context.Solicitudes.Add(new SolicitudAdopcion
            {
                Id = 1,
                MascotaId = 10,
                AdoptanteId = 2,
                PetName = "Milo",
                Email = "user@adoptme.com",
                Status = "Pendiente"
            });
            await context.SaveChangesAsync();

            var controller = CreateController(context, role: "Admin");
            var dto = new ActualizarEstadoSolicitudDto { Status = "Rechazada", Observacion = "Vivienda no apta" };
            var result = await controller.ActualizarEstado(1, dto);

            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var response = Assert.IsType<SolicitudResponseDto>(okResult.Value);
            Assert.Equal("Rechazada", response.Status);

            var inDb = await context.Solicitudes.FindAsync(1);
            Assert.Equal("Rechazada", inDb?.Status);
        }

        [Fact]
        public async Task Test6_AprobarSolicitud_CambiaMascotaDeDisponibleAEnProceso()
        {
            using var context = GetInMemoryDbContext();
            context.Mascotas.Add(new Mascota { Id = 10, Name = "Milo", Species = "Perro", Status = "Disponible" });
            context.Solicitudes.Add(new SolicitudAdopcion { Id = 1, MascotaId = 10, AdoptanteId = 2, PetName = "Milo", Email = "user@adoptme.com", Status = "Pendiente" });
            await context.SaveChangesAsync();

            var controller = CreateController(context, role: "Admin");
            var dto = new ActualizarEstadoSolicitudDto { Status = "Aprobada", Observacion = "Todo correcto" };
            var result = await controller.ActualizarEstado(1, dto);

            var okResult = Assert.IsType<OkObjectResult>(result.Result);
            var response = Assert.IsType<SolicitudResponseDto>(okResult.Value);
            Assert.Equal("Aprobada", response.Status);

            var mascotaInDb = await context.Mascotas.FindAsync(10);
            Assert.Equal("En proceso", mascotaInDb?.Status);
        }

        [Fact]
        public async Task Test7_AprobarSolicitud_RechazaOtrasSolicitudesActivasDeMismaMascota()
        {
            using var context = GetInMemoryDbContext();
            context.Mascotas.Add(new Mascota { Id = 10, Name = "Milo", Species = "Perro", Status = "Disponible" });
            context.Solicitudes.Add(new SolicitudAdopcion { Id = 1, MascotaId = 10, AdoptanteId = 2, PetName = "Milo", Email = "user1@adoptme.com", Status = "Pendiente" });
            context.Solicitudes.Add(new SolicitudAdopcion { Id = 2, MascotaId = 10, AdoptanteId = 3, PetName = "Milo", Email = "user2@adoptme.com", Status = "Pendiente" });
            context.Solicitudes.Add(new SolicitudAdopcion { Id = 3, MascotaId = 10, AdoptanteId = 4, PetName = "Milo", Email = "user3@adoptme.com", Status = "En revisión" });
            await context.SaveChangesAsync();

            var controller = CreateController(context, role: "Admin");
            var dto = new ActualizarEstadoSolicitudDto { Status = "Aprobada" };
            var result = await controller.ActualizarEstado(1, dto);

            Assert.IsType<OkObjectResult>(result.Result);

            var sol1 = await context.Solicitudes.FindAsync(1);
            var sol2 = await context.Solicitudes.FindAsync(2);
            var sol3 = await context.Solicitudes.FindAsync(3);

            Assert.Equal("Aprobada", sol1?.Status);
            Assert.Equal("Rechazada", sol2?.Status);
            Assert.Equal("Rechazada", sol3?.Status);
            Assert.Contains("Rechazada automáticamente", sol2?.Observacion);
            Assert.Contains("Rechazada automáticamente", sol3?.Observacion);
        }

        [Fact]
        public async Task Test8_IdInexistente_DevuelveHttp404()
        {
            using var context = GetInMemoryDbContext();
            var controller = CreateController(context, role: "Admin");
            var dto = new ActualizarEstadoSolicitudDto { Status = "En revisión" };

            var result = await controller.ActualizarEstado(999, dto);
            Assert.IsType<NotFoundObjectResult>(result.Result);
        }

        [Fact]
        public async Task Test9_EstadoNoAdmitido_DevuelveHttp400()
        {
            using var context = GetInMemoryDbContext();
            context.Solicitudes.Add(new SolicitudAdopcion { Id = 1, MascotaId = 10, AdoptanteId = 2, PetName = "Milo", Email = "user@adoptme.com", Status = "Pendiente" });
            await context.SaveChangesAsync();

            var controller = CreateController(context, role: "Admin");
            var dto = new ActualizarEstadoSolicitudDto { Status = "EstadoInvalido" };

            var result = await controller.ActualizarEstado(1, dto);
            Assert.IsType<BadRequestObjectResult>(result.Result);
        }

        [Fact]
        public async Task Test10_MascotaNoDisponible_FallaAprobacionSinCambios()
        {
            using var context = GetInMemoryDbContext();
            context.Mascotas.Add(new Mascota { Id = 10, Name = "Milo", Species = "Perro", Status = "En proceso" });
            context.Solicitudes.Add(new SolicitudAdopcion { Id = 1, MascotaId = 10, AdoptanteId = 2, PetName = "Milo", Email = "user@adoptme.com", Status = "Pendiente" });
            await context.SaveChangesAsync();

            var controller = CreateController(context, role: "Admin");
            var dto = new ActualizarEstadoSolicitudDto { Status = "Aprobada" };

            var result = await controller.ActualizarEstado(1, dto);
            var badRequest = Assert.IsType<BadRequestObjectResult>(result.Result);

            var sol = await context.Solicitudes.FindAsync(1);
            Assert.Equal("Pendiente", sol?.Status);

            var mascota = await context.Mascotas.FindAsync(10);
            Assert.Equal("En proceso", mascota?.Status);
        }
    }
}

