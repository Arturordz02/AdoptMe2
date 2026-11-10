namespace AdoptMe.Api.Models
{
    public class EstadoCentroDto
    {
        public int? CentroId { get; set; }
        public string? Estado { get; set; }
        public bool Activo { get; set; }
        public bool EsDemostrativo { get; set; }
        public string? MotivoRechazo { get; set; }
    }
}
