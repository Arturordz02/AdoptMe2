package com.example.adoptme.model

/**
 * Modelo para un mensaje de chat entre Adoptante y Centro de Adopción.
 * Permite identificar al remitente, receptor, centro asociado por su ID único,
 * saber si el mensaje proviene del Centro y su estado de lectura.
 */
data class MensajeChat(
    val id: Long = System.currentTimeMillis(),
    val idCentro: Int,
    val remitente: String,
    val receptor: String,
    val texto: String,
    val nombreCentro: String,
    val fecha: String,
    val esDelCentro: Boolean,
    val leido: Boolean = false,
    val fechaLectura: String? = null
)

/**
 * Modelo para resumir una conversación activa en el panel del Centro de Adopción.
 * Incluye contador reactivo de mensajes recibidos no leídos.
 */
data class ConversacionCentro(
    val idCentro: Int,
    val correoAdoptante: String,
    val correoCentro: String,
    val nombreCentro: String,
    val ultimoTexto: String,
    val fecha: String,
    val mensajesNoLeidos: Int = 0
)
