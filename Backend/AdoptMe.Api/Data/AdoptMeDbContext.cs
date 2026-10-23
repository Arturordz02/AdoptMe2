using AdoptMe.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace AdoptMe.Api.Data
{
    public class AdoptMeDbContext : DbContext
    {
        public AdoptMeDbContext(
            DbContextOptions<AdoptMeDbContext> options)
            : base(options)
        {
        }

        public DbSet<Mascota> Mascotas { get; set; }

        public DbSet<Usuario> Usuarios { get; set; }

        public DbSet<SolicitudAdopcion> Solicitudes { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            modelBuilder.Entity<Usuario>()
                .HasIndex(u => u.Correo)
                .IsUnique();

            modelBuilder.Entity<SolicitudAdopcion>(entity =>
            {
                entity.ToTable("Solicitudes");

                entity.HasKey(s => s.Id);

                // Relación con Mascota: política restrictiva para preservar histórico (NO CASCADE)
                entity.HasOne(s => s.Mascota)
                      .WithMany()
                      .HasForeignKey(s => s.MascotaId)
                      .OnDelete(DeleteBehavior.Restrict);

                // Relación con Usuario adoptante: política restrictiva para preservar histórico (NO CASCADE)
                entity.HasOne(s => s.Adoptante)
                      .WithMany()
                      .HasForeignKey(s => s.AdoptanteId)
                      .OnDelete(DeleteBehavior.Restrict);

                entity.HasIndex(s => s.MascotaId);
                entity.HasIndex(s => s.AdoptanteId);
                entity.HasIndex(s => s.Status);
            });
        }
    }
}
