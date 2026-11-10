using AdoptMe.Api.Configuration;
using CloudinaryDotNet;
using CloudinaryDotNet.Actions;
using Microsoft.Extensions.Options;

namespace AdoptMe.Api.Services
{
    /// <summary>
    /// Implementación oficial del almacenamiento persistente de imágenes mediante el SDK de Cloudinary.
    /// Opera de forma desacoplada y defensiva: si las credenciales no están configuradas,
    /// no provoca cierres del servidor y devuelve errores controlados al invocar la subida.
    /// </summary>
    public class CloudinaryImageStorageService : IImageStorageService
    {
        private readonly CloudinarySettings _settings;
        private readonly Cloudinary? _cloudinary;
        private readonly ILogger<CloudinaryImageStorageService> _logger;

        public bool IsConfigured => _settings.IsConfigured && _cloudinary != null;

        public CloudinaryImageStorageService(
            IOptions<CloudinarySettings> options,
            ILogger<CloudinaryImageStorageService> logger)
        {
            _settings = options.Value ?? new CloudinarySettings();
            _logger = logger;

            if (_settings.IsConfigured)
            {
                var account = new Account(
                    _settings.CloudName,
                    _settings.ApiKey,
                    _settings.ApiSecret
                );
                _cloudinary = new Cloudinary(account)
                {
                    Api = { Secure = true }
                };
                _logger.LogInformation("CloudinaryImageStorageService inicializado correctamente para el cloud '{CloudName}'.", _settings.CloudName);
            }
            else
            {
                _cloudinary = null;
                _logger.LogWarning("CloudinaryImageStorageService: Credenciales de Cloudinary no configuradas. La subida de imágenes devolverá error controlado.");
            }
        }

        public Task<ImageUploadResult> SubirImagenAsync(Stream stream, string nombreArchivo, CancellationToken cancellationToken = default)
        {
            return SubirImagenAsync(stream, nombreArchivo, null, cancellationToken);
        }

        public async Task<ImageUploadResult> SubirImagenAsync(Stream stream, string nombreArchivo, string? subcarpeta, CancellationToken cancellationToken = default)
        {
            if (!IsConfigured || _cloudinary == null)
            {
                return ImageUploadResult.Fallo("El servicio de almacenamiento de imágenes en la nube (Cloudinary) no está configurado en el servidor.");
            }

            try
            {
                if (stream.CanSeek)
                {
                    stream.Position = 0;
                }

                string carpetaDestino;
                if (!string.IsNullOrWhiteSpace(subcarpeta))
                {
                    carpetaDestino = subcarpeta.Trim();
                }
                else
                {
                    carpetaDestino = string.IsNullOrWhiteSpace(_settings.Folder) ? "adoptme/mascotas" : _settings.Folder.Trim();
                }

                var uploadParams = new ImageUploadParams
                {
                    File = new FileDescription(nombreArchivo, stream),
                    Folder = carpetaDestino,
                    UseFilename = false,
                    UniqueFilename = true,
                    Overwrite = false
                };

                var respuesta = await _cloudinary.UploadAsync(uploadParams, cancellationToken);

                if (respuesta == null)
                {
                    return ImageUploadResult.Fallo("No se recibió respuesta del servidor de almacenamiento de imágenes.");
                }

                if (respuesta.Error != null)
                {
                    _logger.LogError("Error devuelto por Cloudinary al subir imagen: {Mensaje}", respuesta.Error.Message);
                    return ImageUploadResult.Fallo($"Error de almacenamiento remoto: {respuesta.Error.Message}");
                }

                var secureUrl = respuesta.SecureUrl?.ToString() ?? respuesta.Url?.ToString();
                if (string.IsNullOrWhiteSpace(secureUrl) || string.IsNullOrWhiteSpace(respuesta.PublicId))
                {
                    return ImageUploadResult.Fallo("El servidor de almacenamiento no proporcionó una URL segura o identificador válido.");
                }

                _logger.LogInformation("Imagen subida exitosamente a Cloudinary: PublicId={PublicId}, Url={Url}", respuesta.PublicId, secureUrl);
                return ImageUploadResult.Exito(secureUrl, respuesta.PublicId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Excepción no controlada durante la subida de imagen a Cloudinary.");
                return ImageUploadResult.Fallo($"Excepción durante la subida de imagen: {ex.Message}");
            }
        }

        public async Task<bool> EliminarImagenAsync(string publicId, CancellationToken cancellationToken = default)
        {
            if (!IsConfigured || _cloudinary == null)
            {
                _logger.LogWarning("Intento de eliminar imagen remota '{PublicId}', pero Cloudinary no está configurado.", publicId);
                return false;
            }

            if (string.IsNullOrWhiteSpace(publicId))
            {
                return true;
            }

            try
            {
                var deletionParams = new DeletionParams(publicId);
                var respuesta = await _cloudinary.DestroyAsync(deletionParams);

                if (respuesta.Result == "ok" || respuesta.Result == "not found")
                {
                    _logger.LogInformation("Imagen remota '{PublicId}' eliminada o no encontrada en Cloudinary (Resultado: {Resultado}).", publicId, respuesta.Result);
                    return true;
                }

                _logger.LogWarning("Respuesta inesperada al eliminar imagen '{PublicId}' en Cloudinary: {Resultado}", publicId, respuesta.Result);
                return false;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error al intentar eliminar la imagen remota '{PublicId}' en Cloudinary.", publicId);
                return false;
            }
        }
    }
}
