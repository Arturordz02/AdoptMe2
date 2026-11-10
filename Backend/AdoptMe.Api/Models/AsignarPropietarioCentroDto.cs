using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class AsignarPropietarioCentroDto
    {
        [Required(ErrorMessage = "El ID del usuario es obligatorio.")]
        public int UsuarioId { get; set; }
    }
}
