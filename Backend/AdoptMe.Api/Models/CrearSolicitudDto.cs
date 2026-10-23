using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace AdoptMe.Api.Models
{
    public class CrearSolicitudDto
    {
        private int _mascotaId;

        [JsonPropertyName("mascotaId")]
        public int MascotaId
        {
            get => _mascotaId;
            set => _mascotaId = value;
        }

        // Permite compatibilidad si el cliente envía petId
        [JsonPropertyName("petId")]
        public int? PetId
        {
            get => _mascotaId;
            set
            {
                if (value.HasValue && value.Value > 0)
                {
                    _mascotaId = value.Value;
                }
            }
        }

        [Required(ErrorMessage = "El nombre del solicitante es obligatorio.")]
        [MaxLength(150, ErrorMessage = "El nombre no puede superar los 150 caracteres.")]
        public string ApplicantName { get; set; } = string.Empty;

        [Required(ErrorMessage = "El teléfono es obligatorio.")]
        [MaxLength(20, ErrorMessage = "El teléfono no puede superar los 20 caracteres.")]
        public string Phone { get; set; } = string.Empty;

        [Required(ErrorMessage = "La dirección es obligatoria.")]
        [MaxLength(250, ErrorMessage = "La dirección no puede superar los 250 caracteres.")]
        public string Address { get; set; } = string.Empty;

        [Required(ErrorMessage = "El tipo de vivienda es obligatorio.")]
        [MaxLength(100, ErrorMessage = "El tipo de vivienda no puede superar los 100 caracteres.")]
        public string HousingType { get; set; } = string.Empty;

        [MaxLength(100, ErrorMessage = "La disponibilidad de tiempo no puede superar los 100 caracteres.")]
        public string? TimeAvailability { get; set; }

        [MaxLength(150, ErrorMessage = "La experiencia previa no puede superar los 150 caracteres.")]
        public string? PreviousExperience { get; set; }

        [Required(ErrorMessage = "El motivo de adopción es obligatorio.")]
        [MaxLength(2000, ErrorMessage = "El motivo no puede superar los 2000 caracteres.")]
        public string Reason { get; set; } = string.Empty;
    }
}

