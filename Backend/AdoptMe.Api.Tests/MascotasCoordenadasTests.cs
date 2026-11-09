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
    public class MascotasCoordenadasTests
    {
        private AdoptMeDbContext GetInMemoryDbContext()
        {
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
                .Options;

            return new AdoptMeDbContext(options);
        }

        private MascotasController CreateController(AdoptMeDbContext context, string email = "usuario@adoptme.com", string role = "Adoptante")
        {
            var controller = new MascotasController(context);

            var claims = new List<Claim>
            {
                new Claim(ClaimTypes.Email, email),
                new Claim(ClaimTypes.Role, role)
            };

            var identity = new ClaimsIdentity(claims, "TestAuth");
            var principal = new ClaimsPrincipal(identity);

            controller.ControllerContext = new ControllerContext
            {
                HttpContext = new DefaultHttpContext { User = principal }
            };

            return controller;
        }

        [Fact]
        public async Task CrearMascota_ConCoordenadasValidas_GuardaExitosamente()
        {
            using var context = GetInMemoryDbContext();
            var controller = CreateController(context);

            var nueva = new Mascota
            {
                Name = "Firulais",
                Species = "Perro",
                Breed = "Mestizo",
                Location = "Miraflores, Lima",
                Latitud = -12.1221,
                Longitud = -77.0305
            };

            var actionResult = await controller.CrearMascota(nueva);

            var createdResult = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
            var guardada = Assert.IsType<Mascota>(createdResult.Value);
            Assert.Equal(-12.1221, guardada.Latitud);
            Assert.Equal(-77.0305, guardada.Longitud);
            Assert.True(guardada.Id > 0);
        }

        [Fact]
        public async Task CrearMascota_SinCoordenadas_GuardaComoNull_CompatibilidadRegistrosAntiguos()
        {
            using var context = GetInMemoryDbContext();
            var controller = CreateController(context);

            var nueva = new Mascota
            {
                Name = "Miau",
                Species = "Gato",
                Breed = "Siamés",
                Location = "Surco, Lima",
                Latitud = null,
                Longitud = null
            };

            var actionResult = await controller.CrearMascota(nueva);

            var createdResult = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
            var guardada = Assert.IsType<Mascota>(createdResult.Value);
            Assert.Null(guardada.Latitud);
            Assert.Null(guardada.Longitud);
        }

        [Fact]
        public async Task CrearMascota_SoloConLatitudSinLongitud_RetornaBadRequest()
        {
            using var context = GetInMemoryDbContext();
            var controller = CreateController(context);

            var nueva = new Mascota
            {
                Name = "Incompleto",
                Species = "Perro",
                Latitud = -12.12,
                Longitud = null
            };

            var actionResult = await controller.CrearMascota(nueva);
            Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        }

        [Fact]
        public async Task CrearMascota_SoloConLongitudSinLatitud_RetornaBadRequest()
        {
            using var context = GetInMemoryDbContext();
            var controller = CreateController(context);

            var nueva = new Mascota
            {
                Name = "Incompleto2",
                Species = "Perro",
                Latitud = null,
                Longitud = -77.03
            };

            var actionResult = await controller.CrearMascota(nueva);
            Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        }

        [Theory]
        [InlineData(-91.0, -77.0)]
        [InlineData(91.0, -77.0)]
        public async Task CrearMascota_LatitudFueraDeRango_RetornaBadRequest(double lat, double lon)
        {
            using var context = GetInMemoryDbContext();
            var controller = CreateController(context);

            var nueva = new Mascota
            {
                Name = "FueraRangoLat",
                Species = "Perro",
                Latitud = lat,
                Longitud = lon
            };

            var actionResult = await controller.CrearMascota(nueva);
            Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        }

        [Theory]
        [InlineData(-12.0, -181.0)]
        [InlineData(-12.0, 181.0)]
        public async Task CrearMascota_LongitudFueraDeRango_RetornaBadRequest(double lat, double lon)
        {
            using var context = GetInMemoryDbContext();
            var controller = CreateController(context);

            var nueva = new Mascota
            {
                Name = "FueraRangoLon",
                Species = "Perro",
                Latitud = lat,
                Longitud = lon
            };

            var actionResult = await controller.CrearMascota(nueva);
            Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        }

        [Fact]
        public async Task ActualizarMascota_ConCoordenadasValidas_ActualizaExitosamente()
        {
            using var context = GetInMemoryDbContext();
            var m = new Mascota
            {
                Id = 1,
                Name = "Bobby",
                Species = "Perro",
                CreadoPorCorreo = "usuario@adoptme.com",
                Latitud = null,
                Longitud = null
            };
            context.Mascotas.Add(m);
            await context.SaveChangesAsync();

            var controller = CreateController(context, email: "usuario@adoptme.com");
            var datosActualizados = new Mascota
            {
                Latitud = -12.05,
                Longitud = -77.04
            };

            var resultado = await controller.ActualizarMascota(1, datosActualizados);
            Assert.IsType<NoContentResult>(resultado);

            var mascotaActualizada = await context.Mascotas.FindAsync(1);
            Assert.NotNull(mascotaActualizada);
            Assert.Equal(-12.05, mascotaActualizada.Latitud);
            Assert.Equal(-77.04, mascotaActualizada.Longitud);
        }

        [Fact]
        public async Task ActualizarMascota_CoordenadasIncompletas_RetornaBadRequest()
        {
            using var context = GetInMemoryDbContext();
            var m = new Mascota
            {
                Id = 2,
                Name = "Luna",
                Species = "Gato",
                CreadoPorCorreo = "usuario@adoptme.com"
            };
            context.Mascotas.Add(m);
            await context.SaveChangesAsync();

            var controller = CreateController(context, email: "usuario@adoptme.com");
            var datosIncompletos = new Mascota
            {
                Latitud = -12.05,
                Longitud = null
            };

            var resultado = await controller.ActualizarMascota(2, datosIncompletos);
            Assert.IsType<BadRequestObjectResult>(resultado);
        }
    }
}
