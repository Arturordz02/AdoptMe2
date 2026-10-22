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
        // Editar publicación
        [NonAction]
        [HttpPut("{id:int}")]
        public async Task<IActionResult> ActualizarMascota(
          int id,
          [FromBody] Mascota datos)
        {
            var mascota = await _context.Mascotas.FindAsync(id);

            if (mascota == null)
            {
                return NotFound(new
                {
                    mensaje = "Mascota no encontrada."
                });
            }

            mascota.Name = datos.Name;
            mascota.Species = datos.Species;
            mascota.Breed = datos.Breed;
            mascota.Age = datos.Age;
            mascota.Gender = datos.Gender;
            mascota.Location = datos.Location;
            mascota.Status = datos.Status;
            mascota.Description = datos.Description;
            mascota.ImageUrl = datos.ImageUrl;
            mascota.CreadoPorCorreo = datos.CreadoPorCorreo;

            await _context.SaveChangesAsync();

            return NoContent();
        }

        // DELETE: api/Mascotas/1
        // Eliminar publicación
        [NonAction]
        [HttpDelete("{id:int}")]
        public async Task<IActionResult> EliminarMascota(int id)
        {
            var mascota = await _context.Mascotas.FindAsync(id);

            if (mascota == null)
            {
                return NotFound(new
                {
                    mensaje = "Mascota no encontrada."
                });
            }

            _context.Mascotas.Remove(mascota);
            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}
