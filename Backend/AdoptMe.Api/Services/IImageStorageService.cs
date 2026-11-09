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
        /// Sube un flujo binario de imagen al almacenamiento persistente.
        /// </summary>
        /// <param name="stream">Flujo con los bytes de la imagen previamente validados.</param>
        /// <param name="nombreArchivo">Nombre base sugerido o identificador para el recurso.</param>
        /// <param name="cancellationToken">Token de cancelación opcional.</param>
        /// <returns>Resultado con la URL segura HTTPS y el PublicId del recurso remoto.</returns>
        Task<ImageUploadResult> SubirImagenAsync(Stream stream, string nombreArchivo, CancellationToken cancellationToken = default);

        /// <summary>
        /// Elimina un recurso del almacenamiento persistente utilizando su identificador público (PublicId).
        /// </summary>
        /// <param name="publicId">Identificador del recurso en Cloudinary.</param>
        /// <param name="cancellationToken">Token de cancelación opcional.</param>
        /// <returns>Verdadero si la eliminación fue confirmada o el recurso ya no existía.</returns>
        Task<bool> EliminarImagenAsync(string publicId, CancellationToken cancellationToken = default);
    }
}
