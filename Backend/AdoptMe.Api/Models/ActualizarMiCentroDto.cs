using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    public class ActualizarMiCentroDto
    {
        [Required(ErrorMessage = "El nombre del centro es obligatorio.")]
        [MaxLength(150)]
        public string Nombre { get; set; } = string.Empty;

        [Required(ErrorMessage = "La descripción es obligatoria.")]
        [MaxLength(1000)]
        public string Descripcion { get; set; } = string.Empty;

        [Required(ErrorMessage = "La dirección es obligatoria.")]
        [MaxLength(250)]
        public string Direccion { get; set; } = string.Empty;

        [Required(ErrorMessage = "El distrito es obligatorio.")]
        [MaxLength(100)]
        public string Distrito { get; set; } = string.Empty;

        [Required(ErrorMessage = "El teléfono es obligatorio.")]
        [MaxLength(20)]
        public string Telefono { get; set; } = string.Empty;

        [Required(ErrorMessage = "El correo de contacto es obligatorio.")]
        [EmailAddress(ErrorMessage = "El correo de contacto no tiene formato válido.")]
        [MaxLength(150)]
        public string CorreoContacto { get; set; } = string.Empty;

        [Required(ErrorMessage = "El horario es obligatorio.")]
        [MaxLength(150)]
        public string Horario { get; set; } = string.Empty;

        [Range(-90.0, 90.0, ErrorMessage = "La latitud debe estar comprendida entre -90 y 90 grados.")]
        public double? Latitud { get; set; }

        [Range(-180.0, 180.0, ErrorMessage = "La longitud debe estar comprendida entre -180 y 180 grados.")]
        public double? Longitud { get; set; }
    }
}
