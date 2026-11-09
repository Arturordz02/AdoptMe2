package com.example.adoptme.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

data class ResultadoProcesamientoImagen(
    val bitmap: Bitmap,
    val bytes: ByteArray,
    val tamanoBytes: Long,
    val tamanoLegible: String
)

object ImageProcessor {

    private const val DIMENSION_MAXIMA = 1024
    private const val CALIDAD_COMPRESION_INICIAL = 80
    private const val LIMITE_MAXIMO_BYTES = 2L * 1024 * 1024 // 2 MB

    /**
     * Crea un archivo temporal en la caché privada de la app para capturar una foto
     * en alta resolución con la cámara mediante FileProvider sin exponer file:// URI.
     */
    fun crearArchivoTemporalCamara(context: Context): Pair<File, Uri> {
        val directorio = File(context.cacheDir, "camera_photos").apply { if (!exists()) mkdirs() }
        val archivoTemporal = File.createTempFile(
            "captura_${System.currentTimeMillis()}_",
            ".jpg",
            directorio
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivoTemporal
        )
        return Pair(archivoTemporal, uri)
    }

    /**
     * Procesa una imagen de forma asíncrona fuera del hilo principal (Dispatchers.IO):
     * 1. Decodifica las dimensiones originales sin cargar los píxeles (inJustDecodeBounds).
     * 2. Calcula un submuestreo seguro (inSampleSize) para prevenir OutOfMemory.
     * 3. Corrige la orientación física según los metadatos EXIF.
     * 4. Escala proporcionalmente a una resolución máxima de 1024x1024.
     * 5. Comprime a formato JPEG con calidad 80 (reduciendo si supera 2 MB).
     */
    suspend fun procesarImagenUri(
        context: Context,
        uri: Uri,
        archivoLocal: File? = null
    ): ResultadoProcesamientoImagen = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        fun abrirStream(): java.io.InputStream? {
            Log.d("AdoptMe", "abrirStream archivoLocal=$archivoLocal existe=${archivoLocal?.exists()} len=${archivoLocal?.length()} uri=$uri")
            return if (archivoLocal != null && archivoLocal.exists() && archivoLocal.length() > 0L) {
                java.io.FileInputStream(archivoLocal)
            } else {
                try {
                    resolver.openInputStream(uri)
                } catch (e: Exception) {
                    Log.e("AdoptMe", "openInputStream fallo para uri=$uri", e)
                    null
                }
            }
        }

        // 1. Obtener dimensiones sin cargar a memoria
        val opcionesMedida = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val streamMedida = abrirStream() ?: throw IllegalArgumentException("No se pudo abrir el flujo de la imagen seleccionada.")
        streamMedida.use { stream ->
            BitmapFactory.decodeStream(stream, null, opcionesMedida)
        }

        var anchoOriginal = opcionesMedida.outWidth
        var altoOriginal = opcionesMedida.outHeight
        if (anchoOriginal <= 0 || altoOriginal <= 0) {
            throw IllegalArgumentException("La imagen seleccionada tiene dimensiones inválidas o está corrupta.")
        }

        // 2. Calcular inSampleSize para decodificar muestreado
        var inSampleSize = 1
        val mayorDimension = max(anchoOriginal, altoOriginal)
        while ((mayorDimension / inSampleSize) > (DIMENSION_MAXIMA * 2)) {
            inSampleSize *= 2
        }

        // 3. Decodificar Bitmap muestreado
        val opcionesDecodificacion = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val streamDecodificacion = abrirStream() ?: throw IllegalStateException("Error al abrir flujo para decodificar la imagen muestreada.")
        val bitmapMuestreado = streamDecodificacion.use { stream ->
            BitmapFactory.decodeStream(stream, null, opcionesDecodificacion)
        } ?: throw IllegalStateException("Error al decodificar la imagen muestreada.")

        // 4. Leer orientación EXIF
        var rotacionGrados = 0
        try {
            if (archivoLocal != null && archivoLocal.exists() && archivoLocal.length() > 0L) {
                val exif = ExifInterface(archivoLocal.absolutePath)
                val orientacion = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                rotacionGrados = when (orientacion) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } else {
                abrirStream()?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientacion = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotacionGrados = when (orientacion) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            }
        } catch (_: Exception) {
            // Si el stream no tiene metadatos EXIF (ej. PNG o captura virtual), se conserva orientación estándar
        }

        // 5. Corregir rotación si aplica
        val bitmapOrientado = if (rotacionGrados != 0) {
            val matriz = Matrix().apply { postRotate(rotacionGrados.toFloat()) }
            val rotado = Bitmap.createBitmap(
                bitmapMuestreado,
                0,
                0,
                bitmapMuestreado.width,
                bitmapMuestreado.height,
                matriz,
                true
            )
            if (rotado != bitmapMuestreado) {
                bitmapMuestreado.recycle()
            }
            rotado
        } else {
            bitmapMuestreado
        }

        // 6. Escalar proporcionalmente a máx 1024x1024
        val anchoActual = bitmapOrientado.width
        val altoActual = bitmapOrientado.height
        val maxActual = max(anchoActual, altoActual)

        val bitmapFinal = if (maxActual > DIMENSION_MAXIMA) {
            val factorEscala = DIMENSION_MAXIMA.toFloat() / maxActual
            val anchoEscalado = (anchoActual * factorEscala).toInt()
            val altoEscalado = (altoActual * factorEscala).toInt()

            val escalado = Bitmap.createScaledBitmap(
                bitmapOrientado,
                anchoEscalado,
                altoEscalado,
                true
            )
            if (escalado != bitmapOrientado) {
                bitmapOrientado.recycle()
            }
            escalado
        } else {
            bitmapOrientado
        }

        // 7. Compresión a JPEG
        var calidad = CALIDAD_COMPRESION_INICIAL
        var bytes = comprimirBitmap(bitmapFinal, calidad)

        // Si por alguna razón el archivo comprimido supera 2 MB, reducir calidad iterativamente
        while (bytes.size > LIMITE_MAXIMO_BYTES && calidad > 40) {
            calidad -= 15
            bytes = comprimirBitmap(bitmapFinal, calidad)
        }

        if (bytes.size > LIMITE_MAXIMO_BYTES) {
            throw IllegalStateException("No fue posible comprimir la fotografía por debajo del límite de 2 MB.")
        }

        val tamanoLegible = formatearTamanoBytes(bytes.size.toLong())

        ResultadoProcesamientoImagen(
            bitmap = bitmapFinal,
            bytes = bytes,
            tamanoBytes = bytes.size.toLong(),
            tamanoLegible = tamanoLegible
        )
    }

    private fun comprimirBitmap(bitmap: Bitmap, calidad: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, outputStream)
        return outputStream.toByteArray()
    }

    fun formatearTamanoBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
