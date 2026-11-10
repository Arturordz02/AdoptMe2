package com.example.adoptme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.model.ActualizarMiCentroRequest
import com.example.adoptme.model.CentroPropietarioDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.json.JSONObject
import java.io.IOException

sealed interface MiCentroUiState {
    data object Inactivo : MiCentroUiState
    data object Cargando : MiCentroUiState
    data class Exito(val centro: CentroPropietarioDto) : MiCentroUiState
    data class Error(val mensaje: String, val codigoHttp: Int? = null, val estado: String? = null) : MiCentroUiState
}

class MiCentroViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MiCentroUiState>(MiCentroUiState.Inactivo)
    val uiState: StateFlow<MiCentroUiState> = _uiState.asStateFlow()

    private val _estaOperando = MutableStateFlow(false)
    val estaOperando: StateFlow<Boolean> = _estaOperando.asStateFlow()

    fun cargarMiCentro() {
        _uiState.value = MiCentroUiState.Cargando
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.obtenerMiCentro()
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val centro = respuesta.body()!!
                    _uiState.value = MiCentroUiState.Exito(centro)
                    // Sincronizar en Preferencias
                    RetrofitClient.preferencias?.actualizarEstadoCentro(centro.estado, centro.activo)
                    RetrofitClient.preferencias?.actualizarCentroId(centro.id)
                } else {
                    val codigo = respuesta.code()
                    val cuerpoError = respuesta.errorBody()?.string()
                    var mensaje = "No se pudo obtener el perfil de tu centro."
                    var estado: String? = null

                    if (!cuerpoError.isNullOrBlank()) {
                        try {
                            val json = JSONObject(cuerpoError)
                            if (json.has("mensaje")) mensaje = json.getString("mensaje")
                            if (json.has("estado")) estado = json.getString("estado")
                        } catch (_: Exception) { }
                    }

                    if (codigo == 404 && estado == "SinPerfil") {
                        mensaje = "El usuario autenticado no tiene un Centro de Adopción vinculado."
                    } else if (codigo == 401) {
                        mensaje = "Tu sesión ha expirado o no es válida. Inicia sesión nuevamente."
                    } else if (codigo == 403) {
                        mensaje = "Acceso restringido a cuentas de Centro o Administrador."
                    }

                    _uiState.value = MiCentroUiState.Error(mensaje, codigoHttp = codigo, estado = estado)
                }
            } catch (e: IOException) {
                _uiState.value = MiCentroUiState.Error("Sin conexión con el servidor. Revisa tu red.")
            } catch (e: Exception) {
                _uiState.value = MiCentroUiState.Error(e.localizedMessage ?: "Error al cargar información del centro.")
            }
        }
    }

    fun actualizarPerfil(
        request: ActualizarMiCentroRequest,
        onResultado: (exito: Boolean, mensaje: String, pasoAPendiente: Boolean) -> Unit
    ) {
        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.actualizarMiCentro(request)
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val centroActualizado = respuesta.body()!!
                    val pasoAPendiente = centroActualizado.estado.equals("Pendiente", ignoreCase = true)
                    _uiState.value = MiCentroUiState.Exito(centroActualizado)

                    RetrofitClient.preferencias?.actualizarEstadoCentro(
                        centroActualizado.estado,
                        centroActualizado.activo
                    )

                    val mensajeExito = if (pasoAPendiente) {
                        "Perfil actualizado. Debido a cambios sensibles en nombre o ubicación, tu solicitud requiere nueva revisión administrativa."
                    } else {
                        "Perfil del centro actualizado correctamente."
                    }

                    onResultado(true, mensajeExito, pasoAPendiente)
                } else {
                    val cuerpoError = respuesta.errorBody()?.string()
                    var mensaje = "Error al actualizar perfil (${respuesta.code()})."
                    if (!cuerpoError.isNullOrBlank()) {
                        try {
                            val json = JSONObject(cuerpoError)
                            if (json.has("mensaje")) mensaje = json.getString("mensaje")
                        } catch (_: Exception) { }
                    }
                    onResultado(false, mensaje, false)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo de conexión al actualizar.", false)
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun reenviarSolicitud(onResultado: (exito: Boolean, mensaje: String) -> Unit) {
        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.reenviarSolicitud()
                if (respuesta.isSuccessful) {
                    val cuerpo = respuesta.body()
                    val mensaje = cuerpo?.mensaje ?: "Solicitud reenviada exitosamente a revisión."
                    cargarMiCentro()
                    onResultado(true, mensaje)
                } else {
                    val cuerpoError = respuesta.errorBody()?.string()
                    var mensaje = "No se pudo reenviar la solicitud (${respuesta.code()})."
                    if (!cuerpoError.isNullOrBlank()) {
                        try {
                            val json = JSONObject(cuerpoError)
                            if (json.has("mensaje")) mensaje = json.getString("mensaje")
                        } catch (_: Exception) { }
                    }
                    onResultado(false, mensaje)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Error al comunicarse con el servidor.")
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun actualizarLogo(
        imagenPart: MultipartBody.Part,
        onResultado: (exito: Boolean, mensaje: String, nuevoLogoUrl: String?) -> Unit
    ) {
        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.actualizarLogo(imagenPart)
                if (respuesta.isSuccessful) {
                    val cuerpo = respuesta.body()
                    val mensaje = cuerpo?.mensaje ?: "Logotipo actualizado exitosamente."
                    val nuevoUrl = cuerpo?.logoUrl
                    cargarMiCentro()
                    onResultado(true, mensaje, nuevoUrl)
                } else {
                    val cuerpoError = respuesta.errorBody()?.string()
                    var mensaje = "Error al subir logotipo (${respuesta.code()})."
                    if (!cuerpoError.isNullOrBlank()) {
                        try {
                            val json = JSONObject(cuerpoError)
                            if (json.has("mensaje")) mensaje = json.getString("mensaje")
                        } catch (_: Exception) { }
                    }
                    onResultado(false, mensaje, null)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo al enviar la imagen.", null)
            } finally {
                _estaOperando.value = false
            }
        }
    }
}
