using System.ComponentModel.DataAnnotations;

namespace AdoptMe.Api.Models
{
    /// <summary>
    /// DTO para el registro transaccional de un nuevo Centro de Adopción y su usuario responsable.
    /// </summary>
    public class RegistroCentroDto
    {
        // --- Datos de la Persona Responsable ---
        [Required(ErrorMessage = "El nombre del responsable es obligatorio.")]
        [MaxLength(100, ErrorMessage = "El nombre del responsable no puede exceder 100 caracteres.")]
        public string NombreResponsable { get; set; } = string.Empty;

        [Required(ErrorMessage = "El correo de acceso es obligatorio.")]
        [EmailAddress(ErrorMessage = "El formato de correo no es válido.")]
        [MaxLength(150)]
        public string Correo { get; set; } = string.Empty;

        [Required(ErrorMessage = "La contraseña es obligatoria.")]
        [MinLength(6, ErrorMessage = "La contraseña debe tener al menos 6 caracteres.")]
        public string Contrasena { get; set; } = string.Empty;

        [MaxLength(20)]
        public string? TelefonoResponsable { get; set; }

        // --- Datos Institucionales del Centro ---
        [Required(ErrorMessage = "El nombre del centro es obligatorio.")]
        [MaxLength(150, ErrorMessage = "El nombre del centro no puede exceder 150 caracteres.")]
        public string NombreCentro { get; set; } = string.Empty;

        [Required(ErrorMessage = "La descripción del centro es obligatoria.")]
        [MaxLength(1000, ErrorMessage = "La descripción no puede exceder 1000 caracteres.")]
        public string Descripcion { get; set; } = string.Empty;

        [Required(ErrorMessage = "La dirección física es obligatoria.")]
        [MaxLength(250, ErrorMessage = "La dirección no puede exceder 250 caracteres.")]
        public string Direccion { get; set; } = string.Empty;

        [Required(ErrorMessage = "El distrito es obligatorio.")]
        [MaxLength(100, ErrorMessage = "El distrito no puede exceder 100 caracteres.")]
        public string Distrito { get; set; } = string.Empty;

        [Required(ErrorMessage = "El teléfono de contacto del centro es obligatorio.")]
        [MaxLength(20, ErrorMessage = "El teléfono no puede exceder 20 caracteres.")]
        public string TelefonoCentro { get; set; } = string.Empty;

        [Required(ErrorMessage = "El correo de contacto público es obligatorio.")]
        [EmailAddress(ErrorMessage = "El correo de contacto no tiene un formato válido.")]
        [MaxLength(150)]
        public string CorreoContacto { get; set; } = string.Empty;

        [Required(ErrorMessage = "El horario de atención es obligatorio.")]
        [MaxLength(150, ErrorMessage = "El horario no puede exceder 150 caracteres.")]
        public string Horario { get; set; } = string.Empty;

        // --- Coordenadas Geográficas (Opcionales pero consistentes por pares) ---
        [Range(-90.0, 90.0, ErrorMessage = "La latitud debe estar comprendida entre -90 y 90 grados.")]
        public double? Latitud { get; set; }

        [Range(-180.0, 180.0, ErrorMessage = "La longitud debe estar comprendida entre -180 y 180 grados.")]
        public double? Longitud { get; set; }

        // --- Términos y Condiciones ---
        [Range(typeof(bool), "true", "true", ErrorMessage = "Debe aceptar los términos y condiciones del servicio.")]
        public bool AceptacionTerminos { get; set; }
    }
}
