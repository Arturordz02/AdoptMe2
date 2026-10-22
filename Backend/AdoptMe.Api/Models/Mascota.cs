
using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class Mascota
    {
        [Key]
        public int Id { get; set; }

        [Required]
        [MaxLength(100)]
        public string Name { get; set; } = string.Empty;

        [Required]
        [MaxLength(50)]
        public string Species { get; set; } = string.Empty;

        [MaxLength(100)]
        public string Breed { get; set; } = string.Empty;

        [MaxLength(30)]
        public string Age { get; set; } = string.Empty;

        [MaxLength(30)]
        public string Gender { get; set; } = string.Empty;

        [MaxLength(150)]
        public string Location { get; set; } = "Lima";

        [MaxLength(30)]
        public string Status { get; set; } = "Disponible";

        [MaxLength(2000)]
        public string Description { get; set; } = string.Empty;

        // URL de la fotografía almacenada en la nube.
        public string? ImageUrl { get; set; }

        [MaxLength(200)]
        public string CreadoPorCorreo { get; set; } = string.Empty;

        public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;
    }
}

