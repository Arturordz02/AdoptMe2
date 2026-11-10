namespace AdoptMe.Api.Models
{
    /// <summary>
    /// Datos públicos institucionales de un Centro de Adopción aprobado y activo.
    /// Nunca expone datos personales del responsable, contraseñas, UsuarioId ni LogoPublicId.
    /// </summary>
    public class CentroPublicoDto
    {
        public int Id { get; set; }
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
        public bool EsDemostrativo { get; set; }
    }
}
