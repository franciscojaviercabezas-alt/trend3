package es.tendencias.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.tendencias.app.data.TrendsCache
import es.tendencias.app.data.TrendsRepository
import es.tendencias.app.model.Trend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TrendsUiState {
    data class Loading(val previousTrends: List<Trend>? = null) : TrendsUiState
    data class Success(
        val trends: List<Trend>,
        val lastUpdate: Long? = null
    ) : TrendsUiState
    data class Error(
        val message: String,
        val previousTrends: List<Trend>? = null,
        val lastUpdate: Long? = null
    ) : TrendsUiState
}

class TrendsViewModel(
    private val repository: TrendsRepository,
    private val cache: TrendsCache? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrendsUiState>(TrendsUiState.Loading())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private var cachedTrends: List<Trend>? = null

    init {
        val initialTrends = cache?.loadTrends()
        val initialLastUpdate = cache?.loadLastUpdate()
        if (!initialTrends.isNullOrEmpty()) {
            cachedTrends = initialTrends
            _uiState.value = TrendsUiState.Success(
                trends = initialTrends,
                lastUpdate = initialLastUpdate
            )
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (cachedTrends.isNullOrEmpty()) {
                cachedTrends = cache?.loadTrends()
            }
            if (cachedTrends.isNullOrEmpty()) {
                _uiState.value = TrendsUiState.Loading()
            }

            val result = repository.getTrends()
            result.fold(
                onSuccess = { trends ->
                    val now = System.currentTimeMillis()
                    cache?.saveTrends(trends)
                    cache?.saveLastUpdate(now)
                    cache?.clearError()
                    cachedTrends = trends
                    _uiState.value = TrendsUiState.Success(
                        trends = trends,
                        lastUpdate = now
                    )
                },
                onFailure = { error ->
                    val errorMsg = error.localizedMessage ?: "No se pudo actualizar"
                    cache?.saveLastError(errorMsg)
                    val trendsToDisplay = cachedTrends ?: cache?.loadTrends()
                    val lastUpdate = cache?.loadLastUpdate()
                    _uiState.value = TrendsUiState.Error(
                        message = errorMsg,
                        previousTrends = trendsToDisplay,
                        lastUpdate = lastUpdate
                    )
                }
            )
        }
    }
}
