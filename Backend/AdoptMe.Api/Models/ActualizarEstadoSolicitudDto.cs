using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class ActualizarEstadoSolicitudDto
    {
        [Required(ErrorMessage = "El campo status es obligatorio.")]
        [MaxLength(30)]
        public string Status { get; set; } = string.Empty;

        [MaxLength(1000)]
        public string? Observacion { get; set; }
    }
}

