package com.example.adoptme.utils

/**
 * Normaliza la representación textual de la edad a su formato canónico en español ("año", "años", "mes", "meses").
 * Transforma de forma segura variantes como "ano", "anos", "anio", "anios" a "año" / "años".
 */
fun formatearEdadCanonica(edad: String): String {
    val limpia = edad.trim().lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
    val regex = Regex("""^([1-9]\d?)\s*(ano|anos|anio|anios|año|años|mes|meses)$""")
    val match = regex.find(limpia) ?: return edad.trim()
    val cantidad = match.groupValues[1].toIntOrNull() ?: 1
    val unidad = match.groupValues[2]
    return if (unidad.startsWith("m")) {
        if (cantidad == 1) "1 mes" else "$cantidad meses"
    } else {
        if (cantidad == 1) "1 año" else "$cantidad años"
    }
}
