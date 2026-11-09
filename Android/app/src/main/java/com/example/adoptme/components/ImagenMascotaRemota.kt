package com.example.adoptme.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.adoptme.R
import com.example.adoptme.ui.theme.AdoptTeal

/**
 * Componente unificado y robusto para renderizado de fotografías de mascotas (Fase A).
 * Orden de resolución estricto:
 * 1. Si existe ImageUrl HTTPS válida -> Carga asíncrona mediante Coil con soporte de caché, placeholder y error fallback.
 * 2. Si existe photoBitmap local temporal -> Muestra el Bitmap en memoria.
 * 3. Si no existe o falla la red -> Muestra la ilustración fallback por especie correspondiente.
 */
@Composable
fun ImagenMascotaRemota(
    imageUrl: String?,
    photoBitmap: Bitmap? = null,
    fallbackDrawableRes: Int = R.drawable.max,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val fallbackSeguro = if (fallbackDrawableRes != 0) fallbackDrawableRes else R.drawable.max

    if (!imageUrl.isNullOrBlank() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
        val contexto = LocalContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(contexto)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = AdoptTeal
                    )
                }
            },
            error = {
                Image(
                    painter = painterResource(id = fallbackSeguro),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        )
    } else if (photoBitmap != null) {
        Image(
            bitmap = photoBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Image(
            painter = painterResource(id = fallbackSeguro),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
