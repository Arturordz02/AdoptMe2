namespace AdoptMe.Api.Services
{
    /// <summary>
    /// Contrato para el almacenamiento persistente de imágenes en la nube (Cloudinary).
    /// </summary>
    public interface IImageStorageService
    {
        /// <summary>
        /// Indica si el servicio cuenta con las credenciales requeridas para operar.
        /// </summary>
        bool IsConfigured { get; }

        /// <summary>
        /// Sube un flujo binario de imagen al almacenamiento persistente en la carpeta por defecto (adoptme/mascotas).
        /// </summary>
        Task<ImageUploadResult> SubirImagenAsync(Stream stream, string nombreArchivo, CancellationToken cancellationToken = default);

        /// <summary>
        /// Sube un flujo binario de imagen a una subcarpeta específica controlada por el servidor (ej. adoptme/centros).
        /// </summary>
        Task<ImageUploadResult> SubirImagenAsync(Stream stream, string nombreArchivo, string? subcarpeta, CancellationToken cancellationToken = default);

        /// <summary>
        /// Elimina un recurso del almacenamiento persistente utilizando su identificador público (PublicId).
        /// </summary>
        Task<bool> EliminarImagenAsync(string publicId, CancellationToken cancellationToken = default);
    }
}
