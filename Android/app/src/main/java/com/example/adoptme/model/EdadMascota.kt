package com.example.adoptme.model

/**
 * Modelo de datos estructurado para representar y manipular la edad de una mascota.
 * Soporta años (0..40), meses (0..11) y la opción especial "Menos de 1 mes" para recién nacidos.
 */
data class EdadMascota(
    val anios: Int = 0,
    val meses: Int = 0,
    val menosDeUnMes: Boolean = false
)

/**
 * Valida un objeto [EdadMascota].
 * Retorna null si la edad es válida, o un mensaje explicativo si infringe los límites.
 */
fun validarEdadMascota(edad: EdadMascota): String? {
    if (edad.menosDeUnMes) {
        return null
    }
    if (edad.anios < 0) {
        return "La edad en años no puede ser negativa."
    }
    if (edad.anios > 40) {
        return "La edad en años no puede exceder 40."
    }
    if (edad.meses < 0) {
        return "Los meses no pueden ser negativos."
    }
    if (edad.meses > 11) {
        return "Los meses deben estar entre 0 y 11."
    }
    if (edad.anios == 0 && edad.meses == 0) {
        return "Debes indicar los años o meses de la mascota, o marcar 'Menos de 1 mes'."
    }
    return null
}

/**
 * Genera el texto canónico normalizado para persistencia o visualización.
 * Casos:
 * - Menos de 1 mes -> "Menos de 1 mes"
 * - 1 año y 0 meses -> "1 año"
 * - N años y 0 meses -> "$N años"
 * - 0 años y 1 mes -> "1 mes"
 * - 0 años y M meses -> "$M meses"
 * - 1 año y 1 mes -> "1 año y 1 mes"
 * - 1 año y M meses -> "1 año y $M meses"
 * - N años y 1 mes -> "$N años y 1 mes"
 * - N años y M meses -> "$N años y $M meses"
 */
fun formatearEdadMascota(edad: EdadMascota): String {
    if (edad.menosDeUnMes || (edad.anios == 0 && edad.meses == 0)) {
        return "Menos de 1 mes"
    }

    val parteAnios = when (edad.anios) {
        0 -> null
        1 -> "1 año"
        else -> "${edad.anios} años"
    }

    val parteMeses = when (edad.meses) {
        0 -> null
        1 -> "1 mes"
        else -> "${edad.meses} meses"
    }

    return when {
        parteAnios != null && parteMeses != null -> "$parteAnios y $parteMeses"
        parteAnios != null -> parteAnios
        parteMeses != null -> parteMeses
        else -> "Menos de 1 mes"
    }
}

/**
 * Interpreta una cadena de texto (tanto histórica como de nuevo formato compuesto)
 * y la convierte a un objeto [EdadMascota]. Retorna null si el formato no es reconocido.
 */
fun interpretarEdadMascota(texto: String?): EdadMascota? {
    if (texto.isNullOrBlank()) return null

    val limpia = texto.trim().lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')

    if (limpia == "menos de 1 mes" || limpia == "menos de un mes" || limpia == "recien nacido") {
        return EdadMascota(anios = 0, meses = 0, menosDeUnMes = true)
    }

    // Formato compuesto: ej: "2 anos y 6 meses", "1 ano y 1 mes", "2 anios y 3 meses"
    val regexCompuesto = Regex("""^([0-9]\d?)\s*(?:ano|anos|anio|anios|año|años)\s+y\s+([0-9]\d?)\s*(?:mes|meses)$""")
    val matchCompuesto = regexCompuesto.find(limpia)
    if (matchCompuesto != null) {
        val a = matchCompuesto.groupValues[1].toIntOrNull() ?: 0
        val m = matchCompuesto.groupValues[2].toIntOrNull() ?: 0
        if (a in 0..40 && m in 0..11) {
            return if (a == 0 && m == 0) {
                EdadMascota(0, 0, menosDeUnMes = true)
            } else {
                EdadMascota(anios = a, meses = m, menosDeUnMes = false)
            }
        }
    }

    // Formato simple de años: ej: "1 ano", "2 anos", "1 anio", "2 anios", "1 año", "2 años"
    val regexAnios = Regex("""^([1-9]\d?)\s*(?:ano|anos|anio|anios|año|años)$""")
    val matchAnios = regexAnios.find(limpia)
    if (matchAnios != null) {
        val a = matchAnios.groupValues[1].toIntOrNull() ?: 0
        if (a in 1..40) {
            return EdadMascota(anios = a, meses = 0, menosDeUnMes = false)
        }
    }

    // Formato simple de meses: ej: "1 mes", "6 meses", "11 meses"
    val regexMeses = Regex("""^([1-9]\d?)\s*(?:mes|meses)$""")
    val matchMeses = regexMeses.find(limpia)
    if (matchMeses != null) {
        val m = matchMeses.groupValues[1].toIntOrNull() ?: 0
        if (m in 1..11) {
            return EdadMascota(anios = 0, meses = m, menosDeUnMes = false)
        }
        if (m >= 12) {
            val a = m / 12
            val resto = m % 12
            if (a in 1..40) {
                return EdadMascota(anios = a, meses = resto, menosDeUnMes = false)
            }
        }
    }

    return null
}
