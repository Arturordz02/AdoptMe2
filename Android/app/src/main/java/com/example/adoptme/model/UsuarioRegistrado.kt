package com.example.adoptme.model

data class UsuarioRegistrado(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val telefono: String = "",
    val rol: String = "Adoptante"
)
