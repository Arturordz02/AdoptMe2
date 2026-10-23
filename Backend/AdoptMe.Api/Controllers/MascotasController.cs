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
    public class MascotasController : ControllerBase
    {
        private readonly AdoptMeDbContext _context;

        public MascotasController(AdoptMeDbContext context)
        {
            _context = context;
        }

        // GET: api/Mascotas
        // Obtener todas las mascotas
        [HttpGet]
        public async Task<ActionResult<IEnumerable<Mascota>>> ObtenerMascotas()
        {
            var mascotas = await _context.Mascotas
              .AsNoTracking()
              .OrderByDescending(m => m.FechaCreacion)
              .ToListAsync();

            return Ok(mascotas);
        }

        // GET: api/Mascotas/1
        // Obtener mascota por ID
        [HttpGet("{id:int}")]
        public async Task<ActionResult<Mascota>> ObtenerPorId(int id)
        {
            var mascota = await _context.Mascotas.FindAsync(id);

            if (mascota == null)
            {
                return NotFound(new
                {
                    mensaje = "Mascota no encontrada."
                });
            }

            return Ok(mascota);
        }

        // POST: api/Mascotas
        // Publicar nueva mascota
        [Authorize]
        [HttpPost]
        public async Task<ActionResult<Mascota>> CrearMascota(
            [FromBody] Mascota mascota)
        {
            var correoAutenticado = User.FindFirst(ClaimTypes.Email)?.Value
                ?? User.FindFirst("email")?.Value;

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

            _context.Mascotas.Add(mascota);
            await _context.SaveChangesAsync();

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
                mascota.ImageUrl = datos.ImageUrl.Trim();
            }

            // REGLA ESTRICTA: CreadoPorCorreo y FechaCreacion NUNCA se modifican desde el DTO.
            // Se preserva la autoría original y fecha de creación.

            await _context.SaveChangesAsync();

            return NoContent();
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
            );

            if (!esPropietario && !esAdmin)
            {
                return StatusCode(403, new
                {
                    mensaje = "No tienes autorización para eliminar esta mascota. Solo el propietario o un Administrador pueden hacerlo."
                });
            }

            _context.Mascotas.Remove(mascota);
            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}
