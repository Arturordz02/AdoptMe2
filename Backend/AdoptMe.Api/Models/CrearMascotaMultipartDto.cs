using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    /// <summary>
    /// DTO para la creación de mascotas mediante multipart/form-data con imagen opcional.
    /// </summary>
    public class CrearMascotaMultipartDto
    {
        [Required(ErrorMessage = "El nombre de la mascota es obligatorio.")]
        [MaxLength(100)]
        public string Name { get; set; } = string.Empty;

        [Required(ErrorMessage = "La especie de la mascota es obligatoria.")]
        [MaxLength(50)]
        public string Species { get; set; } = string.Empty;

        [MaxLength(100)]
        public string? Breed { get; set; }

        [MaxLength(30)]
        public string? Age { get; set; }

        [MaxLength(30)]
        public string? Gender { get; set; }

        [MaxLength(30)]
        public string? Sex { get; set; }

        [MaxLength(150)]
        public string? Location { get; set; }

        [MaxLength(30)]
        public string? Status { get; set; }

        [MaxLength(2000)]
        public string? Description { get; set; }

        public double? Latitud { get; set; }

        public double? Longitud { get; set; }

        /// <summary>
        /// Identificador de Centro opcional (solo asignable explícitamente por Administrador; para Centros se infiere del JWT).
        /// </summary>
        public int? CentroId { get; set; }

        /// <summary>
        /// Archivo de fotografía opcional enviado en el formulario multipart.
        /// </summary>
        public IFormFile? Imagen { get; set; }

        public string ObtenerGenero()
        {
            if (!string.IsNullOrWhiteSpace(Gender))
            {
                return Gender.Trim();
            }
            if (!string.IsNullOrWhiteSpace(Sex))
            {
                return Sex.Trim();
            }
            return string.Empty;
        }
    }
}
