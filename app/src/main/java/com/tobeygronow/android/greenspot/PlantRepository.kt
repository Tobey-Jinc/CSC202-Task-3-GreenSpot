package com.tobeygronow.android.greenspot

import android.content.Context
import androidx.room.Room
import com.tobeygronow.android.greenspot.database.PlantDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

private const val DATABASE_NAME = "plant-database"

/**
 * Provides easy access to the Plants database
 */
class PlantRepository private constructor(
    context: Context,
    private val coroutineScope: CoroutineScope = GlobalScope
) {
    private val database: PlantDatabase = Room
        // Build the database
        .databaseBuilder(
            context.applicationContext,
            PlantDatabase::class.java,
            DATABASE_NAME
        )
        .build()

    /**
     * Returns an up to date Flow of Plants from the database
     *
     * @return Flow of Plants
     */
    fun getPlants(): Flow<List<Plant>> = database.plantDao().getPlants()

    /**
     * Gets a Plant using its id
     *
     * @param id The id of the Plant to retrieve
     * @return The Plant with the specified id
     */
    suspend fun getPlant(id: UUID): Plant = database.plantDao().getPlant(id)

    /**
     * Updates the specified Plant
     *
     * @param plant The Plant to update
     */
    fun updatePlant(plant: Plant) {
        coroutineScope.launch {
            database.plantDao().updatePlant(plant)
        }
    }

    /**
     * Adds a Plant to the database
     *
     * @param plant The Plant to add to the database
     */
    suspend fun addPlant(plant: Plant) {
        database.plantDao().addPlant(plant)
    }

    /**
     * Removes a Plant from the database
     *
     * @param plant The Plant to be removed from the database
     */
    suspend fun removePlant(plant: Plant) {
        database.plantDao().removePlant(plant)
    }

    /**
     * Make the repository accessible as a singleton
     */
    companion object {
        private var INSTANCE: PlantRepository? = null

        /**
         * Ensures there is only one instance of PlantRepository
         */
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = PlantRepository(context)
            }
        }

        /**
         * Gets the PlantRepository
         *
         * @return PlantRepository
         */
        fun get(): PlantRepository {
            return INSTANCE ?:
            throw IllegalStateException("PlantRepository must be initialized")
        }
    }
}
