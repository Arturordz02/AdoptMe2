using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace AdoptMe.Api.Models
{
    /// <summary>
    /// Representa un Centro o Albergue de Adopción en la plataforma AdoptMe.
    /// Admite centros autogestionados por usuarios verificados y centros demostrativos históricos.
    /// </summary>
    public class Centro
    {
        [Key]
        public int Id { get; set; }

        /// <summary>
        /// Identificador del usuario propietario y responsable del centro.
        /// Nullable para admitir centros demostrativos históricos sin cuentas ficticias.
        /// Un usuario real solo puede administrar un único centro (índice único parcial).
        /// </summary>
        public int? UsuarioId { get; set; }

        [ForeignKey(nameof(UsuarioId))]
        public virtual Usuario? Usuario { get; set; }

        [Required]
        [MaxLength(150)]
        public string Nombre { get; set; } = string.Empty;

        [Required]
        [MaxLength(1000)]
        public string Descripcion { get; set; } = string.Empty;

        [Required]
        [MaxLength(250)]
        public string Direccion { get; set; } = string.Empty;

        [Required]
        [MaxLength(100)]
        public string Distrito { get; set; } = string.Empty;

        [Required]
        [MaxLength(20)]
        public string Telefono { get; set; } = string.Empty;

        [Required]
        [MaxLength(150)]
        public string CorreoContacto { get; set; } = string.Empty;

        [Required]
        [MaxLength(150)]
        public string Horario { get; set; } = string.Empty;

        /// <summary>
        /// Coordenada GPS de latitud (-90 a 90).
        /// Obligatoriamente consistente por pares con Longitud.
        /// </summary>
        public double? Latitud { get; set; }

        /// <summary>
        /// Coordenada GPS de longitud (-180 a 180).
        /// Obligatoriamente consistente por pares con Latitud.
        /// </summary>
        public double? Longitud { get; set; }

        /// <summary>
        /// URL pública segura HTTPS del logotipo en CDN (Cloudinary).
        /// </summary>
        public string? LogoUrl { get; set; }

        /// <summary>
        /// Identificador interno del activo en Cloudinary (adoptme/centros/...).
        /// Dato privado, nunca expuesto en endpoints públicos.
        /// </summary>
        public string? LogoPublicId { get; set; }

        /// <summary>
        /// Estado administrativo de la solicitud del centro: 'Pendiente', 'Aprobado', 'Rechazado'.
        /// Solo los centros 'Aprobado' y 'Activo' son visibles públicamente y pueden publicar mascotas.
        /// </summary>
        [Required]
        [MaxLength(30)]
        public string Estado { get; set; } = "Pendiente";

        /// <summary>
        /// Justificación obligatoria ingresada por el Administrador en caso de rechazo.
        /// </summary>
        [MaxLength(500)]
        public string? MotivoRechazo { get; set; }

        public DateTime FechaSolicitud { get; set; } = DateTime.UtcNow;

        public DateTime? FechaRevision { get; set; }

        /// <summary>
        /// Identificador del Administrador que revisó y aprobó o rechazó la solicitud.
        /// </summary>
        public int? RevisadoPorUsuarioId { get; set; }

        [ForeignKey(nameof(RevisadoPorUsuarioId))]
        public virtual Usuario? RevisadoPorUsuario { get; set; }

        /// <summary>
        /// Indica si es uno de los centros precargados con propósitos pedagógicos/demostrativos.
        /// </summary>
        public bool EsDemostrativo { get; set; } = false;

        /// <summary>
        /// Permite la suspensión temporal o desactivación lógica del centro sin eliminar su histórico.
        /// </summary>
        public bool Activo { get; set; } = true;

        public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;

        public DateTime? FechaActualizacion { get; set; }

        /// <summary>
        /// Colección de mascotas publicadas formalmente bajo la tutela de este centro.
        /// </summary>
        public virtual ICollection<Mascota> Mascotas { get; set; } = new List<Mascota>();
    }
}
