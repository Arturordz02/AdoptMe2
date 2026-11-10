namespace AdoptMe.Api.Models
{
    /// <summary>
    /// Información integral y administrativa de un Centro de Adopción accesible exclusivamente por Administradores.
    /// Incluye datos de auditoría interna y del usuario responsable, sin contraseñas.
    /// </summary>
    public class CentroAdminDto : CentroPropietarioDto
    {
        public string? NombreResponsable { get; set; }
        public string? CorreoResponsable { get; set; }
        public string? TelefonoResponsable { get; set; }
        public int? RevisadoPorUsuarioId { get; set; }
        public string? RevisadoPorNombre { get; set; }
        public DateTime FechaCreacion { get; set; }
        public int TotalMascotasPublicadas { get; set; }
    }
}
