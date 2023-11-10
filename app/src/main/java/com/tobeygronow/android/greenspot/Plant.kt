package com.tobeygronow.android.greenspot

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.util.Date

/**
 * The model class of a Plant
 */
@Entity
data class Plant(
    @PrimaryKey val id: UUID, // Unique ID used as a primary key
    val title: String,
    val place: String,
    val date: Date,
    val latitude: Double? = null, // Starts as null until user sets location
    val longitude: Double? = null, // Starts as null until user sets location
    val photoFileName: String? = null
)
