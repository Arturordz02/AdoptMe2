using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class RegistroUsuarioDto
    {
        [Required]
        [MaxLength(100)]
        public string Nombre { get; set; } = "";

        [Required]
        [EmailAddress]
        public string Correo { get; set; } = "";

        [Required]
        [MinLength(6)]
        public string Contrasena { get; set; } = "";

        [Required]
        public string Telefono { get; set; } = "";
    }
}