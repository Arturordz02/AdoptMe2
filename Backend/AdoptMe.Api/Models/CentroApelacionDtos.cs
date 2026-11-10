using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class CrearApelacionDto
    {
        [Required(ErrorMessage = "El motivo de la apelación es obligatorio.")]
        [MinLength(10, ErrorMessage = "El motivo de la apelación debe contener al menos 10 caracteres explicativos.")]
        [MaxLength(2000)]
        public string MotivoApelacion { get; set; } = string.Empty;
    }

    public class ResolverApelacionDto
    {
        [MaxLength(2000)]
        public string? RespuestaAdmin { get; set; }
    }

    public class CentroApelacionDto
    {
        public int Id { get; set; }
        public int CentroId { get; set; }
        public string CentroNombre { get; set; } = string.Empty;
        public string MotivoApelacion { get; set; } = string.Empty;
        public string Estado { get; set; } = "Pendiente";
        public DateTime FechaCreacion { get; set; }
        public DateTime? FechaRevision { get; set; }
        public int? RevisadoPorAdminId { get; set; }
        public string? RevisadoPorAdminNombre { get; set; }
        public string? RespuestaAdmin { get; set; }
        public string? MotivoSuspensionOriginal { get; set; }
        public DateTime? FechaSuspensionOriginal { get; set; }
    }
}
