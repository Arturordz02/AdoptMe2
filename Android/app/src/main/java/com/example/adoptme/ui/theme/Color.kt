package com.example.adoptme.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// PALETA DE COLORES ADOPTME (Inspirada en prototipo PC 04)
// =========================================================================

// 1. Naranja principal (header, botón login, logo) y degradados
val AdoptOrange = Color(0xFFFF7A45)
val AdoptOrangeLight = Color(0xFFFF9A5A)
val AdoptOrangeDark = Color(0xFFE05D28)

// 2. Verde/teal (parte inferior del header, detalles) y degradados
val AdoptTeal = Color(0xFF2FA88A)
val AdoptTealDark = Color(0xFF1D7A6B)
val AdoptTealLight = Color(0xFFE6F6F2)

// 3. Verde oscuro botón "Iniciar Sesión"
val AdoptGreenButton = Color(0xFF1A5C4A)
val AdoptGreenButtonDark = Color(0xFF134537)

// 4. Fondos y tarjetas
val AdoptMeBg = Color(0xFFFAF7F2)
val AdoptCardWhite = Color(0xFFFFFFFF)
val AdoptMeSurface = Color(0xFFFFFFFF)
val AdoptMeSurfaceVariant = Color(0xFFF3EFE9) // Crema suave para sub-tarjetas
val AdoptSurfaceVariant = AdoptMeSurfaceVariant
val BorderWarm = Color(0xFFE8E2D9)

// 5. Textos
val AdoptTextDark = Color(0xFF1A1A2E)       // Títulos card (casi negro azulado)
val AdoptTextSecondary = Color(0xFF6B7280)  // Subtítulos (gris medio)
val TextMain = Color(0xFF1A1A2E)
val TextMuted = Color(0xFF6B7280)

// Aliases para compatibilidad con código existente
val AdoptMeGreen = AdoptTeal
val AdoptMeGreenDark = AdoptTealDark
val AdoptMeGreenLight = AdoptTealLight
val AdoptMeOrange = AdoptOrange
val AdoptMeOrangeDark = AdoptOrangeDark
val AdoptMeOrangeLight = AdoptOrangeLight

// 6. Colores de estado suaves (no agresivos)
val StatusGreenBg = Color(0xFFE8F5E9)
val StatusGreenText = Color(0xFF2E7D32)

val StatusOrangeBg = Color(0xFFFFF3E0)
val StatusOrangeText = Color(0xFFD35400)

val StatusGrayBg = Color(0xFFECEFF1)
val StatusGrayText = Color(0xFF455A64)

val StatusRedBg = Color(0xFFFFEBEE)
val StatusRedText = Color(0xFFC62828)