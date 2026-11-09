package com.example.adoptme.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Representa un par de coordenadas geográficas (latitud, longitud).
 */
data class CoordenadaGps(val latitude: Double, val longitude: Double)

/**
 * Gestor de geolocalización y permisos de ubicación para AdoptMe.
 * Entrega #4: Integración con Google Maps y Geolocalización.
 */
object LocationHelper {

    /**
     * Verifica si el usuario concedió permisos de ubicación (fina o aproximada).
     */
    fun tienePermisoUbicacion(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Verifica si el hardware de GPS o proveedor por red están activados en el dispositivo.
     */
    fun esGpsHabilitado(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Obtiene las coordenadas actuales (latitud, longitud) del dispositivo de forma asíncrona.
     * Retorna null si no hay permiso, si el GPS está apagado o ante cualquier error de señal.
     */
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(context: Context): CoordenadaGps? {
        if (!tienePermisoUbicacion(context)) return null
        if (!esGpsHabilitado(context)) return null

        return suspendCancellableCoroutine { continuation ->
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()

                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            if (continuation.isActive) continuation.resume(CoordenadaGps(loc.latitude, loc.longitude))
                        } else {
                            // Fallback a última ubicación conocida
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) {
                                    if (lastLoc != null) {
                                        continuation.resume(CoordenadaGps(lastLoc.latitude, lastLoc.longitude))
                                    } else {
                                        continuation.resume(null)
                                    }
                                }
                            }.addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
