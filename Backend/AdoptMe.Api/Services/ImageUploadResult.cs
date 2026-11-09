namespace AdoptMe.Api.Services
{
    /// <summary>
    /// Resultado de una operación de subida de imagen al servicio de almacenamiento en la nube.
    /// </summary>
    public class ImageUploadResult
    {
        public bool Exitoso { get; set; }
        public string? SecureUrl { get; set; }
        public string? PublicId { get; set; }
        public string? Error { get; set; }

        public static ImageUploadResult Exito(string secureUrl, string publicId)
        {
            return new ImageUploadResult
            {
                Exitoso = true,
                SecureUrl = secureUrl,
                PublicId = publicId
            };
        }

        public static ImageUploadResult Fallo(string error)
        {
            return new ImageUploadResult
            {
                Exitoso = false,
                Error = error
            };
        }
    }
}
