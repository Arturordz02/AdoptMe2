package com.example.adoptme.utils

import com.example.adoptme.model.formatearEdadMascota
import com.example.adoptme.model.interpretarEdadMascota

/**
 * Normaliza la representación textual de la edad a su formato canónico en español ("año", "años", "mes", "meses").
 * Soporta formatos simples históricos ("1 ano", "2 anos", "6 meses"), compuestos ("2 años y 6 meses")
 * y la opción "Menos de 1 mes".
 */
fun formatearEdadCanonica(edad: String): String {
    val interpretada = interpretarEdadMascota(edad)
    return if (interpretada != null) {
        formatearEdadMascota(interpretada)
    } else {
        edad.trim()
    }
}
