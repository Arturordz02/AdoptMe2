package com.example.adoptme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.model.AsignarPropietarioCentroRequest
import com.example.adoptme.model.CentroAdminDto
import com.example.adoptme.model.RechazarCentroRequest
import com.example.adoptme.model.SuspenderCentroRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

sealed interface AdminCentrosUiState {
    data object Cargando : AdminCentrosUiState
    data class Exito(val centros: List<CentroAdminDto>) : AdminCentrosUiState
    data class Error(val mensaje: String) : AdminCentrosUiState
}

class AdminCentrosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AdminCentrosUiState>(AdminCentrosUiState.Cargando)
    val uiState: StateFlow<AdminCentrosUiState> = _uiState.asStateFlow()

    private val _estaOperando = MutableStateFlow(false)
    val estaOperando: StateFlow<Boolean> = _estaOperando.asStateFlow()

    private var ultimoFiltroEstado: String? = null
    private var ultimoFiltroActivo: Boolean? = null
    private var ultimoQuery: String? = null

    fun cargarCentros(
        estado: String? = null,
        activo: Boolean? = null,
        q: String? = null
    ) {
        ultimoFiltroEstado = estado
        ultimoFiltroActivo = activo
        ultimoQuery = q
        _uiState.value = AdminCentrosUiState.Cargando

        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.listarCentrosAdmin(
                    estado = if (estado == "Todos") null else estado,
                    activo = activo,
                    q = q
                )
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    _uiState.value = AdminCentrosUiState.Exito(respuesta.body()!!)
                } else {
                    _uiState.value = AdminCentrosUiState.Error(
                        "Error al consultar centros administrativos (${respuesta.code()})."
                    )
                }
            } catch (e: IOException) {
                _uiState.value = AdminCentrosUiState.Error(RetrofitClient.resolverMensajeErrorServidor(e))
            } catch (e: Exception) {
                _uiState.value = AdminCentrosUiState.Error(e.localizedMessage ?: "Error inesperado.")
            }
        }
    }

    fun refrescar() {
        cargarCentros(ultimoFiltroEstado, ultimoFiltroActivo, ultimoQuery)
    }

    fun aprobarCentro(id: Int, onResultado: (Boolean, String) -> Unit) {
        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.aprobarCentro(id)
                if (respuesta.isSuccessful) {
                    refrescar()
                    onResultado(true, "Centro aprobado exitosamente.")
                } else {
                    val msg = procesarErrorHttp(respuesta.code(), respuesta.errorBody()?.string())
                    onResultado(false, msg)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo de conexión.")
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun rechazarCentro(id: Int, motivo: String, onResultado: (Boolean, String) -> Unit) {
        if (motivo.trim().length < 5) {
            onResultado(false, "El motivo de rechazo debe contener al menos 5 caracteres explicativos.")
            return
        }

        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.rechazarCentro(id, RechazarCentroRequest(motivo.trim()))
                if (respuesta.isSuccessful) {
                    refrescar()
                    onResultado(true, "Centro rechazado con motivo registrado.")
                } else {
                    val msg = procesarErrorHttp(respuesta.code(), respuesta.errorBody()?.string())
                    onResultado(false, msg)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo de conexión.")
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun activarCentro(id: Int, onResultado: (Boolean, String) -> Unit) {
        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.activarCentro(id)
                if (respuesta.isSuccessful) {
                    refrescar()
                    onResultado(true, "Centro activado correctamente.")
                } else {
                    val msg = procesarErrorHttp(respuesta.code(), respuesta.errorBody()?.string())
                    onResultado(false, msg)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo de conexión.")
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun desactivarCentro(id: Int, onResultado: (Boolean, String) -> Unit) {
        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.desactivarCentro(id)
                if (respuesta.isSuccessful) {
                    refrescar()
                    onResultado(true, "Centro desactivado temporalmente.")
                } else {
                    val msg = procesarErrorHttp(respuesta.code(), respuesta.errorBody()?.string())
                    onResultado(false, msg)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo de conexión.")
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun suspenderCentro(id: Int, motivo: String, onResultado: (Boolean, String) -> Unit) {
        if (motivo.trim().length < 5) {
            onResultado(false, "El motivo de suspensión debe contener al menos 5 caracteres explicativos.")
            return
        }

        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.suspenderCentro(id, SuspenderCentroRequest(motivo.trim()))
                if (respuesta.isSuccessful) {
                    refrescar()
                    onResultado(true, "Centro suspendido administrativamente.")
                } else {
                    val msg = procesarErrorHttp(respuesta.code(), respuesta.errorBody()?.string())
                    onResultado(false, msg)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo de conexión.")
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun reactivarCentro(id: Int, onResultado: (Boolean, String) -> Unit) {
        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.reactivarCentro(id)
                if (respuesta.isSuccessful) {
                    refrescar()
                    onResultado(true, "Centro reactivado exitosamente.")
                } else {
                    val msg = procesarErrorHttp(respuesta.code(), respuesta.errorBody()?.string())
                    onResultado(false, msg)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo de conexión.")
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun vincularResponsable(id: Int, usuarioId: Int, onResultado: (Boolean, String) -> Unit) {
        _estaOperando.value = true
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.centrosApi.vincularResponsable(
                    id,
                    AsignarPropietarioCentroRequest(usuarioId)
                )
                if (respuesta.isSuccessful) {
                    refrescar()
                    onResultado(true, "Usuario responsable vinculado exitosamente.")
                } else {
                    val msg = procesarErrorHttp(respuesta.code(), respuesta.errorBody()?.string())
                    onResultado(false, msg)
                }
            } catch (e: Exception) {
                onResultado(false, e.localizedMessage ?: "Fallo de conexión.")
            } finally {
                _estaOperando.value = false
            }
        }
    }

    fun asignarPropietario(id: Int, usuarioId: Int, onResultado: (Boolean, String) -> Unit) {
        vincularResponsable(id, usuarioId, onResultado)
    }

    private fun procesarErrorHttp(codigoHttp: Int, cuerpo: String?): String {
        if (codigoHttp == 401) {
            RetrofitClient.preferencias?.limpiarSesionActiva()
            return "Sesión expirada o no autorizada. Por favor vuelve a iniciar sesión."
        }
        if (codigoHttp == 403) {
            return "Acceso denegado: se requieren credenciales de Administrador."
        }
        if (codigoHttp == 404) {
            refrescar()
            return "El centro no fue encontrado en el servidor."
        }
        if (codigoHttp == 409) {
            val msgFromJson = extraerMensajeJson(cuerpo)
            return msgFromJson ?: "Conflicto: El usuario ya es responsable de otro centro, o este centro ya tiene un responsable asignado."
        }
        return extraerMensajeJson(cuerpo) ?: "Error en la operación del servidor ($codigoHttp)."
    }

    private fun extraerMensajeJson(cuerpo: String?): String? {
        if (cuerpo.isNullOrBlank()) return null
        return try {
            val json = JSONObject(cuerpo)
            if (json.has("mensaje")) json.getString("mensaje") else null
        } catch (_: Exception) {
            null
        }
    }
}
