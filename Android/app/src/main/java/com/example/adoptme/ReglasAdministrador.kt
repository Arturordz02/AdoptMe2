package com.example.adoptme

import com.example.adoptme.model.Pet

/**
 * Localiza la mascota correspondiente a una solicitud de adopción.
 * Reglas:
 * - Si petId > 0, busca únicamente por ID.
 * - Si petId es 0, busca por nombre como respaldo.
 * - No usa el nombre como criterio principal cuando exista un petId válido.
 */
fun localizarMascotaParaAprobacion(
    mascotas: List<Pet>,
    petId: Int,
    nombreMascota: String
): Pet? {
    return if (petId > 0) {
        mascotas.find { it.id == petId }
    } else {
        mascotas.find { it.name.equals(nombreMascota, ignoreCase = true) }
    }
}

/**
 * Evalúa si una solicitud de adopción puede ser aprobada para la mascota indicada.
 * Reglas:
 * - Mascota inexistente: bloquea con mensaje explicativo.
 * - Mascota en estado "En proceso": bloquea aprobación.
 * - Mascota en estado "Adoptada" o "Adoptado": bloquea aprobación.
 * - Mascota en cualquier estado diferente de "Disponible": bloquea aprobación.
 * - Mascota en estado "Disponible": permite aprobación.
 */
fun puedeAprobarSolicitud(
    mascota: Pet?
): Pair<Boolean, String> {
    if (mascota == null) {
        return Pair(false, "No se encontró la mascota asociada a la solicitud.")
    }
    val estado = mascota.status.trim()
    if (estado.equals("En proceso", ignoreCase = true)) {
        return Pair(false, "No se puede aprobar la solicitud: ${mascota.name} ya se encuentra 'En proceso'.")
    }
    if (estado.equals("Adoptada", ignoreCase = true) || estado.equals("Adoptado", ignoreCase = true)) {
        return Pair(false, "No se puede aprobar la solicitud: ${mascota.name} ya fue adoptada.")
    }
    if (!estado.equals("Disponible", ignoreCase = true)) {
        return Pair(false, "No se puede aprobar la solicitud: ${mascota.name} se encuentra en estado '$estado'. Solo se pueden aprobar mascotas en estado 'Disponible'.")
    }
    return Pair(true, "Aprobación permitida.")
}

