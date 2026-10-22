# =============================================================================
# DOCKERFILE MULTIETAPA: ASP.NET Core 8 Web API
# Contexto de compilación: Backend/AdoptMe.Api
# =============================================================================

# Etapa 1: Compilación y Publicación
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# Copiar el archivo del proyecto y restaurar dependencias
COPY ["AdoptMe.Api.csproj", "./"]
RUN dotnet restore "./AdoptMe.Api.csproj"

# Copiar el resto del código y compilar en Release
COPY . .
RUN dotnet publish "AdoptMe.Api.csproj" -c Release -o /app/publish /p:UseAppHost=false

# Etapa 2: Entorno de Ejecución Final (ASP.NET Core 8 Runtime)
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app

# Puerto 8080 estándar para contenedores no-root en .NET 8 / Render
EXPOSE 8080
ENV ASPNETCORE_HTTP_PORTS=8080

COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "AdoptMe.Api.dll"]

