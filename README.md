# AdoptMe

Aplicación móvil Android orientada a facilitar la adopción responsable de mascotas.

## Tecnologías

- Kotlin y Jetpack Compose
- Room, DataStore y SharedPreferences
- Retrofit y OkHttp
- ASP.NET Core 8
- Neon PostgreSQL
- Google Maps
- Cloudinary

## Estructura

- `Android/`: aplicación móvil.
- `Backend/`: API REST y pruebas.
- `docs/`: documentación técnica.
- `Backend/AdoptMe.Api/Data/`: scripts SQL.

## Ejecución

1. Abrir la carpeta `Android` en Android Studio.
2. Configurar `MAPS_API_KEY` en `secrets.properties`.
3. Sincronizar Gradle.
4. Ejecutar la aplicación en un dispositivo o emulador Android.

La aplicación consume por defecto:

`https://adoptme2.onrender.com/`

## Firebase

Firebase no se utiliza. La persistencia se implementa mediante Room, DataStore, SharedPreferences, Neon PostgreSQL y Cloudinary.

## Equipo

Proyecto desarrollado para el curso Desarrollo de Aplicaciones Móviles.
