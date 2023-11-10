package com.tobeygronow.android.greenspot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The ViewModel for Plant details
 */
class PlantDetailViewModel(plantId: UUID) : ViewModel() {
    private val plantRepository = PlantRepository.get()

    // State flow for getting up to date Plant data
    private val _plant: MutableStateFlow<Plant?> = MutableStateFlow(null)
    val plant: StateFlow<Plant?> = _plant.asStateFlow()

    init {
        viewModelScope.launch {
            // Get the Plant
            _plant.value = plantRepository.getPlant(plantId)
        }
    }

    /**
     * Updates the current Plant
     */
    fun updatePlant(onUpdate: (Plant) -> Plant) {
        _plant.update { oldPlant ->
            oldPlant?.let { onUpdate(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        plant.value?.let { plantRepository.updatePlant(it) }
    }
}

/**
 * Creates instances of PlantDetailViewModel
 */
class PlantDetailViewModelFactory(
    private val plantId: UUID
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlantDetailViewModel(plantId) as T
    }
}
