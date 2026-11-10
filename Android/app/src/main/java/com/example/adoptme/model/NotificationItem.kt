package com.example.adoptme.model

import java.util.UUID

/**
 * Modelo para notificaciones y avisos del sistema en AdoptMe.
 * Compatible con versiones previas (mantiene title y message como primeros campos).
 * Incorpora id único, marca de tiempo, estado de lectura y tipo.
 */
data class NotificationItem(
    val title: String,
    val message: String,
    val id: String = UUID.randomUUID().toString(),
    val correoPropietario: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fecha: String = "",
    val leida: Boolean = false,
    val tipo: String = "SISTEMA"
)
