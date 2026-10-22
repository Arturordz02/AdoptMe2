using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class LoginUsuarioDto
    {
        [Required]
        [EmailAddress]
        public string Correo { get; set; } = "";

        [Required]
        public string Contrasena { get; set; } = "";
    }
}
