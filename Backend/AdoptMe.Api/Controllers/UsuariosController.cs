using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using Microsoft.AspNetCore.Authorization;


namespace AdoptMe.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class UsuariosController : ControllerBase
    {
        private readonly AdoptMeDbContext _context;
        private readonly IPasswordHasher<Usuario> _passwordHasher;
        private readonly IConfiguration _configuration;

        
        public UsuariosController(
            AdoptMeDbContext context,
            IPasswordHasher<Usuario> passwordHasher,
            IConfiguration configuration)
        {
            _context = context;
            _passwordHasher = passwordHasher;
            _configuration = configuration;
        }


        [HttpPost("registro")]
        public async Task<IActionResult> Registrar(
            [FromBody] RegistroUsuarioDto datos)
        {
            string correo = datos.Correo.Trim().ToLowerInvariant();

            bool existe = await _context.Usuarios
                .AnyAsync(u => u.Correo == correo);

            if (existe)
            {
                return Conflict(new
                {
                    mensaje = "El correo ya se encuentra registrado"
                });
            }

            var usuario = new Usuario
            {
                Nombre = datos.Nombre.Trim(),
                Correo = correo,
                Telefono = datos.Telefono.Trim(),
                Rol = "Adoptante",
                FechaRegistro = DateTime.UtcNow
            };

            usuario.PasswordHash = _passwordHasher.HashPassword(
                usuario,
                datos.Contrasena
            );

            _context.Usuarios.Add(usuario);

            try
            {
                await _context.SaveChangesAsync();
            }
            catch (DbUpdateException)
            {
                return Conflict(new
                {
                    mensaje = "No se pudo completar el registro. Verifica los datos."
                });
            }

            return StatusCode(201, new
            {
                mensaje = "Usuario registrado correctamente",
                usuario.Id,
                usuario.Nombre,
                usuario.Correo,
                usuario.Rol
            });
        }
        
        [HttpPost("login")]
        public async Task<IActionResult> Login(
            [FromBody] LoginUsuarioDto datos)
        {
            string correo = datos.Correo.Trim().ToLowerInvariant();

            var usuario = await _context.Usuarios
                .FirstOrDefaultAsync(u => u.Correo == correo);

            if (usuario == null)
            {
                return Unauthorized(new
                {
                    mensaje = "Correo o contraseña incorrectos."
                });
            }

            var resultado = _passwordHasher.VerifyHashedPassword(
                usuario,
                usuario.PasswordHash,
                datos.Contrasena
            );

            if (resultado == PasswordVerificationResult.Failed)
            {
                return Unauthorized(new
                {
                    mensaje = "Correo o contraseña incorrectos."
                });
            }

            
            var jwtKey = _configuration["Jwt:Key"]
                ?? throw new InvalidOperationException(
                    "No se configuró la clave JWT."
                );

            var claims = new[]
            {
                new Claim(ClaimTypes.NameIdentifier, usuario.Id.ToString()),
                new Claim(ClaimTypes.Name, usuario.Nombre),
                new Claim(ClaimTypes.Email, usuario.Correo),
                new Claim(ClaimTypes.Role, usuario.Rol)
            };

            var key = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(jwtKey)
            );

            var credentials = new SigningCredentials(
                key,
                SecurityAlgorithms.HmacSha256
            );

            var expiration = DateTime.UtcNow.AddHours(2);

            var token = new JwtSecurityToken(
                claims: claims,
                expires: expiration,
                signingCredentials: credentials
            );

            var tokenString = new JwtSecurityTokenHandler()
                .WriteToken(token);

            return Ok(new
            {
                mensaje = "Inicio de sesión correcto",
                usuario.Id,
                usuario.Nombre,
                usuario.Correo,
                usuario.Telefono,
                usuario.Rol,
                token = tokenString,
                expiracion = expiration
            });
        }

        [Authorize]
        [HttpGet("perfil")]
        public async Task<IActionResult> ObtenerPerfil()
        {
            var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var emailClaim = User.FindFirst(ClaimTypes.Email)?.Value;

            Usuario? usuario = null;

            if (int.TryParse(userIdClaim, out int userId))
            {
                usuario = await _context.Usuarios
                    .AsNoTracking()
                    .FirstOrDefaultAsync(u => u.Id == userId);
            }

            if (usuario == null && !string.IsNullOrWhiteSpace(emailClaim))
            {
                string correo = emailClaim.Trim().ToLowerInvariant();
                usuario = await _context.Usuarios
                    .AsNoTracking()
                    .FirstOrDefaultAsync(u => u.Correo == correo);
            }

            if (usuario == null)
            {
                return NotFound(new
                {
                    mensaje = "Usuario no encontrado."
                });
            }

            return Ok(new
            {
                usuario.Id,
                usuario.Nombre,
                usuario.Correo,
                usuario.Telefono,
                usuario.Rol
            });
        }

        [Authorize]
        [HttpPut("perfil")]
        public async Task<IActionResult> ActualizarPerfil([FromBody] ActualizarPerfilDto dto)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var nombreLimpio = dto.Nombre?.Trim() ?? string.Empty;
            if (string.IsNullOrWhiteSpace(nombreLimpio))
            {
                return BadRequest(new
                {
                    mensaje = "El nombre no puede estar vacío."
                });
            }

            var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var emailClaim = User.FindFirst(ClaimTypes.Email)?.Value;

            Usuario? usuario = null;

            if (int.TryParse(userIdClaim, out int userId))
            {
                usuario = await _context.Usuarios
                    .FirstOrDefaultAsync(u => u.Id == userId);
            }

            if (usuario == null && !string.IsNullOrWhiteSpace(emailClaim))
            {
                string correo = emailClaim.Trim().ToLowerInvariant();
                usuario = await _context.Usuarios
                    .FirstOrDefaultAsync(u => u.Correo == correo);
            }

            if (usuario == null)
            {
                return NotFound(new
                {
                    mensaje = "Usuario no encontrado."
                });
            }

            // Permitir modificar únicamente nombre y teléfono
            usuario.Nombre = nombreLimpio;
            usuario.Telefono = dto.Telefono?.Trim() ?? string.Empty;

            await _context.SaveChangesAsync();

            return Ok(new
            {
                usuario.Id,
                usuario.Nombre,
                usuario.Correo,
                usuario.Telefono,
                usuario.Rol
            });
        }

        [Authorize]
        [HttpGet("sesion")]
        public IActionResult VerificarSesion()
        {
            return Ok(new
            {
                mensaje = "Sesión JWT válida",
                correo = User.FindFirst(ClaimTypes.Email)?.Value
            });
        }
    }
}