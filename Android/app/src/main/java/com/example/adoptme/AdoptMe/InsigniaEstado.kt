package com.example.adoptme.AdoptMe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adoptme.ui.theme.StatusGrayBg
import com.example.adoptme.ui.theme.StatusGrayText
import com.example.adoptme.ui.theme.StatusGreenBg
import com.example.adoptme.ui.theme.StatusGreenText
import com.example.adoptme.ui.theme.StatusOrangeBg
import com.example.adoptme.ui.theme.StatusOrangeText
import com.example.adoptme.ui.theme.StatusRedBg
import com.example.adoptme.ui.theme.StatusRedText

@Composable
fun InsigniaEstado(
    estado: String,
    modificador: Modifier = Modifier
) {
    val (colorFondo, colorTexto, etiqueta) = when (estado.lowercase().trim()) {
        "disponible" -> Triple(StatusGreenBg, StatusGreenText, "🟢 Disponible")
        "en proceso" -> Triple(StatusOrangeBg, StatusOrangeText, "⏳ En proceso")
        "adoptada" -> Triple(StatusGrayBg, StatusGrayText, "🏠 Adoptada")
        "pendiente" -> Triple(StatusOrangeBg, StatusOrangeText, "⏳ Pendiente")
        "en revisión", "en revision" -> Triple(StatusOrangeBg, StatusOrangeText, "🔍 En revisión")
        "aprobada" -> Triple(StatusGreenBg, StatusGreenText, "✅ Aprobada")
        "rechazada" -> Triple(StatusRedBg, StatusRedText, "❌ Rechazada")
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            estado
        )
    }

    Box(
        modifier = modificador
            .clip(RoundedCornerShape(8.dp))
            .background(colorFondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = etiqueta,
            color = colorTexto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

