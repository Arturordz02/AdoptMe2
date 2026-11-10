using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace AdoptMe.Api.Models
{
    /// <summary>
    /// Representa la solicitud formal de apelación presentada por un centro suspendido
    /// para solicitar la reconsideración y reactivación operativa ante la Administración.
    /// </summary>
    public class CentroApelacion
    {
        [Key]
        public int Id { get; set; }

        public int CentroId { get; set; }

        [ForeignKey(nameof(CentroId))]
        public virtual Centro? Centro { get; set; }

        [Required]
        [MaxLength(2000)]
        public string MotivoApelacion { get; set; } = string.Empty;

        [Required]
        [MaxLength(30)]
        public string Estado { get; set; } = "Pendiente"; // "Pendiente", "Aprobada", "Rechazada"

        public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;

        public DateTime? FechaRevision { get; set; }

        public int? RevisadoPorAdminId { get; set; }

        [ForeignKey(nameof(RevisadoPorAdminId))]
        public virtual Usuario? RevisadoPorAdmin { get; set; }

        [MaxLength(2000)]
        public string? RespuestaAdmin { get; set; }
    }
}
