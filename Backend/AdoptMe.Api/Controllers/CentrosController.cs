using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using AdoptMe.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace AdoptMe.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class CentrosController : ControllerBase
    {
        private readonly AdoptMeDbContext _context;
        private readonly ICentroAuthorizationService _authService;
        private readonly IImageStorageService _imageStorageService;
        private readonly ILogger<CentrosController> _logger;

        public CentrosController(
            AdoptMeDbContext context,
            ICentroAuthorizationService authService,
            IImageStorageService imageStorageService,
            ILogger<CentrosController> logger)
        {
            _context = context;
            _authService = authService;
            _imageStorageService = imageStorageService;
            _logger = logger;
        }

        // GET: api/centros
        // Directorio público de centros de adopción aprobados y activos
        [HttpGet]
        public async Task<ActionResult<IEnumerable<CentroPublicoDto>>> ObtenerCentros(
            [FromQuery] string? distrito = null,
            [FromQuery] string? q = null)
        {
            var query = _context.Centros
                .AsNoTracking()
                .Where(c => c.Estado == "Aprobado" && c.Activo);

            if (!string.IsNullOrWhiteSpace(distrito))
            {
                string distNorm = distrito.Trim().ToLower();
                query = query.Where(c => c.Distrito.ToLower().Contains(distNorm));
            }

            if (!string.IsNullOrWhiteSpace(q))
            {
                string qNorm = q.Trim().ToLower();
                query = query.Where(c => c.Nombre.ToLower().Contains(qNorm) || c.Descripcion.ToLower().Contains(qNorm));
            }

            var centros = await query
                .OrderBy(c => c.Nombre)
                .Select(c => new CentroPublicoDto
                {
                    Id = c.Id,
                    Nombre = c.Nombre,
                    Descripcion = c.Descripcion,
                    Direccion = c.Direccion,
                    Distrito = c.Distrito,
                    Telefono = c.Telefono,
                    CorreoContacto = c.CorreoContacto,
                    Horario = c.Horario,
                    Latitud = c.Latitud,
                    Longitud = c.Longitud,
                    LogoUrl = c.LogoUrl,
                    EsDemostrativo = c.EsDemostrativo
                })
                .ToListAsync();

            return Ok(centros);
        }

        // GET: api/centros/5
        // Detalle público de un centro específico (solo si está Aprobado y Activo)
        [HttpGet("{id:int}")]
        public async Task<ActionResult<CentroPublicoDto>> ObtenerPorId(int id)
        {
            var centro = await _context.Centros
                .AsNoTracking()
                .FirstOrDefaultAsync(c => c.Id == id && c.Estado == "Aprobado" && c.Activo);

            if (centro == null)
            {
                return NotFound(new
                {
                    mensaje = "Centro no encontrado o no disponible públicamente."
                });
            }

            return Ok(new CentroPublicoDto
            {
                Id = centro.Id,
                Nombre = centro.Nombre,
                Descripcion = centro.Descripcion,
                Direccion = centro.Direccion,
                Distrito = centro.Distrito,
                Telefono = centro.Telefono,
                CorreoContacto = centro.CorreoContacto,
                Horario = centro.Horario,
                Latitud = centro.Latitud,
                Longitud = centro.Longitud,
                LogoUrl = centro.LogoUrl,
                EsDemostrativo = centro.EsDemostrativo
            });
        }

        // GET: api/centros/mi-centro
        // Perfil y estado institucional accesible exclusivamente por el Centro autenticado
        [Authorize(Roles = "Centro,Admin")]
        [HttpGet("mi-centro")]
        public async Task<ActionResult<CentroPropietarioDto>> ObtenerMiCentro()
        {
            var auth = await _authService.ValidarCentroAutenticadoAsync(User);
            if (!auth.EsValido)
            {
                return StatusCode(auth.CodigoError ?? 401, new { mensaje = auth.MensajeError });
            }

            if (!auth.EsCentro && !auth.EsAdmin)
            {
                return StatusCode(403, new { mensaje = "Acceso restringido a cuentas con rol Centro o Administrador." });
            }

            if (auth.Centro == null)
            {
                return NotFound(new
                {
                    mensaje = "El usuario autenticado no tiene un Centro de Adopción vinculado.",
                    estado = "SinPerfil"
                });
            }

            var c = auth.Centro;
            return Ok(new CentroPropietarioDto
            {
                Id = c.Id,
                UsuarioId = c.UsuarioId,
                Nombre = c.Nombre,
                Descripcion = c.Descripcion,
                Direccion = c.Direccion,
                Distrito = c.Distrito,
                Telefono = c.Telefono,
                CorreoContacto = c.CorreoContacto,
                Horario = c.Horario,
                Latitud = c.Latitud,
                Longitud = c.Longitud,
                LogoUrl = c.LogoUrl,
                Estado = c.Estado,
                MotivoRechazo = c.MotivoRechazo,
                MotivoSuspension = c.MotivoSuspension,
                FechaSuspension = c.FechaSuspension,
                FechaReactivacion = c.FechaReactivacion,
                FechaSolicitud = c.FechaSolicitud,
                FechaRevision = c.FechaRevision,
                EsDemostrativo = c.EsDemostrativo,
                Activo = c.Activo,
                FechaActualizacion = c.FechaActualizacion
            });
        }

        // PUT: api/centros/mi-centro
        // Actualizar datos de contacto y ubicación del propio centro
        [Authorize(Roles = "Centro")]
        [HttpPut("mi-centro")]
        public async Task<ActionResult<CentroPropietarioDto>> ActualizarMiCentro([FromBody] ActualizarMiCentroDto dto)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            // Validar coordenadas por pares
            if ((dto.Latitud.HasValue && !dto.Longitud.HasValue) || (!dto.Latitud.HasValue && dto.Longitud.HasValue))
            {
                return BadRequest(new
                {
                    mensaje = "Las coordenadas geográficas deben proporcionarse como un par completo (Latitud y Longitud) o ser ambas omitidas."
                });
            }

            var auth = await _authService.ValidarCentroAutenticadoAsync(User);
            if (!auth.EsValido)
            {
                return StatusCode(auth.CodigoError ?? 401, new { mensaje = auth.MensajeError });
            }

            if (auth.Centro == null)
            {
                return NotFound(new { mensaje = "El usuario no tiene un centro vinculado para editar." });
            }

            var centro = auth.Centro;

            if (!centro.Activo)
            {
                return StatusCode(403, new
                {
                    mensaje = !string.IsNullOrWhiteSpace(centro.MotivoSuspension)
                        ? $"El centro se encuentra suspendido por administración (Motivo: {centro.MotivoSuspension}). No se permiten modificaciones durante la suspensión."
                        : "El centro se encuentra temporalmente inactivo. No se permiten modificaciones en este estado."
                });
            }

            // EVALUACIÓN DE SENSIBILIDAD DE CAMBIOS:
            // 1. Cambio de nombre institucional
            bool cambioNombre = !string.Equals(centro.Nombre.Trim(), dto.Nombre.Trim(), StringComparison.OrdinalIgnoreCase);

            // 2. Cambio en coordenadas geográficas (> 500 metros o agregada/eliminada)
            bool cambioUbicacionSensible = false;
            if (centro.Latitud.HasValue != dto.Latitud.HasValue || centro.Longitud.HasValue != dto.Longitud.HasValue)
            {
                cambioUbicacionSensible = true;
            }
            else if (centro.Latitud.HasValue && centro.Longitud.HasValue && dto.Latitud.HasValue && dto.Longitud.HasValue)
            {
                double distanciaMetros = _authService.CalcularDistanciaMetros(
                    centro.Latitud.Value, centro.Longitud.Value,
                    dto.Latitud.Value, dto.Longitud.Value
                );
                if (distanciaMetros > 500.0)
                {
                    cambioUbicacionSensible = true;
                }
            }

            // Si el centro ya estaba Aprobado y realiza un cambio sensible, regresa a estado Pendiente
            bool vuelveAPendiente = false;
            if (centro.Estado == "Aprobado" && (cambioNombre || cambioUbicacionSensible))
            {
                centro.Estado = "Pendiente";
                centro.FechaRevision = null;
                centro.RevisadoPorUsuarioId = null;
                centro.MotivoRechazo = null;
                vuelveAPendiente = true;
                _logger.LogInformation("Centro ID {Id} ('{Nombre}') regresó a estado Pendiente por modificación sensible (Nombre: {CN}, Ubicación: {CU}).",
                    centro.Id, centro.Nombre, cambioNombre, cambioUbicacionSensible);
            }

            // Aplicar modificaciones
            centro.Nombre = dto.Nombre.Trim();
            centro.Descripcion = dto.Descripcion.Trim();
            centro.Direccion = dto.Direccion.Trim();
            centro.Distrito = dto.Distrito.Trim();
            centro.Telefono = dto.Telefono.Trim();
            centro.CorreoContacto = dto.CorreoContacto.Trim().ToLowerInvariant();
            centro.Horario = dto.Horario.Trim();
            centro.Latitud = dto.Latitud;
            centro.Longitud = dto.Longitud;
            centro.FechaActualizacion = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            return Ok(new CentroPropietarioDto
            {
                Id = centro.Id,
                UsuarioId = centro.UsuarioId,
                Nombre = centro.Nombre,
                Descripcion = centro.Descripcion,
                Direccion = centro.Direccion,
                Distrito = centro.Distrito,
                Telefono = centro.Telefono,
                CorreoContacto = centro.CorreoContacto,
                Horario = centro.Horario,
                Latitud = centro.Latitud,
                Longitud = centro.Longitud,
                LogoUrl = centro.LogoUrl,
                Estado = centro.Estado,
                MotivoRechazo = vuelveAPendiente ? "La solicitud requiere nueva revisión administrativa debido a cambios sustanciales en nombre o geolocalización." : centro.MotivoRechazo,
                MotivoSuspension = centro.MotivoSuspension,
                FechaSuspension = centro.FechaSuspension,
                FechaReactivacion = centro.FechaReactivacion,
                FechaSolicitud = centro.FechaSolicitud,
                FechaRevision = centro.FechaRevision,
                EsDemostrativo = centro.EsDemostrativo,
                Activo = centro.Activo,
                FechaActualizacion = centro.FechaActualizacion
            });
        }

        // POST: api/centros/mi-centro/reenviar
        // Reenviar solicitud a revisión cuando se encuentra en estado Rechazado
        [Authorize(Roles = "Centro")]
        [HttpPost("mi-centro/reenviar")]
        public async Task<IActionResult> ReenviarSolicitud()
        {
            var auth = await _authService.ValidarCentroAutenticadoAsync(User);
            if (!auth.EsValido)
            {
                return StatusCode(auth.CodigoError ?? 401, new { mensaje = auth.MensajeError });
            }

            if (auth.Centro == null)
            {
                return NotFound(new { mensaje = "El usuario no tiene un centro vinculado." });
            }

            var centro = auth.Centro;

            if (centro.Estado == "Pendiente")
            {
                return Conflict(new { mensaje = "La solicitud ya se encuentra en estado Pendiente de revisión." });
            }

            if (centro.Estado == "Aprobado")
            {
                return Conflict(new { mensaje = "El centro ya se encuentra Aprobado. No requiere reenvío." });
            }

            if (centro.Estado != "Rechazado")
            {
                return BadRequest(new { mensaje = $"No se puede reenviar una solicitud en estado '{centro.Estado}'." });
            }

            centro.Estado = "Pendiente";
            centro.MotivoRechazo = null;
            centro.FechaRevision = null;
            centro.RevisadoPorUsuarioId = null;
            centro.FechaSolicitud = DateTime.UtcNow;
            centro.FechaActualizacion = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            _logger.LogInformation("Centro ID {Id} reenvió su solicitud a revisión administrativa.", centro.Id);

            return Ok(new
            {
                mensaje = "Solicitud reenviada exitosamente a revisión administrativa.",
                estado = centro.Estado,
                fechaSolicitud = centro.FechaSolicitud
            });
        }

        // PUT: api/centros/mi-centro/logo
        // Subir o actualizar el logotipo institucional del centro
        [Authorize(Roles = "Centro")]
        [HttpPut("mi-centro/logo")]
        [Consumes("multipart/form-data")]
        public async Task<IActionResult> ActualizarLogo(
            IFormFile? imagen,
            CancellationToken cancellationToken = default)
        {
            if (imagen == null || imagen.Length == 0)
            {
                return BadRequest(new { mensaje = "Debe proporcionar un archivo de imagen válido para el logotipo." });
            }

            // Validar seguridad de archivo mediante FileSecurityValidator (JPEG, PNG, WebP)
            var validacion = await FileSecurityValidator.ValidarImagenAsync(imagen, cancellationToken);
            if (!validacion.EsValido)
            {
                return BadRequest(new { mensaje = validacion.MensajeError });
            }

            var auth = await _authService.ValidarCentroAutenticadoAsync(User, cancellationToken);
            if (!auth.EsValido)
            {
                return StatusCode(auth.CodigoError ?? 401, new { mensaje = auth.MensajeError });
            }

            if (auth.Centro == null)
            {
                return NotFound(new { mensaje = "El usuario no cuenta con un centro registrado." });
            }

            var centro = auth.Centro;

            if (!centro.Activo)
            {
                return StatusCode(403, new
                {
                    mensaje = !string.IsNullOrWhiteSpace(centro.MotivoSuspension)
                        ? "El centro se encuentra suspendido por administración. No es posible cambiar el logotipo en este estado."
                        : "El centro se encuentra temporalmente inactivo. No se permite cambiar el logotipo."
                });
            }

            if (!_imageStorageService.IsConfigured)
            {
                return StatusCode(503, new { mensaje = "El servicio de almacenamiento de imágenes en la nube (Cloudinary) no está configurado en el servidor." });
            }

            string nombreBase = $"logo_centro_{centro.Id}_{DateTime.UtcNow.Ticks}";
            ImageUploadResult uploadResult;

            using (var stream = imagen.OpenReadStream())
            {
                uploadResult = await _imageStorageService.SubirImagenAsync(stream, nombreBase, "adoptme/centros", cancellationToken);
            }

            if (!uploadResult.Exitoso)
            {
                return StatusCode(500, new { mensaje = uploadResult.Error ?? "Error al almacenar el logotipo en el servicio en la nube." });
            }

            string? viejoPublicId = centro.LogoPublicId;

            centro.LogoUrl = uploadResult.SecureUrl;
            centro.LogoPublicId = uploadResult.PublicId;
            centro.FechaActualizacion = DateTime.UtcNow;

            try
            {
                await _context.SaveChangesAsync(cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error al persistir el logotipo del centro {Id} en Neon.", centro.Id);

                // Rollback compensatorio defensivo en Cloudinary
                if (!string.IsNullOrWhiteSpace(uploadResult.PublicId))
                {
                    try
                    {
                        await _imageStorageService.EliminarImagenAsync(uploadResult.PublicId, CancellationToken.None);
                    }
                    catch (Exception rbEx)
                    {
                        _logger.LogError(rbEx, "Fallo al revertir logotipo huérfano {PublicId} en Cloudinary.", uploadResult.PublicId);
                    }
                }

                return StatusCode(500, new { mensaje = "Error al persistir el logotipo en la base de datos." });
            }

            // Limpieza remota best-effort del logo anterior
            if (!string.IsNullOrWhiteSpace(viejoPublicId) && viejoPublicId != uploadResult.PublicId)
            {
                try
                {
                    await _imageStorageService.EliminarImagenAsync(viejoPublicId, CancellationToken.None);
                }
                catch (Exception cleanupEx)
                {
                    _logger.LogWarning(cleanupEx, "No se pudo eliminar el logotipo anterior '{PublicId}' en Cloudinary tras el reemplazo exitoso.", viejoPublicId);
                }
            }

            return Ok(new
            {
                mensaje = "Logotipo del centro actualizado exitosamente.",
                logoUrl = centro.LogoUrl
            });
        }

        // POST: api/centros/mi-centro/apelaciones
        // Registrar una apelación formal de suspensión operativa
        [Authorize(Roles = "Centro,Admin")]
        [HttpPost("mi-centro/apelaciones")]
        public async Task<ActionResult<CentroApelacionDto>> CrearApelacion(
            [FromBody] CrearApelacionDto dto)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var auth = await _authService.ValidarCentroAutenticadoAsync(User);
            if (!auth.EsValido || auth.Centro == null)
            {
                return StatusCode(auth.CodigoError ?? 401, new { mensaje = auth.MensajeError });
            }

            var centro = auth.Centro;
            bool estaSuspendido = !centro.Activo && !string.IsNullOrWhiteSpace(centro.MotivoSuspension);
            if (!estaSuspendido)
            {
                return BadRequest(new
                {
                    mensaje = "Solo un centro en estado formalmente suspendido puede registrar una apelación."
                });
            }

            bool tieneApelacionPendiente = await _context.CentrosApelaciones
                .AnyAsync(a => a.CentroId == centro.Id && a.Estado == "Pendiente");

            if (tieneApelacionPendiente)
            {
                return Conflict(new
                {
                    mensaje = "El centro ya cuenta con una apelación pendiente en evaluación administrativa."
                });
            }

            string motivoLimpio = dto.MotivoApelacion?.Trim() ?? string.Empty;
            if (motivoLimpio.Length < 10)
            {
                return BadRequest(new
                {
                    mensaje = "El motivo de la apelación debe contener al menos 10 caracteres explicativos."
                });
            }

            var nuevaApelacion = new CentroApelacion
            {
                CentroId = centro.Id,
                MotivoApelacion = motivoLimpio,
                Estado = "Pendiente",
                FechaCreacion = DateTime.UtcNow
            };

            _context.CentrosApelaciones.Add(nuevaApelacion);
            await _context.SaveChangesAsync();

            _logger.LogInformation("Apelación #{ApelacionId} registrada exitosamente para Centro #{CentroId} ('{Nombre}').",
                nuevaApelacion.Id, centro.Id, centro.Nombre);

            return Ok(new CentroApelacionDto
            {
                Id = nuevaApelacion.Id,
                CentroId = centro.Id,
                CentroNombre = centro.Nombre,
                MotivoApelacion = nuevaApelacion.MotivoApelacion,
                Estado = nuevaApelacion.Estado,
                FechaCreacion = nuevaApelacion.FechaCreacion,
                MotivoSuspensionOriginal = centro.MotivoSuspension,
                FechaSuspensionOriginal = centro.FechaSuspension
            });
        }

        // GET: api/centros/mi-centro/apelacion-pendiente
        // Consultar si el centro autenticado tiene una apelación actualmente en revisión
        [Authorize(Roles = "Centro,Admin")]
        [HttpGet("mi-centro/apelacion-pendiente")]
        public async Task<ActionResult<CentroApelacionDto>> ObtenerApelacionPendiente()
        {
            var auth = await _authService.ValidarCentroAutenticadoAsync(User);
            if (!auth.EsValido || auth.Centro == null)
            {
                return StatusCode(auth.CodigoError ?? 401, new { mensaje = auth.MensajeError });
            }

            var apelacion = await _context.CentrosApelaciones
                .AsNoTracking()
                .Include(a => a.Centro)
                .Where(a => a.CentroId == auth.Centro.Id && a.Estado == "Pendiente")
                .OrderByDescending(a => a.FechaCreacion)
                .FirstOrDefaultAsync();

            if (apelacion == null)
            {
                return NotFound(new
                {
                    mensaje = "No existen apelaciones pendientes de revisión para tu centro."
                });
            }

            return Ok(new CentroApelacionDto
            {
                Id = apelacion.Id,
                CentroId = apelacion.CentroId,
                CentroNombre = apelacion.Centro?.Nombre ?? auth.Centro.Nombre,
                MotivoApelacion = apelacion.MotivoApelacion,
                Estado = apelacion.Estado,
                FechaCreacion = apelacion.FechaCreacion,
                FechaRevision = apelacion.FechaRevision,
                RevisadoPorAdminId = apelacion.RevisadoPorAdminId,
                RespuestaAdmin = apelacion.RespuestaAdmin,
                MotivoSuspensionOriginal = auth.Centro.MotivoSuspension,
                FechaSuspensionOriginal = auth.Centro.FechaSuspension
            });
        }

        // GET: api/centros/mi-centro/apelaciones
        // Historial completo de apelaciones del centro autenticado
        [Authorize(Roles = "Centro,Admin")]
        [HttpGet("mi-centro/apelaciones")]
        public async Task<ActionResult<IEnumerable<CentroApelacionDto>>> ObtenerHistorialApelaciones()
        {
            var auth = await _authService.ValidarCentroAutenticadoAsync(User);
            if (!auth.EsValido || auth.Centro == null)
            {
                return StatusCode(auth.CodigoError ?? 401, new { mensaje = auth.MensajeError });
            }

            var apelaciones = await _context.CentrosApelaciones
                .AsNoTracking()
                .Include(a => a.Centro)
                .Include(a => a.RevisadoPorAdmin)
                .Where(a => a.CentroId == auth.Centro.Id)
                .OrderByDescending(a => a.FechaCreacion)
                .Select(a => new CentroApelacionDto
                {
                    Id = a.Id,
                    CentroId = a.CentroId,
                    CentroNombre = a.Centro != null ? a.Centro.Nombre : auth.Centro.Nombre,
                    MotivoApelacion = a.MotivoApelacion,
                    Estado = a.Estado,
                    FechaCreacion = a.FechaCreacion,
                    FechaRevision = a.FechaRevision,
                    RevisadoPorAdminId = a.RevisadoPorAdminId,
                    RevisadoPorAdminNombre = a.RevisadoPorAdmin != null ? a.RevisadoPorAdmin.Nombre : null,
                    RespuestaAdmin = a.RespuestaAdmin,
                    MotivoSuspensionOriginal = auth.Centro.MotivoSuspension,
                    FechaSuspensionOriginal = auth.Centro.FechaSuspension
                })
                .ToListAsync();

            return Ok(apelaciones);
        }
    }
}
