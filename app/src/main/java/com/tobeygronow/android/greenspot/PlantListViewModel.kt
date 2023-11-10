package com.tobeygronow.android.greenspot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlantListViewModel"

/**
 * Stores the list of Plants to be displayed
 */
class PlantListViewModel : ViewModel() {
    private val plantRepository = PlantRepository.get()

    // Up to date stream of Plants from the database
    private val _plants: MutableStateFlow<List<Plant>> = MutableStateFlow(emptyList())
    val plants: StateFlow<List<Plant>>
        get() = _plants.asStateFlow()

    init {
        viewModelScope.launch {
            plantRepository.getPlants().collect {
                _plants.value = it
            }
        }
    }

    /**
     * Adds a Plant to the database
     *
     * @param plant The Plant to add to the database
     */
    suspend fun addPlant(plant: Plant) {
        plantRepository.addPlant(plant)
    }
}