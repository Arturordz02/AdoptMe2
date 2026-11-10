using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class SuspenderCentroDto
    {
        [Required(ErrorMessage = "El motivo de suspensión es obligatorio.")]
        [MinLength(5, ErrorMessage = "El motivo de suspensión debe contener al menos 5 caracteres explicativos.")]
        [MaxLength(500, ErrorMessage = "El motivo de suspensión no puede exceder 500 caracteres.")]
        public string Motivo { get; set; } = string.Empty;
    }
}
