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
                .Include(c => c.SuspendidoPorAdmin)
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
                    MotivoSuspension = c.MotivoSuspension,
                    FechaSuspension = c.FechaSuspension,
                    FechaReactivacion = c.FechaReactivacion,
                    SuspendidoPorAdminId = c.SuspendidoPorAdminId,
                    SuspendidoPorAdminNombre = c.SuspendidoPorAdmin != null ? c.SuspendidoPorAdmin.Nombre : null,
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
                .Include(c => c.SuspendidoPorAdmin)
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
                MotivoSuspension = c.MotivoSuspension,
                FechaSuspension = c.FechaSuspension,
                FechaReactivacion = c.FechaReactivacion,
                SuspendidoPorAdminId = c.SuspendidoPorAdminId,
                SuspendidoPorAdminNombre = c.SuspendidoPorAdmin != null ? c.SuspendidoPorAdmin.Nombre : null,
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
        // Aprobar la solicitud inicial de un centro para que opere en la plataforma y aparezca públicamente
        [HttpPut("{id:int}/aprobar")]
        public async Task<IActionResult> AprobarCentro(int id)
        {
            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            if (string.Equals(centro.Estado, "Aprobado", StringComparison.OrdinalIgnoreCase))
            {
                if (centro.Activo)
                {
                    return Ok(new
                    {
                        mensaje = "El centro ya se encuentra aprobado y activo en la plataforma.",
                        id = centro.Id,
                        estado = centro.Estado,
                        activo = centro.Activo
                    });
                }
                else
                {
                    return Conflict(new
                    {
                        mensaje = "El centro ya está formalmente aprobado pero se encuentra suspendido/inactivo. Utiliza la acción de reactivación."
                    });
                }
            }

            if (string.Equals(centro.Estado, "Rechazado", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new
                {
                    mensaje = "No se puede aprobar directamente una solicitud rechazada. El responsable del centro debe reenviar su solicitud para nueva revisión."
                });
            }

            int? adminId = ObtenerAdminId();

            centro.Estado = "Aprobado";
            centro.Activo = true;
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
                activo = centro.Activo,
                fechaRevision = centro.FechaRevision
            });
        }

        // PUT: api/admin/centros/5/rechazar
        // Rechazar la solicitud inicial de un centro con justificación obligatoria
        [HttpPut("{id:int}/rechazar")]
        public async Task<IActionResult> RechazarCentro(int id, [FromBody] RechazarCentroDto dto)
        {
            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            // Regla fundamental de la máquina de estados: Rechazo solo aplica durante la evaluación inicial
            if (string.Equals(centro.Estado, "Aprobado", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new
                {
                    mensaje = "El rechazo solo se aplica durante la evaluación inicial. Para retirar temporalmente un centro aprobado utiliza la suspensión."
                });
            }

            if (!string.Equals(centro.Estado, "Pendiente", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new
                {
                    mensaje = $"Solo se pueden rechazar solicitudes en estado 'Pendiente' de revisión. Estado actual: '{centro.Estado}'."
                });
            }

            string motivoLimpio = dto.Motivo?.Trim() ?? string.Empty;
            if (motivoLimpio.Length < 5)
            {
                return BadRequest(new { mensaje = "El motivo de rechazo es obligatorio y debe contener al menos 5 caracteres explicativos." });
            }

            int? adminId = ObtenerAdminId();

            centro.Estado = "Rechazado";
            centro.MotivoRechazo = motivoLimpio;
            centro.FechaRevision = DateTime.UtcNow;
            centro.RevisadoPorUsuarioId = adminId;
            centro.FechaActualizacion = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            _logger.LogInformation("Centro ID {Id} ('{Nombre}') rechazado en evaluación inicial por administrador ID {AdminId}. Motivo: '{Motivo}'.",
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

        // PUT: api/admin/centros/5/suspender
        // Suspender operativamente un centro ya aprobado conservando cuenta, datos e historial
        [HttpPut("{id:int}/suspender")]
        public async Task<IActionResult> SuspenderCentro(int id, [FromBody] SuspenderCentroDto dto)
        {
            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            if (!string.Equals(centro.Estado, "Aprobado", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new
                {
                    mensaje = $"Solo se puede suspender operativamente un centro en estado 'Aprobado'. Estado actual: '{centro.Estado}'."
                });
            }

            string motivoLimpio = dto.Motivo?.Trim() ?? string.Empty;
            if (motivoLimpio.Length < 5)
            {
                return BadRequest(new { mensaje = "El motivo de suspensión es obligatorio y debe contener al menos 5 caracteres explicativos." });
            }

            if (!centro.Activo && !string.IsNullOrWhiteSpace(centro.MotivoSuspension))
            {
                return Conflict(new { mensaje = "El centro ya se encuentra formalmente suspendido." });
            }

            int? adminId = ObtenerAdminId();

            centro.Activo = false;
            centro.MotivoSuspension = motivoLimpio;
            centro.FechaSuspension = DateTime.UtcNow;
            centro.SuspendidoPorAdminId = adminId;
            centro.FechaReactivacion = null;
            centro.FechaActualizacion = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            _logger.LogInformation("Centro ID {Id} ('{Nombre}') suspendido operativamente por administrador ID {AdminId}. Motivo: '{Motivo}'.",
                centro.Id, centro.Nombre, adminId, centro.MotivoSuspension);

            return Ok(new
            {
                mensaje = "Centro de adopción suspendido operativamente.",
                id = centro.Id,
                estado = centro.Estado,
                activo = centro.Activo,
                motivoSuspension = centro.MotivoSuspension,
                fechaSuspension = centro.FechaSuspension
            });
        }

        // PUT: api/admin/centros/5/reactivar
        // Reactivar un centro previamente suspendido o inactivo, conservando historial de auditoría
        [HttpPut("{id:int}/reactivar")]
        public async Task<IActionResult> ReactivarCentro(int id)
        {
            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            if (!string.Equals(centro.Estado, "Aprobado", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new
                {
                    mensaje = $"Solo se puede reactivar un centro con estado 'Aprobado'. Estado actual: '{centro.Estado}'."
                });
            }

            if (centro.Activo)
            {
                return Conflict(new { mensaje = "El centro ya se encuentra activo en la plataforma." });
            }

            centro.Activo = true;
            centro.FechaReactivacion = DateTime.UtcNow;
            centro.FechaActualizacion = DateTime.UtcNow;
            // Preservamos MotivoSuspension, FechaSuspension y SuspendidoPorAdminId para auditoría histórica

            await _context.SaveChangesAsync();

            _logger.LogInformation("Centro ID {Id} ('{Nombre}') reactivado por administrador.", centro.Id, centro.Nombre);

            return Ok(new
            {
                mensaje = "Centro de adopción reactivado exitosamente.",
                id = centro.Id,
                estado = centro.Estado,
                activo = centro.Activo,
                fechaReactivacion = centro.FechaReactivacion
            });
        }

        // Alias retrocompatible para reactivar
        [HttpPut("{id:int}/activar")]
        public Task<IActionResult> ActivarCentro(int id) => ReactivarCentro(id);

        // Alias retrocompatible para suspender/desactivar
        [HttpPut("{id:int}/desactivar")]
        public async Task<IActionResult> DesactivarCentro(int id, [FromBody] SuspenderCentroDto? dto = null)
        {
            var centro = await _context.Centros.FirstOrDefaultAsync(c => c.Id == id);
            if (centro == null)
            {
                return NotFound(new { mensaje = $"Centro con ID {id} no encontrado." });
            }

            if (dto != null && !string.IsNullOrWhiteSpace(dto.Motivo))
            {
                return await SuspenderCentro(id, dto);
            }

            // Desactivación técnica sin motivo formal de suspensión
            centro.Activo = false;
            centro.FechaActualizacion = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return Ok(new { mensaje = "Centro de adopción desactivado temporalmente.", id = centro.Id, activo = false });
        }

        // PUT: api/admin/centros/5/asignar-propietario
        // PUT: api/admin/centros/5/vincular-responsable
        // Vincular un usuario real de rol Centro a un centro de adopción sin titular asignado (relación 1:1)
        [HttpPut("{id:int}/asignar-propietario")]
        [HttpPut("{id:int}/vincular-responsable")]
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
                    mensaje = "El usuario ya es el responsable asignado de este centro de adopción.",
                    centroId = centro.Id,
                    usuarioId = dto.UsuarioId
                });
            }

            // Regla de integridad: No sobrescribir silenciosamente un responsable existente
            if (centro.UsuarioId.HasValue)
            {
                return Conflict(new
                {
                    mensaje = "Este centro ya tiene un responsable vinculado. No se permite sobrescribir un responsable existente."
                });
            }

            var usuario = await _context.Usuarios.FirstOrDefaultAsync(u => u.Id == dto.UsuarioId);
            if (usuario == null)
            {
                return NotFound(new { mensaje = $"Usuario con ID {dto.UsuarioId} no encontrado." });
            }

            if (!string.Equals(usuario.Rol, "Centro", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new
                {
                    mensaje = $"El usuario debe tener rol 'Centro' para ser vinculado como responsable institucional. Rol actual: '{usuario.Rol}'."
                });
            }

            // Comprobar que no posea ya otro centro diferente (evitar infracción del índice único parcial 1:1)
            bool yaPoseeOtroCentro = await _context.Centros.AnyAsync(c => c.UsuarioId == usuario.Id && c.Id != centro.Id);
            if (yaPoseeOtroCentro)
            {
                return Conflict(new { mensaje = $"El usuario con ID {dto.UsuarioId} ya posee otro centro de adopción asignado." });
            }

            int? adminId = ObtenerAdminId();
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

                _logger.LogInformation("Vinculación de responsable en Centro ID {CentroId} ('{NombreCentro}'): Responsable asignado Usuario ID {NuevoId}. Ejecutado por Admin ID {AdminId}.",
                    centro.Id, centro.Nombre, usuario.Id, adminId);

                return Ok(new
                {
                    mensaje = "Responsable vinculado al centro exitosamente.",
                    centroId = centro.Id,
                    usuarioId = usuario.Id
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error al persistir la vinculación de responsable en Centro ID {CentroId}.", centro.Id);
                return StatusCode(500, new { mensaje = "Ocurrió un error interno al vincular el responsable del centro." });
            }
        }
    }
}
