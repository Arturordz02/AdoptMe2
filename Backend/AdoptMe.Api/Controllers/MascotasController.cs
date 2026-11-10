using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using AdoptMe.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using System.Security.Claims;

namespace AdoptMe.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class MascotasController : ControllerBase
    {
        private readonly AdoptMeDbContext _context;
        private readonly IImageStorageService _imageStorageService;
        private readonly ICentroAuthorizationService _authService;
        private readonly ILogger<MascotasController> _logger;

        [ActivatorUtilitiesConstructor]
        public MascotasController(
            AdoptMeDbContext context,
            IImageStorageService imageStorageService,
            ICentroAuthorizationService authService,
            ILogger<MascotasController> logger)
        {
            _context = context;
            _imageStorageService = imageStorageService;
            _authService = authService;
            _logger = logger;
        }

        public MascotasController(
            AdoptMeDbContext context,
            IImageStorageService imageStorageService,
            ILogger<MascotasController> logger)
            : this(context, imageStorageService, new CentroAuthorizationService(context), logger)
        {
        }

        /// <summary>
        /// Constructor de conveniencia y compatibilidad para pruebas unitarias existentes.
        /// </summary>
        public MascotasController(AdoptMeDbContext context)
            : this(
                context,
                new CloudinaryImageStorageService(
                    Microsoft.Extensions.Options.Options.Create(new Configuration.CloudinarySettings()),
                    Microsoft.Extensions.Logging.Abstractions.NullLogger<CloudinaryImageStorageService>.Instance
                ),
                new CentroAuthorizationService(context),
                Microsoft.Extensions.Logging.Abstractions.NullLogger<MascotasController>.Instance)
        {
        }

        private static string? ValidarCoordenadas(double? latitud, double? longitud)
        {
            if (latitud.HasValue || longitud.HasValue)
            {
                if (!latitud.HasValue || !longitud.HasValue)
                {
                    return "Si se especifican coordenadas, tanto la latitud como la longitud deben estar presentes.";
                }

                if (latitud.Value < -90.0 || latitud.Value > 90.0)
                {
                    return "La latitud debe estar comprendida entre -90 y 90 grados.";
                }

                if (longitud.Value < -180.0 || longitud.Value > 180.0)
                {
                    return "La longitud debe estar comprendida entre -180 y 180 grados.";
                }
            }

            return null;
        }

        // GET: api/Mascotas
        // Obtener todas las mascotas
        [HttpGet]
        public async Task<ActionResult<IEnumerable<Mascota>>> ObtenerMascotas()
        {
            return await _context.Mascotas
                .AsNoTracking()
                .ToListAsync();
        }

        // GET: api/Mascotas/1
        // Obtener mascota por ID
        [HttpGet("{id:int}")]
        public async Task<ActionResult<Mascota>> ObtenerPorId(int id)
        {
            var mascota = await _context.Mascotas
                .AsNoTracking()
                .FirstOrDefaultAsync(m => m.Id == id);

            if (mascota == null)
            {
                return NotFound(new
                {
                    mensaje = "Mascota no encontrada."
                });
            }

            return mascota;
        }

        // POST: api/Mascotas
        // Publicar nueva mascota mediante JSON tradicional (compatibilidad hacia atrás)
        [Authorize]
        [HttpPost]
        public async Task<ActionResult<Mascota>> CrearMascota(
            [FromBody] Mascota mascota)
        {
            var correoAutenticado = User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value;
            var rolAutenticado = User.FindFirst(ClaimTypes.Role)?.Value
                ?? User.FindFirst("role")?.Value;

            if (string.IsNullOrWhiteSpace(correoAutenticado))
            {
                return Unauthorized(new
                {
                    mensaje = "No se pudo identificar al usuario autenticado a partir del token."
                });
            }

            if (string.IsNullOrWhiteSpace(mascota.Name) || string.IsNullOrWhiteSpace(mascota.Species))
            {
                return BadRequest(new
                {
                    mensaje = "El nombre y la especie de la mascota son obligatorios."
                });
            }

            var errorCoordenadas = ValidarCoordenadas(mascota.Latitud, mascota.Longitud);
            if (errorCoordenadas != null)
            {
                return BadRequest(new { mensaje = errorCoordenadas });
            }

            // GESTIÓN Y VALIDACIÓN DE CENTROID POR ROL
            int? centroIdAsignado = null;
            if (string.Equals(rolAutenticado, "Centro", StringComparison.OrdinalIgnoreCase))
            {
                var authOperacion = await _authService.ValidarOperacionCentroAsync(User);
                if (!authOperacion.EsValido)
                {
                    return StatusCode(authOperacion.CodigoError ?? 403, new
                    {
                        mensaje = authOperacion.MensajeError ?? "Tu centro no está autorizado para publicar mascotas."
                    });
                }
                centroIdAsignado = authOperacion.Centro?.Id;
            }
            else if (string.Equals(rolAutenticado, "Admin", StringComparison.OrdinalIgnoreCase) ||
                     string.Equals(rolAutenticado, "Administrador", StringComparison.OrdinalIgnoreCase))
            {
                if (mascota.CentroId.HasValue)
                {
                    bool existeCentro = await _context.Centros.AnyAsync(c => c.Id == mascota.CentroId.Value);
                    if (existeCentro)
                    {
                        centroIdAsignado = mascota.CentroId.Value;
                    }
                }
            }
            else
            {
                // Adoptante: sanitización obligatoria hacia null
                centroIdAsignado = null;
            }

            mascota.Id = 0;
            mascota.Name = mascota.Name.Trim();
            mascota.Species = mascota.Species.Trim();
            mascota.Breed = mascota.Breed?.Trim() ?? string.Empty;
            mascota.Age = mascota.Age?.Trim() ?? string.Empty;
            mascota.Gender = mascota.Gender?.Trim() ?? string.Empty;
            mascota.Location = string.IsNullOrWhiteSpace(mascota.Location) ? "Lima" : mascota.Location.Trim();
            mascota.Status = string.IsNullOrWhiteSpace(mascota.Status) ? "Disponible" : mascota.Status.Trim();
            mascota.Description = mascota.Description?.Trim() ?? string.Empty;
            mascota.FechaCreacion = DateTime.UtcNow;
            mascota.CreadoPorCorreo = correoAutenticado.Trim().ToLowerInvariant();
            mascota.ImagePublicId = null; // Sanitización obligatoria: prevenir inyección arbitraria de identificadores desde JSON
            mascota.CentroId = centroIdAsignado;

            _context.Mascotas.Add(mascota);
            await _context.SaveChangesAsync();

            return CreatedAtAction(
                nameof(ObtenerPorId),
                new { id = mascota.Id },
                mascota
            );
        }

        // POST: api/Mascotas/con-imagen
        // Publicar nueva mascota mediante multipart/form-data con imagen opcional
        [Authorize]
        [HttpPost("con-imagen")]
        [Consumes("multipart/form-data")]
        public async Task<ActionResult<Mascota>> CrearMascotaConImagen(
            [FromForm] CrearMascotaMultipartDto dto,
            CancellationToken cancellationToken = default)
        {
            var correoAutenticado = User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value;
            var rolAutenticado = User.FindFirst(ClaimTypes.Role)?.Value
                ?? User.FindFirst("role")?.Value;

            if (string.IsNullOrWhiteSpace(correoAutenticado))
            {
                return Unauthorized(new
                {
                    mensaje = "No se pudo identificar al usuario autenticado a partir del token."
                });
            }

            if (string.IsNullOrWhiteSpace(dto.Name) || string.IsNullOrWhiteSpace(dto.Species))
            {
                return BadRequest(new
                {
                    mensaje = "El nombre y la especie de la mascota son obligatorios."
                });
            }

            var errorCoordenadas = ValidarCoordenadas(dto.Latitud, dto.Longitud);
            if (errorCoordenadas != null)
            {
                return BadRequest(new { mensaje = errorCoordenadas });
            }

            // GESTIÓN Y VALIDACIÓN PREVIA DE CENTROID POR ROL (antes de subir imágenes)
            int? centroIdAsignado = null;
            if (string.Equals(rolAutenticado, "Centro", StringComparison.OrdinalIgnoreCase))
            {
                var authOperacion = await _authService.ValidarOperacionCentroAsync(User, cancellationToken);
                if (!authOperacion.EsValido)
                {
                    return StatusCode(authOperacion.CodigoError ?? 403, new
                    {
                        mensaje = authOperacion.MensajeError ?? "Tu centro no está autorizado para publicar mascotas."
                    });
                }
                centroIdAsignado = authOperacion.Centro?.Id;
            }
            else if (string.Equals(rolAutenticado, "Admin", StringComparison.OrdinalIgnoreCase) ||
                     string.Equals(rolAutenticado, "Administrador", StringComparison.OrdinalIgnoreCase))
            {
                if (dto.CentroId.HasValue)
                {
                    bool existeCentro = await _context.Centros.AnyAsync(c => c.Id == dto.CentroId.Value, cancellationToken);
                    if (existeCentro)
                    {
                        centroIdAsignado = dto.CentroId.Value;
                    }
                }
            }
            else
            {
                // Adoptante: sanitización obligatoria hacia null
                centroIdAsignado = null;
            }

            ImageUploadResult? uploadResult = null;

            if (dto.Imagen != null && dto.Imagen.Length > 0)
            {
                var validacion = await FileSecurityValidator.ValidarImagenAsync(dto.Imagen, cancellationToken);
                if (!validacion.EsValido)
                {
                    return BadRequest(new
                    {
                        mensaje = validacion.MensajeError
                    });
                }

                if (!_imageStorageService.IsConfigured)
                {
                    return StatusCode(StatusCodes.Status503ServiceUnavailable, new
                    {
                        mensaje = "El servicio de almacenamiento de imágenes en la nube (Cloudinary) no está configurado en el servidor."
                    });
                }

                using var stream = dto.Imagen.OpenReadStream();
                uploadResult = await _imageStorageService.SubirImagenAsync(stream, validacion.NombreGenerado!, cancellationToken);

                if (!uploadResult.Exitoso)
                {
                    return StatusCode(StatusCodes.Status502BadGateway, new
                    {
                        mensaje = uploadResult.Error ?? "Error al almacenar la imagen en el servicio en la nube."
                    });
                }
            }

            var mascota = new Mascota
            {
                Id = 0,
                Name = dto.Name.Trim(),
                Species = dto.Species.Trim(),
                Breed = dto.Breed?.Trim() ?? string.Empty,
                Age = dto.Age?.Trim() ?? string.Empty,
                Gender = dto.ObtenerGenero(),
                Location = string.IsNullOrWhiteSpace(dto.Location) ? "Lima" : dto.Location.Trim(),
                Status = string.IsNullOrWhiteSpace(dto.Status) ? "Disponible" : dto.Status.Trim(),
                Description = dto.Description?.Trim() ?? string.Empty,
                ImageUrl = uploadResult?.SecureUrl,
                ImagePublicId = uploadResult?.PublicId,
                CreadoPorCorreo = correoAutenticado.Trim().ToLowerInvariant(),
                Latitud = dto.Latitud,
                Longitud = dto.Longitud,
                CentroId = centroIdAsignado,
                FechaCreacion = DateTime.UtcNow
            };

            try
            {
                _context.Mascotas.Add(mascota);
                await _context.SaveChangesAsync(cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error al persistir la mascota en la base de datos.");

                // ROLLBACK COMPENSATORIO: Si se subió imagen a Cloudinary pero falló la persistencia transaccional en Neon,
                // realizamos limpieza remota best-effort en Cloudinary para evitar recursos huérfanos.
                if (uploadResult != null && !string.IsNullOrWhiteSpace(uploadResult.PublicId))
                {
                    _logger.LogInformation("Revirtiendo imagen huérfana en Cloudinary: {PublicId}", uploadResult.PublicId);
                    try
                    {
                        await _imageStorageService.EliminarImagenAsync(uploadResult.PublicId, CancellationToken.None);
                    }
                    catch (Exception rollbackEx)
                    {
                        _logger.LogError(rollbackEx, "Fallo al revertir la imagen huérfana {PublicId} en Cloudinary.", uploadResult.PublicId);
                    }
                }

                return StatusCode(StatusCodes.Status500InternalServerError, new
                {
                    mensaje = "Error al persistir la mascota en la base de datos. Cualquier imagen procesada fue revertida para evitar archivos huérfanos."
                });
            }

            return CreatedAtAction(
                nameof(ObtenerPorId),
                new { id = mascota.Id },
                mascota
            );
        }

        // PUT: api/Mascotas/1
        // Editar publicación o cambiar estado
        [Authorize]
        [HttpPut("{id:int}")]
        public async Task<IActionResult> ActualizarMascota(
            int id,
            [FromBody] Mascota datos)
        {
            var correoAutenticado = User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value;
            var rolAutenticado = User.FindFirst(ClaimTypes.Role)?.Value
                ?? User.FindFirst("role")?.Value;

            if (string.IsNullOrWhiteSpace(correoAutenticado))
            {
                return Unauthorized(new
                {
                    mensaje = "No se pudo identificar al usuario autenticado a partir del token."
                });
            }

            var mascota = await _context.Mascotas.FindAsync(id);

            if (mascota == null)
            {
                return NotFound(new
                {
                    mensaje = "Mascota no encontrada."
                });
            }

            bool esPropietario = string.Equals(
                mascota.CreadoPorCorreo,
                correoAutenticado.Trim(),
                StringComparison.OrdinalIgnoreCase
            );

            bool esAdmin = string.Equals(
                rolAutenticado,
                "Admin",
                StringComparison.OrdinalIgnoreCase
            ) || string.Equals(
                rolAutenticado,
                "Administrador",
                StringComparison.OrdinalIgnoreCase
            );

            if (!esPropietario && !esAdmin)
            {
                return StatusCode(403, new
                {
                    mensaje = "No tienes autorización para modificar esta mascota. Solo el propietario o un Administrador pueden hacerlo."
                });
            }

            if (!string.IsNullOrWhiteSpace(datos.Name))
            {
                mascota.Name = datos.Name.Trim();
            }
            if (!string.IsNullOrWhiteSpace(datos.Species))
            {
                mascota.Species = datos.Species.Trim();
            }
            if (datos.Breed != null)
            {
                mascota.Breed = datos.Breed.Trim();
            }
            if (datos.Age != null)
            {
                mascota.Age = datos.Age.Trim();
            }
            if (datos.Gender != null)
            {
                mascota.Gender = datos.Gender.Trim();
            }
            if (!string.IsNullOrWhiteSpace(datos.Location))
            {
                mascota.Location = datos.Location.Trim();
            }
            if (!string.IsNullOrWhiteSpace(datos.Status))
            {
                mascota.Status = datos.Status.Trim();
            }
            if (datos.Description != null)
            {
                mascota.Description = datos.Description.Trim();
            }
            if (datos.ImageUrl != null)
            {
                mascota.ImageUrl = datos.ImageUrl;
            }

            // Actualización de coordenadas geográficas
            if (datos.Latitud.HasValue || datos.Longitud.HasValue)
            {
                var errorCoordenadas = ValidarCoordenadas(datos.Latitud, datos.Longitud);
                if (errorCoordenadas != null)
                {
                    return BadRequest(new { mensaje = errorCoordenadas });
                }

                mascota.Latitud = datos.Latitud;
                mascota.Longitud = datos.Longitud;
            }

            // Preservar CentroId original (no editable arbitrariamente)

            try
            {
                await _context.SaveChangesAsync();
            }
            catch (DbUpdateConcurrencyException)
            {
                if (!MascotaExists(id))
                {
                    return NotFound(new
                    {
                        mensaje = "Mascota no encontrada durante la actualización."
                    });
                }
                throw;
            }

            return NoContent();
        }

        // PUT: api/Mascotas/1/imagen
        // Reemplazar o actualizar la fotografía de una mascota existente con limpieza de activo anterior y rollback defensivo
        [Authorize]
        [HttpPut("{id:int}/imagen")]
        [Consumes("multipart/form-data")]
        public async Task<ActionResult<Mascota>> ActualizarImagenMascota(
            int id,
            IFormFile? imagen,
            CancellationToken cancellationToken = default)
        {
            var correoAutenticado = User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value;
            var rolAutenticado = User.FindFirst(ClaimTypes.Role)?.Value
                ?? User.FindFirst("role")?.Value;

            if (string.IsNullOrWhiteSpace(correoAutenticado))
            {
                return Unauthorized(new
                {
                    mensaje = "No se pudo identificar al usuario autenticado a partir del token."
                });
            }

            var mascota = await _context.Mascotas.FindAsync(new object[] { id }, cancellationToken);

            if (mascota == null)
            {
                return NotFound(new
                {
                    mensaje = "Mascota no encontrada."
                });
            }

            bool esPropietario = string.Equals(
                mascota.CreadoPorCorreo,
                correoAutenticado.Trim(),
                StringComparison.OrdinalIgnoreCase
            );

            bool esAdmin = string.Equals(
                rolAutenticado,
                "Admin",
                StringComparison.OrdinalIgnoreCase
            ) || string.Equals(
                rolAutenticado,
                "Administrador",
                StringComparison.OrdinalIgnoreCase
            );

            if (!esPropietario && !esAdmin)
            {
                return StatusCode(403, new
                {
                    mensaje = "No tienes autorización para modificar la imagen de esta mascota. Solo el propietario o un Administrador pueden hacerlo."
                });
            }

            if (imagen == null || imagen.Length == 0)
            {
                return BadRequest(new
                {
                    mensaje = "Debe proporcionar un archivo de imagen válido para actualizar la fotografía."
                });
            }

            var validacion = await FileSecurityValidator.ValidarImagenAsync(imagen, cancellationToken);
            if (!validacion.EsValido)
            {
                return BadRequest(new
                {
                    mensaje = validacion.MensajeError
                });
            }

            if (!_imageStorageService.IsConfigured)
            {
                return StatusCode(StatusCodes.Status503ServiceUnavailable, new
                {
                    mensaje = "El servicio de almacenamiento de imágenes en la nube (Cloudinary) no está configurado en el servidor."
                });
            }

            using var stream = imagen.OpenReadStream();
            var uploadResult = await _imageStorageService.SubirImagenAsync(stream, validacion.NombreGenerado!, cancellationToken);

            if (!uploadResult.Exitoso)
            {
                return StatusCode(StatusCodes.Status502BadGateway, new
                {
                    mensaje = uploadResult.Error ?? "Error al almacenar la nueva imagen en el servicio en la nube."
                });
            }

            var viejoPublicId = mascota.ImagePublicId;

            mascota.ImageUrl = uploadResult.SecureUrl;
            mascota.ImagePublicId = uploadResult.PublicId;

            try
            {
                await _context.SaveChangesAsync(cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error al persistir la nueva imagen de la mascota {Id} en la base de datos.", id);

                // ROLLBACK COMPENSATORIO: Si se subió la nueva imagen a Cloudinary pero falló la persistencia transaccional en Neon,
                // realizamos limpieza remota best-effort en Cloudinary para evitar recursos huérfanos.
                if (!string.IsNullOrWhiteSpace(uploadResult.PublicId))
                {
                    _logger.LogInformation("Revirtiendo nueva imagen huérfana en Cloudinary: {PublicId}", uploadResult.PublicId);
                    try
                    {
                        await _imageStorageService.EliminarImagenAsync(uploadResult.PublicId, CancellationToken.None);
                    }
                    catch (Exception rollbackEx)
                    {
                        _logger.LogError(rollbackEx, "Fallo al revertir la imagen huérfana {PublicId} en Cloudinary.", uploadResult.PublicId);
                    }
                }

                return StatusCode(StatusCodes.Status500InternalServerError, new
                {
                    mensaje = "Error al persistir la imagen en la base de datos. Cualquier imagen procesada fue revertida."
                });
            }

            // LIMPIEZA REMOTA BEST-EFFORT: Si la nueva imagen se guardó con éxito en Neon y existía una imagen anterior,
            // eliminamos el activo anterior de Cloudinary.
            if (!string.IsNullOrWhiteSpace(viejoPublicId) && viejoPublicId != uploadResult.PublicId)
            {
                try
                {
                    _logger.LogInformation("Eliminando imagen reemplazada de Cloudinary: {PublicId}", viejoPublicId);
                    await _imageStorageService.EliminarImagenAsync(viejoPublicId, CancellationToken.None);
                }
                catch (Exception cleanupEx)
                {
                    _logger.LogWarning(cleanupEx, "No se pudo eliminar la imagen anterior '{PublicId}' en Cloudinary tras el reemplazo exitoso.", viejoPublicId);
                }
            }

            return Ok(mascota);
        }

        // DELETE: api/Mascotas/1
        // Eliminar publicación
        [Authorize]
        [HttpDelete("{id:int}")]
        public async Task<IActionResult> EliminarMascota(int id)
        {
            var correoAutenticado = User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value;
            var rolAutenticado = User.FindFirst(ClaimTypes.Role)?.Value
                ?? User.FindFirst("role")?.Value;

            if (string.IsNullOrWhiteSpace(correoAutenticado))
            {
                return Unauthorized(new
                {
                    mensaje = "No se pudo identificar al usuario autenticado a partir del token."
                });
            }

            var mascota = await _context.Mascotas.FindAsync(id);

            if (mascota == null)
            {
                return NotFound(new
                {
                    mensaje = "Mascota no encontrada."
                });
            }

            bool esPropietario = string.Equals(
                mascota.CreadoPorCorreo,
                correoAutenticado.Trim(),
                StringComparison.OrdinalIgnoreCase
            );

            bool esAdmin = string.Equals(
                rolAutenticado,
                "Admin",
                StringComparison.OrdinalIgnoreCase
            ) || string.Equals(
                rolAutenticado,
                "Administrador",
                StringComparison.OrdinalIgnoreCase
            );

            if (!esPropietario && !esAdmin)
            {
                return StatusCode(403, new
                {
                    mensaje = "No tienes autorización para eliminar esta mascota. Solo el propietario o un Administrador pueden hacerlo."
                });
            }

            // 1. Conservar el identificador público en memoria antes de eliminar el registro
            var publicIdParaEliminar = mascota.ImagePublicId;

            // 2. Persistencia transaccional primaria en Neon: eliminar y confirmar en base de datos primero.
            // Si la base de datos falla, la transacción se aborta y la imagen en Cloudinary permanece intacta.
            _context.Mascotas.Remove(mascota);
            await _context.SaveChangesAsync();

            // 3. Limpieza remota best-effort en Cloudinary tras confirmar el borrado en base de datos.
            // Si Cloudinary falla temporalmente, no afecta al usuario y se registra una advertencia compensatoria sin secretos.
            if (!string.IsNullOrWhiteSpace(publicIdParaEliminar))
            {
                try
                {
                    await _imageStorageService.EliminarImagenAsync(publicIdParaEliminar);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Mascota {Id} eliminada en Neon, pero la limpieza remota de la imagen '{PublicId}' en Cloudinary falló (consistencia compensatoria).", id, publicIdParaEliminar);
                }
            }

            return NoContent();
        }

        private bool MascotaExists(int id)
        {
            return _context.Mascotas.Any(e => e.Id == id);
        }
    }
}
