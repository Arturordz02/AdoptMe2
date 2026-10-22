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

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            modelBuilder.Entity<Usuario>()
                .HasIndex(u => u.Correo)
                .IsUnique();
        }
    }
}