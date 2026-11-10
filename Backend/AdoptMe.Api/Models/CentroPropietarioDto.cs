namespace AdoptMe.Api.Models
{
    /// <summary>
    /// Perfil y estado institucional accesible exclusivamente por el Centro autenticado propietario.
    /// </summary>
    public class CentroPropietarioDto
    {
        public int Id { get; set; }
        public int? UsuarioId { get; set; }
        public string Nombre { get; set; } = string.Empty;
        public string Descripcion { get; set; } = string.Empty;
        public string Direccion { get; set; } = string.Empty;
        public string Distrito { get; set; } = string.Empty;
        public string Telefono { get; set; } = string.Empty;
        public string CorreoContacto { get; set; } = string.Empty;
        public string Horario { get; set; } = string.Empty;
        public double? Latitud { get; set; }
        public double? Longitud { get; set; }
        public string? LogoUrl { get; set; }
        public string Estado { get; set; } = "Pendiente";
        public string? MotivoRechazo { get; set; }
        public DateTime FechaSolicitud { get; set; }
        public DateTime? FechaRevision { get; set; }
        public bool EsDemostrativo { get; set; }
        public bool Activo { get; set; }
        public DateTime? FechaActualizacion { get; set; }
    }
}
