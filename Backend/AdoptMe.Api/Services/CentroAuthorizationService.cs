using System.Security.Claims;
using AdoptMe.Api.Data;
using AdoptMe.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace AdoptMe.Api.Services
{
    public class CentroAuthorizationService : ICentroAuthorizationService
    {
        private readonly AdoptMeDbContext _context;

        public CentroAuthorizationService(AdoptMeDbContext context)
        {
            _context = context;
        }

        public async Task<CentroAuthResult> ValidarCentroAutenticadoAsync(ClaimsPrincipal user, CancellationToken cancellationToken = default)
        {
            var correo = user.FindFirst(ClaimTypes.Email)?.Value
                ?? user.FindFirst("email")?.Value;
            var rol = user.FindFirst(ClaimTypes.Role)?.Value
                ?? user.FindFirst("role")?.Value;
            var idClaim = user.FindFirst(ClaimTypes.NameIdentifier)?.Value
                ?? user.FindFirst("nameid")?.Value
                ?? user.FindFirst("sub")?.Value;

            if (string.IsNullOrWhiteSpace(correo))
            {
                return new CentroAuthResult
                {
                    EsValido = false,
                    CodigoError = 401,
                    MensajeError = "No se pudo identificar al usuario a partir del token JWT."
                };
            }

            bool esAdmin = string.Equals(rol, "Admin", StringComparison.OrdinalIgnoreCase);
            bool esCentro = string.Equals(rol, "Centro", StringComparison.OrdinalIgnoreCase);

            // Buscar usuario en base de datos
            Usuario? usuario = null;
            if (int.TryParse(idClaim, out int uid) && uid > 0)
            {
                usuario = await _context.Usuarios.FirstOrDefaultAsync(u => u.Id == uid, cancellationToken);
            }

            if (usuario == null)
            {
                string correoNorm = correo.Trim().ToLowerInvariant();
                usuario = await _context.Usuarios.FirstOrDefaultAsync(u => u.Correo == correoNorm, cancellationToken);
            }

            if (usuario == null)
            {
                return new CentroAuthResult
                {
                    EsValido = false,
                    CodigoError = 401,
                    MensajeError = "El usuario autenticado no existe en la base de datos."
                };
            }

            // Si es rol Centro, buscar su centro vinculado
            Centro? centro = null;
            if (esCentro)
            {
                centro = await _context.Centros
                    .FirstOrDefaultAsync(c => c.UsuarioId == usuario.Id, cancellationToken);
            }

            bool esAprobado = centro != null &&
                              string.Equals(centro.Estado, "Aprobado", StringComparison.OrdinalIgnoreCase) &&
                              centro.Activo;

            return new CentroAuthResult
            {
                EsValido = true,
                EsAdmin = esAdmin,
                EsCentro = esCentro,
                EsCentroAprobado = esAprobado,
                Usuario = usuario,
                Centro = centro,
                EstadoCentro = centro?.Estado ?? (esCentro ? "SinPerfil" : null)
            };
        }

        public async Task<CentroAuthResult> ValidarOperacionCentroAsync(ClaimsPrincipal user, CancellationToken cancellationToken = default)
        {
            var auth = await ValidarCentroAutenticadoAsync(user, cancellationToken);
            if (!auth.EsValido)
            {
                return auth;
            }

            if (auth.EsAdmin)
            {
                return auth;
            }

            if (!auth.EsCentro)
            {
                return new CentroAuthResult
                {
                    EsValido = false,
                    CodigoError = 403,
                    MensajeError = "Esta acción requiere rol de Centro de Adopción o Administrador."
                };
            }

            if (auth.Centro == null)
            {
                return new CentroAuthResult
                {
                    EsValido = false,
                    CodigoError = 403,
                    MensajeError = "El usuario no cuenta con un Centro de Adopción registrado en el sistema."
                };
            }

            if (!auth.Centro.Activo)
            {
                return new CentroAuthResult
                {
                    EsValido = false,
                    CodigoError = 403,
                    MensajeError = "El centro se encuentra temporalmente inactivo. Contacta a un Administrador."
                };
            }

            if (!string.Equals(auth.Centro.Estado, "Aprobado", StringComparison.OrdinalIgnoreCase))
            {
                return new CentroAuthResult
                {
                    EsValido = false,
                    CodigoError = 403,
                    MensajeError = $"El centro no está autorizado para realizar operaciones institucionales (Estado actual: {auth.Centro.Estado})."
                };
            }

            return auth;
        }

        public double CalcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2)
        {
            const double R = 6371000.0; // Radio de la Tierra en metros
            double dLat = ToRadians(lat2 - lat1);
            double dLon = ToRadians(lon2 - lon1);

            double a = Math.Sin(dLat / 2.0) * Math.Sin(dLat / 2.0) +
                       Math.Cos(ToRadians(lat1)) * Math.Cos(ToRadians(lat2)) *
                       Math.Sin(dLon / 2.0) * Math.Sin(dLon / 2.0);

            double c = 2.0 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1.0 - a));
            return R * c;
        }

        private static double ToRadians(double deg) => deg * (Math.PI / 180.0);
    }
}
