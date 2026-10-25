using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace AdoptMe.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class SolicitudesController : ControllerBase
    {
        private readonly AdoptMeDbContext _context;

        public SolicitudesController(AdoptMeDbContext context)
        {
            _context = context;
        }

        // POST: api/Solicitudes
        // Registrar una nueva solicitud de adopción real
        [Authorize]
        [HttpPost]
        public async Task<ActionResult<SolicitudResponseDto>> CrearSolicitud(
            [FromBody] CrearSolicitudDto dto)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            // 1. Obtener y validar identidad exclusiva del solicitante desde el JWT
            var idClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                ?? User.FindFirst("nameid")?.Value
                ?? User.FindFirst("sub")?.Value;

            if (!int.TryParse(idClaim, out int adoptanteId) || adoptanteId <= 0)
            {
                return Unauthorized(new
                {
                    mensaje = "Token inválido: no se pudo identificar al adoptante."
                });
            }

            var correoAutenticado = User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value;

            if (string.IsNullOrWhiteSpace(correoAutenticado))
            {
                return Unauthorized(new
                {
                    mensaje = "Token inválido: correo del usuario no encontrado."
                });
            }

            // 2. Verificar existencia del usuario solicitante
            var adoptante = await _context.Usuarios.FindAsync(adoptanteId);
            if (adoptante == null)
            {
                return Unauthorized(new
                {
                    mensaje = "El usuario autenticado no existe en la base de datos."
                });
            }

            // 3. Validar existencia y estado de la mascota
            var mascotaId = dto.MascotaId > 0 ? dto.MascotaId : (dto.PetId ?? 0);
            if (mascotaId <= 0)
            {
                return BadRequest(new
                {
                    mensaje = "El ID de la mascota es obligatorio y debe ser válido."
                });
            }

            var mascota = await _context.Mascotas.FindAsync(mascotaId);
            if (mascota == null)
            {
                return NotFound(new
                {
                    mensaje = "La mascota seleccionada no existe en el sistema."
                });
            }

            if (!string.Equals(mascota.Status?.Trim(), "Disponible", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new
                {
                    mensaje = $"La mascota no está disponible para adopción (Estado actual: {mascota.Status})."
                });
            }

            // 4. Validar que la mascota no pertenezca al mismo solicitante
            if (!string.IsNullOrWhiteSpace(mascota.CreadoPorCorreo) &&
                string.Equals(mascota.CreadoPorCorreo.Trim(), correoAutenticado.Trim(), StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(new
                {
                    mensaje = "No puedes solicitar la adopción de una mascota publicada por ti mismo."
                });
            }

            // 5. Validar que no exista solicitud activa duplicada del mismo adoptante
            bool tieneSolicitudActiva = await _context.Solicitudes
                .AnyAsync(s => s.MascotaId == mascotaId &&
                               s.AdoptanteId == adoptanteId &&
                               (s.Status == "Pendiente" ||
                                s.Status == "En revisión" ||
                                s.Status == "En revision" ||
                                s.Status == "Aprobada"));

            if (tieneSolicitudActiva)
            {
                return Conflict(new
                {
                    mensaje = "Ya cuentas con una solicitud activa o aprobada para esta mascota."
                });
            }

            // 6. Persistir solicitud con información del JWT y de la mascota verificada
            var nuevaSolicitud = new SolicitudAdopcion
            {
                MascotaId = mascota.Id,
                AdoptanteId = adoptante.Id,
                PetName = mascota.Name,
                PetSpecies = mascota.Species,
                ApplicantName = string.IsNullOrWhiteSpace(dto.ApplicantName) ? adoptante.Nombre : dto.ApplicantName.Trim(),
                Phone = string.IsNullOrWhiteSpace(dto.Phone) ? adoptante.Telefono : dto.Phone.Trim(),
                Email = correoAutenticado.Trim().ToLowerInvariant(), // Exclusivamente del JWT validado
                Address = dto.Address.Trim(),
                HousingType = dto.HousingType.Trim(),
                TimeAvailability = dto.TimeAvailability?.Trim() ?? string.Empty,
                PreviousExperience = dto.PreviousExperience?.Trim() ?? string.Empty,
                Reason = dto.Reason.Trim(),
                Status = "Pendiente",
                Observacion = null,
                FechaSolicitud = DateTime.UtcNow,
                FechaActualizacion = null
            };

            _context.Solicitudes.Add(nuevaSolicitud);
            await _context.SaveChangesAsync();

            var responseDto = new SolicitudResponseDto
            {
                Id = nuevaSolicitud.Id,
                MascotaId = nuevaSolicitud.MascotaId,
                AdoptanteId = nuevaSolicitud.AdoptanteId,
                PetName = nuevaSolicitud.PetName,
                PetSpecies = nuevaSolicitud.PetSpecies,
                ApplicantName = nuevaSolicitud.ApplicantName,
                Phone = nuevaSolicitud.Phone,
                Email = nuevaSolicitud.Email,
                Address = nuevaSolicitud.Address,
                HousingType = nuevaSolicitud.HousingType,
                TimeAvailability = nuevaSolicitud.TimeAvailability,
                PreviousExperience = nuevaSolicitud.PreviousExperience,
                Reason = nuevaSolicitud.Reason,
                Status = nuevaSolicitud.Status,
                Observacion = nuevaSolicitud.Observacion,
                FechaSolicitud = nuevaSolicitud.FechaSolicitud,
                FechaActualizacion = nuevaSolicitud.FechaActualizacion
            };

            return CreatedAtAction(
                nameof(ObtenerPorId),
                new { id = nuevaSolicitud.Id },
                responseDto
            );
        }

        // GET: api/Solicitudes/admin
        // Consultar todas las solicitudes registradas (exclusivo para Administrador)
        [Authorize(Roles = "Admin")]
        [HttpGet("admin")]
        public async Task<ActionResult<IEnumerable<SolicitudResponseDto>>> ObtenerTodasAdmin()
        {
            var solicitudes = await _context.Solicitudes
                .AsNoTracking()
                .OrderByDescending(s => s.FechaSolicitud)
                .Select(s => new SolicitudResponseDto
                {
                    Id = s.Id,
                    MascotaId = s.MascotaId,
                    AdoptanteId = s.AdoptanteId,
                    PetName = s.PetName,
                    PetSpecies = s.PetSpecies,
                    ApplicantName = s.ApplicantName,
                    Phone = s.Phone,
                    Email = s.Email,
                    Address = s.Address,
                    HousingType = s.HousingType,
                    TimeAvailability = s.TimeAvailability,
                    PreviousExperience = s.PreviousExperience,
                    Reason = s.Reason,
                    Status = s.Status,
                    Observacion = s.Observacion,
                    FechaSolicitud = s.FechaSolicitud,
                    FechaActualizacion = s.FechaActualizacion
                })
                .ToListAsync();

            return Ok(solicitudes);
        }

        // PUT: api/Solicitudes/5/estado
        // Actualizar el estado de una solicitud (Aprobar, Poner en revisión, Rechazar)
        [Authorize(Roles = "Admin")]
        [HttpPut("{id:int}/estado")]
        public async Task<ActionResult<SolicitudResponseDto>> ActualizarEstado(
            int id,
            [FromBody] ActualizarEstadoSolicitudDto dto)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var statusInput = dto.Status?.Trim();
            if (string.IsNullOrWhiteSpace(statusInput))
            {
                return BadRequest(new { mensaje = "El estado no puede estar vacío." });
            }

            // Normalizar variantes de estado
            string estadoNormalizado;
            if (string.Equals(statusInput, "Pendiente", StringComparison.OrdinalIgnoreCase))
            {
                estadoNormalizado = "Pendiente";
            }
            else if (string.Equals(statusInput, "En revisión", StringComparison.OrdinalIgnoreCase) ||
                     string.Equals(statusInput, "En revision", StringComparison.OrdinalIgnoreCase))
            {
                estadoNormalizado = "En revisión";
            }
            else if (string.Equals(statusInput, "Aprobada", StringComparison.OrdinalIgnoreCase))
            {
                estadoNormalizado = "Aprobada";
            }
            else if (string.Equals(statusInput, "Rechazada", StringComparison.OrdinalIgnoreCase))
            {
                estadoNormalizado = "Rechazada";
            }
            else
            {
                return BadRequest(new
                {
                    mensaje = $"Estado '{statusInput}' no admitido. Estados válidos: Pendiente, En revisión, Aprobada, Rechazada."
                });
            }

            var solicitud = await _context.Solicitudes.FindAsync(id);
            if (solicitud == null)
            {
                return NotFound(new { mensaje = $"La solicitud con ID {id} no fue encontrada." });
            }

            if (estadoNormalizado == "Aprobada")
            {
                var mascota = await _context.Mascotas.FindAsync(solicitud.MascotaId);
                if (mascota == null)
                {
                    return NotFound(new { mensaje = "La mascota asociada a esta solicitud no existe." });
                }

                if (!string.Equals(mascota.Status?.Trim(), "Disponible", StringComparison.OrdinalIgnoreCase))
                {
                    return BadRequest(new
                    {
                        mensaje = $"La mascota '{mascota.Name}' no está disponible para adopción (Estado actual: {mascota.Status})."
                    });
                }

                var strategy = _context.Database.CreateExecutionStrategy();
                return await strategy.ExecuteAsync(async () =>
                {
                    var isRelational = _context.Database.IsRelational();
                    var transaction = isRelational ? await _context.Database.BeginTransactionAsync() : null;

                    try
                    {
                        // 1. Cambiar estado de la mascota a "En proceso"
                        mascota.Status = "En proceso";

                        // 2. Actualizar solicitud a "Aprobada"
                        solicitud.Status = "Aprobada";
                        if (!string.IsNullOrWhiteSpace(dto.Observacion))
                        {
                            solicitud.Observacion = dto.Observacion.Trim();
                        }
                        solicitud.FechaActualizacion = DateTime.UtcNow;

                        // 3. Rechazar automáticamente otras solicitudes activas de la misma mascota
                        var otrasActivas = await _context.Solicitudes
                            .Where(s => s.MascotaId == solicitud.MascotaId &&
                                        s.Id != solicitud.Id &&
                                        (s.Status == "Pendiente" ||
                                         s.Status == "En revisión" ||
                                         s.Status == "En revision"))
                            .ToListAsync();

                        foreach (var otra in otrasActivas)
                        {
                            otra.Status = "Rechazada";
                            otra.Observacion = "Rechazada automáticamente: la mascota fue asignada a otra solicitud aprobada.";
                            otra.FechaActualizacion = DateTime.UtcNow;
                        }

                        await _context.SaveChangesAsync();

                        if (transaction != null)
                        {
                            await transaction.CommitAsync();
                        }

                        return Ok(new SolicitudResponseDto
                        {
                            Id = solicitud.Id,
                            MascotaId = solicitud.MascotaId,
                            AdoptanteId = solicitud.AdoptanteId,
                            PetName = solicitud.PetName,
                            PetSpecies = solicitud.PetSpecies,
                            ApplicantName = solicitud.ApplicantName,
                            Phone = solicitud.Phone,
                            Email = solicitud.Email,
                            Address = solicitud.Address,
                            HousingType = solicitud.HousingType,
                            TimeAvailability = solicitud.TimeAvailability,
                            PreviousExperience = solicitud.PreviousExperience,
                            Reason = solicitud.Reason,
                            Status = solicitud.Status,
                            Observacion = solicitud.Observacion,
                            FechaSolicitud = solicitud.FechaSolicitud,
                            FechaActualizacion = solicitud.FechaActualizacion
                        });
                    }
                    catch (Exception)
                    {
                        if (transaction != null)
                        {
                            await transaction.RollbackAsync();
                        }
                        throw;
                    }
                });
            }
            else
            {
                // En revisión, Rechazada o Pendiente
                solicitud.Status = estadoNormalizado;
                if (!string.IsNullOrWhiteSpace(dto.Observacion))
                {
                    solicitud.Observacion = dto.Observacion.Trim();
                }
                solicitud.FechaActualizacion = DateTime.UtcNow;

                await _context.SaveChangesAsync();

                return Ok(new SolicitudResponseDto
                {
                    Id = solicitud.Id,
                    MascotaId = solicitud.MascotaId,
                    AdoptanteId = solicitud.AdoptanteId,
                    PetName = solicitud.PetName,
                    PetSpecies = solicitud.PetSpecies,
                    ApplicantName = solicitud.ApplicantName,
                    Phone = solicitud.Phone,
                    Email = solicitud.Email,
                    Address = solicitud.Address,
                    HousingType = solicitud.HousingType,
                    TimeAvailability = solicitud.TimeAvailability,
                    PreviousExperience = solicitud.PreviousExperience,
                    Reason = solicitud.Reason,
                    Status = solicitud.Status,
                    Observacion = solicitud.Observacion,
                    FechaSolicitud = solicitud.FechaSolicitud,
                    FechaActualizacion = solicitud.FechaActualizacion
                });
            }
        }

        // GET: api/Solicitudes/mis-solicitudes
        // Consultar solicitudes realizadas por el adoptante autenticado
        [Authorize]
        [HttpGet("mis-solicitudes")]
        public async Task<ActionResult<IEnumerable<SolicitudResponseDto>>> MisSolicitudes()
        {
            var idClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                ?? User.FindFirst("nameid")?.Value
                ?? User.FindFirst("sub")?.Value;

            if (!int.TryParse(idClaim, out int adoptanteId) || adoptanteId <= 0)
            {
                return Unauthorized(new
                {
                    mensaje = "Token inválido: no se pudo identificar al usuario."
                });
            }

            var correoAutenticado = (User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value)?.Trim().ToLowerInvariant();

            var solicitudes = await _context.Solicitudes
                .AsNoTracking()
                .Where(s => s.AdoptanteId == adoptanteId ||
                            (!string.IsNullOrEmpty(correoAutenticado) && s.Email.ToLower() == correoAutenticado))
                .OrderByDescending(s => s.FechaSolicitud)
                .Select(s => new SolicitudResponseDto
                {
                    Id = s.Id,
                    MascotaId = s.MascotaId,
                    AdoptanteId = s.AdoptanteId,
                    PetName = s.PetName,
                    PetSpecies = s.PetSpecies,
                    ApplicantName = s.ApplicantName,
                    Phone = s.Phone,
                    Email = s.Email,
                    Address = s.Address,
                    HousingType = s.HousingType,
                    TimeAvailability = s.TimeAvailability,
                    PreviousExperience = s.PreviousExperience,
                    Reason = s.Reason,
                    Status = s.Status,
                    Observacion = s.Observacion,
                    FechaSolicitud = s.FechaSolicitud,
                    FechaActualizacion = s.FechaActualizacion
                })
                .ToListAsync();

            return Ok(solicitudes);
        }

        // GET: api/Solicitudes/5
        // Consultar detalle de solicitud por Id (solicitante, dueño de mascota o admin)
        [Authorize]
        [HttpGet("{id:int}")]
        public async Task<ActionResult<SolicitudResponseDto>> ObtenerPorId(int id)
        {
            var idClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                ?? User.FindFirst("nameid")?.Value
                ?? User.FindFirst("sub")?.Value;

            int.TryParse(idClaim, out int usuarioId);

            var correoAutenticado = (User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value)?.Trim().ToLowerInvariant();

            var rolAutenticado = User.FindFirst(ClaimTypes.Role)?.Value
                ?? User.FindFirst("role")?.Value;

            bool esAdmin = string.Equals(rolAutenticado, "Admin", StringComparison.OrdinalIgnoreCase);

            var solicitud = await _context.Solicitudes
                .Include(s => s.Mascota)
                .FirstOrDefaultAsync(s => s.Id == id);

            if (solicitud == null)
            {
                return NotFound(new
                {
                    mensaje = "Solicitud no encontrada."
                });
            }

            bool esSolicitante = solicitud.AdoptanteId == usuarioId ||
                string.Equals(solicitud.Email, correoAutenticado, StringComparison.OrdinalIgnoreCase);

            bool esPropietarioMascota = solicitud.Mascota != null &&
                string.Equals(solicitud.Mascota.CreadoPorCorreo, correoAutenticado, StringComparison.OrdinalIgnoreCase);

            if (!esSolicitante && !esPropietarioMascota && !esAdmin)
            {
                return StatusCode(403, new
                {
                    mensaje = "No tienes autorización para consultar esta solicitud."
                });
            }

            var responseDto = new SolicitudResponseDto
            {
                Id = solicitud.Id,
                MascotaId = solicitud.MascotaId,
                AdoptanteId = solicitud.AdoptanteId,
                PetName = solicitud.PetName,
                PetSpecies = solicitud.PetSpecies,
                ApplicantName = solicitud.ApplicantName,
                Phone = solicitud.Phone,
                Email = solicitud.Email,
                Address = solicitud.Address,
                HousingType = solicitud.HousingType,
                TimeAvailability = solicitud.TimeAvailability,
                PreviousExperience = solicitud.PreviousExperience,
                Reason = solicitud.Reason,
                Status = solicitud.Status,
                Observacion = solicitud.Observacion,
                FechaSolicitud = solicitud.FechaSolicitud,
                FechaActualizacion = solicitud.FechaActualizacion
            };

            return Ok(responseDto);
        }
    }
}

