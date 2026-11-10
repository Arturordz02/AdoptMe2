using System.Security.Claims;
using AdoptMe.Api.Models;

namespace AdoptMe.Api.Services
{
    public class CentroAuthResult
    {
        public bool EsValido { get; set; }
        public bool EsAdmin { get; set; }
        public bool EsCentro { get; set; }
        public bool EsCentroAprobado { get; set; }
        public Usuario? Usuario { get; set; }
        public Centro? Centro { get; set; }
        public string? EstadoCentro { get; set; }
        public int? CodigoError { get; set; }
        public string? MensajeError { get; set; }
    }

    public interface ICentroAuthorizationService
    {
        Task<CentroAuthResult> ValidarCentroAutenticadoAsync(ClaimsPrincipal user, CancellationToken cancellationToken = default);
        Task<CentroAuthResult> ValidarOperacionCentroAsync(ClaimsPrincipal user, CancellationToken cancellationToken = default);
        double CalcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2);
    }
}
