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

        public DbSet<Centro> Centros { get; set; }

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

            modelBuilder.Entity<Centro>(entity =>
            {
                entity.ToTable("Centros");

                entity.HasKey(c => c.Id);

                // Relación con Usuario propietario: Restrict para evitar borrado accidental
                entity.HasOne(c => c.Usuario)
                      .WithMany()
                      .HasForeignKey(c => c.UsuarioId)
                      .OnDelete(DeleteBehavior.Restrict);

                // Relación con Administrador revisor: SetNull si el admin fuera eliminado
                entity.HasOne(c => c.RevisadoPorUsuario)
                      .WithMany()
                      .HasForeignKey(c => c.RevisadoPorUsuarioId)
                      .OnDelete(DeleteBehavior.SetNull);

                // Relación con Administrador que suspende: SetNull si el admin fuera eliminado
                entity.HasOne(c => c.SuspendidoPorAdmin)
                      .WithMany()
                      .HasForeignKey(c => c.SuspendidoPorAdminId)
                      .OnDelete(DeleteBehavior.SetNull);

                // Índice único parcial para UsuarioId no nulo
                entity.HasIndex(c => c.UsuarioId)
                      .IsUnique()
                      .HasFilter("\"UsuarioId\" IS NOT NULL");

                entity.HasIndex(c => new { c.Estado, c.Activo });
                entity.HasIndex(c => c.Distrito);
                entity.HasIndex(c => c.FechaSolicitud);
            });

            modelBuilder.Entity<Mascota>(entity =>
            {
                // Relación con Centro: SetNull si el centro es desactivado/eliminado
                entity.HasOne(m => m.Centro)
                      .WithMany(c => c.Mascotas)
                      .HasForeignKey(m => m.CentroId)
                      .OnDelete(DeleteBehavior.SetNull);

                entity.HasIndex(m => m.CentroId);
            });
        }
    }
}
