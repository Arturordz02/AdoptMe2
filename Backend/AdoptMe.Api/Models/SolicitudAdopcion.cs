using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace AdoptMe.Api.Models
{
    public class SolicitudAdopcion
    {
        [Key]
        public int Id { get; set; }

        [Required]
        public int MascotaId { get; set; }

        [ForeignKey(nameof(MascotaId))]
        public virtual Mascota? Mascota { get; set; }

        [Required]
        public int AdoptanteId { get; set; }

        [ForeignKey(nameof(AdoptanteId))]
        public virtual Usuario? Adoptante { get; set; }

        [Required]
        [MaxLength(100)]
        public string PetName { get; set; } = string.Empty;

        [MaxLength(50)]
        public string PetSpecies { get; set; } = string.Empty;

        [Required]
        [MaxLength(150)]
        public string ApplicantName { get; set; } = string.Empty;

        [Required]
        [MaxLength(20)]
        public string Phone { get; set; } = string.Empty;

        [Required]
        [MaxLength(200)]
        public string Email { get; set; } = string.Empty;

        [Required]
        [MaxLength(250)]
        public string Address { get; set; } = string.Empty;

        [Required]
        [MaxLength(100)]
        public string HousingType { get; set; } = string.Empty;

        [MaxLength(100)]
        public string TimeAvailability { get; set; } = string.Empty;

        [MaxLength(150)]
        public string PreviousExperience { get; set; } = string.Empty;

        [Required]
        [MaxLength(2000)]
        public string Reason { get; set; } = string.Empty;

        [Required]
        [MaxLength(30)]
        public string Status { get; set; } = "Pendiente";

        [MaxLength(1000)]
        public string? Observacion { get; set; }

        public DateTime FechaSolicitud { get; set; } = DateTime.UtcNow;

        public DateTime? FechaActualizacion { get; set; }
    }
}

