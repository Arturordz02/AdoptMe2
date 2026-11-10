namespace AdoptMe.Api.Models
{
    public class ResultadoRegistroCentroDto
    {
        public string Mensaje { get; set; } = string.Empty;
        public int UsuarioId { get; set; }
        public int CentroId { get; set; }
        public string NombreCentro { get; set; } = string.Empty;
        public string Estado { get; set; } = string.Empty;
        public string Rol { get; set; } = "Centro";
    }
}
