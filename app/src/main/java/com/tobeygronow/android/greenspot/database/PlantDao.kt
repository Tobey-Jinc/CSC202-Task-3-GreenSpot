package com.tobeygronow.android.greenspot.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tobeygronow.android.greenspot.Plant
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * The DAO for the Plant Database
 */
@Dao
interface PlantDao {
    /**
     * Returns all Plants in the database
     */
    @Query("SELECT * FROM plant")
    fun getPlants(): Flow<List<Plant>>

    /**
     * Gets a specific plant using a Plants id
     */
    @Query("SELECT * FROM plant WHERE id=(:id)")
    suspend fun getPlant(id: UUID): Plant

    /**
     * Updates the specified plant
     */
    @Update
    suspend fun updatePlant(plant: Plant)

    /**
     * Inserts the provided Plant into the database
     */
    @Insert
    suspend fun addPlant(plant: Plant)

    /**
     * Removes the specified Plant from the database
     */
    @Delete
    suspend fun removePlant(plant: Plant)
}
