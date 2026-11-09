namespace AdoptMe.Api.Configuration
{
    /// <summary>
    /// Configuración fuertemente tipada para el servicio de almacenamiento Cloudinary.
    /// Las credenciales se leen desde variables de entorno del servidor o User Secrets local.
    /// Nunca deben versionarse ni exponerse en el cliente móvil.
    /// </summary>
    public class CloudinarySettings
    {
        public string CloudName { get; set; } = string.Empty;
        public string ApiKey { get; set; } = string.Empty;
        public string ApiSecret { get; set; } = string.Empty;

        /// <summary>
        /// Carpeta raíz en Cloudinary para las imágenes de mascotas (por defecto adoptme/mascotas).
        /// </summary>
        public string Folder { get; set; } = "adoptme/mascotas";

        /// <summary>
        /// Indica si todas las credenciales requeridas están presentes y no vacías.
        /// </summary>
        public bool IsConfigured =>
            !string.IsNullOrWhiteSpace(CloudName) &&
            !string.IsNullOrWhiteSpace(ApiKey) &&
            !string.IsNullOrWhiteSpace(ApiSecret);
    }
}
