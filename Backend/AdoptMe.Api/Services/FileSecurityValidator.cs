namespace AdoptMe.Api.Services
{
    public class FileValidationResult
    {
        public bool EsValido { get; set; }
        public string? MensajeError { get; set; }
        public string? ExtensionNormalizada { get; set; }
        public string? NombreGenerado { get; set; }

        public static FileValidationResult Valido(string extension, string nombreGenerado)
        {
            return new FileValidationResult
            {
                EsValido = true,
                ExtensionNormalizada = extension,
                NombreGenerado = nombreGenerado
            };
        }

        public static FileValidationResult Invalido(string mensaje)
        {
            return new FileValidationResult
            {
                EsValido = false,
                MensajeError = mensaje
            };
        }
    }

    /// <summary>
    /// Validador riguroso de seguridad para archivos multimedia.
    /// Inspecciona longitudes, tipos MIME, extensiones, cabeceras binarias (magic numbers)
    /// y rechaza contenido ejecutable, SVG o rutas maliciosas.
    /// </summary>
    public static class FileSecurityValidator
    {
        public const long TamanoMaximoBytes = 2 * 1024 * 1024; // 2 MB

        private static readonly HashSet<string> MimesPermitidos = new(StringComparer.OrdinalIgnoreCase)
        {
            "image/jpeg",
            "image/pjpeg",
            "image/png",
            "image/webp"
        };

        private static readonly HashSet<string> ExtensionesPermitidas = new(StringComparer.OrdinalIgnoreCase)
        {
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
        };

        public static async Task<FileValidationResult> ValidarImagenAsync(IFormFile? archivo, CancellationToken cancellationToken = default)
        {
            if (archivo == null || archivo.Length == 0)
            {
                return FileValidationResult.Invalido("El archivo de imagen no fue proporcionado o se encuentra vacío.");
            }

            if (archivo.Length > TamanoMaximoBytes)
            {
                return FileValidationResult.Invalido($"El archivo supera el tamaño máximo permitido de 2 MB (tamaño recibido: {archivo.Length / 1024.0 / 1024.0:F2} MB).");
            }

            // Validar Content-Type declarado
            var contentType = archivo.ContentType?.Trim().ToLowerInvariant() ?? string.Empty;
            if (!MimesPermitidos.Contains(contentType))
            {
                return FileValidationResult.Invalido($"Tipo de contenido no admitido: '{contentType}'. Solo se permiten imágenes JPEG, PNG y WebP.");
            }

            // Validar extensión del nombre enviado
            var extension = Path.GetExtension(archivo.FileName)?.Trim().ToLowerInvariant() ?? string.Empty;
            if (string.IsNullOrWhiteSpace(extension) || !ExtensionesPermitidas.Contains(extension))
            {
                return FileValidationResult.Invalido($"Extensión de archivo no permitida: '{extension}'. Formatos válidos: .jpg, .jpeg, .png, .webp.");
            }

            // Normalizar extensión
            if (extension == ".jpeg")
            {
                extension = ".jpg";
            }

            // Inspección de cabecera binaria (Magic Numbers)
            byte[] header = new byte[12];
            using (var stream = archivo.OpenReadStream())
            {
                int bytesLeidos = await stream.ReadAsync(header.AsMemory(0, header.Length), cancellationToken);
                if (bytesLeidos < 12)
                {
                    return FileValidationResult.Invalido("El archivo no contiene suficientes datos para verificar su firma binaria.");
                }
            }

            bool firmaValida = false;

            // JPEG: FF D8 FF
            if (header[0] == 0xFF && header[1] == 0xD8 && header[2] == 0xFF)
            {
                if (extension == ".jpg" || contentType.Contains("jpeg"))
                {
                    firmaValida = true;
                    extension = ".jpg";
                }
            }
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            else if (header[0] == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47 &&
                     header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A)
            {
                if (extension == ".png" || contentType.Contains("png"))
                {
                    firmaValida = true;
                    extension = ".png";
                }
            }
            // WEBP: RIFF (bytes 0..3: 52 49 46 46) y WEBP (bytes 8..11: 57 45 42 50)
            else if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 &&
                     header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50)
            {
                if (extension == ".webp" || contentType.Contains("webp"))
                {
                    firmaValida = true;
                    extension = ".webp";
                }
            }

            if (!firmaValida)
            {
                return FileValidationResult.Invalido("La firma binaria del archivo no coincide con ningún formato de imagen permitido (JPEG, PNG, WebP). Posible archivo corrupto o renombrado maliciosamente.");
            }

            // Generación de nombre seguro e inmutable en el servidor (Guid)
            var nombreServidor = $"{Guid.NewGuid():N}{extension}";

            return FileValidationResult.Valido(extension, nombreServidor);
        }
    }
}
