namespace AdoptMe.Api.Models
{
    public class Usuario
    {
        public int Id { get; set; }

        public string Nombre { get; set; } = "";

        public string Correo { get; set; } = "";

        public string PasswordHash { get; set; } = "";

        public string Telefono { get; set; } = "";

        public string Rol { get; set; } = "Adoptante";

        public DateTime FechaRegistro { get; set; } = DateTime.UtcNow;
    }
}