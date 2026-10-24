using AdoptMe.Api.Models;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace AdoptMe.Api.Data
{
    public static class SeederCuentasDemo
    {
        public static async Task SembrarCuentasAsync(AdoptMeDbContext context, IPasswordHasher<Usuario> passwordHasher)
        {
            var cuentas = new[]
            {
                new { Correo = "admin@adoptme.com", Nombre = "Administrador Demo", Rol = "Admin", Telefono = "999888777" },
                new { Correo = "centro@adoptme.com", Nombre = "Centro Demo", Rol = "Centro", Telefono = "999777666" },
                new { Correo = "usuario@adoptme.com", Nombre = "Adoptante Demo", Rol = "Adoptante", Telefono = "999666555" }
            };

            foreach (var cuenta in cuentas)
            {
                string correoNormalizado = cuenta.Correo.Trim().ToLowerInvariant();
                var usuarioExistente = await context.Usuarios
                    .FirstOrDefaultAsync(u => u.Correo == correoNormalizado);

                if (usuarioExistente == null)
                {
                    var nuevoUsuario = new Usuario
                    {
                        Nombre = cuenta.Nombre,
                        Correo = correoNormalizado,
                        Rol = cuenta.Rol,
                        Telefono = cuenta.Telefono,
                        FechaRegistro = DateTime.UtcNow
                    };

                    nuevoUsuario.PasswordHash = passwordHasher.HashPassword(nuevoUsuario, "123456");
                    context.Usuarios.Add(nuevoUsuario);
                    Console.WriteLine($"[SEED] Creado usuario demo: {cuenta.Correo} con rol {cuenta.Rol}");
                }
                else
                {
                    // Si ya existe la cuenta demo, se omite sin actualizarla ni regenerar su hash
                    Console.WriteLine($"[SEED] Omitido: usuario demo ya existe ({cuenta.Correo})");
                }
            }

            await context.SaveChangesAsync();
        }
    }
}

