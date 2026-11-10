package com.example.adoptme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.model.CentroPublicoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface CentrosUiState {
    data object Cargando : CentrosUiState
    data class Exito(val centros: List<CentroPublicoDto>) : CentrosUiState
    data class Error(val mensaje: String) : CentrosUiState
}

class CentrosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CentrosUiState>(CentrosUiState.Cargando)
    val uiState: StateFlow<CentrosUiState> = _uiState.asStateFlow()

    private var ultimoDistrito: String? = null
    private var ultimoQuery: String? = null

    init {
        cargarCentros()
    }

    fun cargarCentros(distrito: String? = null, q: String? = null) {
        ultimoDistrito = distrito
        ultimoQuery = q
        _uiState.value = CentrosUiState.Cargando

        viewModelScope.launch {
            try {
                val resultado = RetrofitClient.centrosApi.obtenerCentros(distrito, q)
                _uiState.value = CentrosUiState.Exito(resultado)
            } catch (e: IOException) {
                _uiState.value = CentrosUiState.Error(
                    "No se pudo conectar con el servidor. Verifica tu conexión a internet o intenta nuevamente."
                )
            } catch (e: retrofit2.HttpException) {
                val mensaje = when (e.code()) {
                    404 -> "No se encontraron centros de adopción disponibles."
                    500, 502, 503 -> "El servicio de centros no está disponible temporalmente. Intente más tarde."
                    else -> "Error al consultar los centros de adopción (${e.code()})."
                }
                _uiState.value = CentrosUiState.Error(mensaje)
            } catch (e: Exception) {
                _uiState.value = CentrosUiState.Error(
                    e.localizedMessage ?: "Ocurrió un error inesperado al cargar los centros."
                )
            }
        }
    }

    fun reintentar() {
        cargarCentros(ultimoDistrito, ultimoQuery)
    }
}
