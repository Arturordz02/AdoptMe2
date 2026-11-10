using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Claims;
using System.Text;
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
    public class FakeImageStorageService : IImageStorageService
    {
        public bool IsConfigured { get; set; } = true;
        public bool DeberiaFallarSubida { get; set; } = false;
        public string? ErrorSubida { get; set; }
        public List<string> ImagenesSubidas { get; } = new();
        public List<string> ImagenesEliminadas { get; } = new();

        public Task<ImageUploadResult> SubirImagenAsync(Stream stream, string nombreArchivo, CancellationToken cancellationToken = default)
        {
            return SubirImagenAsync(stream, nombreArchivo, null, cancellationToken);
        }

        public Task<ImageUploadResult> SubirImagenAsync(Stream stream, string nombreArchivo, string? subcarpeta, CancellationToken cancellationToken = default)
        {
            if (!IsConfigured)
            {
                return Task.FromResult(ImageUploadResult.Fallo("El servicio de almacenamiento de imágenes en la nube (Cloudinary) no está configurado en el servidor."));
            }

            if (DeberiaFallarSubida)
            {
                return Task.FromResult(ImageUploadResult.Fallo(ErrorSubida ?? "Error simulado en Cloudinary."));
            }

            string carpeta = string.IsNullOrWhiteSpace(subcarpeta) ? "adoptme/mascotas" : subcarpeta.Trim();
            string publicId = $"{carpeta}/{Guid.NewGuid():N}";
            string secureUrl = $"https://res.cloudinary.com/adoptme-demo/image/upload/v123456/{publicId}.jpg";
            ImagenesSubidas.Add(publicId);

            return Task.FromResult(ImageUploadResult.Exito(secureUrl, publicId));
        }

        public Task<bool> EliminarImagenAsync(string publicId, CancellationToken cancellationToken = default)
        {
            ImagenesEliminadas.Add(publicId);
            return Task.FromResult(true);
        }
    }

    public class FailingDbContext : AdoptMeDbContext
    {
        public bool FailOnSave { get; set; } = false;
        public FailingDbContext(DbContextOptions<AdoptMeDbContext> options) : base(options) { }
        public override Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
        {
            if (FailOnSave)
            {
                throw new DbUpdateException("Error simulado de persistencia en base de datos.");
            }
            return base.SaveChangesAsync(cancellationToken);
        }
    }

    public class MascotasCloudinaryTests
    {
        private static AdoptMeDbContext GetInMemoryDbContext()
        {
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
                .Options;

            return new AdoptMeDbContext(options);
        }

        private static MascotasController CreateController(
            AdoptMeDbContext context,
            IImageStorageService imageStorageService,
            string? email = "usuario@adoptme.com",
            string? role = "Adoptante")
        {
            var controller = new MascotasController(
                context,
                imageStorageService,
                NullLogger<MascotasController>.Instance);

            var claims = new List<Claim>();
            if (!string.IsNullOrWhiteSpace(email))
            {
                claims.Add(new Claim(ClaimTypes.Email, email));
            }
            if (!string.IsNullOrWhiteSpace(role))
            {
                claims.Add(new Claim(ClaimTypes.Role, role));
            }

            var identity = new ClaimsIdentity(claims, string.IsNullOrWhiteSpace(email) ? null : "TestAuth");
            var principal = new ClaimsPrincipal(identity);

            controller.ControllerContext = new ControllerContext
            {
                HttpContext = new DefaultHttpContext { User = principal }
            };

            return controller;
        }

        private static IFormFile CreateMockFormFile(byte[] header, string fileName, string contentType, int payloadLength = 1024)
        {
            var stream = new MemoryStream();
            stream.Write(header, 0, header.Length);

            if (payloadLength > header.Length)
            {
                byte[] dummy = new byte[payloadLength - header.Length];
                stream.Write(dummy, 0, dummy.Length);
            }

            stream.Position = 0;
            return new FormFile(stream, 0, stream.Length, "Imagen", fileName)
            {
                Headers = new HeaderDictionary(),
                ContentType = contentType
            };
        }

        private static readonly byte[] JpegHeader = new byte[] { 0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01 };
        private static readonly byte[] PngHeader = new byte[] { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D };
        private static readonly byte[] WebpHeader = new byte[] { 0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50 };

        // 1. Publicación autenticada con JPEG válido
        [Fact]
        public async Task Test01_PublicacionAutenticada_ConJpegValido_GuardaMascotaYRetornaUrlSegura()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            var formFile = CreateMockFormFile(JpegHeader, "foto_perrito.jpg", "image/jpeg");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "Rocky",
                Species = "Perro",
                Breed = "Pastor Alemán",
                Age = "2 años",
                Gender = "Macho",
                Location = "Miraflores, Lima",
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var createdResult = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
            var guardada = Assert.IsType<Mascota>(createdResult.Value);
            Assert.NotNull(guardada.ImageUrl);
            Assert.StartsWith("https://res.cloudinary.com/", guardada.ImageUrl);
            Assert.NotNull(guardada.ImagePublicId);
            Assert.Single(fakeStorage.ImagenesSubidas);
        }

        // 2. PNG permitido
        [Fact]
        public async Task Test02_PublicacionAutenticada_ConPngValido_PermitidoYGuardado()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            var formFile = CreateMockFormFile(PngHeader, "foto_gatito.png", "image/png");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "Michi",
                Species = "Gato",
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var createdResult = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
            var guardada = Assert.IsType<Mascota>(createdResult.Value);
            Assert.NotNull(guardada.ImageUrl);
            Assert.NotNull(guardada.ImagePublicId);
        }

        // 3. WebP permitido
        [Fact]
        public async Task Test03_PublicacionAutenticada_ConWebPValido_PermitidoYGuardado()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            var formFile = CreateMockFormFile(WebpHeader, "foto_conejo.webp", "image/webp");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "Tambor",
                Species = "Conejo",
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var createdResult = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
            var guardada = Assert.IsType<Mascota>(createdResult.Value);
            Assert.NotNull(guardada.ImageUrl);
            Assert.NotNull(guardada.ImagePublicId);
        }

        // 4. Archivo mayor a 2 MB rechazado
        [Fact]
        public async Task Test04_ArchivoMayorA2MB_RechazadoConBadRequest()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            // 2.5 MB payload
            int tamanoExcesivo = (int)(2.5 * 1024 * 1024);
            var formFile = CreateMockFormFile(JpegHeader, "pesada.jpg", "image/jpeg", tamanoExcesivo);

            var dto = new CrearMascotaMultipartDto
            {
                Name = "Toby",
                Species = "Perro",
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
            Assert.Contains("supera el tamaño máximo", badRequest.Value?.ToString());
            Assert.Empty(fakeStorage.ImagenesSubidas);
        }

        // 5. MIME inválido rechazado
        [Fact]
        public async Task Test05_MimeInvalido_RechazadoConBadRequest()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            var formFile = CreateMockFormFile(new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x35, 0x0A, 0x25, 0xE2, 0xE3 }, "documento.pdf", "application/pdf");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "MascotaPdf",
                Species = "Perro",
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
            Assert.Contains("Tipo de contenido no admitido", badRequest.Value?.ToString());
            Assert.Empty(fakeStorage.ImagenesSubidas);
        }

        // 6. Archivo vacío rechazado
        [Fact]
        public async Task Test06_ArchivoVacio_RechazadoConBadRequest()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            var stream = new MemoryStream();
            var formFile = new FormFile(stream, 0, 0, "Imagen", "vacio.jpg")
            {
                Headers = new HeaderDictionary(),
                ContentType = "image/jpeg"
            };

            var dto = new CrearMascotaMultipartDto
            {
                Name = "Vacio",
                Species = "Perro",
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            // Archivo de 0 bytes es ignorado o rechazado defensivamente
            Assert.Empty(fakeStorage.ImagenesSubidas);
        }

        // 7. Solicitud anónima rechazada
        [Fact]
        public async Task Test07_SolicitudAnonima_RetornaUnauthorized()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: null, role: null);

            var dto = new CrearMascotaMultipartDto
            {
                Name = "Anon",
                Species = "Perro"
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            Assert.IsType<UnauthorizedObjectResult>(actionResult.Result);
        }

        // 8. Publicación sin imagen compatible
        [Fact]
        public async Task Test08_PublicacionSinImagen_GuardaConImageUrlNull()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            var dto = new CrearMascotaMultipartDto
            {
                Name = "SinFoto",
                Species = "Gato",
                Imagen = null
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var createdResult = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
            var guardada = Assert.IsType<Mascota>(createdResult.Value);
            Assert.Null(guardada.ImageUrl);
            Assert.Null(guardada.ImagePublicId);
            Assert.Empty(fakeStorage.ImagenesSubidas);
        }

        // 9. Coordenadas válidas con imagen
        [Fact]
        public async Task Test09_CoordenadasValidas_ConImagen_GuardaCorrectamente()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            var formFile = CreateMockFormFile(JpegHeader, "mapa.jpg", "image/jpeg");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "GeoMascota",
                Species = "Perro",
                Latitud = -12.046374,
                Longitud = -77.030583,
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var createdResult = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
            var guardada = Assert.IsType<Mascota>(createdResult.Value);
            Assert.Equal(-12.046374, guardada.Latitud);
            Assert.Equal(-77.030583, guardada.Longitud);
            Assert.NotNull(guardada.ImageUrl);
        }

        // 10. Coordenadas inválidas rechazadas antes de subir
        [Fact]
        public async Task Test10_CoordenadasInvalidas_RechazadasAntesDeSubirACloudinary()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            var formFile = CreateMockFormFile(JpegHeader, "mapa.jpg", "image/jpeg");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "Invalido",
                Species = "Perro",
                Latitud = 95.0, // Fuera de rango [-90, 90]
                Longitud = -77.0,
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
            Assert.Contains("La latitud debe estar comprendida entre -90 y 90", badRequest.Value?.ToString());
            // No se debió haber subido nada a Cloudinary
            Assert.Empty(fakeStorage.ImagenesSubidas);
        }

        // 11. Fallo Neon elimina la imagen recién subida (Rollback compensatorio con limpieza remota)
        [Fact]
        public async Task Test11_FalloBaseDeDatos_RevierteImagenEnCloudinary()
        {
            // Creamos un DbContext que fallará al guardar duplicando una clave primaria o disposed
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: "failing_db")
                .Options;

            using var context = new AdoptMeDbContext(options);
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage);

            // Insertamos manualmente una mascota con ID 1
            context.Mascotas.Add(new Mascota { Id = 1, Name = "Existente", Species = "Perro" });
            await context.SaveChangesAsync();

            // Disponer el contexto forzará una excepción ObjectDisposedException / DbUpdateException en SaveChangesAsync
            context.Dispose();

            var formFile = CreateMockFormFile(JpegHeader, "rollback.jpg", "image/jpeg");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "RollbackPet",
                Species = "Perro",
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var statusResult = Assert.IsType<ObjectResult>(actionResult.Result);
            Assert.Equal(StatusCodes.Status500InternalServerError, statusResult.StatusCode);

            // Verificamos que la imagen fue subida y luego revertida inmediatamente
            Assert.Single(fakeStorage.ImagenesSubidas);
            Assert.Single(fakeStorage.ImagenesEliminadas);
            Assert.Equal(fakeStorage.ImagenesSubidas[0], fakeStorage.ImagenesEliminadas[0]);
        }

        // 12. DELETE elimina imagen remota
        [Fact]
        public async Task Test12_Delete_EliminaImagenRemotaSiTienePublicId()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "propietario@adoptme.com");

            var mascota = new Mascota
            {
                Id = 10,
                Name = "Pelusa",
                Species = "Gato",
                CreadoPorCorreo = "propietario@adoptme.com",
                ImageUrl = "https://res.cloudinary.com/demo/image/upload/v1/adoptme/mascotas/pelusa.jpg",
                ImagePublicId = "adoptme/mascotas/pelusa"
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            var result = await controller.EliminarMascota(10);

            Assert.IsType<NoContentResult>(result);
            Assert.Contains("adoptme/mascotas/pelusa", fakeStorage.ImagenesEliminadas);
            Assert.Null(await context.Mascotas.FindAsync(10));
        }

        // 13. DELETE de registro antiguo sin PublicId funciona normalmente
        [Fact]
        public async Task Test13_Delete_RegistroAntiguoSinPublicId_EliminaSinErrores()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "propietario@adoptme.com");

            var mascotaAntigua = new Mascota
            {
                Id = 11,
                Name = "Antiguo",
                Species = "Perro",
                CreadoPorCorreo = "propietario@adoptme.com",
                ImageUrl = null,
                ImagePublicId = null
            };
            context.Mascotas.Add(mascotaAntigua);
            await context.SaveChangesAsync();

            var result = await controller.EliminarMascota(11);

            Assert.IsType<NoContentResult>(result);
            Assert.Empty(fakeStorage.ImagenesEliminadas);
            Assert.Null(await context.Mascotas.FindAsync(11));
        }

        // 14. Usuario no propietario no puede eliminar
        [Fact]
        public async Task Test14_UsuarioNoPropietario_NoPuedeEliminar_Retorna403()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "ajeno@adoptme.com", role: "Adoptante");

            var mascota = new Mascota
            {
                Id = 12,
                Name = "DeOtro",
                Species = "Perro",
                CreadoPorCorreo = "dueno_real@adoptme.com",
                ImagePublicId = "adoptme/mascotas/deotro"
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            var result = await controller.EliminarMascota(12);

            var objectResult = Assert.IsType<ObjectResult>(result);
            Assert.Equal(StatusCodes.Status403Forbidden, objectResult.StatusCode);
            Assert.Empty(fakeStorage.ImagenesEliminadas);
            Assert.NotNull(await context.Mascotas.FindAsync(12));
        }

        // 15. Admin sí puede eliminar según las reglas actuales
        [Fact]
        public async Task Test15_Admin_SiPuedeEliminarCualquierMascotaYRemoverFoto()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "admin@adoptme.com", role: "Admin");

            var mascota = new Mascota
            {
                Id = 13,
                Name = "MascotaAdmin",
                Species = "Perro",
                CreadoPorCorreo = "usuario_comun@adoptme.com",
                ImagePublicId = "adoptme/mascotas/admin_remove"
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            var result = await controller.EliminarMascota(13);

            Assert.IsType<NoContentResult>(result);
            Assert.Contains("adoptme/mascotas/admin_remove", fakeStorage.ImagenesEliminadas);
            Assert.Null(await context.Mascotas.FindAsync(13));
        }

        // 16. Configuración Cloudinary ausente devuelve error controlado
        [Fact]
        public async Task Test16_ConfiguracionCloudinaryAusente_DevuelveErrorControlado503()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService
            {
                IsConfigured = false // Cloudinary no configurado
            };
            var controller = CreateController(context, fakeStorage);

            var formFile = CreateMockFormFile(JpegHeader, "foto.jpg", "image/jpeg");
            var dto = new CrearMascotaMultipartDto
            {
                Name = "SinCloudinary",
                Species = "Perro",
                Imagen = formFile
            };

            var actionResult = await controller.CrearMascotaConImagen(dto);

            var statusResult = Assert.IsType<ObjectResult>(actionResult.Result);
            Assert.Equal(StatusCodes.Status503ServiceUnavailable, statusResult.StatusCode);
            Assert.Contains("no está configurado", statusResult.Value?.ToString());
            Assert.Empty(fakeStorage.ImagenesSubidas);
        }

        // 17. Reemplazo exitoso de imagen elimina la imagen previa en Cloudinary
        [Fact]
        public async Task Test17_ActualizarImagen_ReemplazoExitoso_EliminaFotoPreviaYActualizaEntidad()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "dueno@adoptme.com");

            var mascota = new Mascota
            {
                Id = 20,
                Name = "Firulais",
                Species = "Perro",
                CreadoPorCorreo = "dueno@adoptme.com",
                ImageUrl = "https://res.cloudinary.com/demo/image/upload/v1/adoptme/mascotas/foto_antigua.jpg",
                ImagePublicId = "adoptme/mascotas/foto_antigua"
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            var nuevaFoto = CreateMockFormFile(JpegHeader, "nueva_foto.jpg", "image/jpeg");
            var actionResult = await controller.ActualizarImagenMascota(20, nuevaFoto);

            var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
            var actualizada = Assert.IsType<Mascota>(okResult.Value);

            Assert.NotEqual("adoptme/mascotas/foto_antigua", actualizada.ImagePublicId);
            Assert.Contains("adoptme/mascotas/foto_antigua", fakeStorage.ImagenesEliminadas);
            Assert.Single(fakeStorage.ImagenesSubidas);
        }

        // 18. Usuario no autorizado recibe 403 al intentar actualizar imagen
        [Fact]
        public async Task Test18_ActualizarImagen_UsuarioNoAutorizado_Retorna403()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "intruso@adoptme.com", role: "Adoptante");

            var mascota = new Mascota
            {
                Id = 21,
                Name = "Gatita",
                Species = "Gato",
                CreadoPorCorreo = "dueno_real@adoptme.com",
                ImageUrl = "https://res.cloudinary.com/demo/image/upload/v1/adoptme/mascotas/foto.jpg",
                ImagePublicId = "adoptme/mascotas/foto"
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            var nuevaFoto = CreateMockFormFile(JpegHeader, "nueva_foto.jpg", "image/jpeg");
            var actionResult = await controller.ActualizarImagenMascota(21, nuevaFoto);

            var objectResult = Assert.IsType<ObjectResult>(actionResult.Result);
            Assert.Equal(StatusCodes.Status403Forbidden, objectResult.StatusCode);
            Assert.Empty(fakeStorage.ImagenesSubidas);
            Assert.Empty(fakeStorage.ImagenesEliminadas);
        }

        // 19. Sanitización: CrearMascota tradicional JSON anula ImagePublicId arbitrario
        [Fact]
        public async Task Test19_CrearMascotaJson_SanitizaImagePublicId_NoPermiteInyeccion()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "usuario@adoptme.com");

            var mascotaConPublicIdInyectado = new Mascota
            {
                Name = "Inyectado",
                Species = "Perro",
                ImageUrl = "https://externo.com/foto.jpg",
                ImagePublicId = "inyeccion_maliciosa_public_id"
            };

            var actionResult = await controller.CrearMascota(mascotaConPublicIdInyectado);

            var createdResult = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
            var guardada = Assert.IsType<Mascota>(createdResult.Value);
            Assert.Null(guardada.ImagePublicId);
        }

        // 20. ActualizarImagen con archivo inválido retorna BadRequest
        [Fact]
        public async Task Test20_ActualizarImagen_ArchivoInvalido_RetornaBadRequest()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "dueno@adoptme.com");

            var mascota = new Mascota
            {
                Id = 22,
                Name = "Loki",
                Species = "Perro",
                CreadoPorCorreo = "dueno@adoptme.com"
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            var archivoPdf = CreateMockFormFile(new byte[] { 0x25, 0x50, 0x44, 0x46 }, "doc.pdf", "application/pdf");
            var actionResult = await controller.ActualizarImagenMascota(22, archivoPdf);

            var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
            Assert.Empty(fakeStorage.ImagenesSubidas);
        }

        // 21. Rollback en reemplazo (PUT /imagen): Si falla la base de datos, revierte la nueva imagen y conserva la anterior
        [Fact]
        public async Task Test21_ActualizarImagen_FalloEnBaseDeDatosRevierteNuevaImagenYConservaFotoAntigua()
        {
            var options = new DbContextOptionsBuilder<AdoptMeDbContext>()
                .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
                .Options;

            using var context = new FailingDbContext(options);
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "propietario@adoptme.com");

            var mascota = new Mascota
            {
                Id = 30,
                Name = "Original",
                Species = "Perro",
                CreadoPorCorreo = "propietario@adoptme.com",
                ImageUrl = "https://res.cloudinary.com/demo/image/upload/v1/adoptme/mascotas/foto_original.jpg",
                ImagePublicId = "adoptme/mascotas/foto_original"
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            // Activamos la falla en SaveChangesAsync
            context.FailOnSave = true;

            var formFile = CreateMockFormFile(JpegHeader, "nueva_foto_falla.jpg", "image/jpeg");
            var actionResult = await controller.ActualizarImagenMascota(30, formFile);

            var statusResult = Assert.IsType<ObjectResult>(actionResult.Result);
            Assert.Equal(StatusCodes.Status500InternalServerError, statusResult.StatusCode);

            // Verificamos que la nueva imagen subida fue revertida (eliminada)
            Assert.Single(fakeStorage.ImagenesSubidas);
            var nuevoPublicId = fakeStorage.ImagenesSubidas[0];
            Assert.Contains(nuevoPublicId, fakeStorage.ImagenesEliminadas);

            // La imagen anterior ("adoptme/mascotas/foto_original") NUNCA debió eliminarse
            Assert.DoesNotContain("adoptme/mascotas/foto_original", fakeStorage.ImagenesEliminadas);
        }

        // 22. Consistencia compensatoria en DELETE: Si Cloudinary falla temporalmente, el borrado en Neon se completa
        [Fact]
        public async Task Test22_Delete_FalloEnCloudinaryRegistraWarningYCompletaBorradoEnBaseDeDatos()
        {
            using var context = GetInMemoryDbContext();
            var fakeStorage = new FakeImageStorageService();
            var controller = CreateController(context, fakeStorage, email: "propietario@adoptme.com");

            var mascota = new Mascota
            {
                Id = 31,
                Name = "Resiliente",
                Species = "Gato",
                CreadoPorCorreo = "propietario@adoptme.com",
                ImageUrl = "https://res.cloudinary.com/demo/image/upload/v1/adoptme/mascotas/resiliente.jpg",
                ImagePublicId = "adoptme/mascotas/resiliente"
            };
            context.Mascotas.Add(mascota);
            await context.SaveChangesAsync();

            // Simulamos fallo transitorio en Cloudinary
            fakeStorage.DeberiaFallarSubida = true;

            var result = await controller.EliminarMascota(31);

            // La API no bloquea al usuario y completa la eliminación en BD
            Assert.IsType<NoContentResult>(result);
            Assert.Null(await context.Mascotas.FindAsync(31));
        }
    }
}
