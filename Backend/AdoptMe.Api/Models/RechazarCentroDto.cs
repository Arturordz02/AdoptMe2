using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class RechazarCentroDto
    {
        [Required(ErrorMessage = "El motivo de rechazo es obligatorio.")]
        [MinLength(5, ErrorMessage = "El motivo de rechazo debe contener al menos 5 caracteres explicativos.")]
        [MaxLength(500, ErrorMessage = "El motivo de rechazo no puede exceder 500 caracteres.")]
        public string Motivo { get; set; } = string.Empty;
    }
}
