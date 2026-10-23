using System;

namespace AdoptMe.Api.Models
{
    public class SolicitudResponseDto
    {
        public int Id { get; set; }

        public int MascotaId { get; set; }

        public int AdoptanteId { get; set; }

        public string PetName { get; set; } = string.Empty;

        public string PetSpecies { get; set; } = string.Empty;

        public string ApplicantName { get; set; } = string.Empty;

        public string Phone { get; set; } = string.Empty;

        public string Email { get; set; } = string.Empty;

        public string Address { get; set; } = string.Empty;

        public string HousingType { get; set; } = string.Empty;

        public string TimeAvailability { get; set; } = string.Empty;

        public string PreviousExperience { get; set; } = string.Empty;

        public string Reason { get; set; } = string.Empty;

        public string Status { get; set; } = "Pendiente";

        public string? Observacion { get; set; }

        public DateTime FechaSolicitud { get; set; }

        public DateTime? FechaActualizacion { get; set; }
    }
}

