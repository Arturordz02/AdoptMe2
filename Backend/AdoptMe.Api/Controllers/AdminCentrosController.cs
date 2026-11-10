using System.Security.Claims;
using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace AdoptMe.Api.Controllers
{
    [ApiController]
    [Route("api/admin/centros")]
    [Authorize(Roles = "Admin,Administrador")]
    public class AdminCentrosController : ControllerBase
    {
        private readonly AdoptMeDbContext _context;
        private readonly ILogger<AdminCentrosController> _logger;

        public AdminCentrosController(
            AdoptMeDbContext context,
            ILogger<AdminCentrosController> logger)
        {
            _context = context;
            _logger = logger;
        }

        private int? ObtenerAdminId()
        {
            var idClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                ?? User.FindFirst("nameid")?.Value
                ?? User.FindFirst("sub")?.Value;

            return int.TryParse(idClaim, out int aid) ? aid : null;
        }

        // GET: api/admin/centros
        // Listar centros con filtros administrativos por estado, activo y búsqueda
        [HttpGet]
        public async Task<ActionResult<IEnumerable<CentroAdminDto>>> ListarCentrosAdmin(
            [FromQuery] string? estado = null,
            [FromQuery] bool? activo = null,
            [FromQuery] bool? esDemostrativo = null,
            [FromQuery] string? q = null)
        {
            var query = _context.Centros
                .Include(c => c.Usuario)
                .Include(c => c.RevisadoPorUsuario)
                .Include(c => c.Mascotas)
                .AsNoTracking();

            if (!string.IsNullOrWhiteSpace(estado) && !string.Equals(estado.Trim(), "Todos", StringComparison.OrdinalIgnoreCase))
            {
                string estNorm = estado.Trim();
                query = query.Where(c => c.Estado.ToLower() == estNorm.ToLower());
            }

            if (activo.HasValue)
            {
                query = query.Where(c => c.Activo == activo.Value);
            }

            if (esDemostrativo.HasValue)
            {
                query = query.Where(c => c.EsDemostrativo == esDemostrativo.Value);
            }

            if (!string.IsNullOrWhiteSpace(q))
            {
                string qNorm = q.Trim().ToLower();
                query = query.Where(c => c.Nombre.ToLower().Contains(qNorm) ||
                                         c.Distrito.ToLower().Contains(qNorm) ||
                                         c.CorreoContacto.ToLower().Contains(qNorm) ||
                                         (c.Usuario != null && c.Usuario.Nombre.ToLower().Contains(qNorm)));
            }

            var centros = await query
                .OrderByDescending(c => c.FechaSolicitud)
                .Select(c => new CentroAdminDto
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
                    FechaSolicitud = c.FechaSolicitud,
                    FechaRevision = c.FechaRevision,
                    EsDemostrativo = c.EsDemostrativo,
                    Activo = c.Activo,
                    FechaActualizacion = c.FechaActualizacion,
                    NombreResponsable = c.Usuario != null ? c.Usuario.Nombre : null,
                    CorreoResponsable = c.Usuario != null ? c.Usuario.Correo : null,
                    TelefonoResponsable = c.Usuario != null ? c.Usuario.Telefono : null,
                    RevisadoPorUsuarioId = c.RevisadoPorUsuarioId,
                    RevisadoPorNombre = c.RevisadoPorUsuario != null ? c.RevisadoPorUsuario.Nombre : null,
                    FechaCreacion = c.FechaCreacion,
                    TotalMascotasPublicadas = c.Mascotas.Count
                })
                .ToListAsync();

            return Ok(centros);
        }

        // GET: api/admin/centros/5
        // Detalle completo y administrativo de una solicitud de centro
        [HttpGet("{id:int}")]
        public async Task<ActionResult<CentroAdminDto>> ObtenerDetalleAdmin(int id)
        {
            var c = await _context.Centros
                .Include(c => c.Usuario)
                .Include(c => c.RevisadoPorUsuario)
                .Include(c => c.Mascotas)
                .AsNoTracking()
                .FirstOrDefaultAsync(c => c.Id == id);

            if (c == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            return Ok(new CentroAdminDto
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
                FechaSolicitud = c.FechaSolicitud,
                FechaRevision = c.FechaRevision,
                EsDemostrativo = c.EsDemostrativo,
                Activo = c.Activo,
                FechaActualizacion = c.FechaActualizacion,
                NombreResponsable = c.Usuario != null ? c.Usuario.Nombre : null,
                CorreoResponsable = c.Usuario != null ? c.Usuario.Correo : null,
                TelefonoResponsable = c.Usuario != null ? c.Usuario.Telefono : null,
                RevisadoPorUsuarioId = c.RevisadoPorUsuarioId,
                RevisadoPorNombre = c.RevisadoPorUsuario != null ? c.RevisadoPorUsuario.Nombre : null,
                FechaCreacion = c.FechaCreacion,
                TotalMascotasPublicadas = c.Mascotas.Count
            });
        }

        // PUT: api/admin/centros/5/aprobar
        // Aprobar la solicitud de un centro para que opere en la plataforma y aparezca públicamente
        [HttpPut("{id:int}/aprobar")]
        public async Task<IActionResult> AprobarCentro(int id)
        {
            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            int? adminId = ObtenerAdminId();

            centro.Estado = "Aprobado";
            centro.MotivoRechazo = null;
            centro.FechaRevision = DateTime.UtcNow;
            centro.RevisadoPorUsuarioId = adminId;
            centro.FechaActualizacion = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            _logger.LogInformation("Centro ID {Id} ('{Nombre}') aprobado por administrador ID {AdminId}.", centro.Id, centro.Nombre, adminId);

            return Ok(new
            {
                mensaje = "Centro de adopción aprobado exitosamente.",
                id = centro.Id,
                estado = centro.Estado,
                fechaRevision = centro.FechaRevision
            });
        }

        // PUT: api/admin/centros/5/rechazar
        // Rechazar la solicitud de un centro con justificación obligatoria
        [HttpPut("{id:int}/rechazar")]
        public async Task<IActionResult> RechazarCentro(int id, [FromBody] RechazarCentroDto dto)
        {
            if (!ModelState.IsValid || string.IsNullOrWhiteSpace(dto.Motivo))
            {
                return BadRequest(new { mensaje = "El motivo de rechazo es obligatorio y debe ser explicativo." });
            }

            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            int? adminId = ObtenerAdminId();

            centro.Estado = "Rechazado";
            centro.MotivoRechazo = dto.Motivo.Trim();
            centro.FechaRevision = DateTime.UtcNow;
            centro.RevisadoPorUsuarioId = adminId;
            centro.FechaActualizacion = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            _logger.LogInformation("Centro ID {Id} ('{Nombre}') rechazado por administrador ID {AdminId}. Motivo: '{Motivo}'.",
                centro.Id, centro.Nombre, adminId, centro.MotivoRechazo);

            return Ok(new
            {
                mensaje = "Centro de adopción rechazado con motivo registrado.",
                id = centro.Id,
                estado = centro.Estado,
                motivoRechazo = centro.MotivoRechazo,
                fechaRevision = centro.FechaRevision
            });
        }

        // PUT: api/admin/centros/5/activar
        // Reactivar un centro previamente suspendido
        [HttpPut("{id:int}/activar")]
        public async Task<IActionResult> ActivarCentro(int id)
        {
            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            centro.Activo = true;
            centro.FechaActualizacion = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return Ok(new { mensaje = "Centro de adopción activado exitosamente.", id = centro.Id, activo = true });
        }

        // PUT: api/admin/centros/5/desactivar
        // Suspender temporalmente un centro (oculto de búsquedas públicas)
        [HttpPut("{id:int}/desactivar")]
        public async Task<IActionResult> DesactivarCentro(int id)
        {
            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            centro.Activo = false;
            centro.FechaActualizacion = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return Ok(new { mensaje = "Centro de adopción desactivado temporalmente.", id = centro.Id, activo = false });
        }

        // PUT: api/admin/centros/5/asignar-propietario
        // Asignar o transferir un usuario real de rol Centro a un centro de adopción
        [HttpPut("{id:int}/asignar-propietario")]
        public async Task<IActionResult> AsignarPropietario(int id, [FromBody] AsignarPropietarioCentroDto dto)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            // Si el centro ya posee ese mismo titular, la operación es idempotente
            if (centro.UsuarioId.HasValue && centro.UsuarioId.Value == dto.UsuarioId)
            {
                return Ok(new
                {
                    mensaje = "El usuario ya es el propietario asignado de este centro de adopción.",
                    centroId = centro.Id,
                    usuarioId = dto.UsuarioId
                });
            }

            var usuario = await _context.Usuarios.FirstOrDefaultAsync(u => u.Id == dto.UsuarioId);
            if (usuario == null)
            {
                return NotFound(new { mensaje = $"Usuario con ID {dto.UsuarioId} no encontrado." });
            }

            if (!string.Equals(usuario.Rol, "Centro", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new { mensaje = $"El usuario debe tener rol 'Centro' para ser asignado como propietario. Rol actual: '{usuario.Rol}'." });
            }

            // Comprobar que no posea ya otro centro diferente (evitar infracción del índice único parcial)
            bool yaPoseeOtroCentro = await _context.Centros.AnyAsync(c => c.UsuarioId == usuario.Id && c.Id != centro.Id);
            if (yaPoseeOtroCentro)
            {
                return Conflict(new { mensaje = $"El usuario con ID {dto.UsuarioId} ya posee otro centro de adopción asignado." });
            }

            int? adminId = ObtenerAdminId();
            int? anteriorUsuarioId = centro.UsuarioId;
            bool esTransferencia = anteriorUsuarioId.HasValue && anteriorUsuarioId.Value != usuario.Id;

            var strategy = _context.Database.CreateExecutionStrategy();

            try
            {
                await strategy.ExecuteAsync(async () =>
                {
                    using var tx = await _context.Database.BeginTransactionAsync();
                    centro.UsuarioId = usuario.Id;
                    centro.FechaActualizacion = DateTime.UtcNow;
                    await _context.SaveChangesAsync();
                    await tx.CommitAsync();
                });

                if (esTransferencia)
                {
                    _logger.LogInformation("Transferencia de titularidad en Centro ID {CentroId} ('{NombreCentro}'): Propietario anterior Usuario ID {AnteriorId} -> Nuevo propietario Usuario ID {NuevoId}. Ejecutado por Admin ID {AdminId}.",
                        centro.Id, centro.Nombre, anteriorUsuarioId, usuario.Id, adminId);
                }
                else
                {
                    _logger.LogInformation("Asignación de titularidad en Centro ID {CentroId} ('{NombreCentro}'): Propietario asignado Usuario ID {NuevoId}. Ejecutado por Admin ID {AdminId}.",
                        centro.Id, centro.Nombre, usuario.Id, adminId);
                }

                return Ok(new
                {
                    mensaje = esTransferencia
                        ? "Transferencia de titularidad del centro completada exitosamente."
                        : "Propietario asignado al centro exitosamente.",
                    centroId = centro.Id,
                    usuarioId = usuario.Id
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error al persistir la asignación de propietario en Centro ID {CentroId}.", centro.Id);
                return StatusCode(500, new { mensaje = "Ocurrió un error interno al asignar el propietario del centro." });
            }
        }
    }
}
