package com.example.adoptme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.adoptme.SharedPreferences.Preferencias

// =====================================================
// VERDE
// =====================================================

private val VerdeClaro = lightColorScheme(
    primary = AdoptMeGreen,
    onPrimary = Color.White,

    primaryContainer = AdoptMeGreenLight,
    onPrimaryContainer = AdoptMeGreenDark,

    secondary = AdoptMeOrange,
    onSecondary = Color.White,

    secondaryContainer = AdoptMeOrangeLight,
    onSecondaryContainer = AdoptMeOrangeDark,

    background = AdoptMeBg,
    surface = AdoptMeSurface,
    surfaceVariant = AdoptMeSurfaceVariant,

    onBackground = TextMain,
    onSurface = TextMain,
    onSurfaceVariant = TextMuted,

    outline = BorderWarm
)

private val VerdeOscuro = darkColorScheme(
    primary = AdoptMeGreenLight,
    onPrimary = AdoptMeGreenDark,

    primaryContainer = AdoptMeGreen,
    onPrimaryContainer = Color.White,

    secondary = AdoptMeOrange,
    onSecondary = Color.White,

    background = Color(0xFF191615),
    surface = Color(0xFF221F1E),
    surfaceVariant = Color(0xFF2D2A28),

    onBackground = Color(0xFFEDE8E3),
    onSurface = Color(0xFFEDE8E3),
    onSurfaceVariant = Color(0xFFD1C7C0),

    outline = Color(0xFF776E68)
)

// =====================================================
// AZUL
// =====================================================

private val AzulClaro = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,

    primaryContainer = Color(0xFFD7E9FF),
    onPrimaryContainer = Color(0xFF003258),

    secondary = AdoptMeOrange,
    onSecondary = Color.White,

    background = Color(0xFFF8FAFD),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EEF6),

    onBackground = Color(0xFF191C20),
    onSurface = Color(0xFF191C20),
    onSurfaceVariant = Color(0xFF42474E),

    outline = Color(0xFF73777F)
)

private val AzulOscuro = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),

    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),

    secondary = Color(0xFFFFB77D),
    onSecondary = Color(0xFF4D2600),

    background = Color(0xFF111318),
    surface = Color(0xFF191C20),
    surfaceVariant = Color(0xFF272A30),

    onBackground = Color(0xFFE2E2E9),
    onSurface = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFFC2C7CF),

    outline = Color(0xFF8C9199)
)

// =====================================================
// MORADO
// =====================================================

private val MoradoClaro = lightColorScheme(
    primary = Color(0xFF7655A6),
    onPrimary = Color.White,

    primaryContainer = Color(0xFFEBDDFF),
    onPrimaryContainer = Color(0xFF2D1054),

    secondary = AdoptMeOrange,
    onSecondary = Color.White,

    background = Color(0xFFFFF9FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0EAF2),

    onBackground = Color(0xFF1D1A20),
    onSurface = Color(0xFF1D1A20),
    onSurfaceVariant = Color(0xFF4B454D),

    outline = Color(0xFF7D747E)
)

private val MoradoOscuro = darkColorScheme(
    primary = Color(0xFFD8B9FF),
    onPrimary = Color(0xFF3E1E66),

    primaryContainer = Color(0xFF56377D),
    onPrimaryContainer = Color(0xFFEBDDFF),

    secondary = Color(0xFFFFB77D),
    onSecondary = Color(0xFF4D2600),

    background = Color(0xFF151217),
    surface = Color(0xFF1D1A20),
    surfaceVariant = Color(0xFF2B272D),

    onBackground = Color(0xFFE8E0E8),
    onSurface = Color(0xFFE8E0E8),
    onSurfaceVariant = Color(0xFFCEC5CF),

    outline = Color(0xFF978E98)
)

// =====================================================
// TEMA PRINCIPAL
// =====================================================

@Composable
fun AdoptMeTheme(
    tema: String = Preferencias.TEMA_SISTEMA,
    colorPrincipal: String = Preferencias.COLOR_VERDE,
    tamanoTexto: String = Preferencias.TEXTO_NORMAL,
    content: @Composable () -> Unit
) {

    val usarOscuro = when (tema) {

        Preferencias.TEMA_OSCURO ->
            true

        Preferencias.TEMA_CLARO ->
            false

        else ->
            isSystemInDarkTheme()
    }

    val esquemaColores = when (colorPrincipal) {

        Preferencias.COLOR_AZUL -> {

            if (usarOscuro) {
                AzulOscuro
            } else {
                AzulClaro
            }
        }

        Preferencias.COLOR_MORADO -> {

            if (usarOscuro) {
                MoradoOscuro
            } else {
                MoradoClaro
            }
        }

        else -> {

            if (usarOscuro) {
                VerdeOscuro
            } else {
                VerdeClaro
            }
        }
    }

    val escalaTexto = when (tamanoTexto) {

        Preferencias.TEXTO_PEQUENO ->
            0.90f

        Preferencias.TEXTO_GRANDE ->
            1.15f

        else ->
            1f
    }

    val tipografiaPersonalizada =
        Typography.copy(

            displayLarge =
                Typography.displayLarge.copy(
                    fontSize =
                        Typography
                            .displayLarge
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            headlineLarge =
                Typography.headlineLarge.copy(
                    fontSize =
                        Typography
                            .headlineLarge
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            headlineMedium =
                Typography.headlineMedium.copy(
                    fontSize =
                        Typography
                            .headlineMedium
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            headlineSmall =
                Typography.headlineSmall.copy(
                    fontSize =
                        Typography
                            .headlineSmall
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            titleLarge =
                Typography.titleLarge.copy(
                    fontSize =
                        Typography
                            .titleLarge
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            titleMedium =
                Typography.titleMedium.copy(
                    fontSize =
                        Typography
                            .titleMedium
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            titleSmall =
                Typography.titleSmall.copy(
                    fontSize =
                        Typography
                            .titleSmall
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            bodyLarge =
                Typography.bodyLarge.copy(
                    fontSize =
                        Typography
                            .bodyLarge
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            bodyMedium =
                Typography.bodyMedium.copy(
                    fontSize =
                        Typography
                            .bodyMedium
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            bodySmall =
                Typography.bodySmall.copy(
                    fontSize =
                        Typography
                            .bodySmall
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            labelLarge =
                Typography.labelLarge.copy(
                    fontSize =
                        Typography
                            .labelLarge
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            labelMedium =
                Typography.labelMedium.copy(
                    fontSize =
                        Typography
                            .labelMedium
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                ),

            labelSmall =
                Typography.labelSmall.copy(
                    fontSize =
                        Typography
                            .labelSmall
                            .fontSize
                            .value
                            .times(escalaTexto)
                            .sp
                )
        )

    MaterialTheme(
        colorScheme = esquemaColores,
        typography = tipografiaPersonalizada,
        content = content
    )
}